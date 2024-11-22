package svl.kadatha.filex.cloud;

public interface CloudAuthProvider {

    // Starts the authentication process
    void authenticate(AuthCallback callback);

    // Refreshes the access token if needed
    void refreshToken(AuthCallback callback);

    // Returns the current access token
    String getAccessToken();

    // Checks if the access token is valid
    boolean isAccessTokenValid();

    // Logs out and clears stored tokens
    void logout(AuthCallback callback);

    // Callback interface to handle authentication results
    interface AuthCallback {
        void onSuccess(CloudAccountPOJO account);
        void onError(Exception e);
    }
}
