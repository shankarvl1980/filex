package svl.kadatha.filex.cloud;

import android.content.Intent;

public interface CloudAuthProvider {

    void authenticate(AuthCallback callback);

    default void authenticate(AuthCallback callback, String loginHintOrUserId) {
        authenticate(callback);
    }

    void handleAuthorizationResponse(Intent intent);

    default void onActivityResult(int requestCode, int resultCode, Intent data) {
    }

    // ✅ Account-based refresh (works with DB-loaded accounts)
    void refreshToken(CloudAccountPOJO account, AuthCallback callback);

    // ✅ Account-based access token utilities
    default String getAccessToken(CloudAccountPOJO account) {
        return account != null ? account.accessToken : null;
    }

    default boolean isAccessTokenValid(CloudAccountPOJO account) {
        return account != null
                && account.accessToken != null
                && account.tokenExpiryTime > (System.currentTimeMillis() + 60_000L);
    }

    boolean supportsRefresh();

    void logout(AuthCallback callback);

    interface AuthCallback {
        void onSuccess(CloudAccountPOJO account);

        void onError(Exception e);
    }
}
