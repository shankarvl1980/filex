package svl.kadatha.filex;
import android.graphics.Bitmap;

import java.io.Serializable;

public class AlbumPOJO implements Serializable 
{
	private String id;
	private String album_name;
	private final String lower_album_name;
	private String artist;
	private String no_of_songs;
	private Bitmap albumart;

	public AlbumPOJO(String id, String album_name, String lower_album_name,String artist, String no_of_songs, Bitmap art)
	{
		this.id=id;
		this.album_name = album_name;
		this.lower_album_name=lower_album_name;
		this.artist = artist;
		this.no_of_songs=no_of_songs;
		this.albumart=art;
	}
	
	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id=id;
	}

	public String getAlbumName() 
	{
		return album_name;
	}

	public String getLowerAlbumName()
	{
		return lower_album_name;
	}

	public void setAlbumName(String album_name) 
	{
		this.album_name=album_name;
	}

	public String getArtist() 
	{
		return artist;
	}

	public void setArtist(String artist) 
	{
		this.artist = artist;
	}


	public String getNoOfSongs() 
	{
		return no_of_songs;
	}

	public void setNoOfSongs(String no_of_songs) 
	{
		this.no_of_songs = no_of_songs;
	}
	

	public Bitmap getAlbumArt() 
	{
		return albumart;
	}

	public void setAlbumArt(Bitmap art) 
	{
		this.albumart = art;
	}
}
