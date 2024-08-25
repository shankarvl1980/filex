package svl.kadatha.filex;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Timer;
import java.util.concurrent.ConcurrentLinkedQueue;



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
    private int initialClients;

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
                // Log error, but continue
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
        FTPClient client = ftpClients.poll();
        if (client == null || !isClientValid(client)) {
            if (client != null) {
                disconnectAndCloseClient(client);
            }
            client = createAndConnectFtpClient();
        } else {
            // Warm-up check
            try {
                client.sendNoOp();
            } catch (IOException e) {
                disconnectAndCloseClient(client);
                client = createAndConnectFtpClient();
            }
        }
        lastUsedTimes.put(client, System.currentTimeMillis());
        inUseClients.offer(client);
        return client;
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
            // Log the exception
        } finally {
            lastUsedTimes.remove(client);
        }
    }

    public void shutdown() {
        Timber.tag(Global.TAG).d("Shutting down FTP client repository");
        RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
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
            if (!client.sendNoOp()) {
                return false;
            }
            boolean changed = client.changeWorkingDirectory("/");
            return changed;
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