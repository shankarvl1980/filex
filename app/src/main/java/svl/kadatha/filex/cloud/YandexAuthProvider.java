package svl.kadatha.filex.cloud;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.webkit.CookieManager;

import svl.kadatha.filex.FileObjectType;

public class YandexAuthProvider implements CloudAuthProvider {
    private static final String TAG = "YandexAuthProvider";
    private static final String CLIENT_ID = "YOUR_YANDEX_CLIENT_ID";
    private static final String REDIRECT_URI = "yandex-" + CLIENT_ID + "://callback";
    private static final String AUTH_URL = "https://oauth.yandex.com/authorize?response_type=token&client_id=" + CLIENT_ID + "&redirect_uri=" + REDIRECT_URI;

    private final Activity activity;
    private AuthCallback authCallback;
    private CloudAccountPOJO cloudAccount;

    public YandexAuthProvider(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void authenticate(AuthCallback callback) {
        this.authCallback = callback;
        // Launch Chrome Custom Tab or a WebView with AUTH_URL
        // Example using Custom Tab:
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(AUTH_URL));
        activity.startActivity(browserIntent);
    }

    @Override
    public void handleAuthorizationResponse(Intent intent) {
        // For Yandex, handle in onNewIntent or onResume of a redirect activity
        // This method may remain empty if we handle parsing in another activity.
        Uri uri = intent.getData();
        if (uri != null && uri.toString().startsWith(YandexAuthProvider.REDIRECT_URI)) {
            handleAuthResult(uri);
        }
    }

    // Call this in your redirect handling Activity onNewIntent or onResume
    public void handleAuthResult(Uri uri) {
        if (uri != null && uri.toString().startsWith(REDIRECT_URI)) {
            String fragment = uri.getFragment();
            // fragment looks like: "access_token=<token>&token_type=bearer&expires_in=..."
            if (fragment != null && fragment.contains("access_token=")) {
                String accessToken = extractParameter(fragment, "access_token");
                if (accessToken != null) {
                    String accountId = "YandexUser";

                    // According to the constructor:
                    // type, displayName, userId, accessToken, refreshToken, tokenExpiryTime, scopes, extra1, extra2, extra3
                    cloudAccount = new CloudAccountPOJO(
                            FileObjectType.YANDEX_TYPE.toString(),
                            "Yandex User",
                            accountId,
                            accessToken,
                            null, // refreshToken not available here
                            System.currentTimeMillis() + (3600 * 1000), // tokenExpiryTime
                            null, // scopes
                            null, // extra1
                            null, // extra2
                            null  // extra3
                    );

                    if (authCallback != null) {
                        authCallback.onSuccess(cloudAccount);
                    }
                    return;
                }
            }

            if (authCallback != null) {
                authCallback.onError(new Exception("Failed to obtain access token from Yandex"));
            }
        }
    }


    private String extractParameter(String fragment, String paramName) {
        String[] params = fragment.split("&");
        for (String param : params) {
            if (param.startsWith(paramName + "=")) {
                return param.substring((paramName + "=").length());
            }
        }
        return null;
    }

    @Override
    public void refreshToken(AuthCallback callback) {
        // Yandex OAuth tokens might be long-lived or require a refresh token grant
        // If refresh tokens are available, implement the refresh logic here.
        // If not, just return success with current account if still valid.
        if (callback != null) {
            if (cloudAccount != null) {
                callback.onSuccess(cloudAccount);
            } else {
                callback.onError(new Exception("No account to refresh"));
            }
        }
    }

    @Override
    public String getAccessToken() {
        return cloudAccount != null ? cloudAccount.accessToken : null;
    }

    @Override
    public boolean isAccessTokenValid() {
        if (cloudAccount == null || cloudAccount.accessToken == null) {
            return false;
        }
        // If you know the expiration time, check it. Otherwise assume valid:
        long currentTime = System.currentTimeMillis();
        return cloudAccount.tokenExpiryTime > currentTime;
    }

    @Override
    public void logout(AuthCallback callback) {
        cloudAccount = null;
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
