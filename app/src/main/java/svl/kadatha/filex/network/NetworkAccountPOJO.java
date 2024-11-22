package svl.kadatha.filex.network;

import android.os.Parcel;
import android.os.Parcelable;

public class NetworkAccountPOJO implements Parcelable {
    public static final Creator<NetworkAccountPOJO> CREATOR = new Creator<NetworkAccountPOJO>() {
        @Override
        public NetworkAccountPOJO createFromParcel(Parcel in) {
            return new NetworkAccountPOJO(in);
        }

        @Override
        public NetworkAccountPOJO[] newArray(int size) {
            return new NetworkAccountPOJO[size];
        }
    };
    final String host;
    final int port;
    final String user_name;
    final String password;
    final String encoding;
    final String display;
    final String type;
    // FTP-specific fields
    final String mode;
    final boolean anonymous;
    final boolean useFTPS;
    // SFTP-specific fields
    final String privateKeyPath;
    final String privateKeyPassphrase;
    final String knownHostsPath;
    // WebDAV-specific fields
    final String basePath;
    final boolean useHTTPS;
    // SMB-specific fields
    final String domain;
    final String shareName;
    final String smbVersion;

    // Constructor
    public NetworkAccountPOJO(String host, int port, String user_name, String password,
                              String encoding, String display, String type,
                              String mode, boolean anonymous, boolean useFTPS,
                              String privateKeyPath, String privateKeyPassphrase, String knownHostsPath,
                              String basePath, boolean useHTTPS,
                              String domain, String shareName, String smbVersion) {
        this.host = host;
        this.port = port;
        this.user_name = user_name;
        this.password = password;
        this.encoding = encoding;
        this.display = display;
        this.type = type;
        this.mode = mode;
        this.anonymous = anonymous;
        this.useFTPS = useFTPS;
        this.privateKeyPath = privateKeyPath;
        this.privateKeyPassphrase = privateKeyPassphrase;
        this.knownHostsPath = knownHostsPath;
        this.basePath = basePath;
        this.useHTTPS = useHTTPS;
        this.domain = domain;
        this.shareName = shareName;
        this.smbVersion = smbVersion;
    }

    // Copy Constructor for deep copy
    public NetworkAccountPOJO(NetworkAccountPOJO other) {
        this.host = other.host != null ? other.host : null;
        this.port = other.port;
        this.user_name = other.user_name != null ? other.user_name : null;
        this.password = other.password != null ? other.password : null;
        this.encoding = other.encoding != null ? other.encoding : null;
        this.display = other.display != null ? other.display : null;
        this.type = other.type != null ? other.type : null;
        this.mode = other.mode != null ? other.mode : null;
        this.anonymous = other.anonymous;
        this.useFTPS = other.useFTPS;
        this.privateKeyPath = other.privateKeyPath != null ? other.privateKeyPath : null;
        this.privateKeyPassphrase = other.privateKeyPassphrase != null ? other.privateKeyPassphrase : null;
        this.knownHostsPath = other.knownHostsPath != null ? other.knownHostsPath : null;
        this.basePath = other.basePath != null ? other.basePath : null;
        this.useHTTPS = other.useHTTPS;
        this.domain = other.domain != null ? other.domain : null;
        this.shareName = other.shareName != null ? other.shareName : null;
        this.smbVersion = other.smbVersion != null ? other.smbVersion : null;
    }

    // Parcelable implementation
    protected NetworkAccountPOJO(Parcel in) {
        host = in.readString();
        port = in.readInt();
        user_name = in.readString();
        password = in.readString();
        encoding = in.readString();
        display = in.readString();
        type = in.readString();
        mode = in.readString();
        anonymous = in.readByte() != 0;
        useFTPS = in.readByte() != 0;
        privateKeyPath = in.readString();
        privateKeyPassphrase = in.readString();
        knownHostsPath = in.readString();
        basePath = in.readString();
        useHTTPS = in.readByte() != 0;
        domain = in.readString();
        shareName = in.readString();
        smbVersion = in.readString();
    }

    // Method to return a deep copy
    public NetworkAccountPOJO deepCopy() {
        return new NetworkAccountPOJO(this);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(host);
        dest.writeInt(port);
        dest.writeString(user_name);
        dest.writeString(password);
        dest.writeString(encoding);
        dest.writeString(display);
        dest.writeString(type);
        dest.writeString(mode);
        dest.writeByte((byte) (anonymous ? 1 : 0));
        dest.writeByte((byte) (useFTPS ? 1 : 0));
        dest.writeString(privateKeyPath);
        dest.writeString(privateKeyPassphrase);
        dest.writeString(knownHostsPath);
        dest.writeString(basePath);
        dest.writeByte((byte) (useHTTPS ? 1 : 0));
        dest.writeString(domain);
        dest.writeString(shareName);
        dest.writeString(smbVersion);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}

