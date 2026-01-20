package svl.kadatha.filex;

import android.app.Application;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;


public class MainActivityViewModel extends AndroidViewModel {

    public final MutableLiveData<AsyncTaskStatus> isDeletionCompleted = new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    private final TinyDB tinyDB;
    public boolean archive_view, working_dir_open, clean_storage_shown, library_or_search_shown;
    public String toolbar_shown = "bottom";
    public Intent send_intent;
    private boolean isCancelled, show_usb_eject;
    private Future<?> future1, future2, future3, future4, future5, future6, future7, future8, future9, future10;

    public MainActivityViewModel(@NonNull Application application) {
        super(application);
        tinyDB = new TinyDB(application);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cancel(true);
        Global.DELETE_DIRECTORY_ASYNCHRONOUSLY(Global.ARCHIVE_EXTRACT_DIR);
        Global.DELETE_DIRECTORY_ASYNCHRONOUSLY(Global.ROOT_CACHE_DIR);
        Global.DELETE_DIRECTORY_ASYNCHRONOUSLY(Global.USB_CACHE_DIR);
        Global.DELETE_DIRECTORY_ASYNCHRONOUSLY(Global.FTP_CACHE_DIR);
        Global.DELETE_DIRECTORY_ASYNCHRONOUSLY(Global.SFTP_CACHE_DIR);
        Global.DELETE_DIRECTORY_ASYNCHRONOUSLY(Global.WEBDAV_CACHE_DIR);
        Global.DELETE_DIRECTORY_ASYNCHRONOUSLY(Global.SMB_CACHE_DIR);

        Global.DELETE_DIRECTORY_ASYNCHRONOUSLY(Global.CLOUD_CACHE_DIR);

        CacheClearer.performIfDecided(getApplication(), tinyDB);
    }

    public void cancel(boolean mayInterruptRunning) {
        if (future1 != null) {
            future1.cancel(mayInterruptRunning);
        }
        if (future2 != null) {
            future2.cancel(mayInterruptRunning);
        }
        if (future3 != null) {
            future3.cancel(mayInterruptRunning);
        }
        if (future4 != null) {
            future4.cancel(mayInterruptRunning);
        }
        if (future5 != null) {
            future5.cancel(mayInterruptRunning);
        }
        if (future6 != null) {
            future6.cancel(mayInterruptRunning);
        }
        if (future7 != null) {
            future7.cancel(mayInterruptRunning);
        }
        if (future8 != null) {
            future8.cancel(mayInterruptRunning);
        }
        if (future9 != null) {
            future9.cancel(mayInterruptRunning);
        }
        if (future10 != null) {
            future10.cancel(mayInterruptRunning);
        }
        isCancelled = true;
    }

    private boolean isCancelled() {
        return isCancelled;
    }


    public void deleteDirectory(File dir) {
        if (isDeletionCompleted.getValue() != AsyncTaskStatus.NOT_YET_STARTED) {
            return;
        }
        isDeletionCompleted.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService = MyExecutorService.getExecutorService();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                FileUtil.deleteNativeDirectory(dir);
                isDeletionCompleted.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }

    public void getAppList() {
        ExecutorService executorService = MyExecutorService.getExecutorService();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
                repositoryClass.populateAppsList();
            }
        });
    }

    public void getAudioPOJOList(boolean isCancelled) {
        ExecutorService executorService = MyExecutorService.getExecutorService();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
                repositoryClass.getAudioPOJOList(isCancelled);
            }
        });
    }

    public void getAlbumList(boolean isCancelled) {
        ExecutorService executorService = MyExecutorService.getExecutorService();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
                repositoryClass.getAlbumList(isCancelled);
            }
        });
    }


    public void getDownloadList(boolean isCancelled) {
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future2 = executorService.submit(new Runnable() {
            @Override
            public void run() {
                RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
                repositoryClass.getDownLoadList(isCancelled);
            }
        });
    }

    public void getDocumentList(boolean isCancelled) {
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future3 = executorService.submit(new Runnable() {
            @Override
            public void run() {
                RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
                repositoryClass.getDocumentList(isCancelled);
            }
        });
    }

    public void getImageList(boolean isCancelled) {
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future4 = executorService.submit(new Runnable() {
            @Override
            public void run() {
                RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
                repositoryClass.getImageList(isCancelled);
            }
        });
    }

    public void getAudioList(boolean isCancelled) {
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future5 = executorService.submit(new Runnable() {
            @Override
            public void run() {
                RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
                repositoryClass.getAudioList(isCancelled);
            }
        });
    }

    public void getVideoList(boolean isCancelled) {
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future6 = executorService.submit(new Runnable() {
            @Override
            public void run() {
                RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
                repositoryClass.getVideoList(isCancelled);
            }
        });
    }

    public void getArchiveList(boolean isCancelled) {
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future7 = executorService.submit(new Runnable() {
            @Override
            public void run() {
                RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
                repositoryClass.getArchiveList(isCancelled);
            }
        });
    }

    public void getApkList(boolean isCancelled) {
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future8 = executorService.submit(new Runnable() {
            @Override
            public void run() {
                RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
                repositoryClass.getApkList(isCancelled);
            }
        });
    }

    public void getLargeFileList(boolean isCancelled) {
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future9 = executorService.submit(new Runnable() {
            @Override
            public void run() {
                RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
                repositoryClass.getLargeFileList(isCancelled);
            }
        });
    }

    public void getDuplicateFileList(boolean isCancelled) {
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future10 = executorService.submit(new Runnable() {
            @Override
            public void run() {
                RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
                repositoryClass.getDuplicateFileList(isCancelled);
            }
        });
    }
}
