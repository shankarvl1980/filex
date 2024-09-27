package svl.kadatha.filex;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class StorageAnalyserActivityViewModel extends AndroidViewModel {
    String tool_bar_shown;
    private boolean isCancelled;
    private Future<?> future1, future2, future3;

    public StorageAnalyserActivityViewModel(@NonNull Application application) {
        super(application);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cancel(true);
        RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
        repositoryClass.hashmap_internal_directory_size = new HashMap<>();
        repositoryClass.hashmap_external_directory_size = new HashMap<>();
    }

    public void cancel(boolean mayInterruptRunning) {
        if (future1 != null) future1.cancel(mayInterruptRunning);
        if (future2 != null) future2.cancel(mayInterruptRunning);
        if (future3 != null) future3.cancel(mayInterruptRunning);
        isCancelled = true;
    }

    private boolean isCancelled() {
        return isCancelled;
    }


    public void getLargeFileList(boolean isCancelled) {
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future1 = executorService.submit(new Runnable() {
            @Override
            public void run() {
                RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
                repositoryClass.getLargeFileList(isCancelled);
            }
        });

    }

    public void getDuplicateFileList(boolean isCancelled) {
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future2 = executorService.submit(new Runnable() {
            @Override
            public void run() {
                RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
                repositoryClass.getDuplicateFileList(isCancelled);
            }
        });

    }
}
