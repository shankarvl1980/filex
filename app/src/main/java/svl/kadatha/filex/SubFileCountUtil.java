package svl.kadatha.filex;

import android.os.Build;

import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.smbj.session.Session;
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
import svl.kadatha.filex.network.FtpClientRepository;
import svl.kadatha.filex.network.NetworkAccountDetailsViewModel;
import svl.kadatha.filex.network.SftpChannelRepository;
import svl.kadatha.filex.network.SmbClientRepository;
import svl.kadatha.filex.network.WebDavClientRepository;
import svl.kadatha.filex.usb.ReadAccess;
import svl.kadatha.filex.usb.UsbFileRootSingleton;

public final class SubFileCountUtil {

    public static void ensureSubFileCount(FilePOJO pojo, Callback cb) {
        if (!pojo.getIsDirectory()) return;

        // If you added dedicated fields:
        if (pojo.getSize() != null && !pojo.getSize().isEmpty()) return;
        int count = computeSubFileCountBlocking(pojo);
        String si = "(" + count + ")";
        pojo.setSize(si);
        cb.onSubFileCountReady(pojo, count);
    }

    private static int computeSubFileCountBlocking(FilePOJO pojo) {
        FileObjectType type = pojo.getFileObjectType();
        String path = pojo.getPath();

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

                // Cloud providers if you want…
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
        SmbClientRepository smbRepo = null;
        Session session = null;
        try {
            smbRepo = SmbClientRepository.getInstance(NetworkAccountDetailsViewModel.SMB_NETWORK_ACCOUNT_POJO);
            session = smbRepo.getSession();
            String shareName = smbRepo.getShareName();
            try (DiskShare share = (DiskShare) session.connectShare(shareName)) {
                String adj = path.startsWith("/") ? path.substring(1) : path;
                List<FileIdBothDirectoryInformation> list = share.list(adj);
                // minus 2 for "." and ".."
                return list != null ? Math.max(0, list.size() - 2) : 0;
            }
        } finally {
            if (smbRepo != null && session != null) smbRepo.releaseSession(session);
        }
    }

    public interface Callback {
        void onSubFileCountReady(FilePOJO pojo, int subFileCount);
    }
}

