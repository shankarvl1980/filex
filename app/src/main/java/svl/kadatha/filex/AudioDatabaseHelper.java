package svl.kadatha.filex;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import java.util.ArrayList;
import java.util.List;

public class AudioDatabaseHelper
{
	private final Context context;
	private final SQLiteDatabase db;
	
	static final String DATABASE="AudioSavedLists.db";
	
	AudioDatabaseHelper(Context context)
	{
		this.context=context;
		db=context.openOrCreateDatabase(DATABASE, Context.MODE_PRIVATE,null);

	}
	
	public void createTable(String table)
	{
		try
		{
			db.execSQL("CREATE TABLE IF NOT EXISTS "+table+" (id INTEGER PRIMARY KEY, data TEXT, title TEXT, album TEXT,artist TEXT,duration TEXT)");
		}
		catch(SQLiteException e)
		{

		}
	}


	public long insert(String table,AudioPOJO audio)
	{
		
		ContentValues contentValues=new ContentValues();
		contentValues.put("id",audio.getId());
		contentValues.put("data",audio.getData());
		contentValues.put("title",audio.getTitle());
		contentValues.put("album",audio.getAlbum());
		contentValues.put("artist",audio.getArtist());
		contentValues.put("duration",audio.getDuration());
		
		return db.insert(table,null,contentValues);
	}
	

	public void insert(String table,List<AudioPOJO> audio_list)
	{
		
		int size=audio_list.size();
		for(int i=0;i<size;++i)
		{
			AudioPOJO audio=audio_list.get(i);
			ContentValues contentValues=new ContentValues();
			contentValues.put("id",audio.getId());
			contentValues.put("data",audio.getData());
			contentValues.put("title",audio.getTitle());
			contentValues.put("album",audio.getAlbum());
			contentValues.put("artist",audio.getArtist());
			contentValues.put("duration",audio.getDuration());
			db.insert(table,null,contentValues);
		}
		
	}
	
	public int delete(String table,int id)
	{
		return db.delete(table,"id=?",new String [] {Integer.toString(id)});
	}
	
	public void delete(String table,List<AudioPOJO> audio_list)
	{
		
		int size=audio_list.size();
		for(int i=0;i<size;++i)
		{
			AudioPOJO audio=audio_list.get(i);
			db.delete(table,"id=?",new String [] {Integer.toString(audio.getId())});
		}
	}
	
	
	public SQLiteDatabase getDatabase()
	{
		return db;
	}
	
	public void deleteTable(String table)
	{
		db.execSQL("DROP TABLE IF EXISTS "+table);
	
	}
	
	public ArrayList<String> getTables()
	{
		ArrayList<String>l=new ArrayList<>();
		try
		{
			Cursor c=db.rawQuery("SELECT name FROM sqlite_master WHERE TYPE='table' AND name!='android_metadata'",null);
			if(c.moveToFirst())
			{
				while(!c.isAfterLast())
				{

					l.add(c.getString(c.getColumnIndex("name")));
					c.moveToNext();
				}
			}
			c.close();
        }
		catch(SQLiteException e)
		{
			Global.print(context, "Exception thrown: could not extract entries");
		}
		return l;
	}
	
	public ArrayList<AudioPOJO> getAudioList(String table)
	{
		ArrayList<AudioPOJO> audio_list=new ArrayList<>();
		try
		{
			Cursor c=db.rawQuery("SELECT * FROM "+table,null);
			if(c.moveToFirst())
			{
				while(!c.isAfterLast())
				{

					int id=c.getInt(0);
					String data=c.getString(1);
					String title=c.getString(2);
					String album=c.getString(3);
					String artist=c.getString(4);
					String duration=c.getString(5);


					AudioPOJO audio=new AudioPOJO(id, data, title, album, artist, duration,FileObjectType.FILE_TYPE);
					audio_list.add(audio);
					c.moveToNext();

				}
			}
			c.close();
        }
		catch(SQLiteException e)
		{
			Global.print(context, context.getString(R.string.exception_thrown_colon_could_not_extract_entries));
		}
		
		 return audio_list;
		
	}

}
