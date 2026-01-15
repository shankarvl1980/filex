package svl.kadatha.filex.filemodel;

import android.net.Uri;

import java.io.IOException;
import java.util.List;

import svl.kadatha.filex.FileObjectType;
import svl.kadatha.filex.FileUtil;

public class FileModelFactory {

    public static FileModel getFileModel(String path, FileObjectType fileObjectType, Uri uri, String uri_path) {
        FileModel fileModel = null;
        switch (fileObjectType) {
            case ROOT_TYPE:
                break;
            case FILE_TYPE:
            case SEARCH_LIBRARY_TYPE:
                boolean writeable = FileUtil.isFromInternal(fileObjectType, path);
                fileModel = new JavaFileModel(path, writeable, uri, uri_path);
                break;
            case USB_TYPE:
                fileModel = new UsbFileModel(path);
                break;
            case FTP_TYPE:
                fileModel = new FtpFileModel(path);
                break;
            case SFTP_TYPE:
                fileModel = new SftpFileModel(path);
                break;
            case WEBDAV_TYPE:
                fileModel = new WebDavFileModel(path);
                break;
            case SMB_TYPE:
                fileModel = new SmbFileModel(path);
                break;
            case GOOGLE_DRIVE_TYPE:
                try {
                    String p = normalizeGoogleDrivePath(path);
                    fileModel = new GoogleDriveFileModel(p);
                } catch (IOException e) {
                    // Do NOT crash your whole app for a missing file model
                    // Return null so caller can handle it gracefully
                    return null;
                }
                break;

        }
        return fileModel;
    }

    public static FileModel[] getFileModelArray(List<String> file_path_list, FileObjectType fileObjectType, Uri uri, String uri_path) {
        int size = file_path_list.size();
        FileModel[] fileModels = new FileModel[size];
        for (int i = 0; i < size; ++i) {
            String file_path = file_path_list.get(i);
            fileModels[i] = getFileModel(file_path, fileObjectType, uri, uri_path);
        }
        return fileModels;
    }

    private static String normalizeGoogleDrivePath(String path) {
        if (path == null) return null;

        // Use forward slashes always
        path = path.replace('\\', '/');

        // Ensure leading slash
        if (!path.startsWith("/")) path = "/" + path;

        // Strip UI label "My Drive"
        if (path.equals("/My Drive")) return "/";
        if (path.startsWith("/My Drive/")) {
            path = path.substring("/My Drive".length()); // keeps leading '/'
            if (path.isEmpty()) path = "/";
        }

        // Optional: collapse multiple slashes
        while (path.contains("//")) path = path.replace("//", "/");

        return path;
    }

}
