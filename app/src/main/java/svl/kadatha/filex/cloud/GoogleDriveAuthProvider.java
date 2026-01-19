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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import svl.kadatha.filex.FileObjectType;
import svl.kadatha.filex.MyExecutorService;

public final class GoogleDriveAuthProvider implements CloudAuthProvider {

    private static final Uri AUTH_ENDPOINT = Uri.parse("https://accounts.google.com/o/oauth2/v2/auth");
    private static final Uri TOKEN_ENDPOINT = Uri.parse("https://oauth2.googleapis.com/token");
    private static final String CLIENT_ID =
            "603518003549-8m2h1s1blonbo7s3rvok93uf40ipb3cs.apps.googleusercontent.com";

    private static final String CLIENT_ID_SHORT =
            "603518003549-8m2h1s1blonbo7s3rvok93uf40ipb3cs";

    private static final Uri REDIRECT_URI =
            Uri.parse("com.googleusercontent.apps." + CLIENT_ID_SHORT + ":/oauth2redirect");

    private static final String[] SCOPES = new String[] {
            "openid",
            "email",
            "profile",
            "https://www.googleapis.com/auth/drive"
    };

    private static final int REQ_AUTH = 9101;

    private final Activity activity;
    private final AuthorizationService authService;
    private final ExecutorService bg;
    private final OkHttpClient http;

    private AuthCallback authCallback;
    private CloudAccountPOJO cloudAccount;

    public GoogleDriveAuthProvider(@NonNull Activity activity) {
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

        Map<String, String> extra = new HashMap<>();
        extra.put("access_type", "offline");
        extra.put("include_granted_scopes", "true");

        AuthorizationRequest request = new AuthorizationRequest.Builder(
                config,
                CLIENT_ID,
                ResponseTypeValues.CODE,
                REDIRECT_URI
        )
                .setScopes(SCOPES)
                .setPrompt("consent")
                .setAdditionalParameters(extra)
                .build();

        Intent authIntent = authService.getAuthorizationRequestIntent(request);
        activity.startActivityForResult(authIntent, REQ_AUTH);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode != REQ_AUTH) return;

        if (data == null) {
            if (authCallback != null) authCallback.onError(new Exception("Authorization cancelled"));
            return;
        }

        AuthorizationResponse resp = AuthorizationResponse.fromIntent(data);
        AuthorizationException ex = AuthorizationException.fromIntent(data);

        if (resp == null) {
            if (authCallback != null) authCallback.onError(ex != null ? ex : new Exception("Authorization failed"));
            return;
        }

        authService.performTokenRequest(resp.createTokenExchangeRequest(), (tokenResp, tokenEx) -> {
            if (tokenResp == null || tokenResp.accessToken == null) {
                if (authCallback != null) authCallback.onError(tokenEx != null ? tokenEx : new Exception("Token exchange failed"));
                return;
            }

            String accessToken = tokenResp.accessToken;
            String refreshToken = tokenResp.refreshToken;
            long expiry = tokenResp.accessTokenExpirationTime != null
                    ? tokenResp.accessTokenExpirationTime
                    : System.currentTimeMillis() + 3600_000L;

            fetchUserInfo(accessToken, (userInfo, userInfoEx) -> {
                if (userInfo == null) {
                    if (authCallback != null) authCallback.onError(userInfoEx != null ? userInfoEx : new Exception("Failed to fetch user info"));
                    return;
                }

                String userId = !TextUtils.isEmpty(userInfo.email) ? userInfo.email : userInfo.id;
                String display = !TextUtils.isEmpty(userInfo.name) ? userInfo.name : userId;

                cloudAccount = new CloudAccountPOJO(
                        FileObjectType.GOOGLE_DRIVE_TYPE.toString(),
                        display,
                        userId,
                        accessToken,
                        refreshToken,
                        expiry,
                        TextUtils.join(" ", SCOPES),
                        null, null, null
                );


                if (authCallback != null) authCallback.onSuccess(cloudAccount);
            });
        });
    }

    @Override
    public void handleAuthorizationResponse(Intent intent) {
        // not used for Google in Option B
    }

    @Override
    public void refreshToken(CloudAccountPOJO cloudAccount,AuthCallback callback) {
        this.cloudAccount=cloudAccount;
        if (cloudAccount == null || cloudAccount.refreshToken == null) {
            if (callback != null) callback.onError(new Exception("No refresh token available"));
            return;
        }

        AuthorizationServiceConfiguration config =
                new AuthorizationServiceConfiguration(AUTH_ENDPOINT, TOKEN_ENDPOINT);

        TokenRequest req = new TokenRequest.Builder(config, CLIENT_ID)
                .setGrantType(GrantTypeValues.REFRESH_TOKEN)
                .setRefreshToken(cloudAccount.refreshToken)
                .build();

        authService.performTokenRequest(req, (resp, ex) -> {
            if (resp == null || resp.accessToken == null) {
                if (callback != null) callback.onError(ex != null ? ex : new Exception("Token refresh failed"));
                return;
            }

            cloudAccount.accessToken = resp.accessToken;
            cloudAccount.tokenExpiryTime = resp.accessTokenExpirationTime != null
                    ? resp.accessTokenExpirationTime
                    : System.currentTimeMillis() + 3600_000L;

            if (callback != null) callback.onSuccess(cloudAccount);
        });
    }

    @Override
    public String getAccessToken(CloudAccountPOJO cloudAccountPOJO) {
        this.cloudAccount=cloudAccountPOJO;
        return cloudAccount != null ? cloudAccount.accessToken : null;
    }

    @Override
    public boolean isAccessTokenValid(CloudAccountPOJO cloudAccountPOJO) {
        this.cloudAccount=cloudAccountPOJO;
        return cloudAccount != null && cloudAccount.accessToken != null
                && System.currentTimeMillis() < cloudAccount.tokenExpiryTime;
    }

    @Override
    public boolean supportsRefresh() {
        return true;
    }

    @Override
    public void logout(AuthCallback callback) {
        cloudAccount = null;
        if (callback != null) callback.onSuccess(null);
    }

    private interface UserInfoCallback {
        void onFetched(@Nullable UserInfo info, @Nullable Exception e);
    }

    private void fetchUserInfo(@NonNull String accessToken, @NonNull UserInfoCallback cb) {
        bg.execute(() -> {
            try {
                Request req = new Request.Builder()
                        .url("https://openidconnect.googleapis.com/v1/userinfo")
                        .addHeader("Authorization", "Bearer " + accessToken)
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

    private static final class UserInfo {
        @SerializedName("sub") String id;
        @SerializedName("name") String name;
        @SerializedName("email") String email;
        @SerializedName("picture") String picture;
    }

    public void dispose() {
        authService.dispose();
    }
}
