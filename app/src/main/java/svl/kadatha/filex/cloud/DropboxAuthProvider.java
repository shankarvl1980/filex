package svl.kadatha.filex.cloud;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import com.dropbox.core.android.Auth;

import svl.kadatha.filex.FileObjectType;

public class DropboxAuthProvider implements CloudAuthProvider {
    private final Activity activity;
    private AuthCallback authCallback;
    private CloudAccountPOJO cloudAccount;

    public DropboxAuthProvider(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void authenticate(AuthCallback callback) {
        this.authCallback = callback;
        Auth.startOAuth2Authentication(activity, "YOUR_APP_KEY");
    }

    @Override
    public void handleAuthorizationResponse(Intent intent) {

    }

    // Call this method in your Activity's onResume()
    public void handleAuthResult() {
        String accessToken = Auth.getOAuth2Token();
        if (accessToken != null) {
            String uid = Auth.getUid();
            String accountName = "Dropbox User"; // Fetch actual account info if needed

            cloudAccount = new CloudAccountPOJO(
                    FileObjectType.DROP_BOX_TYPE.toString(),
                    accountName,
                    uid,
                    accessToken,
                    null, // Dropbox doesn't provide refresh tokens in basic OAuth flow
                    Long.MAX_VALUE, // Tokens don't expire in basic flow
                    null,
                    null,
                    null,
                    null
            );

            if (authCallback != null) {
                authCallback.onSuccess(cloudAccount);
            }
        } else {
            if (authCallback != null) {
                authCallback.onError(new Exception("Authentication failed"));
            }
        }
    }

    @Override
    public void refreshToken(AuthCallback callback) {
        // Dropbox access tokens do not expire in the basic OAuth flow
        if (callback != null) {
            callback.onSuccess(cloudAccount);
        }
    }

    @Override
    public String getAccessToken() {
        return cloudAccount != null ? cloudAccount.accessToken : null;
    }

    @Override
    public boolean isAccessTokenValid() {
        return cloudAccount != null && cloudAccount.accessToken != null;
    }

    @Override
    public void logout(AuthCallback callback) {
        // Remove the stored access token and user data
        cloudAccount = null;

        // Clear WebView cookies to ensure session termination
        clearWebViewCookies();

        if (callback != null) {
            callback.onSuccess(null);
        }
    }

    private void clearWebViewCookies() {
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();
    }
}
