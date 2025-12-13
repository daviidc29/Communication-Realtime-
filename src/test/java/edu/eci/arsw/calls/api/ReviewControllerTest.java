package edu.eci.arsw.calls.api;

import edu.eci.arsw.calls.domain.Review;
import edu.eci.arsw.calls.domain.ReviewRepository;
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
        when(auth.getPrincipal()).thenReturn("s1");
        Map<String, Object> body = Map.of(
            "reservationId", "res1",
            "tutorId", "t1",
            "rating", 5,
            "comment", "Good"
        );

        ResponseEntity<?> resp = controller.create("Bearer t", body, auth);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(service).create(any(), any(), any(), any(), any(), eq(5), any());
    }

    @Test
    void create_Unauthorized_NullAuthentication() {
        assertEquals(HttpStatus.UNAUTHORIZED, controller.create("tok", Map.of(), null).getStatusCode());
    }

    @Test
    void create_BadRating() {
        when(auth.getPrincipal()).thenReturn("s1");
        Map<String, Object> body = Map.of("rating", "invalid-number");
        
        assertEquals(HttpStatus.BAD_REQUEST, controller.create("tok", body, auth).getStatusCode());
    }

    @Test
    void create_ServiceExceptions() {
        when(auth.getPrincipal()).thenReturn("s1");
        Map<String, Object> body = Map.of("rating", 5);

        doThrow(new SecurityException("Forbidden")).when(service).create(any(),any(),any(),any(),any(),anyInt(),any());
        assertEquals(HttpStatus.FORBIDDEN, controller.create("tok", body, auth).getStatusCode());

        doThrow(new IllegalArgumentException("Bad arg")).when(service).create(any(),any(),any(),any(),any(),anyInt(),any());
        assertEquals(HttpStatus.BAD_REQUEST, controller.create("tok", body, auth).getStatusCode());
        
        doThrow(new RuntimeException("Error")).when(service).create(any(),any(),any(),any(),any(),anyInt(),any());
        assertEquals(HttpStatus.BAD_REQUEST, controller.create("tok", body, auth).getStatusCode());
    }
    
    @Test
    void create_AuthzParsing() {
        when(auth.getPrincipal()).thenReturn("s1");
        doThrow(new RuntimeException("Parse error")).when(authz).parseBearer(anyString());
        
        controller.create("bad-token", Map.of("rating", 5), auth);
        
        verify(service).create(any(), any(), any(), any(), isNull(), anyInt(), any());
    }

    @Test
    void list_Success() {
        when(service.listByTutor("t1", 20)).thenReturn(List.of(new Review()));
        List<Review> result = controller.list("t1", 20);
        assertFalse(result.isEmpty());
        
        controller.list("t1", -5); 
        verify(service).listByTutor("t1", 1);
        
        controller.list("t1", 1000); 
        verify(service).listByTutor("t1", 50);
    }

    @Test
    void summary_Success() {
        ReviewRepository.TutorRatingSummary mockSum = mock(ReviewRepository.TutorRatingSummary.class);
        when(mockSum.getTutorId()).thenReturn("t1");
        when(mockSum.getAvg()).thenReturn(4.0);
        when(mockSum.getCount()).thenReturn(10L); 
        
        when(service.summary("t1")).thenReturn(mockSum);
        
        Map<String, Object> result = controller.summary("t1");
        assertEquals("t1", result.get("tutorId"));
        assertEquals(4.0, result.get("avg"));
    }
}