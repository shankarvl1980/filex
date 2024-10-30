package svl.kadatha.filex;

import android.os.Parcel;
import android.os.Parcelable;

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

    private int id;
    private String data;
    private String title;
    private String lower_title;
    private String album_id;
    private String album;
    private String artist;
    private String duration;
    private FileObjectType fileObjectType; // Enum type
    private int position;

    public AudioPOJO(int id, String data, String title, String album_id, String album, String artist, String duration, FileObjectType fileObjectType) {
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
        id = in.readInt();
        data = in.readString();
        title = in.readString();
        lower_title = title != null ? title.toLowerCase() : null; // Recompute lower_title
        album_id = in.readString();
        album = in.readString();
        artist = in.readString();
        duration = in.readString();
        position = in.readInt();
        String fileObjectTypeName = in.readString();
        if (fileObjectTypeName != null) {
            fileObjectType = FileObjectType.valueOf(fileObjectTypeName); // Convert String back to enum
        } else {
            fileObjectType = null;
        }
    }

    // Getters

    public int getId() {
        return id;
    }

    public void setId(int id) {
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
        // Update lower_title whenever title is set
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

    // Setters

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

    // Parcelable implementation

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(id);
        parcel.writeString(data);
        parcel.writeString(title);
        // lower_title is derived from title; no need to write it
        parcel.writeString(album_id);
        parcel.writeString(album);
        parcel.writeString(artist);
        parcel.writeString(duration);
        parcel.writeInt(position);
        parcel.writeString(fileObjectType != null ? fileObjectType.name() : null); // Write enum as String
    }
}
