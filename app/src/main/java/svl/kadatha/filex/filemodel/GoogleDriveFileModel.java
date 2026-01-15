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
import java.util.Map;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

public class GoogleDriveFileModel implements FileModel, StreamUploadFileModel {

    private static final MediaType JSON = MediaType.parse("application/json; charset=UTF-8");
    private static final MediaType OCTET = MediaType.parse("application/octet-stream");

    // Drive recommends multiples of 256 KiB for resumable chunks.
    // 8 MiB is a good balance for mobile. You can tune.
    private static final int RESUMABLE_CHUNK_SIZE = 8 * 1024 * 1024;

    private final String accessToken;
    private final okhttp3.OkHttpClient httpClient;
    private final Gson gson;
    private final String fileId; // this model points to a Drive file/folder id
    private GoogleDriveFileMetadata metadata;

    // Constructor for root directory
    public GoogleDriveFileModel(String accessToken) throws IOException {
        this(accessToken, "root");
    }

    // Path-based constructor (your existing public API)
    public GoogleDriveFileModel(String accessToken, String path) throws IOException {
        this.accessToken = accessToken;
        this.httpClient = new okhttp3.OkHttpClient();
        this.gson = new Gson();

        String resolvedId = getFileIdByPath(path);
        if (resolvedId == null) throw new FileNotFoundException("File not found: " + path);

        this.fileId = resolvedId;
        this.metadata = getFileMetadata(this.fileId);
    }

    // ✅ Internal constructor: build directly from a known fileId (used by list())
    private GoogleDriveFileModel(String accessToken, okhttp3.OkHttpClient client, Gson gson, String fileId) throws IOException {
        this.accessToken = accessToken;
        this.httpClient = client;
        this.gson = gson;
        this.fileId = fileId;
        this.metadata = getFileMetadata(this.fileId);
    }

    // -----------------------------
    // StreamUploadFileModel
    // -----------------------------
    @Override
    public boolean putChildFromStream(String childName,
                                      InputStream in,
                                      long contentLengthOrMinus1,
                                      long[] bytesRead) {
        if (bytesRead != null && bytesRead.length > 0) bytesRead[0] = 0;

        if (!isDirectory()) return false;

        // If length is unknown, Google Drive resumable upload is painful:
        // you can still do it, but you must buffer or have a known length.
        // For your 2GB goal, enforce length when possible.
        if (contentLengthOrMinus1 <= 0) {
            // If you want to support unknown length, you need a different strategy.
            return false;
        }

        try {
            // 1) Create resumable session
            String uploadUrl = startResumableSession(childName, contentLengthOrMinus1);
            if (uploadUrl == null) return false;

            // 2) Upload in chunks (PUT Content-Range)
            return uploadResumableChunks(uploadUrl, in, contentLengthOrMinus1, bytesRead);

        } catch (Exception e) {
            return false;
        }
    }

    private String startResumableSession(String fileName, long totalBytes) throws IOException {
        // POST https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable
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

            // Google returns the resumable session URL in the Location header
            String location = resp.header("Location");
            return (location != null && !location.isEmpty()) ? location : null;
        }
    }

    private boolean uploadResumableChunks(String uploadUrl,
                                          InputStream in,
                                          long totalBytes,
                                          long[] bytesRead) throws IOException {

        byte[] buf = new byte[RESUMABLE_CHUNK_SIZE];
        long uploaded = 0;

        try (InputStream input = in) {
            while (uploaded < totalBytes) {
                int toRead = (int) Math.min(buf.length, totalBytes - uploaded);
                int off = 0;

                // read exactly toRead unless EOF (which is an error because totalBytes promised)
                while (off < toRead) {
                    int n = input.read(buf, off, toRead - off);
                    if (n == -1) {
                        return false; // premature EOF
                    }
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

                    // 308 Resume Incomplete => keep going
                    if (code == 308) {
                        uploaded += off;
                        if (bytesRead != null && bytesRead.length > 0) bytesRead[0] = uploaded;
                        continue;
                    }

                    // 200/201 => final chunk accepted; upload complete
                    if (code == 200 || code == 201) {
                        uploaded = totalBytes;
                        if (bytesRead != null && bytesRead.length > 0) bytesRead[0] = uploaded;
                        return true;
                    }

                    // other failure
                    return false;
                }
            }
        }

        // If we exit loop cleanly, we should have completed.
        return true;
    }

    // Small RequestBody that wraps a byte[] slice without copying
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

    // -----------------------------
    // FileModel API (mostly unchanged)
    // -----------------------------

    @Override
    public String getName() {
        return metadata.name;
    }

    @Override
    public String getParentName() {
        try {
            if (metadata.parents != null && !metadata.parents.isEmpty()) {
                String parentId = metadata.parents.get(0);
                GoogleDriveFileMetadata parentMetadata = getFileMetadata(parentId);
                return parentMetadata.name;
            }
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public String getPath() {
        try {
            return buildPath(metadata);
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
                GoogleDriveFileMetadata parentMetadata = getFileMetadata(parentId);
                String parentPath = buildPath(parentMetadata);
                return parentPath + "/" + fileMetadata.name;
            }
        } else {
            return "/" + fileMetadata.name;
        }
    }

    @Override
    public String getParentPath() {
        try {
            if (metadata.parents != null && !metadata.parents.isEmpty()) {
                String parentId = metadata.parents.get(0);
                if ("root".equals(parentId)) return "/";
                GoogleDriveFileMetadata parentMetadata = getFileMetadata(parentId);
                return buildPath(parentMetadata);
            }
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public boolean isDirectory() {
        return "application/vnd.google-apps.folder".equals(metadata.mimeType);
    }

    @Override
    public boolean rename(String new_name, boolean overwrite) {
        try {
            HttpUrl url = HttpUrl.parse("https://www.googleapis.com/drive/v3/files/" + fileId).newBuilder().build();

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
                    String responseBody = response.body() != null ? response.body().string() : null;
                    if (responseBody != null) metadata = gson.fromJson(responseBody, GoogleDriveFileMetadata.class);
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
                // Caller MUST close this stream. (As in your design)
                return response.body().byteStream();
            } else {
                if (response.body() != null) response.body().close();
                return null;
            }
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 🚫 Do not use OutputStream abstraction for cloud uploads.
     * Use putChildFromStream() instead.
     */
    @Override
    public OutputStream getChildOutputStream(String child_name, long source_length) {
        throw new UnsupportedOperationException("GoogleDriveFileModel: use putChildFromStream()");
    }

    @Override
    public FileModel[] list() {
        if (!isDirectory()) return new FileModel[0];

        try {
            List<FileModel> fileModels = new ArrayList<>();
            String pageToken = null;

            do {
                HttpUrl.Builder urlBuilder = HttpUrl.parse("https://www.googleapis.com/drive/v3/files")
                        .newBuilder()
                        .addQueryParameter("q", "'" + fileId + "' in parents and trashed = false")
                        .addQueryParameter("fields", "nextPageToken, files(id, name, mimeType, parents, size, modifiedTime)")
                        .addQueryParameter("pageSize", "1000");

                if (pageToken != null) urlBuilder.addQueryParameter("pageToken", pageToken);

                Request request = new Request.Builder()
                        .url(urlBuilder.build())
                        .addHeader("Authorization", "Bearer " + accessToken)
                        .get()
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) break;

                    String responseBody = response.body().string();
                    DriveFilesListResponse filesListResponse = gson.fromJson(responseBody, DriveFilesListResponse.class);
                    if (filesListResponse == null || filesListResponse.files == null) break;

                    for (GoogleDriveFileMetadata m : filesListResponse.files) {
                        // ✅ correct: construct from fileId directly
                        fileModels.add(new GoogleDriveFileModel(accessToken, httpClient, gson, m.id));
                    }

                    pageToken = filesListResponse.nextPageToken;
                }

            } while (pageToken != null);

            return fileModels.toArray(new FileModel[0]);
        } catch (Exception e) {
            return new FileModel[0];
        }
    }

    @Override
    public boolean createFile(String name) {
        if (!isDirectory()) return false;

        // For empty file, resumable is overkill; keep your old multipart behavior
        try {
            HttpUrl url = HttpUrl.parse("https://www.googleapis.com/upload/drive/v3/files")
                    .newBuilder()
                    .addQueryParameter("uploadType", "multipart")
                    .build();

            Map<String, Object> metadataMap = new HashMap<>();
            metadataMap.put("name", name);
            metadataMap.put("parents", Collections.singletonList(fileId));

            String metadataJson = gson.toJson(metadataMap);

            RequestBody metadataPart = RequestBody.create(metadataJson, JSON);
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
            String query = "name = '" + dir_name.replace("'", "\\'") + "' and '" + fileId +
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
                if (!response.isSuccessful() || response.body() == null) return false;

                DriveFilesListResponse filesListResponse = gson.fromJson(response.body().string(), DriveFilesListResponse.class);
                if (filesListResponse != null && filesListResponse.files != null && !filesListResponse.files.isEmpty()) {
                    return true;
                }
            }

            // Create folder
            Map<String, Object> metadataMap = new HashMap<>();
            metadataMap.put("name", dir_name);
            metadataMap.put("parents", Collections.singletonList(fileId));
            metadataMap.put("mimeType", "application/vnd.google-apps.folder");

            RequestBody body = RequestBody.create(gson.toJson(metadataMap), JSON);

            HttpUrl createUrl = HttpUrl.parse("https://www.googleapis.com/drive/v3/files").newBuilder().build();

            Request createRequest = new Request.Builder()
                    .url(createUrl)
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

                String escaped = dirName.replace("'", "\\'");
                String query = "name = '" + escaped + "' and '" + parentId +
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
                        DriveFilesListResponse filesListResponse = gson.fromJson(response.body().string(), DriveFilesListResponse.class);
                        if (filesListResponse != null && filesListResponse.files != null && !filesListResponse.files.isEmpty()) {
                            foundId = filesListResponse.files.get(0).id;
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
        if (isDirectory()) return 0;
        return (metadata.size != null) ? metadata.size : 0;
    }

    @Override
    public boolean exists() {
        return metadata != null;
    }

    @Override
    public long lastModified() {
        if (metadata.modifiedTime != null) {
            try {
                String modifiedTime = metadata.modifiedTime;
                if (modifiedTime.endsWith("Z")) {
                    modifiedTime = modifiedTime.substring(0, modifiedTime.length() - 1) + "+0000";
                } else if (modifiedTime.matches(".*[\\+\\-]\\d\\d:\\d\\d$")) {
                    modifiedTime = modifiedTime.substring(0, modifiedTime.length() - 3) + modifiedTime.substring(modifiedTime.length() - 2);
                }

                // NOTE: your original pattern looked wrong for RFC3339. Keeping your behavior,
                // but ideally parse RFC3339 properly.
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
                sdf.setLenient(true);

                Date date = sdf.parse(modifiedTime);
                return date != null ? date.getTime() : 0;
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }

    @Override
    public boolean isHidden() {
        return metadata.name != null && metadata.name.startsWith(".");
    }

    // -----------------------------
    // helpers and metadata
    // -----------------------------

    // Method to resolve path to fileId (unchanged)
    private String getFileIdByPath(String path) throws IOException {
        if (!path.startsWith("/")) path = "/" + path;
        String[] parts = path.split("/");
        String currentFolderId = "root";

        for (int i = 1; i < parts.length; i++) {
            String name = parts[i];
            if (name.isEmpty()) continue;

            String escapedName = name.replace("'", "\\'");
            String query = "name = '" + escapedName + "' and '" + currentFolderId + "' in parents and trashed = false";

            HttpUrl url = HttpUrl.parse("https://www.googleapis.com/drive/v3/files")
                    .newBuilder()
                    .addQueryParameter("q", query)
                    .addQueryParameter("fields", "files(id, name)")
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

                DriveFilesListResponse filesListResponse = gson.fromJson(response.body().string(), DriveFilesListResponse.class);

                if (filesListResponse.files != null && !filesListResponse.files.isEmpty()) {
                    currentFolderId = filesListResponse.files.get(0).id;
                } else {
                    return null;
                }
            }
        }
        return currentFolderId;
    }

    private GoogleDriveFileMetadata getFileMetadata(String fileId) throws IOException {
        HttpUrl url = HttpUrl.parse("https://www.googleapis.com/drive/v3/files/" + fileId)
                .newBuilder()
                .addQueryParameter("fields", "*")
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
                        .addQueryParameter("fields", "nextPageToken, files(id, name, mimeType, parents)")
                        .addQueryParameter("pageSize", "1000");
                // If you support shared drives, uncomment:
                // .addQueryParameter("supportsAllDrives", "true")
                // .addQueryParameter("includeItemsFromAllDrives", "true");

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
                        System.err.println("List failed: " + response.code() + " - " + response.message());
                        break;
                    }

                    String responseBody = response.body().string();
                    DriveFilesListResponse filesListResponse =
                            gson.fromJson(responseBody, DriveFilesListResponse.class);

                    if (filesListResponse == null || filesListResponse.files == null) {
                        break;
                    }

                    out.addAll(filesListResponse.files);
                    pageToken = filesListResponse.nextPageToken;
                }

            } while (pageToken != null && !pageToken.isEmpty());

        } catch (IOException e) {
            e.printStackTrace();
        }

        return out;
    }


    // Helper classes
    public static class GoogleDriveFileMetadata {
        public String id;
        public String name;
        String mimeType;
        Long size;
        String modifiedTime;
        List<String> parents;
    }

    public static class DriveFilesListResponse {
        public List<GoogleDriveFileMetadata> files;
        @SerializedName("nextPageToken")
        String nextPageToken;
    }

    // Keep your InputStreamRequestBody if you need it elsewhere; not used for resumable.
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
