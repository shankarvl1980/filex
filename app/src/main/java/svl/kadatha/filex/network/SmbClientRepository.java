package svl.kadatha.filex.network;

import com.hierynomus.mssmb2.SMB2Dialect;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.SmbConfig;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class SmbClientRepository {
    private static final String TAG = "Smb-SmbClientRepository";
    // Pool sizing for short operations (not streaming)
    private static final int DEFAULT_POOL_SIZE = 2;      // keep small
    private static final int MAX_POOL_SIZE = 4;          // don’t explode connections
    private static final int MAX_RETRIES = 2;            // retry once on dead socket
    private static final long PROBE_IDLE_MS = 20_000;    // probe session if idle > 20s

    private static SmbClientRepository instance;

    private final SMBClient smbClient;
    private final ConcurrentLinkedQueue<Session> sessionPool = new ConcurrentLinkedQueue<>();
    private final Map<Session, Long> lastUsedTimes = new ConcurrentHashMap<>();

    private final NetworkAccountPOJO networkAccountPOJO;
    private final String shareName;

    private SmbClientRepository(NetworkAccountPOJO pojo) {
        this.networkAccountPOJO = pojo;

        // ---- shareName MUST be a share, NOT "/" ----
        String sn = (pojo.shareName == null) ? "" : pojo.shareName.trim();
        this.shareName = sn;

        SmbConfig config = buildConfigFromPojo(pojo);
        this.smbClient = new SMBClient(config);

        // pre-warm a couple of sessions for small ops
        warmPool();
    }

    public static synchronized SmbClientRepository getInstance(NetworkAccountPOJO pojo) {
        if (instance == null) instance = new SmbClientRepository(pojo);
        return instance;
    }

    // ----------------------------------------------------------------------
    // Public API you should use everywhere
    // ----------------------------------------------------------------------

    /** Handle for share + session. Always close() it (or call releaseShare). */
    public static final class ShareHandle implements AutoCloseable {
        public final Session session;
        public final DiskShare share;

        // pooled=false means dedicated session for streaming
        private final boolean pooled;
        private final SmbClientRepository repo;
        private boolean closed;

        private ShareHandle(SmbClientRepository repo, Session session, DiskShare share, boolean pooled) {
            this.repo = repo;
            this.session = session;
            this.share = share;
            this.pooled = pooled;
        }

        @Override
        public void close() {
            repo.releaseShare(this);
        }
    }

    /**
     * For SHORT operations: list/exists/mkdir/delete/rename etc.
     * Uses pooled session and internally heals broken sockets.
     */
    public ShareHandle acquireShare() throws IOException {
        ensureShareNameValid();

        IOException last = null;
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            Session s = null;
            try {
                s = getPooledSession();
                s = validateSessionMaybeProbe(s);

                DiskShare sh = (DiskShare) s.connectShare(shareName);
                markUsed(s);
                return new ShareHandle(this, s, sh, true);

            } catch (Exception e) {
                last = asIo(e, "acquireShare failed");
                // pooled session is suspect, discard it
                safeDisconnectSession(s);
            }
        }
        throw last != null ? last : new IOException("acquireShare failed");
    }

    /**
     * For STREAMING operations: InputStream/OutputStream.
     * Always creates a FRESH session so the stream owns its socket.
     */
    public ShareHandle acquireShareForStreaming() throws IOException {
        ensureShareNameValid();

        IOException last = null;
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            Session s = null;
            try {
                s = createFreshSession();
                DiskShare sh = (DiskShare) s.connectShare(shareName);
                markUsed(s);
                return new ShareHandle(this, s, sh, false);
            } catch (Exception e) {
                last = asIo(e, "acquireShareForStreaming failed");
                safeDisconnectSession(s);
            }
        }
        throw last != null ? last : new IOException("acquireShareForStreaming failed");
    }

    /** Release handle. Prefer h.close() via try-with-resources. */
    public void releaseShare(ShareHandle h) {
        if (h == null || h.closed) return;
        h.closed = true;

        try { if (h.share != null) h.share.close(); } catch (Exception ignored) {}

        if (h.pooled) {
            // return session to pool if still connected
            if (isSessionConnected(h.session) && sessionPool.size() < MAX_POOL_SIZE) {
                sessionPool.offer(h.session);
                markUsed(h.session);
            } else {
                safeDisconnectSession(h.session);
            }
        } else {
            // streaming = always close session
            safeDisconnectSession(h.session);
        }
    }

    // ----------------------------------------------------------------------
    // Optional: “connect test” for your login screen
    // ----------------------------------------------------------------------

    /** Cheap connection test (uses pooled acquire). */
    public boolean testConnection() {
        ShareHandle h = null;
        try {
            h = acquireShare();
            // minimal call
            return h.share.folderExists("");
        } catch (Exception e) {
            Timber.tag(TAG).w(e, "testConnection failed");
            return false;
        } finally {
            if (h != null) h.close();
        }
    }

    // ----------------------------------------------------------------------
    // Shutdown
    // ----------------------------------------------------------------------

    public synchronized void shutdown() {
        // close pooled sessions
        Session s;
        while ((s = sessionPool.poll()) != null) {
            safeDisconnectSession(s);
        }
        lastUsedTimes.clear();

        try { smbClient.close(); } catch (Exception ignored) {}
        instance = null;
    }

    // ----------------------------------------------------------------------
    // Internals
    // ----------------------------------------------------------------------

    private void ensureShareNameValid() throws IOException {
        if (shareName == null || shareName.isEmpty() || "/".equals(shareName)) {
            throw new IOException("Invalid shareName. Must be a real SMB share (e.g. 'Audit-I'), not '/'");
        }
    }

    private void warmPool() {
        int count = Math.min(DEFAULT_POOL_SIZE, MAX_POOL_SIZE);
        for (int i = 0; i < count; i++) {
            try {
                Session s = createFreshSession();
                sessionPool.offer(s);
                markUsed(s);
            } catch (Exception e) {
                Timber.tag(TAG).w(e, "warmPool session %d failed", i);
            }
        }
    }

    private Session getPooledSession() throws IOException {
        Session s = sessionPool.poll();
        if (s != null) return s;
        return createFreshSession();
    }

    private Session createFreshSession() throws IOException {
        Connection c;
        if (networkAccountPOJO.port > 0) c = smbClient.connect(networkAccountPOJO.host, networkAccountPOJO.port);
        else c = smbClient.connect(networkAccountPOJO.host);

        AuthenticationContext ac = getAuthenticationContext();
        return c.authenticate(ac);
    }

    private AuthenticationContext getAuthenticationContext() {
        if (networkAccountPOJO.anonymous) {
            return AuthenticationContext.anonymous();
        }
        String domain = networkAccountPOJO.domain != null ? networkAccountPOJO.domain : "";
        char[] pw = networkAccountPOJO.password != null ? networkAccountPOJO.password.toCharArray() : new char[0];
        return new AuthenticationContext(networkAccountPOJO.user_name, pw, domain);
    }

    private boolean isSessionConnected(Session s) {
        try {
            return s != null && s.getConnection() != null && s.getConnection().isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    private Session validateSessionMaybeProbe(Session s) throws IOException {
        if (!isSessionConnected(s)) {
            safeDisconnectSession(s);
            return createFreshSession();
        }

        Long last = lastUsedTimes.get(s);
        long now = System.currentTimeMillis();
        boolean shouldProbe = (last == null) || (now - last > PROBE_IDLE_MS);
        if (!shouldProbe) return s;

        // Probe = connectShare + folderExists + close share.
        // This catches half-dead sockets after idle/background/network change.
        try (DiskShare sh = (DiskShare) s.connectShare(shareName)) {
            sh.folderExists("");
        } catch (Exception e) {
            safeDisconnectSession(s);
            return createFreshSession();
        }

        return s;
    }

    public boolean testSmbServerConnection() {
        ShareHandle h = null;
        try {
            // Use non-streaming (pooled) if you have it; otherwise use streaming too.
            // Prefer pooled:
            h = acquireShare();  // if you kept acquireShare()
            // If you removed acquireShare(), replace with:
            // h = acquireShareForStreaming();

            DiskShare share = h.share;

            // Minimal probe that actually touches server
            return share.folderExists("");   // root of the share
        } catch (Exception e) {
            Timber.tag(TAG).w(e, "SMB test connection failed");
            return false;
        } finally {
            releaseShare(h);
        }
    }

    private void markUsed(Session s) {
        if (s != null) lastUsedTimes.put(s, System.currentTimeMillis());
    }

    private void safeDisconnectSession(Session s) {
        if (s == null) return;
        lastUsedTimes.remove(s);

        try { s.close(); } catch (Exception ignored) {}
        try {
            Connection c = s.getConnection();
            if (c != null) {
                try { c.close(); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    private IOException asIo(Exception e, String msg) {
        if (e instanceof IOException) return (IOException) e;

        // keep the socket error visible
        Throwable cause = e.getCause();
        if (cause instanceof SocketException) {
            return new IOException(msg + " (socket): " + cause.getMessage(), e);
        }
        return new IOException(msg, e);
    }

    // Your SMB version mapping, kept here
    private static SmbConfig buildConfigFromPojo(NetworkAccountPOJO pojo) {
        SmbConfig.Builder b = SmbConfig.builder()
                .withTimeout(60, TimeUnit.SECONDS)
                .withSoTimeout(60, TimeUnit.SECONDS);

        if (pojo.smbVersion != null && !pojo.smbVersion.trim().isEmpty()) {
            List<SMB2Dialect> dialects = getDialectsFromVersion(pojo.smbVersion.trim());
            if (!dialects.isEmpty()) {
                b.withDialects(dialects.toArray(new SMB2Dialect[0]));
            }
        }
        return b.build();
    }

    private static List<SMB2Dialect> getDialectsFromVersion(String smbVersion) {
        List<SMB2Dialect> dialects = new ArrayList<>();
        switch (smbVersion) {
            case "SMB2":
                dialects.add(SMB2Dialect.SMB_2_0_2);
                dialects.add(SMB2Dialect.SMB_2_1);
                break;
            case "SMB3":
                dialects.add(SMB2Dialect.SMB_3_0);
                dialects.add(SMB2Dialect.SMB_3_0_2);
                dialects.add(SMB2Dialect.SMB_3_1_1);
                break;
            case "SMB1":
                // SMBJ doesn’t support SMB1
                break;
            default:
                dialects.addAll(Arrays.asList(SMB2Dialect.values()));
                break;
        }
        return dialects;
    }

    // Convenience for callers that need sanitized paths
    public static String stripLeadingSlash(String p) {
        if (p == null) return "";
        return p.startsWith("/") ? p.substring(1) : p;
    }

    public String getShareName() {
        return shareName;
    }
}
