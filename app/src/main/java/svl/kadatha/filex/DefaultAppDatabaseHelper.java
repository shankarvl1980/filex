package svl.kadatha.filex;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DefaultAppDatabaseHelper extends SQLiteOpenHelper {
    static final String DATABASE = "DefaultApp.db";
    static final String TABLE = "PackageList";
    private static final int VERSION = 2;
    private final Context context;

    public DefaultAppDatabaseHelper(Context context) {
        super(context, DATABASE, null, VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE + " (mime_type TEXT PRIMARY KEY,file_type TEXT, app_name TEXT, app_package_name TEXT, app_component_name TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(sqLiteDatabase);
    }

    public List<DefaultAppsDialog.DefaultAppPOJO> getDefaultAppPOJOList() {
        List<DefaultAppsDialog.DefaultAppPOJO> defaultAppPOJOS = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT DISTINCT tbl_name FROM sqlite_master WHERE tbl_name='" + TABLE + "'", null)) {
            if (c != null && c.moveToFirst()) {
                try (Cursor cursor = db.query(TABLE, null, null, null, null, null, null)) {
                    if (cursor != null && cursor.getCount() > 0) {
                        PackageManager packageManager = context.getPackageManager();
                        while (cursor.moveToNext()) {
                            try {
                                String mime_type = cursor.getString(0);
                                String file_type = cursor.getString(1);
                                String app_name = cursor.getString(2);
                                String app_package_name = cursor.getString(3);
                                String app_component_name = cursor.getString(4);

                                String file_with_package_name = app_package_name + ".png";
                                if (!Global.APK_ICON_PACKAGE_NAME_LIST.contains(file_with_package_name)) {
                                    Drawable APKicon = packageManager.getApplicationIcon(app_package_name);
                                    Bitmap bitmap;
                                    if (APKicon instanceof BitmapDrawable) {
                                        bitmap = ((BitmapDrawable) APKicon).getBitmap();
                                    } else {
                                        bitmap = Bitmap.createBitmap(APKicon.getIntrinsicWidth(), APKicon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                                        Canvas canvas = new Canvas(bitmap);
                                        APKicon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                                        APKicon.draw(canvas);
                                    }

                                    File f = new File(Global.APK_ICON_DIR, file_with_package_name);
                                    FileOutputStream fileOutputStream = null;
                                    try {
                                        fileOutputStream = new FileOutputStream(f);
                                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
                                        fileOutputStream.close();
                                        Global.APK_ICON_PACKAGE_NAME_LIST.add(file_with_package_name);
                                    } catch (IOException e) {
                                        if (fileOutputStream != null) {
                                            try {
                                                fileOutputStream.close();
                                            } catch (IOException ioException) {

                                            }
                                        }
                                    }

                                }
                                defaultAppPOJOS.add(new DefaultAppsDialog.DefaultAppPOJO(mime_type, file_type, app_name, app_package_name, app_component_name));
                            } catch (PackageManager.NameNotFoundException |
                                     IllegalStateException e) {

                            }

                        }
                    }
                }
            }

        }

        return defaultAppPOJOS;
    }

    public String getPackageName(String mime_type) {
        String app_package_name = null;
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT DISTINCT tbl_name FROM sqlite_master WHERE tbl_name='" + TABLE + "'", null)) {
            if (c != null && c.moveToFirst()) {
                try (Cursor cursor = db.query(TABLE, new String[]{"app_package_name"}, "mime_type=?", new String[]{mime_type}, null, null, null)) {
                    if (cursor.moveToFirst()) {
                        app_package_name = cursor.getString(0);
                    }
                }
            }
        } catch (SQLiteException e) {
        }
        return app_package_name;
    }

    public String getComponentName(String mime_type) {
        String app_component_name = null;
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT DISTINCT tbl_name FROM sqlite_master WHERE tbl_name='" + TABLE + "'", null)) {
            if (c != null && c.moveToFirst()) {
                try (Cursor cursor = db.query(TABLE, new String[]{"app_component_name"}, "mime_type=?", new String[]{mime_type}, null, null, null)) {
                    if (cursor.moveToFirst()) {
                        app_component_name = cursor.getString(0);
                    }
                }
            }
        } catch (SQLiteException e) {
        }
        return app_component_name;
    }

    public void delete_row(String mime_type) {
        if (mime_type == null) {
            return;
        }
        getWritableDatabase().delete(TABLE, "mime_type=?", new String[]{mime_type});
    }


    public long insert_row(String mime_type, String file_type, String app_name, String app_package_name, String app_component_name) {
        if (mime_type == null || file_type == null || app_name == null || app_package_name == null || app_component_name == null) {
            return 0;
        }
        SQLiteDatabase db = getWritableDatabase();
        onCreate(db);
        ContentValues contentValues = new ContentValues();
        contentValues.put("mime_type", mime_type);
        contentValues.put("file_type", file_type);
        contentValues.put("app_name", app_name);
        contentValues.put("app_package_name", app_package_name);
        contentValues.put("app_component_name", app_component_name);
        return db.insertWithOnConflict(TABLE, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void deleteTable() {
        getWritableDatabase().execSQL("DROP TABLE IF EXISTS " + TABLE);

    }
}
