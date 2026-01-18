package svl.kadatha.filex.network;

import android.os.Build;
import android.os.Bundle;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;

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

import svl.kadatha.filex.App;
import svl.kadatha.filex.FileObjectType;
import svl.kadatha.filex.FilePOJO;
import svl.kadatha.filex.FilePOJOUtil;
import svl.kadatha.filex.FileSelectorActivity;
import svl.kadatha.filex.Global;
import svl.kadatha.filex.MainActivity;
import svl.kadatha.filex.RepositoryClass;
import timber.log.Timber;

public class FtpClientRepository {
    private static final long IDLE_TIMEOUT = 180000; // 3 minutes
    private static final int MAX_IDLE_CONNECTIONS = 5;
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 1000; // 1 second delay between retries
    private static final String TAG = "Ftp-ftpClientRepository";
    private static FtpClientRepository instance;
    private final ConcurrentLinkedQueue<FTPClient> ftpClients;
    private final ConcurrentLinkedQueue<FTPClient> inUseClients;
    private final Map<FTPClient, Long> lastUsedTimes;
    private final ScheduledExecutorService keepAliveScheduler = Executors.newScheduledThreadPool(1);
    private NetworkAccountPOJO networkAccountPOJO;
    private int initialClients;

    private FtpClientRepository(NetworkAccountPOJO networkAccountPOJO) {
        this.ftpClients = new ConcurrentLinkedQueue<>();
        this.inUseClients = new ConcurrentLinkedQueue<>();
        this.lastUsedTimes = new ConcurrentHashMap<>();
        this.networkAccountPOJO = networkAccountPOJO;
        initializeClients();
    }

    public static synchronized FtpClientRepository getInstance(NetworkAccountPOJO networkAccountPOJO) {
        if (instance == null) {
            instance = new FtpClientRepository(networkAccountPOJO);
        }
        return instance;
    }

    private void initializeClients() {
        initialClients = Math.min(MAX_IDLE_CONNECTIONS, 4);
        // Initialize the first client
        try {
            FTPClient client = createAndConnectFtpClient();
            ftpClients.offer(client);
            lastUsedTimes.put(client, System.currentTimeMillis());
        } catch (IOException e) {
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

    public synchronized FTPClient getFtpClient() throws IOException {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                FTPClient client = getOrCreateClient();
                client = validateAndPrepareClient(client);
                return client;
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

    private FTPClient validateAndPrepareClient(FTPClient client) throws IOException {
        if (!client.isConnected()) {
            client = reconnectClient(client);
        } else {
            try {
                if (!client.sendNoOp()) {
                    client = reconnectClient(client);
                }
            } catch (IOException e) {
                client = reconnectClient(client);
            }
        }
        lastUsedTimes.put(client, System.currentTimeMillis());
        inUseClients.offer(client);
        return client;
    }

    private FTPClient createAndConnectFtpClient() throws IOException {
        FTPClient client;
        if (networkAccountPOJO.useFTPS) {
            // Initialize FTPSClient for FTPS connections
            client = new FTPSClient(false);
        } else {
            // Initialize FTPClient for standard FTP connections
            client = new FTPClient();
        }

        client.setConnectTimeout(5000);
        connectAndLogin(client);
        configureFtpClient(client);

        return client;
    }

    private FTPClient reconnectClient(FTPClient client) throws IOException {
        disconnectAndCloseClient(client);

        // Re-create the FTPClient instance
        if (networkAccountPOJO.useFTPS) {
            client = new FTPSClient(false);
        } else {
            client = new FTPClient();
        }

        client.setConnectTimeout(5000);
        connectAndLogin(client);
        configureFtpClient(client);

        return client;
    }

    private void connectAndLogin(FTPClient client) throws IOException {
        client.connect(networkAccountPOJO.host, networkAccountPOJO.port);

        if (!FTPReply.isPositiveCompletion(client.getReplyCode())) {
            throw new IOException("Failed to connect to the FTP server. Reply code: " + client.getReplyCode());
        }

        // Handle Login: Anonymous or Authenticated
        boolean loginSuccess;
        if (networkAccountPOJO.anonymous) {
            // Anonymous Login
            String anonymousPassword = (networkAccountPOJO.password != null && !networkAccountPOJO.password.isEmpty())
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
    }

    private void configureFtpClient(FTPClient client) throws IOException {
        // Set Control Keep Alive Timeout
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            client.setControlKeepAliveTimeout(java.time.Duration.ofSeconds(30));
            client.setControlKeepAliveReplyTimeout(java.time.Duration.ofSeconds(10));
        } else {
            client.setControlKeepAliveTimeout(30);
            client.setControlKeepAliveReplyTimeout(10_000); // ms
        }


        // If using FTPS, configure additional security settings
        if (networkAccountPOJO.useFTPS && client instanceof FTPSClient) {
            FTPSClient ftps = (FTPSClient) client;
            ftps.execPBSZ(0);
            ftps.execPROT("P"); // Private data connection

            // Optionally, set SSL protocols and cipher suites
            ftps.setEnabledProtocols(new String[]{"TLSv1.2", "TLSv1.3"});
        }

        // Set File Type to Binary
        client.setFileType(FTP.BINARY_FILE_TYPE);

        if (networkAccountPOJO.mode.equals("active")) {
            client.enterLocalActiveMode();
        } else {
            client.enterLocalPassiveMode();
        }
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
            if (inUseClients.contains(client)) continue;

            try {
                if (client.isConnected()) {
                    boolean ok = client.sendNoOp();
                    if (ok) {
                        lastUsedTimes.put(client, System.currentTimeMillis()); // ✅ refresh
                    } else {
                        throw new IOException("NOOP returned false");
                    }
                }
            } catch (IOException e) {
                Timber.tag(TAG).e("KeepAlive failed: %s", e.getMessage());
                disconnectAndCloseClient(client);
                ftpClients.remove(client);
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

        for (FTPClient client : ftpClients) {
            disconnectAndCloseClient(client);
        }
        for (FTPClient client : inUseClients) {
            disconnectAndCloseClient(client);
        }
        ftpClients.clear();
        inUseClients.clear();
        lastUsedTimes.clear();
        networkAccountPOJO = null;
        NetworkAccountDetailsViewModel.FTP_NETWORK_ACCOUNT_POJO = null;
        instance = null;
    }

    public boolean testConnection(FTPClient client) {
        if (client == null || !client.isConnected()) {
            return false;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                client.setControlKeepAliveTimeout(Duration.ofSeconds(10));
            } else {
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
