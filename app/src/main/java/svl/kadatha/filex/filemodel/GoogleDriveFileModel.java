package svl.kadatha.filex.filemodel;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;
import svl.kadatha.filex.FileObjectType;
import svl.kadatha.filex.FilePOJO;
import svl.kadatha.filex.FilePOJOUtil;
import svl.kadatha.filex.cloud.CloudAuthActivityViewModel;

/**
 * Drop-in GoogleDriveFileModel
 *
 * Goals:
 * 1) If FilePOJO already has cloudId/parentId/mimeType/size/modifiedTime -> NO extra metadata HTTPS.
 * 2) If IDs are missing -> fall back to path resolution (slow).
 * 3) list() uses list response metadata (no per-child metadata fetch).
 * 4) Still supports downloads/uploads/rename/delete/etc.
 *
 * IMPORTANT:
 * - Implement tryResolveFilePOJO(path, type) using your existing hashmap cache.
 * - Keep using UI "display path" for navigation. Drive "identity" = cloudId.
 */
public class GoogleDriveFileModel implements FileModel, StreamUploadFileModel {

    private static final MediaType JSON = MediaType.parse("application/json; charset=UTF-8");
    private static final MediaType OCTET = MediaType.parse("application/octet-stream");
    private static final int RESUMABLE_CHUNK_SIZE = 8 * 1024 * 1024;

    private final String accessToken;
    private final OkHttpClient httpClient;
    private final Gson gson;

    private final String fileId;               // Drive file/folder id (or "root")
    private GoogleDriveFileMetadata metadata;  // may be partial; lazy fetch if needed

    public GoogleDriveFileModel(String path) throws IOException {
        this.accessToken = CloudAuthActivityViewModel.GOOGLE_DRIVE_ACCESS_TOKEN;
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();

        // ✅ Fast path: resolve from cached FilePOJO (already listed in UI)
        FilePOJO pojo = FilePOJOUtil.GET_FILE_POJO(path, FileObjectType.GOOGLE_DRIVE_TYPE);
        if (pojo != null && pojo.getCloudId() != null && !pojo.getCloudId().isEmpty()) {
            this.fileId = pojo.getCloudId();
            this.metadata = buildMetaFromPojo(pojo);
            return;
        }

        // 🐢 Slow fallback: resolve by path queries
        String resolvedId = getFileIdByPath(path);
        if (resolvedId == null) throw new FileNotFoundException("File not found: " + path);

        this.fileId = resolvedId;
        this.metadata = null; // lazy fetch when needed
    }

    /** Build minimal metadata from FilePOJO so most getters avoid HTTPS. */
    private static GoogleDriveFileMetadata buildMetaFromPojo(FilePOJO pojo) {
        GoogleDriveFileMetadata m = new GoogleDriveFileMetadata();
        m.id = pojo.getCloudId();
        m.name = pojo.getName();
        m.mimeType = pojo.getDriveMimeType();

        long sz = pojo.getSizeLong();
        m.size = (sz > 0) ? sz : null;

        // parents (optional)
        if (pojo.getParentCloudId() != null && !pojo.getParentCloudId().isEmpty()) {
            m.parents = new ArrayList<>();
            m.parents.add(pojo.getParentCloudId());
        } else {
            // leave null; ensureMetadataParentsLoaded() will fetch only if required
            m.parents = null;
        }

        // modifiedTime: only if you store it in FilePOJO later; otherwise leave null
        // m.modifiedTime = pojo.getDriveModifiedTime();

        return m;
    }


    private GoogleDriveFileModel(String accessToken, OkHttpClient client, Gson gson, GoogleDriveFileMetadata meta) {
        this.accessToken = accessToken;
        this.httpClient = client;
        this.gson = gson;
        this.fileId = meta != null ? meta.id : null;
        this.metadata = meta;
    }

    // ---------------------------------------------------------------------
    // StreamUploadFileModel
    // ---------------------------------------------------------------------
    @Override
    public boolean putChildFromStream(String childName, InputStream in, long contentLengthOrMinus1, long[] bytesRead) {
        if (bytesRead != null && bytesRead.length > 0) bytesRead[0] = 0;
        if (!isDirectory()) return false;

        // resumable upload requires total size
        if (contentLengthOrMinus1 <= 0) return false;

        try {
            String uploadUrl = startResumableSession(childName, contentLengthOrMinus1);
            if (uploadUrl == null) return false;
            return uploadResumableChunks(uploadUrl, in, contentLengthOrMinus1, bytesRead);
        } catch (Exception e) {
            return false;
        }
    }

    private String startResumableSession(String fileName, long totalBytes) throws IOException {
        HttpUrl url = HttpUrl.parse("https://www.googleapis.com/upload/drive/v3/files")
                .newBuilder()
                .addQueryParameter("uploadType", "resumable")
                .build();

        Map<String, Object> metadataMap = new HashMap<>();
        metadataMap.put("name", fileName);
        metadataMap.put("parents", Collections.singletonList(fileId));
        String metadataJson = gson.toJson(metadataMap);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("X-Upload-Content-Type", "application/octet-stream")
                .addHeader("X-Upload-Content-Length", Long.toString(totalBytes))
                .post(RequestBody.create(metadataJson, JSON))
                .build();

        try (Response resp = httpClient.newCall(request).execute()) {
            if (!resp.isSuccessful()) return null;
            String location = resp.header("Location");
            return (location != null && !location.isEmpty()) ? location : null;
        }
    }

    private boolean uploadResumableChunks(String uploadUrl, InputStream in, long totalBytes, long[] bytesRead) throws IOException {
        byte[] buf = new byte[RESUMABLE_CHUNK_SIZE];
        long uploaded = 0;

        try (InputStream input = in) {
            while (uploaded < totalBytes) {
                int toRead = (int) Math.min(buf.length, totalBytes - uploaded);
                int off = 0;

                while (off < toRead) {
                    int n = input.read(buf, off, toRead - off);
                    if (n == -1) return false; // premature EOF
                    off += n;
                }

                long start = uploaded;
                long endInclusive = uploaded + off - 1;

                RequestBody chunkBody = new ByteArrayRequestBody(buf, off, OCTET);

                Request put = new Request.Builder()
                        .url(uploadUrl)
                        .addHeader("Authorization", "Bearer " + accessToken)
                        .addHeader("Content-Length", Integer.toString(off))
                        .addHeader("Content-Type", "application/octet-stream")
                        .addHeader("Content-Range", "bytes " + start + "-" + endInclusive + "/" + totalBytes)
                        .put(chunkBody)
                        .build();

                try (Response resp = httpClient.newCall(put).execute()) {
                    int code = resp.code();

                    if (code == 308) {
                        uploaded += off;
                        if (bytesRead != null && bytesRead.length > 0) bytesRead[0] = uploaded;
                        continue;
                    }

                    if (code == 200 || code == 201) {
                        uploaded = totalBytes;
                        if (bytesRead != null && bytesRead.length > 0) bytesRead[0] = uploaded;
                        return true;
                    }

                    return false;
                }
            }
        }

        return true;
    }

    public static List<GoogleDriveFileMetadata> listFilesInFolder(String parentId, String oauthToken) {
        List<GoogleDriveFileMetadata> out = new ArrayList<>();
        String pageToken = null;

        OkHttpClient client = new OkHttpClient();
        Gson gson = new Gson();

        try {
            do {
                HttpUrl.Builder urlBuilder = HttpUrl.parse("https://www.googleapis.com/drive/v3/files")
                        .newBuilder()
                        .addQueryParameter("q", "'" + parentId + "' in parents and trashed=false")
                        .addQueryParameter("fields", "nextPageToken, files(id,name,mimeType,parents,modifiedTime,size)")
                        .addQueryParameter("pageSize", "1000");

                if (pageToken != null && !pageToken.isEmpty()) {
                    urlBuilder.addQueryParameter("pageToken", pageToken);
                }

                Request request = new Request.Builder()
                        .url(urlBuilder.build())
                        .header("Authorization", "Bearer " + oauthToken)
                        .get()
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        break;
                    }

                    DriveFilesListResponse res =
                            gson.fromJson(response.body().string(), DriveFilesListResponse.class);

                    if (res == null || res.files == null) break;

                    out.addAll(res.files);
                    pageToken = res.nextPageToken;
                }

            } while (pageToken != null && !pageToken.isEmpty());
        } catch (IOException ignored) {
        }

        return out;
    }

    private static final class ByteArrayRequestBody extends RequestBody {
        private final byte[] data;
        private final int len;
        private final MediaType type;

        ByteArrayRequestBody(byte[] data, int len, MediaType type) {
            this.data = data;
            this.len = len;
            this.type = type;
        }

        @Override public MediaType contentType() { return type; }
        @Override public long contentLength() { return len; }

        @Override public void writeTo(BufferedSink sink) throws IOException {
            sink.write(data, 0, len);
        }
    }

    // ---------------------------------------------------------------------
    // FileModel API
    // ---------------------------------------------------------------------
    @Override
    public String getName() {
        ensureMetadataBasicLoaded();
        return metadata != null ? metadata.name : null;
    }

    @Override
    public String getParentName() {
        try {
            ensureMetadataParentsLoaded();
            if (metadata != null && metadata.parents != null && !metadata.parents.isEmpty()) {
                String parentId = metadata.parents.get(0);
                GoogleDriveFileMetadata parentMeta = getFileMetadata(parentId);
                return parentMeta != null ? parentMeta.name : null;
            }
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * ⚠️ This is expensive and should not be used for UI paths.
     * Prefer your UI displayPath = parentDisplayPath + "/" + name.
     */
    @Override
    public String getPath() {
        try {
            ensureMetadataBasicLoaded();
            return (metadata != null) ? buildPath(metadata) : null;
        } catch (IOException e) {
            return null;
        }
    }

    private String buildPath(GoogleDriveFileMetadata fileMetadata) throws IOException {
        if (fileMetadata.parents != null && !fileMetadata.parents.isEmpty()) {
            String parentId = fileMetadata.parents.get(0);
            if ("root".equals(parentId)) {
                return "/" + fileMetadata.name;
            } else {
                GoogleDriveFileMetadata parent = getFileMetadata(parentId);
                String parentPath = buildPath(parent);
                return parentPath + "/" + fileMetadata.name;
            }
        }
        return "/" + fileMetadata.name;
    }

    @Override
    public String getParentPath() {
        try {
            ensureMetadataParentsLoaded();
            if (metadata != null && metadata.parents != null && !metadata.parents.isEmpty()) {
                String parentId = metadata.parents.get(0);
                if ("root".equals(parentId)) return "/";
                GoogleDriveFileMetadata parent = getFileMetadata(parentId);
                return buildPath(parent);
            }
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public boolean isDirectory() {
        ensureMetadataBasicLoaded();
        return metadata != null && "application/vnd.google-apps.folder".equals(metadata.mimeType);
    }

    @Override
    public boolean rename(String new_name, boolean overwrite) {
        try {
            HttpUrl url = HttpUrl.parse("https://www.googleapis.com/drive/v3/files/" + fileId)
                    .newBuilder()
                    .addQueryParameter("fields", "id,name,mimeType,parents,modifiedTime,size")
                    .build();

            Map<String, Object> jsonMap = new HashMap<>();
            jsonMap.put("name", new_name);

            RequestBody body = RequestBody.create(gson.toJson(jsonMap), MediaType.parse("application/json; charset=utf-8"));

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .patch(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        String responseBody = response.body().string();
                        if (responseBody != null && !responseBody.isEmpty()) {
                            metadata = gson.fromJson(responseBody, GoogleDriveFileMetadata.class);
                        }
                    }
                    return true;
                }
                return false;
            }
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean delete() {
        try {
            HttpUrl url = HttpUrl.parse("https://www.googleapis.com/drive/v3/files/" + fileId).newBuilder().build();

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .delete()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public InputStream getInputStream() {
        if (isDirectory()) return null;

        try {
            HttpUrl url = HttpUrl.parse("https://www.googleapis.com/drive/v3/files/" + fileId)
                    .newBuilder()
                    .addQueryParameter("alt", "media")
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .get()
                    .build();

            Response response = httpClient.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                return response.body().byteStream(); // caller closes
            } else {
                if (response.body() != null) response.body().close();
                return null;
            }
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public OutputStream getChildOutputStream(String child_name, long source_length) {
        throw new UnsupportedOperationException("GoogleDriveFileModel: use putChildFromStream()");
    }

    @Override
    public FileModel[] list() {
        if (!isDirectory()) return new FileModel[0];

        try {
            List<FileModel> out = new ArrayList<>();
            String pageToken = null;

            do {
                HttpUrl.Builder urlBuilder = HttpUrl.parse("https://www.googleapis.com/drive/v3/files")
                        .newBuilder()
                        .addQueryParameter("q", "'" + fileId + "' in parents and trashed = false")
                        .addQueryParameter("fields", "nextPageToken, files(id,name,mimeType,parents,modifiedTime,size)")
                        .addQueryParameter("pageSize", "1000");

                if (pageToken != null && !pageToken.isEmpty()) {
                    urlBuilder.addQueryParameter("pageToken", pageToken);
                }

                Request request = new Request.Builder()
                        .url(urlBuilder.build())
                        .addHeader("Authorization", "Bearer " + accessToken)
                        .get()
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) break;

                    DriveFilesListResponse res = gson.fromJson(response.body().string(), DriveFilesListResponse.class);
                    if (res == null || res.files == null) break;

                    for (GoogleDriveFileMetadata m : res.files) {
                        out.add(new GoogleDriveFileModel(accessToken, httpClient, gson, m)); // ✅ no extra metadata call
                    }

                    pageToken = res.nextPageToken;
                }

            } while (pageToken != null && !pageToken.isEmpty());

            return out.toArray(new FileModel[0]);
        } catch (Exception e) {
            return new FileModel[0];
        }
    }

    @Override
    public boolean createFile(String name) {
        if (!isDirectory()) return false;

        try {
            HttpUrl url = HttpUrl.parse("https://www.googleapis.com/upload/drive/v3/files")
                    .newBuilder()
                    .addQueryParameter("uploadType", "multipart")
                    .build();

            Map<String, Object> metadataMap = new HashMap<>();
            metadataMap.put("name", name);
            metadataMap.put("parents", Collections.singletonList(fileId));

            RequestBody metadataPart = RequestBody.create(gson.toJson(metadataMap), JSON);
            RequestBody filePart = RequestBody.create(new byte[0], OCTET);

            okhttp3.MultipartBody multipartBody = new okhttp3.MultipartBody.Builder()
                    .setType(okhttp3.MultipartBody.MIXED)
                    .addPart(okhttp3.Headers.of("Content-Type", "application/json; charset=UTF-8"), metadataPart)
                    .addPart(okhttp3.Headers.of("Content-Type", "application/octet-stream"), filePart)
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .post(multipartBody)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean makeDirIfNotExists(String dir_name) {
        if (!isDirectory()) return false;

        try {
            String query = "name = '" + escapeForDriveQuery(dir_name) + "' and '" + fileId +
                    "' in parents and mimeType = 'application/vnd.google-apps.folder' and trashed = false";

            HttpUrl url = HttpUrl.parse("https://www.googleapis.com/drive/v3/files")
                    .newBuilder()
                    .addQueryParameter("q", query)
                    .addQueryParameter("fields", "files(id)")
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    DriveFilesListResponse filesListResponse = gson.fromJson(response.body().string(), DriveFilesListResponse.class);
                    if (filesListResponse != null && filesListResponse.files != null && !filesListResponse.files.isEmpty()) {
                        return true;
                    }
                }
            }

            Map<String, Object> metadataMap = new HashMap<>();
            metadataMap.put("name", dir_name);
            metadataMap.put("parents", Collections.singletonList(fileId));
            metadataMap.put("mimeType", "application/vnd.google-apps.folder");

            RequestBody body = RequestBody.create(gson.toJson(metadataMap), JSON);

            Request createRequest = new Request.Builder()
                    .url("https://www.googleapis.com/drive/v3/files")
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .post(body)
                    .build();

            try (Response createResponse = httpClient.newCall(createRequest).execute()) {
                return createResponse.isSuccessful();
            }

        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean makeDirsRecursively(String extended_path) {
        if (!isDirectory()) return false;

        try {
            String[] paths = extended_path.split("/");
            String parentId = fileId;

            for (String dirName : paths) {
                if (dirName.isEmpty()) continue;

                String query = "name = '" + escapeForDriveQuery(dirName) + "' and '" + parentId +
                        "' in parents and mimeType = 'application/vnd.google-apps.folder' and trashed = false";

                HttpUrl url = HttpUrl.parse("https://www.googleapis.com/drive/v3/files")
                        .newBuilder()
                        .addQueryParameter("q", query)
                        .addQueryParameter("fields", "files(id)")
                        .build();

                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer " + accessToken)
                        .get()
                        .build();

                String foundId = null;
                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        DriveFilesListResponse res = gson.fromJson(response.body().string(), DriveFilesListResponse.class);
                        if (res != null && res.files != null && !res.files.isEmpty()) {
                            foundId = res.files.get(0).id;
                        }
                    } else {
                        return false;
                    }
                }

                if (foundId != null) {
                    parentId = foundId;
                } else {
                    Map<String, Object> metadataMap = new HashMap<>();
                    metadataMap.put("name", dirName);
                    metadataMap.put("parents", Collections.singletonList(parentId));
                    metadataMap.put("mimeType", "application/vnd.google-apps.folder");

                    RequestBody body = RequestBody.create(gson.toJson(metadataMap), JSON);

                    Request createRequest = new Request.Builder()
                            .url("https://www.googleapis.com/drive/v3/files")
                            .addHeader("Authorization", "Bearer " + accessToken)
                            .post(body)
                            .build();

                    try (Response createResponse = httpClient.newCall(createRequest).execute()) {
                        if (!createResponse.isSuccessful() || createResponse.body() == null) return false;
                        GoogleDriveFileMetadata createdFolder = gson.fromJson(createResponse.body().string(), GoogleDriveFileMetadata.class);
                        if (createdFolder == null || createdFolder.id == null) return false;
                        parentId = createdFolder.id;
                    }
                }
            }

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public long getLength() {
        ensureMetadataSizeLoaded();
        if (isDirectory()) return 0;
        return (metadata != null && metadata.size != null) ? metadata.size : 0;
    }

    @Override
    public boolean exists() {
        return fileId != null && !fileId.isEmpty();
    }

    @Override
    public long lastModified() {
        ensureMetadataModifiedLoaded();
        if (metadata == null || metadata.modifiedTime == null) return 0;

        // RFC3339: "2026-01-16T12:34:56.000Z" OR "...56Z"
        try {
            SimpleDateFormat f1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            f1.setTimeZone(TimeZone.getTimeZone("UTC"));
            f1.setLenient(true);
            Date d;
            try {
                d = f1.parse(metadata.modifiedTime);
            } catch (Exception ignore) {
                SimpleDateFormat f2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                f2.setTimeZone(TimeZone.getTimeZone("UTC"));
                f2.setLenient(true);
                d = f2.parse(metadata.modifiedTime);
            }
            return d != null ? d.getTime() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public boolean isHidden() {
        ensureMetadataBasicLoaded();
        return metadata != null && metadata.name != null && metadata.name.startsWith(".");
    }

    // ---------------------------------------------------------------------
    // Metadata lazy loading (fetch ONLY when needed)
    // ---------------------------------------------------------------------
    private void ensureMetadataBasicLoaded() {
        if (metadata != null && metadata.name != null && metadata.mimeType != null) return;
        try { metadata = getFileMetadata(fileId); } catch (Exception ignored) {}
    }

    private void ensureMetadataParentsLoaded() {
        if (metadata != null && metadata.parents != null) return;
        try { metadata = getFileMetadata(fileId); } catch (Exception ignored) {}
    }

    private void ensureMetadataSizeLoaded() {
        if (metadata != null && metadata.size != null) return;
        try { metadata = getFileMetadata(fileId); } catch (Exception ignored) {}
    }

    private void ensureMetadataModifiedLoaded() {
        if (metadata != null && metadata.modifiedTime != null) return;
        try { metadata = getFileMetadata(fileId); } catch (Exception ignored) {}
    }

    // ---------------------------------------------------------------------
    // Drive query helpers
    // ---------------------------------------------------------------------
    private static String escapeForDriveQuery(String name) {
        return name == null ? "" : name.replace("'", "\\'");
    }

    private String getFileIdByPath(String path) throws IOException {
        if (path == null) return null;
        if (!path.startsWith("/")) path = "/" + path;

        String[] parts = path.split("/");
        String currentFolderId = "root";

        for (int i = 1; i < parts.length; i++) {
            String name = parts[i];
            if (name == null || name.isEmpty()) continue;

            String query = "name = '" + escapeForDriveQuery(name) + "' and '" + currentFolderId + "' in parents and trashed = false";

            HttpUrl url = HttpUrl.parse("https://www.googleapis.com/drive/v3/files")
                    .newBuilder()
                    .addQueryParameter("q", query)
                    .addQueryParameter("fields", "files(id,name)")
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    throw new IOException("Failed to retrieve file ID: " + response.code() + " - " + response.message());
                }

                DriveFilesListResponse res = gson.fromJson(response.body().string(), DriveFilesListResponse.class);
                if (res != null && res.files != null && !res.files.isEmpty()) {
                    currentFolderId = res.files.get(0).id;
                } else {
                    return null;
                }
            }
        }
        return currentFolderId;
    }

    private GoogleDriveFileMetadata getFileMetadata(String id) throws IOException {
        HttpUrl url = HttpUrl.parse("https://www.googleapis.com/drive/v3/files/" + id)
                .newBuilder()
                .addQueryParameter("fields", "id,name,mimeType,parents,modifiedTime,size")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + accessToken)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return gson.fromJson(response.body().string(), GoogleDriveFileMetadata.class);
            }
            throw new IOException("Failed to get file metadata: " + response.code() + " - " + response.message());
        }
    }

    // ---------------------------------------------------------------------
    // DTOs
    // ---------------------------------------------------------------------
    public static class GoogleDriveFileMetadata {
        public String id;
        public String name;
        public String mimeType;
        public Long size;
        public String modifiedTime;
        public List<String> parents;
    }

    public static class DriveFilesListResponse {
        public List<GoogleDriveFileMetadata> files;
        @SerializedName("nextPageToken")
        public String nextPageToken;
    }

    // Not used for resumable flow, but kept for compatibility if you still use it elsewhere.
    public static class InputStreamRequestBody extends RequestBody {
        private final MediaType contentType;
        private final InputStream inputStream;
        private final long contentLength;

        public InputStreamRequestBody(InputStream inputStream, long contentLength, MediaType contentType) {
            this.inputStream = inputStream;
            this.contentLength = contentLength;
            this.contentType = contentType;
        }

        @Override public MediaType contentType() { return contentType; }
        @Override public long contentLength() { return contentLength; }

        @Override
        public void writeTo(BufferedSink sink) throws IOException {
            Source source = Okio.source(inputStream);
            try {
                sink.writeAll(source);
            } finally {
                source.close();
            }
        }
    }
}
