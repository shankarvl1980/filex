package svl.kadatha.filex.cloud;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;

import svl.kadatha.filex.AsyncTaskStatus;
import svl.kadatha.filex.FileObjectType;
import svl.kadatha.filex.Global;
import svl.kadatha.filex.IndexedLinkedHashMap;
import svl.kadatha.filex.MakeFilePOJOUtil;
import svl.kadatha.filex.MyExecutorService;
import svl.kadatha.filex.RepositoryClass;

public class CloudAccountViewModel extends AndroidViewModel {
    public static String GOOGLE_DRIVE_ACCESS_TOKEN;
    public static String DROP_BOX_ACCESS_TOKEN;
    public static String MEDIA_FIRE_ACCESS_TOKEN;
    public static String YANDEX_ACCESS_TOKEN;
    private final CloudAccountsDatabaseHelper cloudAccountsDatabaseHelper;
    public CloudAuthProvider authProvider;
    public FileObjectType fileObjectType;
    public List<CloudAccountPOJO> cloudAccountPOJOList;
    public IndexedLinkedHashMap<Integer, CloudAccountPOJO> mselecteditems = new IndexedLinkedHashMap<>();
    public MutableLiveData<AsyncTaskStatus> cloudAccountConnectionAsyncTaskStatus = new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public MutableLiveData<AsyncTaskStatus> cloudAccountStorageDirFillAsyncTaskStatus = new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    private CloudAccountPOJO cloudAccount;

    public CloudAccountViewModel(@NonNull Application application) {
        super(application);
        cloudAccountsDatabaseHelper = new CloudAccountsDatabaseHelper(application);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cloudAccountsDatabaseHelper.close();
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

    public void authenticate() {
        authProvider.authenticate(new CloudAuthProvider.AuthCallback() {
            @Override
            public void onSuccess(CloudAccountPOJO account) {
                long l = cloudAccountsDatabaseHelper.updateOrInsert(account.type, account.userId, account);
                // Update the list of accounts if necessary
                //cloudAccountPOJOList = cloudAccountsDatabaseHelper.getAllAccounts();
                // Notify observers if you're using LiveData
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
                    cloudAccountConnectionAsyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
                }
            }

            @Override
            public void onError(Exception e) {
                // Handle error (e.g., show a message)
            }
        });
    }

    public void populateStorageDir(FileObjectType fileObjectType, CloudAccountPOJO cloudAccountPOJO) {
        if (cloudAccountStorageDirFillAsyncTaskStatus.getValue() != AsyncTaskStatus.NOT_YET_STARTED) {
            return;
        }
        ExecutorService executorService = MyExecutorService.getExecutorService();
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                ;
                String type = cloudAccountPOJO.type;
                if (type.equals(FileObjectType.GOOGLE_DRIVE_TYPE.toString())) {
                    CloudAccountViewModel.GOOGLE_DRIVE_ACCESS_TOKEN = cloudAccountPOJO.accessToken;
                } else if (type.equals(FileObjectType.DROP_BOX_TYPE.toString())) {
                    CloudAccountViewModel.DROP_BOX_ACCESS_TOKEN = cloudAccountPOJO.accessToken;
                } else if (type.equals(FileObjectType.MEDIA_FIRE_TYPE.toString())) {
                    CloudAccountViewModel.MEDIA_FIRE_ACCESS_TOKEN = cloudAccountPOJO.accessToken;
                } else if (type.equals(FileObjectType.YANDEX_TYPE.toString())) {
                    CloudAccountViewModel.YANDEX_ACCESS_TOKEN = cloudAccountPOJO.accessToken;
                }
                if (!Global.CHECK_WHETHER_STORAGE_DIR_CONTAINS_FILE_OBJECT(fileObjectType)) {
                    RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
                    repositoryClass.storage_dir.add(MakeFilePOJOUtil.MAKE_FilePOJO(fileObjectType, "/"));
                }
                cloudAccountStorageDirFillAsyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }
}
