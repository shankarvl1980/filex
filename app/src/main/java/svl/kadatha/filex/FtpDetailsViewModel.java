package svl.kadatha.filex;

import android.app.Application;
import android.util.SparseBooleanArray;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class FtpDetailsViewModel extends AndroidViewModel {

    private final Application application;
    private boolean isCancelled;
    private Future<?> future1,future2,future3,future4,future5;
    public final MutableLiveData<AsyncTaskStatus> asyncTaskStatus =new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> deleteAsyncTaskStatus =new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> ftpConnectAsyncTaskStatus =new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> removeFtpAsyncTaskStatus =new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public List<FtpDetailsDialog.FtpPOJO> ftpPOJOList;
    public SparseBooleanArray mselecteditems=new SparseBooleanArray();
    public List<FtpDetailsDialog.FtpPOJO> ftpPOJO_selected_array=new ArrayList<>();
    public boolean loggedInStatus;
    private final FtpDatabaseHelper ftpDatabaseHelper;
    public String path;

    public FtpDetailsViewModel(@NonNull Application application) {
        super(application);
        this.application=application;
        ftpDatabaseHelper=new FtpDatabaseHelper(application);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        ftpDatabaseHelper.close();
        cancel(true);
    }

    private void cancel(boolean mayInterruptRunning){
        if(future1!=null) future1.cancel(mayInterruptRunning);
        if(future2!=null) future2.cancel(mayInterruptRunning);
        if(future3!=null) future3.cancel(mayInterruptRunning);
        if(future4!=null) future4.cancel(mayInterruptRunning);
        if(future5!=null) future5.cancel(mayInterruptRunning);
        isCancelled=true;
    }

    private boolean isCancelled()
    {
        return isCancelled;
    }

    public synchronized void fetchFtpPojoList()
    {
        if(asyncTaskStatus.getValue()!=AsyncTaskStatus.NOT_YET_STARTED)return;
        asyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future1=executorService.submit(new Runnable() {
            @Override
            public void run() {
                ftpPOJOList=ftpDatabaseHelper.getFtpPOJOlist();
                asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }

    public synchronized void deleteFtpPojo(List<FtpDetailsDialog.FtpPOJO> ftpPOJOS_for_delete)
    {
        if(deleteAsyncTaskStatus.getValue()!=AsyncTaskStatus.NOT_YET_STARTED)return;
        deleteAsyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future2=executorService.submit(new Runnable() {
            @Override
            public void run() {

                int size=ftpPOJOS_for_delete.size();
                for(int i=0;i<size;++i)
                {
                    FtpDetailsDialog.FtpPOJO ftpPOJO=ftpPOJOS_for_delete.get(i);
                    int j=ftpDatabaseHelper.delete(ftpPOJO.server);
                    if(j>0)
                    {
                        ftpPOJOList.remove(ftpPOJO);
                    }

                }

                deleteAsyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }

    public synchronized void connectFtp(FtpDetailsDialog.FtpPOJO ftpPOJO)
    {
        if(ftpConnectAsyncTaskStatus.getValue()!=AsyncTaskStatus.NOT_YET_STARTED)return;
        ftpConnectAsyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future3=executorService.submit(new Runnable() {
            @Override
            public void run() {
                MainActivity.FTP_CLIENT=new FTPClient();
                loggedInStatus=false;
                try {
                    MainActivity.FTP_CLIENT.connect(ftpPOJO.server,ftpPOJO.port);
                    if(FTPReply.isPositiveCompletion(MainActivity.FTP_CLIENT.getReplyCode()))
                    {
                        loggedInStatus=MainActivity.FTP_CLIENT.login(ftpPOJO.user_name,ftpPOJO.password);
                        if(loggedInStatus)
                        {
                            MainActivity.FTP_CLIENT.setFileType(FTP.BINARY_FILE_TYPE);
                            //if(ftpPOJO.mode.equals("passive"))
                            {
                                MainActivity.FTP_CLIENT.enterLocalPassiveMode();
                            }
                            path=MainActivity.FTP_CLIENT.printWorkingDirectory();

                            Iterator<FilePOJO> iterator=Global.STORAGE_DIR.iterator();
                            while(iterator.hasNext())
                            {
                                if(iterator.next().getFileObjectType()==FileObjectType.FTP_TYPE)
                                {
                                    iterator.remove();
                                }
                            }
                            Global.STORAGE_DIR.add(FilePOJOUtil.MAKE_FilePOJO(FileObjectType.FTP_TYPE,path));

                            Iterator<FilePOJO> iterator1=MainActivity.RECENTS.iterator();
                            while (iterator1.hasNext())
                            {
                                if(iterator1.next().getFileObjectType()==FileObjectType.FTP_TYPE)
                                {
                                    iterator1.remove();
                                }
                            }

                            FilePOJOUtil.REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL(Collections.singletonList(""),FileObjectType.FTP_TYPE);
                        }
                        else
                        {
                            Global.print_background_thread(application,application.getString(R.string.server_could_not_be_connected));
                        }
                    }
                }
                catch (IOException e) {
                    Global.print_background_thread(application,application.getString(R.string.server_could_not_be_connected));
                }
                finally {
                    ftpConnectAsyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
                }

            }
        });
    }

    public synchronized void removeFtpPojoList(String server, String original_server)
    {
        if(removeFtpAsyncTaskStatus.getValue()!=AsyncTaskStatus.NOT_YET_STARTED)return;
        removeFtpAsyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        if(original_server==null)original_server="";
        ExecutorService executorService=MyExecutorService.getExecutorService();
        String finalOriginal_server = original_server;
        future4=executorService.submit(new Runnable() {
            @Override
            public void run() {
                FtpDetailsDialog.FtpPOJO ftpPOJO=ftpDatabaseHelper.getFtpPOJO(server);
                Iterator<FtpDetailsDialog.FtpPOJO> iterator=ftpPOJOList.iterator();
                while(iterator.hasNext())
                {
                    String srvr=iterator.next().server;
                    if(srvr.equals(server) || srvr.equals(finalOriginal_server))
                    {
                        iterator.remove();
                        break;
                    }
                }
                ftpPOJOList.add(ftpPOJO);
                removeFtpAsyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }
}
