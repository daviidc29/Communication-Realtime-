package edu.eci.arsw.calls.domain;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Repositorio para gestionar reseñas en MongoDB.
 */
public interface ReviewRepository extends MongoRepository<Review, String> {

    List<Review> findTop50ByTutorIdOrderByCreatedAtDesc(String tutorId);

    @Aggregation(pipeline = {
            "{ $match: { tutorId: ?0 } }",
            "{ $group: { _id: '$tutorId', " +
                    "count: { $sum: 1 }, " +
                    "avg: { $avg: { $convert: { input: '$rating', to: 'double', onError: null, onNull: null } } } " +
            "} }",
            "{ $project: { _id: 0, tutorId: '$_id', count: 1, avg: { $ifNull: ['$avg', 0] } } }"
    })
    TutorRatingSummary aggregateSummary(String tutorId);

    interface TutorRatingSummary {
        String getTutorId();
        Long getCount();
        Double getAvg();
    }
}
