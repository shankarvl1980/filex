package svl.kadatha.filex.cloud;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.ViewModel;

import java.util.List;

import svl.kadatha.filex.FileObjectType;
import svl.kadatha.filex.IndexedLinkedHashMap;
import svl.kadatha.filex.cloud.CloudAccountPOJO;
import svl.kadatha.filex.cloud.CloudAuthProvider;
import svl.kadatha.filex.network.NetworkAccountPOJO;

public class CloudAccountViewModel extends AndroidViewModel {
    public CloudAuthProvider authProvider;
    private FileObjectType fileObjectType;
    private CloudAccountPOJO cloudAccount;
    private CloudAccountsDatabaseHelper cloudAccountsDatabaseHelper;
    public List<CloudAccountPOJO> cloudAccountPOJOList;
    public IndexedLinkedHashMap<Integer, CloudAccountPOJO> mselecteditems = new IndexedLinkedHashMap<>();

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
                cloudAccountsDatabaseHelper.updateOrInsert(cloudAccount.type,cloudAccount.userId,account);
            }

            @Override
            public void onError(Exception e) {
                // Handle error
            }
        });
    }

    // Other methods...
}
