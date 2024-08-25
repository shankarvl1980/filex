package svl.kadatha.filex.asynctasks;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import svl.kadatha.filex.AlternativeAsyncTask;
import svl.kadatha.filex.CopyToActivity;
import svl.kadatha.filex.FileObjectType;
import svl.kadatha.filex.FilePOJO;
import svl.kadatha.filex.FilePOJOUtil;
import svl.kadatha.filex.FileUtil;
import svl.kadatha.filex.Global;
import svl.kadatha.filex.filemodel.FileModel;
import svl.kadatha.filex.filemodel.FileModelFactory;

public class CopyToAsyncTask extends AlternativeAsyncTask<Void, Void, Boolean> {


    public static final String TASK_TYPE = "copy_to";
    private final TaskProgressListener listener;
    private List<Uri> data_list;
    private FileObjectType destFileObjectType;
    private String dest_folder;
    private Uri tree_uri;
    private String tree_uri_path;
    private List<String> copied_files_name;
    private Context context;
    private List<String> copied_source_file_path_list;
    private int counter_no_files;
    private long counter_size_files;
    private FilePOJO filePOJO;
    private String file_name;
    private String copied_file;
    final long[] bytes_read = new long[1];
    private List<String> overwritten_file_path_list;

    public CopyToAsyncTask(Context context,List<Uri>data_list,String file_name,String dest_folder,FileObjectType destFileObjectType,Uri tree_uri,String tree_uri_path,List<String>overwritten_file_path_list, TaskProgressListener listener) {
        this.context=context;
        this.data_list=data_list;
        this.file_name=file_name;
        this.dest_folder=dest_folder;
        this.destFileObjectType=destFileObjectType;
        this.tree_uri=tree_uri;
        this.tree_uri_path=tree_uri_path;
        this.overwritten_file_path_list=overwritten_file_path_list;
        this.listener = listener;
        this.copied_files_name=new ArrayList<>();
        this.copied_source_file_path_list=new ArrayList<>();
        this.counter_no_files=0;
        this.counter_size_files=0;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        if(destFileObjectType== FileObjectType.ROOT_TYPE)
        {
            return false;
        }
        if (isCancelled() || data_list == null || data_list.isEmpty()) {
            return false;
        }

        FileModel destFileModel= FileModelFactory.getFileModel(dest_folder,destFileObjectType,tree_uri,tree_uri_path);
        boolean onlyOneUri=data_list.size()==1;
        boolean copy_result = false;
        for(Uri data:data_list){
            if(!onlyOneUri) {
                file_name= CopyToActivity.getFileNameOfUri(context,data);
            }else{
                if(file_name.equals(""))file_name=CopyToActivity.getFileNameOfUri(context,data);
            }
            copy_result= FileUtil.CopyUriFileModel(data,destFileModel,file_name,bytes_read);
            if (copy_result) {
                String dest_file_path = Global.CONCATENATE_PARENT_CHILD_PATH(dest_folder, file_name);
                copied_files_name.add(file_name);
                copied_source_file_path_list.add(dest_file_path);
                counter_no_files++;
                counter_size_files+=getLengthUri(context,data);
                copied_file=file_name;
                publishProgress();
            }

        }
        if(counter_no_files>0){
            filePOJO= FilePOJOUtil.ADD_TO_HASHMAP_FILE_POJO(dest_folder,copied_files_name,destFileObjectType,overwritten_file_path_list);
        }


        copied_files_name.clear();
        copied_source_file_path_list.clear();

        return copy_result;
    }

    @Override
    protected void onProgressUpdate() {
        super.onProgressUpdate();
        if (listener != null) {
            listener.onProgressUpdate(TASK_TYPE, counter_no_files, counter_size_files, file_name,copied_file);
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
            listener.onTaskCancelled(TASK_TYPE,filePOJO);
        }
    }

    public static long getLengthUri(Context context, Uri uri)
    {
        long fileSize = 0;
        try (AssetFileDescriptor fileDescriptor = context.getContentResolver().openAssetFileDescriptor(uri, "r")) {
            fileSize = fileDescriptor.getLength();
        } catch (IOException e) {

        }
        finally {
            return fileSize;
        }
    }
}
