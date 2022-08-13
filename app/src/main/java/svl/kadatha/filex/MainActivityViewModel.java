package svl.kadatha.filex;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class MainActivityViewModel extends ViewModel {


    private boolean isCancelled;
    private Future<?> future1,future2,future3;
    public MutableLiveData<Boolean> isExtractionCompleted=new MutableLiveData<>();
    public boolean zipFileExtracted;
    public MutableLiveData<Boolean> isDeletionCompleted=new MutableLiveData<>();
    public boolean checkedSAFPermissionPasteSetUp;




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

    public synchronized void extractArchive(ZipFile zipfile)
    {
        if(Boolean.TRUE.equals(isExtractionCompleted.getValue())) return;
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
                isExtractionCompleted.postValue(true);
            }
        });
    }

    public void deleteDirectory(File dir)
    {
        ExecutorService executorService=MyExecutorService.getExecutorService();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                FileUtil.deleteNativeDirectory(dir);
                isDeletionCompleted.postValue(true);
            }
        });
    }

    public void setSAFCheckedBoolean()
    {
        checkedSAFPermissionPasteSetUp=true;
    }


}
