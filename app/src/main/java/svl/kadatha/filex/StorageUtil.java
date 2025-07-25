package svl.kadatha.filex;

import android.content.Context;
import android.os.Environment;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.os.EnvironmentCompat;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StorageUtil {
    static List<File> STORAGE_DIR;

    public static ArrayList<FilePOJO> getSdCardPaths(final Context context, final boolean includePrimaryExternalStorage) {
        final File[] externalFilesDirs = ContextCompat.getExternalFilesDirs(context, null);
        if (externalFilesDirs.length == 0) {
            return null;
        }
        if (externalFilesDirs.length == 1) {
            if (externalFilesDirs[0] == null) {
                return null;
            }
            final String storageState = EnvironmentCompat.getStorageState(externalFilesDirs[0]);
            if (!Environment.MEDIA_MOUNTED.equals(storageState)) {
                return null;
            }
            if (!includePrimaryExternalStorage && Environment.isExternalStorageEmulated()) {
                return null;
            }
        }
        final ArrayList<FilePOJO> result = new ArrayList<>();
        STORAGE_DIR = new ArrayList<>(); //This is to ensure check on duplicate file paths
        {
            STORAGE_DIR.add(new File("/"));
            result.add(new FilePOJO(FileObjectType.ROOT_TYPE, "/", null, "/", true, 0L, null, 0L, null, R.drawable.folder_icon, null, Global.ENABLE_ALFA, View.INVISIBLE, View.INVISIBLE, 0, 0L, null, 0, null, null));
        }

        int length = externalFilesDirs.length;
        for (int i = 0; i < length; ++i) {
            final File file = externalFilesDirs[i];
            if (file == null) {
                continue;
            }
            if (Environment.MEDIA_MOUNTED.equals(EnvironmentCompat.getStorageState(file))) {
                File f = new File(getRootOfInnerSdCardFolder(file));
                verify_add_file_path(f, result);
            }
        }

        File mnt = new File("/storage");
        if (!mnt.exists()) {
            mnt = new File("/mnt");
        }

        File[] fileList = mnt.listFiles(new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory() && pathname.exists()
                        && pathname.canRead() && pathname.canWrite() && !pathname.isHidden()
                        && isSymlink(pathname);
            }
        });

        if (fileList != null) {
            for (File f : fileList) {
                if (f.getAbsolutePath().equalsIgnoreCase(Environment.getExternalStorageDirectory().getAbsolutePath())) {
                    if (Environment.MEDIA_MOUNTED.equals(EnvironmentCompat.getStorageState(f))) {
                        verify_add_file_path(f, result);
                    }
                }
            }
        }

        if (result.isEmpty()) {
            return null;
        }
        return result;
    }

    private static void verify_add_file_path(File f, List<FilePOJO> result) {
        if (STORAGE_DIR.contains(f)) {
            return;
        }
        STORAGE_DIR.add(f);
        result.add(MakeFilePOJOUtil.MAKE_FilePOJO(f, false, FileObjectType.FILE_TYPE));
        int j = 0;
        File dummy_file;
        do {
            String fileName = "DummyFile" + (++j);
            dummy_file = new File(f, fileName);
        }
        while (dummy_file.exists());
        RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
        if (isWritable(dummy_file)) {
            repositoryClass.internal_storage_path_list.add(f.getAbsolutePath());
        }
        if (Environment.isExternalStorageRemovable(f)) {
            repositoryClass.external_storage_path_list.add(f.getAbsolutePath());
        }
    }

    public static boolean isWritable(@NonNull final File file) {
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
            // noinspection ResultOfMethodCallIgnored
            file.delete();
        }
        return result;
    }

    public static boolean isSymlink(File file) {
        try {
            File canon;
            if (file.getParent() == null) {
                canon = file;
            } else {
                File canonDir = file.getParentFile().getCanonicalFile();
                canon = new File(canonDir, file.getName());
            }
            return canon.getCanonicalFile().equals(canon.getAbsoluteFile());
        } catch (IOException e) {
            return true;
        }
    }

    /**
     * Given any file/folder inside an sd card, this will return the path of the sd card
     */
    private static String getRootOfInnerSdCardFolder(File file) {
        if (file == null) {
            return null;
        }

        String absolutePath = file.getAbsolutePath();
        return absolutePath.substring(0, absolutePath.indexOf("Android/data"));
    }

    static FilePOJO MAKE_FilePOJO_ROOT(File f, boolean extract_icon) {
        String name = f.getName();
        String path = f.getAbsolutePath();
        boolean isDirectory = f.isDirectory();
        long dateLong = f.lastModified();
        String date = Global.SDF.format(dateLong);
        long sizeLong = f.length();
        String si;

        String file_ext = "";
        int play_overlay_visible = View.INVISIBLE;
        int pdf_overlay_visible = View.INVISIBLE;
        float alfa = Global.ENABLE_ALFA;

        if (!isDirectory) {
            int idx = name.lastIndexOf(".");
            if (idx > 0) {
                file_ext = name.substring(idx + 1);
                if (file_ext.matches(Global.VIDEO_REGEX)) {
                    play_overlay_visible = View.VISIBLE;
                } else if (file_ext.matches(Global.PDF_REGEX)) {
                    pdf_overlay_visible = View.VISIBLE;
                }
            }
            si = FileUtil.humanReadableByteCount(sizeLong);
        } else {

            String sub_file_count = null;
            String[] file_list;
            if ((file_list = f.list()) != null) {
                sub_file_count = "(" + file_list.length + ")";

            }
            si = sub_file_count;
        }
        if (MainActivity.SHOW_HIDDEN_FILE && f.isHidden()) {
            alfa = Global.DISABLE_ALFA;
        }
        int type = 0;
        return new FilePOJO(FileObjectType.ROOT_TYPE, name, null, path, isDirectory, dateLong, date, sizeLong, si, type, file_ext, alfa, play_overlay_visible, pdf_overlay_visible, 0, 0L, null, 0, null, null);
    }
}
