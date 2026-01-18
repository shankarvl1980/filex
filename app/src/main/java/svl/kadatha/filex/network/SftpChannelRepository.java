package svl.kadatha.filex.network;

import android.os.Bundle;

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

public class SftpChannelRepository {
    private static final int MAX_IDLE_CONNECTIONS = 5;
    private static final long IDLE_TIMEOUT = 180000; // 3 minutes
    private static final String TAG = "Sftp-channel";
    private static SftpChannelRepository instance;
    private final ConcurrentLinkedQueue<ChannelSftp> sftpChannels;
    private final ConcurrentLinkedQueue<ChannelSftp> inUseChannels;
    private final Map<ChannelSftp, Long> lastUsedTimes;
    private final ScheduledExecutorService keepAliveScheduler;
    private NetworkAccountPOJO networkAccountPOJO;
    private int initialChannels;

    private SftpChannelRepository(NetworkAccountPOJO networkAccountPOJO) {
        this.sftpChannels = new ConcurrentLinkedQueue<>();
        this.inUseChannels = new ConcurrentLinkedQueue<>();
        this.lastUsedTimes = new ConcurrentHashMap<>();
        this.keepAliveScheduler = Executors.newScheduledThreadPool(1);
        this.networkAccountPOJO = networkAccountPOJO;
        initializeChannels();
    }

    public static synchronized SftpChannelRepository getInstance(NetworkAccountPOJO networkAccountPOJO) {
        if (instance == null) {
            instance = new SftpChannelRepository(networkAccountPOJO);
        }
        return instance;
    }

    private void initializeChannels() {
        initialChannels = Math.min(MAX_IDLE_CONNECTIONS, 4);
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
        ChannelSftp ch = sftpChannels.poll();

        if (!isAlive(ch)) {
            disconnectAndCloseChannel(ch);
            ch = createAndConnectSftpChannel();
        }

        inUseChannels.offer(ch);
        lastUsedTimes.put(ch, System.currentTimeMillis());
        return ch;
    }

    private boolean isAlive(ChannelSftp ch) {
        if (ch == null) return false;
        if (!ch.isConnected()) return false;

        try {
            Session s = ch.getSession();
            if (s == null || !s.isConnected()) return false;

            // cheap liveness probe (optional but robust)
            s.sendKeepAliveMsg();
            return true;
        } catch (Exception e) {
            return false;
        }
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
        Session session = jsch.getSession(
                networkAccountPOJO.user_name,
                networkAccountPOJO.host,
                networkAccountPOJO.port
        );

        if (!networkAccountPOJO.privateKeyPath.isEmpty()) {
            jsch.addIdentity(networkAccountPOJO.privateKeyPath, networkAccountPOJO.privateKeyPassphrase);
        } else {
            session.setPassword(networkAccountPOJO.password);
        }


        if (!networkAccountPOJO.knownHostsPath.isEmpty()) {
            jsch.setKnownHosts(networkAccountPOJO.knownHostsPath);
        } else {
            session.setConfig("StrictHostKeyChecking", "no");
        }

        session.setTimeout(15_000);
        session.setServerAliveInterval(30_000);
        session.setServerAliveCountMax(2);

        session.connect(15_000);

        ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
        channelSftp.connect(15_000);

        return channelSftp;
    }


    private void sendKeepAlive() {
        for (ChannelSftp channel : sftpChannels) {
            if (inUseChannels.contains(channel)) continue;

            try {
                Session s = channel.getSession();
                if (s != null && s.isConnected()) {
                    s.sendKeepAliveMsg();
                    lastUsedTimes.put(channel, System.currentTimeMillis()); // ✅ refresh
                } else {
                    throw new Exception("Session not connected");
                }
            } catch (Exception e) {
                disconnectAndCloseChannel(channel);
                sftpChannels.remove(channel);
            }
        }
    }


    private void disconnectAndCloseChannel(ChannelSftp channel) {
        if (channel == null) return;

        try {
            try {
                if (channel.isConnected()) channel.disconnect();
            } catch (Exception ignored) {}

            try {
                Session session = channel.getSession();
                if (session != null && session.isConnected()) session.disconnect();
            } catch (Exception ignored) {}
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

        for (ChannelSftp channel : sftpChannels) {
            disconnectAndCloseChannel(channel);
        }
        for (ChannelSftp channel : inUseChannels) {
            disconnectAndCloseChannel(channel);
        }
        sftpChannels.clear();
        inUseChannels.clear();
        lastUsedTimes.clear();
        networkAccountPOJO = null;
        NetworkAccountDetailsViewModel.SFTP_NETWORK_ACCOUNT_POJO = null;
        instance = null;
        NetworkAccountDetailsViewModel.clearNetworkFileObjectType(FileObjectType.SFTP_TYPE);
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
            Timber.tag(TAG).e("Error getting SFTP channel: %s", e.getMessage());
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
