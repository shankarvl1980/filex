package svl.kadatha.filex;

import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;

import org.apache.commons.net.ftp.FTPClient;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import timber.log.Timber;

public class NetworkAccountDetailsViewModel extends AndroidViewModel {

    private final Application application;
    private boolean isCancelled;
    private Future<?> future1;
    private Future<?> future2;
    private Future<?> future3;
    private Future<?> future4;
    private Future<?> future5;
    private Future<?> future6;
    private Future<?> future7;
    private Future<?> future8;
    private Future<?> future9;
    public final MutableLiveData<AsyncTaskStatus> asyncTaskStatus =new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> deleteAsyncTaskStatus =new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> ftpConnectAsyncTaskStatus =new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> replaceFtpAsyncTaskStatus =new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> replaceAndConnectFtpAsyncTaskStatus =new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> changeFtpDisplayAsyncTaskStatus =new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> checkDuplicateFtpDisplayAsyncTaskStatus =new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> testServiceConnectionAsyncTaskStatus =new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public List<NetworkAccountsDetailsDialog.NetworkAccountPOJO> networkAccountPOJOList;
    public IndexedLinkedHashMap<Integer, NetworkAccountsDetailsDialog.NetworkAccountPOJO> mselecteditems=new IndexedLinkedHashMap<>();

    public boolean loggedInStatus;
    private final NetworkAccountsDatabaseHelper networkAccountsDatabaseHelper;

    public boolean networkAccountPOJOAlreadyExists;
    public static NetworkAccountsDetailsDialog.NetworkAccountPOJO FTP_NETWORK_ACCOUNT_POJO;
    public static NetworkAccountsDetailsDialog.NetworkAccountPOJO SFTP_NETWORK_ACCOUNT_POJO;
    public static NetworkAccountsDetailsDialog.NetworkAccountPOJO WEBDAV_NETWORK_ACCOUNT_POJO;
    public static NetworkAccountsDetailsDialog.NetworkAccountPOJO SMB_NETWORK_ACCOUNT_POJO;
    public static String FTP_WORKING_DIR_PATH;
    public static String SFTP_WORKING_DIR_PATH;
    public boolean isNetworkConnected;
    public String type;
    public NetworkAccountsDetailsDialog.NetworkAccountPOJO networkAccountPOJO=null;

    private static final String TAG = "NetworkAccountViewModel";

    public NetworkAccountDetailsViewModel(@NonNull Application application) {
        super(application);
        this.application=application;
        networkAccountsDatabaseHelper=new NetworkAccountsDatabaseHelper(application);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        networkAccountsDatabaseHelper.close();
        cancel(true);
    }

    private void cancel(boolean mayInterruptRunning){
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

    public synchronized void fetchNetworkAccountPojoList(String type)
    {
        if(asyncTaskStatus.getValue()!=AsyncTaskStatus.NOT_YET_STARTED)return;
        asyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future1=executorService.submit(new Runnable() {
            @Override
            public void run() {
                networkAccountPOJOList=networkAccountsDatabaseHelper.getNetworkAccountPOJOList(type);
                asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }

    public synchronized void deleteNetworkAccountPojo(List<NetworkAccountsDetailsDialog.NetworkAccountPOJO> networkAccountPOJOS_for_delete)
    {
        if(deleteAsyncTaskStatus.getValue()!=AsyncTaskStatus.NOT_YET_STARTED)return;
        deleteAsyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future2=executorService.submit(new Runnable() {
            @Override
            public void run() {

                int size=networkAccountPOJOS_for_delete.size();
                for(int i=0;i<size;++i)
                {
                    NetworkAccountsDetailsDialog.NetworkAccountPOJO networkAccountPOJO=networkAccountPOJOS_for_delete.get(i);
                    int j=networkAccountsDatabaseHelper.delete(networkAccountPOJO.server,networkAccountPOJO.port,networkAccountPOJO.user_name,type);
                    if(j>0)
                    {
                        networkAccountPOJOList.remove(networkAccountPOJO);
                    }
                }
                deleteAsyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }

    public synchronized void connectNetworkAccount(NetworkAccountsDetailsDialog.NetworkAccountPOJO networkAccountPOJO)
    {
        if(ftpConnectAsyncTaskStatus.getValue()!=AsyncTaskStatus.NOT_YET_STARTED)return;
        ftpConnectAsyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future3=executorService.submit(new Runnable() {
            @Override
            public void run() {
                type= networkAccountPOJO.type;
                if(type.equals(NetworkAccountsDetailsDialog.FTP)){
                    connectFtp(networkAccountPOJO,ftpConnectAsyncTaskStatus);
                }
                else if(type.equals(NetworkAccountsDetailsDialog.SFTP)){
                    connectSftp(networkAccountPOJO,ftpConnectAsyncTaskStatus);
                }
            }
        });
    }

    public synchronized void replaceAndConnectNetworkAccount(Bundle bundle)
    {
        if(replaceAndConnectFtpAsyncTaskStatus.getValue()!=AsyncTaskStatus.NOT_YET_STARTED)return;
        replaceAndConnectFtpAsyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future5=executorService.submit(new Runnable() {
            @Override
            public void run() {
                replaceNetworkAccountPojo(bundle);
                String server=bundle.getString("server");
                int port=bundle.getInt("port");
                String user_name=bundle.getString("user_name");
                NetworkAccountsDetailsDialog.NetworkAccountPOJO networkAccountPOJO=networkAccountsDatabaseHelper.getNetworkAccountPOJO(server,port,user_name,type);
                type= networkAccountPOJO.type;
                if(type.equals(NetworkAccountsDetailsDialog.FTP)){
                    connectFtp(networkAccountPOJO,replaceAndConnectFtpAsyncTaskStatus);
                }
                else if(type.equals(NetworkAccountsDetailsDialog.SFTP)){
                    connectSftp(networkAccountPOJO,replaceAndConnectFtpAsyncTaskStatus);
                }
            }
        });
    }

    private void connectFtp(NetworkAccountsDetailsDialog.NetworkAccountPOJO networkAccountPOJO,MutableLiveData<AsyncTaskStatus> asyncTaskStatus){
        loggedInStatus=false;
        FtpClientRepository ftpClientRepository = null;
        FTPClient ftpClient = null;
        try {
            NetworkAccountsDetailsDialog.NetworkAccountPOJO networkAccountPOJOCopy=networkAccountPOJO.deepCopy();
            ftpClientRepository=FtpClientRepository.getInstance(networkAccountPOJOCopy);
            ftpClientRepository.shutdown();
            ftpClientRepository=FtpClientRepository.getInstance(networkAccountPOJO);
            FTP_NETWORK_ACCOUNT_POJO=networkAccountPOJO;
            NetworkAccountDetailsViewModel.this.networkAccountPOJO=networkAccountPOJO;

            loggedInStatus=true;
            if(loggedInStatus)
            {
                ftpClient=ftpClientRepository.getFtpClient();
                FTP_WORKING_DIR_PATH = ftpClient.printWorkingDirectory();
                if(!Global.CHECK_WHETHER_STORAGE_DIR_CONTAINS_FTP_FILE_OBJECT(FileObjectType.FTP_TYPE))
                {
                    RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
                    repositoryClass.storage_dir.add(FilePOJOUtil.MAKE_FilePOJO(FileObjectType.FTP_TYPE, FTP_WORKING_DIR_PATH));
                }
            }
        }
        catch (Exception e) {
            Global.print_background_thread(application,application.getString(R.string.server_could_not_be_connected));
        }
        finally {
            if (ftpClientRepository != null && ftpClient != null) {
                ftpClientRepository.releaseFtpClient(ftpClient);
                Timber.tag(TAG).d("FTP client released");
            }
            asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
        }
    }

    private void connectSftp(NetworkAccountsDetailsDialog.NetworkAccountPOJO networkAccountPOJO,MutableLiveData<AsyncTaskStatus> asyncTaskStatus){
        loggedInStatus=false;
        SftpChannelRepository sftpChannelRepository = null;
        ChannelSftp channelSftp= null;
        try {
            NetworkAccountsDetailsDialog.NetworkAccountPOJO networkAccountPOJOCopy=networkAccountPOJO.deepCopy();
            sftpChannelRepository=SftpChannelRepository.getInstance(networkAccountPOJOCopy);
            sftpChannelRepository.shutdown();
            sftpChannelRepository=sftpChannelRepository.getInstance(networkAccountPOJO);
            SFTP_NETWORK_ACCOUNT_POJO=networkAccountPOJO;
            NetworkAccountDetailsViewModel.this.networkAccountPOJO=networkAccountPOJO;

            loggedInStatus=true;
            if(loggedInStatus)
            {
                channelSftp=sftpChannelRepository.getSftpChannel();
                SFTP_WORKING_DIR_PATH = channelSftp.pwd();
                if(!Global.CHECK_WHETHER_STORAGE_DIR_CONTAINS_FTP_FILE_OBJECT(FileObjectType.SFTP_TYPE))
                {
                    RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
                    repositoryClass.storage_dir.add(FilePOJOUtil.MAKE_FilePOJO(FileObjectType.SFTP_TYPE, SFTP_WORKING_DIR_PATH));
                }
            }
        }
        catch (Exception e) {
            Global.print_background_thread(application,application.getString(R.string.server_could_not_be_connected));
        }
        finally {
            if (sftpChannelRepository != null && channelSftp != null) {
                sftpChannelRepository.releaseChannel(channelSftp);
                Timber.tag(TAG).d("SFTP client released");
            }
            asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
        }
    }

    private void replaceNetworkAccountPojo(Bundle bundle)
    {
        long row_number;
        String original_server=bundle.getString("original_server");
        String original_user_name=bundle.getString("original_user_name");
        int original_port=bundle.getInt("original_port");
        NetworkAccountsDetailsDialog.NetworkAccountPOJO networkAccountPOJO=bundle.getParcelable("networkAccountPOJO");
        boolean update=bundle.getBoolean("update");
        boolean replace=bundle.getBoolean("replace");
        if(original_server==null)original_server="";
        if(original_user_name==null)original_user_name="";
        if(replace)
        {
            networkAccountsDatabaseHelper.delete(networkAccountPOJO.server,networkAccountPOJO.port ,networkAccountPOJO.user_name,networkAccountPOJO.type);
        }
        if(update)
        {
            row_number=networkAccountsDatabaseHelper.update(original_server,original_port,original_user_name,networkAccountPOJO.type,networkAccountPOJO);
        }
        else
        {
            row_number=networkAccountsDatabaseHelper.insert(networkAccountPOJO);
        }

        networkAccountPOJOList=networkAccountsDatabaseHelper.getNetworkAccountPOJOList(type);
    }

    public synchronized void replaceNetworkAccountPojoList(Bundle bundle)
    {
        if(replaceFtpAsyncTaskStatus.getValue()!=AsyncTaskStatus.NOT_YET_STARTED)return;
        replaceFtpAsyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future4=executorService.submit(new Runnable() {
            @Override
            public void run() {
                replaceNetworkAccountPojo(bundle);
                replaceFtpAsyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }


    public synchronized void changeNetworkAccountPojoDisplay(String server,int port ,String user_name, String new_name, String type)
    {
        if(changeFtpDisplayAsyncTaskStatus.getValue()!=AsyncTaskStatus.NOT_YET_STARTED)return;
        changeFtpDisplayAsyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future6=executorService.submit(new Runnable() {
            @Override
            public void run() {
                networkAccountsDatabaseHelper.change_display(server,port,user_name,new_name,type);
                networkAccountPOJOList=networkAccountsDatabaseHelper.getNetworkAccountPOJOList(type);
                changeFtpDisplayAsyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }

    public synchronized void checkWhetherNetworkAccountPojoAlreadyExists(String server,int port,String user_name)
    {
        if(checkDuplicateFtpDisplayAsyncTaskStatus.getValue()!=AsyncTaskStatus.NOT_YET_STARTED)return;
        checkDuplicateFtpDisplayAsyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future7 = executorService.submit(new Runnable() {
            @Override
            public void run() {
                networkAccountPOJOAlreadyExists = false;
                NetworkAccountsDetailsDialog.NetworkAccountPOJO networkAccountPOJO = networkAccountsDatabaseHelper.getNetworkAccountPOJO(server,port ,user_name,type);
                networkAccountPOJOAlreadyExists = networkAccountPOJO != null;
                checkDuplicateFtpDisplayAsyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }

    public synchronized void testServiceConnection(){
        if(testServiceConnectionAsyncTaskStatus.getValue()!=AsyncTaskStatus.NOT_YET_STARTED)return;
        testServiceConnectionAsyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        isNetworkConnected=false;
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future8 = executorService.submit(new Runnable() {
            @Override
            public void run() {
                if(type.equals(NetworkAccountsDetailsDialog.FTP)){
                    isNetworkConnected =FtpClientRepository.getInstance(FTP_NETWORK_ACCOUNT_POJO).testFtpServerConnection();
                }
                else if(type.equals(NetworkAccountsDetailsDialog.SFTP)){
                    isNetworkConnected =SftpChannelRepository.getInstance(SFTP_NETWORK_ACCOUNT_POJO).testSftpServerConnection();
                }
                testServiceConnectionAsyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }
}
