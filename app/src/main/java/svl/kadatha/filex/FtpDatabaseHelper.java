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
        db.execSQL("CREATE TABLE IF NOT EXISTS "+TABLE+" (server TEXT, port INTEGER, mode TEXT, user_name TEXT, password TEXT, type TEXT ,anonymous INTEGER,encoding TEXT,display TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public List<FtpDetailsDialog.FtpPOJO> getFtpPOJOlist(String type_) {
        List<FtpDetailsDialog.FtpPOJO> ftpPOJOList = new ArrayList<>();
        SQLiteDatabase sqLiteDatabase = getReadableDatabase();
        try {
            String selection = "type = ?";
            String[] selectionArgs = { type_ };
            Cursor cursor = sqLiteDatabase.query(TABLE, null, selection, selectionArgs, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {

                while (!cursor.isAfterLast()) {
                    String server = cursor.getString(0);
                    int port = cursor.getInt(1);
                    String mode = cursor.getString(2);
                    String user_name = cursor.getString(3);
                    String password = cursor.getString(4);
                    String type = cursor.getString(5);
                    boolean anonymous = cursor.getInt(6) != 0;
                    String encoding = cursor.getString(7);
                    String display = cursor.getString(8);

                    ftpPOJOList.add(new FtpDetailsDialog.FtpPOJO(server, port, mode, user_name, password, type, anonymous, encoding, display));
                    cursor.moveToNext();
                }
                cursor.close();
            }
        } catch (Exception e) {

        }
        return ftpPOJOList;
    }


    public long insert(String server,int port,String mode, String user_name,String password,String type,boolean anonymous,String encoding, String display)
    {
        SQLiteDatabase sqLiteDatabase=getWritableDatabase();
        onCreate(sqLiteDatabase);
        sqLiteDatabase.delete(TABLE,"server=?"+" AND "+"user_name=?"+" AND "+"type=?",new String[]{server,user_name,type});
        ContentValues contentValues=new ContentValues();
        contentValues.put("server",server);
        contentValues.put("port",port);
        contentValues.put("mode",mode);
        contentValues.put("user_name",user_name);
        contentValues.put("password",password);
        contentValues.put("type",type);
        contentValues.put("anonymous",anonymous ? 1 : 0);
        contentValues.put("encoding",encoding);
        contentValues.put("display",display);
        return sqLiteDatabase.insert(TABLE,null,contentValues);
    }

    public long update(String original_server,String original_user_name, String server,int port,String mode, String user_name,String password,String type,boolean anonymous,String encoding, String display)
    {
        SQLiteDatabase sqLiteDatabase=getWritableDatabase();
        onCreate(sqLiteDatabase);
        ContentValues contentValues=new ContentValues();
        contentValues.put("server",server);
        contentValues.put("port",port);
        contentValues.put("mode",mode);
        contentValues.put("user_name",user_name);
        contentValues.put("password",password);
        contentValues.put("type",type);
        contentValues.put("anonymous",anonymous ? 1 : 0);
        contentValues.put("encoding",encoding);
        contentValues.put("display",display);
        Cursor cursor=sqLiteDatabase.query(TABLE,null,"server=?"+" AND "+"user_name=?"+" AND "+"type=?",new String[]{original_server,original_user_name,type},null,null,null);
        if(cursor!=null && cursor.getCount()>0)
        {
            cursor.close();
            return sqLiteDatabase.update(TABLE,contentValues,"server=?"+" AND "+"user_name=?"+" AND "+"type=?",new String[]{original_server,original_user_name,type});
        }
        else
        {
            cursor.close();
            return sqLiteDatabase.insert(TABLE,null,contentValues);
        }

    }

    public int delete(String server, String user_name, String type)
    {
        return getWritableDatabase().delete(TABLE,"server=?"+" AND "+"user_name=?"+" AND "+"type=?",new String[]{server,user_name,type});
    }

    public int change_display(String server,String user_name, String new_name, String type)
    {
        SQLiteDatabase sqLiteDatabase=getReadableDatabase();
        ContentValues contentValues=new ContentValues();
        contentValues.put("display",new_name);
        return sqLiteDatabase.update(TABLE,contentValues,"server=?"+" AND "+"user_name=?"+" AND "+"type=?",new String[]{server,user_name,type});
    }

    public FtpDetailsDialog.FtpPOJO getFtpPOJO(String server_string, String user_name_string,String type_)
    {
        FtpDetailsDialog.FtpPOJO ftpPOJO = null;
        SQLiteDatabase sqLiteDatabase=getReadableDatabase();
       Cursor cursor=sqLiteDatabase.query(TABLE,null,"server=?"+" AND "+"user_name=?"+" AND "+"type=?",new String[]{server_string,user_name_string,type_},null,null,null);
       if(cursor!=null && cursor.moveToFirst())
       {
           String server = cursor.getString(0);
           int port = cursor.getInt(1);
           String mode = cursor.getString(2);
           String user_name = cursor.getString(3);
           String password = cursor.getString(4);
           String type = cursor.getString(5);
           boolean anonymous = cursor.getInt(6) != 0;
           String encoding = cursor.getString(7);
           String display = cursor.getString(8);

           ftpPOJO=new FtpDetailsDialog.FtpPOJO(server, port, mode, user_name, password, type, anonymous, encoding, display);

           cursor.close();
       }
           
           return ftpPOJO;
    }
}
