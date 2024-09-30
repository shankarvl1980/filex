package svl.kadatha.filex;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class NetworkAccountsDatabaseHelper extends SQLiteOpenHelper {

    static final String DATABASE = "NetworkAccountsDetails.db";
    static final String TABLE = "NetworkAccountsList";
    private static final int DATABASE_VERSION = 1;

    public NetworkAccountsDatabaseHelper(Context context) {
        super(context, DATABASE, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create table with all necessary fields including new ones
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
                "host TEXT, " +
                "port INTEGER, " +
                "user_name TEXT, " +
                "password TEXT, " +
                "encoding TEXT, " +
                "display TEXT, " +
                "type TEXT, " +
                "mode TEXT, " +
                "anonymous INTEGER, " +
                "useFTPS INTEGER, " +
                "privateKeyPath TEXT, " +
                "privateKeyPassphrase TEXT, " +
                "knownHostsPath TEXT, " +
                "basePath TEXT, " +
                "useHTTPS INTEGER, " +
                "domain TEXT, " +
                "shareName TEXT, " +
                "smbVersion TEXT, " +
                "PRIMARY KEY (host, port, user_name, type)" +
                ")");

        // Create an index for faster lookups
        db.execSQL("CREATE INDEX idx_server_port_user_type ON " + TABLE +
                " (host, port, user_name, type)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This method will be implemented in future versions when needed
    }

    // Insert method
    public long insert(NetworkAccountsDetailsDialog.NetworkAccountPOJO pojo) {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        // Delete existing record if any
        sqLiteDatabase.delete(TABLE, "host=? AND port=? AND user_name=? AND type=?",
                new String[]{pojo.host, String.valueOf(pojo.port), pojo.user_name, pojo.type});
        ContentValues contentValues = createContentValues(pojo);
        return sqLiteDatabase.insert(TABLE, null, contentValues);
    }

    // UpdateOrInsert method
    public long updateOrInsert(String original_host, int original_port, String original_user_name, String original_type,
                               NetworkAccountsDetailsDialog.NetworkAccountPOJO pojo) {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        ContentValues contentValues = createContentValues(pojo);

        // Attempt to update the existing record
        int rowsAffected = sqLiteDatabase.update(TABLE, contentValues,
                "host=? AND port=? AND user_name=? AND type=?",
                new String[]{original_host, String.valueOf(original_port), original_user_name, original_type});

        if (rowsAffected == 0) {
            // No existing record found; insert a new one
            try {
                return sqLiteDatabase.insert(TABLE, null, contentValues);
            } catch (SQLiteException e) {
                e.printStackTrace();
                return -1; // Indicate failure
            }
        } else {
            // Update was successful; return the number of rows updated
            return rowsAffected;
        }
    }


    // Delete method
    public int delete(String host, int port, String user_name, String type) {
        return getWritableDatabase().delete(TABLE, "host=? AND port=? AND user_name=? AND type=?",
                new String[]{host, String.valueOf(port), user_name, type});
    }

    // Change display name method
    public int change_display(String host, int port, String user_name, String new_display_name, String type) {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("display", new_display_name);
        return sqLiteDatabase.update(TABLE, contentValues,
                "host=? AND port=? AND user_name=? AND type=?",
                new String[]{host, String.valueOf(port), user_name, type});
    }

    // Get NetworkAccountPOJO list by type
    public List<NetworkAccountsDetailsDialog.NetworkAccountPOJO> getNetworkAccountPOJOList(String type) {
        List<NetworkAccountsDetailsDialog.NetworkAccountPOJO> pojoList = new ArrayList<>();
        SQLiteDatabase sqLiteDatabase = getReadableDatabase();
        try {
            String selection = "type = ?";
            String[] selectionArgs = {type};
            Cursor cursor = sqLiteDatabase.query(TABLE, null, selection, selectionArgs, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    NetworkAccountsDetailsDialog.NetworkAccountPOJO pojo = createPojoFromCursor(cursor);
                    pojoList.add(pojo);
                } while (cursor.moveToNext());
                cursor.close();
            }
        } catch (Exception e) {
            String msg = e.getMessage();
        }
        return pojoList;
    }

    // Get NetworkAccountPOJO
    public NetworkAccountsDetailsDialog.NetworkAccountPOJO getNetworkAccountPOJO(String host, int port, String user_name, String type) {
        NetworkAccountsDetailsDialog.NetworkAccountPOJO pojo = null;
        SQLiteDatabase sqLiteDatabase = getReadableDatabase();
        Cursor cursor = sqLiteDatabase.query(TABLE, null,
                "host=? AND port=? AND user_name=? AND type=?",
                new String[]{host, String.valueOf(port), user_name, type},
                null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            pojo = createPojoFromCursor(cursor);
            cursor.close();
        }
        return pojo;
    }

    // Helper method to create ContentValues from POJO
    private ContentValues createContentValues(NetworkAccountsDetailsDialog.NetworkAccountPOJO pojo) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("host", pojo.host);
        contentValues.put("port", pojo.port);
        contentValues.put("user_name", pojo.user_name);
        contentValues.put("password", pojo.password);
        contentValues.put("encoding", pojo.encoding);
        contentValues.put("display", pojo.display);
        contentValues.put("type", pojo.type);
        contentValues.put("mode", pojo.mode);
        contentValues.put("anonymous", pojo.anonymous ? 1 : 0);
        contentValues.put("useFTPS", pojo.useFTPS ? 1 : 0);
        contentValues.put("privateKeyPath", pojo.privateKeyPath);
        contentValues.put("privateKeyPassphrase", pojo.privateKeyPassphrase);
        contentValues.put("knownHostsPath", pojo.knownHostsPath);
        contentValues.put("basePath", pojo.basePath);
        contentValues.put("useHTTPS", pojo.useHTTPS ? 1 : 0);
        contentValues.put("domain", pojo.domain);
        contentValues.put("shareName", pojo.shareName);
        contentValues.put("smbVersion", pojo.smbVersion);
        return contentValues;
    }

    // Helper method to create POJO from Cursor
    private NetworkAccountsDetailsDialog.NetworkAccountPOJO createPojoFromCursor(Cursor cursor) {
        String host = cursor.getString(cursor.getColumnIndex("host"));
        int port = cursor.getInt(cursor.getColumnIndex("port"));
        String user_name = cursor.getString(cursor.getColumnIndex("user_name"));
        String password = cursor.getString(cursor.getColumnIndex("password"));
        String encoding = cursor.getString(cursor.getColumnIndex("encoding"));
        String display = cursor.getString(cursor.getColumnIndex("display"));
        String type = cursor.getString(cursor.getColumnIndex("type"));
        String mode = cursor.getString(cursor.getColumnIndex("mode"));
        boolean anonymous = cursor.getInt(cursor.getColumnIndex("anonymous")) != 0;
        boolean useFTPS = cursor.getInt(cursor.getColumnIndex("useFTPS")) != 0;
        String privateKeyPath = cursor.getString(cursor.getColumnIndex("privateKeyPath"));
        String privateKeyPassphrase = cursor.getString(cursor.getColumnIndex("privateKeyPassphrase"));
        String knownHostsPath = cursor.getString(cursor.getColumnIndex("knownHostsPath"));
        String basePath = cursor.getString(cursor.getColumnIndex("basePath"));
        boolean useHTTPS = cursor.getInt(cursor.getColumnIndex("useHTTPS")) != 0;
        String domain = cursor.getString(cursor.getColumnIndex("domain"));
        String shareName = cursor.getString(cursor.getColumnIndex("shareName"));
        String smbVersion = cursor.getString(cursor.getColumnIndex("smbVersion"));

        return new NetworkAccountsDetailsDialog.NetworkAccountPOJO(
                host, port, user_name, password,
                encoding, display, type,
                mode, anonymous, useFTPS,
                privateKeyPath, privateKeyPassphrase, knownHostsPath,
                basePath, useHTTPS,
                domain, shareName, smbVersion
        );
    }
}