package svl.kadatha.filex;

import static svl.kadatha.filex.MakeFilePOJOUtil.EXTRACT_ICON;
import static svl.kadatha.filex.MakeFilePOJOUtil.GET_FILE_TYPE;

import android.view.View;

import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.Metadata;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class MakeCloudFilePOJOUtil {


    static FilePOJO MAKE_FilePOJO_FromDriveAPI(String file_path, boolean extract_icon, FileObjectType fileObjectType, String oauthToken) throws IOException {

        // Handle the root folder case
        if (file_path.equals("/")) {
            // Create a FilePOJO representing the root folder
            return new FilePOJO(
                    fileObjectType,
                    "/",            // name
                    null,           // package_name (not needed)
                    "/",            // path
                    true,           // isDirectory
                    0L,             // dateLong
                    null,           // date (no modification time for root)
                    0L,             // sizeLong
                    null,           // si
                    R.drawable.folder_icon, // type (folder)
                    null,           // file_ext
                    Global.ENABLE_ALFA,
                    View.INVISIBLE,
                    0,
                    0L,
                    null,
                    0,
                    null,
                    null
            );
        }

        // For non-root paths:
        String fileName = new File(file_path).getName();
        String query = "name='" + fileName.replace("'", "\\'") + "'";
        String fields = "files(id,name,mimeType,modifiedTime,size)";

        String encodedQuery = java.net.URLEncoder.encode(query, "UTF-8");
        String url = "https://www.googleapis.com/drive/v3/files?q=" + encodedQuery + "&fields=" + fields;

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + oauthToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Drive API request failed: " + response.code() + " - " + response.message());
            }

            String responseBody = response.body().string();
            DriveFileListResponse listResponse = new Gson().fromJson(responseBody, DriveFileListResponse.class);

            if (listResponse.files == null || listResponse.files.isEmpty()) {
                // No file found matching the given name
                return null;
            }

            // Take the first matching file
            DriveFileMetadata meta = listResponse.files.get(0);

            String name = meta.name;
            boolean isDirectory = "application/vnd.google-apps.folder".equals(meta.mimeType);
            long dateLong = 0L;
            String date = "";

            if (meta.modifiedTime != null) {
                try {
                    Date d = MakeFilePOJOUtil.SDF_FTP.parse(meta.modifiedTime);
                    date = Global.SDF.format(d);
                } catch (ParseException ignored) {
                }
            }

            long sizeLong = 0L;
            String si = "";
            String file_ext = "";
            int overlay_visible = View.INVISIBLE;
            float alfa = Global.ENABLE_ALFA;
            String package_name = null;
            int type = R.drawable.folder_icon;

            if (!isDirectory) {
                type = R.drawable.unknown_file_icon;
                int idx = name.lastIndexOf(".");
                if (idx != -1) {
                    file_ext = name.substring(idx + 1);
                    type = GET_FILE_TYPE(false, file_ext);
                    if (type == -2) {
                        overlay_visible = View.VISIBLE;
                    } else if (extract_icon && type == 0) {
                        package_name = EXTRACT_ICON(file_path, file_ext);
                    }
                }
                if (meta.size != null) {
                    sizeLong = meta.size;
                    si = FileUtil.humanReadableByteCount(sizeLong);
                }
            } else {
                // For directories, if you need sub-file count, do another request.
            }

            return new FilePOJO(
                    fileObjectType,
                    name,
                    package_name,
                    file_path,
                    isDirectory,
                    dateLong,
                    date,
                    sizeLong,
                    si,
                    type,
                    file_ext,
                    alfa,
                    overlay_visible,
                    0,
                    0L,
                    null,
                    0,
                    null,
                    null
            );
        }
    }


    static FilePOJO MAKE_FilePOJO(Metadata metadata, boolean extract_icon, FileObjectType fileObjectType, String file_path, DbxClientV2 dbxClient) {
        String name = metadata.getName();
        boolean isDirectory = (metadata instanceof FolderMetadata);
        long dateLong = 0L;
        String date = "";
        long sizeLong = 0L;
        String si = "";
        String file_ext = "";
        int overlay_visible = View.INVISIBLE;
        float alfa = Global.ENABLE_ALFA;
        String package_name = null;
        int type = R.drawable.folder_icon;

        if (!isDirectory && metadata instanceof FileMetadata) {
            FileMetadata fMeta = (FileMetadata) metadata;
            type = R.drawable.unknown_file_icon;
            int idx = name.lastIndexOf(".");
            if (idx != -1) {
                file_ext = name.substring(idx + 1);
                type = GET_FILE_TYPE(false, file_ext);
                if (type == -2) {
                    overlay_visible = View.VISIBLE;
                } else if (extract_icon && type == 0) {
                    package_name = EXTRACT_ICON(file_path, file_ext);
                }
            }

            sizeLong = fMeta.getSize();
            si = FileUtil.humanReadableByteCount(sizeLong);

            Date d = fMeta.getServerModified();
            date = Global.SDF.format(d);

        } else if (isDirectory) {
            // Optional: count items in the folder
            // Careful: This may be an expensive operation
            // ListFolderResult res = dbxClient.files().listFolder(file_path);
            // int count = res.getEntries().size();
            // si = "(" + count + ")";
        }

        return new FilePOJO(fileObjectType, name, package_name, file_path, isDirectory, dateLong, date, sizeLong, si, type, file_ext, alfa, overlay_visible, 0, 0L, null, 0, null, null);
    }

    static class DriveFileMetadata {
        String id;
        String name;
        String mimeType;
        @SerializedName("modifiedTime")
        String modifiedTime; // in RFC3339 format, e.g. "2023-11-02T10:20:30.000Z"
        Long size;
    }

    static class DriveFileListResponse {
        List<DriveFileMetadata> files;
    }
}
