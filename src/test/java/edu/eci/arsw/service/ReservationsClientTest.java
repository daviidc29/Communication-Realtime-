package edu.eci.arsw.service;


import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.eci.arsw.calls.service.ReservationsClient;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReservationsClientTest {

    private MockWebServer server;
    private ReservationsClient client;

    @BeforeEach
    void setup() throws IOException {
        server = new MockWebServer();
        server.start();
        String baseUrl = server.url("/api/reservations").toString();
        client = new ReservationsClient(baseUrl);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void getReservation_Success() {
        // Simular respuesta JSON válida
        server.enqueue(new MockResponse()
                .setBody("{\"id\":\"res1\", \"studentId\":\"s1\", \"tutorId\":\"t1\"}")
                .addHeader("Content-Type", "application/json"));

        Map<String, Object> result = client.getReservation("res1", "Bearer token");

        assertEquals("res1", result.get("id"));
        assertEquals("s1", result.get("studentId"));
    }

    @Test
    void getReservation_NotFound() {
        // Simular 404
        server.enqueue(new MockResponse().setResponseCode(404));

        Map<String, Object> result = client.getReservation("res999", null);

        assertTrue(result.isEmpty());
    }

    @Test
    void getReservation_WithMalformedToken() {
        // Probar lógica de token
        server.enqueue(new MockResponse()
                .setBody("{}")
                .addHeader("Content-Type", "application/json"));
        
        // "bearer   xyz" -> debe limpiarse
        client.getReservation("res1", "bearer   xyz");
        
        try {
            // Verificar que el header llegó limpio (opcional, o confiar en cobertura)
            var req = server.takeRequest();
            assertEquals("Bearer xyz", req.getHeader("Authorization"));
        } catch (InterruptedException e) {
            fail("Request interrupted");
        }
    }
}