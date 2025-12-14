package edu.eci.arsw.calls.domain;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Repositorio para gestionar las reseñas en MongoDB.
 */
public interface ReviewRepository extends MongoRepository<Review, String> {

    List<Review> findTop50ByTutorIdOrderByCreatedAtDesc(String tutorId);

    long countByTutorId(String tutorId);
    List<Review> findTop3ByTutorIdOrderByCreatedAtDesc(String tutorId);
    List<Review> findTop5ByOrderByCreatedAtDesc();

    @Aggregation(pipeline = {
            "{ $match: { tutorId: ?0 } }",
            "{ $group: { _id: '$tutorId', " +
                    "count: { $sum: 1 }, " +
                    "avg: { $avg: { $convert: { input: '$rating', to: 'double', onError: null, onNull: null } } } " +
            "} }",
            "{ $project: { _id: 0, tutorId: '$_id', count: 1, avg: { $ifNull: ['$avg', 0] } } }"
    })
    List<TutorRatingSummaryDoc> aggregateSummary(String tutorId);
}
