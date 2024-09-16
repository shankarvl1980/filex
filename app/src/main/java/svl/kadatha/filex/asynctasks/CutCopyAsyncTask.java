package svl.kadatha.filex.asynctasks;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.core.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import svl.kadatha.filex.AlternativeAsyncTask;
import svl.kadatha.filex.FileObjectType;
import svl.kadatha.filex.FilePOJO;
import svl.kadatha.filex.FilePOJOUtil;
import svl.kadatha.filex.FileUtil;
import svl.kadatha.filex.Global;
import svl.kadatha.filex.filemodel.FileModel;
import svl.kadatha.filex.filemodel.FileModelFactory;
import timber.log.Timber;

public class CutCopyAsyncTask extends AlternativeAsyncTask<Void, Void, Boolean> {
    public static final String TASK_TYPE_CUT = "paste-cut";
    public static final String TASK_TYPE_COPY = "paste-copy";
    private final TaskProgressListener listener;
    private final List<String> copied_files_name;
    private final List<String> copied_source_file_path_list;
    private final List<String> overwritten_file_path_list;
    private int counter_no_files;
    private final long[] counter_size_files=new long[1];
    private String copied_file_name;
    private String current_file_name;
    private FilePOJO filePOJO;
    private final List<String> files_selected_array;
    private final FileObjectType sourceFileObjectType;
    private final Uri tree_uri;
    private final String tree_uri_path;
    private final String source_folder;
    private final String dest_folder;
    private final FileObjectType destFileObjectType;
    private final boolean cut;
    private final Uri source_uri;
    private final String source_uri_path;


    public CutCopyAsyncTask(ArrayList<String> files_selected_array, String source_folder, FileObjectType sourceFileObjectType, Uri source_uri, String source_uri_path,
                        String dest_folder, FileObjectType destFileObjectType,Uri tree_uri, String tree_uri_path, boolean cut, List<String>overwritten_file_path_list,TaskProgressListener listener) {
    this.files_selected_array = files_selected_array;
    this.source_folder=source_folder;
    this.sourceFileObjectType = sourceFileObjectType;
    this.source_uri=source_uri;
    this.source_uri_path=source_uri_path;
    this.dest_folder = dest_folder;
    this.destFileObjectType = destFileObjectType;
    this.tree_uri=tree_uri;
    this.tree_uri_path=tree_uri_path;
    this.cut = cut;
    this.overwritten_file_path_list=overwritten_file_path_list;
    this.listener = listener;

    copied_files_name = new ArrayList<>();
    copied_source_file_path_list = new ArrayList<>();

}
    @Override
    protected Boolean doInBackground(Void... params) {
        boolean copy_result = false;
        if(sourceFileObjectType== FileObjectType.ROOT_TYPE || destFileObjectType==FileObjectType.ROOT_TYPE)
        {
            if(destFileObjectType==FileObjectType.USB_TYPE || sourceFileObjectType==FileObjectType.USB_TYPE)
            {
                return false;
            }

        }
        else {

            FileModel[] sourceFileModels = FileModelFactory.getFileModelArray(files_selected_array, sourceFileObjectType, source_uri, source_uri_path);
            FileModel destFileModel = FileModelFactory.getFileModel(dest_folder, destFileObjectType, tree_uri, tree_uri_path);

            int size = sourceFileModels.length;
            for (int i = 0; i < size; ++i) {
                if (isCancelled()) return false;
                FileModel sourceFileModel = sourceFileModels[i];
                String file_path = sourceFileModel.getPath();
                current_file_name = sourceFileModel.getName();
                boolean isSourceFromInternal = FileUtil.isFromInternal(sourceFileObjectType, file_path);
                if (sourceFileObjectType == FileObjectType.FILE_TYPE) {
                    if (isSourceFromInternal) {
                        copy_result = CopyFileModel(sourceFileModel, destFileModel, current_file_name, cut);

                    } else // that is cut and paste  from external directory
                    {
                        copy_result = CopyFileModel(sourceFileModel, destFileModel, current_file_name, false);
                        if (copy_result && cut) {
                            sourceFileModel.delete();
                        }
                    }
                } else {
                    copy_result = CopyFileModel(sourceFileModel, destFileModel, current_file_name, false);
                    if (copy_result && cut) {
                        sourceFileModel.delete();
                    }
                }


                if (copy_result) {
                    copied_files_name.add(current_file_name);
                    copied_source_file_path_list.add(file_path);
                }

                files_selected_array.remove(file_path);
            }
        }
        if(counter_no_files>0)
        {
            filePOJO= FilePOJOUtil.ADD_TO_HASHMAP_FILE_POJO(dest_folder,copied_files_name,destFileObjectType,overwritten_file_path_list);
            if(cut)
            {
                if(sourceFileObjectType==FileObjectType.SEARCH_LIBRARY_TYPE)
                {
                    FilePOJOUtil.REMOVE_FROM_HASHMAP_FILE_POJO_ON_REMOVAL_SEARCH_LIBRARY(source_folder,copied_source_file_path_list,FileObjectType.FILE_TYPE);
                }
                else
                {
                    FilePOJOUtil.REMOVE_FROM_HASHMAP_FILE_POJO(source_folder,copied_files_name,sourceFileObjectType);
                }

            }
            copied_files_name.clear();
            copied_source_file_path_list.clear();
        }
        return copy_result;
    }

    @Override
    protected void onProgressUpdate(Void value) {
        super.onProgressUpdate(value);
        if (listener != null) {
            listener.onProgressUpdate(cut ? TASK_TYPE_CUT:TASK_TYPE_COPY, counter_no_files, counter_size_files[0],current_file_name ,copied_file_name);
        }
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        if (listener != null) {
            listener.onTaskCompleted(cut ? TASK_TYPE_CUT:TASK_TYPE_COPY, result, filePOJO);
        }
    }

    @Override
    protected void onCancelled(Boolean aBoolean) {
        super.onCancelled(aBoolean);
        if (listener != null) {
            listener.onTaskCancelled(cut ? TASK_TYPE_CUT:TASK_TYPE_COPY,filePOJO);
        }
    }


    public boolean CopyFileModel(FileModel sourceFileModel, FileModel destFileModel, String current_file_name, boolean cut) {
        Timber.tag("CopyFileModel").d("Starting copy operation. Source: " + sourceFileModel.getPath() + ", Destination: " + destFileModel.getPath());

        Stack<Pair<FileModel, FileModel>> stack = new Stack<>();
        stack.push(new Pair<>(sourceFileModel, destFileModel));

        boolean allCopiesSuccessful = true;

        // Create a handler for regular progress updates
        Handler progressHandler = new Handler(Looper.getMainLooper());
        Runnable progressRunnable = new Runnable() {
            @Override
            public void run() {
                publishProgress(null);
                progressHandler.postDelayed(this, 1000); // Run every 1 second
            }
        };

        while (!stack.isEmpty()) {
            if (isCancelled()) {
                Timber.tag("CopyFileModel").d("Operation cancelled");
                progressHandler.removeCallbacks(progressRunnable); // Stop the handler
                return false;
            }

            Pair<FileModel, FileModel> pair = stack.pop();
            FileModel source = pair.first;
            FileModel dest = pair.second;

            Timber.tag("CopyFileModel").d("Processing: " + source.getPath());

            if (source.isDirectory()) {
                String newDirName = source.getName();
                String destPath = Global.CONCATENATE_PARENT_CHILD_PATH(dest.getPath(), newDirName);
                Timber.tag("CopyFileModel").d("Creating directory: " + destPath);

                if (!dest.makeDirIfNotExists(newDirName)) {
                    Timber.tag("CopyFileModel").e("Failed to create directory: " + destPath);
                    allCopiesSuccessful = false;
                    break;
                }

                FileModel childDestFileModel = FileModelFactory.getFileModel(destPath, destFileObjectType, tree_uri, tree_uri_path);
                FileModel[] sourceChildFileModels = source.list();

                if (sourceChildFileModels != null) {
                    Timber.tag("CopyFileModel").d("Adding " + sourceChildFileModels.length + " child items to stack");
                    for (FileModel childSource : sourceChildFileModels) {
                        stack.push(new Pair<>(childSource, childDestFileModel));
                    }
                } else {
                    Timber.tag("CopyFileModel").w("No child items found in directory: " + source.getPath());
                }

                ++counter_no_files;
                publishProgress(null);
            } else {
                Timber.tag("CopyFileModel").d("Copying file: " + source.getPath());

                copied_file_name = source.getName();

                // Start the progress handler
                progressHandler.post(progressRunnable);

                boolean success = FileUtil.copy_FileModel_FileModel(source, dest, source.getName(), cut, counter_size_files);
                if(success){
                    ++counter_no_files;
                    publishProgress(null);
                }

                // Stop the progress handler
                progressHandler.removeCallbacks(progressRunnable);

                if (!success) {
                    Timber.tag("CopyFileModel").e("Failed to copy file: " + source.getPath());
                    allCopiesSuccessful = false;
                    break;
                }
            }
        }

        if (cut && allCopiesSuccessful) {
            Timber.tag("CopyFileModel").d("Deleting source after successful cut operation: " + sourceFileModel.getPath());
            FileUtil.deleteFileModel(sourceFileModel);
        } else if (cut) {
            Timber.tag("CopyFileModel").d("Not deleting source due to unsuccessful copy during cut operation");
        }

        Timber.tag("CopyFileModel").d("Copy operation completed. All copies successful: " + allCopiesSuccessful);
        return allCopiesSuccessful;
    }

}