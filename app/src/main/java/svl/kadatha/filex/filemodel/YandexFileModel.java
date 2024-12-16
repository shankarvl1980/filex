package svl.kadatha.filex.filemodel;

import android.util.Log;

import com.google.gson.Gson;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
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
import svl.kadatha.filex.MakeCloudFilePOJOUtil;

public class YandexFileModel implements FileModel {
    private static final String TAG = "YandexFileModel";
    private static final String BASE_URL = "https://cloud-api.yandex.net/v1/disk/resources";
    private static final String DOWNLOAD_URL = "https://cloud-api.yandex.net/v1/disk/resources/download";
    private static final String UPLOAD_URL = "https://cloud-api.yandex.net/v1/disk/resources/upload";
    private static final String MOVE_URL = "https://cloud-api.yandex.net/v1/disk/resources/move";
    // (Optional) For parsing RFC3339 dates if you need lastModified():
    private static final SimpleDateFormat RFC3339_FORMAT;

    static {
        RFC3339_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        RFC3339_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private final String accessToken;
    private final String path; // Yandex Disk path, e.g., "/Folder/File.txt"
    private final OkHttpClient client;
    private final Gson gson = new Gson();
    private MakeCloudFilePOJOUtil.YandexResource metadata;

    public YandexFileModel(String accessToken, String path) throws IOException {
        this.accessToken = accessToken;
        this.path = path;
        this.client = new OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        this.metadata = fetchMetadata(path);
        if (this.metadata == null) {
            throw new FileNotFoundException("Metadata not found for path: " + path);
        }
    }

    private MakeCloudFilePOJOUtil.YandexResource fetchMetadata(String path) throws IOException {
        HttpUrl url = HttpUrl.parse(BASE_URL)
                .newBuilder()
                .addQueryParameter("path", path.equals("/") ? "/" : path)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "OAuth " + accessToken)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return gson.fromJson(response.body().charStream(), MakeCloudFilePOJOUtil.YandexResource.class);
            } else {
                Log.e(TAG, "Failed to fetch metadata: " + response.code() + " " + response.message());
                return null;
            }
        }
    }

    @Override
    public String getName() {
        return metadata.name;
    }

    @Override
    public String getParentName() {
        String parentPath = getParentPath();
        if (parentPath != null && !parentPath.equals("/")) {
            try {
                MakeCloudFilePOJOUtil.YandexResource parentMetadata = fetchMetadata(parentPath);
                if (parentMetadata != null) {
                    return parentMetadata.name;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getParentPath() {
        if (path.equals("/")) {
            return null;
        }
        int lastSlashIndex = path.lastIndexOf('/');
        if (lastSlashIndex == 0) {
            return "/";
        } else if (lastSlashIndex > 0) {
            return path.substring(0, lastSlashIndex);
        } else {
            return null;
        }
    }

    @Override
    public boolean isDirectory() {
        return metadata.isDir();
    }

    @Override
    public boolean rename(String new_name, boolean overwrite) {
        String parentPath = getParentPath();
        if (parentPath == null) {
            parentPath = "/";
        }
        String newPath = parentPath.equals("/") ? "/" + new_name : parentPath + "/" + new_name;

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
            if (response.isSuccessful()) {
                metadata = fetchMetadata(newPath);
                return true;
            } else {
                Log.e(TAG, "Rename failed: " + response.code() + " " + response.message());
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean delete() {
        HttpUrl url = HttpUrl.parse(BASE_URL)
                .newBuilder()
                .addQueryParameter("path", path)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "OAuth " + accessToken)
                .delete()
                .build();

        try (Response response = client.newCall(request).execute()) {
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
            if (dRes.isSuccessful() && dRes.body() != null) {
                MakeCloudFilePOJOUtil.YandexDownloadResponse dlResp = gson.fromJson(dRes.body().charStream(), MakeCloudFilePOJOUtil.YandexDownloadResponse.class);
                dRes.close();
                if (dlResp != null && dlResp.href != null) {
                    Request fileReq = new Request.Builder().url(dlResp.href).get().build();
                    Response fileRes = client.newCall(fileReq).execute();
                    if (fileRes.isSuccessful() && fileRes.body() != null) {
                        return fileRes.body().byteStream();
                        // Don't close fileRes here, caller must close stream
                    } else {
                        if (fileRes != null) {
                            fileRes.close();
                        }
                    }
                }
            } else {
                if (dRes != null) {
                    dRes.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public OutputStream getChildOutputStream(String child_name, long source_length) {
        if (!isDirectory()) {
            return null;
        }
        String childPath = path.equals("/") ? "/" + child_name : path + "/" + child_name;

        // Get upload link
        HttpUrl uUrl = HttpUrl.parse(UPLOAD_URL)
                .newBuilder()
                .addQueryParameter("path", childPath)
                .addQueryParameter("overwrite", "false")
                .build();

        Request uReq = new Request.Builder()
                .url(uUrl)
                .header("Authorization", "OAuth " + accessToken)
                .get()
                .build();

        try (Response uRes = client.newCall(uReq).execute()) {
            if (uRes.isSuccessful() && uRes.body() != null) {
                MakeCloudFilePOJOUtil.YandexUploadResponse uplResp = gson.fromJson(uRes.body().charStream(), MakeCloudFilePOJOUtil.YandexUploadResponse.class);
                if (uplResp != null && uplResp.href != null) {
                    PipedOutputStream outputStream = new PipedOutputStream();
                    PipedInputStream inputStream = new PipedInputStream(outputStream);

                    new Thread(() -> {
                        try {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            byte[] buffer = new byte[8192];
                            int len;
                            while ((len = inputStream.read(buffer)) != -1) {
                                baos.write(buffer, 0, len);
                            }
                            byte[] data = baos.toByteArray();

                            RequestBody uploadBody = RequestBody.create(data, MediaType.parse("application/octet-stream"));
                            Request putReq = new Request.Builder().url(uplResp.href).put(uploadBody).build();
                            try (Response putRes = client.newCall(putReq).execute()) {
                                if (!putRes.isSuccessful()) {
                                    Log.e(TAG, "Upload failed: " + putRes.code() + " " + putRes.message());
                                }
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }).start();

                    return outputStream;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public FileModel[] list() {
        if (!isDirectory()) {
            return new FileModel[0];
        }

        HttpUrl url = HttpUrl.parse(BASE_URL)
                .newBuilder()
                .addQueryParameter("path", path.equals("/") ? "/" : path)
                .addQueryParameter("fields", "_embedded.items.name,_embedded.items.type,_embedded.items.size,_embedded.items.modified,_embedded.items.path")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "OAuth " + accessToken)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                MakeCloudFilePOJOUtil.YandexResource dirRes = gson.fromJson(response.body().charStream(), MakeCloudFilePOJOUtil.YandexResource.class);
                if (dirRes != null && dirRes._embedded != null && dirRes._embedded.items != null) {
                    List<FileModel> fileModels = new ArrayList<>();
                    for (MakeCloudFilePOJOUtil.YandexResource item : dirRes._embedded.items) {
                        // Convert "disk:/..." to "/..."
                        String itemPath = item.path.replaceFirst("^disk:", "");
                        fileModels.add(new YandexFileModel(accessToken, itemPath));
                    }
                    return fileModels.toArray(new FileModel[0]);
                }
            } else {
                Log.e(TAG, "List directory failed: " + (response != null ? response.code() : "no response"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new FileModel[0];
    }

    @Override
    public boolean createFile(String name) {
        if (!isDirectory()) {
            return false;
        }
        String filePath = path.equals("/") ? "/" + name : path + "/" + name;
        // Create empty file by uploading empty content
        return uploadFileContent(filePath, new ByteArrayInputStream(new byte[0]));
    }

    private boolean uploadFileContent(String filePath, InputStream input) {
        HttpUrl uUrl = HttpUrl.parse(UPLOAD_URL)
                .newBuilder()
                .addQueryParameter("path", filePath)
                .addQueryParameter("overwrite", "false")
                .build();

        Request uReq = new Request.Builder()
                .url(uUrl)
                .header("Authorization", "OAuth " + accessToken)
                .get()
                .build();

        try (Response uRes = client.newCall(uReq).execute()) {
            if (!uRes.isSuccessful() || uRes.body() == null) {
                Log.e(TAG, "Get upload URL failed: " + (uRes != null ? uRes.code() : "no response"));
                return false;
            }
            MakeCloudFilePOJOUtil.YandexUploadResponse uplResp = gson.fromJson(uRes.body().charStream(), MakeCloudFilePOJOUtil.YandexUploadResponse.class);
            if (uplResp != null && uplResp.href != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int len;
                while ((len = input.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
                byte[] data = baos.toByteArray();

                RequestBody uploadBody = RequestBody.create(data, MediaType.parse("application/octet-stream"));
                Request putReq = new Request.Builder().url(uplResp.href).put(uploadBody).build();

                try (Response putRes = client.newCall(putReq).execute()) {
                    return putRes.isSuccessful();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean makeDirIfNotExists(String dir_name) {
        if (!isDirectory()) {
            return false;
        }
        String dirPath = path.equals("/") ? "/" + dir_name : path + "/" + dir_name;

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
            if (response.isSuccessful()) {
                return true;
            } else if (response.code() == 409) {
                // Folder already exists
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean makeDirsRecursively(String extended_path) {
        if (!isDirectory()) {
            return false;
        }
        String[] dirs = extended_path.split("/");
        String currentPath = path;
        for (String dir : dirs) {
            if (dir.isEmpty()) {
                continue;
            }
            String newPath = currentPath.equals("/") ? "/" + dir : currentPath + "/" + dir;
            if (!makeDirIfNotExists(dir)) {
                return false;
            }
            currentPath = newPath; // update current path
        }
        return true;
    }

    @Override
    public long getLength() {
        if (isDirectory()) {
            return 0;
        }
        return metadata.size != null ? metadata.size : 0;
    }

    @Override
    public boolean exists() {
        try {
            MakeCloudFilePOJOUtil.YandexResource m = fetchMetadata(path);
            return m != null;
        } catch (IOException e) {
            // If fetching fails, consider not found
            return false;
        }
    }

    @Override
    public long lastModified() {
        if (isDirectory()) {
            return 0;
        }
        if (metadata.modified != null) {
            try {
                Date d = RFC3339_FORMAT.parse(metadata.modified);
                if (d != null) {
                    return d.getTime();
                }
            } catch (Exception e) {
                // Parsing failed, return 0
            }
        }
        return 0;
    }

    @Override
    public boolean isHidden() {
        return getName().startsWith(".");
    }


}
