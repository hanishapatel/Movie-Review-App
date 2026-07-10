package com.example.demo.movies;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ReviewService {
    @Autowired
    private ReviewRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    public Review createReview(String reviewBody, Integer rating, String author, boolean spoiler, String imdbId) {
        String displayName = (author == null || author.isBlank()) ? "Anonymous" : author.trim();
        Review review = repository.insert(new Review(reviewBody, rating, displayName, spoiler,
                LocalDateTime.now(), LocalDateTime.now()));
        mongoTemplate.update(Movie.class)
                .matching(Criteria.where("imdbId").is(imdbId))
                .apply(new Update().push("reviewIds").value(review))
                .first();
        return review;
    }

    public Review updateReview(String id, String reviewBody, Integer rating, Boolean spoiler) {
        Review review = repository.findById(new ObjectId(id))
                .orElseThrow(() -> new RuntimeException("Review not found: " + id));
        if (reviewBody != null) review.setBody(reviewBody);
        if (rating != null) review.setRating(rating);
        if (spoiler != null) review.setSpoiler(spoiler);
        review.setUpdated(LocalDateTime.now());
        return repository.save(review);
    }

    public Review upvoteReview(String id) {
        Review review = repository.findById(new ObjectId(id))
                .orElseThrow(() -> new RuntimeException("Review not found: " + id));
        review.setUpvotes(review.getUpvotes() + 1);
        return repository.save(review);
    }

    public void deleteReview(String id) {
        ObjectId objectId = new ObjectId(id);
        // remove the reference from any movie that holds it, then delete the review
        mongoTemplate.update(Movie.class)
                .apply(new Update().pull("reviewIds", objectId))
                .all();
        repository.deleteById(objectId);
    }
}
