package svl.kadatha.filex.cloud;

import androidx.lifecycle.ViewModel;

import svl.kadatha.filex.cloud.CloudAccountPOJO;
import svl.kadatha.filex.cloud.CloudAuthProvider;

public class CloudAccountViewModel extends ViewModel {
    private CloudAuthProvider authProvider;

    private CloudAccountPOJO cloudAccount;

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
                // Save account to database
            }

            @Override
            public void onError(Exception e) {
                // Handle error
            }
        });
    }
    // Other methods...
}
