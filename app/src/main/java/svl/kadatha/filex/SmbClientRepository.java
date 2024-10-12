package svl.kadatha.filex;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.hierynomus.mssmb2.SMB2Dialect;
import com.hierynomus.mssmb2.SMBApiException;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.SmbConfig;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import timber.log.Timber;

public class SmbClientRepository {
    private static final String TAG = "Smb-SmbClientRepository";
    private static SmbClientRepository instance;
    private final SMBClient smbClient;
    private final ConcurrentLinkedQueue<Session> sessionPool;
    private final ConcurrentLinkedQueue<Session> inUseSessions;
    private final Map<Session, Long> lastUsedTimes;
    private final ScheduledExecutorService keepAliveScheduler = Executors.newScheduledThreadPool(1);
    private NetworkAccountsDetailsDialog.NetworkAccountPOJO networkAccountPOJO;
    private static final long IDLE_TIMEOUT = 180000; // 3 minutes
    private static final int MAX_IDLE_SESSIONS = 5;
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 1000; // 1 second delay between retries
    private int initialSessions;

    // Added property for shareName
    private final String shareName;

    private SmbClientRepository(NetworkAccountsDetailsDialog.NetworkAccountPOJO networkAccountPOJO) {
        this.networkAccountPOJO = networkAccountPOJO;

        // Set the shareName from networkAccountPOJO
        if (networkAccountPOJO.shareName != null && !networkAccountPOJO.shareName.isEmpty()) {
            this.shareName = networkAccountPOJO.shareName;
        } else {
            // Set to default or root directory if shareName is not provided
            this.shareName = "/"; // or set to a default share name if applicable
        }

        // Build SmbConfig with custom dialects if smbVersion is specified
        SmbConfig.Builder configBuilder = SmbConfig.builder()
                .withTimeout(60, TimeUnit.SECONDS)
                .withSoTimeout(60, TimeUnit.SECONDS);

        if (networkAccountPOJO.smbVersion != null && !networkAccountPOJO.smbVersion.isEmpty()) {
            // Parse the SMB version and set the dialects
            List<SMB2Dialect> dialects = getDialectsFromVersion(networkAccountPOJO.smbVersion);
            if (!dialects.isEmpty()) {
                // Corrected method name and usage
                configBuilder.withDialects(dialects.toArray(new SMB2Dialect[0]));
            }
        }

        SmbConfig config = configBuilder.build();

        this.smbClient = new SMBClient(config);
        this.sessionPool = new ConcurrentLinkedQueue<>();
        this.inUseSessions = new ConcurrentLinkedQueue<>();
        this.lastUsedTimes = new ConcurrentHashMap<>();
        initializeSessions();
    }

    public static synchronized SmbClientRepository getInstance(NetworkAccountsDetailsDialog.NetworkAccountPOJO networkAccountPOJO) {
        if (instance == null) {
            instance = new SmbClientRepository(networkAccountPOJO);
        }
        return instance;
    }

    private List<SMB2Dialect> getDialectsFromVersion(String smbVersion) {
        List<SMB2Dialect> dialects = new ArrayList<>();
        switch (smbVersion) {
            case "SMB1":
                // SMB1 is not supported by smbj due to security issues
                Timber.tag(TAG).w("SMB1 is not supported by SMBJ library");
                break;
            case "SMB2":
                dialects.add(SMB2Dialect.SMB_2_0_2);
                dialects.add(SMB2Dialect.SMB_2_1);
                break;
            case "SMB3":
                dialects.add(SMB2Dialect.SMB_3_0);
                dialects.add(SMB2Dialect.SMB_3_0_2);
                dialects.add(SMB2Dialect.SMB_3_1_1);
                break;
            default:
                // Use default dialects
                dialects.addAll(Arrays.asList(SMB2Dialect.values()));
                break;
        }
        return dialects;
    }

    private void initializeSessions() {
        initialSessions = Math.min(MAX_IDLE_SESSIONS, 5);
        // Initialize the first session
        try {
            Session session = createAndConnectSession();
            sessionPool.offer(session);
            lastUsedTimes.put(session, System.currentTimeMillis());
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize the first SMB session.", e);
        }

        // Initialize additional sessions
        for (int i = 1; i < initialSessions; i++) {
            try {
                Session session = createAndConnectSession();
                sessionPool.offer(session);
                lastUsedTimes.put(session, System.currentTimeMillis());
            } catch (IOException e) {
                Timber.tag(TAG).e("Failed to initialize SMB session %d: %s", i + 1, e.getMessage());
                // Continue with available sessions
            }
        }

        keepAliveScheduler.scheduleWithFixedDelay(this::sendKeepAlive, 30, 30, TimeUnit.SECONDS);
    }

    public synchronized Session getSession() throws IOException {
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            try {
                Session session = getOrCreateSession();
                session = validateAndPrepareSession(session);
                return session;
            } catch (IOException e) {
                Timber.tag(TAG).w("SMB connection attempt %d failed: %s", attempt + 1, e.getMessage());
                if (attempt == MAX_RETRIES - 1) {
                    throw e; // Rethrow on last attempt
                }
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while retrying SMB connection", ie);
                }
            } finally {
                attempt++; // Increment attempt after each try-catch block
            }
        }
        throw new IOException("Failed to get a valid SMB session after " + MAX_RETRIES + " attempts");
    }

    private Session getOrCreateSession() throws IOException {
        Session session = sessionPool.poll();
        if (session == null) {
            return createAndConnectSession();
        }
        return session;
    }

    private Session validateAndPrepareSession(Session session) throws IOException {
        if (!isSessionConnected(session)) {
            session = reconnectSession(session);
        }
        lastUsedTimes.put(session, System.currentTimeMillis());
        inUseSessions.offer(session);
        return session;
    }

    public boolean isSessionConnected(Session session) {
        return session != null && session.getConnection() != null && session.getConnection().isConnected();
    }

    private Session createAndConnectSession() throws IOException {
        Connection connection;
        if (networkAccountPOJO.port > 0) {
            connection = smbClient.connect(networkAccountPOJO.host, networkAccountPOJO.port);
        } else {
            connection = smbClient.connect(networkAccountPOJO.host);
        }
        AuthenticationContext ac = getAuthenticationContext();
        return connection.authenticate(ac);
    }

    private Session reconnectSession(Session session) throws IOException {
        disconnectAndCloseSession(session);
        return createAndConnectSession();
    }

    private AuthenticationContext getAuthenticationContext() {
        if (networkAccountPOJO.anonymous) {
            return AuthenticationContext.anonymous();
        } else {
            String domain = networkAccountPOJO.domain != null ? networkAccountPOJO.domain : "";
            char[] passwordChars = networkAccountPOJO.password != null ? networkAccountPOJO.password.toCharArray() : new char[0];
            return new AuthenticationContext(networkAccountPOJO.user_name, passwordChars, domain);
        }
    }

    private void disconnectAndCloseSession(Session session) {
        try {
            if (session != null) {
                Connection connection = session.getConnection();
                if (connection != null && connection.isConnected()) {
                    connection.close();
                }
            }
        } catch (Exception e) {
            Timber.tag(TAG).e("Error disconnecting SMB session: %s", e.getMessage());
        } finally {
            lastUsedTimes.remove(session);
        }
    }

    public synchronized void releaseSession(Session session) {
        inUseSessions.remove(session);
        if (sessionPool.size() < initialSessions) {
            sessionPool.offer(session);
        } else {
            disconnectAndCloseSession(session);
        }
    }

    private void sendKeepAlive() {
        for (Session session : sessionPool) {
            if (inUseSessions.contains(session)) {
                // Skip sessions that are currently in use
                continue;
            }
            try {
                if (isSessionConnected(session)) {
                    // Perform a minimal operation to keep the session alive
                    if (shareName != null && !shareName.isEmpty()) {
                        try (DiskShare share = (DiskShare) session.connectShare(shareName)) {
                            // Perform a minimal operation, such as checking if the root folder exists
                            share.folderExists("");
                            Timber.tag(TAG).d("Performed keep-alive operation on session: %s", session);
                        } catch (Exception e) {
                            Timber.tag(TAG).e("Failed to perform keep-alive operation: %s", e.getMessage());
                            disconnectAndCloseSession(session);
                            sessionPool.remove(session);
                        }
                    } else {
                        // If shareName is not provided, we can perform a minimal operation that doesn't require a share
                        // For example, we can attempt to list the shares available on the server
                        Timber.tag(TAG).d("No shareName provided, skipping keep-alive operation that requires a share");
                    }
                }
            } catch (Exception e) {
                Timber.tag(TAG).e("Failed to send keep-alive: %s", e.getMessage());
                disconnectAndCloseSession(session);
                sessionPool.remove(session); // Remove invalid session from pool
            }
        }
    }

    public void shutdown() {
        Timber.tag(TAG).d("Shutting down SMB client repository");
        keepAliveScheduler.shutdown();
        try {
            if (!keepAliveScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                keepAliveScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            keepAliveScheduler.shutdownNow();
        }

        // Perform necessary cleanup similar to FtpClientRepository
        // For example, remove SMB entries from storage_dir, etc.

        RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
        Iterator<FilePOJO> iterator = repositoryClass.storage_dir.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getFileObjectType() == FileObjectType.SMB_TYPE) {
                iterator.remove();
            }
        }

        Iterator<FilePOJO> iterator1 = MainActivity.RECENT.iterator();
        while (iterator1.hasNext()) {
            if (iterator1.next().getFileObjectType() == FileObjectType.SMB_TYPE) {
                iterator1.remove();
            }
        }

        Iterator<FilePOJO> iterator2 = FileSelectorActivity.RECENT.iterator();
        while (iterator2.hasNext()) {
            if (iterator2.next().getFileObjectType() == FileObjectType.SMB_TYPE) {
                iterator2.remove();
            }
        }

        Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_REFRESH_STORAGE_DIR_ACTION, LocalBroadcastManager.getInstance(App.getAppContext()), "");

        FilePOJOUtil.REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL(Collections.singletonList(""), FileObjectType.SMB_TYPE);
        Global.DELETE_DIRECTORY_ASYNCHRONOUSLY(Global.SMB_CACHE_DIR);

        for (Session session : sessionPool) {
            disconnectAndCloseSession(session);
        }
        for (Session session : inUseSessions) {
            disconnectAndCloseSession(session);
        }
        sessionPool.clear();
        inUseSessions.clear();
        lastUsedTimes.clear();
        networkAccountPOJO = null;
        NetworkAccountDetailsViewModel.SMB_NETWORK_ACCOUNT_POJO = null;
        instance = null;
    }

    public boolean testSmbServerConnection() {
        Session session = null;
        try {
            session = getSession();
            if (isSessionConnected(session)) {
                if (shareName != null && !shareName.isEmpty()) {
                    try (DiskShare share = (DiskShare) session.connectShare(shareName)) {
                        // Try to access root directory
                        return share.folderExists("");
                    } catch (SMBApiException e) {
                        Timber.tag(TAG).e("Failed to access share: %s", e.getMessage());
                        return false;
                    }
                } else {
                    // If shareName is not provided, test the connection by listing available shares
                    Timber.tag(TAG).d("No shareName provided, testing connection by listing shares");
                    try {
                        List<String> shares = listShares(session);
                        return !shares.isEmpty();
                    } catch (Exception e) {
                        Timber.tag(TAG).e("Failed to list shares: %s", e.getMessage());
                        return false;
                    }
                }
            } else {
                return false;
            }
        } catch (IOException e) {
            Timber.tag(TAG).e("Failed to test SMB connection: %s", e.getMessage());
            return false;
        } finally {
            if (session != null) {
                releaseSession(session);
            }
        }
    }

    public void cleanIdleSessions() {
        long currentTime = System.currentTimeMillis();
        Iterator<Session> iterator = sessionPool.iterator();
        while (iterator.hasNext()) {
            Session session = iterator.next();
            Long lastUsedTime = lastUsedTimes.get(session);
            if (lastUsedTime == null || (currentTime - lastUsedTime > IDLE_TIMEOUT)) {
                iterator.remove();
                disconnectAndCloseSession(session);
            }
        }
    }

    // Added method to get shareName
    public String getShareName() {
        return shareName;
    }

    // Placeholder method to list available shares
    public List<String> listShares(Session session) throws IOException {
        // SMBJ doesn't provide a direct method to list shares.
        // You might need to use another library like JCIFS or implement NetShareEnum.
        // For now, we'll return an empty list.
        return new ArrayList<>();
    }
}
