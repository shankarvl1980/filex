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

    public CloudAccountsDatabaseHelper(Context context) {
        super(context, DATABASE, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create table with composite primary key (type, user_id)
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
                "type TEXT, " +
                "user_id TEXT, " +
                "display_name TEXT, " +
                "access_token TEXT, " +
                "refresh_token TEXT, " +
                "token_expiry_time INTEGER, " +
                "scopes TEXT, " +
                "extra1 TEXT, " +
                "extra2 TEXT, " +
                "extra3 TEXT, " +
                "PRIMARY KEY (type, user_id)" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    // Insert method
    public long insert(CloudAccountPOJO pojo) {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        // Delete existing record if any
        sqLiteDatabase.delete(TABLE, "type=? AND user_id=?",
                new String[]{pojo.type, pojo.userId});
        ContentValues contentValues = createContentValues(pojo);
        return sqLiteDatabase.insert(TABLE, null, contentValues);
    }

    // Update or Insert method
    public long updateOrInsert(String original_type, String original_user_id, CloudAccountPOJO pojo) {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        ContentValues contentValues = createContentValues(pojo);

        // Attempt to update the existing record
        int rowsAffected = sqLiteDatabase.update(TABLE, contentValues,
                "type=? AND user_id=?",
                new String[]{original_type, original_user_id});

        if (rowsAffected == 0) {
            // No existing record found; insert a new one
            return sqLiteDatabase.insert(TABLE, null, contentValues);
        } else {
            // Update was successful; return the number of rows updated
            return rowsAffected;
        }
    }

    // Delete method
    public int delete(String type, String user_id) {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        return sqLiteDatabase.delete(TABLE,
                "type=? AND user_id=?",
                new String[]{type, user_id});
    }

    // Change display name method
    public int changeDisplayName(String type, String user_id, String new_display_name) {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("display_name", new_display_name);
        return sqLiteDatabase.update(TABLE, contentValues,
                "type=? AND user_id=?",
                new String[]{type, user_id});
    }

    // Get list of CloudAccountPOJO by type
    public List<CloudAccountPOJO> getCloudAccountList(String type) {
        List<CloudAccountPOJO> pojoList = new ArrayList<>();
        SQLiteDatabase sqLiteDatabase = getReadableDatabase();
        String selection = "type = ?";
        String[] selectionArgs = {type};
        Cursor cursor = sqLiteDatabase.query(TABLE, null, selection, selectionArgs, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                CloudAccountPOJO pojo = createPojoFromCursor(cursor);
                pojoList.add(pojo);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return pojoList;
    }

    // Get a specific CloudAccountPOJO
    public CloudAccountPOJO getCloudAccount(String type, String user_id) {
        CloudAccountPOJO pojo = null;
        SQLiteDatabase sqLiteDatabase = getReadableDatabase();
        Cursor cursor = sqLiteDatabase.query(TABLE, null,
                "type=? AND user_id=?",
                new String[]{type, user_id},
                null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            pojo = createPojoFromCursor(cursor);
            cursor.close();
        }
        return pojo;
    }

    // Helper method to create ContentValues from POJO
    private ContentValues createContentValues(CloudAccountPOJO pojo) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("type", pojo.type);
        contentValues.put("user_id", pojo.userId);
        contentValues.put("display_name", pojo.displayName);
        contentValues.put("access_token", pojo.accessToken);
        contentValues.put("refresh_token", pojo.refreshToken);
        contentValues.put("token_expiry_time", pojo.tokenExpiryTime);
        contentValues.put("scopes", pojo.scopes);
        contentValues.put("extra1", pojo.extra1);
        contentValues.put("extra2", pojo.extra2);
        contentValues.put("extra3", pojo.extra3);
        return contentValues;
    }

    // Helper method to create POJO from Cursor
    private CloudAccountPOJO createPojoFromCursor(Cursor cursor) {
        String type = cursor.getString(cursor.getColumnIndex("type"));
        String userId = cursor.getString(cursor.getColumnIndex("user_id"));
        String displayName = cursor.getString(cursor.getColumnIndex("display_name"));
        String accessToken = cursor.getString(cursor.getColumnIndex("access_token"));
        String refreshToken = cursor.getString(cursor.getColumnIndex("refresh_token"));
        long tokenExpiryTime = cursor.getLong(cursor.getColumnIndex("token_expiry_time"));
        String scopes = cursor.getString(cursor.getColumnIndex("scopes"));
        String extra1 = cursor.getString(cursor.getColumnIndex("extra1"));
        String extra2 = cursor.getString(cursor.getColumnIndex("extra2"));
        String extra3 = cursor.getString(cursor.getColumnIndex("extra3"));

        return new CloudAccountPOJO(
                type, displayName, userId,
                accessToken, refreshToken, tokenExpiryTime,
                scopes, extra1, extra2, extra3
        );
    }
}
