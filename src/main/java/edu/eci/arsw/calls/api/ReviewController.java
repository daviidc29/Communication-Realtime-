package edu.eci.arsw.calls.api;

import edu.eci.arsw.calls.domain.Review;
import edu.eci.arsw.calls.security.AuthorizationService;
import edu.eci.arsw.calls.service.ReviewService;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para gestión de reseñas
 */
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private static final String ERROR = "error";

    private final ReviewService reviewService;
    private final AuthorizationService authz;

    public ReviewController(ReviewService reviewService, AuthorizationService authz) {
        this.reviewService = reviewService;
        this.authz = authz;
    }

    /**
     * Crea una nueva reseña (autenticado)
     * 
     * @param bearer         token Bearer del usuario autenticado
     * @param body           cuerpo con reservationId, tutorId, rating, comment
     * @param authentication info del usuario autenticado
     * @return resultado de la operación
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String bearer,
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        String userId = (authentication == null) ? null : String.valueOf(authentication.getPrincipal());
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(ERROR, "UNAUTHORIZED"));
        }

        String reservationId = String.valueOf(body.getOrDefault("reservationId", ""));
        String tutorId = String.valueOf(body.getOrDefault("tutorId", ""));
        int rating;
        try {
            rating = Integer.parseInt(String.valueOf(body.getOrDefault("rating", "0")));
        } catch (NumberFormatException nfe) {
            return ResponseEntity.badRequest().body(Map.of(ERROR, "rating inválido"));
        }
        String comment = body.get("comment") == null ? null : String.valueOf(body.get("comment"));

        String studentName = null;
        try {
            authz.parseBearer(bearer);
        } catch (Exception ignore) {
            /* noop */ }

        try {
            reviewService.create(bearer, reservationId, tutorId, userId, studentName, rating, comment);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(ERROR, se.getMessage()));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().body(Map.of(ERROR, iae.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(ERROR, e.getMessage()));
        }
    }

    /**
     * Lista reseñas de un tutor (público)
     * 
     * @param tutorId ID del tutor
     * @param limit   límite máximo de reseñas a retornar
     * @return Lista de reseñas
     */
    @GetMapping("/tutor/{tutorId}")
    public List<ReviewResponse> list(@PathVariable String tutorId,
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        limit = Math.max(1, Math.min(50, limit));
        return reviewService.listByTutor(tutorId, limit)
                .stream()
                .map(ReviewResponse::from)
                .toList();
    }

    /**
     * Resumen promedio y conteo (público)
     * 
     * @param tutorId ID del tutor
     * @return Resumen con tutorId, avg, count
     */
    @GetMapping("/tutor/{tutorId}/summary")
    public ReviewService.TutorSummary summary(@PathVariable String tutorId) {
        return reviewService.summary(tutorId);
    }

    public record ReviewResponse(
            String id,
            String tutorId,
            String reservationId,
            String studentId,
            String studentName,
            int rating,
            String comment,
            String createdAt) {
        static ReviewResponse from(Review r) {
            return new ReviewResponse(
                    r.getId(),
                    r.getTutorId(),
                    r.getReservationId(),
                    r.getStudentId(),
                    r.getStudentName(),
                    r.getRating(),
                    r.getComment(),
                    r.getCreatedAt() == null ? null : r.getCreatedAt().toString());
        }
    }
}
