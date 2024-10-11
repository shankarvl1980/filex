package svl.kadatha.filex.asynctasks;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import svl.kadatha.filex.AlternativeAsyncTask;
import svl.kadatha.filex.FileObjectType;
import svl.kadatha.filex.FilePOJO;
import svl.kadatha.filex.FilePOJOUtil;
import svl.kadatha.filex.FileUtil;
import svl.kadatha.filex.Global;
import svl.kadatha.filex.filemodel.FileModel;
import svl.kadatha.filex.filemodel.FileModelFactory;
import svl.kadatha.filex.filemodel.FtpFileModel;

public class UnarchiveAsyncTask extends AlternativeAsyncTask<Void, Void, Boolean> {

    public static final String TASK_TYPE = "archive-unzip";
    private final TaskProgressListener listener;
    private final String zip_folder_name;
    private final String zip_dest_path;
    private final String dest_folder;
    private final FileObjectType destFileObjectType;
    private final FileObjectType sourceFileObjectType;
    private final Uri tree_uri;
    private final String tree_uri_path;
    private final List<String> written_file_name_list;
    private final List<String> written_file_path_list;
    private final Set<String> first_part_entry_name_set;
    private final Set<String> first_part_entry_path_set;
    private final List<String> zip_entry_selected_array;
    private final String zip_file_path;
    private final long[] counter_size_files = new long[1];
    private String current_file_name;
    private FilePOJO filePOJO;
    private ZipFile zipfile;
    private int counter_no_files;


    public UnarchiveAsyncTask(String dest_folder, ArrayList<String> zip_entry_selected_array,FileObjectType sourceFileObjectType ,FileObjectType destFileObjectType, String zip_folder_name, String zip_file_path, Uri tree_uri, String tree_uri_path, TaskProgressListener listener) {
        this.dest_folder = dest_folder;
        this.zip_entry_selected_array = zip_entry_selected_array;
        this.destFileObjectType = destFileObjectType;
        this.sourceFileObjectType=sourceFileObjectType;
        this.zip_folder_name = zip_folder_name;
        this.zip_file_path = zip_file_path;
        this.tree_uri = tree_uri;
        this.tree_uri_path = tree_uri_path;
        this.listener = listener;

        written_file_name_list = new ArrayList<>();
        written_file_path_list = new ArrayList<>();
        first_part_entry_name_set = new HashSet<>();
        first_part_entry_path_set = new HashSet<>();

        if (zip_folder_name == null) {
            zip_dest_path = dest_folder;
        } else {
            zip_dest_path = Global.CONCATENATE_PARENT_CHILD_PATH(dest_folder, zip_folder_name);
        }
        current_file_name = new File(zip_file_path).getName();
    }


    @Override
    protected Boolean doInBackground(Void... params) {
        boolean success = false, isWritable = false;
        FileModel destFileModel = FileModelFactory.getFileModel(dest_folder, destFileObjectType, tree_uri, tree_uri_path);
        if (zip_folder_name != null) {
            if (!destFileModel.makeDirIfNotExists(zip_folder_name)) return false;
        }

        if (destFileObjectType == FileObjectType.FILE_TYPE) {
            try {
                zipfile = new ZipFile(zip_file_path);
            } catch (IOException e) {
                return unzip(zip_file_path, tree_uri, tree_uri_path, zip_dest_path);
            }
        } else if (Global.whether_file_cached(destFileObjectType)) {
            return unzip(zip_file_path, tree_uri, tree_uri_path, zip_dest_path);
        } else {
            return false;
        }


        if (!zip_entry_selected_array.isEmpty()) {
            for (String s : zip_entry_selected_array) {
                if (isCancelled()) {
                    return false;
                }
                ZipEntry zipEntry = zipfile.getEntry(s.substring(Global.ARCHIVE_EXTRACT_DIR.getAbsolutePath().length() + 1));
                success = read_zip_entry(zipEntry, zip_dest_path, tree_uri, tree_uri_path);
            }
        } else {
            Enumeration<? extends ZipEntry> zip_entries = zipfile.entries();
            while (zip_entries.hasMoreElements()) {
                if (isCancelled()) {
                    return false;
                }
                ZipEntry zipEntry = zip_entries.nextElement();
                success = read_zip_entry(zipEntry, zip_dest_path, tree_uri, tree_uri_path);
            }
        }

        if (zip_folder_name == null) {
            written_file_name_list.addAll(first_part_entry_name_set);
            written_file_path_list.addAll(first_part_entry_path_set);
        } else {
            written_file_name_list.add(zip_folder_name);
            written_file_path_list.add(zip_dest_path);
        }

        if (counter_no_files > 0){
            filePOJO = FilePOJOUtil.ADD_TO_HASHMAP_FILE_POJO(dest_folder, written_file_name_list, destFileObjectType, written_file_path_list);
        }
        return success;
    }

    @Override
    protected void onProgressUpdate(Void value) {
        super.onProgressUpdate(value);
        if (listener != null) {
            listener.onProgressUpdate(TASK_TYPE, counter_no_files, counter_size_files[0], current_file_name, current_file_name);
        }
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        if (listener != null) {
            listener.onTaskCompleted(TASK_TYPE, result, filePOJO);
        }
    }

    @Override
    protected void onCancelled(Boolean aBoolean) {
        super.onCancelled(aBoolean);
        if (listener != null) {
            listener.onTaskCancelled(TASK_TYPE, filePOJO);
        }
    }

    private boolean read_zip_entry(ZipEntry zipEntry, String zip_dest_path, Uri uri, String uri_path) {
        InputStream inStream;
        BufferedInputStream bufferedInputStream = null;
        Handler progressHandler = new Handler(Looper.getMainLooper());
        Runnable progressRunnable = new Runnable() {
            @Override
            public void run() {
                publishProgress(null);
                progressHandler.postDelayed(this, 1000); // Run every 1 second
            }
        };
        progressHandler.post(progressRunnable);
        try {
            inStream = zipfile.getInputStream(zipEntry);
            bufferedInputStream = new BufferedInputStream(inStream);
            String zip_entry_name = UNARCHIVE(zip_dest_path, zipEntry, destFileObjectType, uri, uri_path, bufferedInputStream, counter_size_files);
            ++counter_no_files;
            publishProgress(null);
            current_file_name = zip_entry_name;
            int idx = zip_entry_name.indexOf(File.separator);
            if (idx != -1) {
                String first_part = zip_entry_name.substring(0, idx);
                first_part_entry_name_set.add(first_part);
                first_part_entry_path_set.add(Global.CONCATENATE_PARENT_CHILD_PATH(zip_dest_path, first_part));
            } else {
                first_part_entry_name_set.add(zip_entry_name);
                first_part_entry_path_set.add(Global.CONCATENATE_PARENT_CHILD_PATH(zip_folder_name, zip_entry_name));
            }
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            progressHandler.removeCallbacks(progressRunnable);
            try {
                if (bufferedInputStream != null) {
                    bufferedInputStream.close();
                }
            } catch (Exception ignored) {

            }
        }
    }

    private boolean unzip(String zip_file_path, Uri uri, String uri_path, String zip_dest_path) {
        BufferedInputStream bufferedInputStream;
        ZipInputStream zipInputStream;
        FileModel sourceZipFileModel = FileModelFactory.getFileModel(zip_file_path, sourceFileObjectType, uri, uri_path);
        InputStream inputStream = sourceZipFileModel.getInputStream();
        if (inputStream == null) return false;
        bufferedInputStream = new BufferedInputStream(inputStream);
        zipInputStream = new ZipInputStream(bufferedInputStream);

        Handler progressHandler = new Handler(Looper.getMainLooper());
        Runnable progressRunnable = new Runnable() {
            @Override
            public void run() {
                publishProgress(null);
                progressHandler.postDelayed(this, 1000); // Run every 1 second
            }
        };
        progressHandler.post(progressRunnable);
        try {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null && !isCancelled()) {
                String zip_entry_name = UNARCHIVE(zip_dest_path, zipEntry, destFileObjectType, uri, uri_path, zipInputStream, counter_size_files);
                ++counter_no_files;
                publishProgress(null);
                current_file_name = zip_entry_name;
                String entry_name = zipEntry.getName();
                int idx = entry_name.indexOf(File.separator);
                if (idx != -1) {
                    String first_part = zip_entry_name.substring(0, idx);
                    first_part_entry_name_set.add(first_part);
                    first_part_entry_path_set.add((Global.CONCATENATE_PARENT_CHILD_PATH(zip_dest_path, first_part)));
                } else {
                    first_part_entry_name_set.add(zip_entry_name);
                    first_part_entry_path_set.add(Global.CONCATENATE_PARENT_CHILD_PATH(zip_folder_name, zip_entry_name));
                }
            }
            if (zip_folder_name == null) {
                written_file_name_list.addAll(first_part_entry_name_set);
                written_file_path_list.addAll(first_part_entry_path_set);
            } else {
                written_file_name_list.add(zip_folder_name);
                written_file_path_list.add(zip_dest_path);
            }
            zipInputStream.close();
            bufferedInputStream.close();
            if (counter_no_files > 0)
                filePOJO = FilePOJOUtil.ADD_TO_HASHMAP_FILE_POJO(dest_folder, written_file_name_list, destFileObjectType, written_file_path_list);
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            progressHandler.removeCallbacks(progressRunnable);
            try {
                zipInputStream.close();
            } catch (Exception e) {

            }
        }
    }

    public static String UNARCHIVE(String zip_dest_path, ZipEntry zipEntry, FileObjectType destFileObjectType, Uri uri, String uri_path, InputStream zipInputStream, long[] bytes_read) throws IOException {
        String zip_entry_name = zipEntry.getName();
        String dest_file_path = Global.CONCATENATE_PARENT_CHILD_PATH(zip_dest_path, zip_entry_name);
        File dest_file = new File(dest_file_path);

        FileModel fileModel = FileModelFactory.getFileModel(zip_dest_path, destFileObjectType, uri, uri_path);

        if (zipEntry.isDirectory()) {
            fileModel.makeDirsRecursively(zip_entry_name);
        } else {
            String parent_dest_file_path = Global.getParentPath(dest_file_path);

            FileModel zipEntryFileModel = FileModelFactory.getFileModel(parent_dest_file_path, destFileObjectType, uri, uri_path);
            boolean parent_dir_exists = zipEntryFileModel.exists();

            if (!parent_dir_exists) {
                String zip_entry_parent = new File(zip_entry_name).getParent();
                fileModel.makeDirsRecursively(zip_entry_parent);
            }

            OutputStream outStream;

            zipEntryFileModel = FileModelFactory.getFileModel(parent_dest_file_path, destFileObjectType, uri, uri_path);
            outStream = zipEntryFileModel.getChildOutputStream(dest_file.getName(), 0);

            if (outStream != null) {
                BufferedOutputStream bufferedOutStream = new BufferedOutputStream(outStream);
                byte[] b = new byte[FileUtil.BUFFER_SIZE];
                int bytesread;
                while ((bytesread = zipInputStream.read(b)) != -1) {
                    bufferedOutStream.write(b, 0, bytesread);
                    bytes_read[0] += bytesread;
                }
                bufferedOutStream.close();
                if (outStream instanceof FtpFileModel.FTPOutputStreamWrapper) {
                    ((FtpFileModel.FTPOutputStreamWrapper) outStream).completePendingCommand();
                }
            }
        }
        return zip_entry_name;
    }
}
