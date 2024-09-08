package svl.kadatha.filex;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import timber.log.Timber;

public class FtpClientRepository {
    private static FtpClientRepository instance;
    private final ConcurrentLinkedQueue<FTPClient> ftpClients;
    private final ConcurrentLinkedQueue<FTPClient> inUseClients;
    private final Map<FTPClient, Long> lastUsedTimes;
    private final FtpDetailsDialog.FtpPOJO ftpPOJO;
    private static final long IDLE_TIMEOUT = 300000; // 5 minutes
    private static final int MAX_IDLE_CONNECTIONS = 5;
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 1000; // 1 second delay between retries
    private int initialClients;
    private static final String TAG = "Ftp-ftpClientRepository";

    private FtpClientRepository(FtpDetailsDialog.FtpPOJO ftpPOJO) {
        this.ftpClients = new ConcurrentLinkedQueue<>();
        this.inUseClients = new ConcurrentLinkedQueue<>();
        this.lastUsedTimes = new ConcurrentHashMap<>();
        this.ftpPOJO = ftpPOJO;
        initializeClients();
    }

    private void initializeClients() {
        initialClients = Math.min(MAX_IDLE_CONNECTIONS, 5);
        for (int i = 0; i < initialClients; i++) {
            try {
                FTPClient client = createAndConnectFtpClient();
                ftpClients.offer(client);
                lastUsedTimes.put(client, System.currentTimeMillis());
            } catch (IOException e) {
                Timber.tag(TAG).e("Failed to initialize FTP client: %s", e.getMessage());
            }
        }
    }

    public static synchronized FtpClientRepository getInstance(FtpDetailsDialog.FtpPOJO ftpPOJO) {
        if (instance == null) {
            instance = new FtpClientRepository(ftpPOJO);
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
        client.enterLocalPassiveMode();

        lastUsedTimes.put(client, System.currentTimeMillis());
        inUseClients.offer(client);
        return true;
    }

    private void reconnectClient(FTPClient client) throws IOException {
        disconnectAndCloseClient(client);
        client.connect(ftpPOJO.server, ftpPOJO.port);
        if (!FTPReply.isPositiveCompletion(client.getReplyCode())) {
            throw new IOException("Failed to connect to the FTP server");
        }
        if (!client.login(ftpPOJO.user_name, ftpPOJO.password)) {
            throw new IOException("Failed to log in to the FTP server");
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

    private FTPClient createAndConnectFtpClient() throws IOException {
        FTPClient client = new FTPClient();
        client.setConnectTimeout(5000);
        client.connect(ftpPOJO.server, ftpPOJO.port);
        if (!FTPReply.isPositiveCompletion(client.getReplyCode())) {
            throw new IOException("Failed to connect to the FTP server");
        }

        client.setControlKeepAliveTimeout(10);
        client.setControlKeepAliveReplyTimeout(500);

        if (!client.login(ftpPOJO.user_name, ftpPOJO.password)) {
            throw new IOException("Failed to log in to the FTP server");
        }

        client.setFileType(FTP.BINARY_FILE_TYPE);
        client.enterLocalPassiveMode();

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

    public void shutdown() {
        Timber.tag(TAG).d("Shutting down FTP client repository");
        RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
        Iterator<FilePOJO> iterator = repositoryClass.storage_dir.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getFileObjectType() == FileObjectType.FTP_TYPE) {
                iterator.remove();
            }
        }

        Iterator<FilePOJO> iterator1 = MainActivity.RECENTS.iterator();
        while (iterator1.hasNext()) {
            if (iterator1.next().getFileObjectType() == FileObjectType.FTP_TYPE) {
                iterator1.remove();
            }
        }

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
    }

    public boolean testConnection(FTPClient client) {
        if (client == null || !client.isConnected()) {
            return false;
        }

        try {
            client.setControlKeepAliveTimeout(10);
            if (!client.sendNoOp()) {
                return false;
            }
            return client.changeWorkingDirectory("/");
        } catch (IOException e) {
            return false;
        }
    }

    public boolean testServerConnection() {
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