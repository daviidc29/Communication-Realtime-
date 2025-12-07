package edu.eci.arsw.calls.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

/**
 * Servicio para verificar la elegibilidad de una reserva.
 */
@Service
public class EligibilityService {

    private static final Logger log = LoggerFactory.getLogger(EligibilityService.class);

    private final RestClient schedulerClient;
    private final String baseUrl;

    // Se define el tipo de referencia para garantizar Type Safety
    private static final ParameterizedTypeReference<Map<String, Object>> RESPONSE_TYPE = new ParameterizedTypeReference<>() {
    };

    public EligibilityService(
            @Value("${reservations.base:http://localhost:8090/api/reservations}") String baseUrl) {
        this.baseUrl = baseUrl;
        this.schedulerClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    /**
     * Verifica la elegibilidad de una reserva para un usuario dado.
     *
     * @param reservationId ID de la reserva.
     * @param userId        ID del usuario.
     * @return Resultado de la verificación de elegibilidad.
     */
    public EligibilityResult checkReservation(String reservationId, String userId) {
        return checkReservation(reservationId, userId, null);
    }

    /**
     * Verifica la elegibilidad de una reserva para un usuario dado, con token
     * opcional.
     *
     * @param reservationId ID de la reserva.
     * @param userId        ID del usuario.
     * @param bearerToken   Token de autenticación opcional.
     * @return Resultado de la verificación de elegibilidad.
     */
    public EligibilityResult checkReservation(String reservationId,
            String userId,
            @Nullable String bearerToken) {
        try {
            RestClient.RequestHeadersSpec<?> req = buildRequest(reservationId, bearerToken);
            Map<String, Object> resp = req.retrieve().body(RESPONSE_TYPE);

            if (resp == null) {
                String reason = "Reservations empty response from " + baseUrl;
                log.warn("[Eligibility] Respuesta vacía del backend de reservas: {}", reason);
                return EligibilityResult.notEligible(reason);
            }

            return handleResponse(resp, reservationId, userId);

        } catch (RestClientResponseException ex) {
            return handleRestClientResponseException(ex, reservationId);
        } catch (ResourceAccessException ex) {
            // Errores de conexión (DNS, timeout, refused, etc.)
            log.warn("[Eligibility] Backend de reservas INACCESIBLE ({}) para id {}. " +
                    "Permitimos la llamada como fallback.",
                    baseUrl, reservationId, ex);
            return EligibilityResult.ok();

        } catch (Exception ex) {
            // Cualquier otro error inesperado tampoco debe tumbar la llamada
            log.warn("[Eligibility] Error inesperado consultando {} para reserva {}. " +
                    "Permitimos la llamada como fallback. Causa: {}",
                    baseUrl, reservationId, ex.toString());
            return EligibilityResult.ok();
        }
    }

    private RestClient.RequestHeadersSpec<?> buildRequest(String reservationId, @Nullable String bearerToken) {
        RestClient.RequestHeadersSpec<?> req = schedulerClient.get()
                .uri("/{id}", reservationId)
                .header("Accept", "application/json");

        if (bearerToken != null && !bearerToken.isBlank()) {

            String token = bearerToken.replaceFirst("(?i)^bearer\\s+", "");
            req = req.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return req;
    }

    private EligibilityResult handleResponse(Map<String, Object> resp, String reservationId, String userId) {
        String status = String.valueOf(resp.getOrDefault("status", ""));
        String studentId = String.valueOf(resp.getOrDefault("studentId",
                resp.getOrDefault("student_id", "")));
        String tutorId = String.valueOf(resp.getOrDefault("tutorId",
                resp.getOrDefault("tutor_id", "")));

        boolean isParticipant = userId.equals(studentId) || userId.equals(tutorId);
        String up = status == null ? "" : status.toUpperCase();
        boolean eligibleStatus = "ACEPTADO".equals(up)
                || "ACTIVE".equals(up)
                || "ACTIVA".equals(up);

        if (eligibleStatus && isParticipant) {
            return EligibilityResult.ok();
        }

        String reason = "Not active/participant (status=" + status
                + ", studentId=" + studentId
                + ", tutorId=" + tutorId + ")";
        log.info("[Eligibility] Usuario {} NO elegible para reserva {}: {}",
                userId, reservationId, reason);
        return EligibilityResult.notEligible(reason);
    }

    private EligibilityResult handleRestClientResponseException(RestClientResponseException ex, String reservationId) {
        String body = ex.getResponseBodyAsString();
        if (body.length() > 200) {
            body = body.substring(0, 200) + "…";
        }

        String baseMsg = "Reservations error: " + ex.getStatusCode()
                + " from " + baseUrl + " id=" + reservationId;

        if (ex.getStatusCode().is4xxClientError()) {
            String reason = baseMsg + (!body.isBlank() ? " body=" + body : "");
            log.info("[Eligibility] 4xx desde backend de reservas. Bloqueando llamada: {}",
                    reason);
            return EligibilityResult.notEligible(reason);
        } else {
            log.warn("[Eligibility] 5xx desde backend de reservas. " +
                    "Permitimos la llamada como fallback. {} body={}",
                    baseMsg, body);
            return EligibilityResult.ok();
        }
    }

    /**
     * Resultado de la verificación de elegibilidad.
     *
     * @param eligible Indica si es elegible.
     * @param reason   Razón en caso de no ser elegible.
     */
    public record EligibilityResult(boolean eligible, String reason) {
        public static EligibilityResult ok() {
            return new EligibilityResult(true, null);
        }

        public static EligibilityResult notEligible(String reason) {
            return new EligibilityResult(false, reason);
        }
    }
}
