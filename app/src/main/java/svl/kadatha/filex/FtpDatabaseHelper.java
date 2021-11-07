package svl.kadatha.filex;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class FtpDatabaseHelper extends SQLiteOpenHelper {

    static final String DATABASE="FtpDetails.db";
    static final String TABLE="FtpList";

    FtpDatabaseHelper(Context context)
    {
        super(context,DATABASE,null,1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS "+TABLE+" (server TEXT PRIMARY KEY, port INTEGER, mode TEXT, user_name TEXT, password TEXT, anonymous INTEGER,encoding TEXT,display TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public List<FtpDetailsDialog.FtpPOJO> getFtpPOJOlist() {
        List<FtpDetailsDialog.FtpPOJO> ftpPOJOList = new ArrayList<>();
        SQLiteDatabase sqLiteDatabase = getReadableDatabase();
        try {
            Cursor cursor = sqLiteDatabase.query(TABLE, null, null, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                while (cursor.moveToNext()) {
                    String server = cursor.getString(0);
                    int port = cursor.getInt(1);
                    String mode = cursor.getString(2);
                    String user_name = cursor.getString(3);
                    String password = cursor.getString(4);
                    boolean anonymous = cursor.getInt(5) != 0;
                    String encoding = cursor.getString(6);
                    String display = cursor.getString(7);

                    ftpPOJOList.add(new FtpDetailsDialog.FtpPOJO(server, port, mode, user_name, password, anonymous, encoding, display));

                }
                cursor.close();
            }
        } catch (Exception e) {

        }
        return ftpPOJOList;
    }


    public long insert(String server,int port,String mode, String user_name,String password,boolean anonymous,String encoding, String display)
    {
        SQLiteDatabase sqLiteDatabase=getWritableDatabase();
        onCreate(sqLiteDatabase);
        sqLiteDatabase.delete(TABLE,"server=?",new String[]{server});
        ContentValues contentValues=new ContentValues();
        contentValues.put("server",server);
        contentValues.put("port",port);
        contentValues.put("mode",mode);
        contentValues.put("user_name",user_name);
        contentValues.put("password",password);
        contentValues.put("anonymous",anonymous ? 1 : 0);
        contentValues.put("encoding",encoding);
        contentValues.put("display",display);
        return sqLiteDatabase.insert(TABLE,null,contentValues);
    }

    public long update(String original_server, String server,int port,String mode, String user_name,String password,boolean anonymous,String encoding, String display)
    {
        SQLiteDatabase sqLiteDatabase=getWritableDatabase();
        onCreate(sqLiteDatabase);
        sqLiteDatabase.delete(TABLE,"server=?",new String[]{server});
        ContentValues contentValues=new ContentValues();
        contentValues.put("server",server);
        contentValues.put("port",port);
        contentValues.put("mode",mode);
        contentValues.put("user_name",user_name);
        contentValues.put("password",password);
        contentValues.put("anonymous",anonymous ? 1 : 0);
        contentValues.put("encoding",encoding);
        contentValues.put("display",display);
        Cursor cursor=sqLiteDatabase.query(TABLE,null,"server=?",new String[]{original_server},null,null,null);
        if(cursor!=null && cursor.getCount()>0)
        {
            cursor.close();
            return sqLiteDatabase.update(TABLE,contentValues,"server=?",new String[]{original_server});

        }
        else
        {
            return sqLiteDatabase.insert(TABLE,null,contentValues);
        }

    }

    public int delete(String server)
    {
        return getWritableDatabase().delete(TABLE,"server=?",new String[]{server});
    }

    public int change_display(String server, String new_name)
    {
        SQLiteDatabase sqLiteDatabase=getReadableDatabase();
        ContentValues contentValues=new ContentValues();
        contentValues.put("display",new_name);
        return sqLiteDatabase.update(TABLE,contentValues,"server=?",new String[]{server});
    }

    public FtpDetailsDialog.FtpPOJO getFtpPOJO(String server_string)
    {
        FtpDetailsDialog.FtpPOJO ftpPOJO = null;
        SQLiteDatabase sqLiteDatabase=getReadableDatabase();
       Cursor cursor=sqLiteDatabase.query(TABLE,null,"server=?",new String[]{server_string},null,null,null);
       if(cursor!=null && cursor.moveToFirst())
       {
           String server = cursor.getString(0);
           int port = cursor.getInt(1);
           String mode = cursor.getString(2);
           String user_name = cursor.getString(3);
           String password = cursor.getString(4);
           boolean anonymous = cursor.getInt(5) != 0;
           String encoding = cursor.getString(6);
           String display = cursor.getString(7);

           ftpPOJO=new FtpDetailsDialog.FtpPOJO(server, port, mode, user_name, password, anonymous, encoding, display);

           cursor.close();
       }
           
           return ftpPOJO;
    }
}
