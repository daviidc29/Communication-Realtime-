package edu.eci.arsw.calls.service;

import edu.eci.arsw.calls.domain.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Servicio para gestionar las reseñas de tutores.
 */
@Service
public class ReviewService {

    private static final String TUTOR_ID = "tutorId";

    private final ReviewRepository repo;
    private final ReservationsClient reservationsClient;

    public ReviewService(ReviewRepository repo, ReservationsClient reservationsClient) {
        this.repo = repo;
        this.reservationsClient = reservationsClient;
    }

    /**
     * Lista las reseñas de un tutor con un límite máximo.
     *
     * @param tutorId ID del tutor.
     * @param limit   Límite máximo de reseñas a retornar.
     * @return Lista de reseñas del tutor.
     */
    public List<Review> listByTutor(String tutorId, int limit) {
        List<Review> all = repo.findTop50ByTutorIdOrderByCreatedAtDesc(tutorId);
        return all.size() > limit ? all.subList(0, limit) : all;
    }

    /**
     * Resumen de las reseñas de un tutor.
     *
     * @param tutorId ID del tutor.
     * @return Resumen con promedio y conteo de reseñas.
     */
    public record TutorSummary(String tutorId, double avg, long count) {
    }

    /**
     * Obtiene el resumen de calificaciones de un tutor.
     *
     * @param tutorId ID del tutor.
     * @return Resumen con promedio y conteo de reseñas.
     */
    public TutorSummary summary(String tutorId) {
        try {
            var rows = repo.aggregateSummary(tutorId);
            if (rows == null || rows.isEmpty())
                return new TutorSummary(tutorId, 0.0, 0L);

            var s = rows.get(0);
            return new TutorSummary(
                    s.getTutorId() == null ? tutorId : s.getTutorId(),
                    s.getAvg() == null ? 0.0 : s.getAvg(),
                    s.getCount() == null ? 0L : s.getCount());

        } catch (Exception e) {
            return new TutorSummary(tutorId, 0.0, 0L);
        }
    }

    /**
     * Método de depuración para obtener información interna de las reseñas.
     *
     * @param tutorId ID del tutor.
     * @return Mapa con información de depuración.
     */
    public Map<String, Object> debugTutor(String tutorId) {
        var lastForTutor = repo.findTop3ByTutorIdOrderByCreatedAtDesc(tutorId).stream().map(r -> Map.of(
                "id", r.getId(),
                TUTOR_ID, r.getTutorId(),
                "rating", r.getRating(),
                "createdAt", String.valueOf(r.getCreatedAt()),
                "reservationId", r.getReservationId())).toList();

        var lastAny = repo.findTop5ByOrderByCreatedAtDesc().stream().map(r -> Map.of(
                "id", r.getId(),
                TUTOR_ID, r.getTutorId(),
                "rating", r.getRating(),
                "createdAt", String.valueOf(r.getCreatedAt()))).toList();

        return Map.of(
                TUTOR_ID, tutorId,
                "totalReviews", repo.count(),
                "countByTutorId", repo.countByTutorId(tutorId),
                "last3ForTutor", lastForTutor,
                "last5AnyTutor", lastAny);
    }

    public void create(String bearerToken,
            String reservationId,
            String tutorId,
            String studentId,
            String studentName,
            int rating,
            String comment) {

        reservationId = reservationId == null ? "" : reservationId.trim();
        tutorId = tutorId == null ? "" : tutorId.trim();
        studentId = studentId == null ? "" : studentId.trim();
        studentName = (studentName == null) ? null : studentName.trim();

        if (rating < 1 || rating > 5)
            throw new IllegalArgumentException("rating fuera de rango (1..5)");
        if (reservationId.isBlank())
            throw new IllegalArgumentException("reservationId requerido");
        if (tutorId.isBlank())
            throw new IllegalArgumentException("tutorId requerido");
        if (studentId.isBlank())
            throw new SecurityException("UNAUTHORIZED");

        Map<String, Object> r = reservationsClient.getReservation(reservationId, bearerToken);
        if (r == null)
            throw new IllegalStateException("No se pudo validar la reserva");

        String rStudent = String.valueOf(r.getOrDefault("studentId", r.getOrDefault("student_id", ""))).trim();
        String rTutor = String.valueOf(r.getOrDefault(TUTOR_ID, r.getOrDefault("tutor_id", ""))).trim();


        if (!studentId.equals(rStudent))
            throw new SecurityException("La reserva no pertenece al estudiante autenticado");
        if (!tutorId.equals(rTutor))
            throw new IllegalArgumentException("tutorId no coincide con la reserva");

        Review review = new Review();
        review.setReservationId(reservationId);
        review.setTutorId(rTutor);
        review.setStudentId(rStudent);
        review.setStudentName(studentName);
        review.setRating(rating);
        review.setComment(comment == null ? null : comment.strip());
        review.setCreatedAt(Instant.now());

        repo.save(review);
    }
}