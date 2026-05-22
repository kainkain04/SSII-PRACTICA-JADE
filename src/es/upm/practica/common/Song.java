package es.upm.practica.common;

import java.io.Serializable;

public class Song implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String title;
    private String artist;
    private String genre;
    private String mood;
    private String description;

    public Song(String id, String title, String artist, String genre, String mood, String description) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.genre = genre;
        this.mood = mood;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getGenre() {
        return genre;
    }

    public String getMood() {
        return mood;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return title + " de " + artist;
    }
}