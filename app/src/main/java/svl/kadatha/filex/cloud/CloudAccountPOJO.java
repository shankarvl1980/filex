package svl.kadatha.filex.cloud;

import android.os.Parcel;
import android.os.Parcelable;

public class CloudAccountPOJO implements Parcelable {
    public static final Creator<CloudAccountPOJO> CREATOR = new Creator<CloudAccountPOJO>() {
        @Override
        public CloudAccountPOJO createFromParcel(Parcel in) {
            return new CloudAccountPOJO(in);
        }

        @Override
        public CloudAccountPOJO[] newArray(int size) {
            return new CloudAccountPOJO[size];
        }
    };

    public final String type;
    public final String displayName;
    public final String userId;
    public final String refreshToken;
    public final String scopes;
    public final String extra1;
    public final String extra2;
    public final String extra3;
    public String accessToken;
    public long tokenExpiryTime;

    // Constructor
    public CloudAccountPOJO(String type, String displayName, String userId,
                            String accessToken, String refreshToken, long tokenExpiryTime,
                            String scopes, String extra1, String extra2, String extra3) {
        this.type = type;
        this.displayName = displayName;
        this.userId = userId;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenExpiryTime = tokenExpiryTime;
        this.scopes = scopes;
        this.extra1 = extra1;
        this.extra2 = extra2;
        this.extra3 = extra3;
    }

    // Copy Constructor for deep copy
    public CloudAccountPOJO(CloudAccountPOJO other) {
        this.type = other.type != null ? new String(other.type) : null;
        this.displayName = other.displayName != null ? new String(other.displayName) : null;
        this.userId = other.userId != null ? new String(other.userId) : null;
        this.accessToken = other.accessToken != null ? new String(other.accessToken) : null;
        this.refreshToken = other.refreshToken != null ? new String(other.refreshToken) : null;
        this.tokenExpiryTime = other.tokenExpiryTime;
        this.scopes = other.scopes != null ? new String(other.scopes) : null;
        this.extra1 = other.extra1 != null ? new String(other.extra1) : null;
        this.extra2 = other.extra2 != null ? new String(other.extra2) : null;
        this.extra3 = other.extra3 != null ? new String(other.extra3) : null;
    }

    // Parcelable implementation
    protected CloudAccountPOJO(Parcel in) {
        type = in.readString();
        displayName = in.readString();
        userId = in.readString();
        accessToken = in.readString();
        refreshToken = in.readString();
        tokenExpiryTime = in.readLong();
        scopes = in.readString();
        extra1 = in.readString();
        extra2 = in.readString();
        extra3 = in.readString();
    }

    // Method to return a deep copy
    public CloudAccountPOJO deepCopy() {
        return new CloudAccountPOJO(this);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(type);
        dest.writeString(displayName);
        dest.writeString(userId);
        dest.writeString(accessToken);
        dest.writeString(refreshToken);
        dest.writeLong(tokenExpiryTime);
        dest.writeString(scopes);
        dest.writeString(extra1);
        dest.writeString(extra2);
        dest.writeString(extra3);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
