package svl.kadatha.filex;

import static svl.kadatha.filex.MakeFilePOJOUtil.EXTRACT_ICON;
import static svl.kadatha.filex.MakeFilePOJOUtil.GET_FILE_TYPE;

import android.view.View;

import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.Metadata;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import svl.kadatha.filex.filemodel.GoogleDriveFileModel;

/**
 * Drop-in MakeCloudFilePOJOUtil
 * <p>
 * Key upgrades:
 * 1) Drive: build FilePOJO directly from already-listed metadata (ID-based)
 * - sets cloudId, parentCloudId, driveMimeType
 * - avoids the unsafe "query by name" problem for listing flows
 * <p>
 * 2) Drive root POJO includes cloudId="root"
 * <p>
 * 3) Dropbox: opportunistically stores Dropbox id (if available)
 * <p>
 * 4) Yandex: path-based, no stable id stored (fields left null)
 */
public class MakeCloudFilePOJOUtil {

    // ---------------------------------------------------------------------
    // ✅ DRIVE: Preferred path (NO network): from Drive metadata
    // ---------------------------------------------------------------------
    static FilePOJO MAKE_FilePOJO_FromDriveMeta(GoogleDriveFileModel.GoogleDriveFileMetadata meta,
                                                String displayPath,
                                                boolean extract_icon,
                                                FileObjectType fileObjectType) {
        if (meta == null) return null;

        // Root display (if you ever pass it here)
        if ("/".equals(displayPath) || displayPath == null) {
            return MAKE_DriveRootPOJO(fileObjectType);
        }

        String name = meta.name != null ? meta.name : "";
        String mimeType = meta.mimeType;
        boolean isDirectory = "application/vnd.google-apps.folder".equals(mimeType);

        long dateLong = 0L;
        String date = "";

        // NOTE: meta.modifiedTime is RFC3339; your app uses SDF_FTP in some places.
        // Keeping your existing approach to avoid bigger refactor.
        if (meta.modifiedTime != null) {
            CloudDateUtil.DatePair dp = CloudDateUtil.toDatePair(meta.modifiedTime);
            dateLong = dp.epochMillis;
            date = dp.ui;
        }

        long sizeLong = 0L;
        String si = "";
        String file_ext = "";
        int play_overlay_visible = View.INVISIBLE;
        int pdf_overlay_visible = View.INVISIBLE;
        float alfa = Global.ENABLE_ALFA;
        String package_name = null;
        int type = R.drawable.folder_icon;

        if (!isDirectory) {
            type = R.drawable.unknown_file_icon;

            int idx = name.lastIndexOf(".");
            if (idx > 0) {
                file_ext = name.substring(idx + 1);
                type = GET_FILE_TYPE(false, file_ext);
                if (type == -2) {
                    play_overlay_visible = View.VISIBLE;
                } else if (type == -3) {
                    pdf_overlay_visible = View.VISIBLE;
                } else if (extract_icon && type == 0) {
                    package_name = EXTRACT_ICON(displayPath, file_ext);
                }
            }

            if (meta.size != null) {
                sizeLong = meta.size;
                si = FileUtil.humanReadableByteCount(sizeLong);
            }
        }

        FilePOJO pojo = new FilePOJO(fileObjectType, name, package_name, displayPath, isDirectory, dateLong, date, sizeLong, si, type, file_ext, alfa, play_overlay_visible, pdf_overlay_visible);

        // ✅ Store identity fields
        pojo.setCloudId(meta.id);
        if (meta.parents != null && !meta.parents.isEmpty()) {
            pojo.setParentCloudId(meta.parents.get(0));
        }
        pojo.setDriveMimeType(mimeType);

        return pojo;
    }

    static FilePOJO MAKE_DriveRootPOJO(FileObjectType fileObjectType) {
        FilePOJO root = new FilePOJO(fileObjectType, "/", null, "/", true, 0L, null, 0L, null, R.drawable.folder_icon, null, Global.ENABLE_ALFA, View.INVISIBLE, View.INVISIBLE);

        root.setCloudId("root");
        root.setParentCloudId(null);
        root.setDriveMimeType("application/vnd.google-apps.folder");
        return root;
    }

    // ---------------------------------------------------------------------
    // ⚠️ DRIVE: Legacy slow/unsafe fallback (network) from a *path* string
    // - Keep only for "user typed path / jump to path"
    // - Not recommended for listing flows because it queries only by name.
    // ---------------------------------------------------------------------
    static FilePOJO MAKE_FilePOJO_FromDriveAPI(String file_path,
                                               boolean extract_icon,
                                               FileObjectType fileObjectType,
                                               String oauthToken) throws IOException {

        if (file_path == null || file_path.equals("/")) {
            return MAKE_DriveRootPOJO(fileObjectType);
        }

        String fileName = new File(file_path).getName();
        String query = "name='" + fileName.replace("'", "\\'") + "'";
        String fields = "files(id,name,mimeType,modifiedTime,size,parents)";

        String encodedQuery = java.net.URLEncoder.encode(query, "UTF-8");
        String url = "https://www.googleapis.com/drive/v3/files?q=" + encodedQuery + "&fields=" + fields;

        OkHttpClient client = Global.HTTP;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + oauthToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Drive API request failed: " + response.code() + " - " + response.message());
            }

            String responseBody = response.body() != null ? response.body().string() : null;
            if (responseBody == null) return null;

            DriveFileListResponse listResponse = new Gson().fromJson(responseBody, DriveFileListResponse.class);
            if (listResponse == null || listResponse.files == null || listResponse.files.isEmpty()) {
                return null;
            }

            DriveFileMetadata meta = listResponse.files.get(0);

            // Build POJO
            String name = meta.name;
            boolean isDirectory = "application/vnd.google-apps.folder".equals(meta.mimeType);

            long dateLong = 0L;
            String date = "";
            if (meta.modifiedTime != null) {
                CloudDateUtil.DatePair dp = CloudDateUtil.toDatePair(meta.modifiedTime);
                dateLong = dp.epochMillis;
                date = dp.ui;
            }

            long sizeLong = 0L;
            String si = "";
            String file_ext = "";
            int play_overlay_visible = View.INVISIBLE;
            int pdf_overlay_visible = View.INVISIBLE;
            float alfa = Global.ENABLE_ALFA;
            String package_name = null;
            int type = R.drawable.folder_icon;

            if (!isDirectory) {
                type = R.drawable.unknown_file_icon;
                int idx = name != null ? name.lastIndexOf(".") : -1;
                if (idx > 0) {
                    file_ext = name.substring(idx + 1);
                    type = GET_FILE_TYPE(false, file_ext);
                    if (type == -2) {
                        play_overlay_visible = View.VISIBLE;
                    } else if (type == -3) {
                        pdf_overlay_visible = View.VISIBLE;
                    } else if (extract_icon && type == 0) {
                        package_name = EXTRACT_ICON(file_path, file_ext);
                    }
                }

                if (meta.size != null) {
                    sizeLong = meta.size;
                    si = FileUtil.humanReadableByteCount(sizeLong);
                }
            }

            FilePOJO pojo = new FilePOJO(
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
                    play_overlay_visible,
                    pdf_overlay_visible
            );

            // ✅ store identity
            pojo.setCloudId(meta.id);
            if (meta.parents != null && !meta.parents.isEmpty()) {
                pojo.setParentCloudId(meta.parents.get(0));
            }
            pojo.setDriveMimeType(meta.mimeType);

            return pojo;
        }
    }

    // ---------------------------------------------------------------------
    // DROPBOX (opportunistic cloudId)
    // ---------------------------------------------------------------------
    static FilePOJO MAKE_FilePOJO(Metadata metadata,
                                  boolean extract_icon,
                                  FileObjectType fileObjectType,
                                  String file_path) {

        String name = metadata.getName();
        boolean isDirectory = (metadata instanceof FolderMetadata);

        long dateLong = 0L;
        String date = "";
        long sizeLong = 0L;
        String si = "";
        String file_ext = "";
        int play_overlay_visible = View.INVISIBLE;
        int pdf_overlay_visible = View.INVISIBLE;
        float alfa = Global.ENABLE_ALFA;
        String package_name = null;
        int type = R.drawable.folder_icon;

        if (!isDirectory && metadata instanceof FileMetadata) {
            FileMetadata fMeta = (FileMetadata) metadata;
            type = R.drawable.unknown_file_icon;

            int idx = name.lastIndexOf(".");
            if (idx > 0) {
                file_ext = name.substring(idx + 1);
                type = GET_FILE_TYPE(false, file_ext);
                if (type == -2) {
                    play_overlay_visible = View.VISIBLE;
                } else if (type == -3) {
                    pdf_overlay_visible = View.VISIBLE;
                } else if (extract_icon && type == 0) {
                    package_name = EXTRACT_ICON(file_path, file_ext);
                }
            }

            sizeLong = fMeta.getSize();
            si = FileUtil.humanReadableByteCount(sizeLong);

            Date d = fMeta.getServerModified();
            if (d != null) {
                dateLong = d.getTime();
                date = Global.SDF.format(d);
            }
        }

        FilePOJO pojo = new FilePOJO(fileObjectType, name, package_name, file_path, isDirectory,
                dateLong, date, sizeLong, si, type, file_ext, alfa, play_overlay_visible, pdf_overlay_visible);

        // ✅ opportunistic id (if present in your SDK version)
        try {
            String id = null;
            if (metadata instanceof FileMetadata) id = ((FileMetadata) metadata).getId();
            else if (metadata instanceof FolderMetadata) id = ((FolderMetadata) metadata).getId();
            pojo.setCloudId(id);
        } catch (Exception ignored) {
        }

        // parent id not typically needed for Dropbox (path-based)
        return pojo;
    }

    // ---------------------------------------------------------------------
    // YANDEX (path-based; no stable id captured)
    // ---------------------------------------------------------------------
    static FilePOJO MAKE_FilePOJO(YandexResource resource,
                                  boolean extract_icon,
                                  FileObjectType fileObjectType,
                                  String file_path) {

        String name = resource.name;
        boolean isDirectory = "dir".equalsIgnoreCase(resource.type);

        long dateLong = 0L;
        String date = "";
        long sizeLong = 0L;
        String si = "";
        String file_ext = "";
        int play_overlay_visible = View.INVISIBLE;
        int pdf_overlay_visible = View.INVISIBLE;
        float alfa = Global.ENABLE_ALFA;
        String package_name = null;
        int type;

        if (!isDirectory) {
            type = R.drawable.unknown_file_icon;

            int idx = name.lastIndexOf(".");
            if (idx > 0 && idx < name.length() - 1) {
                file_ext = name.substring(idx + 1);
                type = GET_FILE_TYPE(false, file_ext);
                if (type == -2) {
                    play_overlay_visible = View.VISIBLE;
                } else if (type == -3) {
                    pdf_overlay_visible = View.VISIBLE;
                } else if (extract_icon && type == 0) {
                    package_name = EXTRACT_ICON(file_path, file_ext);
                }
            }

            if (resource.size != null) {
                sizeLong = resource.size;
                si = FileUtil.humanReadableByteCount(sizeLong);
            }

            if (resource.modified != null) {
                CloudDateUtil.DatePair dp = CloudDateUtil.toDatePair(resource.modified);
                dateLong = dp.epochMillis;
                date = dp.ui;

            }
        } else {
            type = R.drawable.folder_icon;
        }

        FilePOJO pojo = new FilePOJO(
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
                play_overlay_visible,
                pdf_overlay_visible
        );

        // Yandex: no stable item ID used across operations in your current flow.
        // Keep cloud fields null.
        return pojo;
    }

//    static FilePOJO MAKE_OneDriveRootPOJO(FileObjectType fileObjectType) {
//        FilePOJO root = new FilePOJO(
//                fileObjectType,
//                "/",
//                null,
//                "/",
//                true,
//                0L,
//                null,
//                0L,
//                null,
//                R.drawable.folder_icon,
//                null,
//                Global.ENABLE_ALFA,
//                View.INVISIBLE,
//                View.INVISIBLE
//        );
//
//        // OneDrive root has a real id, but unless you fetch it at login,
//        // keep this stable marker. If you DO have root id, set it here.
//        root.setCloudId("root");
//        root.setParentCloudId(null);
//        return root;
//    }
//
//    static FilePOJO MAKE_FilePOJO_FromOneDriveModel(OneDriveFileModel model,
//                                                    String displayPath,
//                                                    boolean extract_icon,
//                                                    FileObjectType fileObjectType) {
//
//        if (model == null) return null;
//
//        DriveItem item = model.getDriveItemUnsafe();
//        if (item == null) {
//            // Fallback: at least create a minimal directory/file shell
//            String nameFallback = new File(displayPath).getName();
//            boolean isDirFallback = model.isDirectory();
//            FilePOJO p = new FilePOJO(
//                    fileObjectType,
//                    nameFallback,
//                    null,
//                    displayPath,
//                    isDirFallback,
//                    0L,
//                    null,
//                    0L,
//                    null,
//                    isDirFallback ? R.drawable.folder_icon : R.drawable.unknown_file_icon,
//                    null,
//                    Global.ENABLE_ALFA,
//                    View.INVISIBLE,
//                    View.INVISIBLE
//            );
//            return p;
//        }
//
//        return MAKE_FilePOJO_FromOneDriveDriveItem(item, displayPath, extract_icon, fileObjectType);
//    }

    // ---------------------------------------------------------------------
    // Helper DTOs (Drive + Yandex)
    // ---------------------------------------------------------------------
    static class DriveFileMetadata {
        @SerializedName("id")
        String id;
        @SerializedName("name")
        String name;
        @SerializedName("mimeType")
        String mimeType;
        @SerializedName("modifiedTime")
        String modifiedTime; // RFC3339
        @SerializedName("size")
        Long size;
        @SerializedName("parents")
        List<String> parents;
    }

    static class DriveFileListResponse {
        @SerializedName("files")
        List<DriveFileMetadata> files;
    }

    static class YandexResourceListResponse {
        @SerializedName("items")
        List<YandexResource> items;
    }

    public static class YandexDownloadResponse {
        @SerializedName("href")
        public String href;
        @SerializedName("method")
        String method;
        @SerializedName("templated")
        boolean templated;
    }

    public static class YandexUploadResponse {
        @SerializedName("href")
        public String href;
        @SerializedName("method")
        String method;
        @SerializedName("templated")
        boolean templated;
    }

    static final class CloudDateUtil {

        private static final ThreadLocal<java.text.SimpleDateFormat> RFC3339_MILLIS =
                new ThreadLocal<java.text.SimpleDateFormat>() {
                    @Override
                    protected java.text.SimpleDateFormat initialValue() {
                        java.text.SimpleDateFormat f =
                                new java.text.SimpleDateFormat(
                                        "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                                        java.util.Locale.US
                                );
                        f.setLenient(true);
                        f.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                        return f;
                    }
                };

        private static final ThreadLocal<java.text.SimpleDateFormat> RFC3339_NO_MILLIS =
                new ThreadLocal<java.text.SimpleDateFormat>() {
                    @Override
                    protected java.text.SimpleDateFormat initialValue() {
                        java.text.SimpleDateFormat f =
                                new java.text.SimpleDateFormat(
                                        "yyyy-MM-dd'T'HH:mm:ssZ",
                                        java.util.Locale.US
                                );
                        f.setLenient(true);
                        f.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                        return f;
                    }
                };

        private CloudDateUtil() {
        }

        static java.util.Date parseRfc3339(String s) {
            if (s == null) return null;
            s = s.trim();
            if (s.isEmpty()) return null;

            // Normalize RFC3339 timezone to SimpleDateFormat-friendly form
            // 2024-01-01T10:20:30.123Z      -> Z
            // 2024-01-01T10:20:30.123+05:30 -> +0530
            s = normalizeTimezone(s);

            try {
                if (s.indexOf('.') >= 0) {
                    return RFC3339_MILLIS.get().parse(s);
                }
                return RFC3339_NO_MILLIS.get().parse(s);
            } catch (Exception e) {
                // Defensive fallback
                try {
                    return RFC3339_MILLIS.get().parse(s);
                } catch (Exception ignored) {
                }
                try {
                    return RFC3339_NO_MILLIS.get().parse(s);
                } catch (Exception ignored) {
                }
                return null;
            }
        }

        private static String normalizeTimezone(String s) {
            // Handle trailing 'Z'
            if (s.endsWith("Z")) {
                return s.substring(0, s.length() - 1) + "+0000";
            }

            // Handle +HH:MM or -HH:MM
            int len = s.length();
            if (len >= 6) {
                char sign = s.charAt(len - 6);
                if ((sign == '+' || sign == '-') && s.charAt(len - 3) == ':') {
                    return s.substring(0, len - 6)
                            + sign
                            + s.substring(len - 5, len - 3)
                            + s.substring(len - 2);
                }
            }
            return s;
        }

        static DatePair toDatePair(String rfc3339) {
            java.util.Date d = parseRfc3339(rfc3339);
            if (d == null) return new DatePair(0L, "");
            return new DatePair(d.getTime(), Global.SDF.format(d));
        }

        static final class DatePair {
            final long epochMillis;
            final String ui;

            DatePair(long epochMillis, String ui) {
                this.epochMillis = epochMillis;
                this.ui = ui;
            }
        }
    }

    public static class YandexResource {
        @SerializedName("name")
        public String name;
        @SerializedName("path")
        public String path;       // e.g. "disk:/Folder/File.txt"
        @SerializedName("modified")
        public String modified;   // RFC3339
        @SerializedName("size")
        public Long size;
        @SerializedName("_embedded")
        public YandexResourceEmbedded _embedded;
        @SerializedName("type")
        String type;              // "file" or "dir"

        boolean isFile() {
            return "file".equals(type);
        }

        public boolean isDir() {
            return "dir".equals(type);
        }
    }

    public static class YandexResourceEmbedded {
        @SerializedName("items")
        public java.util.List<YandexResource> items;
    }
}
