package svl.kadatha.filex;

import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.hierynomus.smbj.session.Session;
import com.jcraft.jsch.ChannelSftp;
import com.thegrizzlylabs.sardineandroid.Sardine;

import org.apache.commons.net.ftp.FTPClient;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class NetworkAccountDetailsViewModel extends AndroidViewModel {

    private static final String TAG = "NetworkAccountViewModel";
    public static NetworkAccountsDetailsDialog.NetworkAccountPOJO FTP_NETWORK_ACCOUNT_POJO;
    public static NetworkAccountsDetailsDialog.NetworkAccountPOJO SFTP_NETWORK_ACCOUNT_POJO;
    public static NetworkAccountsDetailsDialog.NetworkAccountPOJO WEBDAV_NETWORK_ACCOUNT_POJO;
    public static NetworkAccountsDetailsDialog.NetworkAccountPOJO SMB_NETWORK_ACCOUNT_POJO;
    public static String FTP_WORKING_DIR_PATH;
    public static String SFTP_WORKING_DIR_PATH;
    public static String WEBDAV_WORKING_DIR_PATH;
    public static String SMB_WORKING_DIR_PATH;
    public final MutableLiveData<AsyncTaskStatus> asyncTaskStatus = new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> deleteAsyncTaskStatus = new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> networkConnectAsyncTaskStatus = new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> replaceNetworkAccountAsyncTaskStatus = new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> replaceAndConnectNetworkAccountAsyncTaskStatus = new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> changeNetworkAccountDisplayAsyncTaskStatus = new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> checkDuplicateNetworkAccountDisplayAsyncTaskStatus = new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> testServiceConnectionAsyncTaskStatus = new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> disconnectNetworkConnectionAsyncTaskStatus = new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    private final Application application;
    private final NetworkAccountsDatabaseHelper networkAccountsDatabaseHelper;
    public List<NetworkAccountsDetailsDialog.NetworkAccountPOJO> networkAccountPOJOList;
    public IndexedLinkedHashMap<Integer, NetworkAccountsDetailsDialog.NetworkAccountPOJO> mselecteditems = new IndexedLinkedHashMap<>();
    public boolean loggedInStatus;
    public boolean networkAccountPOJOAlreadyExists;
    public boolean isNetworkConnected;
    public String type;
    public NetworkAccountsDetailsDialog.NetworkAccountPOJO networkAccountPOJO = null;
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
    private Future<?> future10;

    public NetworkAccountDetailsViewModel(@NonNull Application application) {
        super(application);
        this.application = application;
        networkAccountsDatabaseHelper = new NetworkAccountsDatabaseHelper(application);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        networkAccountsDatabaseHelper.close();
        cancel(true);
    }

    private void cancel(boolean mayInterruptRunning) {
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

    public synchronized void fetchNetworkAccountPojoList(String type) {
        if (asyncTaskStatus.getValue() != AsyncTaskStatus.NOT_YET_STARTED) {
            return;
        }
        asyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future1 = executorService.submit(new Runnable() {
            @Override
            public void run() {
                networkAccountPOJOList = networkAccountsDatabaseHelper.getNetworkAccountPOJOList(type);
                asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }

    public synchronized void deleteNetworkAccountPojo(List<NetworkAccountsDetailsDialog.NetworkAccountPOJO> networkAccountPOJOS_for_delete) {
        if (deleteAsyncTaskStatus.getValue() != AsyncTaskStatus.NOT_YET_STARTED) {
            return;
        }
        deleteAsyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future2 = executorService.submit(new Runnable() {
            @Override
            public void run() {

                int size = networkAccountPOJOS_for_delete.size();
                for (int i = 0; i < size; ++i) {
                    NetworkAccountsDetailsDialog.NetworkAccountPOJO networkAccountPOJO = networkAccountPOJOS_for_delete.get(i);
                    int j = networkAccountsDatabaseHelper.delete(networkAccountPOJO.host, networkAccountPOJO.port, networkAccountPOJO.user_name, type);
                    if (j > 0) {
                        networkAccountPOJOList.remove(networkAccountPOJO);
                    }
                }
                deleteAsyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }

    public synchronized void connectNetworkAccount(NetworkAccountsDetailsDialog.NetworkAccountPOJO networkAccountPOJO) {
        if (networkConnectAsyncTaskStatus.getValue() != AsyncTaskStatus.NOT_YET_STARTED) {
            return;
        }
        networkConnectAsyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future3 = executorService.submit(new Runnable() {
            @Override
            public void run() {
                type = networkAccountPOJO.type;
                if (type.equals(NetworkAccountsDetailsDialog.FTP)) {
                    connectFtp(networkAccountPOJO, networkConnectAsyncTaskStatus);
                } else if (type.equals(NetworkAccountsDetailsDialog.SFTP)) {
                    connectSftp(networkAccountPOJO, networkConnectAsyncTaskStatus);
                } else if (type.equals(NetworkAccountsDetailsDialog.WebDAV)) {
                    connectWebDav(networkAccountPOJO, networkConnectAsyncTaskStatus);
                } else if (type.equals(NetworkAccountsDetailsDialog.SMB)) {
                    connectSmb(networkAccountPOJO, networkConnectAsyncTaskStatus);
                }
            }
        });
    }

    public synchronized void replaceAndConnectNetworkAccount(Bundle bundle) {
        if (replaceAndConnectNetworkAccountAsyncTaskStatus.getValue() != AsyncTaskStatus.NOT_YET_STARTED) {
            return;
        }
        replaceAndConnectNetworkAccountAsyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future5 = executorService.submit(new Runnable() {
            @Override
            public void run() {
                replaceNetworkAccountPojo(bundle);
                String host = bundle.getString("host");
                int port = bundle.getInt("port");
                String user_name = bundle.getString("user_name");
                NetworkAccountsDetailsDialog.NetworkAccountPOJO networkAccountPOJO = networkAccountsDatabaseHelper.getNetworkAccountPOJO(host, port, user_name, type);
                type = networkAccountPOJO.type;
                if (type.equals(NetworkAccountsDetailsDialog.FTP)) {
                    connectFtp(networkAccountPOJO, replaceAndConnectNetworkAccountAsyncTaskStatus);
                } else if (type.equals(NetworkAccountsDetailsDialog.SFTP)) {
                    connectSftp(networkAccountPOJO, replaceAndConnectNetworkAccountAsyncTaskStatus);
                } else if (type.equals(NetworkAccountsDetailsDialog.WebDAV)) {
                    connectWebDav(networkAccountPOJO, replaceAndConnectNetworkAccountAsyncTaskStatus);
                } else if (type.equals(NetworkAccountsDetailsDialog.SMB)) {
                    connectSmb(networkAccountPOJO, networkConnectAsyncTaskStatus);
                }
            }
        });
    }

    private void connectFtp(NetworkAccountsDetailsDialog.NetworkAccountPOJO networkAccountPOJO, MutableLiveData<AsyncTaskStatus> asyncTaskStatus) {
        loggedInStatus = false;
        FtpClientRepository ftpClientRepository = null;
        FTPClient ftpClient = null;
        try {
            NetworkAccountsDetailsDialog.NetworkAccountPOJO networkAccountPOJOCopy = networkAccountPOJO.deepCopy();
            ftpClientRepository = FtpClientRepository.getInstance(networkAccountPOJOCopy);
            ftpClientRepository.shutdown();
            ftpClientRepository = FtpClientRepository.getInstance(networkAccountPOJO);
            FTP_NETWORK_ACCOUNT_POJO = networkAccountPOJO;
            NetworkAccountDetailsViewModel.this.networkAccountPOJO = networkAccountPOJO;

            ftpClient = ftpClientRepository.getFtpClient();
            if (ftpClient != null && ftpClient.isConnected()) {
                loggedInStatus = true;
            } else {
                loggedInStatus = false;
                throw new Exception("FTP client is not connected");
            }
            FTP_WORKING_DIR_PATH = ftpClient.printWorkingDirectory();
            if (!Global.CHECK_WHETHER_STORAGE_DIR_CONTAINS_FILE_OBJECT(FileObjectType.FTP_TYPE)) {
                RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
                repositoryClass.storage_dir.add(MakeFilePOJOUtil.MAKE_FilePOJO(FileObjectType.FTP_TYPE, FTP_WORKING_DIR_PATH));
            }
        } catch (Exception e) {
            loggedInStatus = false;
            Global.print_background_thread(application, application.getString(R.string.server_could_not_be_connected));
        } finally {
            if (ftpClientRepository != null && ftpClient != null) {
                ftpClientRepository.releaseFtpClient(ftpClient);
            }
            asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
        }
    }

    private void connectSftp(NetworkAccountsDetailsDialog.NetworkAccountPOJO networkAccountPOJO, MutableLiveData<AsyncTaskStatus> asyncTaskStatus) {
        loggedInStatus = false;
        SftpChannelRepository sftpChannelRepository = null;
        ChannelSftp channelSftp = null;
        try {
            NetworkAccountsDetailsDialog.NetworkAccountPOJO networkAccountPOJOCopy = networkAccountPOJO.deepCopy();
            sftpChannelRepository = SftpChannelRepository.getInstance(networkAccountPOJOCopy);
            sftpChannelRepository.shutdown();
            sftpChannelRepository = SftpChannelRepository.getInstance(networkAccountPOJO);
            SFTP_NETWORK_ACCOUNT_POJO = networkAccountPOJO;
            NetworkAccountDetailsViewModel.this.networkAccountPOJO = networkAccountPOJO;

            channelSftp = sftpChannelRepository.getSftpChannel();
            if (channelSftp != null && channelSftp.isConnected()) {
                loggedInStatus = true;
            } else {
                loggedInStatus = false;
                throw new Exception("SFTP client is not connected");
            }
            SFTP_WORKING_DIR_PATH = channelSftp.pwd();
            if (!Global.CHECK_WHETHER_STORAGE_DIR_CONTAINS_FILE_OBJECT(FileObjectType.SFTP_TYPE)) {
                RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
                repositoryClass.storage_dir.add(MakeFilePOJOUtil.MAKE_FilePOJO(FileObjectType.SFTP_TYPE, SFTP_WORKING_DIR_PATH));
            }
        } catch (Exception e) {
            loggedInStatus = false;
            Global.print_background_thread(application, application.getString(R.string.server_could_not_be_connected));
        } finally {
            if (sftpChannelRepository != null && channelSftp != null) {
                sftpChannelRepository.releaseChannel(channelSftp);
            }
            asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
        }
    }

    private void connectWebDav(NetworkAccountsDetailsDialog.NetworkAccountPOJO networkAccountPOJO, MutableLiveData<AsyncTaskStatus> asyncTaskStatus) {
        loggedInStatus = false;
        WebDavClientRepository webDavClientRepository;
        Sardine sardine;
        try {
            NetworkAccountsDetailsDialog.NetworkAccountPOJO networkAccountPOJOCopy = networkAccountPOJO.deepCopy();
            webDavClientRepository = WebDavClientRepository.getInstance(networkAccountPOJOCopy);
            webDavClientRepository.shutdown();
            webDavClientRepository = WebDavClientRepository.getInstance(networkAccountPOJO);
            WEBDAV_NETWORK_ACCOUNT_POJO = networkAccountPOJO;
            NetworkAccountDetailsViewModel.this.networkAccountPOJO = networkAccountPOJO;

            sardine = webDavClientRepository.getSardine();
            if (sardine != null) {
                loggedInStatus = true;
            } else {
                loggedInStatus = false;
                throw new Exception("WebDAV client is not connected");
            }
            WEBDAV_WORKING_DIR_PATH = webDavClientRepository.getBasePath(sardine);
            if (!Global.CHECK_WHETHER_STORAGE_DIR_CONTAINS_FILE_OBJECT(FileObjectType.WEBDAV_TYPE)) {
                RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
                repositoryClass.storage_dir.add(MakeFilePOJOUtil.MAKE_FilePOJO(FileObjectType.WEBDAV_TYPE, WEBDAV_WORKING_DIR_PATH));
            }
        } catch (Exception e) {
            loggedInStatus = false;
            Global.print_background_thread(application, application.getString(R.string.server_could_not_be_connected));
        } finally {
            asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
        }
    }

    private void connectSmb(NetworkAccountsDetailsDialog.NetworkAccountPOJO networkAccountPOJO, MutableLiveData<AsyncTaskStatus> asyncTaskStatus) {
        loggedInStatus = false;
        SmbClientRepository smbClientRepository = null;
        Session session = null;
        try {
            NetworkAccountsDetailsDialog.NetworkAccountPOJO networkAccountPOJOCopy = networkAccountPOJO.deepCopy();
            smbClientRepository = SmbClientRepository.getInstance(networkAccountPOJOCopy);
            smbClientRepository.shutdown();
            smbClientRepository = SmbClientRepository.getInstance(networkAccountPOJO);
            SMB_NETWORK_ACCOUNT_POJO = networkAccountPOJO;
            NetworkAccountDetailsViewModel.this.networkAccountPOJO = networkAccountPOJO;

            session = smbClientRepository.getSession();
            if (smbClientRepository.isSessionConnected(session)) {
                loggedInStatus = true;
                SMB_WORKING_DIR_PATH = "/";
            } else {
                loggedInStatus = false;
                throw new Exception("Smb client is not connected");
            }

            if (!Global.CHECK_WHETHER_STORAGE_DIR_CONTAINS_FILE_OBJECT(FileObjectType.SMB_TYPE)) {
                RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
                repositoryClass.storage_dir.add(MakeFilePOJOUtil.MAKE_FilePOJO(FileObjectType.SMB_TYPE, SMB_WORKING_DIR_PATH));
            }
        } catch (Exception e) {
            loggedInStatus = false;
            Global.print_background_thread(application, application.getString(R.string.server_could_not_be_connected));
        } finally {
            if (session != null) {
                smbClientRepository.releaseSession(session);
            }
            asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
        }
    }

    private void replaceNetworkAccountPojo(Bundle bundle) {
        long row_number;
        String original_host = bundle.getString("original_host");
        String original_user_name = bundle.getString("original_user_name");
        int original_port = bundle.getInt("original_port");
        NetworkAccountsDetailsDialog.NetworkAccountPOJO networkAccountPOJO = bundle.getParcelable("networkAccountPOJO");
        boolean update = bundle.getBoolean("update");
        boolean replace = bundle.getBoolean("replace");
        if (original_host == null) {
            original_host = "";
        }
        if (original_user_name == null) {
            original_user_name = "";
        }
        if (replace) {
            networkAccountsDatabaseHelper.delete(networkAccountPOJO.host, networkAccountPOJO.port, networkAccountPOJO.user_name, networkAccountPOJO.type);
        }
        if (update) {
            row_number = networkAccountsDatabaseHelper.updateOrInsert(original_host, original_port, original_user_name, networkAccountPOJO.type, networkAccountPOJO);
        } else {
            row_number = networkAccountsDatabaseHelper.insert(networkAccountPOJO);
        }

        networkAccountPOJOList = networkAccountsDatabaseHelper.getNetworkAccountPOJOList(type);
    }

    public synchronized void replaceNetworkAccountPojoList(Bundle bundle) {
        if (replaceNetworkAccountAsyncTaskStatus.getValue() != AsyncTaskStatus.NOT_YET_STARTED) {
            return;
        }
        replaceNetworkAccountAsyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future4 = executorService.submit(new Runnable() {
            @Override
            public void run() {
                replaceNetworkAccountPojo(bundle);
                replaceNetworkAccountAsyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }


    public synchronized void changeNetworkAccountPojoDisplay(String host, int port, String user_name, String new_name, String type) {
        if (changeNetworkAccountDisplayAsyncTaskStatus.getValue() != AsyncTaskStatus.NOT_YET_STARTED) {
            return;
        }
        changeNetworkAccountDisplayAsyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future6 = executorService.submit(new Runnable() {
            @Override
            public void run() {
                networkAccountsDatabaseHelper.change_display(host, port, user_name, new_name, type);
                networkAccountPOJOList = networkAccountsDatabaseHelper.getNetworkAccountPOJOList(type);
                changeNetworkAccountDisplayAsyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }

    public synchronized void checkWhetherNetworkAccountPojoAlreadyExists(String host, int port, String user_name) {
        if (checkDuplicateNetworkAccountDisplayAsyncTaskStatus.getValue() != AsyncTaskStatus.NOT_YET_STARTED) {
            return;
        }
        checkDuplicateNetworkAccountDisplayAsyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future7 = executorService.submit(new Runnable() {
            @Override
            public void run() {
                networkAccountPOJOAlreadyExists = false;
                NetworkAccountsDetailsDialog.NetworkAccountPOJO networkAccountPOJO = networkAccountsDatabaseHelper.getNetworkAccountPOJO(host, port, user_name, type);
                networkAccountPOJOAlreadyExists = networkAccountPOJO != null;
                checkDuplicateNetworkAccountDisplayAsyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }

    public synchronized void testServiceConnection() {
        if (testServiceConnectionAsyncTaskStatus.getValue() != AsyncTaskStatus.NOT_YET_STARTED) {
            return;
        }
        testServiceConnectionAsyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        isNetworkConnected = false;
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future8 = executorService.submit(new Runnable() {
            @Override
            public void run() {
                if (type.equals(NetworkAccountsDetailsDialog.FTP)) {
                    isNetworkConnected = FtpClientRepository.getInstance(FTP_NETWORK_ACCOUNT_POJO).testFtpServerConnection();
                } else if (type.equals(NetworkAccountsDetailsDialog.SFTP)) {
                    isNetworkConnected = SftpChannelRepository.getInstance(SFTP_NETWORK_ACCOUNT_POJO).testSftpServerConnection();
                } else if (type.equals(NetworkAccountsDetailsDialog.WebDAV)) {
                    try {
                        isNetworkConnected = WebDavClientRepository.getInstance(WEBDAV_NETWORK_ACCOUNT_POJO).getSardine() != null;
                    } catch (IOException e) {
                        isNetworkConnected = false;
                        throw new RuntimeException(e);
                    }
                } else if (type.equals(NetworkAccountsDetailsDialog.SMB)) {
                    isNetworkConnected = SmbClientRepository.getInstance(SMB_NETWORK_ACCOUNT_POJO).testSmbServerConnection();
                }
                testServiceConnectionAsyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }

    public synchronized void disconnectNetworkConnection() {
        if (disconnectNetworkConnectionAsyncTaskStatus.getValue() != AsyncTaskStatus.NOT_YET_STARTED) {
            return;
        }
        disconnectNetworkConnectionAsyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future9 = executorService.submit(new Runnable() {
            @Override
            public void run() {
                switch (type) {
                    case NetworkAccountsDetailsDialog.FTP:
                        FtpClientRepository ftpClientRepository = FtpClientRepository.getInstance(networkAccountPOJO);
                        ftpClientRepository.shutdown();
                        break;
                    case NetworkAccountsDetailsDialog.SFTP:
                        SftpChannelRepository sftpChannelRepository = SftpChannelRepository.getInstance(networkAccountPOJO);
                        sftpChannelRepository.shutdown();
                        break;
                    case NetworkAccountsDetailsDialog.WebDAV:
                        WebDavClientRepository webDavClientRepository;
                        try {
                            webDavClientRepository = WebDavClientRepository.getInstance(networkAccountPOJO);
                            webDavClientRepository.shutdown();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        break;
                    case NetworkAccountsDetailsDialog.SMB:
                        SmbClientRepository smbClientRepository = SmbClientRepository.getInstance(networkAccountPOJO);
                        smbClientRepository.shutdown();
                        break;
                }
                disconnectNetworkConnectionAsyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }
}
