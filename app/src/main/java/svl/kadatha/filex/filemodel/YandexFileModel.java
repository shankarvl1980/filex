package svl.kadatha.filex.filemodel;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;
import svl.kadatha.filex.Global;
import svl.kadatha.filex.MakeCloudFilePOJOUtil;
import timber.log.Timber;

public class YandexFileModel implements FileModel, StreamUploadFileModel {

    private static final String TAG = "YandexFileModel";

    private static final String BASE_URL = "https://cloud-api.yandex.net/v1/disk/resources";
    private static final String DOWNLOAD_URL = "https://cloud-api.yandex.net/v1/disk/resources/download";
    private static final String UPLOAD_URL = "https://cloud-api.yandex.net/v1/disk/resources/upload";
    private static final String MOVE_URL = "https://cloud-api.yandex.net/v1/disk/resources/move";

    private static final MediaType OCTET = MediaType.parse("application/octet-stream");

    private static final SimpleDateFormat RFC3339_FORMAT;
    private static final SimpleDateFormat RFC3339_Z =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
    private static final SimpleDateFormat RFC3339_Z_MS =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);

    static {
        // Many Yandex timestamps are RFC3339 with timezone offsets too.
        // We'll try this "Z" format first, then fallback to java parsing best-effort.
        RFC3339_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        RFC3339_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private final String accessToken;
    private final String path; // app-level path: "/" or "/Folder/File"
    private final OkHttpClient client;
    private final Gson gson = Global.GSON;

    private MakeCloudFilePOJOUtil.YandexResource metadata;

    public YandexFileModel(String accessToken, String path) throws IOException {
        this(accessToken, path, null);
    }

    // internal ctor to reuse client (useful when listing)
    private YandexFileModel(String accessToken, String path, OkHttpClient reuseClient) throws IOException {
        this.accessToken = accessToken;
        this.path = normalizePath(path);

        this.client = (reuseClient != null)
                ? reuseClient
                : new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(0, TimeUnit.SECONDS) // important for big uploads
                .build();

        this.metadata = fetchMetadata(this.path);
        if (this.metadata == null) {
            throw new FileNotFoundException("Metadata not found for path: " + this.path);
        }
    }

    private static String normalizePath(String p) {
        if (p == null || p.trim().isEmpty()) return "/";
        p = p.trim();
        if (!p.startsWith("/")) p = "/" + p;
        if (p.length() > 1 && p.endsWith("/")) p = p.substring(0, p.length() - 1);
        return p;
    }

    private static String normalizeRfc3339(String s) {
        if (s == null) return null;

        // Convert "+03:00" → "+0300"
        // Convert "-05:30" → "-0530"
        if (s.length() >= 6) {
            char c = s.charAt(s.length() - 6);
            if (c == '+' || c == '-') {
                return s.substring(0, s.length() - 3)
                        + s.substring(s.length() - 2);
            }
        }
        return s;
    }

    // -----------------------------
    // FileModel basics
    // -----------------------------

    private MakeCloudFilePOJOUtil.YandexResource fetchMetadata(String p) throws IOException {
        HttpUrl url = HttpUrl.parse(BASE_URL)
                .newBuilder()
                .addQueryParameter("path", "/".equals(p) ? "/" : p)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "OAuth " + accessToken)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return gson.fromJson(response.body().charStream(), MakeCloudFilePOJOUtil.YandexResource.class);
            }
            Timber.tag(TAG).e("fetchMetadata failed: %d %s", response.code(), response.message());
            return null;
        }
    }

    @Override
    public String getName() {
        // For root, your metadata.name might be null depending on fields.
        if ("/".equals(path)) return "/";
        return (metadata != null && metadata.name != null) ? metadata.name : new java.io.File(path).getName();
    }

    @Override
    public String getParentName() {
        String parentPath = getParentPath();
        if (parentPath == null) return null;
        if ("/".equals(parentPath)) return "/";

        try {
            MakeCloudFilePOJOUtil.YandexResource parent = fetchMetadata(parentPath);
            return (parent != null) ? parent.name : null;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getParentPath() {
        if ("/".equals(path)) return null;
        int idx = path.lastIndexOf('/');
        if (idx == 0) return "/";
        if (idx > 0) return path.substring(0, idx);
        return null;
    }

    @Override
    public boolean isDirectory() {
        return metadata != null && metadata.isDir();
    }

    @Override
    public boolean rename(String new_name, boolean overwrite) {
        if ("/".equals(path)) return false;

        String parent = getParentPath();
        if (parent == null) parent = "/";

        String newPath = "/".equals(parent) ? ("/" + new_name) : (parent + "/" + new_name);

        HttpUrl url = HttpUrl.parse(MOVE_URL)
                .newBuilder()
                .addQueryParameter("from", path)
                .addQueryParameter("path", newPath)
                .addQueryParameter("overwrite", String.valueOf(overwrite))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "OAuth " + accessToken)
                .post(RequestBody.create(new byte[0], null))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Timber.tag(TAG).e("rename failed: %d %s", response.code(), response.message());
                return false;
            }
            // Yandex move can be async; metadata might not be instantly available.
            // Best-effort refresh:
            try {
                this.metadata = fetchMetadata(newPath);
            } catch (Exception ignored) {
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean delete() {
        if ("/".equals(path)) return false;

        HttpUrl url = HttpUrl.parse(BASE_URL)
                .newBuilder()
                .addQueryParameter("path", path)
                .addQueryParameter("permanently", "true")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "OAuth " + accessToken)
                .delete()
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public InputStream getInputStream() {
        if (isDirectory()) return null;

        // 1) Ask Yandex for download link
        HttpUrl dUrl = HttpUrl.parse(DOWNLOAD_URL)
                .newBuilder()
                .addQueryParameter("path", path)
                .build();

        Request dReq = new Request.Builder()
                .url(dUrl)
                .header("Authorization", "OAuth " + accessToken)
                .get()
                .build();

        try {
            Response dRes = client.newCall(dReq).execute();
            if (!dRes.isSuccessful() || dRes.body() == null) {
                if (dRes != null) dRes.close();
                return null;
            }

            MakeCloudFilePOJOUtil.YandexDownloadResponse dl =
                    gson.fromJson(dRes.body().charStream(), MakeCloudFilePOJOUtil.YandexDownloadResponse.class);
            dRes.close();

            if (dl == null || dl.href == null) return null;

            // 2) Open direct file stream (caller closes)
            Request fileReq = new Request.Builder().url(dl.href).get().build();
            Response fileRes = client.newCall(fileReq).execute();

            if (fileRes.isSuccessful() && fileRes.body() != null) {
                // IMPORTANT: caller closes InputStream => closes ResponseBody.
                return fileRes.body().byteStream();
            } else {
                fileRes.close();
                return null;
            }

        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 🚫 For Yandex, do not expose OutputStream uploads.
     * Use putChildFromStream() (true streaming PUT).
     */
    @Override
    public OutputStream getChildOutputStream(String child_name, long source_length) {
        throw new UnsupportedOperationException("YandexFileModel: use putChildFromStream()");
    }

    @Override
    public FileModel[] list() {
        if (!isDirectory()) return new FileModel[0];

        HttpUrl url = HttpUrl.parse(BASE_URL)
                .newBuilder()
                .addQueryParameter("path", "/".equals(path) ? "/" : path)
                .addQueryParameter("fields",
                        "_embedded.items.name,_embedded.items.type,_embedded.items.size,_embedded.items.modified,_embedded.items.path")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "OAuth " + accessToken)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                Timber.tag(TAG).e("list failed: %d %s", response.code(), response.message());
                return new FileModel[0];
            }

            MakeCloudFilePOJOUtil.YandexResource dirRes =
                    gson.fromJson(response.body().charStream(), MakeCloudFilePOJOUtil.YandexResource.class);

            if (dirRes == null || dirRes._embedded == null || dirRes._embedded.items == null) {
                return new FileModel[0];
            }

            List<FileModel> out = new ArrayList<>();
            for (MakeCloudFilePOJOUtil.YandexResource item : dirRes._embedded.items) {
                // API returns "disk:/..."
                String itemPath = (item.path != null) ? item.path.replaceFirst("^disk:", "") : null;
                if (itemPath == null || itemPath.trim().isEmpty()) continue;
                out.add(new YandexFileModel(accessToken, itemPath, client));
            }
            return out.toArray(new FileModel[0]);

        } catch (IOException e) {
            return new FileModel[0];
        }
    }

    @Override
    public boolean createFile(String name) {
        if (!isDirectory()) return false;

        // Do not call putChildFromStream() here to avoid recursion in future.
        final String childPath = "/".equals(path) ? ("/" + name) : (path + "/" + name);

        HttpUrl uUrl = HttpUrl.parse(UPLOAD_URL)
                .newBuilder()
                .addQueryParameter("path", childPath)
                .addQueryParameter("overwrite", "true")
                .build();

        Request uReq = new Request.Builder()
                .url(uUrl)
                .header("Authorization", "OAuth " + accessToken)
                .get()
                .build();

        try (Response uRes = client.newCall(uReq).execute()) {
            if (!uRes.isSuccessful() || uRes.body() == null) return false;

            MakeCloudFilePOJOUtil.YandexUploadResponse upl =
                    gson.fromJson(uRes.body().charStream(), MakeCloudFilePOJOUtil.YandexUploadResponse.class);

            if (upl == null || upl.href == null) return false;

            Request putReq = new Request.Builder()
                    .url(upl.href)
                    .put(RequestBody.create(new byte[0], null))
                    .build();

            try (Response putRes = client.newCall(putReq).execute()) {
                return putRes.isSuccessful();
            }
        } catch (IOException e) {
            return false;
        }
    }


    @Override
    public boolean makeDirIfNotExists(String dir_name) {
        if (!isDirectory()) return false;

        String dirPath = "/".equals(path) ? ("/" + dir_name) : (path + "/" + dir_name);

        HttpUrl url = HttpUrl.parse(BASE_URL)
                .newBuilder()
                .addQueryParameter("path", dirPath)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "OAuth " + accessToken)
                .put(RequestBody.create(new byte[0], null))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) return true;
            if (response.code() == 409) return true; // already exists
            Timber.tag(TAG).e("mkdir failed: %d %s", response.code(), response.message());
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean makeDirsRecursively(String extended_path) {
        if (!isDirectory()) return false;

        String current = path;
        String[] parts = extended_path.split("/");

        for (String part : parts) {
            if (part == null || part.isEmpty()) continue;

            String next = "/".equals(current) ? ("/" + part) : (current + "/" + part);

            // Ensure next folder exists by calling API on THAT path
            if (!mkdirAbsolute(next)) return false;

            current = next;
        }
        return true;
    }

    private boolean mkdirAbsolute(String absoluteDirPath) {
        HttpUrl url = HttpUrl.parse(BASE_URL)
                .newBuilder()
                .addQueryParameter("path", absoluteDirPath)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "OAuth " + accessToken)
                .put(RequestBody.create(new byte[0], null))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) return true;
            if (response.code() == 409) return true;
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public long getLength() {
        if (isDirectory()) return 0;
        return (metadata != null && metadata.size != null) ? metadata.size : 0;
    }

    @Override
    public boolean exists() {
        if ("/".equals(path)) return true;
        if (metadata != null) return true;
        try {
            return fetchMetadata(path) != null;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public long lastModified() {
        if (isDirectory()) return 0;
        if (metadata == null || metadata.modified == null) return 0;

        String raw = metadata.modified;
        String s = normalizeRfc3339(raw);

        try {
            if (s.contains(".")) {
                Date d = RFC3339_Z_MS.parse(s);
                return d != null ? d.getTime() : 0;
            } else {
                Date d = RFC3339_Z.parse(s);
                return d != null ? d.getTime() : 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public boolean isHidden() {
        String n = getName();
        return n != null && n.startsWith(".");
    }

    // -----------------------------
    // StreamUploadFileModel
    // -----------------------------

    @Override
    public boolean putChildFromStream(@NonNull String childName,
                                      @NonNull InputStream in,
                                      long contentLengthOrMinus1,
                                      long[] bytesRead) {

        if (bytesRead != null && bytesRead.length > 0) bytesRead[0] = 0;
        if (!isDirectory()) return false;

        final String childPath = "/".equals(path) ? ("/" + childName) : (path + "/" + childName);

        // 1) Get upload link (href)
        HttpUrl uUrl = HttpUrl.parse(UPLOAD_URL)
                .newBuilder()
                .addQueryParameter("path", childPath)
                .addQueryParameter("overwrite", "true")
                .build();

        Request uReq = new Request.Builder()
                .url(uUrl)
                .header("Authorization", "OAuth " + accessToken)
                .get()
                .build();

        try (Response uRes = client.newCall(uReq).execute()) {
            if (!uRes.isSuccessful() || uRes.body() == null) {
                Timber.tag(TAG).e("get upload href failed: %d %s", uRes.code(), uRes.message());
                try { in.close(); } catch (Exception ignored) {}
                return false;
            }

            MakeCloudFilePOJOUtil.YandexUploadResponse upl =
                    gson.fromJson(uRes.body().charStream(), MakeCloudFilePOJOUtil.YandexUploadResponse.class);

            if (upl == null || upl.href == null || upl.href.trim().isEmpty()) {
                Timber.tag(TAG).e("upload href missing");
                try { in.close(); } catch (Exception ignored) {}
                return false;
            }

            final long len = (contentLengthOrMinus1 >= 0) ? contentLengthOrMinus1 : -1;

            RequestBody body = new RequestBody() {
                @Override
                public MediaType contentType() {
                    return OCTET;
                }

                @Override
                public long contentLength() {
                    // 0-byte => returns 0 (valid); -1 => chunked
                    return len;
                }

                @Override
                public void writeTo(BufferedSink sink) throws IOException {
                    byte[] buf = new byte[64 * 1024];
                    int n;
                    long total = 0;

                    // IMPORTANT: do NOT close `in` here
                    while ((n = in.read(buf)) != -1) {
                        sink.write(buf, 0, n);
                        total += n;
                        if (bytesRead != null && bytesRead.length > 0) bytesRead[0] = total;
                    }
                }
            };

            Request putReq = new Request.Builder()
                    .url(upl.href)
                    .put(body)
                    .build();

            try (Response putRes = client.newCall(putReq).execute()) {
                if (!putRes.isSuccessful()) {
                    Timber.tag(TAG).e("upload PUT failed: %d %s", putRes.code(), putRes.message());
                    return false;
                }
            } finally {
                // Upload owns the stream: close after request completes
                try { in.close(); } catch (Exception ignored) {}
            }

            // refresh metadata best-effort
            try { this.metadata = fetchMetadata(childPath); } catch (Exception ignored) {}

            // strict length check if known and >0 (0-byte is fine)
            if (contentLengthOrMinus1 > 0 && bytesRead != null && bytesRead.length > 0) {
                if (bytesRead[0] != contentLengthOrMinus1) return false;
            }

            return true;

        } catch (IOException e) {
            Timber.tag(TAG).e(e, "putChildFromStream failed");
            try { in.close(); } catch (Exception ignored) {}
            return false;
        }
    }

}
