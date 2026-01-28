package svl.kadatha.filex.network;

import com.thegrizzlylabs.sardineandroid.DavResource;
import com.thegrizzlylabs.sardineandroid.Sardine;
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import svl.kadatha.filex.FileObjectType;
import svl.kadatha.filex.NavSessionStore;
import timber.log.Timber;

public class WebDavClientRepository {
    private static final String TAG = "WebDavClientRepository";
    private static WebDavClientRepository instance;

    public final String baseUrl;
    private final Sardine sardine;
    private NetworkAccountPOJO networkAccountPOJO;
    private String discoveredBasePath;
    private OkHttpClient okHttpClient;

    private WebDavClientRepository(NetworkAccountPOJO networkAccountPOJO) throws IOException {
        Timber.tag(TAG).d("Entering WebDavClientRepository constructor");
        this.networkAccountPOJO = networkAccountPOJO;

        this.baseUrl = buildBaseUrl();
        Timber.tag(TAG).d("Base URL built: %s", this.baseUrl);

        this.sardine = createAndConfigureSardineClient();
        if (this.sardine == null) throw new IOException("Failed to create Sardine client");

        if (!testWebDavServerConnection()) {
            throw new IOException("Unable to connect to the WebDAV server with the provided credentials.");
        }

        discoveredBasePath = discoverBasePath(sardine);
    }

    public static synchronized WebDavClientRepository getInstance(NetworkAccountPOJO networkAccountPOJO) throws IOException {
        if (instance == null) instance = new WebDavClientRepository(networkAccountPOJO);
        return instance;
    }

    /**
     * KEY FIXES:
     * - Preemptive Authorization header (Interceptor)
     * - followRedirects(false) + retryOnConnectionFailure(false) to avoid replaying a streaming body
     * - sane timeouts for big uploads
     */
    private Sardine createAndConfigureSardineClient() {
        try {
            final String credential = Credentials.basic(
                    networkAccountPOJO.user_name,
                    networkAccountPOJO.password
            );

            okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(0, TimeUnit.SECONDS) // allow large uploads; caller controls cancel
                    .followRedirects(false)
                    .followSslRedirects(false)
                    .retryOnConnectionFailure(false)
                    .addInterceptor(chain -> {
                        Request req = chain.request();
                        // Preemptive auth: send on FIRST request (prevents 401->retry->StreamClosed)
                        if (req.header("Authorization") == null) {
                            req = req.newBuilder().header("Authorization", credential).build();
                        }
                        return chain.proceed(req);
                    })
                    .build();

            return new OkHttpSardine(okHttpClient);
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "Failed to create sardine");
            return null;
        }
    }

    public OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    private boolean testWebDavServerConnection() {
        try {
            List<DavResource> resources = sardine.list(baseUrl);
            return resources != null;
        } catch (IOException e) {
            Timber.tag(TAG).e(e, "WebDAV connection test failed: %s", e.getMessage());
            return false;
        }
    }

    // Build the base URL from the POJO properties
    private String buildBaseUrl() {
        String protocol = networkAccountPOJO.useHTTPS ? "https" : "http";
        String host = networkAccountPOJO.host;
        int port = networkAccountPOJO.port;
        String basePath = networkAccountPOJO.basePath != null ? networkAccountPOJO.basePath.trim() : "";

        StringBuilder url = new StringBuilder();
        url.append(protocol).append("://").append(host);

        if ((networkAccountPOJO.useHTTPS && port != 443) || (!networkAccountPOJO.useHTTPS && port != 80)) {
            url.append(":").append(port);
        }

        // ROOT must end with '/'
        if (basePath.isEmpty() || "/".equals(basePath)) {
            url.append("/");
            return url.toString();
        }

        if (!basePath.startsWith("/")) url.append("/");
        url.append(basePath);

        // do NOT strip trailing '/'
        return url.toString();
    }

    /**
     * Build full URL from relative path.
     * NOTE: This does NOT percent-encode special characters.
     * If you have special chars/spaces, encode at call site or keep server paths clean.
     */
    public String buildUrl(String relativePath) {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String p = (relativePath == null) ? "" : relativePath.trim();

        if (p.isEmpty() || "/".equals(p)) return base + "/";
        if (!p.startsWith("/")) p = "/" + p;
        p = p.replace('\\', '/');

        return base + p;
    }

    public void shutdown() {
        Timber.tag(TAG).d("Shutting down WebDAV client repository");
        networkAccountPOJO = null;
        NetworkAccountDetailsViewModel.WEBDAV_NETWORK_ACCOUNT_POJO = null;
        instance = null;
        NetworkAccountDetailsViewModel.clearNetworkFileObjectType(FileObjectType.WEBDAV_TYPE);
        NavSessionStore.bump(FileObjectType.WEBDAV_TYPE);
    }

    public Sardine getSardine() {
        return sardine;
    }

    public synchronized String getBasePath(Sardine sardine) throws IOException {
        if (networkAccountPOJO.basePath != null && !networkAccountPOJO.basePath.isEmpty()) {
            return networkAccountPOJO.basePath;
        }
        if (discoveredBasePath != null && !discoveredBasePath.isEmpty()) {
            return discoveredBasePath;
        }
        discoveredBasePath = discoverBasePath(sardine);
        if (!discoveredBasePath.isEmpty()) return discoveredBasePath;
        return "/";
    }

    private String discoverBasePath(Sardine sardine) {
        try {
            List<DavResource> resources = sardine.list(baseUrl, 0);
            if (!resources.isEmpty()) {
                DavResource rootResource = resources.get(0);
                URI hrefUri = rootResource.getHref();
                String discoveredPath = sanitizePath(hrefUri.getPath());
                return (discoveredPath == null || discoveredPath.isEmpty()) ? "/" : discoveredPath;
            }
            return "/";
        } catch (IOException e) {
            Timber.tag(TAG).e(e, "Failed to perform PROPFIND for base path discovery.");
            return "/";
        }
    }

    private String sanitizePath(String path) {
        if (path == null) return null;
        if (!path.startsWith("/")) path = "/" + path;
        if (path.endsWith("/") && path.length() > 1) path = path.substring(0, path.length() - 1);
        return path;
    }
}
