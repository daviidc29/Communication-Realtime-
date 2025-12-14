package edu.eci.arsw.calls.domain;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Repositorio de reseñas
 */
public interface ReviewRepository extends MongoRepository<Review, String> {

    List<Review> findTop50ByTutorIdOrderByCreatedAtDesc(String tutorId);

    @Aggregation(pipeline = {
            "{ $match: { tutorId: ?0 } }",
            "{ $group: { _id: '$tutorId', count: { $sum: 1 }, avg: { $avg: '$rating' } } }",
            "{ $project: { _id: 0, tutorId: '$_id', count: 1, avg: 1 } }"
    })
    TutorRatingSummary aggregateSummary(String tutorId);

    interface TutorRatingSummary {
        String getTutorId();

        Long getCount();

        Double getAvg();
    }
}
