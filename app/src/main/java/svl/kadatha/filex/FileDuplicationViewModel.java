package svl.kadatha.filex;

import android.content.Context;
import android.net.Uri;
import android.provider.DocumentsContract;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class FileDuplicationViewModel extends ViewModel {

    public final MutableLiveData<AsyncTaskStatus> asyncTaskStatus=new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> filterSelectedArrayAsyncTaskStatus=new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    private boolean isCancelled;
    private Future<?> future1,future2,future3;

    public List<FilePOJO> filePOJOS;
    public List<String> source_duplicate_file_path_array;
    public List<String> destination_duplicate_file_path_array;
    public ArrayList<String> not_to_be_replaced_files_path_array;
    public ArrayList<String> overwritten_file_path_list;
    public boolean directoriesRemoved;

    public String source_folder,dest_folder;
    public FileObjectType sourceFileObjectType,destFileObjectType;
    boolean cut;
    public ArrayList<String>files_selected_array;
    public ArrayList<Uri> data_list=new ArrayList<>();
    public boolean yes,apply_to_all;


    @Override
    protected void onCleared() {
        super.onCleared();
        cancel(true);
    }

    public void cancel(boolean mayInterruptRunning){
        if(future1!=null) future1.cancel(mayInterruptRunning);
        if(future2!=null) future2.cancel(mayInterruptRunning);
        if(future3!=null) future3.cancel(mayInterruptRunning);
        isCancelled=true;
    }

    private boolean isCancelled()
    {
        return isCancelled;
    }


    public void checkForExistingFileWithSameName(String source_folder,FileObjectType sourceFileObjectType, String dest_folder,FileObjectType destFileObjectType,ArrayList<String>files_selected_array, boolean cut ,boolean findAllDuplicates, ArrayList<Uri> data_list)
    {
        if(asyncTaskStatus.getValue()!=AsyncTaskStatus.NOT_YET_STARTED)return;
        asyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        this.source_folder=source_folder;
        this.sourceFileObjectType=sourceFileObjectType;
        this.dest_folder=dest_folder;
        this.destFileObjectType=destFileObjectType;
        this.cut=cut;
        this.files_selected_array=files_selected_array;
        source_duplicate_file_path_array=new ArrayList<>();
        not_to_be_replaced_files_path_array=new ArrayList<>();
        destination_duplicate_file_path_array=new ArrayList<>();
        overwritten_file_path_list=new ArrayList<>();
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future1=executorService.submit(new Runnable() {
            @Override
            public void run() {
                if(sourceFileObjectType==FileObjectType.SEARCH_LIBRARY_TYPE)
                {
                    boolean to_remove_directories = data_list != null;
                    if(to_remove_directories){
                        Iterator<Uri> data_list_iterator=data_list.iterator();
                        while(data_list_iterator.hasNext()){
                            Uri data=data_list_iterator.next();
                            if(isDirectoryUri(App.getAppContext(),data)){
                                data_list_iterator.remove();
                                directoriesRemoved=true;
                            }
                        }
                    }

                    //process to have only files with unique file names
                    Set<String> file_name_set=new HashSet<>();
                    Iterator<String> iterator=files_selected_array.iterator();

                    while(iterator.hasNext())
                    {
                        String file_path=iterator.next();
                        boolean inserted=file_name_set.add(new File(file_path).getName());
                        if(!inserted) {
                            iterator.remove();
                        }
                    }
                }
                Global.REMOVE_RECURSIVE_PATHS(files_selected_array,dest_folder,destFileObjectType,sourceFileObjectType);
                RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
                filePOJOS=repositoryClass.hashmap_file_pojo.get(destFileObjectType+dest_folder);
                int filePojoSize=filePOJOS.size();
                int fileSelectedSize=files_selected_array.size();
                FilePOJO filePOJO;
                String file_path;
                boolean stop_loop = false;
                for(int i=0;i<filePojoSize;++i)
                {
                    filePOJO=filePOJOS.get(i);
                    for(int j=0;j<fileSelectedSize;++j)
                    {
                        file_path=files_selected_array.get(j);
                        if(filePOJO.getName().equals(new File(file_path).getName()))
                        {
                            source_duplicate_file_path_array.add(file_path);
                            destination_duplicate_file_path_array.add(filePOJO.getPath());

                            if(!findAllDuplicates)
                            {
                                stop_loop=true;
                                break;
                            }

                        }

                    }
                    if(stop_loop)break;
                }
                asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }

    public void filterFileSelectedArray(Context context,boolean yes,boolean apply_to_all,ArrayList<Uri>data_list){
        if(filterSelectedArrayAsyncTaskStatus.getValue()!=AsyncTaskStatus.NOT_YET_STARTED)return;
        filterSelectedArrayAsyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        this.data_list=data_list;
        this.yes=yes;this.apply_to_all=apply_to_all;
        ExecutorService executorService=MyExecutorService.getExecutorService();
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                if(yes){
                    if(apply_to_all){
                        files_selected_array.removeAll(not_to_be_replaced_files_path_array);
                        overwritten_file_path_list.addAll(destination_duplicate_file_path_array);
                        removeNotTobeCopiedUris(context,data_list,not_to_be_replaced_files_path_array);
                    }
                    else{
                        files_selected_array.removeAll(not_to_be_replaced_files_path_array);
                        removeNotTobeCopiedUris(context,data_list,not_to_be_replaced_files_path_array);
                    }
                }
                else{
                    if(apply_to_all){
                        files_selected_array.removeAll(source_duplicate_file_path_array);
                        removeNotTobeCopiedUris(context,data_list,source_duplicate_file_path_array);
                    }
                    else{
                        files_selected_array.removeAll(not_to_be_replaced_files_path_array);
                        removeNotTobeCopiedUris(context,data_list,not_to_be_replaced_files_path_array);
                    }
                }
                filterSelectedArrayAsyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }

    private void removeNotTobeCopiedUris(Context context, List<Uri> data_list, List<String> file_path_list){
        if(data_list==null || data_list.isEmpty() || file_path_list.isEmpty()) return;
        Iterator<Uri> iterator=data_list.iterator();
        while (iterator.hasNext()){
            String name=CopyToActivity.getFileNameOfUri(context,iterator.next());
            for(String f_name:file_path_list){
                if(name.equals(f_name)) {
                    iterator.remove();
                    break;
                }

            }
        }
    }

    public static boolean isDirectoryUri(Context context, @NonNull Uri uri) {
        // First, try to get the mime type
        String mimeType = null;
        try {
            mimeType = context.getContentResolver().getType(uri);
        } catch (Exception e) {

        }

        // Check if it's a directory based on mime type
        if (mimeType != null) {
            if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType)) {
                return true;
            } else {
                // If we have a non-null MIME type that isn't a directory, it's likely a file
                return false;
            }
        }


        // If all checks fail, assume it's not a directory
        return false;
    }
}
