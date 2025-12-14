package edu.eci.arsw.calls.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.eci.arsw.calls.domain.Review;
import edu.eci.arsw.calls.security.AuthorizationService;
import edu.eci.arsw.calls.service.ReviewService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la gestión de reseñas.
 */
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private static final String ERROR = "error";
    private static final ObjectMapper OM = new ObjectMapper();

    private final ReviewService reviewService;
    private final AuthorizationService authz;

    public ReviewController(ReviewService reviewService, AuthorizationService authz) {
        this.reviewService = reviewService;
        this.authz = authz;
    }

    /**
     * Crea una nueva reseña.
     *
     * @param bearer token Bearer para autenticación
     * @param body   datos de la reseña
     * @return respuesta HTTP con el resultado de la operación
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String bearer,
            @RequestBody Map<String, Object> body
    ) {
        if (bearer == null || bearer.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(ERROR, "UNAUTHORIZED"));
        }

        try {
            authz.parseBearer(bearer);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(ERROR, "INVALID_TOKEN"));
        }

        String studentId;
        String studentName;
        try {
            Map<String, Object> claims = decodeJwtClaims(bearer);
            studentId = String.valueOf(claims.getOrDefault("sub", "")).trim();
            studentName = String.valueOf(
                    claims.getOrDefault("name", claims.getOrDefault("email", ""))
            ).trim();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(ERROR, "INVALID_TOKEN_PAYLOAD"));
        }

        if (studentId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(ERROR, "UNAUTHORIZED"));
        }

        String reservationId = String.valueOf(body.getOrDefault("reservationId", "")).trim();
        String tutorId = String.valueOf(body.getOrDefault("tutorId", "")).trim();

        int rating;
        try {
            rating = Integer.parseInt(String.valueOf(body.getOrDefault("rating", "0")).trim());
        } catch (NumberFormatException nfe) {
            return ResponseEntity.badRequest().body(Map.of(ERROR, "rating inválido"));
        }

        String comment = body.get("comment") == null ? null : String.valueOf(body.get("comment")).trim();

        try {
            reviewService.create(bearer, reservationId, tutorId, studentId, studentName, rating, comment);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (DuplicateKeyException dke) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(ERROR, "Ya existe una reseña para esa reserva"));
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(ERROR, se.getMessage()));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().body(Map.of(ERROR, iae.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(ERROR, e.getMessage()));
        }
    }

    /**
     * Lista las reseñas de un tutor específico.
     *
     * @param tutorId ID del tutor
     * @param limit   límite máximo de reseñas a retornar
     * @return lista de reseñas
     */
    @GetMapping("/tutor/{tutorId}")
    public List<ReviewResponse> list(
            @PathVariable("tutorId") String tutorId,
            @RequestParam(name = "limit", defaultValue = "20") int limit
    ) {
        limit = Math.max(1, Math.min(50, limit));
        String tid = tutorId == null ? "" : tutorId.trim();

        return reviewService.listByTutor(tid, limit)
                .stream()
                .map(ReviewResponse::from)
                .toList();
    }

    /**
     * Obtiene el resumen de calificaciones de un tutor.
     *
     * @param tutorId ID del tutor
     * @return mapa con el resumen de calificaciones
     */
    @GetMapping("/tutor/{tutorId}/summary")
    public Map<String, Object> summary(@PathVariable("tutorId") String tutorId) {
        String tid = tutorId == null ? "" : tutorId.trim();
        var s = reviewService.summary(tid);
        return Map.of("tutorId", s.tutorId(), "avg", s.avg(), "count", s.count());
    }

    /**
     * Decodifica los claims de un token JWT.
     *
     * @param bearer token Bearer
     * @return mapa con los claims del JWT
     * @throws IllegalArgumentException si el token es inválido
     * @throws IOException si ocurre un error al leer el JSON
     * @throws ArrayIndexOutOfBoundsException si el token no tiene el formato esperado
     */
    private static Map<String, Object> decodeJwtClaims(String bearer)
            throws IllegalArgumentException, java.io.IOException, ArrayIndexOutOfBoundsException {
        String token = bearer.replaceFirst("(?i)^Bearer\\s+", "").trim();
        String[] parts = token.split("\\.");
        if (parts.length < 2) throw new IllegalArgumentException("JWT inválido");

        byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
        String json = new String(payload, StandardCharsets.UTF_8);

        return OM.readValue(json, new TypeReference<Map<String, Object>>() {});
    }

    /**
     * Representación de respuesta de una reseña.
     */
    public record ReviewResponse(
            String id,
            String tutorId,
            String reservationId,
            String studentId,
            String studentName,
            int rating,
            String comment,
            String createdAt
    ) {
        static ReviewResponse from(Review r) {
            return new ReviewResponse(
                    r.getId(),
                    r.getTutorId(),
                    r.getReservationId(),
                    r.getStudentId(),
                    r.getStudentName(),
                    r.getRating(),
                    r.getComment(),
                    r.getCreatedAt() == null ? null : r.getCreatedAt().toString()
            );
        }
    }
}
