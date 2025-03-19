package com.example.demo.movies;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
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
//    private String imdbId;
//    private String title;
//    private String releaseDate;
//    private String trailerLink;
//    private String poster;
//    private List<String> backdrops;
//    private List<String> genres;
//    @DocumentReference
//    private List<Review> reviewIds;

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

//    @Override
//    public String toString() {
//        return "Movie{" +
//                "id='" + id + '\'' +
//                ", imdbId='" + imdbId + '\'' +
//                ", title='" + title + '\'' +
//                ", releaseDate='" + releaseDate + '\'' +
//                ", trailerLink='" + trailerLink + '\'' +
//                ", poster='" + poster + '\'' +
//                ", backdrops=" + backdrops +
//                ", genres=" + genres +
//                ", reviewIds=" + reviewIds +
//                '}';
//    }

}