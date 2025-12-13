package edu.eci.arsw.calls.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

/**
 * Cliente REST para el servicio de reservas
 */
@Service
public class ReservationsClient {

    private final RestClient client;

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE = new ParameterizedTypeReference<>() {
    };

    public ReservationsClient(
            @Value("${reservations.base:http://localhost:8090/api/reservations}") String baseUrl) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
    }

    /**
     * Obtiene una reserva por ID
     * 
     * @param reservationId ID de la reserva
     * @param bearerToken   token Bearer opcional para autenticación
     * @return datos de la reserva o {} si no se encuentra
     */
    public Map<String, Object> getReservation(String reservationId, @Nullable String bearerToken) {
        RestClient.RequestHeadersSpec<?> req = client.get()
                .uri("/{id}", reservationId)
                .header("Accept", "application/json");

        if (bearerToken != null && !bearerToken.isBlank()) {
            String token = bearerToken.replaceFirst("(?i)^bearer\\s+", "");
            req = req.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        try {
            Map<String, Object> body = req.retrieve().body(MAP_TYPE);
            return body != null ? body : Map.of();
        } catch (RestClientResponseException ex) {
            return Map.of();
        }
    }
}
