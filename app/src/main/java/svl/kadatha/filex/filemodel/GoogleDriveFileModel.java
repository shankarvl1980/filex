package svl.kadatha.filex.filemodel;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

public class GoogleDriveFileModel implements FileModel {
    private final String accessToken;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final String fileId;
    private GoogleDriveFileMetadata metadata;


    // Constructor for root directory
    public GoogleDriveFileModel(String accessToken) throws IOException {
        this(accessToken, "root");
    }

    // New constructor that accepts a path
    public GoogleDriveFileModel(String accessToken, String path) throws IOException {
        this.accessToken = accessToken;
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();
        this.fileId = getFileIdByPath(path);
        if (this.fileId == null) {
            throw new FileNotFoundException("File not found: " + path);
        }
        this.metadata = getFileMetadata(this.fileId);
    }

    public static List<GoogleDriveFileMetadata> listFilesInFolder(String parentId, String oauthToken) {
        List<GoogleDriveFileMetadata> metadataList = new ArrayList<>();
        String pageToken = null;

        try {
            do {
                HttpUrl.Builder urlBuilder = HttpUrl.parse("https://www.googleapis.com/drive/v3/files")
                        .newBuilder()
                        .addQueryParameter("q", "'" + parentId + "' in parents and trashed=false")
                        .addQueryParameter("fields", "nextPageToken, files(id, name, mimeType, parents)")
                        .addQueryParameter("pageSize", "1000");

                if (pageToken != null) {
                    urlBuilder.addQueryParameter("pageToken", pageToken);
                }

                HttpUrl url = urlBuilder.build();

                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer " + oauthToken)
                        .get()
                        .build();

                Response response = new OkHttpClient().newCall(request).execute();
                if (!response.isSuccessful()) {
                    System.err.println("List failed: " + response.code() + " - " + response.message());
                    response.close();
                    break;
                }

                String responseBody = response.body().string();
                response.close();

                DriveFilesListResponse filesListResponse = new Gson().fromJson(responseBody, DriveFilesListResponse.class);
                if (filesListResponse == null || filesListResponse.files == null) {
                    // No files or unexpected response
                    break;
                }

                // Add all files to our metadata list
                metadataList.addAll(filesListResponse.files);

                pageToken = filesListResponse.nextPageToken;
            } while (pageToken != null);

        } catch (IOException e) {
            e.printStackTrace();
            // In case of error, return the files collected so far or empty list
        }

        return metadataList;
    }

    // Method to resolve path to fileId
    private String getFileIdByPath(String path) throws IOException {
        // Normalize path
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        String[] parts = path.split("/");
        String currentFolderId = "root"; // Start from root

        for (int i = 1; i < parts.length; i++) { // Skip the first empty string
            String name = parts[i];
            if (name.isEmpty()) {
                continue;
            }

            // Escape single quotes in the name
            String escapedName = name.replace("'", "\\'");

            // Search for a child with the given name under the current folder
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

            Response response = httpClient.newCall(request).execute();

            if (response.isSuccessful()) {
                String responseBody = response.body().string();
                DriveFilesListResponse filesListResponse = gson.fromJson(responseBody, DriveFilesListResponse.class);

                if (filesListResponse.files != null && !filesListResponse.files.isEmpty()) {
                    // Assume the first match is the correct one
                    currentFolderId = filesListResponse.files.get(0).id;
                } else {
                    // Item not found
                    return null;
                }
            } else {
                throw new IOException("Failed to retrieve file ID: " + response.code() + " - " + response.message());
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

        Response response = httpClient.newCall(request).execute();
        if (response.isSuccessful()) {
            String responseBody = response.body().string();
            GoogleDriveFileMetadata metadata = gson.fromJson(responseBody, GoogleDriveFileMetadata.class);
            return metadata;
        } else {
            throw new IOException("Failed to get file metadata: " + response.code() + " - " + response.message());
        }
    }

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
            } else {
                return null; // No parent
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String getPath() {
        try {
            return buildPath(metadata);
        } catch (IOException e) {
            e.printStackTrace();
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
                if ("root".equals(parentId)) {
                    return "/";
                } else {
                    GoogleDriveFileMetadata parentMetadata = getFileMetadata(parentId);
                    return buildPath(parentMetadata);
                }
            } else {
                return null; // No parent
            }
        } catch (IOException e) {
            e.printStackTrace();
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
            HttpUrl url = HttpUrl.parse("https://www.googleapis.com/drive/v3/files/" + fileId)
                    .newBuilder()
                    .build();

            Map<String, Object> jsonMap = new HashMap<>();
            jsonMap.put("name", new_name);
            String json = gson.toJson(jsonMap);

            RequestBody body = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .patch(body)
                    .build();

            Response response = httpClient.newCall(request).execute();

            if (response.isSuccessful()) {
                String responseBody = response.body().string();
                metadata = gson.fromJson(responseBody, GoogleDriveFileMetadata.class);
                return true;
            } else {
                System.err.println("Rename failed: " + response.code() + " - " + response.message());
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean delete() {
        try {
            HttpUrl url = HttpUrl.parse("https://www.googleapis.com/drive/v3/files/" + fileId)
                    .newBuilder()
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .delete()
                    .build();

            Response response = httpClient.newCall(request).execute();

            return response.isSuccessful();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public InputStream getInputStream() {
        if (isDirectory()) {
            return null;
        }

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

            if (response.isSuccessful()) {
                // Return the response body InputStream
                return response.body().byteStream();
            } else {
                System.err.println("Download failed: " + response.code() + " - " + response.message());
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public OutputStream getChildOutputStream(String child_name, long source_length) {
        if (!isDirectory()) {
            return null;
        }

        PipedOutputStream outputStream = new PipedOutputStream();
        try {
            PipedInputStream inputStream = new PipedInputStream(outputStream);
            new Thread(() -> {
                try {
                    uploadFile(child_name, inputStream, source_length);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
            return outputStream;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void uploadFile(String fileName, InputStream inputStream, long source_length) throws IOException {
        HttpUrl url = HttpUrl.parse("https://www.googleapis.com/upload/drive/v3/files")
                .newBuilder()
                .addQueryParameter("uploadType", "multipart")
                .build();

        // Metadata
        Map<String, Object> metadataMap = new HashMap<>();
        metadataMap.put("name", fileName);
        metadataMap.put("parents", Collections.singletonList(fileId));
        String metadataJson = gson.toJson(metadataMap);

        // Request bodies
        RequestBody metadataPart = RequestBody.create(metadataJson, MediaType.parse("application/json; charset=UTF-8"));
        RequestBody filePart = new InputStreamRequestBody(inputStream, source_length, MediaType.parse("application/octet-stream"));

        MultipartBody multipartBody = new MultipartBody.Builder()
                .setType(MultipartBody.MIXED)
                .addPart(
                        Headers.of("Content-Type", "application/json; charset=UTF-8"),
                        metadataPart)
                .addPart(
                        Headers.of("Content-Type", "application/octet-stream"),
                        filePart)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + accessToken)
                .post(multipartBody)
                .build();

        Response response = httpClient.newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new IOException("Failed to upload file: " + response.code() + " - " + response.message());
        }
    }

    @Override
    public FileModel[] list() {
        if (!isDirectory()) {
            return new FileModel[0];
        }

        try {
            List<FileModel> fileModels = new ArrayList<>();
            String pageToken = null;

            do {
                HttpUrl.Builder urlBuilder = HttpUrl.parse("https://www.googleapis.com/drive/v3/files")
                        .newBuilder()
                        .addQueryParameter("q", "'" + fileId + "' in parents and trashed = false")
                        .addQueryParameter("fields", "nextPageToken, files(id, name, mimeType, parents)")
                        .addQueryParameter("pageSize", "1000");
                if (pageToken != null) {
                    urlBuilder.addQueryParameter("pageToken", pageToken);
                }
                HttpUrl url = urlBuilder.build();

                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer " + accessToken)
                        .get()
                        .build();

                Response response = httpClient.newCall(request).execute();

                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    DriveFilesListResponse filesListResponse = gson.fromJson(responseBody, DriveFilesListResponse.class);

                    for (GoogleDriveFileMetadata fileMetadata : filesListResponse.files) {
                        fileModels.add(new GoogleDriveFileModel(accessToken, fileMetadata.id));
                    }

                    pageToken = filesListResponse.nextPageToken;

                } else {
                    System.err.println("List failed: " + response.code() + " - " + response.message());
                    break;
                }
            } while (pageToken != null);

            return fileModels.toArray(new FileModel[0]);

        } catch (IOException e) {
            e.printStackTrace();
            return new FileModel[0];
        }
    }

    @Override
    public boolean createFile(String name) {
        if (!isDirectory()) {
            return false;
        }

        try {
            // Metadata
            Map<String, Object> metadataMap = new HashMap<>();
            metadataMap.put("name", name);
            metadataMap.put("parents", Collections.singletonList(fileId));
            String metadataJson = gson.toJson(metadataMap);

            // Empty content
            byte[] emptyContent = new byte[0];
            RequestBody fileContent = RequestBody.create(emptyContent, MediaType.parse("application/octet-stream"));

            // Build request
            HttpUrl url = HttpUrl.parse("https://www.googleapis.com/upload/drive/v3/files")
                    .newBuilder()
                    .addQueryParameter("uploadType", "multipart")
                    .build();

            RequestBody metadataPart = RequestBody.create(metadataJson, MediaType.parse("application/json; charset=UTF-8"));

            MultipartBody multipartBody = new MultipartBody.Builder()
                    .setType(MultipartBody.MIXED)
                    .addPart(
                            Headers.of("Content-Type", "application/json; charset=UTF-8"),
                            metadataPart)
                    .addPart(
                            Headers.of("Content-Type", "application/octet-stream"),
                            fileContent)
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .post(multipartBody)
                    .build();

            Response response = httpClient.newCall(request).execute();

            return response.isSuccessful();

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean makeDirIfNotExists(String dir_name) {
        if (!isDirectory()) {
            return false;
        }

        try {
            // Check if directory exists
            String query = "name = '" + dir_name + "' and '" + fileId + "' in parents and mimeType = 'application/vnd.google-apps.folder' and trashed = false";

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

            Response response = httpClient.newCall(request).execute();

            if (response.isSuccessful()) {
                String responseBody = response.body().string();
                DriveFilesListResponse filesListResponse = gson.fromJson(responseBody, DriveFilesListResponse.class);

                if (filesListResponse.files != null && !filesListResponse.files.isEmpty()) {
                    // Directory exists
                    return true;
                } else {
                    // Create directory
                    Map<String, Object> metadataMap = new HashMap<>();
                    metadataMap.put("name", dir_name);
                    metadataMap.put("parents", Collections.singletonList(fileId));
                    metadataMap.put("mimeType", "application/vnd.google-apps.folder");
                    String metadataJson = gson.toJson(metadataMap);

                    RequestBody body = RequestBody.create(metadataJson, MediaType.parse("application/json; charset=UTF-8"));

                    HttpUrl createUrl = HttpUrl.parse("https://www.googleapis.com/drive/v3/files")
                            .newBuilder()
                            .build();

                    Request createRequest = new Request.Builder()
                            .url(createUrl)
                            .addHeader("Authorization", "Bearer " + accessToken)
                            .post(body)
                            .build();

                    Response createResponse = httpClient.newCall(createRequest).execute();

                    return createResponse.isSuccessful();
                }
            } else {
                System.err.println("Check directory failed: " + response.code() + " - " + response.message());
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean makeDirsRecursively(String extended_path) {
        if (!isDirectory()) {
            return false;
        }

        try {
            String[] paths = extended_path.split("/");
            String parentId = fileId;

            for (String dirName : paths) {
                if (dirName.isEmpty()) {
                    continue;
                }

                // Check if directory exists
                String query = "name = '" + dirName + "' and '" + parentId + "' in parents and mimeType = 'application/vnd.google-apps.folder' and trashed = false";

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

                Response response = httpClient.newCall(request).execute();

                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    DriveFilesListResponse filesListResponse = gson.fromJson(responseBody, DriveFilesListResponse.class);

                    if (filesListResponse.files != null && !filesListResponse.files.isEmpty()) {
                        // Directory exists
                        parentId = filesListResponse.files.get(0).id;
                    } else {
                        // Create directory
                        Map<String, Object> metadataMap = new HashMap<>();
                        metadataMap.put("name", dirName);
                        metadataMap.put("parents", Collections.singletonList(parentId));
                        metadataMap.put("mimeType", "application/vnd.google-apps.folder");
                        String metadataJson = gson.toJson(metadataMap);

                        RequestBody body = RequestBody.create(metadataJson, MediaType.parse("application/json; charset=UTF-8"));

                        HttpUrl createUrl = HttpUrl.parse("https://www.googleapis.com/drive/v3/files")
                                .newBuilder()
                                .build();

                        Request createRequest = new Request.Builder()
                                .url(createUrl)
                                .addHeader("Authorization", "Bearer " + accessToken)
                                .post(body)
                                .build();

                        Response createResponse = httpClient.newCall(createRequest).execute();

                        if (createResponse.isSuccessful()) {
                            String createResponseBody = createResponse.body().string();
                            GoogleDriveFileMetadata createdFolder = gson.fromJson(createResponseBody, GoogleDriveFileMetadata.class);
                            parentId = createdFolder.id;
                        } else {
                            System.err.println("Create directory failed: " + createResponse.code() + " - " + createResponse.message());
                            return false;
                        }
                    }
                } else {
                    System.err.println("Check directory failed: " + response.code() + " - " + response.message());
                    return false;
                }
            }

            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public long getLength() {
        if (isDirectory()) {
            return 0;
        }
        if (metadata.size != null) {
            return metadata.size;
        } else {
            return 0;
        }
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

                // Handle time zone designator
                if (modifiedTime.endsWith("Z")) {
                    // 'Z' indicates UTC, replace it with '+0000' for SimpleDateFormat
                    modifiedTime = modifiedTime.substring(0, modifiedTime.length() - 1) + "+0000";
                } else if (modifiedTime.matches(".*[\\+\\-]\\d\\d:\\d\\d$")) {
                    // Convert timezone from +hh:mm or -hh:mm to +hhmm or -hhmm
                    modifiedTime = modifiedTime.substring(0, modifiedTime.length() - 3) + modifiedTime.substring(modifiedTime.length() - 2);
                }

                // Define the date format pattern
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
                sdf.setLenient(true);

                Date date = sdf.parse(modifiedTime);
                return date.getTime();
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
        } else {
            return 0;
        }
    }

    @Override
    public boolean isHidden() {
        return metadata.name != null && metadata.name.startsWith(".");
    }

    // Helper classes
    public static class GoogleDriveFileMetadata {
        public String id;
        public String name;
        String mimeType;
        Long size;
        String modifiedTime;
        List<String> parents;
        // Other fields as needed...
    }

    public static class DriveFilesListResponse {
        public List<GoogleDriveFileMetadata> files;
        @SerializedName("nextPageToken")
        String nextPageToken;
    }

    // Custom RequestBody to read from InputStream
    public static class InputStreamRequestBody extends RequestBody {
        private final MediaType contentType;
        private final InputStream inputStream;
        private final long contentLength;

        public InputStreamRequestBody(InputStream inputStream, long contentLength, MediaType contentType) {
            this.inputStream = inputStream;
            this.contentLength = contentLength;
            this.contentType = contentType;
        }

        @Override
        public MediaType contentType() {
            return contentType;
        }

        @Override
        public long contentLength() {
            return contentLength; // Return the known content length if known
        }

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
