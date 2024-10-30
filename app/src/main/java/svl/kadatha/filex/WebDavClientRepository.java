package svl.kadatha.filex;

import android.os.Bundle;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.thegrizzlylabs.sardineandroid.DavResource;
import com.thegrizzlylabs.sardineandroid.Sardine;
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import timber.log.Timber;

public class WebDavClientRepository {
    private static final String TAG = "WebDavClientRepository";
    private static WebDavClientRepository instance;
    public final String baseUrl;
    private final Sardine sardine;
    private NetworkAccountsDetailsDialog.NetworkAccountPOJO networkAccountPOJO;
    private String discoveredBasePath; // Cached base path

    private WebDavClientRepository(NetworkAccountsDetailsDialog.NetworkAccountPOJO networkAccountPOJO) throws IOException {
        Timber.tag(TAG).d("Entering WebDavClientRepository constructor");
        this.networkAccountPOJO = networkAccountPOJO;
        this.baseUrl = buildBaseUrl();
        Timber.tag(TAG).d("Base URL built: %s", this.baseUrl);

        this.sardine = createAndConfigureSardineClient();
        if (this.sardine == null) {
            Timber.tag(TAG).e("Sardine client creation failed");
            throw new IOException("Failed to create Sardine client");
        }

        Timber.tag(TAG).d("Attempting to test WebDAV server connection");
        if (!testWebDavServerConnection()) {
            Timber.tag(TAG).e("WebDAV server connection test failed");
            throw new IOException("Unable to connect to the WebDAV server with the provided credentials.");
        }
        Timber.tag(TAG).d("WebDavClientRepository constructor completed successfully");
        discoveredBasePath = discoverBasePath(sardine);
    }

    public static synchronized WebDavClientRepository getInstance(NetworkAccountsDetailsDialog.NetworkAccountPOJO networkAccountPOJO) throws IOException {
        Timber.tag(TAG).d("Entering getInstance method");
        if (instance == null) {
            Timber.tag(TAG).d("Creating new WebDavClientRepository instance");
            instance = new WebDavClientRepository(networkAccountPOJO);
        } else {
            Timber.tag(TAG).d("Returning existing WebDavClientRepository instance");
        }
        return instance;
    }

    private Sardine createAndConfigureSardineClient() {
        Timber.tag(TAG).d("Entering createAndConfigureSardineClient method");
        try {
            Timber.tag(TAG).d("Attempting to create Sardine client");
            Sardine sardine = new OkHttpSardine();
            sardine.setCredentials(networkAccountPOJO.user_name, networkAccountPOJO.password);
            Timber.tag(TAG).d("Sardine client created successfully");
            return sardine;
        } catch (IllegalArgumentException e) {
            Timber.tag(TAG).e(e, "Invalid arguments for Sardine client creation: %s", e.getMessage());
        } catch (RuntimeException e) {
            Timber.tag(TAG).e(e, "Runtime exception during Sardine client creation: %s", e.getMessage());
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "Unexpected exception during Sardine client creation: %s", e.getMessage());
        }
        Timber.tag(TAG).d("Exiting createAndConfigureSardineClient method with null");
        return null;
    }

    private boolean testWebDavServerConnection() {
        Timber.tag(TAG).d("Entering testWebDavServerConnection method");
        try {
            List<DavResource> resources = sardine.list(baseUrl);
            boolean result = resources != null;
            Timber.tag(TAG).d("WebDAV connection test result: %s", result ? "success" : "failure");
            return result;
        } catch (IOException e) {
            Timber.tag(TAG).e(e, "WebDAV connection test failed: %s", e.getMessage());
            return false;
        } finally {
            Timber.tag(TAG).d("Exiting testWebDavServerConnection method");
        }
    }

    // Build the base URL from the POJO properties
    private String buildBaseUrl() {
        String protocol = networkAccountPOJO.useHTTPS ? "https" : "http";
        String host = networkAccountPOJO.host;
        int port = networkAccountPOJO.port;
        String basePath = networkAccountPOJO.basePath != null ? networkAccountPOJO.basePath : "";

        // Construct the base URL
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(protocol).append("://").append(host);

        // Append port if it's specified and not the default
        if ((networkAccountPOJO.useHTTPS && port != 443) || (!networkAccountPOJO.useHTTPS && port != 80)) {
            urlBuilder.append(":").append(port);
        }

        // Ensure basePath starts with a slash
        if (!basePath.startsWith("/")) {
            urlBuilder.append("/");
        }
        urlBuilder.append(basePath);

        // Remove trailing slash if present
        if (urlBuilder.charAt(urlBuilder.length() - 1) == '/') {
            urlBuilder.deleteCharAt(urlBuilder.length() - 1);
        }

        return urlBuilder.toString();
    }

    /**
     * Builds a full URL from the given relative path.
     *
     * @param relativePath The relative path to append to the base URL.
     * @return The full URL.
     */
    public String buildUrl(String relativePath) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(baseUrl); // Use the base URL already built in the class

        // Ensure the relativePath starts with '/'
        if (!relativePath.startsWith("/")) {
            urlBuilder.append("/");
        }
        urlBuilder.append(relativePath);

        // Remove trailing slash if present (optional, based on how you want the URLs formatted)
        if (urlBuilder.charAt(urlBuilder.length() - 1) == '/') {
            urlBuilder.deleteCharAt(urlBuilder.length() - 1);
        }

        return urlBuilder.toString(); // Return the full constructed URL
    }


    // Helper method to construct full URL
    private String buildFullPath(String relativePath) {
        StringBuilder sb = new StringBuilder();
        sb.append(baseUrl);
        if (!relativePath.startsWith("/")) {
            sb.append("/");
        }
        sb.append(relativePath);
        return sb.toString();
    }

    // Shutdown the repository (if needed)
    public void shutdown() {
        Timber.tag(TAG).d("Shutting down WebDAV client repository");
        // OkHttpSardine manages its own connections, so no explicit shutdown is needed.
        // If you have any additional resources, clean them up here.

        RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
        Iterator<FilePOJO> iterator = repositoryClass.storage_dir.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getFileObjectType() == FileObjectType.WEBDAV_TYPE) {
                iterator.remove();
            }
        }

        Iterator<FilePOJO> iterator1 = MainActivity.RECENT.iterator();
        while (iterator1.hasNext()) {
            if (iterator1.next().getFileObjectType() == FileObjectType.WEBDAV_TYPE) {
                iterator1.remove();
            }
        }

        Iterator<FilePOJO> iterator2 = FileSelectorActivity.RECENT.iterator();
        while (iterator2.hasNext()) {
            if (iterator2.next().getFileObjectType() == FileObjectType.WEBDAV_TYPE) {
                iterator2.remove();
            }
        }

        Bundle bundle = new Bundle();
        bundle.putSerializable("fileObjectType", FileObjectType.WEBDAV_TYPE);
        Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_REFRESH_STORAGE_DIR_ACTION, LocalBroadcastManager.getInstance(App.getAppContext()), null);
        Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_POP_UP_NETWORK_FILE_TYPE_FRAGMENT, LocalBroadcastManager.getInstance(App.getAppContext()), bundle);
        FilePOJOUtil.REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL(Collections.singletonList(""), FileObjectType.WEBDAV_TYPE);
        Global.DELETE_DIRECTORY_ASYNCHRONOUSLY(Global.WEBDAV_CACHE_DIR);

        networkAccountPOJO = null;
        NetworkAccountDetailsViewModel.WEBDAV_NETWORK_ACCOUNT_POJO = null;
        instance = null;
    }

    /**
     * Provides access to the Sardine client for performing WebDAV operations elsewhere.
     *
     * @return The Sardine client instance.
     */
    public Sardine getSardine() {
        return sardine;
    }

    public synchronized String getBasePath(Sardine sardine) throws IOException {
        // If base path is already set in NetworkAccountPOJO, use it
        if (networkAccountPOJO.basePath != null && !networkAccountPOJO.basePath.isEmpty()) {
            Timber.tag(TAG).d("Using configured base path: %s", networkAccountPOJO.basePath);
            return networkAccountPOJO.basePath;
        }

        // If base path is already discovered and cached, return it
        if (discoveredBasePath != null && !discoveredBasePath.isEmpty()) {
            Timber.tag(TAG).d("Using cached discovered base path: %s", discoveredBasePath);
            return discoveredBasePath;
        }

        // Discover base path dynamically
        discoveredBasePath = discoverBasePath(sardine);
        if (!discoveredBasePath.isEmpty()) {
            Timber.tag(TAG).d("Dynamically discovered base path: %s", discoveredBasePath);
            return discoveredBasePath;
        }

        // Fallback to root if discovery fails
        Timber.tag(TAG).w("Failed to discover base path. Defaulting to '/'");
        return "/";
    }

    /**
     * Discovers the base path of the WebDAV server via a PROPFIND request.
     *
     * @return The discovered base path as a String, or "/" if discovery fails.
     */
    private String discoverBasePath(Sardine sardine) {
        Timber.tag(TAG).d("Attempting to discover base path dynamically.");
        try {
            List<DavResource> resources = sardine.list(baseUrl, 0); // Depth=0

            if (!resources.isEmpty()) {
                DavResource rootResource = resources.get(0);
                URI hrefUri = rootResource.getHref();

                String discoveredPath = parsePathFromHref(hrefUri);

                // Fallback to '/' if parsing fails
                if (discoveredPath == null || discoveredPath.isEmpty()) {
                    Timber.tag(TAG).w("Parsed base path is empty. Using '/' as base path.");
                    discoveredPath = "/";
                }

                Timber.tag(TAG).d("Discovered base path: %s", discoveredPath);
                return discoveredPath;
            } else {
                Timber.tag(TAG).w("No resources found at the root path. Using '/' as base path.");
                return "/";
            }
        } catch (IOException e) {
            Timber.tag(TAG).e(e, "Failed to perform PROPFIND for base path discovery.");
            return "/";
        }
    }

    private String parsePathFromHref(URI hrefUri) {
        try {
            String path = hrefUri.getPath();
            return sanitizePath(path);
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "Error parsing path from href URI: %s", hrefUri);
            return null;
        }
    }


    /**
     * Sanitizes the provided path by ensuring it starts with '/' and does not end with '/'.
     *
     * @param path The path to sanitize.
     * @return The sanitized path.
     */
    private String sanitizePath(String path) {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }
}