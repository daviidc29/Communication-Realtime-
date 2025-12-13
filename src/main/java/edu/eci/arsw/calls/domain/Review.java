package edu.eci.arsw.calls.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Reseña de un estudiante sobre un tutor
 */
@Document("reviews")
@CompoundIndex(name = "unique_review_student_reservation", def = "{'reservationId':1,'studentId':1}", unique = true)
public class Review {

    @Id
    private String id;

    @Indexed
    private String tutorId;

    @Indexed
    private String reservationId;

    @Indexed
    private String studentId;

    private String studentName;
    private int rating;
    private String comment;

    @Indexed
    private Instant createdAt = Instant.now();

    public String getId() {
        return id;
    }

    public String getTutorId() {
        return tutorId;
    }

    public void setTutorId(String tutorId) {
        this.tutorId = tutorId;
    }

    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
