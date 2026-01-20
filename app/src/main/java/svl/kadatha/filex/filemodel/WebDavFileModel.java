package svl.kadatha.filex.filemodel;

import com.thegrizzlylabs.sardineandroid.DavResource;
import com.thegrizzlylabs.sardineandroid.Sardine;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;
import svl.kadatha.filex.Global;
import svl.kadatha.filex.network.NetworkAccountDetailsViewModel;
import svl.kadatha.filex.network.WebDavClientRepository;
import timber.log.Timber;

public class WebDavFileModel implements FileModel, StreamUploadFileModel {

    private static final String TAG = "WebDavFileModel";
    private final String path;

    public WebDavFileModel(String path) {
        this.path = path;
    }

    private boolean isDirectoryInternal() {
        Sardine sardine;
        String url;
        try {
            WebDavClientRepository repo =
                    WebDavClientRepository.getInstance(NetworkAccountDetailsViewModel.WEBDAV_NETWORK_ACCOUNT_POJO);
            sardine = repo.getSardine();
            url = repo.buildUrl(path);
        } catch (IOException e) {
            Timber.tag(TAG).e(e, "Failed to get Sardine client for isDirectory check");
            return false;
        }

        try {
            List<DavResource> resources = sardine.list(url, 0);
            if (resources.isEmpty()) return false;
            return resources.get(0).isDirectory();
        } catch (IOException e) {
            Timber.tag(TAG).e(e, "Error checking if path is directory: %s", path);
            return false;
        }
    }

    @Override
    public String getName() {
        return new File(path).getName();
    }

    @Override
    public String getParentName() {
        File parent = new File(path).getParentFile();
        return (parent != null) ? parent.getName() : null;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getParentPath() {
        return new File(path).getParent();
    }

    @Override
    public boolean isDirectory() {
        return isDirectoryInternal();
    }

    @Override
    public boolean rename(String new_name, boolean overwrite) {
        String new_file_path = Global.CONCATENATE_PARENT_CHILD_PATH(getParentPath(), new_name);

        try {
            WebDavClientRepository repo =
                    WebDavClientRepository.getInstance(NetworkAccountDetailsViewModel.WEBDAV_NETWORK_ACCOUNT_POJO);
            Sardine sardine = repo.getSardine();

            String url = repo.buildUrl(path);
            String destUrl = repo.buildUrl(new_file_path);

            sardine.move(url, destUrl);
            return true;
        } catch (IOException e) {
            Timber.tag(TAG).e(e, "Rename operation failed");
            return false;
        }
    }

    @Override
    public boolean delete() {
        try {
            deleteRecursively(path);
            return true;
        } catch (IOException e) {
            Timber.tag(TAG).e(e, "Error deleting WebDAV resource");
            return false;
        }
    }

    private void deleteRecursively(String targetPath) throws IOException {
        WebDavClientRepository repo =
                WebDavClientRepository.getInstance(NetworkAccountDetailsViewModel.WEBDAV_NETWORK_ACCOUNT_POJO);
        Sardine sardine = repo.getSardine();
        String url = repo.buildUrl(targetPath);

        WebDavFileModel target = new WebDavFileModel(targetPath);
        if (target.isDirectory()) {
            List<DavResource> resources = sardine.list(url);
            if (!resources.isEmpty()) resources.remove(0);

            for (DavResource r : resources) {
                String resourcePath = r.getPath();
                new WebDavFileModel(resourcePath).delete();
            }
            sardine.delete(url);
        } else {
            sardine.delete(url);
        }
    }

    @Override
    public InputStream getInputStream() {
        try {
            WebDavClientRepository repo =
                    WebDavClientRepository.getInstance(NetworkAccountDetailsViewModel.WEBDAV_NETWORK_ACCOUNT_POJO);
            Sardine sardine = repo.getSardine();
            String url = repo.buildUrl(path);
            return sardine.get(url);
        } catch (IOException e) {
            Timber.tag(TAG).e(e, "Failed to get InputStream");
            return null;
        }
    }

    /**
     * IMPORTANT:
     * WebDAV upload must NOT use OutputStream buffering.
     * Use putChildFromStream() (streaming PUT) via FileUtil WebDAV fast-path.
     */
    @Override
    public OutputStream getChildOutputStream(String childName, long sourceLength) {
        throw new UnsupportedOperationException(
                "WebDAV uploads must use putChildFromStream() (streaming PUT)."
        );
    }

    /**
     * Streaming PUT upload (no buffering).
     * If contentLengthOrMinus1 <= 0, OkHttp will use chunked encoding.
     */
    @Override
    public boolean putChildFromStream(String childName,
                                      InputStream in,
                                      long contentLengthOrMinus1,
                                      long[] bytesRead) {

        if (bytesRead != null && bytesRead.length > 0) bytesRead[0] = 0;

        final String filePath = Global.CONCATENATE_PARENT_CHILD_PATH(path, childName);

        WebDavClientRepository repo;
        OkHttpClient client;
        String url;

        try {
            repo = WebDavClientRepository.getInstance(NetworkAccountDetailsViewModel.WEBDAV_NETWORK_ACCOUNT_POJO);
            client = repo.getOkHttpClient();
            url = repo.buildUrl(filePath);
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "putChildFromStream init failed");
            try {
                in.close();
            } catch (Exception ignored) {
            }
            return false;
        }

        final long len = (contentLengthOrMinus1 > 0) ? contentLengthOrMinus1 : -1;

        RequestBody body = new RequestBody() {
            @Override
            public MediaType contentType() {
                return MediaType.parse("application/octet-stream");
            }

            @Override
            public long contentLength() {
                return len; // -1 => chunked transfer
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                byte[] buf = new byte[64 * 1024];
                int n;
                long total = 0;

                // IMPORTANT: do NOT close `in` here.
                while ((n = in.read(buf)) != -1) {
                    sink.write(buf, 0, n);
                    total += n;
                    if (bytesRead != null && bytesRead.length > 0) {
                        bytesRead[0] = total;
                    }
                }
                // No sink.flush() needed; OkHttp handles it.
            }
        };

        Request request = new Request.Builder()
                .url(url)
                .put(body)
                .header("Content-Type", "application/octet-stream")
                // IMPORTANT: remove the empty Expect header; it can break some servers
                // .header("Expect", "")
                .build();

        try (Response resp = client.newCall(request).execute()) {

            if (!resp.isSuccessful()) {
                if (resp.code() == 413) {
                    Timber.tag(TAG).e("WebDAV PUT failed: 413 Request Entity Too Large (server limit)");
                } else {
                    Timber.tag(TAG).e("WebDAV PUT failed: %d %s", resp.code(), resp.message());
                }
                return false;
            }
            return true;

        } catch (IOException e) {
            Timber.tag(TAG).e(e, "WebDAV PUT failed (IO)");
            return false;

        } finally {
            // Upload owns the stream: close it once the call finishes (success/fail).
            try {
                in.close();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public FileModel[] list() {
        try {
            WebDavClientRepository repo =
                    WebDavClientRepository.getInstance(NetworkAccountDetailsViewModel.WEBDAV_NETWORK_ACCOUNT_POJO);
            Sardine sardine = repo.getSardine();
            String url = repo.buildUrl(path);

            List<DavResource> resources = sardine.list(url);
            if (resources.isEmpty()) return new FileModel[0];
            resources.remove(0);

            List<FileModel> out = new ArrayList<>();
            for (DavResource r : resources) {
                out.add(new WebDavFileModel(r.getPath()));
            }
            return out.toArray(new FileModel[0]);

        } catch (IOException e) {
            Timber.tag(TAG).e(e, "Failed to list files");
            return new FileModel[0];
        }
    }

    @Override
    public boolean createFile(String name) {
        String file_path = Global.CONCATENATE_PARENT_CHILD_PATH(path, name);
        try {
            WebDavClientRepository repo =
                    WebDavClientRepository.getInstance(NetworkAccountDetailsViewModel.WEBDAV_NETWORK_ACCOUNT_POJO);
            repo.getSardine().put(repo.buildUrl(file_path), new byte[0]);
            return true;
        } catch (IOException e) {
            Timber.tag(TAG).e(e, "Failed to create file");
            return false;
        }
    }

    @Override
    public boolean makeDirIfNotExists(String dir_name) {
        String dir_path = Global.CONCATENATE_PARENT_CHILD_PATH(path, dir_name);
        try {
            WebDavClientRepository repo =
                    WebDavClientRepository.getInstance(NetworkAccountDetailsViewModel.WEBDAV_NETWORK_ACCOUNT_POJO);
            repo.getSardine().createDirectory(repo.buildUrl(dir_path));
            return true;
        } catch (IOException e) {
            // Better than string-matching "Method Not Allowed" (still keep your old behavior)
            String msg = e.getMessage();
            if (msg != null && msg.contains("Method Not Allowed")) return true;
            Timber.tag(TAG).e(e, "Failed to create directory");
            return false;
        }
    }

    @Override
    public boolean makeDirsRecursively(String extended_path) {
        String fullPath = Global.CONCATENATE_PARENT_CHILD_PATH(path, extended_path);
        try {
            WebDavClientRepository repo =
                    WebDavClientRepository.getInstance(NetworkAccountDetailsViewModel.WEBDAV_NETWORK_ACCOUNT_POJO);
            repo.getSardine().createDirectory(repo.buildUrl(fullPath));
            return true;
        } catch (IOException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("Method Not Allowed")) return true;
            Timber.tag(TAG).e(e, "Failed to create directories recursively");
            return false;
        }
    }

    @Override
    public long getLength() {
        try {
            WebDavClientRepository repo =
                    WebDavClientRepository.getInstance(NetworkAccountDetailsViewModel.WEBDAV_NETWORK_ACCOUNT_POJO);
            Sardine sardine = repo.getSardine();
            String url = repo.buildUrl(path);

            List<DavResource> resources = sardine.list(url, 0);
            if (!resources.isEmpty()) {
                return resources.get(0).getContentLength();
            }
            return 0;
        } catch (IOException e) {
            return 0;
        }
    }

    @Override
    public boolean exists() {
        try {
            WebDavClientRepository repo =
                    WebDavClientRepository.getInstance(NetworkAccountDetailsViewModel.WEBDAV_NETWORK_ACCOUNT_POJO);
            List<DavResource> resources = repo.getSardine().list(repo.buildUrl(path), 0);
            return resources != null && !resources.isEmpty();
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public long lastModified() {
        try {
            WebDavClientRepository repo =
                    WebDavClientRepository.getInstance(NetworkAccountDetailsViewModel.WEBDAV_NETWORK_ACCOUNT_POJO);
            List<DavResource> resources = repo.getSardine().list(repo.buildUrl(path), 0);
            if (!resources.isEmpty()) {
                Date d = resources.get(0).getModified();
                return (d != null) ? d.getTime() : 0;
            }
            return 0;
        } catch (IOException e) {
            return 0;
        }
    }

    @Override
    public boolean isHidden() {
        return new File(path).isHidden();
    }
}
