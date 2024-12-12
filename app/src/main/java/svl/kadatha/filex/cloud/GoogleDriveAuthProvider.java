package svl.kadatha.filex.cloud;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.Nullable;

import net.openid.appauth.*;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import okhttp3.*;
import svl.kadatha.filex.MyExecutorService;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class GoogleDriveAuthProvider implements CloudAuthProvider {

    private final Activity activity;
    private AuthCallback authCallback;
    private CloudAccountPOJO cloudAccount;

    private static final String client_id = "566755170747-8lio5rj01qgmpq469e036791bpqu9qjg.apps.googleusercontent.com";
    private static final String authorization_endpoint = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String token_endpoint = "https://oauth2.googleapis.com/token";
    private static final String[] scopes = {
            "openid",
            "email",
            "profile",
            "https://www.googleapis.com/auth/drive.file",
            "https://www.googleapis.com/auth/drive.appdata"
    };

    private AuthorizationService authService;
    private AuthorizationRequest authRequest;
    private AuthState authState;

    public GoogleDriveAuthProvider(Activity activity) {
        this.activity = activity;
        authService = new AuthorizationService(activity);
        buildAuthRequest();
    }

    private void buildAuthRequest() {
        AuthorizationServiceConfiguration serviceConfig = new AuthorizationServiceConfiguration(
                Uri.parse(authorization_endpoint),
                Uri.parse(token_endpoint)
        );

        // Generate a loopback redirect URI
        Uri redirectUri = Uri.parse("svl.kadatha.filex:/oauth2redirect");

        AuthorizationRequest.Builder authRequestBuilder = new AuthorizationRequest.Builder(
                serviceConfig,
                client_id,
                ResponseTypeValues.CODE,
                redirectUri
        )
                .setScopes(scopes)
                .setPrompt("consent"); // Set the 'prompt' parameter directly
        authRequest = authRequestBuilder.build();
    }

    @Override
    public void authenticate(AuthCallback callback) {
        this.authCallback = callback;

        // Start the authorization flow
        authService.performAuthorizationRequest(
                authRequest,
                createPendingIntent()
        );
    }

    private PendingIntent createPendingIntent() {
        Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setAction("com.google.auth.TOKEN_ACTION");
        return PendingIntent.getActivity(
                activity, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    @Override
    public void handleAuthorizationResponse(Intent intent) {
        if (!"com.google.auth.TOKEN_ACTION".equals(intent.getAction())) {
            return;
        }

        AuthorizationResponse response = AuthorizationResponse.fromIntent(intent);
        AuthorizationException error = AuthorizationException.fromIntent(intent);

        if (response != null) {
            authService.performTokenRequest(
                    response.createTokenExchangeRequest(),
                    (tokenResponse, exception) -> {
                        if (tokenResponse != null) {
                            handleTokenResponse(tokenResponse);
                        } else {
                            if (authCallback != null) {
                                authCallback.onError(exception != null ? exception : new Exception("Token exchange failed"));
                            }
                        }
                    }
            );
        } else {
            if (authCallback != null) {
                authCallback.onError(error != null ? error : new Exception("Authorization failed"));
            }
        }
    }

    private void handleTokenResponse(TokenResponse response) {
        long tokenExpiryTime = response.accessTokenExpirationTime != null
                ? response.accessTokenExpirationTime
                : System.currentTimeMillis() + (3600 * 1000); // Default to 1 hour

        // Fetch user info
        fetchUserInfo(response.accessToken, (userInfo, userInfoEx) -> {
            if (userInfo != null) {
                cloudAccount = new CloudAccountPOJO(
                        "google_drive",
                        userInfo.name,
                        userInfo.id,
                        response.accessToken,
                        response.refreshToken,
                        tokenExpiryTime,
                        null, // scopes
                        null,
                        null,
                        null
                );

                if (authCallback != null) {
                    authCallback.onSuccess(cloudAccount);
                }
            } else {
                if (authCallback != null) {
                    authCallback.onError(userInfoEx != null ? userInfoEx : new Exception("Failed to fetch user info"));
                }
            }
        });
    }

    private void fetchUserInfo(String accessToken, UserInfoCallback callback) {
        ExecutorService executorService = MyExecutorService.getExecutorService();
        executorService.execute(() -> {
            try {
                OkHttpClient client = new OkHttpClient();

                Request request = new Request.Builder()
                        .url("https://www.googleapis.com/oauth2/v3/userinfo")
                        .addHeader("Authorization", "Bearer " + accessToken)
                        .build();

                Response response = client.newCall(request).execute();

                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Gson gson = new Gson();
                    UserInfo userInfo = gson.fromJson(responseBody, UserInfo.class);
                    if (callback != null) {
                        callback.onUserInfoFetched(userInfo, null);
                    }
                } else {
                    if (callback != null) {
                        callback.onUserInfoFetched(null, new Exception("Failed to fetch user info: " + response.code()));
                    }
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onUserInfoFetched(null, e);
                }
            }
        });
    }

    // UserInfo class to parse JSON response
    private static class UserInfo {
        @SerializedName("sub")
        String id;
        @SerializedName("name")
        String name;
        @SerializedName("email")
        String email;
        @SerializedName("picture")
        String picture;
    }

    private interface UserInfoCallback {
        void onUserInfoFetched(UserInfo userInfo, Exception e);
    }

    public void refreshToken(AuthCallback callback) {
        if (cloudAccount == null || cloudAccount.refreshToken == null) {
            if (callback != null) {
                callback.onError(new Exception("No refresh token available"));
            }
            return;
        }

        TokenRequest tokenRequest = new TokenRequest.Builder(
                new AuthorizationServiceConfiguration(
                        Uri.parse(authorization_endpoint),
                        Uri.parse(token_endpoint)
                ),
                client_id
        )
                .setGrantType(GrantTypeValues.REFRESH_TOKEN)
                .setRefreshToken(cloudAccount.refreshToken)
                .build();

        authService.performTokenRequest(tokenRequest, (response, ex) -> {
            if (response != null) {
                long tokenExpiryTime = response.accessTokenExpirationTime != null
                        ? response.accessTokenExpirationTime
                        : System.currentTimeMillis() + (3600 * 1000); // Default to 1 hour

                cloudAccount.accessToken = response.accessToken;
                cloudAccount.tokenExpiryTime = tokenExpiryTime;

                if (callback != null) {
                    callback.onSuccess(cloudAccount);
                }
            } else {
                if (callback != null) {
                    callback.onError(ex != null ? ex : new Exception("Token refresh failed"));
                }
            }
        });
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
        if (cloudAccount != null && cloudAccount.accessToken != null) {
            ExecutorService executorService = MyExecutorService.getExecutorService();
            executorService.execute(() -> {
                try {
                    OkHttpClient client = new OkHttpClient();

                    RequestBody requestBody = new FormBody.Builder()
                            .add("token", cloudAccount.accessToken)
                            .build();

                    Request request = new Request.Builder()
                            .url("https://oauth2.googleapis.com/revoke")
                            .post(requestBody)
                            .build();

                    Response response = client.newCall(request).execute();

                    if (response.isSuccessful()) {
                        cloudAccount = null;
                        if (callback != null) {
                            callback.onSuccess(null);
                        }
                    } else {
                        if (callback != null) {
                            callback.onError(new Exception("Failed to revoke token: " + response.code()));
                        }
                    }

                } catch (Exception e) {
                    if (callback != null) {
                        callback.onError(e);
                    }
                }
            });
        } else {
            cloudAccount = null;
            if (callback != null) {
                callback.onSuccess(null);
            }
        }
    }

    // Dispose of AuthorizationService when done
    public void dispose() {
        if (authService != null) {
            authService.dispose();
        }
    }
}
