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

import timber.log.Timber;

public class FtpDetailsViewModel extends AndroidViewModel {

    private final Application application;
    private boolean isCancelled;
    private Future<?> future1;
    private Future<?> future2;
    private Future<?> future3;
    private Future<?> future4;
    private Future<?> future5;
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

    public boolean ftpPOJOAlreadyExists;
    public static FtpDetailsDialog.FtpPOJO FTP_POJO;
    public static String FTP_WORKING_DIR_PATH;

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
                loggedInStatus=false;
                try {
                    FTP_POJO=ftpPOJO;
                    loggedInStatus=CONNECT();
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

    public static boolean CONNECT() throws IOException {
        if(FTP_POJO==null)return false;

        MainActivity.FTP_CLIENT=new FTPClient();
        MainActivity.FTP_CLIENT.connect(FTP_POJO.server,FTP_POJO.port);
        if(FTPReply.isPositiveCompletion(MainActivity.FTP_CLIENT.getReplyCode())) {
            MainActivity.FTP_CLIENT.setControlKeepAliveTimeout(1);//Send An Keep Alive Message every second
            MainActivity.FTP_CLIENT.setControlKeepAliveReplyTimeout(5000);
            MainActivity.FTP_CLIENT.enterLocalPassiveMode();
            boolean loggedInStatus = MainActivity.FTP_CLIENT.login(FTP_POJO.user_name, FTP_POJO.password);
            if (loggedInStatus) {

                MainActivity.FTP_CLIENT.setFileType(FTP.BINARY_FILE_TYPE);

                FTP_WORKING_DIR_PATH = MainActivity.FTP_CLIENT.printWorkingDirectory();
                //Timber.tag(Global.TAG).d("ftp working dir path "+FTP_WORKING_DIR_PATH);

                //if()
                {
                    Iterator<FilePOJO> iterator = Global.STORAGE_DIR.iterator();
                    while (iterator.hasNext()) {
                        if (iterator.next().getFileObjectType() == FileObjectType.FTP_TYPE) {
                            iterator.remove();
                        }
                    }
                    Global.STORAGE_DIR.add(FilePOJOUtil.MAKE_FilePOJO(FileObjectType.FTP_TYPE, FTP_WORKING_DIR_PATH));

                    Iterator<FilePOJO> iterator1 = MainActivity.RECENTS.iterator();
                    while (iterator1.hasNext()) {
                        if (iterator1.next().getFileObjectType() == FileObjectType.FTP_TYPE) {
                            iterator1.remove();
                        }
                    }

                    FilePOJOUtil.REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL(Collections.singletonList(""), FileObjectType.FTP_TYPE);
                }

                return true;
            }
        }

        Iterator<FilePOJO> iterator=Global.STORAGE_DIR.iterator();
        while(iterator.hasNext())
        {
            if(iterator.next().getFileObjectType()==FileObjectType.FTP_TYPE)
            {
                iterator.remove();
            }
        }
        Global.STORAGE_DIR.add(FilePOJOUtil.MAKE_FilePOJO(FileObjectType.FTP_TYPE,FTP_WORKING_DIR_PATH));

        Iterator<FilePOJO> iterator1=MainActivity.RECENTS.iterator();
        while (iterator1.hasNext())
        {
            if(iterator1.next().getFileObjectType()==FileObjectType.FTP_TYPE)
            {
                iterator1.remove();
            }
        }

        FilePOJOUtil.REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL(Collections.singletonList(""),FileObjectType.FTP_TYPE);
        return false;
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
                    FTP_POJO=ftpPOJO;
                    loggedInStatus=CONNECT();
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
        Future<?> future6 = executorService.submit(new Runnable() {
            @Override
            public void run() {
                ftpPOJOAlreadyExists = false;
                FtpDetailsDialog.FtpPOJO ftpPOJO = ftpDatabaseHelper.getFtpPOJO(server, user_name);
                ftpPOJOAlreadyExists = ftpPOJO != null;
                checkDuplicateFtpDisplayAsyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }
}
