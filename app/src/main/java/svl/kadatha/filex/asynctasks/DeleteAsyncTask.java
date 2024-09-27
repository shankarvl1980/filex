package svl.kadatha.filex.asynctasks;

import android.net.Uri;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import svl.kadatha.filex.AlternativeAsyncTask;
import svl.kadatha.filex.FileObjectType;
import svl.kadatha.filex.FilePOJO;
import svl.kadatha.filex.FilePOJOUtil;
import svl.kadatha.filex.filemodel.FileModel;
import svl.kadatha.filex.filemodel.FileModelFactory;
import timber.log.Timber;

public class DeleteAsyncTask extends AlternativeAsyncTask<Void, Void, Boolean> {
    public static final String TASK_TYPE = "delete";
    public final List<String> deleted_file_names = new ArrayList<>();
    public final List<String> deleted_files_path_list = new ArrayList<>();
    private final String source_folder;
    private final TaskProgressListener listener;
    private final List<String> files_selected_array;
    private final FileObjectType sourceFileObjectType;
    private final Uri source_uri;
    private final String source_uri_path;
    FileModel[] sourceFileModels;
    private int counter_no_files;
    private long counter_size_files;
    private String current_file_name;
    private String deleted_file_name;
    private FilePOJO filePOJO;

    public DeleteAsyncTask(ArrayList<String> files_selected_array, String source_folder, Uri source_uri, String source_uri_path, FileObjectType sourceFileObjectType, TaskProgressListener listener) {
        this.files_selected_array = files_selected_array;
        this.source_folder = source_folder;
        this.source_uri = source_uri;
        this.source_uri_path = source_uri_path;
        this.sourceFileObjectType = sourceFileObjectType;
        this.listener = listener;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        boolean success;
        sourceFileModels = FileModelFactory.getFileModelArray(files_selected_array, sourceFileObjectType, source_uri, source_uri_path);
        success = deleteFileModelArray(sourceFileModels, deleted_file_names, deleted_files_path_list);

        if (!deleted_file_names.isEmpty()) {
            if (sourceFileObjectType == FileObjectType.SEARCH_LIBRARY_TYPE) {
                FilePOJOUtil.REMOVE_FROM_HASHMAP_FILE_POJO_ON_REMOVAL_SEARCH_LIBRARY(source_folder, deleted_files_path_list, FileObjectType.FILE_TYPE);
            } else {
                FilePOJOUtil.REMOVE_FROM_HASHMAP_FILE_POJO(source_folder, deleted_file_names, sourceFileObjectType);
            }
        }
        return success;
    }

    private boolean deleteFileModelArray(FileModel[] sourceFileModels, List<String> deleted_file_names, List<String> deleted_files_path_list) {
        boolean success = false;
        int size = sourceFileModels.length;
        for (int i = 0; i < size; ++i) {
            if (isCancelled()) {
                return false;
            }

            FileModel fileModel = sourceFileModels[i];
            String file_path = fileModel.getPath();
            current_file_name = fileModel.getName();
            success = deleteFileModel(fileModel);

            if (success) {
                deleted_file_names.add(current_file_name);
                deleted_files_path_list.add(file_path);
            }
        }
        return success;
    }

    @Override
    protected void onProgressUpdate(Void value) {
        super.onProgressUpdate(value);
        if (listener != null) {
            listener.onProgressUpdate(TASK_TYPE, counter_no_files, counter_size_files, current_file_name, deleted_file_name);
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

    public boolean deleteFileModel(final FileModel fileModel) {
        Timber.tag("DeleteFileModel").d("Starting deletion of: " + fileModel.getPath());
        Stack<FileModel> stack = new Stack<>();
        stack.push(fileModel);

        boolean success = true;

        while (!stack.isEmpty()) {
            if (isCancelled()) {
                Timber.tag("DeleteFileModel").d("Operation cancelled");
                return false;
            }

            FileModel currentFile = stack.peek();  // Peek instead of pop
            Timber.tag("DeleteFileModel").d("Processing: " + currentFile.getPath());

            if (currentFile.isDirectory()) {
                Timber.tag("DeleteFileModel").d("This is found to be directory: " + currentFile.getPath());
                FileModel[] list = currentFile.list();
                if (list == null || list.length == 0) {
                    // Directory is empty or can't be read, try to delete it
                    stack.pop();
                    Timber.tag("DeleteFileModel").d("Attempting to delete empty directory: " + currentFile.getPath());
                    boolean deleteResult = deleteFile(currentFile);
                    success &= deleteResult;
                    Timber.tag("DeleteFileModel").d("Delete result for " + currentFile.getPath() + ": " + deleteResult);
                } else {
                    // Add children to the stack
                    Timber.tag("DeleteFileModel").d("Adding " + list.length + " children to stack for: " + currentFile.getPath());
                    for (FileModel child : list) {
                        stack.push(child);
                    }
                }
            } else {
                // It's a file, pop and delete it
                stack.pop();
                Timber.tag("DeleteFileModel").d("Attempting to delete file: " + currentFile.getPath());
                boolean deleteResult = deleteFile(currentFile);
                success &= deleteResult;
                Timber.tag("DeleteFileModel").d("Delete result for " + currentFile.getPath() + ": " + deleteResult);
            }
        }

        Timber.tag("DeleteFileModel").d("Deletion process completed. Overall success: " + success);
        return success;
    }

    private boolean deleteFile(FileModel file) {
        counter_no_files++;
        counter_size_files += (!file.isDirectory()) ? file.getLength() : 0;
        deleted_file_name = file.getName();
        publishProgress(null);
        return file.delete();
    }
}