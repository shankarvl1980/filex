package svl.kadatha.filex;

import android.os.Build;

import com.google.gson.Gson;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.smbj.share.DiskShare;
import com.jcraft.jsch.ChannelSftp;
import com.thegrizzlylabs.sardineandroid.DavResource;
import com.thegrizzlylabs.sardineandroid.Sardine;

import org.apache.commons.net.ftp.FTPClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Vector;

import me.jahnen.libaums.core.fs.UsbFile;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import svl.kadatha.filex.cloud.CloudAuthActivityViewModel;
import svl.kadatha.filex.filemodel.GoogleDriveFileModel;
import svl.kadatha.filex.network.FtpClientRepository;
import svl.kadatha.filex.network.NetworkAccountDetailsViewModel;
import svl.kadatha.filex.network.SftpChannelRepository;
import svl.kadatha.filex.network.SmbClientRepository;
import svl.kadatha.filex.network.WebDavClientRepository;
import svl.kadatha.filex.usb.ReadAccess;
import svl.kadatha.filex.usb.UsbFileRootSingleton;

public final class SubFileCountUtil {
    private static final int cap = 100;
    private static final OkHttpClient CLOUD_HTTP = Global.HTTP;
    private static final Gson CLOUD_GSON = Global.GSON;
    ;

    public static void ensureSubFileCount(FilePOJO pojo, Callback cb) {
        if (pojo == null) return;
        if (!pojo.getIsDirectory()) return;

        // If you added dedicated fields:
        if (pojo.getSize() != null && !pojo.getSize().isEmpty()) return;
        int count = computeSubFileCountBlocking(pojo);
        String si;
        if (Global.CLOUD_FILE_OBJECT_TYPES.contains(pojo.getFileObjectType())) {
            int limitSignal = cap + 1;
            si = (count >= limitSignal) ? "(" + cap + "+)" : "(" + count + ")";
        } else {
            si = "(" + count + ")";
        }

        pojo.setSize(si);
        if (cb != null) {
            cb.onSubFileCountReady(pojo, count);
        }
    }

    private static int computeSubFileCountBlocking(FilePOJO pojo) {
        FileObjectType type = pojo.getFileObjectType();
        String path = pojo.getPath();
        String token;
        try {
            switch (type) {
                case FILE_TYPE:
                    return countLocalFileChildren(path);

                case USB_TYPE:
                    return countUsbChildren(path);

                case ROOT_TYPE:
                    return countRootChildren(path);

                case FTP_TYPE:
                    return countFtpChildren(path);

                case SFTP_TYPE:
                    return countSftpChildren(path);

                case WEBDAV_TYPE:
                    return countWebDavChildren(path);

                case SMB_TYPE:
                    return countSmbChildren(path);

                case GOOGLE_DRIVE_TYPE:
                    token = CloudAuthActivityViewModel.GOOGLE_DRIVE_ACCESS_TOKEN;
                    return countDriveChildrenCapped(pojo, token, cap + 1);

                case DROP_BOX_TYPE: {
                    token = CloudAuthActivityViewModel.DROP_BOX_ACCESS_TOKEN;
                    return countDropboxChildrenCapped(pojo, token, cap + 1);
                }

                case YANDEX_TYPE: {
                    token = CloudAuthActivityViewModel.YANDEX_ACCESS_TOKEN;
                    return countYandexChildrenCapped(pojo, token, cap + 1);
                }

//                case ONE_DRIVE_TYPE: {
//                    token = CloudAuthActivityViewModel.ONE_DRIVE_ACCESS_TOKEN;
//                    return countOneDriveChildrenCapped(pojo, token, cap + 1);
//                }


                default:
                    return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    private static int countLocalFileChildren(String path) {
        // Prefer fast NIO API if Android 8.0+ (API 26)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Path nioPath = Paths.get(path);

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(
                    nioPath,
                    Global.GET_NIO_FILE_NAME_FILTER()  // Your filter
            )) {
                int count = 0;
                for (Path p : stream) {
                    count++; // just count, do not store!
                }
                return count;
            } catch (Exception e) {
                // Ignore and fallback below
            }
        }

        // Fallback for older Android or errors
        File dir = new File(path);
        String[] list = dir.list(Global.File_NAME_FILTER);
        return list != null ? list.length : 0;
    }

    private static int countUsbChildren(String path) throws IOException {
        try (ReadAccess access = UsbFileRootSingleton.getInstance().acquireUsbFileRootForRead()) {
            UsbFile usbRoot = access.getUsbFile();
            UsbFile f = usbRoot.search(Global.GET_TRUNCATED_FILE_PATH_USB(path));
            if (f == null) return 0;
            String[] list = f.list();
            return list != null ? list.length : 0;
        }
    }

    private static int countRootChildren(String path) {
        String[] list = RootUtils.listFilesInDirectory(path);
        return list != null ? list.length : 0;
    }

    private static int countFtpChildren(String path) throws IOException {
        FtpClientRepository repo = FtpClientRepository.getInstance(NetworkAccountDetailsViewModel.FTP_NETWORK_ACCOUNT_POJO);
        FTPClient client = null;
        try {
            client = repo.getFtpClient();
            String[] names = client.listNames(path);
            return names != null ? names.length : 0;
        } finally {
            if (repo != null && client != null) repo.releaseFtpClient(client);
        }
    }

    private static int countSftpChildren(String path) throws Exception {
        SftpChannelRepository repo = SftpChannelRepository.getInstance(NetworkAccountDetailsViewModel.SFTP_NETWORK_ACCOUNT_POJO);
        ChannelSftp ch = null;
        try {
            ch = repo.getSftpChannel();
            Vector<ChannelSftp.LsEntry> entries = ch.ls(path);
            return entries != null ? entries.size() : 0;
        } finally {
            if (repo != null && ch != null) repo.releaseChannel(ch);
        }
    }

    private static int countWebDavChildren(String path) throws IOException {
        WebDavClientRepository repo = WebDavClientRepository.getInstance(NetworkAccountDetailsViewModel.WEBDAV_NETWORK_ACCOUNT_POJO);
        Sardine sardine = repo.getSardine();
        String url = repo.buildUrl(path);
        List<DavResource> resources = sardine.list(url);
        // subtract 1 for self if needed
        return resources != null ? Math.max(0, resources.size() - 1) : 0;
    }

    private static int countSmbChildren(String path) throws IOException {
        SmbClientRepository smbClientRepository = SmbClientRepository.getInstance(NetworkAccountDetailsViewModel.SMB_NETWORK_ACCOUNT_POJO);
        SmbClientRepository.ShareHandle h = null;
        try {
            h = smbClientRepository.acquireShare();
            DiskShare share = h.share;
            String adj = path.startsWith("/") ? path.substring(1) : path;
            List<FileIdBothDirectoryInformation> list = share.list(adj);
            // minus 2 for "." and ".."
            return list != null ? Math.max(0, list.size() - 2) : 0;

        } finally {
            if (smbClientRepository != null) smbClientRepository.releaseShare(h);
        }
    }

    private static int countDriveChildrenCapped(FilePOJO pojo, String oauthToken, int capPlusOne) throws IOException {
        // capPlusOne example: 101 (meaning show "100+" when result >= 101)
        String folderId = pojo.getCloudId();
        if (folderId == null || folderId.isEmpty()) return 0;

        int count = 0;
        String pageToken = null;

        while (true) {
            HttpUrl.Builder b = HttpUrl.parse("https://www.googleapis.com/drive/v3/files")
                    .newBuilder()
                    .addQueryParameter("q", "'" + folderId + "' in parents and trashed=false")
                    .addQueryParameter("pageSize", "1000")
                    .addQueryParameter("fields", "nextPageToken,files(id)");

            if (pageToken != null && !pageToken.isEmpty()) {
                b.addQueryParameter("pageToken", pageToken);
            }

            Request req = new Request.Builder()
                    .url(b.build())
                    .header("Authorization", "Bearer " + oauthToken)
                    .get()
                    .build();

            try (Response resp = CLOUD_HTTP.newCall(req).execute()) {
                if (!resp.isSuccessful() || resp.body() == null) return count;

                GoogleDriveFileModel.DriveFilesListResponse res =
                        CLOUD_GSON.fromJson(resp.body().charStream(), GoogleDriveFileModel.DriveFilesListResponse.class);

                if (res == null || res.files == null) return count;

                count += res.files.size();
                if (count >= capPlusOne) return capPlusOne; // signal "cap+"

                pageToken = res.nextPageToken;
                if (pageToken == null || pageToken.isEmpty()) return count; // done
            }
        }
    }

    private static int countDropboxChildrenCapped(FilePOJO pojo, String accessToken, int capPlusOne) {
        try {
            String folderPath = pojo.getPath(); // Dropbox is path-based in your app
            if (folderPath == null || folderPath.trim().isEmpty()) folderPath = "/";
            folderPath = folderPath.trim();
            if (!folderPath.startsWith("/")) folderPath = "/" + folderPath;

            // Dropbox API uses "" to refer to root in listFolder
            String argPath = "/".equals(folderPath) ? "" : folderPath;

            com.dropbox.core.DbxRequestConfig cfg =
                    com.dropbox.core.DbxRequestConfig.newBuilder("FileX").build();
            com.dropbox.core.v2.DbxClientV2 client =
                    new com.dropbox.core.v2.DbxClientV2(cfg, accessToken);

            // One-shot listing with limit=capPlusOne
            com.dropbox.core.v2.files.ListFolderResult result = client.files()
                    .listFolderBuilder(argPath)
                    .withLimit((long) capPlusOne)
                    .start();

            int count = (result.getEntries() != null) ? result.getEntries().size() : 0;

            // If there's more beyond what we fetched => cap+
            if (result.getHasMore()) return capPlusOne;

            // If it exactly filled the capPlusOne bucket => still cap+
            // (rare edge, but safe)
            if (count >= capPlusOne) return capPlusOne;

            return count;

        } catch (Exception ignored) {
            return 0;
        }
    }

    private static int countYandexChildrenCapped(FilePOJO pojo, String oauthToken, int capPlusOne) {
        try {
            String folderPath = pojo.getPath(); // Yandex is path-based
            if (folderPath == null || folderPath.isEmpty()) return 0;

            // Ask only for total if possible; also request minimal items to validate embedded exists
            HttpUrl url = HttpUrl.parse("https://cloud-api.yandex.net/v1/disk/resources")
                    .newBuilder()
                    .addQueryParameter("path", folderPath)
                    .addQueryParameter("limit", "0")
                    .addQueryParameter("fields", "_embedded.total")
                    .build();

            Request req = new Request.Builder()
                    .url(url)
                    .header("Authorization", "OAuth " + oauthToken)
                    .get()
                    .build();

            try (Response resp = CLOUD_HTTP.newCall(req).execute()) {
                if (!resp.isSuccessful() || resp.body() == null) return 0;

                YandexTotalResponse r = CLOUD_GSON.fromJson(resp.body().charStream(), YandexTotalResponse.class);
                if (r == null || r._embedded == null) return 0;

                int total = r._embedded.total;
                return (total >= capPlusOne) ? capPlusOne : total;
            }

        } catch (Exception ignored) {
            return 0;
        }
    }

    public interface Callback {
        void onSubFileCountReady(FilePOJO pojo, int subFileCount);
    }

    private static final class YandexTotalResponse {
        YandexEmbedded _embedded;
    }

//    private static int countOneDriveChildrenCapped(FilePOJO pojo, String bearerToken, int capPlusOne) {
//        try {
//            String folderId = pojo.getCloudId(); // best: OneDrive item id
//            if (folderId == null || folderId.isEmpty()) return 0;
//
//            HttpUrl url = HttpUrl.parse("https://graph.microsoft.com/v1.0/me/drive/items/" + folderId + "/children")
//                    .newBuilder()
//                    .addQueryParameter("$top", Integer.toString(capPlusOne))
//                    .addQueryParameter("$select", "id")
//                    .build();
//
//            Request req = new Request.Builder()
//                    .url(url)
//                    .header("Authorization", "Bearer " + bearerToken)
//                    .get()
//                    .build();
//
//            try (Response resp = CLOUD_HTTP.newCall(req).execute()) {
//                if (!resp.isSuccessful() || resp.body() == null) return 0;
//
//                OneDriveChildrenResponse r =
//                        CLOUD_GSON.fromJson(resp.body().charStream(), OneDriveChildrenResponse.class);
//
//                if (r == null || r.value == null) return 0;
//
//                int count = r.value.size();
//
//                // If Graph says there is more => cap+
//                if (r.nextLink != null && !r.nextLink.isEmpty()) return capPlusOne;
//
//                // If we already hit capPlusOne in this page => cap+
//                if (count >= capPlusOne) return capPlusOne;
//
//                return count;
//            }
//
//        } catch (Exception ignored) {
//            return 0;
//        }
//    }

//    private static final class OneDriveChildrenResponse {
//        @com.google.gson.annotations.SerializedName("value")
//        java.util.List<OneDriveItem> value;
//
//        @com.google.gson.annotations.SerializedName("@odata.nextLink")
//        String nextLink;
//    }
//    private static final class OneDriveItem {
//        @com.google.gson.annotations.SerializedName("id")
//        String id;
//    }

    private static final class YandexEmbedded {
        int total;
    }
}

