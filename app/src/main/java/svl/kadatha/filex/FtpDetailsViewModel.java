package svl.kadatha.filex;

import android.app.Application;
import android.os.Bundle;
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
    private Future<?> future1,future2,future3,future4,future5,future6;
    public final MutableLiveData<AsyncTaskStatus> asyncTaskStatus =new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> deleteAsyncTaskStatus =new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> ftpConnectAsyncTaskStatus =new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> replaceFtpAsyncTaskStatus =new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> replaceAndConnectFtpAsyncTaskStatus =new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> changeFtpDisplayAsyncTaskStatus =new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> checkDuplicateFtpDisplayAsyncTaskStatus =new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public List<FtpDetailsDialog.FtpPOJO> ftpPOJOList;
    public SparseBooleanArray mselecteditems=new SparseBooleanArray();
    public List<FtpDetailsDialog.FtpPOJO> ftpPOJO_selected_array=new ArrayList<>();
    public boolean loggedInStatus;
    private final FtpDatabaseHelper ftpDatabaseHelper;
    public String path;
    public boolean ftpPOJOAlreadyExists;

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
                    int j=ftpDatabaseHelper.delete(ftpPOJO.server,ftpPOJO.user_name);
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
                    connect(ftpPOJO);
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

    private void connect(FtpDetailsDialog.FtpPOJO ftpPOJO) throws IOException {
        MainActivity.FTP_CLIENT.connect(ftpPOJO.server,ftpPOJO.port);
        if(FTPReply.isPositiveCompletion(MainActivity.FTP_CLIENT.getReplyCode()))
        {

            loggedInStatus=MainActivity.FTP_CLIENT.login(ftpPOJO.user_name,ftpPOJO.password);
            if(loggedInStatus)
            {
                //if(ftpPOJO.mode.equals("passive"))
                {
                    MainActivity.FTP_CLIENT.enterLocalPassiveMode();
                }
                MainActivity.FTP_CLIENT.setFileType(FTP.BINARY_FILE_TYPE);

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

    private void replaceFtpPojo(Bundle bundle)
    {
        long row_number;
        String original_server=bundle.getString("original_server");
        String original_user_name=bundle.getString("original_user_name");
        String server=bundle.getString("server");
        int port=bundle.getInt("port");
        String mode=bundle.getString("mode");
        String user_name=bundle.getString("user_name");
        String password=bundle.getString("password");
        boolean anonymous=bundle.getBoolean("anonymous");
        String encoding=bundle.getString("encoding");
        String display=bundle.getString("display");
        boolean update=bundle.getBoolean("update");
        if(original_server==null)original_server="";
        if(original_user_name==null)original_user_name="";
        if(update)
        {
            row_number=ftpDatabaseHelper.update(original_server,original_user_name,server,port,mode,user_name,password, anonymous,encoding,display);
        }
        else
        {
            row_number=ftpDatabaseHelper.insert(server,port,mode,user_name,password, anonymous,encoding,display);
        }

        ftpPOJOList=ftpDatabaseHelper.getFtpPOJOlist();
    }

    public synchronized void replaceFtpPojoList(Bundle bundle)
    {
        if(replaceFtpAsyncTaskStatus.getValue()!=AsyncTaskStatus.NOT_YET_STARTED)return;
        replaceFtpAsyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future4=executorService.submit(new Runnable() {
            @Override
            public void run() {
                replaceFtpPojo(bundle);
                replaceFtpAsyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }

    public synchronized void replaceAndConnectFtpPojoList(Bundle bundle)
    {
        if(replaceAndConnectFtpAsyncTaskStatus.getValue()!=AsyncTaskStatus.NOT_YET_STARTED)return;
        replaceAndConnectFtpAsyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future4=executorService.submit(new Runnable() {
            @Override
            public void run() {
                replaceFtpPojo(bundle);
                String server=bundle.getString("server");
                String user_name=bundle.getString("user_name");
                FtpDetailsDialog.FtpPOJO ftpPOJO=ftpDatabaseHelper.getFtpPOJO(server,user_name);
                MainActivity.FTP_CLIENT=new FTPClient();
                loggedInStatus=false;
                try {
                    connect(ftpPOJO);
                }
                catch (IOException e) {
                    Global.print_background_thread(application,application.getString(R.string.server_could_not_be_connected));
                }
                finally {
                    replaceAndConnectFtpAsyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
                }
            }
        });
    }

    public synchronized void changeFtpPojoDisplay(String server,String user_name, String new_name)
    {
        if(changeFtpDisplayAsyncTaskStatus.getValue()!=AsyncTaskStatus.NOT_YET_STARTED)return;
        changeFtpDisplayAsyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future5=executorService.submit(new Runnable() {
            @Override
            public void run() {
                ftpDatabaseHelper.change_display(server,user_name,new_name);
                ftpPOJOList=ftpDatabaseHelper.getFtpPOJOlist();
                changeFtpDisplayAsyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }

    public synchronized void checkWhetherFtpPojoAlreadyExists(String server,String user_name)
    {
        if(checkDuplicateFtpDisplayAsyncTaskStatus.getValue()!=AsyncTaskStatus.NOT_YET_STARTED)return;
        checkDuplicateFtpDisplayAsyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future6=executorService.submit(new Runnable() {
            @Override
            public void run() {
                ftpPOJOAlreadyExists=false;
                FtpDetailsDialog.FtpPOJO ftpPOJO=ftpDatabaseHelper.getFtpPOJO(server,user_name);
                ftpPOJOAlreadyExists= ftpPOJO != null;
                checkDuplicateFtpDisplayAsyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }
}
