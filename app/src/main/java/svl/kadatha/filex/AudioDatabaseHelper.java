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
			db.execSQL("CREATE TABLE IF NOT EXISTS "+table+" (id INTEGER, data TEXT, title TEXT,album_id TEXT, album TEXT,artist TEXT,duration TEXT)");
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
		contentValues.put("album_id",audio.getAlbumId());
		contentValues.put("album",audio.getAlbum());
		contentValues.put("artist",audio.getArtist());
		contentValues.put("duration",audio.getDuration());

		try{
			return db.insert(table,null,contentValues);
		}
		catch (SQLiteException e){}
		return 0;
	}
	

	public void insert(String table,List<AudioPOJO> audio_list)
	{
		
		int size=audio_list.size();
		for(int i=0;i<size;++i)
		{
			AudioPOJO audio=audio_list.get(i);
			insert(table,audio);
		}
		
	}
	
	public int delete_by_audio_id(String table,int id)
	{
		return db.delete(table,"id=?",new String [] {Integer.toString(id)});
	}
	
	public void delete_by_rowid(String table,List<Long> audio_rowid_list)
	{
		
		//int size=audio_list.size();
		int size=audio_rowid_list.size();
		for(int i=0;i<size;++i)
		{
			long rowID= audio_rowid_list.get(i);
			db.delete(table,"rowid=?",new String []{String.valueOf(rowID)});
		}
	}
	
	
	public SQLiteDatabase getDatabase()
	{
		return db;
	}
	
	public void deleteTable(String table)
	{
		try{
			db.execSQL("DROP TABLE IF EXISTS "+table);
		}
		catch (SQLiteException e){}

	
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
	
	public IndexedLinkedHashMap<Long,AudioPOJO> getAudioList(String table)
	{
		IndexedLinkedHashMap<Long,AudioPOJO> audio_list=new IndexedLinkedHashMap<>();
		try
		{
			Cursor c=db.rawQuery("SELECT  *,rowid FROM "+table ,null);
			if(c.moveToFirst())
			{
				while(!c.isAfterLast())
				{

					long rowid=c.getLong(c.getColumnIndexOrThrow("rowid"));
					int id=c.getInt(0);
					String data=c.getString(1);
					String title=c.getString(2);
					String album_id=c.getString(3);
					String album=c.getString(4);
					String artist=c.getString(5);
					String duration=c.getString(6);


					AudioPOJO audio=new AudioPOJO(id, data, title,album_id, album, artist, duration,FileObjectType.FILE_TYPE);
					audio_list.put(rowid,audio);
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
