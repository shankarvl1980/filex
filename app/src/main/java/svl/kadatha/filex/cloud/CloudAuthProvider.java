package svl.kadatha.filex.cloud;

import android.content.Intent;

public interface CloudAuthProvider {
    void authenticate(AuthCallback callback);
    void handleAuthorizationResponse(Intent intent);

    // Option B addition
    default void onActivityResult(int requestCode, int resultCode, Intent data) {}

    void refreshToken(AuthCallback callback);
    String getAccessToken();
    boolean isAccessTokenValid();
    void logout(AuthCallback callback);

    interface AuthCallback {
        void onSuccess(CloudAccountPOJO account);
        void onError(Exception e);
    }
}
