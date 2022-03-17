package svl.kadatha.filex;
import android.graphics.Bitmap;

public class AudioPOJO
{
	private int id;
	private String data;
	private String title;
	private final String lower_title;
	private String album;
	private String artist;
	private String duration;
	private Bitmap albumart;
	private final FileObjectType fileObjectType;

	public AudioPOJO(int id,  String data, String title,String album, String artist, String duration,Bitmap albumart,FileObjectType fileObjectType)
	{
		this.id=id;
		this.data = data;
		this.title = title;
		this.lower_title=title.toLowerCase();
		this.album = album;
		this.artist = artist;
		this.duration=duration;
		this.albumart=albumart;
		this.fileObjectType=fileObjectType;
	}
	public int getId()
	{
		return id;
	}
	
	public void setId(int id)
	{
		this.id=id;
	}
	
	public String getData() 
	{
		return data;
	}

	public void setData(String data) 
	{
		this.data = data;
	}

	public String getTitle() 
	{
		return title;
	}

	public String getLowerTitle()
	{
		return lower_title;
	}

	public void setTitle(String title) 
	{
		this.title = title;
	}

	public String getAlbum() 
	{
		return album;
	}

	public void setAlbum(String album) 
	{
		this.album = album;
	}

	public String getArtist() 
	{
		return artist;
	}

	public void setArtist(String artist) 
	{
		this.artist = artist;
	}
	

	public String getDuration() 
	{
		return duration;
	}

	public void setDuration(String duration) 
	{
		this.duration = duration;
	}
	

	public Bitmap getAlbumArt() 
	{
		return albumart;
	}

	public void setAlbumArt(Bitmap art) 
	{
		this.albumart = art;
	}

	public FileObjectType getFileObjectType()
	{
		return this.fileObjectType;
	}
}
