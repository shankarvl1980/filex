package svl.kadatha.filex;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class ArchiveSetUpViewModel extends ViewModel {

    public final MutableLiveData<AsyncTaskStatus> isRecursiveFilesRemoved = new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public ArrayList<String> files_selected_array = new ArrayList<>();
    public String dest_folder = Global.INTERNAL_PRIMARY_STORAGE_PATH;
    public FileObjectType custom_dir_fileObjectType = FileObjectType.FILE_TYPE;
    public List<FilePOJO> destFilePOJOs;
    private boolean isCancelled;
    private Future<?> future1, future2;

    @Override
    protected void onCleared() {
        super.onCleared();
        cancel(true);
    }

    public void cancel(boolean mayInterruptRunning) {
        if (future1 != null) future1.cancel(mayInterruptRunning);
        if (future2 != null) future2.cancel(mayInterruptRunning);
        isCancelled = true;
    }

    private boolean isCancelled() {
        return isCancelled;
    }

    public void removeRecursiveFiles(ArrayList<String> files_selected_array, String archivedestfolder, FileObjectType destFileObjectType, FileObjectType sourceFileObjectType) {
        if (isRecursiveFilesRemoved.getValue() != AsyncTaskStatus.NOT_YET_STARTED) return;
        isRecursiveFilesRemoved.setValue(AsyncTaskStatus.STARTED);
        this.files_selected_array = files_selected_array;
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future1 = executorService.submit(new Runnable() {
            @Override
            public void run() {
                Global.REMOVE_RECURSIVE_PATHS(files_selected_array, sourceFileObjectType, archivedestfolder, destFileObjectType);
                isRecursiveFilesRemoved.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }
}
