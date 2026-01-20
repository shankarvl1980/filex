package svl.kadatha.filex.cloud;

import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

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
import svl.kadatha.filex.network.NetworkAccountDetailsViewModel;

public class CloudAuthActivityViewModel extends AndroidViewModel {

    // tokens
    public static String GOOGLE_DRIVE_ACCESS_TOKEN;
    public static String DROP_BOX_ACCESS_TOKEN;
    public static String MEDIA_FIRE_ACCESS_TOKEN;
    public static String YANDEX_ACCESS_TOKEN;

    // active accounts
    public static CloudAccountPOJO GOOGLE_DRIVE_ACCOUNT;
    public static CloudAccountPOJO DROPBOX_ACCOUNT;
    public static CloudAccountPOJO MEDIAFIRE_ACCOUNT;
    public static CloudAccountPOJO YANDEX_ACCOUNT;
    public final MutableLiveData<AsyncTaskStatus> asyncTaskStatus =
            new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> deleteAsyncTaskStatus =
            new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> cloudAccountConnectionAsyncTaskStatus =
            new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> cloudAccountStorageDirFillAsyncTaskStatus =
            new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    // Toolbar/batch disconnect status
    public final MutableLiveData<AsyncTaskStatus> disconnectCloudConnectionAsyncTaskStatus =
            new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    // Row-click disconnect status (disconnect -> then Activity connects)
    public final MutableLiveData<AsyncTaskStatus> rowDisconnectAsyncTaskStatus =
            new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    private final CloudAccountsDatabaseHelper cloudAccountsDatabaseHelper;
    // -------------------------------------------------------------------------
    // Sequential disconnect queue (BATCH only)
    // -------------------------------------------------------------------------
    private final java.util.ArrayDeque<FileObjectType> disconnectQueue = new java.util.ArrayDeque<>();
    // Provider set by Activity for connect/auth flows (OAuth etc.)
    public CloudAuthProvider authProvider;
    public FileObjectType fileObjectType;
    public List<CloudAccountPOJO> cloudAccountPOJOList;
    public IndexedLinkedHashMap<Integer, CloudAccountPOJO> mselecteditems = new IndexedLinkedHashMap<>();
    public boolean connected;
    // pending connect (row click flow only)
    private volatile PendingConnect rowPendingConnect = null;
    private CloudAccountPOJO cloudAccount;
    // futures
    private Future<?> future1;
    private Future<?> future2;
    private Future<?> future3;
    private Future<?> future4;

    // Use separate future refs so cancelling one doesn't kill the other flow unexpectedly
    private Future<?> futureDisconnectBatch;
    private Future<?> futureDisconnectRow;
    private volatile ProviderFactory providerFactory;
    private boolean disconnectInFlight = false;

    // -------------------------------------------------------------------------
    // ctor / lifecycle
    // -------------------------------------------------------------------------
    public CloudAuthActivityViewModel(@NonNull Application application) {
        super(application);
        cloudAccountsDatabaseHelper = new CloudAccountsDatabaseHelper(application);
    }

    // -------------------------------------------------------------------------
    // Active mapping helpers
    // -------------------------------------------------------------------------
    private static boolean sameKey(CloudAccountPOJO a, CloudAccountPOJO b) {
        return a != null && b != null
                && a.type != null && b.type != null
                && a.userId != null && b.userId != null
                && a.type.equals(b.type)
                && a.userId.equals(b.userId);
    }

    public static boolean isPojoConnected(CloudAccountPOJO pojo) {
        if (pojo == null || pojo.type == null) return false;
        switch (pojo.type) {
            case "GOOGLE_DRIVE_TYPE":
                return GOOGLE_DRIVE_ACCOUNT != null && sameKey(pojo, GOOGLE_DRIVE_ACCOUNT);
            case "DROP_BOX_TYPE":
                return DROPBOX_ACCOUNT != null && sameKey(pojo, DROPBOX_ACCOUNT);
            case "MEDIA_FIRE_TYPE":
                return MEDIAFIRE_ACCOUNT != null && sameKey(pojo, MEDIAFIRE_ACCOUNT);
            case "YANDEX_TYPE":
                return YANDEX_ACCOUNT != null && sameKey(pojo, YANDEX_ACCOUNT);
            default:
                return false;
        }
    }

    public static CloudAccountPOJO getActiveForType(FileObjectType t) {
        switch (t) {
            case GOOGLE_DRIVE_TYPE:
                return GOOGLE_DRIVE_ACCOUNT;
            case DROP_BOX_TYPE:
                return DROPBOX_ACCOUNT;
            case MEDIA_FIRE_TYPE:
                return MEDIAFIRE_ACCOUNT;
            case YANDEX_TYPE:
                return YANDEX_ACCOUNT;
            default:
                return null;
        }
    }

    public static void setActive(FileObjectType t, CloudAccountPOJO a) {
        switch (t) {
            case GOOGLE_DRIVE_TYPE:
                GOOGLE_DRIVE_ACCOUNT = a;
                GOOGLE_DRIVE_ACCESS_TOKEN = a.accessToken;
                break;
            case DROP_BOX_TYPE:
                DROPBOX_ACCOUNT = a;
                DROP_BOX_ACCESS_TOKEN = a.accessToken;
                break;
            case MEDIA_FIRE_TYPE:
                MEDIAFIRE_ACCOUNT = a;
                MEDIA_FIRE_ACCESS_TOKEN = a.accessToken;
                break;
            case YANDEX_TYPE:
                YANDEX_ACCOUNT = a;
                YANDEX_ACCESS_TOKEN = a.accessToken;
                break;
        }
    }

    public static void clearActive(FileObjectType t) {
        switch (t) {
            case GOOGLE_DRIVE_TYPE:
                GOOGLE_DRIVE_ACCOUNT = null;
                GOOGLE_DRIVE_ACCESS_TOKEN = null;
                break;
            case DROP_BOX_TYPE:
                DROPBOX_ACCOUNT = null;
                DROP_BOX_ACCESS_TOKEN = null;
                break;
            case MEDIA_FIRE_TYPE:
                MEDIAFIRE_ACCOUNT = null;
                MEDIA_FIRE_ACCESS_TOKEN = null;
                break;
            case YANDEX_TYPE:
                YANDEX_ACCOUNT = null;
                YANDEX_ACCESS_TOKEN = null;
                break;
        }
    }

    public void attachProviderFactory(@NonNull ProviderFactory factory) {
        this.providerFactory = factory;
    }

    @NonNull
    private CloudAuthProvider requireProvider(FileObjectType type) {
        ProviderFactory f = providerFactory;
        if (f == null) throw new IllegalStateException("ProviderFactory not attached to ViewModel");
        CloudAuthProvider p = f.create(type);
        if (p == null)
            throw new IllegalStateException("ProviderFactory returned null provider for " + type);
        return p;
    }

    private synchronized void enqueueDisconnect(@NonNull FileObjectType type) {
        // avoid duplicates so toolbar selection doesn't enqueue same type twice
        if (!disconnectQueue.contains(type)) disconnectQueue.addLast(type);
    }

    private synchronized void startDisconnectDrainIfNeeded() {
        if (disconnectInFlight) return;
        disconnectInFlight = true;
        disconnectCloudConnectionAsyncTaskStatus.postValue(AsyncTaskStatus.STARTED);
        scheduleDrainStep();
    }

    public CloudAuthProvider getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(CloudAuthProvider provider) {
        this.authProvider = provider;
    }

    private void scheduleDrainStep() {
        ExecutorService executorService = MyExecutorService.getExecutorService();
        futureDisconnectBatch = executorService.submit(this::drainDisconnectQueueStep);
    }

    private void drainDisconnectQueueStep() {
        final FileObjectType type;

        synchronized (this) {
            type = disconnectQueue.pollFirst();
            if (type == null) {
                disconnectInFlight = false;
                disconnectCloudConnectionAsyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
                return;
            }
        }

        CloudAuthProvider provider;
        try {
            provider = requireProvider(type);
        } catch (Throwable t) {
            // can't create provider -> still clear local mapping and continue
            clearAfterDisconnect(type);
            scheduleDrainStep();
            return;
        }

        try {
            provider.logout(new CloudAuthProvider.AuthCallback() {
                @Override
                public void onSuccess(CloudAccountPOJO account) {
                    clearAfterDisconnect(type);
                    scheduleDrainStep(); // continue on executor
                }

                @Override
                public void onError(Exception e) {
                    Global.print_background_thread(App.getAppContext(),
                            App.getAppContext().getString(R.string.error_while_disconnecting_from_cloud));
                    clearAfterDisconnect(type);
                    scheduleDrainStep(); // continue on executor
                }
            });
        } catch (Throwable t) {
            clearAfterDisconnect(type);
            scheduleDrainStep();
        }
    }

    private void clearAfterDisconnect(@NonNull FileObjectType type) {
        CloudAuthActivityViewModel.clearActive(type);
        NetworkAccountDetailsViewModel.clearNetworkFileObjectType(type);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cloudAccountsDatabaseHelper.close();
        cancel(true);
    }

    private void cancel(boolean mayInterruptRunning) {
        if (future1 != null) future1.cancel(mayInterruptRunning);
        if (future2 != null) future2.cancel(mayInterruptRunning);
        if (future3 != null) future3.cancel(mayInterruptRunning);
        if (future4 != null) future4.cancel(mayInterruptRunning);
        if (futureDisconnectBatch != null) futureDisconnectBatch.cancel(mayInterruptRunning);
        if (futureDisconnectRow != null) futureDisconnectRow.cancel(mayInterruptRunning);
    }

    // -------------------------------------------------------------------------
    // basic setters/getters
    // -------------------------------------------------------------------------
    public CloudAccountPOJO getCloudAccount() {
        return cloudAccount;
    }

    public void setCloudAccount(CloudAccountPOJO account) {
        this.cloudAccount = account;
    }

    // -------------------------------------------------------------------------
    // Toolbar batch disconnect: Activity calls this ONCE
    // -------------------------------------------------------------------------
    public synchronized void disconnectSelectedConnectedAccountsFromToolbar() {
        int s = mselecteditems.size();
        if (s == 0) return;

        for (int i = 0; i < s; i++) {
            CloudAccountPOJO pojo = mselecteditems.getValueAtIndex(i);
            if (isPojoConnected(pojo)) {
                try {
                    FileObjectType t = FileObjectType.valueOf(pojo.type);
                    enqueueDisconnect(t);
                } catch (Throwable ignore) {
                }
            }
        }
        startDisconnectDrainIfNeeded();
    }

    // -------------------------------------------------------------------------
    // Row-click flow: disconnect active of that type (if any), then Activity connects
    // -------------------------------------------------------------------------
    public synchronized void disconnectThenConnectRowClick(@NonNull FileObjectType type,
                                                           @NonNull CloudAccountPOJO account,
                                                           @NonNull CloudAuthProvider provider) {
        if (rowDisconnectAsyncTaskStatus.getValue() != AsyncTaskStatus.NOT_YET_STARTED) return;

        // always set pending connect first
        rowPendingConnect = new PendingConnect(type, account);

        rowDisconnectAsyncTaskStatus.setValue(AsyncTaskStatus.STARTED);

        CloudAccountPOJO active = getActiveForType(type);
        if (active == null) {
            // no active -> proceed directly to connect via observer
            rowDisconnectAsyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            return;
        }

        ExecutorService executorService = MyExecutorService.getExecutorService();
        futureDisconnectRow = executorService.submit(() -> {
            try {
                provider.logout(new CloudAuthProvider.AuthCallback() {
                    @Override
                    public void onSuccess(CloudAccountPOJO ignored) {
                        clearAfterDisconnect(type);
                        rowDisconnectAsyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
                    }

                    @Override
                    public void onError(Exception e) {
                        Global.print_background_thread(App.getAppContext(),
                                App.getAppContext().getString(R.string.error_while_disconnecting_from_cloud));
                        clearAfterDisconnect(type);
                        rowDisconnectAsyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
                    }
                });
            } catch (Throwable t) {
                clearAfterDisconnect(type);
                rowDisconnectAsyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }

    public synchronized PendingConnect consumeRowPendingConnect() {
        PendingConnect pc = rowPendingConnect;
        rowPendingConnect = null;
        return pc;
    }

    // -------------------------------------------------------------------------
    // DB + connect/auth flows
    // -------------------------------------------------------------------------
    public synchronized void fetchCloudAccountPojoList() {
        if (asyncTaskStatus.getValue() != AsyncTaskStatus.NOT_YET_STARTED) return;

        asyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future1 = executorService.submit(() -> {
            cloudAccountPOJOList = cloudAccountsDatabaseHelper.getAllCloudAccountList();
            asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
        });
    }

    public synchronized void deleteCloudAccountPojo(List<CloudAccountPOJO> cloudAccountPOJOS_for_delete) {
        if (deleteAsyncTaskStatus.getValue() != AsyncTaskStatus.NOT_YET_STARTED) return;

        deleteAsyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future2 = executorService.submit(() -> {
            int size = cloudAccountPOJOS_for_delete.size();
            for (int i = 0; i < size; ++i) {
                CloudAccountPOJO cloudAccountPOJO = cloudAccountPOJOS_for_delete.get(i);
                int j = cloudAccountsDatabaseHelper.delete(cloudAccountPOJO.type, cloudAccountPOJO.userId);
                if (j > 0 && cloudAccountPOJOList != null) {
                    cloudAccountPOJOList.remove(cloudAccountPOJO);
                }
            }
            deleteAsyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
        });
    }

    public synchronized void authenticate() {
        if (cloudAccountConnectionAsyncTaskStatus.getValue() != AsyncTaskStatus.NOT_YET_STARTED)
            return;
        ensureType();
        if (authProvider == null)
            throw new IllegalStateException("authProvider not set before authenticate()");

        cloudAccountConnectionAsyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        connected = false;

        ExecutorService executorService = MyExecutorService.getExecutorService();
        future3 = executorService.submit(() -> {
            authProvider.authenticate(new CloudAuthProvider.AuthCallback() {
                @Override
                public void onSuccess(CloudAccountPOJO account) {
                    long l = cloudAccountsDatabaseHelper.updateOrInsert(account.type, account.userId, account);
                    if (l != -1) {
                        cloudAccount = account;
                        setActive(fileObjectType, account);
                        onCloudConnection();
                        connected = true;
                    } else {
                        connected = false;
                    }
                    cloudAccountConnectionAsyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
                }

                @Override
                public void onError(Exception e) {
                    connected = false;
                    cloudAccountConnectionAsyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
                }
            });
        });
    }

    public synchronized void populateStorageDir(FileObjectType type, CloudAccountPOJO account) {
        if (cloudAccountStorageDirFillAsyncTaskStatus.getValue() != AsyncTaskStatus.NOT_YET_STARTED)
            return;

        cloudAccountStorageDirFillAsyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        connected = false;

        ExecutorService executorService = MyExecutorService.getExecutorService();
        future4 = executorService.submit(() -> {
            this.fileObjectType = type;
            this.cloudAccount = account;

            try {
                FileObjectType t = FileObjectType.valueOf(account.type);
                setActive(t, account);
            } catch (Throwable ignore) {
            }

            onCloudConnection();
            connected = true;
            cloudAccountStorageDirFillAsyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
        });
    }

    private void onCloudConnection() {
        ensureType();

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

    private void ensureType() {
        if (fileObjectType == null) {
            throw new IllegalStateException("fileObjectType not set before cloud operation");
        }
    }

    public void saveAccount(@NonNull CloudAccountPOJO account) {
        cloudAccountsDatabaseHelper.updateOrInsert(account.type, account.userId, account);
    }

    // -------------------------------------------------------------------------
    // Provider factory (Activity supplies "new Provider(Activity)" safely)
    // -------------------------------------------------------------------------
    public interface ProviderFactory {
        CloudAuthProvider create(FileObjectType type);
    }

    public static final class PendingConnect {
        public final FileObjectType type;
        public final CloudAccountPOJO account;

        public PendingConnect(FileObjectType type, CloudAccountPOJO account) {
            this.type = type;
            this.account = account;
        }
    }
}
