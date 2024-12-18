package svl.kadatha.filex.audio;

import android.os.Parcel;
import android.os.Parcelable;

import svl.kadatha.filex.FileObjectType;

public class AudioPOJO implements Parcelable {
    public static final Creator<AudioPOJO> CREATOR = new Creator<AudioPOJO>() {
        @Override
        public AudioPOJO createFromParcel(Parcel in) {
            return new AudioPOJO(in);
        }

        @Override
        public AudioPOJO[] newArray(int size) {
            return new AudioPOJO[size];
        }
    };

    private long id;  // Changed from int to long
    private String data;
    private String title;
    private String lower_title;
    private String album_id;
    private String album;
    private String artist;
    private String duration;
    private FileObjectType fileObjectType;
    private int position = -1;//default value

    public AudioPOJO(long id, String data, String title, String album_id, String album, String artist, String duration, FileObjectType fileObjectType) {  // Changed parameter type to long
        this.id = id;
        this.data = data;
        this.title = title;
        this.lower_title = title != null ? title.toLowerCase() : null;
        this.album_id = album_id;
        this.album = album;
        this.artist = artist;
        this.duration = duration;
        this.fileObjectType = fileObjectType;
    }

    protected AudioPOJO(Parcel in) {
        id = in.readLong();  // Changed from readInt to readLong
        data = in.readString();
        title = in.readString();
        lower_title = title != null ? title.toLowerCase() : null;
        album_id = in.readString();
        album = in.readString();
        artist = in.readString();
        duration = in.readString();
        position = in.readInt();
        String fileObjectTypeName = in.readString();
        if (fileObjectTypeName != null) {
            fileObjectType = FileObjectType.valueOf(fileObjectTypeName);
        } else {
            fileObjectType = null;
        }
    }

    // Getters
    public long getId() {  // Changed return type to long
        return id;
    }

    public void setId(long id) {  // Changed parameter type to long
        this.id = id;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        this.lower_title = title != null ? title.toLowerCase() : null;
    }

    public String getLowerTitle() {
        return lower_title;
    }

    public String getAlbumId() {
        return album_id;
    }

    public void setAlbumId(String album_id) {
        this.album_id = album_id;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public FileObjectType getFileObjectType() {
        return fileObjectType;
    }

    public void setFileObjectType(FileObjectType fileObjectType) {
        this.fileObjectType = fileObjectType;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeLong(id);  // Changed from writeInt to writeLong
        parcel.writeString(data);
        parcel.writeString(title);
        parcel.writeString(album_id);
        parcel.writeString(album);
        parcel.writeString(artist);
        parcel.writeString(duration);
        parcel.writeInt(position);
        parcel.writeString(fileObjectType != null ? fileObjectType.name() : null);
    }
}