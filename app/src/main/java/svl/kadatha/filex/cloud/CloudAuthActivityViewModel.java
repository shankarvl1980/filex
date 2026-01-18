package svl.kadatha.filex.cloud;

import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import svl.kadatha.filex.App;
import svl.kadatha.filex.AsyncTaskStatus;
import svl.kadatha.filex.FileObjectType;
import svl.kadatha.filex.FilePOJOUtil;
import svl.kadatha.filex.Global;
import svl.kadatha.filex.IndexedLinkedHashMap;
import svl.kadatha.filex.MakeFilePOJOUtil;
import svl.kadatha.filex.MyExecutorService;
import svl.kadatha.filex.R;
import svl.kadatha.filex.RepositoryClass;
import svl.kadatha.filex.network.FtpClientRepository;
import svl.kadatha.filex.network.NetworkAccountDetailsViewModel;
import svl.kadatha.filex.network.NetworkAccountsDetailsDialog;
import svl.kadatha.filex.network.SftpChannelRepository;
import svl.kadatha.filex.network.SmbClientRepository;
import svl.kadatha.filex.network.WebDavClientRepository;

public class CloudAuthActivityViewModel extends AndroidViewModel {
    public static String GOOGLE_DRIVE_ACCESS_TOKEN;
    public static String DROP_BOX_ACCESS_TOKEN;
    public static String MEDIA_FIRE_ACCESS_TOKEN;
    public static String YANDEX_ACCESS_TOKEN;
    private final CloudAccountsDatabaseHelper cloudAccountsDatabaseHelper;
    public CloudAuthProvider authProvider;
    public FileObjectType fileObjectType;
    public List<CloudAccountPOJO> cloudAccountPOJOList;
    public IndexedLinkedHashMap<Integer, CloudAccountPOJO> mselecteditems = new IndexedLinkedHashMap<>();
    public final MutableLiveData<AsyncTaskStatus> asyncTaskStatus = new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> deleteAsyncTaskStatus = new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public MutableLiveData<AsyncTaskStatus> cloudAccountConnectionAsyncTaskStatus = new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public MutableLiveData<AsyncTaskStatus> cloudAccountStorageDirFillAsyncTaskStatus = new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> disconnectCloudConnectionAsyncTaskStatus = new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    private CloudAccountPOJO cloudAccount;
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
    public boolean connected;

    public CloudAuthActivityViewModel(@NonNull Application application) {
        super(application);
        cloudAccountsDatabaseHelper = new CloudAccountsDatabaseHelper(application);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cloudAccountsDatabaseHelper.close();
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
    public CloudAccountPOJO getCloudAccount() {
        return cloudAccount;
    }

    public void setCloudAccount(CloudAccountPOJO account) {
        this.cloudAccount = account;
    }

    public void setAuthProvider(CloudAuthProvider provider) {
        this.authProvider = provider;
    }

    public synchronized void fetchCloudAccountPojoList(FileObjectType fileObjectType) {
        if (asyncTaskStatus.getValue() != AsyncTaskStatus.NOT_YET_STARTED) {
            return;
        }
        asyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        this.fileObjectType = fileObjectType;
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future1 = executorService.submit(new Runnable() {
            @Override
            public void run() {
                cloudAccountPOJOList = cloudAccountsDatabaseHelper.getCloudAccountList(fileObjectType.toString());
                asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }

    public synchronized void deleteCloudAccountPojo(List<CloudAccountPOJO> cloudAccountPOJOS_for_delete) {
        if (deleteAsyncTaskStatus.getValue() != AsyncTaskStatus.NOT_YET_STARTED) {
            return;
        }
        deleteAsyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future2 = executorService.submit(new Runnable() {
            @Override
            public void run() {
                int size = cloudAccountPOJOS_for_delete.size();
                for (int i = 0; i < size; ++i) {
                    CloudAccountPOJO cloudAccountPOJO = cloudAccountPOJOS_for_delete.get(i);
                    int j = cloudAccountsDatabaseHelper.delete(cloudAccountPOJO.type, cloudAccountPOJO.userId);
                    if (j > 0) {
                        cloudAccountPOJOS_for_delete.remove(cloudAccountPOJO);
                    }
                }
                deleteAsyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }

    public synchronized void authenticate() {
        if (cloudAccountConnectionAsyncTaskStatus.getValue() != AsyncTaskStatus.NOT_YET_STARTED) {
            return;
        }
        cloudAccountConnectionAsyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        connected=false;
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future3=executorService.submit(new Runnable() {
            @Override
            public void run() {
                authProvider.authenticate(new CloudAuthProvider.AuthCallback() {
                    @Override
                    public void onSuccess(CloudAccountPOJO account) {
                        long l = cloudAccountsDatabaseHelper.updateOrInsert(account.type, account.userId, account);
                        if (l != -1) {
                            cloudAccount = account;
                            switch (fileObjectType) {
                                case GOOGLE_DRIVE_TYPE:
                                    GOOGLE_DRIVE_ACCESS_TOKEN = account.accessToken;
                                    break;
                                case ONE_DRIVE_TYPE:
                                    break;
                                case DROP_BOX_TYPE:
                                    DROP_BOX_ACCESS_TOKEN = account.accessToken;
                                    break;
                                case MEDIA_FIRE_TYPE:
                                    MEDIA_FIRE_ACCESS_TOKEN = account.accessToken;
                                    break;
                                case BOX_TYPE:
                                    break;
                                case NEXT_CLOUD_TYPE:
                                    break;
                                case YANDEX_TYPE:
                                    YANDEX_ACCESS_TOKEN = account.accessToken;
                                    break;
                            }
                            onCloudConnection();
                            connected=true;
                            cloudAccountConnectionAsyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        connected=false;
                        cloudAccountConnectionAsyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
                    }
                });
            }
        });
    }

    public synchronized void populateStorageDir(FileObjectType fileObjectType, CloudAccountPOJO cloudAccountPOJO) {
        if (cloudAccountStorageDirFillAsyncTaskStatus.getValue() != AsyncTaskStatus.NOT_YET_STARTED) {
            return;
        }
        cloudAccountStorageDirFillAsyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        connected=false;
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future4=executorService.submit(new Runnable() {
            @Override
            public void run() {
                String type = cloudAccountPOJO.type;
                if (type.equals(FileObjectType.GOOGLE_DRIVE_TYPE.toString())) {
                    CloudAuthActivityViewModel.GOOGLE_DRIVE_ACCESS_TOKEN = cloudAccountPOJO.accessToken;
                } else if (type.equals(FileObjectType.DROP_BOX_TYPE.toString())) {
                    CloudAuthActivityViewModel.DROP_BOX_ACCESS_TOKEN = cloudAccountPOJO.accessToken;
                } else if (type.equals(FileObjectType.MEDIA_FIRE_TYPE.toString())) {
                    CloudAuthActivityViewModel.MEDIA_FIRE_ACCESS_TOKEN = cloudAccountPOJO.accessToken;
                } else if (type.equals(FileObjectType.YANDEX_TYPE.toString())) {
                    CloudAuthActivityViewModel.YANDEX_ACCESS_TOKEN = cloudAccountPOJO.accessToken;
                }

                onCloudConnection();
                connected=true;
                cloudAccountStorageDirFillAsyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }

    public synchronized void disconnectCloudConnection() {
        if (disconnectCloudConnectionAsyncTaskStatus.getValue() != AsyncTaskStatus.NOT_YET_STARTED) {
            return;
        }
        disconnectCloudConnectionAsyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future9 = executorService.submit(new Runnable() {
            @Override
            public void run() {
                authProvider.logout(new CloudAuthProvider.AuthCallback() {
                    @Override
                    public void onSuccess(CloudAccountPOJO account) {
                        NetworkAccountDetailsViewModel.clearNetworkFileObjectType(fileObjectType);
                    }

                    @Override
                    public void onError(Exception e) {
                            Global.print_background_thread(App.getAppContext(),App.getAppContext().getString(R.string.error_while_disconnecting_from_cloud));
                    }
                });
                disconnectCloudConnectionAsyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }

    private void onCloudConnection() {
        if (!Global.CHECK_WHETHER_STORAGE_DIR_CONTAINS_FILE_OBJECT(fileObjectType)) {
            RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
            repositoryClass.storage_dir.add(MakeFilePOJOUtil.MAKE_FilePOJO(fileObjectType, "/"));
        }
        Bundle bundle = new Bundle();
        bundle.putSerializable("fileObjectType", fileObjectType);
        Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_REFRESH_STORAGE_DIR_ACTION, LocalBroadcastManager.getInstance(App.getAppContext()), null);
        Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_POP_UP_NETWORK_FILE_TYPE_FRAGMENT, LocalBroadcastManager.getInstance(App.getAppContext()), bundle);
        FilePOJOUtil.REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL(Collections.singletonList(""), fileObjectType);
        Global.DELETE_DIRECTORY_ASYNCHRONOUSLY(Global.CLOUD_CACHE_DIR);
    }

    public void saveAccount(@NonNull CloudAccountPOJO account) {
        cloudAccountsDatabaseHelper.updateOrInsert(account.type, account.userId, account);
    }
}
