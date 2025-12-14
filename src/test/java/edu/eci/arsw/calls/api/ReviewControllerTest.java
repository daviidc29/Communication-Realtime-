package edu.eci.arsw.calls.api;

import edu.eci.arsw.calls.domain.Review;
import edu.eci.arsw.calls.security.AuthorizationService;
import edu.eci.arsw.calls.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock private ReviewService service;
    @Mock private AuthorizationService authz;
    @Mock private Authentication auth; 
    
    @InjectMocks private ReviewController controller;


    @Test
    void create_Success() {
        when(auth.getPrincipal()).thenReturn("student1");
        Map<String, Object> body = Map.of(
            "reservationId", "res1",
            "tutorId", "tutor1",
            "rating", 5,
            "comment", "Excellent"
        );

        ResponseEntity<Map<String, Object>> response = controller.create("Bearer token", body, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(true, response.getBody().get("ok"));
        
        verify(service).create(
            eq("Bearer token"), eq("res1"), eq("tutor1"), eq("student1"), isNull(), eq(5), eq("Excellent")
        );
        verify(authz).parseBearer("Bearer token");
    }

    @Test
    void create_Success_DefaultsAndExceptionsIgnored() {
        when(auth.getPrincipal()).thenReturn("student1");
        
        doThrow(new RuntimeException("Token invalido")).when(authz).parseBearer(anyString());

        Map<String, Object> body = Map.of("reservationId", "res1");

        ResponseEntity<Map<String, Object>> response = controller.create("Bearer bad_token", body, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(service).create(any(), any(), any(), any(), any(), eq(0), isNull());
    }

    @Test
    void create_Unauthorized_NullAuthentication() {
        ResponseEntity<Map<String, Object>> resp = controller.create("t", Map.of(), null);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        assertEquals("UNAUTHORIZED", resp.getBody().get("error"));
    }

    @Test
    void create_Unauthorized_BlankPrincipal() {
        when(auth.getPrincipal()).thenReturn("   ");
        ResponseEntity<Map<String, Object>> resp = controller.create("t", Map.of(), auth);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void create_BadRequest_InvalidRatingFormat() {
        when(auth.getPrincipal()).thenReturn("s1");
        Map<String, Object> body = Map.of("rating", "cinco"); // String inválido

        ResponseEntity<Map<String, Object>> resp = controller.create("t", body, auth);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("rating inválido", resp.getBody().get("error"));
    }

    @Test
    void create_ServiceExceptions() {
        when(auth.getPrincipal()).thenReturn("s1");
        Map<String, Object> body = Map.of("rating", 5);

        doThrow(new SecurityException("No es tu reserva"))
            .when(service).create(any(), any(), any(), any(), any(), anyInt(), any());
        assertEquals(HttpStatus.FORBIDDEN, controller.create("t", body, auth).getStatusCode());

        doThrow(new IllegalArgumentException("Faltan datos"))
            .when(service).create(any(), any(), any(), any(), any(), anyInt(), any());
        assertEquals(HttpStatus.BAD_REQUEST, controller.create("t", body, auth).getStatusCode());

        doThrow(new RuntimeException("Error inesperado"))
            .when(service).create(any(), any(), any(), any(), any(), anyInt(), any());
        assertEquals(HttpStatus.BAD_REQUEST, controller.create("t", body, auth).getStatusCode());
    }


    @Test
    void list_Success_Mapping() {
        Review r = mock(Review.class);
        when(r.getId()).thenReturn("r1");
        when(r.getCreatedAt()).thenReturn(Instant.now());
        
        when(service.listByTutor("t1", 20)).thenReturn(List.of(r));

        List<ReviewController.ReviewResponse> result = controller.list("t1", 20);

        assertEquals(1, result.size());
        assertEquals("r1", result.get(0).id());
        assertNotNull(result.get(0).createdAt());
    }

    @Test
    void list_Mapping_NullDate() {
        Review r = mock(Review.class);
        when(r.getCreatedAt()).thenReturn(null);
        when(service.listByTutor("t1", 20)).thenReturn(List.of(r));

        List<ReviewController.ReviewResponse> result = controller.list("t1", 20);
        assertNull(result.get(0).createdAt());
    }

    @Test
    void list_LimitClamping() {
        when(service.listByTutor(anyString(), anyInt())).thenReturn(Collections.emptyList());

        controller.list("t1", -10);
        verify(service).listByTutor("t1", 1);

        controller.list("t1", 100);
        verify(service).listByTutor("t1", 50);
    }


    @Test
    void summary_Success() {
        ReviewService.TutorSummary mockSummary = new ReviewService.TutorSummary("t1", 4.5, 10L);
        when(service.summary("t1")).thenReturn(mockSummary);

        ReviewService.TutorSummary result = controller.summary("t1");

        assertEquals("t1", result.tutorId());
        assertEquals(4.5, result.avg());
    }
}