package svl.kadatha.filex;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class ArchiveSetUpViewModel extends ViewModel {

    private boolean isCancelled;
    private Future<?> future1,future2;
    public final MutableLiveData<Boolean> isRecursiveFilesRemoved=new MutableLiveData<>();
    public ArrayList<String> files_selected_array=new ArrayList<>();
    public String folderclickselected=Global.INTERNAL_PRIMARY_STORAGE_PATH;
    public FileObjectType custom_dir_fileObjectType=FileObjectType.FILE_TYPE;


    @Override
    protected void onCleared() {
        super.onCleared();
        cancel(true);
    }

    public void cancel(boolean mayInterruptRunning){
        if(future1!=null) future1.cancel(mayInterruptRunning);
        if(future2!=null) future2.cancel(mayInterruptRunning);
        isCancelled=true;
    }

    private boolean isCancelled()
    {
        return isCancelled;
    }

    public void removeRecursiveFiles(ArrayList<String>files_selected_array,String archivedestfolder,FileObjectType destFileObjectType, FileObjectType sourceFileObjectType)
    {
        if(Boolean.TRUE.equals(isRecursiveFilesRemoved.getValue()))return;
        this.files_selected_array=files_selected_array;
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future1=executorService.submit(new Runnable() {
            @Override
            public void run() {
                Global.REMOVE_RECURSIVE_PATHS(files_selected_array,archivedestfolder,destFileObjectType,sourceFileObjectType);
                isRecursiveFilesRemoved.postValue(true);
            }
        });


    }

}
