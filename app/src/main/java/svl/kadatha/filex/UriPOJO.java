package svl.kadatha.filex;

import android.net.Uri;

public class UriPOJO {
    private final Uri uri;
    private final String uri_authority;
    private final String uri_path;


    UriPOJO(Uri uri, String uri_authority, String uri_path) {
        this.uri = uri;
        this.uri_authority = uri_authority;
        this.uri_path = uri_path;
    }

    public Uri get_uri() {
        return uri;
    }

    public String get_authority() {
        return uri_authority;
    }

    public String get_path() {
        return uri_path;
    }
}
