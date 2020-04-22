package com.uzumaki.naruto.musicalbeat.Model;

import android.net.Uri;

/**
 * Created by Bhavik Bist on 15-07-2017.
 */

public class SongInfo {
    private String songName;
    private String artistName;
    private Uri uri;
    private String songPath;
    private String duration;

    public SongInfo() {
    }

    public String getSongPath() {
        return songPath;
    }

    public void setSongPath(String songPath) {
        this.songPath = songPath;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration){
        this.duration = duration;
    }

    public String getSongName() {
        return songName;
    }

    public void setSongName(String songName) {
        this.songName = songName;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }
}
