package svl.kadatha.filex.network;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

import svl.kadatha.filex.cloud.TokenCrypto;

public class NetworkAccountsDatabaseHelper extends SQLiteOpenHelper {

    static final String DATABASE = "NetworkAccountsDetails.db";
    static final String TABLE = "NetworkAccountsList";
    private static final int DATABASE_VERSION = 1;

    // Columns
    private static final String COL_HOST = "host";
    private static final String COL_PORT = "port";
    private static final String COL_USER_NAME = "user_name";
    private static final String COL_PASSWORD = "password";
    private static final String COL_ENCODING = "encoding";
    private static final String COL_DISPLAY = "display";
    private static final String COL_TYPE = "type";
    private static final String COL_MODE = "mode";
    private static final String COL_ANONYMOUS = "anonymous";
    private static final String COL_USE_FTPS = "useFTPS";
    private static final String COL_PRIVATE_KEY_PATH = "privateKeyPath";
    private static final String COL_PRIVATE_KEY_PASSPHRASE = "privateKeyPassphrase";
    private static final String COL_KNOWN_HOSTS_PATH = "knownHostsPath";
    private static final String COL_BASE_PATH = "basePath";
    private static final String COL_USE_HTTPS = "useHTTPS";
    private static final String COL_DOMAIN = "domain";
    private static final String COL_SHARE_NAME = "shareName";
    private static final String COL_SMB_VERSION = "smbVersion";

    private final Context appContext;

    public NetworkAccountsDatabaseHelper(Context context) {
        super(context, DATABASE, null, DATABASE_VERSION);
        this.appContext = context.getApplicationContext();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
                COL_HOST + " TEXT, " +
                COL_PORT + " INTEGER, " +
                COL_USER_NAME + " TEXT, " +
                COL_PASSWORD + " TEXT, " +
                COL_ENCODING + " TEXT, " +
                COL_DISPLAY + " TEXT, " +
                COL_TYPE + " TEXT, " +
                COL_MODE + " TEXT, " +
                COL_ANONYMOUS + " INTEGER, " +
                COL_USE_FTPS + " INTEGER, " +
                COL_PRIVATE_KEY_PATH + " TEXT, " +
                COL_PRIVATE_KEY_PASSPHRASE + " TEXT, " +
                COL_KNOWN_HOSTS_PATH + " TEXT, " +
                COL_BASE_PATH + " TEXT, " +
                COL_USE_HTTPS + " INTEGER, " +
                COL_DOMAIN + " TEXT, " +
                COL_SHARE_NAME + " TEXT, " +
                COL_SMB_VERSION + " TEXT, " +
                "PRIMARY KEY (" + COL_HOST + ", " + COL_PORT + ", " + COL_USER_NAME + ", " + COL_TYPE + ")" +
                ")");

        db.execSQL("CREATE INDEX idx_server_port_user_type ON " + TABLE +
                " (" + COL_HOST + ", " + COL_PORT + ", " + COL_USER_NAME + ", " + COL_TYPE + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // future migrations
    }

    // Insert
    public long insert(NetworkAccountPOJO pojo) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE, COL_HOST + "=? AND " + COL_PORT + "=? AND " + COL_USER_NAME + "=? AND " + COL_TYPE + "=?",
                new String[]{pojo.host, String.valueOf(pojo.port), pojo.user_name, pojo.type});
        ContentValues values = createContentValues(pojo);
        return db.insert(TABLE, null, values);
    }

    // UpdateOrInsert
    public long updateOrInsert(String original_host, int original_port, String original_user_name, String original_type,
                               NetworkAccountPOJO pojo) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = createContentValues(pojo);

        int rowsAffected = db.update(TABLE, values,
                COL_HOST + "=? AND " + COL_PORT + "=? AND " + COL_USER_NAME + "=? AND " + COL_TYPE + "=?",
                new String[]{original_host, String.valueOf(original_port), original_user_name, original_type});

        if (rowsAffected == 0) {
            try {
                return db.insert(TABLE, null, values);
            } catch (SQLiteException e) {
                return -1;
            }
        }
        return rowsAffected;
    }

    // Delete
    public int delete(String host, int port, String user_name, String type) {
        return getWritableDatabase().delete(TABLE,
                COL_HOST + "=? AND " + COL_PORT + "=? AND " + COL_USER_NAME + "=? AND " + COL_TYPE + "=?",
                new String[]{host, String.valueOf(port), user_name, type});
    }

    // Change display name
    public int change_display(String host, int port, String user_name, String new_display_name, String type) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_DISPLAY, new_display_name);
        return db.update(TABLE, values,
                COL_HOST + "=? AND " + COL_PORT + "=? AND " + COL_USER_NAME + "=? AND " + COL_TYPE + "=?",
                new String[]{host, String.valueOf(port), user_name, type});
    }

    // Get all accounts
    public List<NetworkAccountPOJO> getAllNetworkAccountPOJOList() {
        List<NetworkAccountPOJO> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor c = null;
        try {
            c = db.query(TABLE, null, null, null, null, null,
                    COL_TYPE + " ASC, " + COL_DISPLAY + " COLLATE NOCASE ASC");
            if (c != null && c.moveToFirst()) {
                do {
                    list.add(createPojoFromCursor(c));
                } while (c.moveToNext());
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
        }
        return list;
    }

    // Get by type
    public List<NetworkAccountPOJO> getNetworkAccountPOJOList(String type) {
        List<NetworkAccountPOJO> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor c = null;
        try {
            c = db.query(TABLE, null, COL_TYPE + " = ?", new String[]{type},
                    null, null, null);
            if (c != null && c.moveToFirst()) {
                do {
                    list.add(createPojoFromCursor(c));
                } while (c.moveToNext());
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
        }
        return list;
    }

    // Get specific account
    public NetworkAccountPOJO getNetworkAccountPOJO(String host, int port, String user_name, String type) {
        SQLiteDatabase db = getReadableDatabase();

        Cursor c = null;
        try {
            c = db.query(TABLE, null,
                    COL_HOST + "=? AND " + COL_PORT + "=? AND " + COL_USER_NAME + "=? AND " + COL_TYPE + "=?",
                    new String[]{host, String.valueOf(port), user_name, type},
                    null, null, null);

            if (c != null && c.moveToFirst()) {
                return createPojoFromCursor(c);
            }
        } finally {
            if (c != null) c.close();
        }
        return null;
    }

    // -------------------- helpers --------------------

    private ContentValues createContentValues(NetworkAccountPOJO pojo) {
        ContentValues v = new ContentValues();

        v.put(COL_HOST, pojo.host);
        v.put(COL_PORT, pojo.port);
        v.put(COL_USER_NAME, pojo.user_name);

        // Encrypt secrets at rest (TEXT + base64 payload)
        try {
            v.put(COL_PASSWORD, TokenCrypto.encrypt(appContext, pojo.password));
            v.put(COL_PRIVATE_KEY_PASSPHRASE, TokenCrypto.encrypt(appContext, pojo.privateKeyPassphrase));
        } catch (Exception e) {
            // fail-open fallback: avoid breaking save; you can tighten later
            v.put(COL_PASSWORD, pojo.password);
            v.put(COL_PRIVATE_KEY_PASSPHRASE, pojo.privateKeyPassphrase);
        }

        v.put(COL_ENCODING, pojo.encoding);
        v.put(COL_DISPLAY, pojo.display);
        v.put(COL_TYPE, pojo.type);
        v.put(COL_MODE, pojo.mode);
        v.put(COL_ANONYMOUS, pojo.anonymous ? 1 : 0);
        v.put(COL_USE_FTPS, pojo.useFTPS ? 1 : 0);
        v.put(COL_PRIVATE_KEY_PATH, pojo.privateKeyPath);
        v.put(COL_KNOWN_HOSTS_PATH, pojo.knownHostsPath);
        v.put(COL_BASE_PATH, pojo.basePath);
        v.put(COL_USE_HTTPS, pojo.useHTTPS ? 1 : 0);
        v.put(COL_DOMAIN, pojo.domain);
        v.put(COL_SHARE_NAME, pojo.shareName);
        v.put(COL_SMB_VERSION, pojo.smbVersion);

        return v;
    }

    private NetworkAccountPOJO createPojoFromCursor(Cursor c) {
        String host = c.getString(c.getColumnIndexOrThrow(COL_HOST));
        int port = c.getInt(c.getColumnIndexOrThrow(COL_PORT));
        String user_name = c.getString(c.getColumnIndexOrThrow(COL_USER_NAME));

        String passwordPayload = c.getString(c.getColumnIndexOrThrow(COL_PASSWORD));
        String passphrasePayload = c.getString(c.getColumnIndexOrThrow(COL_PRIVATE_KEY_PASSPHRASE));

        String password;
        String privateKeyPassphrase;
        try {
            password = TokenCrypto.decrypt(appContext, passwordPayload);
            privateKeyPassphrase = TokenCrypto.decrypt(appContext, passphrasePayload);
        } catch (Exception e) {
            // Legacy/plaintext or corrupted fallback
            password = passwordPayload;
            privateKeyPassphrase = passphrasePayload;
        }

        String encoding = c.getString(c.getColumnIndexOrThrow(COL_ENCODING));
        String display = c.getString(c.getColumnIndexOrThrow(COL_DISPLAY));
        String type = c.getString(c.getColumnIndexOrThrow(COL_TYPE));
        String mode = c.getString(c.getColumnIndexOrThrow(COL_MODE));
        boolean anonymous = c.getInt(c.getColumnIndexOrThrow(COL_ANONYMOUS)) != 0;
        boolean useFTPS = c.getInt(c.getColumnIndexOrThrow(COL_USE_FTPS)) != 0;

        String privateKeyPath = c.getString(c.getColumnIndexOrThrow(COL_PRIVATE_KEY_PATH));
        String knownHostsPath = c.getString(c.getColumnIndexOrThrow(COL_KNOWN_HOSTS_PATH));
        String basePath = c.getString(c.getColumnIndexOrThrow(COL_BASE_PATH));
        boolean useHTTPS = c.getInt(c.getColumnIndexOrThrow(COL_USE_HTTPS)) != 0;
        String domain = c.getString(c.getColumnIndexOrThrow(COL_DOMAIN));
        String shareName = c.getString(c.getColumnIndexOrThrow(COL_SHARE_NAME));
        String smbVersion = c.getString(c.getColumnIndexOrThrow(COL_SMB_VERSION));

        return new NetworkAccountPOJO(
                host, port, user_name, password,
                encoding, display, type,
                mode, anonymous, useFTPS,
                privateKeyPath, privateKeyPassphrase, knownHostsPath,
                basePath, useHTTPS,
                domain, shareName, smbVersion
        );
    }
}
