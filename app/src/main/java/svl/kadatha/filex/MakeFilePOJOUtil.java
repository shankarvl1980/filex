package svl.kadatha.filex;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.View;

import androidx.annotation.RequiresApi;

import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.Metadata;
import com.google.gson.Gson;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.msfscc.fileinformation.FileAllInformation;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.smbj.common.SMBRuntimeException;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.thegrizzlylabs.sardineandroid.DavResource;
import com.thegrizzlylabs.sardineandroid.Sardine;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import me.jahnen.libaums.core.fs.UsbFile;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import svl.kadatha.filex.appmanager.AppManagerListFragment;
import svl.kadatha.filex.cloud.CloudAccountViewModel;
import svl.kadatha.filex.filemodel.FileModel;
import svl.kadatha.filex.filemodel.WebDavFileModel;
import svl.kadatha.filex.network.FtpClientRepository;
import svl.kadatha.filex.network.NetworkAccountDetailsViewModel;
import svl.kadatha.filex.network.SftpChannelRepository;
import svl.kadatha.filex.network.SmbClientRepository;
import svl.kadatha.filex.network.WebDavClientRepository;
import svl.kadatha.filex.usb.ReadAccess;
import svl.kadatha.filex.usb.UsbFileRootSingleton;
import timber.log.Timber;

public class MakeFilePOJOUtil {
    static final SimpleDateFormat SDF_FTP = new SimpleDateFormat("yyyyMMddHHmmss");
    private static final String TAG = "Ftp-MakeFilePOJOUtil";

    public static FilePOJO MAKE_FilePOJO(FileModel f, boolean extract_icon, FileObjectType fileObjectType) {
        String name = f.getName();
        String path = f.getPath();
        boolean isDirectory = f.isDirectory();
        long dateLong = f.lastModified();
        String date = Global.SDF.format(dateLong);
        long sizeLong = 0L;
        String si;

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
                type = GET_FILE_TYPE(isDirectory, file_ext);
                if (type == -2) {
                    overlay_visible = View.VISIBLE;
                } else if (extract_icon && type == 0) {
                    package_name = EXTRACT_ICON(path, file_ext);
                }
            }

            sizeLong = f.getLength();
            si = FileUtil.humanReadableByteCount(sizeLong);
        } else {
            String sub_file_count = null;
            if (fileObjectType == FileObjectType.FILE_TYPE) {
                String[] file_list;
                File file = new File(f.getPath());
                if ((file_list = file.list(Global.File_NAME_FILTER)) != null) {
                    sub_file_count = "(" + file_list.length + ")";
                }
            } else {
                FileModel[] file_list;
                if ((file_list = f.list()) != null) {
                    sub_file_count = "(" + file_list.length + ")";
                }
            }
            si = sub_file_count;
        }

        if (f.isHidden()) {
            alfa = Global.DISABLE_ALFA;
        }
        return new FilePOJO(fileObjectType, name, package_name, path, isDirectory, dateLong, date, sizeLong, si, type, file_ext, alfa, overlay_visible, 0, 0L, null, 0, null, null);
    }

    public static FilePOJO MAKE_FilePOJO(File f, boolean extract_icon, FileObjectType fileObjectType) {
        String name = f.getName();
        String path = f.getAbsolutePath();
        boolean isDirectory = f.isDirectory();
        long dateLong = f.lastModified();
        String date = Global.SDF.format(dateLong);
        long sizeLong = 0L;
        String si;
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
                type = GET_FILE_TYPE(isDirectory, file_ext);
                if (type == -2) {
                    overlay_visible = View.VISIBLE;
                } else if (extract_icon && type == 0) {
                    package_name = EXTRACT_ICON(path, file_ext);
                }
            }

            sizeLong = f.length();
            si = FileUtil.humanReadableByteCount(sizeLong);
        } else {
            String sub_file_count = null;
            String[] file_list;
            if ((file_list = f.list(Global.File_NAME_FILTER)) != null) {
                sub_file_count = "(" + file_list.length + ")";
            }
            si = sub_file_count;
        }

        if (f.isHidden()) {
            alfa = Global.DISABLE_ALFA;
        }
        return new FilePOJO(fileObjectType, name, package_name, path, isDirectory, dateLong, date, sizeLong, si, type, file_ext, alfa, overlay_visible, 0, 0L, null, 0, null, null);
    }

    static FilePOJO MAKE_FilePOJO_ZIP(File f, boolean extract_icon, FileObjectType fileObjectType) {
        String name = f.getName();
        String path = f.getAbsolutePath();
        boolean isDirectory = f.isDirectory();
        long dateLong = f.lastModified();
        String date = Global.SDF.format(dateLong);
        long sizeLong = 0L;
        String si;
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
                type = GET_FILE_TYPE(isDirectory, file_ext);
                if (type == -2) {
                    overlay_visible = View.VISIBLE;
                } else if (extract_icon && type == 0) {
                    package_name = EXTRACT_ICON(path, file_ext);
                }
            }
            try (ZipFile zipFile = new ZipFile(ArchiveViewActivity.ZIP_FILE)) {
                ZipEntry zipEntry = zipFile.getEntry(path.substring(Global.ARCHIVE_CACHE_DIR_LENGTH + 1));
                if (zipEntry != null) {
                    sizeLong = zipEntry.getSize();
                }
            } catch (IOException e) {

            }
            si = FileUtil.humanReadableByteCount(sizeLong);
        } else {
            String sub_file_count = null;
            String[] file_list;
            if ((file_list = f.list(Global.File_NAME_FILTER)) != null) {
                sub_file_count = "(" + file_list.length + ")";
            }
            si = sub_file_count;
        }

        if (f.isHidden()) {
            alfa = Global.DISABLE_ALFA;
        }
        return new FilePOJO(fileObjectType, name, package_name, path, isDirectory, dateLong, date, sizeLong, si, type, file_ext, alfa, overlay_visible, 0, 0L, null, 0, null, null);
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    static FilePOJO MAKE_FilePOJO(Path p, boolean extract_icon, FileObjectType fileObjectType) {
        String name = p.getFileName().toString();
        String path = p.toAbsolutePath().toString();
        boolean isDirectory;
        long dateLong = 0;
        long sizeLong = 0L;
        try {
            BasicFileAttributes basicFileAttributes = Files.readAttributes(p, BasicFileAttributes.class);
            isDirectory = basicFileAttributes.isDirectory();
            dateLong = basicFileAttributes.lastModifiedTime().toMillis();
            if (!isDirectory) {
                sizeLong = basicFileAttributes.size();
            }
        } catch (IOException e) {
            isDirectory = Files.isDirectory(p);
            try {
                dateLong = Files.getLastModifiedTime(p).toMillis();
            } catch (IOException ioe) {

            }
        }

        String date = Global.SDF.format(dateLong);
        String si;
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
                type = GET_FILE_TYPE(isDirectory, file_ext);
                if (type == -2) {
                    overlay_visible = View.VISIBLE;
                } else if (extract_icon && type == 0) {
                    package_name = EXTRACT_ICON(path, file_ext);
                }
            }
            si = FileUtil.humanReadableByteCount(sizeLong);
        } else {
            String sub_file_count = null;
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(path), Global.GET_NIO_FILE_NAME_FILTER())) {
                int count = 0;
                for (Path pa : directoryStream) {
                    ++count;
                }
                sub_file_count = "(" + count + ")";
            } catch (IOException e) {

            }
            si = sub_file_count;
        }

        if (name.startsWith(".")) {
            alfa = Global.DISABLE_ALFA;
        }
        return new FilePOJO(fileObjectType, name, package_name, path, isDirectory, dateLong, date, sizeLong, si, type, file_ext, alfa, overlay_visible, 0, 0L, null, 0, null, null);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    static FilePOJO MAKE_FilePOJO_ZIP(Path p, boolean extract_icon, FileObjectType fileObjectType) {
        String name = p.getFileName().toString();
        String path = p.toAbsolutePath().toString();
        boolean isDirectory;
        long dateLong = 0;
        long sizeLong = 0L;
        try {
            BasicFileAttributes basicFileAttributes = Files.readAttributes(p, BasicFileAttributes.class);
            isDirectory = basicFileAttributes.isDirectory();
            dateLong = basicFileAttributes.lastModifiedTime().toMillis();
            if (!isDirectory) {
                sizeLong = basicFileAttributes.size();
            }
        } catch (IOException e) {
            isDirectory = Files.isDirectory(p);
            try {
                dateLong = Files.getLastModifiedTime(p).toMillis();
            } catch (IOException ioe) {

            }
        }

        String date = Global.SDF.format(dateLong);
        String si;
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
                type = GET_FILE_TYPE(isDirectory, file_ext);
                if (type == -2) {
                    overlay_visible = View.VISIBLE;
                } else if (extract_icon && type == 0) {
                    package_name = EXTRACT_ICON(path, file_ext);
                }
            }
            try (ZipFile zipFile = new ZipFile(ArchiveViewActivity.ZIP_FILE)) {
                ZipEntry zipEntry = zipFile.getEntry(path.substring(Global.ARCHIVE_CACHE_DIR_LENGTH + 1));
                if (zipEntry != null) {
                    sizeLong = zipEntry.getSize();
                }
            } catch (IOException e) {
            }
            si = FileUtil.humanReadableByteCount(sizeLong);
        } else {
            String sub_file_count = null;
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(path), Global.GET_NIO_FILE_NAME_FILTER())) {
                int count = 0;
                for (Path pa : directoryStream) {
                    ++count;
                }
                sub_file_count = "(" + count + ")";
            } catch (IOException e) {

            }
            si = sub_file_count;
        }

        if (name.startsWith(".")) {
            alfa = Global.DISABLE_ALFA;
        }
        return new FilePOJO(fileObjectType, name, package_name, path, isDirectory, dateLong, date, sizeLong, si, type, file_ext, alfa, overlay_visible, 0, 0L, null, 0, null, null);
    }

    static FilePOJO MAKE_FilePOJO(UsbFile f, boolean extract_icon) {
        String name = f.getName();
        String path = f.getAbsolutePath();
        boolean isDirectory = f.isDirectory();
        long dateLong = 0L;
        String date = "date";
        try {
            dateLong = f.lastModified();
            date = Global.SDF.format(dateLong);
        } catch (Exception e) {

        }

        long sizeLong = 0L;
        String si;
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
                type = GET_FILE_TYPE(isDirectory, file_ext);
                if (type == -2) {
                    overlay_visible = View.VISIBLE;
                } else if (extract_icon && type == 0) {
                    package_name = EXTRACT_ICON(path, file_ext);
                }
            }
            sizeLong = f.getLength();
            si = FileUtil.humanReadableByteCount(sizeLong);
        } else {
            String sub_file_count = null;
            String[] file_list;
            try {
                file_list = f.list();
                sub_file_count = "(" + file_list.length + ")";

            } catch (IOException e) {

            }
            si = sub_file_count;
        }

        return new FilePOJO(FileObjectType.USB_TYPE, name, package_name, path, isDirectory, dateLong, date, sizeLong, si, type, file_ext, alfa, overlay_visible, 0, 0L, null, 0, null, null);
    }

    static FilePOJO MAKE_FilePOJO(FTPFile f, boolean extract_icon, FileObjectType fileObjectType, String file_path, FTPClient ftpClient) {
        Timber.tag(TAG).d("Creating FilePOJO for FTP file: %s", file_path);
        String name = f.getName();
        String path = file_path;
        boolean isDirectory = f.isDirectory();
        long dateLong = 0L;
        String date = "";
        try {
            String str = ftpClient.getModificationTime(file_path);
            if (str != null) {
                if (str.contains(" ")) {
                    str = str.substring(str.indexOf(" "));
                }
                Date d = SDF_FTP.parse(str);
                date = Global.SDF.format(d);
            }
        } catch (Exception e) {
            Timber.tag(TAG).e("Error getting modification time for FTP file: %s", e.getMessage());
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
                type = GET_FILE_TYPE(isDirectory, file_ext);
                if (type == -2) {
                    overlay_visible = View.VISIBLE;
                } else if (extract_icon && type == 0) {
                    package_name = EXTRACT_ICON(path, file_ext);
                }
            }
            sizeLong = f.getSize();
            si = FileUtil.humanReadableByteCount(sizeLong);
        } else {
            String sub_file_count = null;
            String[] file_list;
            try {
                if ((file_list = ftpClient.listNames(file_path)) != null) {
                    sub_file_count = "(" + file_list.length + ")";
                }
                si = sub_file_count;
            } catch (IOException e) {
                Timber.tag(TAG).e("Error listing FTP directory contents: %s", e.getMessage());
            }
        }

        return new FilePOJO(fileObjectType, name, package_name, path, isDirectory, dateLong, date, sizeLong, si, type, file_ext, alfa, overlay_visible, 0, 0L, null, 0, null, null);
    }


    static FilePOJO MAKE_FilePOJO_ROOT(String file_path, boolean extract_icon, FileObjectType fileObjectType) {
        String command = "stat -c '%F|%s|%Y|%n|%a' '" + file_path + "'";
        String output = RootUtils.executeCommand(command);
        if (output == null || output.trim().isEmpty()) {
            return null;
        }

        String[] fields = output.split("\\|", -1); // Include trailing empty strings
        if (fields.length < 5) {
            // Output does not have all required fields
            return null;
        }

        String fileType = fields[0];
        String sizeStr = fields[1];
        String modTimeStr = fields[2];
        String name = fields[3];
        String permissions = fields[4];
        boolean isDirectory = fileType.equalsIgnoreCase("directory");
        long sizeLong;
        long dateLong;

        try {
            sizeLong = Long.parseLong(sizeStr);
        } catch (NumberFormatException e) {
            sizeLong = 0L;
        }

        try {
            dateLong = Long.parseLong(modTimeStr) * 1000L; // Convert seconds to milliseconds
        } catch (NumberFormatException e) {
            dateLong = 0L;
        }

        String date = Global.SDF.format(dateLong);
        String path = name; // 'stat' outputs the full path in %n
        String si;

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
                type = GET_FILE_TYPE(isDirectory, file_ext);
                if (type == -2) {
                    overlay_visible = View.VISIBLE;
                } else if (extract_icon && type == 0) {
                    package_name = EXTRACT_ICON(path, file_ext);
                }
            }
            si = FileUtil.humanReadableByteCount(sizeLong);
        } else {
            String sub_file_count = null;
            String[] file_list;
            if ((file_list = RootUtils.listFilesInDirectory(file_path)) != null) {
                sub_file_count = "(" + file_list.length + ")";
            }
            si = sub_file_count;
        }

        if (name.startsWith(".")) {
            alfa = Global.DISABLE_ALFA;
        }
        return new FilePOJO(fileObjectType, name, package_name, path, isDirectory, dateLong, date, sizeLong, si, type, file_ext, alfa, overlay_visible, 0, 0L, null, 0, null, null);
    }

    public static FilePOJO MAKE_FilePOJO(ChannelSftp.LsEntry entry, boolean extract_icon, FileObjectType fileObjectType, String file_path, ChannelSftp channelSftp) {
        Timber.tag(TAG).d("Creating FilePOJO for SFTP file: %s", file_path);
        String name = entry.getFilename();
        String path = file_path;
        SftpATTRS attrs = entry.getAttrs();
        boolean isDirectory = attrs.isDir();
        long dateLong = 0L;
        String date = "";
        try {
            int mtime = attrs.getMTime(); // Modification time in seconds since epoch
            dateLong = ((long) mtime) * 1000; // Convert to milliseconds
            date = Global.SDF.format(new Date(dateLong));
        } catch (Exception e) {
            Timber.tag(TAG).e("Error getting modification time for SFTP file: %s", e.getMessage());
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
                type = GET_FILE_TYPE(isDirectory, file_ext);
                if (type == -2) {
                    overlay_visible = View.VISIBLE;
                } else if (extract_icon && type == 0) {
                    package_name = EXTRACT_ICON(path, file_ext);
                }
            }
            sizeLong = attrs.getSize();
            si = FileUtil.humanReadableByteCount(sizeLong);
        } else {
            String sub_file_count = null;
            try {
                Vector<ChannelSftp.LsEntry> entries = channelSftp.ls(file_path);
                if (entries != null) {
                    sub_file_count = "(" + entries.size() + ")";
                }
                si = sub_file_count;
            } catch (SftpException e) {
                Timber.tag(TAG).e("Error listing SFTP directory contents: %s", e.getMessage());
            }
        }
        return new FilePOJO(fileObjectType, name, package_name, path, isDirectory, dateLong, date, sizeLong, si, type, file_ext, alfa, overlay_visible, 0, 0L, null, 0, null, null);
    }

    static FilePOJO MAKE_FilePOJO(DavResource resource, boolean extract_icon, FileObjectType fileObjectType, String file_path, Sardine sardine) {
        Timber.tag(TAG).d("Creating FilePOJO for WebDAV resource: %s", file_path);
        String name = resource.getName();
        String path = file_path;
        boolean isDirectory = resource.isDirectory();
        long dateLong = 0L;
        String date = "";
        try {
            Date modifiedDate = resource.getModified();
            if (modifiedDate != null) {
                dateLong = modifiedDate.getTime();
                date = Global.SDF.format(modifiedDate);
            }
        } catch (Exception e) {
            Timber.tag(TAG).e("Error getting modification time for WebDAV resource: %s", e.getMessage());
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
                type = GET_FILE_TYPE(isDirectory, file_ext);
                if (type == -2) {
                    overlay_visible = View.VISIBLE;
                } else if (extract_icon && type == 0) {
                    package_name = EXTRACT_ICON(path, file_ext);
                }
            }
            sizeLong = resource.getContentLength();
            si = FileUtil.humanReadableByteCount(sizeLong);
        } else {
            String sub_file_count = null;
            try {
                List<DavResource> resources = sardine.list(resource.getHref().toString());
                if (resources != null) {
                    // Subtract 1 to exclude the current directory itself
                    sub_file_count = "(" + (resources.size() - 1) + ")";
                }
                si = sub_file_count;
            } catch (IOException e) {
                Timber.tag(TAG).e("Error listing WebDAV directory contents: %s", e.getMessage());
            }
        }
        return new FilePOJO(fileObjectType, name, package_name, path, isDirectory, dateLong, date, sizeLong, si, type, file_ext, alfa, overlay_visible, 0, 0L, null, 0, null, null);
    }

    public static FilePOJO MAKE_FilePOJO(SmbFileInfo smbFileInfo, boolean extract_icon, FileObjectType fileObjectType) {

        String name = smbFileInfo.getFileName();
        String path = smbFileInfo.getFilePath();
        boolean isDirectory = smbFileInfo.getFileAttributes().contains(FileAttributes.FILE_ATTRIBUTE_DIRECTORY);
        long dateLong = smbFileInfo.getChangeTime();
        String date = Global.SDF.format(new Date(dateLong));

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
                type = GET_FILE_TYPE(isDirectory, file_ext);
                if (type == -2) {
                    overlay_visible = View.VISIBLE;
                } else if (extract_icon && type == 0) {
                    package_name = EXTRACT_ICON(path, file_ext);
                }
            }
            sizeLong = smbFileInfo.getFileSize();
            si = FileUtil.humanReadableByteCount(sizeLong);
        } else {
            String sub_file_count;
            SmbClientRepository smbClientRepository = null;
            Session session = null;
            String shareName;
            try {
                smbClientRepository = SmbClientRepository.getInstance(NetworkAccountDetailsViewModel.SMB_NETWORK_ACCOUNT_POJO);
                session = smbClientRepository.getSession();
                shareName = smbClientRepository.getShareName();
                try (DiskShare share = (DiskShare) session.connectShare(shareName)) {
                    // Adjust fileclickselected to remove leading "/" if necessary
                    String adjustedPath = path.startsWith("/") ? path.substring(1) : path;

                    // List files in the directory
                    List<FileIdBothDirectoryInformation> fileList = share.list(adjustedPath);
                    sub_file_count = "(" + (fileList.size() - 2) + ")";
                    si = sub_file_count;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (smbClientRepository != null && session != null) {
                    smbClientRepository.releaseSession(session);
                    Timber.tag(TAG).d("SMB session released");
                }
            }
        }
        return new FilePOJO(fileObjectType, name, package_name, path, isDirectory, dateLong, date, sizeLong, si, type, file_ext, alfa, overlay_visible, 0, 0L, null, 0, null, null);
    }


    public static FilePOJO MAKE_FilePOJO(FileObjectType fileObjectType, String file_path) {
        FilePOJO filePOJO = null;
        if (fileObjectType == FileObjectType.FILE_TYPE) {
            File f = new File(file_path);
            filePOJO = MAKE_FilePOJO(f, true, fileObjectType);
        } else if (fileObjectType == FileObjectType.USB_TYPE) {
            try (ReadAccess access = UsbFileRootSingleton.getInstance().acquireUsbFileRootForRead()) {
                UsbFile usbFileRoot = access.getUsbFile();
                try {
                    UsbFile f = usbFileRoot.search(Global.GET_TRUNCATED_FILE_PATH_USB(file_path));
                    filePOJO = MAKE_FilePOJO(f, true);
                } catch (IOException e) {
                    return null;
                }
            }
        } else if (fileObjectType == FileObjectType.ROOT_TYPE) {
            filePOJO = MAKE_FilePOJO_ROOT(file_path, false, fileObjectType);
        } else if (fileObjectType == FileObjectType.FTP_TYPE) {
            if (file_path.equals(File.separator)) {
                filePOJO = new FilePOJO(fileObjectType, File.separator, null, File.separator, true, 0L, null, 0L, null, R.drawable.folder_icon, null, Global.ENABLE_ALFA, View.INVISIBLE, 0, 0L, null, 0, null, null);
            } else {
                FtpClientRepository ftpClientRepository = FtpClientRepository.getInstance(NetworkAccountDetailsViewModel.FTP_NETWORK_ACCOUNT_POJO);
                FTPClient ftpClient = null;
                try {
                    ftpClient = ftpClientRepository.getFtpClient();
                    FTPFile f = FileUtil.getFtpFile(ftpClient, file_path);
                    if (f != null) {
                        filePOJO = MAKE_FilePOJO(f, false, fileObjectType, file_path, ftpClient);
                    }

                } catch (IOException e) {

                } finally {
                    if (ftpClientRepository != null && ftpClient != null) {
                        ftpClientRepository.releaseFtpClient(ftpClient);
                    }
                }
            }
        } else if (fileObjectType == FileObjectType.SFTP_TYPE) {
            SftpChannelRepository sftpChannelRepository = SftpChannelRepository.getInstance(NetworkAccountDetailsViewModel.SFTP_NETWORK_ACCOUNT_POJO);
            ChannelSftp channelSftp = null;
            try {
                channelSftp = sftpChannelRepository.getSftpChannel();
                ChannelSftp.LsEntry lsEntry = FileUtil.getSftpEntry(channelSftp, file_path);
                if (lsEntry != null) {
                    filePOJO = MAKE_FilePOJO(lsEntry, false, fileObjectType, file_path, channelSftp);
                }
            } catch (Exception e) {

            } finally {
                if (sftpChannelRepository != null && channelSftp != null) {
                    sftpChannelRepository.releaseChannel(channelSftp);
                    Timber.tag(TAG).d("SFTP channel released");
                }
            }
        } else if (fileObjectType == FileObjectType.WEBDAV_TYPE) {
            WebDavClientRepository webDavClientRepository;
            Sardine sardine;
            try {
                webDavClientRepository = WebDavClientRepository.getInstance(NetworkAccountDetailsViewModel.WEBDAV_NETWORK_ACCOUNT_POJO);
                sardine = webDavClientRepository.getSardine();
                //String fullPath = basePath + (file_path.startsWith("/") ? file_path : "/" + file_path);
                String url = webDavClientRepository.buildUrl(file_path);

                if (file_path.equals("/")) {
                    // Special case for root directory
                    filePOJO = new FilePOJO(fileObjectType, "/", null, "/", true, 0L, null, 0L, null, R.drawable.folder_icon, null, Global.ENABLE_ALFA, View.INVISIBLE, 0, 0L, null, 0, null, null);
                } else if (new WebDavFileModel(file_path).exists()) {
                    // If it exists, get its properties
                    List<DavResource> resources = sardine.getResources(url);
                    if (!resources.isEmpty()) {
                        DavResource resource = resources.get(0);
                        filePOJO = MAKE_FilePOJO(resource, false, fileObjectType, file_path, sardine);
                    }
                }
            } catch (IOException e) {
                Timber.tag(TAG).e("Error creating FilePOJO for WebDAV resource: %s", e.getMessage());
            }
        } else if (fileObjectType == FileObjectType.SMB_TYPE) {
            SmbClientRepository smbClientRepository = null;
            Session session = null;
            String shareName;
            try {
                smbClientRepository = SmbClientRepository.getInstance(NetworkAccountDetailsViewModel.SMB_NETWORK_ACCOUNT_POJO);
                session = smbClientRepository.getSession();
                shareName = smbClientRepository.getShareName();
                try (DiskShare share = (DiskShare) session.connectShare(shareName)) {
                    // Adjust file_path if necessary (e.g., remove leading "/")
                    String adjustedPath = file_path.startsWith("/") ? file_path.substring(1) : file_path;

                    // Check if the path is root
                    if (adjustedPath.isEmpty()) {
                        filePOJO = new FilePOJO(fileObjectType, "/", null, "/", true, 0L, null, 0L, null, R.drawable.folder_icon, null, Global.ENABLE_ALFA, View.INVISIBLE, 0, 0L, null, 0, null, null);
                    } else if (share.folderExists(adjustedPath) || share.fileExists(adjustedPath)) {
                        // Get file information
                        FileAllInformation fileInfo = share.getFileInformation(adjustedPath);

                        // Create SmbFileInfo object
                        SmbFileInfo smbFileInfo = new SmbFileInfo(
                                adjustedPath.substring(adjustedPath.lastIndexOf('/') + 1),
                                file_path,
                                fileInfo.getBasicInformation().getFileAttributes(),
                                fileInfo.getStandardInformation().getEndOfFile(),
                                fileInfo.getBasicInformation().getCreationTime().toEpochMillis(),
                                fileInfo.getBasicInformation().getLastAccessTime().toEpochMillis(),
                                fileInfo.getBasicInformation().getLastWriteTime().toEpochMillis(),
                                fileInfo.getBasicInformation().getChangeTime().toEpochMillis()
                        );

                        filePOJO = MAKE_FilePOJO(smbFileInfo, false, fileObjectType);
                    }
                }
            } catch (IOException | SMBRuntimeException e) {
                Timber.tag(TAG).e("Error creating FilePOJO for SMB file: %s", e.getMessage());
            } finally {
                if (smbClientRepository != null && session != null) {
                    smbClientRepository.releaseSession(session);
                    Timber.tag(TAG).d("SMB session released");
                }
            }
        } else if (fileObjectType == FileObjectType.GOOGLE_DRIVE_TYPE) {
            try {
                String oauthToken = CloudAccountViewModel.GOOGLE_DRIVE_ACCESS_TOKEN;

                // Attempt to create a FilePOJO from the given file path and OAuth token
                filePOJO = MakeCloudFilePOJOUtil.MAKE_FilePOJO_FromDriveAPI(file_path, true, fileObjectType, oauthToken);
            } catch (IOException e) {
                Timber.tag(TAG).e("Error creating FilePOJO for Google Drive file: %s", e.getMessage());
            }
        } else if (fileObjectType == FileObjectType.DROP_BOX_TYPE) {
            try {
                String accessToken = CloudAccountViewModel.DROP_BOX_ACCESS_TOKEN;
                // Get Dropbox client
                DbxClientV2 dbxClient = new DbxClientV2(new com.dropbox.core.DbxRequestConfig("YourAppName"), accessToken);

                if (file_path.equals("/")) {
                    // Treat root as empty string in Dropbox
                    // Create a FilePOJO representing the root directory
                    filePOJO = new FilePOJO(
                            fileObjectType,
                            "/",             // name
                            null,            // package_name
                            "/",             // path
                            true,            // isDirectory
                            0L,              // dateLong
                            null,            // date
                            0L,              // sizeLong
                            null,            // si
                            R.drawable.folder_icon, // type (folder)
                            null,            // file_ext
                            Global.ENABLE_ALFA,
                            View.INVISIBLE,
                            0,
                            0L,
                            null,
                            0,
                            null,
                            null
                    );
                } else {
                    // For non-root paths, fetch metadata
                    Metadata metadata = dbxClient.files().getMetadata(file_path);
                    // Create a FilePOJO from the metadata
                    filePOJO = MakeCloudFilePOJOUtil.MAKE_FilePOJO(metadata, true, fileObjectType, file_path, dbxClient);
                }
            } catch (Exception e) {
                Timber.tag(TAG).e("Error creating FilePOJO for Dropbox file: %s", e.getMessage());
            }
        } else if (fileObjectType == FileObjectType.MEDIA_FIRE_TYPE) {

        } else if (fileObjectType == FileObjectType.YANDEX_TYPE) {
            try {
                String accessToken = CloudAccountViewModel.YANDEX_ACCESS_TOKEN;
                OkHttpClient client = new OkHttpClient();
                Gson gson = new Gson();
                HttpUrl url = HttpUrl.parse("https://cloud-api.yandex.net/v1/disk/resources")
                        .newBuilder()
                        .addQueryParameter("path", file_path)
                        .addQueryParameter("fields", "name,type,size,modified,path")
                        .build();

                Request request = new Request.Builder()
                        .url(url)
                        .header("Authorization", "OAuth " + accessToken)
                        .get()
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        MakeCloudFilePOJOUtil.YandexResource resource = gson.fromJson(response.body().charStream(), MakeCloudFilePOJOUtil.YandexResource.class);
                        if (resource != null) {
                            // Convert the YandexResource into a FilePOJO
                            filePOJO = MakeCloudFilePOJOUtil.MAKE_FilePOJO(resource, true, fileObjectType, file_path);
                        }
                    } else {
                        Timber.tag(TAG).e("Failed to fetch Yandex metadata: %s %s", response.code(), response.message());
                    }
                }

            } catch (Exception e) {
                Timber.tag(TAG).e("Error creating FilePOJO for Yandex file: %s", e.getMessage());
            }
        }
        return filePOJO;
    }

    static String EXTRACT_ICON(String file_path, String file_ext) {
        PackageManager packageManager = App.getAppContext().getPackageManager();
        if (packageManager == null) {
            return null;
        }
        if (file_ext.matches(Global.APK_REGEX)) {
            PackageInfo PI = packageManager.getPackageArchiveInfo(file_path, 0);
            if (PI == null) {
                return null;
            }
            PI.applicationInfo.publicSourceDir = file_path;
            String package_name = PI.packageName;
            String file_with_package_name = package_name + ".png";
            AppManagerListFragment.extract_icon(file_with_package_name, packageManager, PI);
            return package_name;
        } else {
            return null;
        }
    }


    static int GET_FILE_TYPE(boolean isDirectory, String file_ext) {
        if (isDirectory) {
            return R.drawable.folder_icon;
        } else if (file_ext.matches(Global.AUDIO_REGEX)) {
            return R.drawable.audio_file_icon;
        } else if (file_ext.matches(Global.PDF_REGEX)) {
            return R.drawable.pdf_file_icon;
        } else if (file_ext.matches(Global.APK_REGEX)) {
            return 0;
        } else if (file_ext.matches(Global.ZIP_REGEX) || file_ext.matches(Global.UNIX_ARCHIVE_REGEX)) {
            return R.drawable.archive_file_icon;
        } else if (file_ext.matches(Global.IMAGE_REGEX)) {
            return -1;
        } else if (file_ext.matches(Global.VIDEO_REGEX)) {
            return -2;
        } else if (file_ext.matches(Global.TEXT_REGEX) || file_ext.matches(Global.RTF_REGEX)) {
            return R.drawable.text_file_icon;
        } else if (file_ext.matches(Global.DOC_REGEX)) {
            return R.drawable.word_file_icon;
        } else if (file_ext.matches(Global.XLS_REGEX)) {
            return R.drawable.xls_file_icon;
        } else if (file_ext.matches(Global.PPT_REGEX)) {
            return R.drawable.ppt_file_icon;
        } else {
            return R.drawable.unknown_file_icon;
        }
    }

    public static Set<FileAttributes> parseAttributes(long fileAttributesLong) {
        Set<FileAttributes> attrs = EnumSet.noneOf(FileAttributes.class);
        for (FileAttributes attr : FileAttributes.values()) {
            if ((fileAttributesLong & attr.getValue()) == attr.getValue()) {
                attrs.add(attr);
            }
        }
        return attrs;
    }

    public static class SmbFileInfo {
        private final String fileName;
        private final String filePath;
        private final Set<FileAttributes> fileAttributes;
        private final long fileSize;
        private final long creationTime;
        private final long lastAccessTime;
        private final long lastWriteTime;
        private final long changeTime;

        public SmbFileInfo(String fileName, String filePath, long fileAttributesLong, long fileSize,
                           long creationTime, long lastAccessTime, long lastWriteTime, long changeTime) {
            this.fileName = fileName;
            this.filePath = filePath;
            this.fileAttributes = parseAttributes(fileAttributesLong);
            this.fileSize = fileSize;
            this.creationTime = creationTime;
            this.lastAccessTime = lastAccessTime;
            this.lastWriteTime = lastWriteTime;
            this.changeTime = changeTime;
        }


        // Getters for all fields
        public String getFileName() {
            return fileName;
        }

        public String getFilePath() {
            return filePath;
        }

        public Set<FileAttributes> getFileAttributes() {
            return fileAttributes;
        }

        public long getFileSize() {
            return fileSize;
        }

        public long getCreationTime() {
            return creationTime;
        }

        public long getLastAccessTime() {
            return lastAccessTime;
        }

        public long getLastWriteTime() {
            return lastWriteTime;
        }

        public long getChangeTime() {
            return changeTime;
        }
    }
}
