package svl.kadatha.filex;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import java.util.ArrayList;
import java.util.List;

public class AudioDatabaseHelper {
    private static final String DATABASE_NAME = "AudioSavedLists.db";
    private static final int DATABASE_VERSION = 1;
    // Column Names
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_DATA = "data";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_ALBUM_ID = "album_id";
    private static final String COLUMN_ALBUM = "album";
    private static final String COLUMN_ARTIST = "artist";
    private static final String COLUMN_DURATION = "duration";
    private final Context context;
    private final SQLiteDatabase db;

    /**
     * Constructor that initializes the database.
     *
     * @param context The application context.
     * @throws SQLException If the database cannot be opened or created.
     */
    AudioDatabaseHelper(Context context) throws SQLException {
        this.context = context;
        // Open or create the database with proper error handling
        db = context.openOrCreateDatabase(DATABASE_NAME, Context.MODE_PRIVATE, null);
    }

    /**
     * Validates the table name to prevent SQL injection.
     * Allows only alphanumeric characters and underscores.
     *
     * @param table The table name to validate.
     * @return True if valid, False otherwise.
     */
    private boolean isValidTableName(String table) {
        return table != null && table.matches("^[a-zA-Z0-9_]+$");
    }

    /**
     * Creates a table if it doesn't exist. Table names are validated.
     *
     * @param table The name of the table to create.
     * @throws IllegalArgumentException If the table name is invalid.
     */
    public void createTable(String table) {
        if (!isValidTableName(table)) {
            throw new IllegalArgumentException("Invalid table name: " + table);
        }
        String createTableSQL = String.format(
                "CREATE TABLE IF NOT EXISTS `%s` (%s INTEGER, %s TEXT, %s TEXT, %s TEXT, %s TEXT, %s TEXT, %s TEXT)",
                table, COLUMN_ID, COLUMN_DATA, COLUMN_TITLE, COLUMN_ALBUM_ID, COLUMN_ALBUM, COLUMN_ARTIST, COLUMN_DURATION
        );
        try {
            db.execSQL(createTableSQL);
        } catch (SQLiteException e) {
            // Optionally, handle the error (e.g., notify the user or retry)
        }
    }

    /**
     * Inserts a single AudioPOJO into the specified table.
     *
     * @param table The table name.
     * @param audio The AudioPOJO object to insert.
     * @return The row ID of the newly inserted row, or -1 if an error occurred.
     */
    public long insert(String table, AudioPOJO audio) {
        if (!isValidTableName(table)) {
            throw new IllegalArgumentException("Invalid table name: " + table);
        }

        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_ID, audio.getId());
        contentValues.put(COLUMN_DATA, audio.getData());
        contentValues.put(COLUMN_TITLE, audio.getTitle());
        contentValues.put(COLUMN_ALBUM_ID, audio.getAlbumId());
        contentValues.put(COLUMN_ALBUM, audio.getAlbum());
        contentValues.put(COLUMN_ARTIST, audio.getArtist());
        contentValues.put(COLUMN_DURATION, audio.getDuration());

        try {
            return db.insertOrThrow(table, null, contentValues);
        } catch (SQLiteException e) {
            // Optionally, handle the error (e.g., log it or notify the user)
            return -1;
        }
    }

    /**
     * Inserts a list of AudioPOJO objects into the specified table using a transaction for efficiency.
     *
     * @param table     The table name.
     * @param audioList The list of AudioPOJO objects to insert.
     */
    public void insert(String table, List<AudioPOJO> audioList) {
        if (!isValidTableName(table)) {
            throw new IllegalArgumentException("Invalid table name: " + table);
        }

        db.beginTransaction();
        try {
            for (AudioPOJO audio : audioList) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(COLUMN_ID, audio.getId());
                contentValues.put(COLUMN_DATA, audio.getData());
                contentValues.put(COLUMN_TITLE, audio.getTitle());
                contentValues.put(COLUMN_ALBUM_ID, audio.getAlbumId());
                contentValues.put(COLUMN_ALBUM, audio.getAlbum());
                contentValues.put(COLUMN_ARTIST, audio.getArtist());
                contentValues.put(COLUMN_DURATION, audio.getDuration());

                db.insertOrThrow(table, null, contentValues);
            }
            db.setTransactionSuccessful();
        } catch (SQLiteException e) {
            // Optionally, handle the error (e.g., log it or notify the user)
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Deletes a record from the table based on the audio ID.
     *
     * @param table The table name.
     * @param id    The audio ID to delete.
     * @return The number of rows affected.
     */
    public int deleteByAudioId(String table, long id) {
        if (!isValidTableName(table)) {
            throw new IllegalArgumentException("Invalid table name: " + table);
        }

        try {
            return db.delete(table, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        } catch (SQLiteException e) {
            // Optionally, handle the error
            return 0;
        }
    }

    /**
     * Deletes multiple records from the table based on a list of row IDs using a transaction.
     *
     * @param table          The table name.
     * @param audioRowIdList The list of row IDs to delete.
     */
    public void deleteByRowId(String table, List<Long> audioRowIdList) {
        if (!isValidTableName(table)) {
            throw new IllegalArgumentException("Invalid table name: " + table);
        }

        if (audioRowIdList == null || audioRowIdList.isEmpty()) {
            return;
        }

        db.beginTransaction();
        try {
            // Using the IN clause to delete multiple records at once
            StringBuilder whereClause = new StringBuilder("rowid IN (");
            for (int i = 0; i < audioRowIdList.size(); i++) {
                whereClause.append("?");
                if (i < audioRowIdList.size() - 1) {
                    whereClause.append(", ");
                }
            }
            whereClause.append(")");

            // Convert the list to an array of Strings
            String[] whereArgs = new String[audioRowIdList.size()];
            for (int i = 0; i < audioRowIdList.size(); i++) {
                whereArgs[i] = String.valueOf(audioRowIdList.get(i));
            }

            db.delete(table, whereClause.toString(), whereArgs);
            db.setTransactionSuccessful();
        } catch (SQLiteException e) {
            // Optionally, handle the error
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Retrieves the underlying SQLiteDatabase instance.
     *
     * @return The SQLiteDatabase instance.
     */
    public SQLiteDatabase getDatabase() {
        return db;
    }

    /**
     * Drops the specified table if it exists.
     *
     * @param table The table name.
     */
    public void deleteTable(String table) {
        if (!isValidTableName(table)) {
            throw new IllegalArgumentException("Invalid table name: " + table);
        }

        String dropTableSQL = String.format("DROP TABLE IF EXISTS `%s`", table);
        try {
            db.execSQL(dropTableSQL);
        } catch (SQLiteException e) {
            // Optionally, handle the error
        }
    }

    /**
     * Retrieves all table names excluding 'android_metadata'.
     *
     * @return A list of table names.
     */
    public ArrayList<String> getTables() {
        ArrayList<String> tables = new ArrayList<>();
        String query = "SELECT name FROM sqlite_master WHERE type='table' AND name!='android_metadata'";
        try (Cursor cursor = db.rawQuery(query, null)) {
            if (cursor.moveToFirst()) {
                do {
                    String tableName = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                    tables.add(tableName);
                } while (cursor.moveToNext());
            }
        } catch (SQLiteException e) {
            Global.print(context, "Exception thrown: could not extract entries");
        }
        return tables;
    }

    /**
     * Retrieves a list of AudioPOJO objects from the specified table.
     *
     * @param table The table name.
     * @return An IndexedLinkedHashMap mapping row IDs to AudioPOJO objects.
     */
    public IndexedLinkedHashMap<Long, AudioPOJO> getAudioList(String table) {
        IndexedLinkedHashMap<Long, AudioPOJO> audioList = new IndexedLinkedHashMap<>();
        if (!isValidTableName(table)) {
            throw new IllegalArgumentException("Invalid table name: " + table);
        }

        String query = String.format("SELECT *, rowid FROM `%s`", table);
        try (Cursor cursor = db.rawQuery(query, null)) {
            if (cursor.moveToFirst()) {
                // Get column indices
                int rowIdIndex = cursor.getColumnIndexOrThrow("rowid");
                int idIndex = cursor.getColumnIndexOrThrow(COLUMN_ID);
                int dataIndex = cursor.getColumnIndexOrThrow(COLUMN_DATA);
                int titleIndex = cursor.getColumnIndexOrThrow(COLUMN_TITLE);
                int albumIdIndex = cursor.getColumnIndexOrThrow(COLUMN_ALBUM_ID);
                int albumIndex = cursor.getColumnIndexOrThrow(COLUMN_ALBUM);
                int artistIndex = cursor.getColumnIndexOrThrow(COLUMN_ARTIST);
                int durationIndex = cursor.getColumnIndexOrThrow(COLUMN_DURATION);

                do {
                    long rowId = cursor.getLong(rowIdIndex);
                    long id = cursor.getLong(idIndex);
                    String data = cursor.getString(dataIndex);
                    String title = cursor.getString(titleIndex);
                    String albumId = cursor.getString(albumIdIndex);
                    String album = cursor.getString(albumIndex);
                    String artist = cursor.getString(artistIndex);
                    String duration = cursor.getString(durationIndex);

                    AudioPOJO audio = new AudioPOJO(id, data, title, albumId, album, artist, duration, FileObjectType.FILE_TYPE);
                    audioList.put(rowId, audio);
                } while (cursor.moveToNext());
            }
        } catch (SQLiteException e) {

        }

        return audioList;
    }

    /**
     * Closes the database connection. Should be called when the helper is no longer needed.
     */
    public void closeDatabase() {
        if (db != null && db.isOpen()) {
            db.close();
        }
    }
}
