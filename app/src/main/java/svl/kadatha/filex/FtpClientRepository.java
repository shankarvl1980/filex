package svl.kadatha.filex;

import android.os.Build;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class FtpClientRepository {
    private static FtpClientRepository instance;
    private final ConcurrentLinkedQueue<FTPClient> ftpClients;
    private final ConcurrentLinkedQueue<FTPClient> inUseClients;
    private final Map<FTPClient, Long> lastUsedTimes;
    private NetworkAccountsDetailsDialog.NetworkAccountPOJO networkAccountPOJO;
    private static final long IDLE_TIMEOUT = 180000; // 3 minutes
    private static final int MAX_IDLE_CONNECTIONS = 5;
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 1000; // 1 second delay between retries
    private int initialClients;
    private final ScheduledExecutorService keepAliveScheduler = Executors.newScheduledThreadPool(1);
    private static final String TAG = "Ftp-ftpClientRepository";

    private FtpClientRepository(NetworkAccountsDetailsDialog.NetworkAccountPOJO networkAccountPOJO) {
        this.ftpClients = new ConcurrentLinkedQueue<>();
        this.inUseClients = new ConcurrentLinkedQueue<>();
        this.lastUsedTimes = new ConcurrentHashMap<>();
        this.networkAccountPOJO = networkAccountPOJO;
        initializeClients();
    }

    private void initializeClients() {
        initialClients = Math.min(MAX_IDLE_CONNECTIONS, 5);
        // Initialize the first client
        try {
            FTPClient client = createAndConnectFtpClient();
            ftpClients.offer(client);
            lastUsedTimes.put(client, System.currentTimeMillis());
        } catch (IOException e) {
            Timber.tag(TAG).e("Failed to initialize the first FTP client: %s", e.getMessage());
            throw new RuntimeException("Failed to initialize the first FTP client.", e);
        }

        // Initialize additional clients
        for (int i = 1; i < initialClients; i++) {
            try {
                FTPClient client = createAndConnectFtpClient();
                ftpClients.offer(client);
                lastUsedTimes.put(client, System.currentTimeMillis());
            } catch (IOException e) {
                Timber.tag(TAG).e("Failed to initialize FTP client %d: %s", i + 1, e.getMessage());
                // Continue with available clients
            }
        }
        keepAliveScheduler.scheduleWithFixedDelay(this::sendKeepAlive, 30, 30, TimeUnit.SECONDS);
    }

    public static synchronized FtpClientRepository getInstance(NetworkAccountsDetailsDialog.NetworkAccountPOJO networkAccountPOJO) {
        if (instance == null) {
            instance = new FtpClientRepository(networkAccountPOJO);
        }
        return instance;
    }

    public synchronized FTPClient getFtpClient() throws IOException {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                FTPClient client = getOrCreateClient();
                if (validateAndPrepareClient(client)) {
                    return client;
                }
            } catch (IOException e) {
                Timber.tag(TAG).w("FTP connection attempt %d failed: %s", attempt + 1, e.getMessage());
                if (attempt == MAX_RETRIES - 1) {
                    throw e; // Rethrow on last attempt
                }
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while retrying FTP connection", ie);
                }
            }
        }
        throw new IOException("Failed to get a valid FTP client after " + MAX_RETRIES + " attempts");
    }

    private FTPClient getOrCreateClient() throws IOException {
        FTPClient client = ftpClients.poll();
        if (client == null) {
            return createAndConnectFtpClient();
        }
        return client;
    }

    private boolean validateAndPrepareClient(FTPClient client) throws IOException {
        if (!client.isConnected()) {
            reconnectClient(client);
        }

        if (!testConnection(client)) {
            disconnectAndCloseClient(client);
            client = createAndConnectFtpClient();
        }

        // Ensure we're in the correct mode and have the right settings
        client.setFileType(FTP.BINARY_FILE_TYPE);
        if (networkAccountPOJO.mode.equals("active")) {
            client.enterLocalActiveMode();
        } else {
            client.enterLocalPassiveMode();
        }

        lastUsedTimes.put(client, System.currentTimeMillis());
        inUseClients.offer(client);
        return true;
    }

    private FTPClient createAndConnectFtpClient() throws IOException {
        FTPClient client = new FTPClient();
        client.setConnectTimeout(5000);
        client.connect(networkAccountPOJO.host, networkAccountPOJO.port);

        if (!FTPReply.isPositiveCompletion(client.getReplyCode())) {
            throw new IOException("Failed to connect to the FTP server. Reply code: " + client.getReplyCode());
        }

        // Set Control Keep Alive Timeout
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            client.setControlKeepAliveTimeout(Duration.ofSeconds(10));
        } else {
            client.setControlKeepAliveTimeout(10);
        }

        // Set Control Keep Alive Reply Timeout
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            client.setControlKeepAliveReplyTimeout(Duration.ofMillis(500));
        } else {
            client.setControlKeepAliveReplyTimeout(500);
        }

        // Handle Login: Anonymous or Authenticated
        boolean loginSuccess;
        if (networkAccountPOJO.anonymous) {
            // Anonymous Login
            String anonymousPassword = networkAccountPOJO.password != null && !networkAccountPOJO.password.isEmpty()
                    ? networkAccountPOJO.password
                    : "anonymous@"; // Default anonymous password

            loginSuccess = client.login("anonymous", anonymousPassword);
        } else {
            // Authenticated Login
            loginSuccess = client.login(networkAccountPOJO.user_name, networkAccountPOJO.password);
        }

        if (!loginSuccess) {
            int replyCode = client.getReplyCode();
            String replyString = client.getReplyString();
            throw new IOException("Failed to log in to the FTP server. Reply code: " + replyCode + ", Reply string: " + replyString);
        }

        // Set File Type to Binary
        client.setFileType(FTP.BINARY_FILE_TYPE);

        if (networkAccountPOJO.mode.equals("active")) {
            client.enterLocalActiveMode();
        } else {
            client.enterLocalPassiveMode();
        }
        return client;
    }


    private void disconnectAndCloseClient(FTPClient client) {
        try {
            if (client.isConnected()) {
                client.disconnect();
            }
        } catch (IOException e) {
            Timber.tag(TAG).e("Error disconnecting FTP client: %s", e.getMessage());
        } finally {
            lastUsedTimes.remove(client);
        }
    }

    private void reconnectClient(FTPClient client) throws IOException {
        disconnectAndCloseClient(client);

        client.connect(networkAccountPOJO.host, networkAccountPOJO.port);

        if (!FTPReply.isPositiveCompletion(client.getReplyCode())) {
            throw new IOException("Failed to connect to the FTP server. Reply code: " + client.getReplyCode());
        }

        // Handle Login: Anonymous or Authenticated
        boolean loginSuccess;
        if (networkAccountPOJO.anonymous) {
            // Anonymous Login
            String anonymousPassword = networkAccountPOJO.password != null && !networkAccountPOJO.password.isEmpty()
                    ? networkAccountPOJO.password
                    : "anonymous@"; // Default anonymous password

            loginSuccess = client.login("anonymous", anonymousPassword);
        } else {
            // Authenticated Login
            loginSuccess = client.login(networkAccountPOJO.user_name, networkAccountPOJO.password);
        }

        if (!loginSuccess) {
            throw new IOException("Failed to log in to the FTP server. Reply code: " + client.getReplyCode() + ", Reply string: " + client.getReplyString());
        }

        // Reapply FTP Settings
        client.setFileType(FTP.BINARY_FILE_TYPE);
        if (networkAccountPOJO.mode.equals("active")) {
            client.enterLocalActiveMode();
        } else {
            client.enterLocalPassiveMode();
        }
    }


    public synchronized void releaseFtpClient(FTPClient client) {
        inUseClients.remove(client);
        if (ftpClients.size() < initialClients) {
            ftpClients.offer(client);
        } else {
            disconnectAndCloseClient(client);
        }
    }

    private void sendKeepAlive() {
        for (FTPClient client : ftpClients) {
            if (inUseClients.contains(client)) {
                // Skip clients that are currently in use
                continue;
            }
            try {
                if (client.isConnected()) {
                    client.sendNoOp(); // Send NOOP to keep connection alive
                    Timber.tag(TAG).d("Sent NOOP to keep connection alive for client: %s", client);
                }
            } catch (IOException e) {
                Timber.tag(TAG).e("Failed to send NOOP: %s", e.getMessage());
                disconnectAndCloseClient(client);
                ftpClients.remove(client); // Remove invalid client from pool
            }
        }
    }


    private boolean isClientValid(FTPClient client) {
        if (!client.isConnected()) {
            return false;
        }

        Long lastUsedTime = lastUsedTimes.get(client);
        if (lastUsedTime == null) {
            return false;
        }

        return (System.currentTimeMillis() - lastUsedTime < IDLE_TIMEOUT);
    }

    public void shutdown() {
        Timber.tag(TAG).d("Shutting down FTP client repository");
        keepAliveScheduler.shutdown();
        try {
            if (!keepAliveScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                keepAliveScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            keepAliveScheduler.shutdownNow();
        }

        RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
        Iterator<FilePOJO> iterator = repositoryClass.storage_dir.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getFileObjectType() == FileObjectType.FTP_TYPE) {
                iterator.remove();
            }
        }

        Iterator<FilePOJO> iterator1 = MainActivity.RECENT.iterator();
        while (iterator1.hasNext()) {
            if (iterator1.next().getFileObjectType() == FileObjectType.FTP_TYPE) {
                iterator1.remove();
            }
        }

        Iterator<FilePOJO> iterator2 = FileSelectorActivity.RECENT.iterator();
        while (iterator2.hasNext()) {
            if (iterator2.next().getFileObjectType() == FileObjectType.FTP_TYPE) {
                iterator2.remove();
            }
        }

        Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_REFRESH_STORAGE_DIR_ACTION, LocalBroadcastManager.getInstance(App.getAppContext()),"");

        FilePOJOUtil.REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL(Collections.singletonList(""), FileObjectType.FTP_TYPE);
        Global.DELETE_DIRECTORY_ASYNCHRONOUSLY(Global.FTP_CACHE_DIR);

        for (FTPClient client : ftpClients) {
            disconnectAndCloseClient(client);
        }
        for (FTPClient client : inUseClients) {
            disconnectAndCloseClient(client);
        }
        ftpClients.clear();
        inUseClients.clear();
        lastUsedTimes.clear();
        networkAccountPOJO=null;
        NetworkAccountDetailsViewModel.FTP_NETWORK_ACCOUNT_POJO=null;
        instance=null;
    }

    public boolean testConnection(FTPClient client) {
        if (client == null || !client.isConnected()) {
            return false;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                client.setControlKeepAliveTimeout(Duration.ofSeconds(10));
            }else{
                client.setControlKeepAliveTimeout(10);
            }

            if (!client.sendNoOp()) {
                return false;
            }
            return client.changeWorkingDirectory("/");
        } catch (IOException e) {
            return false;
        }
    }

    public boolean testFtpServerConnection() {
        FTPClient client = null;
        try {
            client = getFtpClient();
            return testConnection(client);
        } catch (IOException e) {
            return false;
        } finally {
            if (client != null) {
                releaseFtpClient(client);
            }
        }
    }

    public void cleanIdleConnections() {
        long currentTime = System.currentTimeMillis();
        Iterator<FTPClient> iterator = ftpClients.iterator();
        while (iterator.hasNext()) {
            FTPClient client = iterator.next();
            Long lastUsedTime = lastUsedTimes.get(client);
            if (lastUsedTime == null || (currentTime - lastUsedTime > IDLE_TIMEOUT)) {
                iterator.remove();
                disconnectAndCloseClient(client);
            }
        }
    }
}