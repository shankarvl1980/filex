package svl.kadatha.filex.cloud;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.URLEncoder;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import svl.kadatha.filex.FileObjectType;

public class MediaFireAuthProvider implements CloudAuthProvider {
    private static final String AUTHORIZATION_ENDPOINT = "https://www.mediafire.com/dialog/oauth.php";
    private static final String TOKEN_ENDPOINT = "https://www.mediafire.com/api/1.5/user/get_session_token.php";
    private static final String REDIRECT_URI = "https://www.example.com/auth_success"; // Replace with your redirect URI
    private static final String CLIENT_ID = "YOUR_MEDIAFIRE_APP_ID"; // Replace with your MediaFire App ID
    private static final String CLIENT_SECRET = "YOUR_MEDIAFIRE_API_KEY"; // Replace with your MediaFire API Key
    private static final String RESPONSE_TYPE = "code";

    private final Activity activity;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private AuthCallback authCallback;
    private CloudAccountPOJO cloudAccount;

    public MediaFireAuthProvider(Activity activity) {
        this.activity = activity;
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();
    }

    @Override
    public void authenticate(AuthCallback callback) {
        this.authCallback = callback;
        String authUrl = AUTHORIZATION_ENDPOINT +
                "?client_id=" + CLIENT_ID +
                "&response_type=" + RESPONSE_TYPE +
                "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI) +
                "&scope=" + URLEncoder.encode("myfiles email");

        // Launch a WebView for user authentication
        launchWebView(authUrl);
    }

    @Override
    public void handleAuthorizationResponse(Intent intent) {

    }

    private void launchWebView(String authUrl) {
        WebView webView = new WebView(activity);
        webView.getSettings().setJavaScriptEnabled(true);
        activity.setContentView(webView);

        webView.setWebViewClient(new WebViewClient() {
            boolean authComplete = false;

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith(REDIRECT_URI)) {
                    if (!authComplete) {
                        authComplete = true;
                        handleRedirectUri(Uri.parse(url));
                    }
                    return true;
                }
                return false;
            }
        });

        webView.loadUrl(authUrl);
    }

    private void handleRedirectUri(Uri uri) {
        String code = uri.getQueryParameter("code");
        String error = uri.getQueryParameter("error");

        if (code != null) {
            // Exchange authorization code for access token
            exchangeAuthorizationCode(code);
        } else if (error != null) {
            // Handle error
            if (authCallback != null) {
                authCallback.onError(new Exception("Authentication error: " + error));
            }
        }
    }

    private void exchangeAuthorizationCode(String code) {
        String tokenUrl = TOKEN_ENDPOINT +
                "?client_id=" + CLIENT_ID +
                "&client_secret=" + CLIENT_SECRET +
                "&grant_type=authorization_code" +
                "&code=" + code +
                "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI) +
                "&response_format=json";

        Request request = new Request.Builder()
                .url(tokenUrl)
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (authCallback != null) {
                    new Handler(Looper.getMainLooper()).post(() -> authCallback.onError(e));
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    parseTokenResponse(responseBody);
                } else {
                    if (authCallback != null) {
                        new Handler(Looper.getMainLooper()).post(() ->
                                authCallback.onError(new Exception("Token request failed with code: " + response.code()))
                        );
                    }
                }
            }
        });
    }

    private void parseTokenResponse(String response) {
        try {
            MediaFireTokenResponse tokenResponse = gson.fromJson(response, MediaFireTokenResponse.class);
            if (tokenResponse != null && tokenResponse.response != null) {
                String sessionToken = tokenResponse.response.session_token;
                long timeRemaining = Long.parseLong(tokenResponse.response.time_remaining);
                String userEmail = tokenResponse.response.email;
                String userDisplayName = tokenResponse.response.display_name;
                String userId = tokenResponse.response.user_id;

                // Calculate token expiry time
                long expiryTime = System.currentTimeMillis() + (timeRemaining * 1000);

                cloudAccount = new CloudAccountPOJO(
                        FileObjectType.MEDIA_FIRE_TYPE.toString(),
                        userDisplayName,
                        userId,
                        sessionToken,
                        null, // MediaFire does not provide a refresh token
                        expiryTime,
                        null, // Scopes are not specified
                        userEmail,
                        null,
                        null
                );

                if (authCallback != null) {
                    new Handler(Looper.getMainLooper()).post(() -> authCallback.onSuccess(cloudAccount));
                }
            } else {
                if (authCallback != null) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            authCallback.onError(new Exception("Invalid token response"))
                    );
                }
            }
        } catch (Exception e) {
            if (authCallback != null) {
                new Handler(Looper.getMainLooper()).post(() -> authCallback.onError(e));
            }
        }
    }

    @Override
    public void refreshToken(AuthCallback callback) {
        // Implement token refresh logic if needed
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
        return cloudAccount != null && System.currentTimeMillis() < cloudAccount.tokenExpiryTime;
    }

    @Override
    public void logout(AuthCallback callback) {
        // Clear stored tokens and session data
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

    // Inner class to represent the token response structure
    private static class MediaFireTokenResponse {
        ResponseData response;

        static class ResponseData {
            String session_token;
            String time_remaining;
            String email;
            String display_name;
            String user_id;
        }
    }
}
