package svl.kadatha.filex.cloud;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.webkit.CookieManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import svl.kadatha.filex.FileObjectType;

public final class MediaFireAuthProvider implements CloudAuthProvider {

    private static final String AUTH_ENDPOINT =
            "https://www.mediafire.com/dialog/oauth.php";

    // Code -> session token (MediaFire-specific; not standard OAuth token JSON)
    private static final String GET_SESSION_TOKEN_ENDPOINT =
            "https://www.mediafire.com/api/1.5/user/get_session_token.php";

    // “Refresh” equivalent: renew session token (if supported by your MediaFire app setup)
    // If this endpoint doesn't work for you, set supportsRefresh() to false.
    private static final String RENEW_SESSION_TOKEN_ENDPOINT =
            "https://www.mediafire.com/api/user/renew_session_token.php";

    // TODO: fill these with your real values
    private static final String CLIENT_ID = "svl.kadatha.filex-1980";
    private static final String CLIENT_SECRET = "YOUR_MEDIAFIRE_API_KEY";

    // Custom scheme redirect (recommended). Must be added in manifest intent-filter.
    // Example: mf-<client_id>://callback
    private static final String REDIRECT_URI = "mf-" + CLIENT_ID + "://callback";

    private static final String SCOPE = "myfiles email";

    private final Activity activity;
    private final OkHttpClient http;
    private final Gson gson;

    private AuthCallback authCallback;

    public MediaFireAuthProvider(@NonNull Activity activity) {
        this.activity = activity;
        this.http = new OkHttpClient();
        this.gson = new Gson();
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static long safeSecondsToMs(@Nullable String secondsStr) {
        try {
            long sec = Long.parseLong(secondsStr);
            // Keep a minimum to avoid “already expired” from weird server values
            return Math.max(60, sec) * 1000L;
        } catch (Exception e) {
            return 3600_000L;
        }
    }

    @Override
    public void authenticate(AuthCallback callback) {
        this.authCallback = callback;

        String authUrl = AUTH_ENDPOINT
                + "?client_id=" + enc(CLIENT_ID)
                + "&response_type=code"
                + "&redirect_uri=" + enc(REDIRECT_URI)
                + "&scope=" + enc(SCOPE);

        // Browser/CustomTabs-style launch (no WebView UI hijack)
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
        activity.startActivity(i);
    }

    @Override
    public void handleAuthorizationResponse(Intent intent) {
        Uri uri = intent != null ? intent.getData() : null;
        if (uri == null) return;

        // Validate redirect
        String expectedPrefix = Uri.parse(REDIRECT_URI).getScheme() + "://";
        if (uri.getScheme() == null || !(REDIRECT_URI.startsWith(uri.getScheme() + "://"))) return;

        String code = uri.getQueryParameter("code");
        String error = uri.getQueryParameter("error");

        if (!TextUtils.isEmpty(error)) {
            postAuthError(new Exception("MediaFire auth error: " + error));
            return;
        }
        if (TextUtils.isEmpty(code)) {
            postAuthError(new Exception("MediaFire auth: missing code"));
            return;
        }

        exchangeAuthorizationCode(code);
    }

    private void exchangeAuthorizationCode(@NonNull String code) {
        // MediaFire returns session_token + time_remaining in JSON
        String url = GET_SESSION_TOKEN_ENDPOINT
                + "?client_id=" + enc(CLIENT_ID)
                + "&client_secret=" + enc(CLIENT_SECRET)
                + "&grant_type=authorization_code"
                + "&code=" + enc(code)
                + "&redirect_uri=" + enc(REDIRECT_URI)
                + "&response_format=json";

        Request req = new Request.Builder().url(url).get().build();
        http.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                postAuthError(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    postAuthError(new Exception("MediaFire token exchange failed: " + response.code()));
                    return;
                }

                String body = response.body() != null ? response.body().string() : "";
                MediaFireTokenResponse tr = gson.fromJson(body, MediaFireTokenResponse.class);

                if (tr == null || tr.response == null || TextUtils.isEmpty(tr.response.session_token)) {
                    postAuthError(new Exception("MediaFire: invalid token response"));
                    return;
                }

                String sessionToken = tr.response.session_token;
                long expiry = System.currentTimeMillis() + safeSecondsToMs(tr.response.time_remaining);

                String display = !TextUtils.isEmpty(tr.response.display_name)
                        ? tr.response.display_name
                        : "MediaFire User";

                String userId = !TextUtils.isEmpty(tr.response.user_id)
                        ? tr.response.user_id
                        : "mediafire";

                CloudAccountPOJO account = new CloudAccountPOJO(
                        FileObjectType.MEDIA_FIRE_TYPE.toString(),
                        display,
                        userId,
                        sessionToken,                 // stored in accessToken slot
                        null,                         // no OAuth refresh token
                        expiry,
                        SCOPE,
                        tr.response.email,            // extra1 = email
                        null,
                        null
                );

                postAuthSuccess(account);
            }
        });
    }

    /**
     * Refresh path = renew session token (MediaFire-specific).
     * If this endpoint fails consistently, return supportsRefresh() = false and force OAuth.
     */
    @Override
    public void refreshToken(CloudAccountPOJO account, AuthCallback callback) {
        if (account == null || TextUtils.isEmpty(account.accessToken)) {
            if (callback != null) callback.onError(new Exception("No session token to renew"));
            return;
        }

        String url = RENEW_SESSION_TOKEN_ENDPOINT
                + "?session_token=" + enc(account.accessToken)
                + "&response_format=json";

        Request req = new Request.Builder().url(url).get().build();
        http.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                postToCallbackError(callback, e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    postToCallbackError(callback, new Exception("MediaFire renew failed: " + response.code()));
                    return;
                }

                String body = response.body() != null ? response.body().string() : "";
                MediaFireTokenResponse tr = gson.fromJson(body, MediaFireTokenResponse.class);

                if (tr == null || tr.response == null || TextUtils.isEmpty(tr.response.session_token)) {
                    postToCallbackError(callback, new Exception("MediaFire: invalid renew response"));
                    return;
                }

                account.accessToken = tr.response.session_token;
                account.tokenExpiryTime = System.currentTimeMillis() + safeSecondsToMs(tr.response.time_remaining);

                postToCallbackSuccess(callback, account);
            }
        });
    }

    @Override
    public boolean supportsRefresh() {
        // Only true if renew endpoint works for your MediaFire integration.
        return true;
    }

    @Override
    public void logout(AuthCallback callback) {
        // You said “local disconnect”: DB delete + cache clear is enough.
        // Cookie clear is optional.
        clearCookies();
        if (callback != null) callback.onSuccess(null);
    }

    private void clearCookies() {
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();
    }

    private void postAuthSuccess(CloudAccountPOJO account) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (authCallback != null) authCallback.onSuccess(account);
        });
    }

    private void postAuthError(Exception e) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (authCallback != null) authCallback.onError(e);
        });
    }

    private void postToCallbackSuccess(AuthCallback cb, CloudAccountPOJO account) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (cb != null) cb.onSuccess(account);
        });
    }

    private void postToCallbackError(AuthCallback cb, Exception e) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (cb != null) cb.onError(e);
        });
    }

    private static final class MediaFireTokenResponse {
        ResponseData response;

        static final class ResponseData {
            @SerializedName("session_token")
            String session_token;
            @SerializedName("time_remaining")
            String time_remaining;
            @SerializedName("email")
            String email;
            @SerializedName("display_name")
            String display_name;
            @SerializedName("user_id")
            String user_id;
        }
    }
}
