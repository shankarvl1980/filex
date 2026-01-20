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

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import svl.kadatha.filex.FileObjectType;
import svl.kadatha.filex.MyExecutorService;

public final class DropboxAuthProvider implements CloudAuthProvider {

    // Dropbox OAuth endpoints
    private static final Uri AUTH_ENDPOINT = Uri.parse("https://www.dropbox.com/oauth2/authorize");
    private static final Uri TOKEN_ENDPOINT = Uri.parse("https://api.dropboxapi.com/oauth2/token");

    // Dropbox App Key (client_id)
    private static final String CLIENT_ID = "igtdjo14bfrwsck";

    // Redirect URI must match exactly what you register in Dropbox App Console
    // Example custom scheme:
    //   svl.kadatha.filex.dropbox://oauth2redirect
    private static final Uri REDIRECT_URI =
            Uri.parse("svl.kadatha.filex.dropbox://oauth2redirect");


    // Request exactly the scopes you enabled in Dropbox console.
    // For file manager typical minimum:
    // - files.metadata.read
    // - files.content.read
    // - files.content.write
    private static final String[] SCOPES = new String[]{
            "account_info.read",
            "files.metadata.read",
            "files.content.read",
            "files.content.write"
    };

    private static final int REQ_AUTH = 9201;

    private final Activity activity;
    private final AuthorizationService authService;
    private final ExecutorService bg;
    private final OkHttpClient http;

    private AuthCallback authCallback;
    private CloudAccountPOJO cloudAccount;

    public DropboxAuthProvider(@NonNull Activity activity) {
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

        // Dropbox requires token_access_type=offline to return refresh_token
        // (otherwise you may only get short-lived access token). :contentReference[oaicite:1]{index=1}
        Map<String, String> extra = new HashMap<>();
        extra.put("token_access_type", "offline");

        AuthorizationRequest request = new AuthorizationRequest.Builder(
                config,
                CLIENT_ID,
                ResponseTypeValues.CODE,
                REDIRECT_URI
        )
                .setScopes(SCOPES)                 // Dropbox scopes model :contentReference[oaicite:2]{index=2}
                .setAdditionalParameters(extra)
                // Optional: force account chooser/consent behavior
                // .setPrompt("select_account")
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

            String accessToken = tokenResp.accessToken;
            String refreshToken = tokenResp.refreshToken; // should be present with token_access_type=offline :contentReference[oaicite:3]{index=3}

            long expiry = (tokenResp.accessTokenExpirationTime != null)
                    ? tokenResp.accessTokenExpirationTime
                    : System.currentTimeMillis() + 4 * 60 * 60 * 1000L; // conservative fallback

            fetchCurrentAccount(accessToken, (acct, acctEx) -> {
                if (acct == null) {
                    if (authCallback != null)
                        authCallback.onError(acctEx != null ? acctEx : new Exception("Failed to fetch Dropbox account"));
                    return;
                }

                cloudAccount = new CloudAccountPOJO(
                        FileObjectType.DROP_BOX_TYPE.toString(),
                        acct.name != null ? acct.name.displayName : "Dropbox",
                        acct.accountId,
                        accessToken,
                        refreshToken,
                        expiry,
                        TextUtils.join(" ", SCOPES),
                        acct.email,     // extra1 = email (optional)
                        null,
                        null
                );

                if (authCallback != null) authCallback.onSuccess(cloudAccount);
            });
        });
    }

    @Override
    public void handleAuthorizationResponse(Intent intent) {
        // For your app you’re using onActivityResult-based flow
    }

    @Override
    public void refreshToken(CloudAccountPOJO account, AuthCallback callback) {
        this.cloudAccount = account;

        if (account == null || TextUtils.isEmpty(account.refreshToken)) {
            if (callback != null) callback.onError(new Exception("No refresh token available"));
            return;
        }

        AuthorizationServiceConfiguration config =
                new AuthorizationServiceConfiguration(AUTH_ENDPOINT, TOKEN_ENDPOINT);

        TokenRequest req = new TokenRequest.Builder(config, CLIENT_ID)
                .setGrantType(GrantTypeValues.REFRESH_TOKEN)
                .setRefreshToken(account.refreshToken)
                .build();

        authService.performTokenRequest(req, (resp, ex) -> {
            if (resp == null || resp.accessToken == null) {
                if (callback != null)
                    callback.onError(ex != null ? ex : new Exception("Token refresh failed"));
                return;
            }

            account.accessToken = resp.accessToken;
            account.tokenExpiryTime = (resp.accessTokenExpirationTime != null)
                    ? resp.accessTokenExpirationTime
                    : System.currentTimeMillis() + 4 * 60 * 60 * 1000L;

            if (callback != null) callback.onSuccess(account);
        });
    }

    @Override
    public String getAccessToken(CloudAccountPOJO account) {
        this.cloudAccount = account;
        return account != null ? account.accessToken : null;
    }

    @Override
    public boolean isAccessTokenValid(CloudAccountPOJO account) {
        this.cloudAccount = account;
        return account != null
                && account.accessToken != null
                && account.tokenExpiryTime > (System.currentTimeMillis() + 60_000L); // 60s buffer
    }

    @Override
    public boolean supportsRefresh() {
        return true;
    }

    @Override
    public void logout(AuthCallback callback) {
        // For “local disconnect”, you don’t need to revoke. Just clear in-memory.
        cloudAccount = null;
        if (callback != null) callback.onSuccess(null);
    }

    // -------- Dropbox account lookup --------
    // POST https://api.dropboxapi.com/2/users/get_current_account with Bearer token :contentReference[oaicite:4]{index=4}

    private void fetchCurrentAccount(@NonNull String accessToken, @NonNull CurrentAccountCallback cb) {
        bg.execute(() -> {
            try {
                Request req = new Request.Builder()
                        .url("https://api.dropboxapi.com/2/users/get_current_account")
                        .addHeader("Authorization", "Bearer " + accessToken)
                        .post(RequestBody.create(new byte[0], MediaType.parse("application/json")))
                        .build();

                try (Response res = http.newCall(req).execute()) {
                    if (!res.isSuccessful()) {
                        cb.onFetched(null, new Exception("get_current_account failed: " + res.code()));
                        return;
                    }
                    String body = res.body() != null ? res.body().string() : "";
                    DropboxCurrentAccount acct = new Gson().fromJson(body, DropboxCurrentAccount.class);
                    cb.onFetched(acct, null);
                }
            } catch (IOException e) {
                cb.onFetched(null, e);
            }
        });
    }

    public void dispose() {
        authService.dispose();
    }

    private interface CurrentAccountCallback {
        void onFetched(@Nullable DropboxCurrentAccount acct, @Nullable Exception e);
    }

    private static final class DropboxCurrentAccount {
        @SerializedName("account_id")
        String accountId;
        @SerializedName("email")
        String email;
        @SerializedName("name")
        Name name;

        static final class Name {
            @SerializedName("display_name")
            String displayName;
        }
    }
}
