package svl.kadatha.filex.cloud;

import android.app.Activity;
import android.content.Intent;

import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;

public class GoogleDriveAuthProvider implements CloudAuthProvider {
    private static final int RC_SIGN_IN = 1000;
    private final Activity activity;
    private final GoogleSignInClient googleSignInClient;
    private AuthCallback authCallback;
    private CloudAccountPOJO cloudAccount;

    public GoogleDriveAuthProvider(Activity activity) {
        this.activity = activity;
        googleSignInClient = buildGoogleSignInClient();
    }

    private GoogleSignInClient buildGoogleSignInClient() {
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(
                        new Scope(Scopes.DRIVE_FILE),
                        new Scope(Scopes.DRIVE_APPFOLDER)
                )
                .requestServerAuthCode("YOUR_SERVER_CLIENT_ID", true)
                .build();

        return GoogleSignIn.getClient(activity, options);
    }

    @Override
    public void authenticate(AuthCallback callback) {
        this.authCallback = callback;
        Intent signInIntent = googleSignInClient.getSignInIntent();
        activity.startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    // Call this method from your Activity's onActivityResult
    public void handleSignInResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != RC_SIGN_IN) {
            return;
        }
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);

            // Obtain the authorization code
            String authCode = account.getServerAuthCode();

            // Exchange auth code for tokens on your backend server
            exchangeAuthCodeForTokens(authCode, account);

        } catch (ApiException e) {
            if (authCallback != null) {
                authCallback.onError(e);
            }
        }
    }

    private void exchangeAuthCodeForTokens(String authCode, GoogleSignInAccount account) {
        // For security, exchange auth code for tokens on your secure server
        // Here, we'll simulate this process

        // Simulated token exchange
        new Thread(() -> {
            try {
                // Simulate network call
                Thread.sleep(1000);

                // Simulate tokens
                String accessToken = "SIMULATED_ACCESS_TOKEN";
                String refreshToken = "SIMULATED_REFRESH_TOKEN";
                long tokenExpiryTime = System.currentTimeMillis() + 3600 * 1000; // 1 hour

                cloudAccount = new CloudAccountPOJO(
                        "google_drive",
                        account.getDisplayName(),
                        account.getId(),
                        accessToken,
                        refreshToken,
                        tokenExpiryTime,
                        "drive.file drive.appfolder",
                        null,
                        null,
                        null
                );

                if (authCallback != null) {
                    authCallback.onSuccess(cloudAccount);
                }

            } catch (InterruptedException e) {
                if (authCallback != null) {
                    authCallback.onError(e);
                }
            }
        }).start();
    }

    @Override
    public void refreshToken(AuthCallback callback) {
        // Implement token refresh logic using the refresh token
        // Should be done on a secure backend server

        // Simulated token refresh
        new Thread(() -> {
            try {
                Thread.sleep(500);
                cloudAccount = new CloudAccountPOJO(
                        cloudAccount.type,
                        cloudAccount.displayName,
                        cloudAccount.userId,
                        "NEW_SIMULATED_ACCESS_TOKEN",
                        cloudAccount.refreshToken,
                        System.currentTimeMillis() + 3600 * 1000,
                        cloudAccount.scopes,
                        cloudAccount.extra1,
                        cloudAccount.extra2,
                        cloudAccount.extra3
                );

                if (callback != null) {
                    callback.onSuccess(cloudAccount);
                }

            } catch (InterruptedException e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }
        }).start();
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
        googleSignInClient.signOut().addOnCompleteListener(activity, task -> {
            cloudAccount = null;
            if (callback != null) {
                callback.onSuccess(null);
            }
        });
    }
}
