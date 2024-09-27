package svl.kadatha.filex;

import java.io.Serializable;

public class
AlbumPOJO implements Serializable {
    private final String lower_album_name;
    private String id;
    private String album_name;
    private String artist;
    private String no_of_songs;
    private String album_path;

    public AlbumPOJO(String id, String album_name, String artist, String no_of_songs, String album_path) {
        this.id = id;
        this.album_name = album_name;
        this.lower_album_name = album_name.toLowerCase();
        this.artist = artist;
        this.no_of_songs = no_of_songs;
        this.album_path = album_path;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAlbumName() {
        return album_name;
    }

    public void setAlbumName(String album_name) {
        this.album_name = album_name;
    }

    public String getLowerAlbumName() {
        return lower_album_name;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }


    public String getNoOfSongs() {
        return no_of_songs;
    }

    public void setNoOfSongs(String no_of_songs) {
        this.no_of_songs = no_of_songs;
    }

    public String getAlbum_path() {
        return album_path;
    }

    public void setAlbum_path(String album_path) {
        this.album_path = album_path;
    }

}
