package svl.kadatha.filex.asynctasks;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import svl.kadatha.filex.AlternativeAsyncTask;
import svl.kadatha.filex.CopyToActivity;
import svl.kadatha.filex.FileObjectType;
import svl.kadatha.filex.FilePOJO;
import svl.kadatha.filex.FilePOJOUtil;
import svl.kadatha.filex.FileUtil;
import svl.kadatha.filex.filemodel.FileModel;
import svl.kadatha.filex.filemodel.FileModelFactory;
import timber.log.Timber;

public class CopyToAsyncTask extends AlternativeAsyncTask<Void, Void, Boolean> {

    public static final String TASK_TYPE = "copy_to";
    private final TaskProgressListener listener;
    private final List<Uri> data_list;
    private final FileObjectType destFileObjectType;
    private final String dest_folder;
    private final Uri tree_uri;
    private final String tree_uri_path;
    private final List<String> copied_files_name;
    private final Context context;
    private final long[] counter_size_files = new long[1];
    private final List<String> overwritten_file_path_list;
    private int counter_no_files;
    private FilePOJO filePOJO;
    private String current_file_name;
    private String copied_file;

    public CopyToAsyncTask(Context context, List<Uri> data_list, String file_name, String dest_folder, FileObjectType destFileObjectType, Uri tree_uri, String tree_uri_path, List<String> overwritten_file_path_list, TaskProgressListener listener) {
        this.context = context;
        this.data_list = data_list;
        this.current_file_name = file_name;
        this.dest_folder = dest_folder;
        this.destFileObjectType = destFileObjectType;
        this.tree_uri = tree_uri;
        this.tree_uri_path = tree_uri_path;
        this.overwritten_file_path_list = overwritten_file_path_list;
        this.listener = listener;
        this.copied_files_name = new ArrayList<>();
        this.counter_no_files = 0;
        this.counter_size_files[0] = 0;
    }

    public static long getLengthUri(Context context, Uri uri) {
        long fileSize = 0;
        try (AssetFileDescriptor fileDescriptor = context.getContentResolver().openAssetFileDescriptor(uri, "r")) {
            fileSize = fileDescriptor.getLength();
        } catch (IOException e) {

        } finally {
            return fileSize;
        }
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        if (destFileObjectType == FileObjectType.ROOT_TYPE) {
            return false;
        }

        if (isCancelled() || data_list == null || data_list.isEmpty()) {
            return false;
        }

        Handler progressHandler = new Handler(Looper.getMainLooper());
        Runnable progressRunnable = new Runnable() {
            @Override
            public void run() {
                publishProgress(null);
                progressHandler.postDelayed(this, 1000); // Run every 1 second
            }
        };
        progressHandler.post(progressRunnable);
        FileModel destFileModel = FileModelFactory.getFileModel(dest_folder, destFileObjectType, tree_uri, tree_uri_path);
        boolean onlyOneUri = data_list.size() == 1;
        boolean copy_result = false;
        for (Uri data : data_list) {
            if (isCancelled()) {
                progressHandler.removeCallbacks(progressRunnable);
                return false;
            }
            if (!onlyOneUri) {
                current_file_name = CopyToActivity.getFileNameOfUri(context, data);
            } else {
                if (current_file_name.isEmpty())
                    current_file_name = CopyToActivity.getFileNameOfUri(context, data);
            }

            copy_result = FileUtil.CopyUriFileModel(data, destFileModel, current_file_name, counter_size_files);

            if (copy_result) {
                copied_files_name.add(current_file_name);
                counter_no_files++;
                copied_file = current_file_name;
                publishProgress(null);
            }
        }

        if (counter_no_files > 0) {
            filePOJO = FilePOJOUtil.ADD_TO_HASHMAP_FILE_POJO(dest_folder, copied_files_name, destFileObjectType, overwritten_file_path_list);
        }

        copied_files_name.clear();
        progressHandler.removeCallbacks(progressRunnable);
        return copy_result;
    }

    @Override
    protected void onProgressUpdate(Void value) {
        super.onProgressUpdate(value);
        if (listener != null) {
            listener.onProgressUpdate(TASK_TYPE, counter_no_files, counter_size_files[0], current_file_name, copied_file);
        }
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        Timber.tag("copy_to_result").d("copy_to_result in post execute - %s", result);
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
}
