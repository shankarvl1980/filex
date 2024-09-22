package svl.kadatha.filex;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class SftpChannelRepository {
    private static SftpChannelRepository instance;
    private final ConcurrentLinkedQueue<ChannelSftp> sftpChannels;
    private final ConcurrentLinkedQueue<ChannelSftp> inUseChannels;
    private final Map<ChannelSftp, Long> lastUsedTimes;
    private final ScheduledExecutorService keepAliveScheduler;
    private NetworkAccountsDetailsDialog.NetworkAccountPOJO networkAccountPOJO;
    private int initialChannels;
    private static final int MAX_IDLE_CONNECTIONS = 5;
    private static final long IDLE_TIMEOUT = 180000; // 3 minutes
    private static final String TAG = "Sftp-sftpchannel";

    private SftpChannelRepository(NetworkAccountsDetailsDialog.NetworkAccountPOJO networkAccountPOJO) {
        this.sftpChannels = new ConcurrentLinkedQueue<>();
        this.inUseChannels = new ConcurrentLinkedQueue<>();
        this.lastUsedTimes = new ConcurrentHashMap<>();
        this.keepAliveScheduler = Executors.newScheduledThreadPool(1);
        this.networkAccountPOJO = networkAccountPOJO;
        initializeChannels();
    }

    public static synchronized SftpChannelRepository getInstance(NetworkAccountsDetailsDialog.NetworkAccountPOJO networkAccountPOJO) {
        if (instance == null) {
            instance = new SftpChannelRepository(networkAccountPOJO);
        }
        return instance;
    }

    private void initializeChannels() {
        initialChannels = Math.min(MAX_IDLE_CONNECTIONS, 5);
        // Initialize the first channel
        try {
            ChannelSftp channel = createAndConnectSftpChannel();
            sftpChannels.offer(channel);
            lastUsedTimes.put(channel, System.currentTimeMillis());
        } catch (JSchException e) {
            Timber.tag(TAG).e("Failed to initialize the first SFTP channel: %s", e.getMessage());
            throw new RuntimeException("Failed to initialize the first FTP channel.", e);
        }
        for (int i = 1; i < initialChannels; i++) {
            try {
                ChannelSftp channel = createAndConnectSftpChannel();
                sftpChannels.offer(channel);
                lastUsedTimes.put(channel, System.currentTimeMillis());
            } catch (JSchException e) {
                // Log error and continue
            }
        }
        keepAliveScheduler.scheduleWithFixedDelay(this::sendKeepAlive, 30, 30, TimeUnit.SECONDS);
    }

    public synchronized ChannelSftp getSftpChannel() throws JSchException {
        ChannelSftp channel = sftpChannels.poll();
        if (channel == null || !channel.isConnected()) {
            channel = createAndConnectSftpChannel();
        }
        inUseChannels.offer(channel);
        lastUsedTimes.put(channel, System.currentTimeMillis());
        return channel;
    }

    public synchronized void releaseChannel(ChannelSftp channel) {
        inUseChannels.remove(channel);
        if (sftpChannels.size() < initialChannels) {
            sftpChannels.offer(channel);
        } else {
            disconnectAndCloseChannel(channel);
        }
    }

    private ChannelSftp createAndConnectSftpChannel() throws JSchException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(networkAccountPOJO.user_name, networkAccountPOJO.server, networkAccountPOJO.port);

        //if (sftpPOJO.useKeyAuth)
        if(!networkAccountPOJO.privateKeyPath.isEmpty())
        {
            jsch.addIdentity(networkAccountPOJO.privateKeyPath, networkAccountPOJO.privateKeyPassphrase);
        } else {
            session.setPassword(networkAccountPOJO.password);
        }

        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
        channelSftp.connect();

        return channelSftp;
    }

    private void sendKeepAlive() {
        for (ChannelSftp channel : sftpChannels) {
            if (!inUseChannels.contains(channel)) {
                try {
                    channel.getSession().sendKeepAliveMsg();
                } catch (Exception e) {
                    // Log error and remove faulty channel
                    disconnectAndCloseChannel(channel);
                    sftpChannels.remove(channel);
                }
            }
        }
    }

    private void disconnectAndCloseChannel(ChannelSftp channel) {
        if (channel.isConnected()) {
            channel.disconnect();
        }

        try {
            Session session = channel.getSession();
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        } catch (JSchException e) {
            // Log the exception
            Timber.tag(TAG).e("Error getting session for channel: %s", e.getMessage());
            // You might want to add more specific error handling here if needed
        } finally {
            lastUsedTimes.remove(channel);
        }
    }

    public void shutdown() {
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
            if (iterator.next().getFileObjectType() == FileObjectType.SFTP_TYPE) {
                iterator.remove();
            }
        }

        Iterator<FilePOJO> iterator1 = MainActivity.RECENT.iterator();
        while (iterator1.hasNext()) {
            if (iterator1.next().getFileObjectType() == FileObjectType.SFTP_TYPE) {
                iterator1.remove();
            }
        }

        Iterator<FilePOJO> iterator2 = FileSelectorActivity.RECENT.iterator();
        while (iterator2.hasNext()) {
            if (iterator2.next().getFileObjectType() == FileObjectType.SFTP_TYPE) {
                iterator2.remove();
            }
        }

        Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_REFRESH_STORAGE_DIR_ACTION, LocalBroadcastManager.getInstance(App.getAppContext()),"");
        FilePOJOUtil.REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL(Collections.singletonList(""), FileObjectType.SFTP_TYPE);
        Global.DELETE_DIRECTORY_ASYNCHRONOUSLY(Global.SFTP_CACHE_DIR);


        for (ChannelSftp channel : sftpChannels) {
            disconnectAndCloseChannel(channel);
        }
        for (ChannelSftp channel : inUseChannels) {
            disconnectAndCloseChannel(channel);
        }
        sftpChannels.clear();
        inUseChannels.clear();
        lastUsedTimes.clear();
    }

    public boolean testConnection(ChannelSftp channel) {
        if (channel == null || !channel.isConnected()) {
            return false;
        }

        try {
            channel.getSession().sendKeepAliveMsg();
            channel.pwd(); // Attempt to get the current working directory
            return true;
        } catch (Exception e) {
            Timber.tag(TAG).e("Error testing connection: %s", e.getMessage());
            return false;
        }
    }

    public boolean testSftpServerConnection() {
        ChannelSftp channel = null;
        try {
            channel = getSftpChannel();
            return testConnection(channel);
        } catch (JSchException e) {
            Timber.tag(TAG).e( "Error getting SFTP channel: %s", e.getMessage());
            return false;
        } finally {
            if (channel != null) {
                releaseChannel(channel);
            }
        }
    }

    public void cleanIdleConnections() {
        long currentTime = System.currentTimeMillis();
        Iterator<ChannelSftp> iterator = sftpChannels.iterator();
        while (iterator.hasNext()) {
            ChannelSftp channel = iterator.next();
            Long lastUsedTime = lastUsedTimes.get(channel);
            if (lastUsedTime == null || (currentTime - lastUsedTime > IDLE_TIMEOUT)) {
                disconnectAndCloseChannel(channel);
                iterator.remove();
            }
        }
    }
}
