package com.example.demo.movies;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import java.util.List;

@Document(collection = "movielist")
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Movie {
    @Id
    private String id;

    @JsonProperty("imdbId")
    private String imdbId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("releaseDate")
    private String releaseDate;

    @JsonProperty("trailerLink")
    private String trailerLink;

    @JsonProperty("poster")
    private String poster;

    @JsonProperty("backdrops")
    private List<String> backdrops;

    @JsonProperty("genres")
    private List<String> genres;

    @JsonProperty("reviewIds")
    @DocumentReference
    private List<Review> reviewIds;

    // Computed at read time (not persisted in Mongo)
    @Transient
    @JsonProperty("averageRating")
    private Double averageRating;

    @Transient
    @JsonProperty("reviewCount")
    private Integer reviewCount;

    public Movie(String imdbId, String title, String releaseDate, String trailerLink, String poster, List<String> backdrops, List<String> genres) {
        this.imdbId = imdbId;
        this.title = title;
        this.releaseDate = releaseDate;
        this.trailerLink = trailerLink;
        this.poster = poster;
        this.backdrops = backdrops;
        this.genres = genres;
    }

    @Override
    public String toString() {
        return "Movie{id='" + id + "', imdbId='" + imdbId + "', title='" + title + "', releaseDate='" + releaseDate + "'}";
    }
}
