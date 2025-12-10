package svl.kadatha.filex;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.provider.DocumentsContract;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jcraft.jsch.ChannelSftp;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

import me.jahnen.libaums.core.fs.UsbFile;
import svl.kadatha.filex.filemodel.FileModel;
import svl.kadatha.filex.filemodel.FtpFileModel;
import svl.kadatha.filex.filemodel.UsbFileModel;

/**
 * Utility class for helping parsing file systems.
 */
public final class FileUtil {
    public final static int BUFFER_SIZE = 16 * 1024;
    private static final String PRIMARY_VOLUME_NAME = "primary";
    private static final String TAG = "Ftp-FileUtil";
    public static int USB_CHUNK_SIZE;

    private FileUtil() {
        throw new UnsupportedOperationException();
    }


    public static Uri getDocumentUri(@NonNull final String target_file_path, @NonNull Uri tree_uri, String tree_uri_path) {
        String target_uri_id = getDocumentID(target_file_path, tree_uri, tree_uri_path);
        return DocumentsContract.buildDocumentUriUsingTree(tree_uri, target_uri_id);
    }

    public static Uri createDocumentUri(Context context, @NonNull final String parent_file_path, @Nullable String name, final boolean isDirectory,
                                        @NonNull Uri tree_uri, String tree_uri_path) {
        Uri uri = getDocumentUri(Global.CONCATENATE_PARENT_CHILD_PATH(parent_file_path, name), tree_uri, tree_uri_path);
        if (existsUri(context, uri)) {
            return uri;
        }
        Uri parent_uri = getDocumentUri(parent_file_path, tree_uri, tree_uri_path);
        if (isDirectory) {
            try {
                uri = DocumentsContract.createDocument(context.getContentResolver(), parent_uri, DocumentsContract.Document.MIME_TYPE_DIR, name);
            } catch (FileNotFoundException ignored) {

            }
        } else {
            try {
                uri = DocumentsContract.createDocument(context.getContentResolver(), parent_uri, "text", name);
            } catch (FileNotFoundException ignored) {
            }
        }
        return uri;
    }

    public static String getDocumentID(String file_path, @NonNull Uri tree_uri, @NonNull String tree_uri_path) {
        String relativePath = "";
        if (!file_path.equals(tree_uri_path)) {
            if (tree_uri_path.equals(File.separator)) {
                relativePath = file_path.substring(tree_uri_path.length());
            } else {
                relativePath = file_path.substring(tree_uri_path.length() + 1);
            }
        }
        String target_uri_id = DocumentsContract.getTreeDocumentId(tree_uri);
        if (!target_uri_id.endsWith(File.separator)) {
            target_uri_id = target_uri_id + File.separator;
        }
        target_uri_id = target_uri_id + relativePath;
        return target_uri_id;
    }

    public static String getMimeTypeUri(Context context, Uri uri) {
        return context.getContentResolver().getType(uri);
    }

    public static boolean isDirectoryUri(Context context, @NonNull final String target_file_path, @NonNull Uri tree_uri, String tree_uri_path) {
        Uri uri = getDocumentUri(target_file_path, tree_uri, tree_uri_path);
        if (uri != null) {
            String mime_type = getMimeTypeUri(context, uri);
            return mime_type.equals(DocumentsContract.Document.MIME_TYPE_DIR);
        } else {
            return false;
        }
    }

    public static boolean isDirectoryUri(Context context, @NonNull Uri uri) {
        String mime_type;
        Cursor cursor = context.getContentResolver().query(uri, new String[]{DocumentsContract.Document.COLUMN_MIME_TYPE}, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            mime_type = cursor.getString(0);
            cursor.close();
            if (mime_type == null) {
                return false;
            } else {
                return mime_type.equals(DocumentsContract.Document.MIME_TYPE_DIR);
            }
        }
        return false;
    }

    public static long getSizeUri(Context context, @NonNull Uri uri) {
        String size = "0";
        Cursor cursor = context.getContentResolver().query(uri, new String[]{DocumentsContract.Document.COLUMN_SIZE}, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            size = cursor.getString(0);
            cursor.close();
        }
        return Long.parseLong(size);
    }

    public static boolean existsUri(Context context, @NonNull final String target_file_path, @NonNull Uri tree_uri, String tree_uri_path) {
        Uri uri = getDocumentUri(target_file_path, tree_uri, tree_uri_path);
        if (uri != null) {
            return existsUri(context, uri);
        } else {
            return false;
        }
    }

    public static boolean existsUri(Context context, Uri uri) {
        return context.getContentResolver().getType(uri) != null;
    }

    public static boolean copy_File_FileModel(@NonNull final File sourceFile, @NonNull final FileModel destFileModel, String child_name, boolean cut, long[] bytes_read) {
        FileInputStream fileInputStream;
        OutputStream outputStream;
        boolean success = false;

        try {
            fileInputStream = new FileInputStream(sourceFile);
            outputStream = destFileModel.getChildOutputStream(child_name, 0);

            bufferedCopy(fileInputStream, outputStream, false, bytes_read);

            // Ensure FTP transfer is completed if it's an FTP output stream
            if (outputStream instanceof FtpFileModel.FTPOutputStreamWrapper) {
                ((FtpFileModel.FTPOutputStreamWrapper) outputStream).completePendingCommand();
            }

            if (cut) {
                sourceFile.delete();
            }

            success = true;
        } catch (Exception e) {
        }
        return success;
    }


    public static boolean copy_FileModel_FileModel(@NonNull final FileModel sourceFileModel, @NonNull final FileModel destFileModel, String child_name, boolean cut, long[] bytes_read) {
        InputStream inputStream;
        OutputStream outputStream;
        boolean success = false;

        try {
            inputStream = sourceFileModel.getInputStream();
            outputStream = destFileModel.getChildOutputStream(child_name, 0);

            boolean fromUsbFile = sourceFileModel instanceof UsbFileModel;
            bufferedCopy(inputStream, outputStream, fromUsbFile, bytes_read);

            // Ensure FTP transfer is completed if it's an FTP output stream
            if (outputStream instanceof FtpFileModel.FTPOutputStreamWrapper) {
                ((FtpFileModel.FTPOutputStreamWrapper) outputStream).completePendingCommand();
            }

            if (cut) {
                sourceFileModel.delete();
            }
            success = true;
        } catch (Exception e) {
        }
        return success;
    }

    public static boolean CopyAnyUriOrHttp(
            @NonNull Uri data,
            @NonNull FileModel destFileModel,
            @NonNull String fileName,
            long[] bytesRead
    ) {
        if (bytesRead == null || bytesRead.length == 0) {
            throw new IllegalArgumentException("bytesRead must be a long[1] array");
        }

        String scheme = data.getScheme();
        if (scheme == null) {
            return false;
        }
        scheme = scheme.toLowerCase();

        if ("http".equals(scheme) || "https".equals(scheme)) {
            // new HTTP(S) download path
            return CopyHttpUrlToFileModel(data, destFileModel, fileName, bytesRead);
        } else {
            return CopyUriFileModel(data, destFileModel, fileName, bytesRead);
        }
    }

    public static boolean CopyUriFileModel(@NonNull Uri data, FileModel destFileModel, String file_name, long[] bytes_read) {
        InputStream inStream;
        OutputStream fileOutStream;

        try {
            inStream = App.getAppContext().getContentResolver().openInputStream(data);
            fileOutStream = destFileModel.getChildOutputStream(file_name, 0);

            bufferedCopy(inStream, fileOutStream, false, bytes_read);

            // Ensure FTP transfer is completed if it's an FTP output stream
            if (fileOutStream instanceof FtpFileModel.FTPOutputStreamWrapper) {
                ((FtpFileModel.FTPOutputStreamWrapper) fileOutStream).completePendingCommand();
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean CopyHttpUrlToFileModel(
            @NonNull Uri data,
            @NonNull FileModel destFileModel,
            String fileName,
            long[] bytesRead
    ) {
        HttpURLConnection connection = null;

        try {
            URL url = new URL(data.toString());
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(15_000); // 15 seconds
            connection.setReadTimeout(30_000);    // 30 seconds
            connection.setInstanceFollowRedirects(true);

            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                // not a successful response
                return false;
            }

            InputStream inStream = new BufferedInputStream(connection.getInputStream());
            OutputStream outStream = destFileModel.getChildOutputStream(fileName, 0);

            // fromUsbFile = false (network)
            bufferedCopy(inStream, outStream, false, bytesRead);

            // FTP case: same behavior as CopyUriFileModel
            if (outStream instanceof FtpFileModel.FTPOutputStreamWrapper) {
                ((FtpFileModel.FTPOutputStreamWrapper) outStream).completePendingCommand();
            }

            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();  // streams already closed by bufferedCopy
            }
        }
    }


    @SuppressWarnings("null")
    public static boolean copy_File_File(@NonNull final File source, @NonNull final File target, boolean cut, long[] bytes_read) {
        try (FileInputStream fileInStream = new FileInputStream(source); FileOutputStream fileOutStream = new FileOutputStream(target)) {
            bufferedCopy(fileInStream, fileOutStream, false, bytes_read);
            if (cut) {
                // rename method does not work where move is between sd and internal memory. hence copy and cut
                deleteNativeFile(source);
            }

        } catch (Exception e) {
            return false;
        }
        return true;
    }


    @SuppressWarnings("null")
    public static boolean copy_File_SAFFile(Context context, @NonNull final File source, @NonNull String target_file_path, String name, Uri tree_uri, String tree_uri_path, boolean cut, long[] bytes_read) {
        OutputStream outStream;
        try (FileInputStream fileInStream = new FileInputStream(source)) {
            Uri uri = createDocumentUri(context, target_file_path, name, false, tree_uri, tree_uri_path);
            if (uri != null) {
                outStream = context.getContentResolver().openOutputStream(uri);
                if (outStream != null) {
                    bufferedCopy(fileInStream, outStream, false, bytes_read);
                }

                if (cut) {
                    deleteNativeFile(source);
                }
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static UsbFile getUsbFile(UsbFile rootUsbFile, String file_path) {
        if (rootUsbFile == null) {
            return null;
        }
        UsbFile usbFile = null;
        try {
            usbFile = rootUsbFile.search(Global.GET_TRUNCATED_FILE_PATH_USB(file_path));
        } catch (IOException | ConcurrentModificationException e) {
            return usbFile;
        }
        return usbFile;
    }


    public static FTPFile getFtpFile(FTPClient ftpClient, String file_path) {
        File file = new File(file_path);
        String parent_path = file.getParent();
        String name = file.getName();
        try {
            FTPFile[] ftpFiles_array = ftpClient.listFiles(parent_path);
            int size = ftpFiles_array.length;
            for (int i = 0; i < size; ++i) {
                FTPFile ftpFile = ftpFiles_array[i];
                if (ftpFile.getName().equals(name)) {
                    return ftpFile;
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }


    public static ChannelSftp.LsEntry getSftpEntry(ChannelSftp channelSftp, String path) {
        try {
            // Special handling for root directory
            if (path.equals("/")) {
                // Create a synthetic LsEntry for root if necessary
                // Alternatively, decide how to represent the root directory
                // Since root doesn't have a parent, you might skip it or handle differently
                // For simplicity, return null or throw an exception
                return null;
            }

            String normalizedPath = path;
            if (path.endsWith("/")) {
                normalizedPath = path.substring(0, path.length() - 1);
            }

            // Determine parent directory and target name
            int lastSlash = normalizedPath.lastIndexOf('/');
            String parentDir;
            String targetName;

            if (lastSlash != -1) {
                parentDir = normalizedPath.substring(0, lastSlash);
                targetName = normalizedPath.substring(lastSlash + 1);
                // Handle case where parentDir becomes empty (e.g., "/file.txt")
                if (parentDir.isEmpty()) {
                    parentDir = "/";
                }
            } else {
                parentDir = ".";
                targetName = normalizedPath;
            }

            // List entries in the parent directory
            Vector<ChannelSftp.LsEntry> entries = channelSftp.ls(parentDir);

            // Iterate through entries to find the target
            for (ChannelSftp.LsEntry entry : entries) {
                if (entry.getFilename().equals(targetName)) {
                    return entry;
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }


    private static boolean deleteNativeFile(@NonNull final File file) {
        if (file.delete()) {
            return true;
        }
        return !file.exists();
    }


    private static boolean deleteSAFFile(Context context, String target_file_path, Uri tree_uri, String tree_uri_path) {
        // Try with Storage Access Framework.
        Uri uri = getDocumentUri(target_file_path, tree_uri, tree_uri_path);
        try {
            return uri != null && DocumentsContract.deleteDocument(context.getContentResolver(), uri);
        } catch (FileNotFoundException | IllegalArgumentException e) {
            return false;
        }
    }

    public static boolean deleteFileModel(final FileModel fileModel) {
        if (fileModel == null) {
            return false;
        }

        Stack<FileModel> stack = new Stack<>();
        stack.push(fileModel);
        boolean success = true;

        while (!stack.isEmpty() && success) {
            FileModel currentFile = stack.pop();

            if (currentFile.isDirectory()) {
                FileModel[] list = currentFile.list();
                if (list != null && list.length > 0) {
                    // Push the current directory back onto the stack
                    stack.push(currentFile);
                    // Push all children onto the stack
                    for (FileModel child : list) {
                        stack.push(child);
                    }
                } else {
                    // Empty directory, try to delete it
                    success = currentFile.delete();
                    if (!success) {
                        System.err.println("Failed to delete directory: " + currentFile);
                    }
                }
            } else {
                // It's a file, try to delete it
                success = currentFile.delete();
                if (!success) {
                    System.err.println("Failed to delete file: " + currentFile);
                }
            }
        }

        // If the original fileModel was a directory and we've successfully deleted all its contents,
        // we need to delete the directory itself
        if (success && fileModel.isDirectory() && fileModel.exists()) {
            success = fileModel.delete();
            if (!success) {
                System.err.println("Failed to delete root directory: " + fileModel);
            }
        }
        return success;
    }


    public static boolean deleteNativeDirectory(final File folder) {
        if (folder == null || !folder.exists()) {
            return false;
        }

        Stack<File> stack = new Stack<>();
        stack.push(folder);
        boolean success = true;

        while (!stack.isEmpty() && success) {
            File current = stack.pop();

            if (current.isDirectory()) {
                File[] list = current.listFiles();
                if (list != null && list.length > 0) {
                    // Push the current directory back onto the stack
                    stack.push(current);
                    // Push all children onto the stack
                    for (File child : list) {
                        stack.push(child);
                    }
                } else {
                    // Empty directory, try to delete it
                    success = deleteNativeFile(current);
                    if (!success) {
                        System.err.println("Failed to delete directory: " + current);
                    }
                }
            } else {
                // It's a file, try to delete it
                success = deleteNativeFile(current);
                if (!success) {
                    System.err.println("Failed to delete file: " + current);
                }
            }
        }

        // If the original folder still exists (it was not empty initially),
        // we need to delete it now
        if (success && folder.exists()) {
            success = deleteNativeFile(folder);
            if (!success) {
                System.err.println("Failed to delete root folder: " + folder);
            }
        }
        return success;
    }


    public static boolean deleteSAFDirectory(Context context, final String file_path, Uri tree_uri, String tree_uri_path) {
        File folder = new File(file_path);
        if (!folder.exists()) {
            return false;
        }

        Stack<File> stack = new Stack<>();
        stack.push(folder);
        boolean success = true;

        while (!stack.isEmpty() && success) {
            File current = stack.pop();

            if (current.isDirectory()) {
                File[] list = current.listFiles();
                if (list != null && list.length > 0) {
                    // Push the current directory back onto the stack
                    stack.push(current);
                    // Push all children onto the stack
                    for (File child : list) {
                        stack.push(child);
                    }
                } else {
                    // Empty directory, try to delete it
                    success = deleteSAFFile(context, current.getAbsolutePath(), tree_uri, tree_uri_path);
                    if (!success) {
                        System.err.println("Failed to delete directory: " + current.getAbsolutePath());
                    }
                }
            } else {
                // It's a file, try to delete it
                success = deleteSAFFile(context, current.getAbsolutePath(), tree_uri, tree_uri_path);
                if (!success) {
                    System.err.println("Failed to delete file: " + current.getAbsolutePath());
                }
            }
        }

        // If the original folder still exists (it was not empty initially),
        // we need to delete it now
        if (success && folder.exists()) {
            success = deleteSAFFile(context, folder.getAbsolutePath(), tree_uri, tree_uri_path);
            if (!success) {
                System.err.println("Failed to delete root folder: " + folder.getAbsolutePath());
            }
        }
        return success;
    }


    public static boolean mkdirsNative(@NonNull final File file) {
        if (file.exists()) {
            return file.isDirectory();
        }
        return file.mkdirs();
    }


    public static boolean isFromInternal(FileObjectType fileObjectType, @NonNull final String file_path) {
        if (!fileObjectType.equals(FileObjectType.FILE_TYPE) && !fileObjectType.equals(FileObjectType.SEARCH_LIBRARY_TYPE)) {
            return false;
        }
        RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
        for (String internal_storage_path : repositoryClass.internal_storage_path_list) {
            if (Global.IS_CHILD_FILE(file_path, internal_storage_path)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isFilePathFromExternalStorage(FileObjectType fileObjectType, String file_path) {
        if (!fileObjectType.equals(FileObjectType.FILE_TYPE) && !fileObjectType.equals(FileObjectType.SEARCH_LIBRARY_TYPE)) {
            return false;
        }
        RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
        for (String external_path : repositoryClass.external_storage_path_list) {
            if (Global.IS_CHILD_FILE(file_path, external_path)) {
                return true;
            }
        }
        return false;
    }


    public static boolean isWritable(FileObjectType fileObjectType, @NonNull final String file_path) {
        return isFromInternal(fileObjectType, file_path);
    }

    public static String humanReadableByteCount(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        int z = (63 - Long.numberOfLeadingZeros(bytes)) / 10;
        return String.format("%.2f %sB", (double) bytes / (1L << (z * 10)), " KMGTPE".charAt(z));
    }


    @NonNull
    public static String getSdCardPath() {
        String sdCardDirectory = Environment.getExternalStorageDirectory().getAbsolutePath();
        try {
            sdCardDirectory = new File(sdCardDirectory).getCanonicalPath();
        } catch (IOException ioe) {
        }
        return sdCardDirectory;
    }

    public static String[] getExtSdCardPaths(Context context) {
        List<String> paths = new ArrayList<>();
        for (File file : context.getExternalFilesDirs("external")) {
            if (file != null && !file.equals(context.getExternalFilesDir("external"))) {
                int index = file.getAbsolutePath().lastIndexOf("/Android/data");
                if (index >= 0) {
                    String path = file.getAbsolutePath().substring(0, index);
                    try {
                        path = new File(path).getCanonicalPath();
                    } catch (IOException e) {
                        // Keep non-canonical path.
                    }
                    paths.add(path);
                } else {
                }
            }
        }
        return paths.toArray(new String[0]);
    }

    public static String getExtSdCardFolder(@NonNull final File file, Context context) {
        String[] extSdPaths = getExtSdCardPaths(context);
        try {
            for (String extSdPath : extSdPaths) {
                if (Global.IS_CHILD_FILE(file.getCanonicalPath(), extSdPath)) {
                    return extSdPath;
                }
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    public static boolean isOnExtSdCard(@NonNull final File file, Context context) {
        return getExtSdCardFolder(file, context) != null;
    }


    @Nullable
    public static String getFullPathFromTreeUri(@Nullable final Uri tree_uri, Context context) {
        if (tree_uri == null) {
            return null;
        }
        String volumePath = FileUtil.getVolumePath(FileUtil.getVolumeIdFromTreeUri(tree_uri), context);
        if (volumePath == null) {
            return File.separator;
        }
        if (volumePath.endsWith(File.separator)) {
            volumePath = volumePath.substring(0, volumePath.length() - 1);
        }

        String documentPath = FileUtil.getDocumentPathFromTreeUri(tree_uri);
        if (documentPath.endsWith(File.separator)) {
            documentPath = documentPath.substring(0, documentPath.length() - 1);
        }

        if (!documentPath.isEmpty()) {
            if (documentPath.startsWith(File.separator)) {
                return volumePath + documentPath;
            } else {
                return volumePath + File.separator + documentPath;
            }
        } else {
            return volumePath;
        }
    }


    private static String getVolumePath(final String volumeId, Context context) {
        try {
            StorageManager mStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);

            Class<?> storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");

            Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
            Method getUuid = storageVolumeClazz.getMethod("getUuid");
            Method getPath = storageVolumeClazz.getMethod("getPath");
            Method isPrimary = storageVolumeClazz.getMethod("isPrimary");
            Object result = getVolumeList.invoke(mStorageManager);

            final int length = Array.getLength(result);
            for (int i = 0; i < length; ++i) {
                Object storageVolumeElement = Array.get(result, i);
                String uuid = (String) getUuid.invoke(storageVolumeElement);
                Boolean primary = (Boolean) isPrimary.invoke(storageVolumeElement);

                // primary volume?
                if (primary && PRIMARY_VOLUME_NAME.equals(volumeId)) {
                    return (String) getPath.invoke(storageVolumeElement);
                }

                // other volumes?
                if (uuid != null) {
                    if (uuid.equals(volumeId)) {
                        return (String) getPath.invoke(storageVolumeElement);
                    }
                }
            }

            // not found.
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    private static String getVolumeIdFromTreeUri(final Uri tree_uri) {
        final String docId = DocumentsContract.getTreeDocumentId(tree_uri);
        final String[] split = docId.split(":");

        if (split.length > 0) {
            return split[0];
        } else {
            return null;
        }
    }


    private static String getDocumentPathFromTreeUri(final Uri tree_uri) {
        final String docId = DocumentsContract.getTreeDocumentId(tree_uri);
        final String[] split = docId.split(":");
        if ((split.length >= 2) && (split[1] != null)) {
            return split[1];
        } else {
            return File.separator;
        }
    }

    public static void bufferedCopy(InputStream inputStream, OutputStream outputStream, boolean fromUsbFile, long[] bytes_read) throws IOException {
        byte[] buffer = (fromUsbFile) ? new byte[USB_CHUNK_SIZE] : new byte[BUFFER_SIZE];
        int count;
        try (InputStream in = inputStream; OutputStream out = outputStream) {
            while ((count = in.read(buffer)) != -1) {
                out.write(buffer, 0, count);
                bytes_read[0] += count;
            }
            out.flush();
        } catch (IOException e) {
            throw e;
        }
    }
}
	
