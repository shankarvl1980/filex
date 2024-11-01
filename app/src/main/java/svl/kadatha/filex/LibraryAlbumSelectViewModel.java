package svl.kadatha.filex;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class LibraryAlbumSelectViewModel extends ViewModel {
    public final MutableLiveData<AsyncTaskStatus> asyncTaskStatus = new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final List<LibraryAlbumSelectDialog.LibraryDirPOJO> libraryDirPOJOS = new ArrayList<>();
    private boolean isCancelled;
    private Future<?> future1, future2;

    @Override
    protected void onCleared() {
        super.onCleared();
        cancel(true);
    }

    public void cancel(boolean mayInterruptRunning) {
        if (future1 != null) {
            future1.cancel(mayInterruptRunning);
        }
        if (future2 != null) {
            future2.cancel(mayInterruptRunning);
        }
        isCancelled = true;
    }

    private boolean isCancelled() {
        return isCancelled;
    }

    public void fetchAlbumDirectories(String library_type) {
        if (asyncTaskStatus.getValue() != AsyncTaskStatus.NOT_YET_STARTED) {
            return;
        }
        asyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future1 = executorService.submit(new Runnable() {
            @Override
            public void run() {

                libraryDirPOJOS.add(new LibraryAlbumSelectDialog.LibraryDirPOJO("All", "All", false));
                libraryDirPOJOS.addAll(Global.LIBRARY_FILTER_HASHMAP.get(library_type));
                asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });


    }


}
