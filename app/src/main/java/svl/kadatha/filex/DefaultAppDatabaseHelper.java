package svl.kadatha.filex;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.List;

public class DefaultAppDatabaseHelper extends SQLiteOpenHelper
{
    static final String DATABASE="DefaultApp.db";
    static final String TABLE="PackageList";
    private final Context context;
    public DefaultAppDatabaseHelper(Context context) {
        super(context, DATABASE, null, 1);
        this.context=context;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS "+TABLE+" (mime_type TEXT,file_type TEXT, app_name TEXT, app_package_name TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    public List<DefaultAppsDialog.DefaultAppPOJO> getDefaultAppPOJOList()
    {
        List<DefaultAppsDialog.DefaultAppPOJO> defaultAppPOJOS=new ArrayList<>();
        SQLiteDatabase db=getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT DISTINCT tbl_name FROM sqlite_master WHERE tbl_name='"+TABLE+"'", null))
        {
            if (c != null && c.moveToFirst())
            {
                Cursor cursor=db.query(TABLE,null,null,null,null,null,null);
                if(cursor!=null && cursor.getCount()>0)
                {
                    PackageManager packageManager=context.getPackageManager();
                    while(cursor.moveToNext())
                    {
                        String mime_type=cursor.getString(0);
                        String file_type=cursor.getString(1);
                        String app_name=cursor.getString(2);
                        String app_package_name=cursor.getString(3);
                        Drawable app_icon=null;
                        try {
                            app_icon=packageManager.getApplicationIcon(app_package_name);
                        } catch (PackageManager.NameNotFoundException e) {

                        }
                        defaultAppPOJOS.add(new DefaultAppsDialog.DefaultAppPOJO(mime_type,file_type,app_icon,app_name,app_package_name));
                    }
                }
                cursor.close();
            }

        }

        return defaultAppPOJOS;
    }
    public String getPackageName(String mime_type)
    {
        String app_package_name=null;
        SQLiteDatabase db=getReadableDatabase();
        //try(Cursor c=db.rawQuery("SELECT name FROM sqlite_master WHERE TYPE='table' AND name!='android_metadata'",null))
        try (Cursor c = db.rawQuery("SELECT DISTINCT tbl_name FROM sqlite_master WHERE tbl_name='"+TABLE+"'", null))
        {
            if (c != null && c.moveToFirst()) {
                try (Cursor cursor = db.query(TABLE, new String[]{"app_package_name"}, "mime_type=?", new String[]{mime_type}, null, null, null)) {
                    if (cursor.moveToFirst()) {
                        app_package_name = cursor.getString(0);
                    }
                }
            }
        }
        return app_package_name;
    }

    public void delete_row(String mime_type)
    {
        if(mime_type==null)
        {
            return;
        }
        getWritableDatabase().delete(TABLE,"mime_type=?",new String []{mime_type});
    }


    public long insert_row(String mime_type, String file_type, String app_name, String app_package_name)
    {
        SQLiteDatabase db=getWritableDatabase();
        onCreate(db);
        ContentValues contentValues=new ContentValues();
        contentValues.put("mime_type",mime_type);
        contentValues.put("file_type",file_type);
        contentValues.put("app_name",app_name);
        contentValues.put("app_package_name",app_package_name);
        return db.insert(TABLE,null,contentValues);
    }

    public void deleteTable()
    {
        getWritableDatabase().execSQL("DROP TABLE IF EXISTS "+TABLE);

    }
}
