package svl.kadatha.filex.cloud;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class CloudAccountsDatabaseHelper extends SQLiteOpenHelper {

    static final String DATABASE = "CloudAccountsDetails.db";
    static final String TABLE = "CloudAccountsList";
    private static final int DATABASE_VERSION = 1;

    // Column names (single source of truth)
    private static final String COL_TYPE = "type";
    private static final String COL_USER_ID = "user_id";
    private static final String COL_DISPLAY_NAME = "display_name";
    private static final String COL_ACCESS_TOKEN = "access_token";
    private static final String COL_REFRESH_TOKEN = "refresh_token";
    private static final String COL_TOKEN_EXPIRY_TIME = "token_expiry_time";
    private static final String COL_SCOPES = "scopes";
    private static final String COL_EXTRA1 = "extra1";
    private static final String COL_EXTRA2 = "extra2";
    private static final String COL_EXTRA3 = "extra3";

    private final Context appContext;

    public CloudAccountsDatabaseHelper(Context context) {
        super(context, DATABASE, null, DATABASE_VERSION);
        this.appContext = context.getApplicationContext();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Composite primary key (type, user_id)
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
                COL_TYPE + " TEXT, " +
                COL_USER_ID + " TEXT, " +
                COL_DISPLAY_NAME + " TEXT, " +
                COL_ACCESS_TOKEN + " TEXT, " +
                COL_REFRESH_TOKEN + " TEXT, " +
                COL_TOKEN_EXPIRY_TIME + " INTEGER, " +
                COL_SCOPES + " TEXT, " +
                COL_EXTRA1 + " TEXT, " +
                COL_EXTRA2 + " TEXT, " +
                COL_EXTRA3 + " TEXT, " +
                "PRIMARY KEY (" + COL_TYPE + ", " + COL_USER_ID + ")" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // If you later bump DATABASE_VERSION, implement migrations here.
    }

    // Insert
    public long insert(CloudAccountPOJO pojo) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE, COL_TYPE + "=? AND " + COL_USER_ID + "=?",
                new String[]{pojo.type, pojo.userId});
        ContentValues values = createContentValues(pojo);
        return db.insert(TABLE, null, values);
    }

    // Update or Insert
    public long updateOrInsert(String original_type, String original_user_id, CloudAccountPOJO pojo) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = createContentValues(pojo);

        int rowsAffected = db.update(TABLE, values,
                COL_TYPE + "=? AND " + COL_USER_ID + "=?",
                new String[]{original_type, original_user_id});

        if (rowsAffected == 0) {
            return db.insert(TABLE, null, values);
        }
        return rowsAffected;
    }

    // Delete
    public int delete(String type, String user_id) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE,
                COL_TYPE + "=? AND " + COL_USER_ID + "=?",
                new String[]{type, user_id});
    }

    // Change display name
    public int changeDisplayName(String type, String user_id, String new_display_name) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_DISPLAY_NAME, new_display_name);
        return db.update(TABLE, values,
                COL_TYPE + "=? AND " + COL_USER_ID + "=?",
                new String[]{type, user_id});
    }

    public List<CloudAccountPOJO> getAllCloudAccountList() {
        List<CloudAccountPOJO> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        try (Cursor c = db.query(TABLE, null, null, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                do {
                    list.add(createPojoFromCursor(c));
                } while (c.moveToNext());
            }
        }
        return list;
    }

    // Get list by type
    public List<CloudAccountPOJO> getCloudAccountList(String type) {
        List<CloudAccountPOJO> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        String selection = COL_TYPE + " = ?";
        String[] args = {type};

        try (Cursor c = db.query(TABLE, null, selection, args, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                do {
                    list.add(createPojoFromCursor(c));
                } while (c.moveToNext());
            }
        }
        return list;
    }

    // Get a specific account
    public CloudAccountPOJO getCloudAccount(String type, String user_id) {
        SQLiteDatabase db = getReadableDatabase();

        try (Cursor c = db.query(TABLE, null,
                COL_TYPE + "=? AND " + COL_USER_ID + "=?",
                new String[]{type, user_id},
                null, null, null)) {

            if (c != null && c.moveToFirst()) {
                return createPojoFromCursor(c);
            }
        }
        return null;
    }

    // -------------------- helpers --------------------

    private ContentValues createContentValues(CloudAccountPOJO pojo) {
        ContentValues values = new ContentValues();

        values.put(COL_TYPE, pojo.type);
        values.put(COL_USER_ID, pojo.userId);
        values.put(COL_DISPLAY_NAME, pojo.displayName);

        // Encrypt tokens at rest (TEXT + base64 payload)
        try {
            values.put(COL_ACCESS_TOKEN, TokenCrypto.encrypt(appContext, pojo.accessToken));
            values.put(COL_REFRESH_TOKEN, TokenCrypto.encrypt(appContext, pojo.refreshToken));
        } catch (Exception e) {
            // Fallback: do not crash login flow. You can tighten this later to "fail closed".
            values.put(COL_ACCESS_TOKEN, pojo.accessToken);
            values.put(COL_REFRESH_TOKEN, pojo.refreshToken);
        }

        values.put(COL_TOKEN_EXPIRY_TIME, pojo.tokenExpiryTime);
        values.put(COL_SCOPES, pojo.scopes);
        values.put(COL_EXTRA1, pojo.extra1);
        values.put(COL_EXTRA2, pojo.extra2);
        values.put(COL_EXTRA3, pojo.extra3);

        return values;
    }

    private CloudAccountPOJO createPojoFromCursor(Cursor c) {
        String type = c.getString(c.getColumnIndexOrThrow(COL_TYPE));
        String userId = c.getString(c.getColumnIndexOrThrow(COL_USER_ID));
        String displayName = c.getString(c.getColumnIndexOrThrow(COL_DISPLAY_NAME));

        String accessPayload = c.getString(c.getColumnIndexOrThrow(COL_ACCESS_TOKEN));
        String refreshPayload = c.getString(c.getColumnIndexOrThrow(COL_REFRESH_TOKEN));

        String accessToken;
        String refreshToken;
        try {
            accessToken = TokenCrypto.decrypt(appContext, accessPayload);
            refreshToken = TokenCrypto.decrypt(appContext, refreshPayload);
        } catch (Exception e) {
            // Legacy/plaintext or corrupted values fallback
            accessToken = accessPayload;
            refreshToken = refreshPayload;
        }

        long tokenExpiryTime = c.getLong(c.getColumnIndexOrThrow(COL_TOKEN_EXPIRY_TIME));
        String scopes = c.getString(c.getColumnIndexOrThrow(COL_SCOPES));
        String extra1 = c.getString(c.getColumnIndexOrThrow(COL_EXTRA1));
        String extra2 = c.getString(c.getColumnIndexOrThrow(COL_EXTRA2));
        String extra3 = c.getString(c.getColumnIndexOrThrow(COL_EXTRA3));

        return new CloudAccountPOJO(
                type, displayName, userId,
                accessToken, refreshToken, tokenExpiryTime,
                scopes, extra1, extra2, extra3
        );
    }
}
