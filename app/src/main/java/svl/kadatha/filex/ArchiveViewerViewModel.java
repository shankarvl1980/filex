package svl.kadatha.filex;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class ArchiveViewerViewModel extends AndroidViewModel {

    private final Application application;
    private boolean isCancelled;
    private Future<?> future1,future2,future3,future4,future5,future6,future7,future8,future9;
    public final MutableLiveData<AsyncTaskStatus> isExtractionCompleted=new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public boolean zipFileExtracted;
    public final MutableLiveData<AsyncTaskStatus> isDeletionCompleted=new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public boolean checkedSAFPermissionPasteSetUp;

    public ArchiveViewerViewModel(@NonNull Application application) {
        super(application);
        this.application=application;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cancel(true);
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

    public synchronized void extractArchive(ZipFile zipfile)
    {
        if(isExtractionCompleted.getValue()!=AsyncTaskStatus.NOT_YET_STARTED)return;
        isExtractionCompleted.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future1=executorService.submit(new Runnable() {
            @Override
            public void run() {
                zipFileExtracted=false;
                if(Global.ARCHIVE_EXTRACT_DIR.exists())
                {
                    FileUtil.deleteNativeDirectory(Global.ARCHIVE_EXTRACT_DIR);
                }

                Enumeration<? extends ZipEntry> zip_entries=zipfile.entries();
                while(zip_entries.hasMoreElements())
                {
                    ZipEntry zipentry=zip_entries.nextElement();
                    File f=new File(Global.ARCHIVE_EXTRACT_DIR,zipentry.getName());
                    if(zipentry.isDirectory() && !f.exists())
                    {
                        zipFileExtracted=f.mkdirs();
                    }
                    else if(!zipentry.isDirectory())
                    {
                        if(!f.getParentFile().exists())
                        {
                            zipFileExtracted=f.getParentFile().mkdirs();
                        }
                        try
                        {
                            zipFileExtracted=f.createNewFile();
                        }
                        catch(IOException e)
                        {
                            zipFileExtracted=false;
                        }
                    }
                }
                isExtractionCompleted.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
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

}
