package svl.kadatha.filex.cloud;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import svl.kadatha.filex.AsyncTaskStatus;
import svl.kadatha.filex.FileObjectType;
import svl.kadatha.filex.IndexedLinkedHashMap;
import svl.kadatha.filex.cloud.CloudAccountPOJO;
import svl.kadatha.filex.cloud.CloudAuthProvider;
import svl.kadatha.filex.network.NetworkAccountPOJO;

public class CloudAccountViewModel extends AndroidViewModel {
    public CloudAuthProvider authProvider;
    private FileObjectType fileObjectType;
    private CloudAccountPOJO cloudAccount;
    private final CloudAccountsDatabaseHelper cloudAccountsDatabaseHelper;
    public List<CloudAccountPOJO> cloudAccountPOJOList;
    public IndexedLinkedHashMap<Integer, CloudAccountPOJO> mselecteditems = new IndexedLinkedHashMap<>();
    public static String GOOGLE_DRIVE_ACCESS_TOKEN;
    public static String DROP_BOX_ACCESS_TOKEN;
    public MutableLiveData<AsyncTaskStatus> cloudAccountConnectionAsyncTaskStatus=new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);

    public CloudAccountViewModel(@NonNull Application application) {
        super(application);
        cloudAccountsDatabaseHelper=new CloudAccountsDatabaseHelper(application);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cloudAccountsDatabaseHelper.close();
    }

    public void setCloudAccount(CloudAccountPOJO account) {
        this.cloudAccount = account;
    }

    public CloudAccountPOJO getCloudAccount() {
        return cloudAccount;
    }

    public void setAuthProvider(CloudAuthProvider provider) {
        this.authProvider = provider;
    }

    public void authenticate() {
        authProvider.authenticate(new CloudAuthProvider.AuthCallback() {
            @Override
            public void onSuccess(CloudAccountPOJO account) {
                long l=cloudAccountsDatabaseHelper.updateOrInsert(account.type, account.userId, account);
                // Update the list of accounts if necessary
                //cloudAccountPOJOList = cloudAccountsDatabaseHelper.getAllAccounts();
                // Notify observers if you're using LiveData
                if(l!=-1){
                    cloudAccount=account;
                }
            }

            @Override
            public void onError(Exception e) {
                // Handle error (e.g., show a message)
            }
        });
    }


    // Other methods...
}
