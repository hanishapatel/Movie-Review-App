package com.example.demo.movies;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "reviews")
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Review {
    @Id
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId id;

    @JsonProperty("body")
    private String body;

    // 1..5 star rating
    @JsonProperty("rating")
    private Integer rating;

    // optional display name (no auth) — defaults to "Anonymous"
    @JsonProperty("author")
    private String author;

    // "helpful" upvotes
    @JsonProperty("upvotes")
    private int upvotes;

    // marks the review as containing spoilers (blurred in the UI)
    @JsonProperty("spoiler")
    private boolean spoiler;

    @JsonProperty("created")
    private LocalDateTime created;

    @JsonProperty("updated")
    private LocalDateTime updated;

    public Review(String body, Integer rating, String author, boolean spoiler,
                  LocalDateTime created, LocalDateTime updated) {
        this.body = body;
        this.rating = rating;
        this.author = author;
        this.spoiler = spoiler;
        this.upvotes = 0;
        this.created = created;
        this.updated = updated;
    }
}
