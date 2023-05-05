package svl.kadatha.filex.ftpserver.utils;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import svl.kadatha.filex.FileObjectType;
import svl.kadatha.filex.Global;
import svl.kadatha.filex.UriPOJO;
import svl.kadatha.filex.ftpserver.ftp.FsSettings;
import timber.log.Timber;


public abstract class FtpServerFileUtil {

    private static final String LOG = "FileUtil";

    /**
     * Copy a file. The target file may even be on external SD card for Kitkat.
     *
     * @param source The source file
     * @param target The target file
     * @return true if the copying was successful.
     */
    @SuppressWarnings("null")
    private static boolean copyFile(final File source, final File target, Context context) {
        FileInputStream inStream = null;
        OutputStream outStream = null;
        FileChannel inChannel = null;
        FileChannel outChannel = null;
        try {
            inStream = new FileInputStream(source);

            // First try the normal way
            if (isWritable(target)) {
                // standard way
                outStream = new FileOutputStream(target);
                inChannel = inStream.getChannel();
                outChannel = ((FileOutputStream) outStream).getChannel();
                inChannel.transferTo(0, inChannel.size(), outChannel);
            } else {
                // Storage Access Framework
                DocumentFile targetDocument = getDocumentFile(target, false, context);
                if(targetDocument==null)return false;
                outStream =
                        context.getContentResolver().openOutputStream(targetDocument.getUri());

                if (outStream != null) {
                    // Both for SAF and for Kitkat, write to output stream.
                    byte[] buffer = new byte[16384]; // MAGIC_NUMBER
                    int bytesRead;
                    while ((bytesRead = inStream.read(buffer)) != -1) {
                        outStream.write(buffer, 0, bytesRead);
                    }
                }

            }
        } catch (Exception e) {
            Timber.tag(LOG).e(
                    "Error when copying file from " + source.getAbsolutePath() + " to " + target.getAbsolutePath(), e);
            return false;
        } finally {
            try {
                inStream.close();
            } catch (Exception e) {
                // ignore exception
            }

            try {
                outStream.close();
            } catch (Exception e) {
                // ignore exception
            }

            try {
                inChannel.close();
            } catch (Exception e) {
                // ignore exception
            }

            try {
                outChannel.close();
            } catch (Exception e) {
                // ignore exception
            }
        }
        return true;
    }

    public static FileOutputStream getOutputStream(final File target, Context context) throws FileNotFoundException {
        FileOutputStream outStream = null;
        // First try the normal way
        if (isWritable(target)) {
            // standard way
            outStream = new FileOutputStream(target);
        } else {
            // Storage Access Framework
            DocumentFile targetDocument = getDocumentFile(target, false, context);
            if(targetDocument!=null) outStream =  new ParcelFileDescriptor.AutoCloseOutputStream(context.getContentResolver().openFileDescriptor(targetDocument.getUri(), "rw"));
        }
        return outStream;
    }

    /**
     * Delete a file. May be even on external SD card.
     *
     * @param file the file to be deleted.
     * @return True if successfully deleted.
     */
    public static boolean deleteFile(@NonNull final File file, Context context) {
        // First try the normal deletion.
        if (file == null) return true;

        boolean fileDelete = false;
        if (file.isDirectory()) {
            fileDelete = rmdir(file, context);
        }
        if (file.delete() || fileDelete)
            return true;

        // Try with Storage Access Framework.
        if (FtpServerFileUtil.isOnExtSdCard(file, context)) {
            DocumentFile document = getDocumentFile(file, false, context);
            if (document == null) {
                return false;
            }
            return document.delete();
        }

        // Try the Kitkat workaround.

        return !file.exists();
    }

    private static boolean rename(File source, File target) {
        source.renameTo(target);
        return false;
    }

    /**
     * Move a file. The target file may even be on external SD card.
     *
     * @param source The source file
     * @param target The target file
     * @return true if the copying was successful.
     */
    public static boolean moveFile(@NonNull final File source, @NonNull final File target, Context context) {

        if (target.exists()) {
            return false;
        }

        // First try the normal rename.
        if (rename(source, target)) {
            return true;
        }

        // Try the Storage Access Framework if it is just a rename within the same parent folder.
        if (source.getParent().equals(target.getParent()) && FtpServerFileUtil.isOnExtSdCard(source, context)) {
            DocumentFile document = getDocumentFile(source, false, context);
            if (document == null) {
                return false;
            }
            try {
                if (DocumentsContract.renameDocument(context.getContentResolver(), document.getUri(), target.getName()) != null) {
                    return true;
                }
            } catch (Exception e) {
                return false;
            }
        }

        // if all else failed, try to copy the file and delete the source file
        return copyFile(source, target, context) && deleteFile(source, context);
    }

    /**
     * Rename a folder. In case of extSdCard in Kitkat, the old folder stays in place, but files are moved.
     *
     * @param source The source folder.
     * @param target The target folder.
     * @return true if the renaming was successful.
     */
    public static boolean renameFolder(@NonNull final File source, @NonNull final File target, Context context) {
        // First try the normal rename.
        if (rename(source, target)) {
            return true;
        }
        if (target.exists()) {
            return false;
        }

        // Try the Storage Access Framework if it is just a rename within the same parent folder.
        if (source.getParent().equals(target.getParent()) && FtpServerFileUtil.isOnExtSdCard(source, context)) {
            DocumentFile document = getDocumentFile(source, true, context);
            if (document == null) {
                return false;
            }
            try {
                if(DocumentsContract.renameDocument(context.getContentResolver(), document.getUri(), target.getName()) != null) {
                    return true;
                }
            } catch (Exception e) {
                return false;
            }
        }

        // Try the manual way, moving files individually.
        if (!mkdir(target, context)) {
            return false;
        }

        File[] sourceFiles = source.listFiles();

        if (sourceFiles == null) {
            return true;
        }

        for (File sourceFile : sourceFiles) {
            String fileName = sourceFile.getName();
            File targetFile = new File(target, fileName);
            if (!copyFile(sourceFile, targetFile, context)) {
                // stop on first error
                return false;
            }
        }
        // Only after successfully copying all files, delete files on source folder.
        for (File sourceFile : sourceFiles) {
            if (!deleteFile(sourceFile, context)) {
                // stop on first error
                return false;
            }
        }
        return true;
    }

    /**
     * Create a folder. The folder may even be on external SD card for Kitkat.
     *
     * @param file The folder to be created.
     * @return True if creation was successful.
     * @deprecated use {@link #mkdirs(Context, File)}
     */
    public static boolean mkdir(final File file, Context context) {
        if (file == null)
            return false;
        if (file.exists()) {
            // nothing to create.
            return file.isDirectory();
        }

        // Try the normal way
        if (file.mkdirs()) {
            return true;
        }

        // Try with Storage Access Framework.
        if (FtpServerFileUtil.isOnExtSdCard(file, context)) {
            DocumentFile document = getDocumentFile(file, true, context);
            if (document == null) {
                return false;
            }
            // getDocumentFile implicitly creates the directory.
            return document.exists();
        }

        // Try the Kitkat workaround.

        return false;
    }

    public static boolean mkdirs(Context context, File file) {
        boolean isSuccessful = true;
        isSuccessful = mkdir(new File(file.getPath()), context);
        return isSuccessful;
    }

    public static boolean mkfile(final File file, Context context) {
        if (file == null)
            return false;
        if (file.exists()) {
            // nothing to create.
            return !file.isDirectory();
        }

        // Try the normal way
        try {
            if (file.createNewFile()) {
                return true;
            }
        } catch (Exception e) {
            return false;
        }

        // Try with Storage Access Framework.
        if (FtpServerFileUtil.isOnExtSdCard(file, context)) {
            DocumentFile document = getDocumentFile(file.getParentFile(), true, context);
            if (document == null) {
                return false;
            }
            // getDocumentFile implicitly creates the directory.
            try {
                // use for SingleDocumentFile
                return DocumentsContract.createDocument(context.getContentResolver(), document.getUri(), DocumentsContract.Document.COLUMN_MIME_TYPE, file.getName()) != null;
                // return document.createFile("image", file.getName()) != null;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        return false;
    }

    /**
     * Delete a folder.
     *
     * @param file The folder name.
     * @return true if successful.
     */
    private static boolean rmdir(@NonNull final File file, Context context) {
        if (!file.exists()) return true;

        File[] files = file.listFiles();
        if (files != null && files.length > 0) {
            for (File child : files) {
                rmdir(child, context);
            }
        }

        // Try the normal way
        if (file.delete()) {
            return true;
        }

        // Try with Storage Access Framework.
        DocumentFile document = getDocumentFile(file, true, context);
        if (document != null && document.delete()) {
            return true;
        }

        // Try the Kitkat workaround.

        return !file.exists();
    }

    /**
     * Check if a file is readable.
     *
     * @param file The file
     * @return true if the file is readable.
     */
    public static boolean isReadable(final File file) {
        if (file == null)
            return false;
        if (!file.exists()) return false;

        boolean result;
        try {
            result = file.canRead();
        } catch (SecurityException e) {
            return false;
        }

        return result;
    }

    /**
     * Check if a file is writable. Detects write issues on external SD card.
     *
     * @param file The file
     * @return true if the file is writable.
     */
    public static boolean isWritable(final File file) {
        if (file == null)
            return false;
        boolean isExisting = file.exists();

        try {
            FileOutputStream output = new FileOutputStream(file, true);
            try {
                output.close();
            } catch (IOException e) {
                // do nothing.
            }
        } catch (FileNotFoundException e) {
            return false;
        }
        boolean result = file.canWrite();

        // Ensure that file is not created during this process.
        if (!isExisting) {
            file.delete();
        }

        return result;
    }

    // Utility methods for Android 5

    /**
     * Check for a directory if it is possible to create files within this directory, either via normal writing or via
     * Storage Access Framework.
     *
     * @param folder The directory
     * @return true if it is possible to write in this directory.
     */
    public static boolean isWritableNormalOrSaf(final File folder, Context c) {
        // Verify that this is a directory.
        if (folder == null)
            return false;
        if (!folder.exists() || !folder.isDirectory()) {
            return false;
        }

        // Find a non-existing file in this directory.
        int i = 0;
        File file;
        do {
            String fileName = "AugendiagnoseDummyFile" + (++i);
            file = new File(folder, fileName);
        } while (file.exists());

        // First check regular writability
        if (isWritable(file)) {
            return true;
        }

        // Next check SAF writability.
        DocumentFile document = getDocumentFile(file, false, c);

        if (document == null) {
            return false;
        }

        // This should have created the file - otherwise something is wrong with access URL.
        boolean result = document.canWrite() && file.exists();

        // Ensure that the dummy file is not remaining.
        deleteFile(file, c);
        return result;
    }

    /**
     * Get a list of external SD card paths. (Kitkat or higher.)
     *
     * @return A list of external SD card paths.
     */
    private static String[] getExtSdCardPaths(Context context) {
        List<String> paths = new ArrayList<>();
        for (File file : context.getExternalFilesDirs("external")) {
            if (file != null && !file.equals(context.getExternalFilesDir("external"))) {
                int index = file.getAbsolutePath().lastIndexOf("/Android/data");
                if (index < 0) {
                    Timber.tag(LOG).w( "Unexpected external file dir: " + file.getAbsolutePath());
                } else {
                    String path = file.getAbsolutePath().substring(0, index);
                    try {
                        path = new File(path).getCanonicalPath();
                    } catch (IOException e) {
                        // Keep non-canonical path.
                    }
                    paths.add(path);
                }
            }
        }
        if (paths.isEmpty()) paths.add("/storage/sdcard1");
        return paths.toArray(new String[0]);
    }

    public static String[] getExtSdCardPathsForActivity(Context context) {
        List<String> paths = new ArrayList<>();
        for (File file : context.getExternalFilesDirs("external")) {
            if (file != null) {
                int index = file.getAbsolutePath().lastIndexOf("/Android/data");
                if (index < 0) {
                    Timber.tag(LOG).w( "Unexpected external file dir: " + file.getAbsolutePath());
                } else {
                    String path = file.getAbsolutePath().substring(0, index);
                    try {
                        path = new File(path).getCanonicalPath();
                    } catch (IOException e) {
                        // Keep non-canonical path.
                    }
                    paths.add(path);
                }
            }
        }
        if (paths.isEmpty()) paths.add("/storage/sdcard1");
        return paths.toArray(new String[0]);
    }

    /**
     * Determine the main folder of the external SD card containing the given file.
     *
     * @param file the file.
     * @return The main folder of the external SD card containing this file, if the file is on an SD card. Otherwise,
     * null is returned.
     */
    private static String getExtSdCardFolder(final File file, Context context) {
        String[] extSdPaths = getExtSdCardPaths(context);
        try {
            for (int i = 0; i < extSdPaths.length; i++) {
                if (file.getCanonicalPath().startsWith(extSdPaths[i])) {
                    return extSdPaths[i];
                }
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    /**
     * Determine if a file is on external sd card. (Kitkat or higher.)
     *
     * @param file The file.
     * @return true if on external sd card.
     */
    public static boolean isOnExtSdCard(final File file, Context c) {
        return getExtSdCardFolder(file, c) != null;
    }

    /**
     * Get a DocumentFile corresponding to the given file (for writing on ExtSdCard on Android 5). If the file is not
     * existing, it is created.
     *
     * @param file        The file.
     * @param isDirectory flag indicating if the file should be a directory.
     * @return The DocumentFile
     */
    public static DocumentFile getDocumentFile(final File file, final boolean isDirectory, Context context) {
        String baseFolder = getExtSdCardFolder(file, context);
        boolean originalDirectory = false;
        if (baseFolder == null) {
            return null;
        }

        String relativePath = null;
        try {
            String fullPath = file.getCanonicalPath();
            if (!baseFolder.equals(fullPath))
                relativePath = fullPath.substring(baseFolder.length() + 1);
            else originalDirectory = true;
        } catch (IOException e) {
            return null;
        } catch (Exception f) {
            originalDirectory = true;
            //continue
        }


        Uri treeUri = null;
        File parent_file=file.getParentFile();
        if(parent_file!=null)
        {
            if(!parent_file.exists()) return null;
        }
        else {
            return null;
        }

        UriPOJO uriPOJO= Global.CHECK_AVAILABILITY_URI_PERMISSION(parent_file.getAbsolutePath(), FileObjectType.FILE_TYPE);
        if(uriPOJO!=null)
        {
            treeUri=uriPOJO.get_uri();
        }
        if (treeUri == null) {
            return null;
        }
        if(!uriPOJO.get_path().equals(baseFolder))
        {
            return null;
        }

        if (file.exists()) {
            Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri) + relativePath);
            DocumentFile document = DocumentFile.fromSingleUri(context, documentUri);
            if (document != null) {
                return document;
            }
        }

        // start with root of SD card and then parse through document tree.
        DocumentFile document = DocumentFile.fromTreeUri(context, treeUri);
        if (originalDirectory) return document;
        String[] parts = relativePath.split("/");
        for (int i = 0; i < parts.length; i++) {
            DocumentFile nextDocument = document.findFile(parts[i]);

            if (nextDocument == null) {
                if ((i < parts.length - 1) || isDirectory) {
                    nextDocument = document.createDirectory(parts[i]);
                } else {
                    nextDocument = document.createFile(DocumentsContract.Document.COLUMN_MIME_TYPE, parts[i]);
                }
            }
            document = nextDocument;
        }

        return document;
    }

    // Utility methods for Kitkat

    /**
     * Checks whether the target path exists or is writable
     *
     * @param f       the target path
     * @param context
     * @return 1 if exists or writable, 0 if not writable
     */
    public static int checkFolder(final String f, Context context) {
        if (f == null) return 0;

        File folder = new File(f);
        if (FtpServerFileUtil.isOnExtSdCard(folder, context)) {
            if (!folder.exists() || !folder.isDirectory()) {
                return 0;
            }

            // On Android 5, trigger storage access framework.
            if (FtpServerFileUtil.isWritableNormalOrSaf(folder, context)) {
                return 1;

            }
        } else if (folder.canWrite()) {
            return 1;
        } else {
            return 0;
        }
        return 0;
    }
}