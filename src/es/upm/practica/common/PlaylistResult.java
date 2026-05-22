package es.upm.practica.common;

import java.io.Serializable;
import java.util.List;

public class PlaylistResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private String mood;
    private List<Song> songs;
    private String explanation;

    public PlaylistResult(String mood, List<Song> songs, String explanation) {
        this.mood = mood;
        this.songs = songs;
        this.explanation = explanation;
    }

    public String getMood() {
        return mood;
    }

    public List<Song> getSongs() {
        return songs;
    }

    public String getExplanation() {
        return explanation;
    }

    @Override
    public String toString() {
        return "PlaylistResult{" +
                "mood='" + mood + '\'' +
                ", songs=" + songs +
                ", explanation='" + explanation + '\'' +
                '}';
    }
}