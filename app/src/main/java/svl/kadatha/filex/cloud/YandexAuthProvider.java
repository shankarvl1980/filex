package svl.kadatha.filex.cloud;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.GrantTypeValues;
import net.openid.appauth.ResponseTypeValues;
import net.openid.appauth.TokenRequest;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import svl.kadatha.filex.FileObjectType;
import svl.kadatha.filex.MyExecutorService;

public final class YandexAuthProvider implements CloudAuthProvider {

    private static final Uri AUTH_ENDPOINT = Uri.parse("https://oauth.yandex.com/authorize");
    private static final Uri TOKEN_ENDPOINT = Uri.parse("https://oauth.yandex.com/token");

    // TODO: put your real client id here
    private static final String CLIENT_ID = "YOUR_YANDEX_CLIENT_ID";

    // Redirect should be custom scheme you register in manifest:
    // yandex-<client_id>://callback
    private static final Uri REDIRECT_URI = Uri.parse("yandex-" + CLIENT_ID + "://callback");

    // Pick minimal scopes. For Disk, typical ones are like cloud_api:disk.read / write.
    // Use only what you need.
    private static final String[] SCOPES = new String[]{
            "login:info",
            "cloud_api:disk.read"
            // "cloud_api:disk.write" // if you need uploads/changes
    };

    private static final int REQ_AUTH = 9201;

    private final Activity activity;
    private final AuthorizationService authService;
    private final ExecutorService bg;
    private final OkHttpClient http;

    private AuthCallback authCallback;

    public YandexAuthProvider(@NonNull Activity activity) {
        this.activity = activity;
        this.authService = new AuthorizationService(activity);
        this.bg = MyExecutorService.getExecutorService();
        this.http = new OkHttpClient();
    }

    @Override
    public void authenticate(AuthCallback callback) {
        this.authCallback = callback;

        AuthorizationServiceConfiguration config =
                new AuthorizationServiceConfiguration(AUTH_ENDPOINT, TOKEN_ENDPOINT);

        AuthorizationRequest request = new AuthorizationRequest.Builder(
                config,
                CLIENT_ID,
                ResponseTypeValues.CODE,
                REDIRECT_URI
        )
                .setScopes(SCOPES)
                // Yandex supports PKCE parameters; AppAuth will include code_verifier when you set it.
                // If your AppAuth version supports it, enable PKCE explicitly like this:
                // .setCodeVerifier(CodeVerifierUtil.generateRandomCodeVerifier())
                .build();

        Intent authIntent = authService.getAuthorizationRequestIntent(request);
        activity.startActivityForResult(authIntent, REQ_AUTH);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode != REQ_AUTH) return;

        if (data == null) {
            if (authCallback != null)
                authCallback.onError(new Exception("Authorization cancelled"));
            return;
        }

        AuthorizationResponse resp = AuthorizationResponse.fromIntent(data);
        AuthorizationException ex = AuthorizationException.fromIntent(data);

        if (resp == null) {
            if (authCallback != null)
                authCallback.onError(ex != null ? ex : new Exception("Authorization failed"));
            return;
        }

        authService.performTokenRequest(resp.createTokenExchangeRequest(), (tokenResp, tokenEx) -> {
            if (tokenResp == null || tokenResp.accessToken == null) {
                if (authCallback != null)
                    authCallback.onError(tokenEx != null ? tokenEx : new Exception("Token exchange failed"));
                return;
            }

            final String accessToken = tokenResp.accessToken;
            final String refreshToken = tokenResp.refreshToken; // Yandex returns this in code flow :contentReference[oaicite:5]{index=5}
            final long expiry = (tokenResp.accessTokenExpirationTime != null)
                    ? tokenResp.accessTokenExpirationTime
                    : System.currentTimeMillis() + 3600_000L;

            fetchUserInfo(accessToken, (info, infoEx) -> {
                if (info == null) {
                    if (authCallback != null)
                        authCallback.onError(infoEx != null ? infoEx : new Exception("Failed to fetch user info"));
                    return;
                }

                String display = !TextUtils.isEmpty(info.displayName) ? info.displayName :
                        (!TextUtils.isEmpty(info.login) ? info.login : "Yandex User");

                CloudAccountPOJO account = new CloudAccountPOJO(
                        FileObjectType.YANDEX_TYPE.toString(),
                        display,
                        info.id,                 // stable user id :contentReference[oaicite:6]{index=6}
                        accessToken,
                        refreshToken,
                        expiry,
                        TextUtils.join(" ", SCOPES),
                        null, null, null
                );

                if (authCallback != null) authCallback.onSuccess(account);
            });
        });
    }

    @Override
    public void handleAuthorizationResponse(Intent intent) {
        // Not used in AppAuth pattern
    }

    @Override
    public void refreshToken(CloudAccountPOJO cloudAccount, AuthCallback callback) {
        if (cloudAccount == null || TextUtils.isEmpty(cloudAccount.refreshToken)) {
            if (callback != null) callback.onError(new Exception("No refresh token available"));
            return;
        }

        AuthorizationServiceConfiguration config =
                new AuthorizationServiceConfiguration(AUTH_ENDPOINT, TOKEN_ENDPOINT);

        // Yandex refresh uses /token with grant_type=refresh_token :contentReference[oaicite:7]{index=7}
        TokenRequest req = new TokenRequest.Builder(config, CLIENT_ID)
                .setGrantType(GrantTypeValues.REFRESH_TOKEN)
                .setRefreshToken(cloudAccount.refreshToken)
                .build();

        authService.performTokenRequest(req, (resp, ex) -> {
            if (resp == null || resp.accessToken == null) {
                if (callback != null)
                    callback.onError(ex != null ? ex : new Exception("Token refresh failed"));
                return;
            }

            cloudAccount.accessToken = resp.accessToken;
            cloudAccount.tokenExpiryTime = (resp.accessTokenExpirationTime != null)
                    ? resp.accessTokenExpirationTime
                    : System.currentTimeMillis() + 3600_000L;

            // Some providers rotate refresh token; keep if returned
            if (resp.refreshToken != null) {
                // Your CloudAccountPOJO.refreshToken is final in your model.
                // Long-term: make refreshToken mutable OR store rotated token in extra1 and DB.
                // For now, if yours is final, you cannot update it here.
            }

            if (callback != null) callback.onSuccess(cloudAccount);
        });
    }

    @Override
    public String getAccessToken(CloudAccountPOJO account) {
        return account != null ? account.accessToken : null;
    }

    @Override
    public boolean isAccessTokenValid(CloudAccountPOJO account) {
        return account != null
                && account.accessToken != null
                && System.currentTimeMillis() < account.tokenExpiryTime;
    }

    @Override
    public boolean supportsRefresh() {
        return true;
    }

    @Override
    public void logout(AuthCallback callback) {
        // local disconnect: just let app delete DB row and cached files
        if (callback != null) callback.onSuccess(null);
    }

    private void fetchUserInfo(@NonNull String accessToken, @NonNull UserInfoCallback cb) {
        bg.execute(() -> {
            try {
                // Yandex user info endpoint :contentReference[oaicite:8]{index=8}
                Request req = new Request.Builder()
                        .url("https://login.yandex.ru/info?format=json")
                        .addHeader("Authorization", "OAuth " + accessToken)
                        .build();

                try (Response res = http.newCall(req).execute()) {
                    if (!res.isSuccessful()) {
                        cb.onFetched(null, new Exception("UserInfo failed: " + res.code()));
                        return;
                    }
                    String body = res.body() != null ? res.body().string() : "";
                    UserInfo info = new Gson().fromJson(body, UserInfo.class);
                    cb.onFetched(info, null);
                }
            } catch (IOException e) {
                cb.onFetched(null, e);
            }
        });
    }

    public void dispose() {
        authService.dispose();
    }

    private interface UserInfoCallback {
        void onFetched(@Nullable UserInfo info, @Nullable Exception e);
    }

    private static final class UserInfo {
        @SerializedName("id")
        String id;                 // unique Yandex user id :contentReference[oaicite:9]{index=9}
        @SerializedName("login")
        String login;           // username :contentReference[oaicite:10]{index=10}
        @SerializedName("display_name")
        String displayName;
    }
}
