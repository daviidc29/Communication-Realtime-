package edu.eci.arsw.calls.service;

import edu.eci.arsw.calls.domain.Review;
import edu.eci.arsw.calls.domain.ReviewRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Servicio para gestión de reseñas
 */
@Service
public class ReviewService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ReviewService.class);

    private final ReviewRepository repo;
    private final ReservationsClient reservationsClient;

    public ReviewService(ReviewRepository repo, ReservationsClient reservationsClient) {
        this.repo = repo;
        this.reservationsClient = reservationsClient;
    }

    /**
     * Lista reseñas de un tutor (público)
     * 
     * @param tutorId ID del tutor
     * @param limit   cantidad máxima de reseñas a retornar
     * @return lista de reseñas
     */
    public List<Review> listByTutor(String tutorId, int limit) {
        List<Review> all = repo.findTop50ByTutorIdOrderByCreatedAtDesc(tutorId);
        return all.size() > limit ? all.subList(0, limit) : all;
    }

    /**
     * Resumen de calificaciones de un tutor
     * 
     * @param tutorId ID del tutor
     * @param avg     promedio de calificaciones
     * @param count   cantidad de calificaciones
     * @return resumen con tutorId, avg, count
     */
    public record TutorSummary(String tutorId, double avg, long count) {
    }

    /**
     * Obtiene el resumen de calificaciones de un tutor (público)
     * 
     * @param tutorId ID del tutor
     * @return resumen con tutorId, avg, count
     */
    public TutorSummary summary(String tutorId) {
        try {
            var s = repo.aggregateSummary(tutorId);
            if (s == null)
                return new TutorSummary(tutorId, 0.0, 0L);

            String tid = (s.getTutorId() != null && !s.getTutorId().isBlank()) ? s.getTutorId() : tutorId;
            long count = (s.getCount() != null) ? s.getCount() : 0L;
            double avg = (s.getAvg() != null) ? s.getAvg() : 0.0;

            return new TutorSummary(tid, avg, count);
        } catch (Exception e) {
            log.error("[Reviews] Error calculando summary tutorId={}", tutorId, e);
            return new TutorSummary(tutorId, 0.0, 0L);
        }
    }

    /**
     * Crea una nueva reseña
     * 
     * @param bearerToken   token Bearer del estudiante
     * @param reservationId ID de la reserva
     * @param tutorId       ID del tutor
     * @param studentId     ID del estudiante
     * @param studentName   nombre del estudiante
     * @param rating        calificación (1..5)
     * @param comment       comentario opcional
     */
    public void create(String bearerToken, String reservationId, String tutorId,
            String studentId, String studentName, int rating, String comment) {
        if (rating < 1 || rating > 5)
            throw new IllegalArgumentException("rating fuera de rango (1..5)");
        if (reservationId == null || reservationId.isBlank())
            throw new IllegalArgumentException("reservationId requerido");
        if (tutorId == null || tutorId.isBlank())
            throw new IllegalArgumentException("tutorId requerido");

        Map<String, Object> r = reservationsClient.getReservation(reservationId, bearerToken);
        if (r == null) {
            throw new IllegalStateException("No se pudo validar la reserva");
        }
        String rStudent = String.valueOf(r.getOrDefault("studentId", r.getOrDefault("student_id", "")));
        String rTutor = String.valueOf(r.getOrDefault("tutorId", r.getOrDefault("tutor_id", "")));

        if (!studentId.equals(rStudent)) {
            throw new SecurityException("La reserva no pertenece al estudiante autenticado");
        }
        if (!tutorId.equals(rTutor)) {
            throw new IllegalArgumentException("tutorId no coincide con la reserva");
        }

        Review review = new Review();
        review.setReservationId(reservationId);
        review.setTutorId(tutorId);
        review.setStudentId(studentId);
        review.setStudentName(studentName);
        review.setRating(rating);
        review.setComment(comment == null ? null : comment.strip());
        repo.save(review);
    }
}
