package svl.kadatha.filex;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;


public class MainActivityViewModel extends AndroidViewModel {

    private final Application application;
    private boolean isCancelled;
    private Future<?> future1,future2,future3,future4,future5,future6,future7,future8,future9;

    public final MutableLiveData<AsyncTaskStatus> isDeletionCompleted=new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public boolean checkedSAFPermissionPasteSetUp;
    private final TinyDB tinyDB;

    public boolean archive_view,working_dir_open,library_or_search_shown,network_shown;
    public String toolbar_shown="bottom";


    public MainActivityViewModel(@NonNull Application application) {
        super(application);
        this.application=application;
        tinyDB=new TinyDB(application);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cancel(true);
        Global.REMOVE_USB_URI_PERMISSIONS();
        Global.DELETE_DIRECTORY_ASYNCHRONOUSLY(Global.ARCHIVE_EXTRACT_DIR);
        Global.DELETE_DIRECTORY_ASYNCHRONOUSLY(Global.USB_CACHE_DIR);
        FtpClientRepository.getInstance().disconnect_ftp_clients();
        if(Global.WHETHER_TO_CLEAR_CACHE_TODAY)
        {
            Global.DELETE_DIRECTORY_ASYNCHRONOUSLY(application.getCacheDir());
            if(Global.SIZE_APK_ICON_LIST>800)
            {
                Global.DELETE_DIRECTORY_ASYNCHRONOUSLY(Global.APK_ICON_DIR);
            }
            tinyDB.putInt("cache_cleared_month",Global.CURRENT_MONTH);
            Global.print(application,"cleared cache");
        }
    }

    public void cancel(boolean mayInterruptRunning){
        if(future1!=null) future1.cancel(mayInterruptRunning);
        if(future2!=null) future2.cancel(mayInterruptRunning);
        if(future3!=null) future3.cancel(mayInterruptRunning);
        if(future4!=null) future4.cancel(mayInterruptRunning);
        if(future5!=null) future5.cancel(mayInterruptRunning);
        if(future6!=null) future6.cancel(mayInterruptRunning);
        if(future7!=null) future7.cancel(mayInterruptRunning);
        if(future8!=null) future8.cancel(mayInterruptRunning);
        if(future9!=null) future9.cancel(mayInterruptRunning);
        isCancelled=true;
    }

    private boolean isCancelled()
    {
        return isCancelled;
    }


    public void deleteDirectory(File dir)
    {
        if(isDeletionCompleted.getValue()!=AsyncTaskStatus.NOT_YET_STARTED)return;
        isDeletionCompleted.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService=MyExecutorService.getExecutorService();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                FileUtil.deleteNativeDirectory(dir);
                isDeletionCompleted.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }

    public void setSAFCheckedBoolean()
    {
        checkedSAFPermissionPasteSetUp=true;
    }

    public void getAppList()
    {
        ExecutorService executorService=MyExecutorService.getExecutorService();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
                repositoryClass.populateAppsList(application);
            }
        });
    }

    public void getAudioPOJOList(boolean isCancelled)
    {
        ExecutorService executorService=MyExecutorService.getExecutorService();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
                repositoryClass.getAudioPOJOList(application,isCancelled);
            }
        });
    }

    public void getAlbumList(boolean isCancelled)
    {
        ExecutorService executorService=MyExecutorService.getExecutorService();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
                repositoryClass.getAlbumList(application,isCancelled);
            }
        });
    }


    public void getDownloadList(boolean isCancelled)
    {
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future2=executorService.submit(new Runnable() {
            @Override
            public void run() {
                RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
                repositoryClass.getDownLoadList(application,isCancelled);
            }
        });

    }

    public void getDocumentList(boolean isCancelled)
    {
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future3=executorService.submit(new Runnable() {
            @Override
            public void run() {
                RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
                repositoryClass.getDocumentList(application,isCancelled);
            }
        });

    }

    public void getImageList(boolean isCancelled)
    {
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future4=executorService.submit(new Runnable() {
            @Override
            public void run() {
                RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
                repositoryClass.getImageList(application,isCancelled);
            }
        });

    }

    public void getAudioList(boolean isCancelled)
    {
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future5=executorService.submit(new Runnable() {
            @Override
            public void run() {
                RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
                repositoryClass.getAudioList(application,isCancelled);
            }
        });

    }

    public void getVideoList(boolean isCancelled)
    {
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future6=executorService.submit(new Runnable() {
            @Override
            public void run() {
                RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
                repositoryClass.getVideoList(application,isCancelled);
            }
        });

    }

    public void getArchiveList(boolean isCancelled)
    {
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future7=executorService.submit(new Runnable() {
            @Override
            public void run() {
                RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
                repositoryClass.getArchiveList(application,isCancelled);
            }
        });

    }

    public void getApkList(boolean isCancelled)
    {
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future8=executorService.submit(new Runnable() {
            @Override
            public void run() {
                RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
                repositoryClass.getApkList(application,isCancelled);
            }
        });
    }

    public void getLargeFileList(boolean isCancelled)
    {
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future9=executorService.submit(new Runnable() {
            @Override
            public void run() {
                RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
                repositoryClass.getLargeFileList(application,isCancelled);
            }
        });

    }
}
