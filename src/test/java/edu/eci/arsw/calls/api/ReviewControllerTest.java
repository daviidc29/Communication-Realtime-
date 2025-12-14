package edu.eci.arsw.calls.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.eci.arsw.calls.domain.Review;
import edu.eci.arsw.calls.security.AuthorizationService;
import edu.eci.arsw.calls.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Base64;
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
    @InjectMocks private ReviewController controller;

    private final ObjectMapper mapper = new ObjectMapper();

    private String createFakeToken(Map<String, Object> claims) throws Exception {
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{}".getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(mapper.writeValueAsBytes(claims));
        String signature = "sig";
        return "Bearer " + header + "." + payload + "." + signature;
    }


    @Test
    void create_Success() throws Exception {
        String token = createFakeToken(Map.of("sub", "student1", "name", "Pepito"));
        Map<String, Object> body = Map.of("reservationId", "res1", "tutorId", "t1", "rating", 5);

        ResponseEntity<Map<String, Object>> response = controller.create(token, body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(service).create(eq(token), eq("res1"), eq("t1"), eq("student1"), eq("Pepito"), eq(5), isNull());
    }

    @Test
    void create_Success_NameFallbackToEmail() throws Exception {
        String token = createFakeToken(Map.of("sub", "student1", "email", "pepito@mail.com"));
        Map<String, Object> body = Map.of("rating", 5);

        controller.create(token, body);

        verify(service).create(any(), anyString(), anyString(), eq("student1"), eq("pepito@mail.com"), eq(5), any());
    }

    @Test
    void create_Unauthorized_MissingBearer() {
        assertEquals(HttpStatus.UNAUTHORIZED, controller.create(null, Map.of()).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, controller.create("", Map.of()).getStatusCode());
    }

    @Test
    void create_Unauthorized_AuthzServiceFails() {
        doThrow(new RuntimeException("Invalid signature")).when(authz).parseBearer(anyString());
        
        ResponseEntity<Map<String, Object>> resp = controller.create("Bearer abc.def.ghi", Map.of());
        
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        assertEquals("INVALID_TOKEN", resp.getBody().get("error"));
    }

    @Test
    void create_Unauthorized_BadJwtFormat() {
        String badToken = "Bearer SoloUnaParte"; 
        
        ResponseEntity<Map<String, Object>> resp = controller.create(badToken, Map.of());
        
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        assertEquals("INVALID_TOKEN_PAYLOAD", resp.getBody().get("error"));
    }

    @Test
    void create_Unauthorized_BadBase64() {
        String badToken = "Bearer header.Not@Base64.sig";
        
        ResponseEntity<Map<String, Object>> resp = controller.create(badToken, Map.of());
        
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        assertEquals("INVALID_TOKEN_PAYLOAD", resp.getBody().get("error"));
    }

    @Test
    void create_Unauthorized_MissingSub() throws Exception {
        String token = createFakeToken(Map.of("name", "Pepe"));
        
        ResponseEntity<Map<String, Object>> resp = controller.create(token, Map.of());
        
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        assertEquals("UNAUTHORIZED", resp.getBody().get("error"));
    }
    
    @Test
    void create_Unauthorized_SubIsEmpty() throws Exception {
        String token = createFakeToken(Map.of("sub", "   "));
        ResponseEntity<Map<String, Object>> resp = controller.create(token, Map.of());
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void create_BadRequest_InvalidRating() throws Exception {
        String token = createFakeToken(Map.of("sub", "s1"));
        Map<String, Object> body = Map.of("rating", "bad-number");

        ResponseEntity<Map<String, Object>> resp = controller.create(token, body);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("rating inválido", resp.getBody().get("error"));
    }

    @Test
    void create_ServiceExceptions() throws Exception {
        String token = createFakeToken(Map.of("sub", "s1"));
        Map<String, Object> body = Map.of("rating", 5);

        doThrow(new DuplicateKeyException("Dup")).when(service).create(any(), any(), any(), any(), any(), anyInt(), any());
        assertEquals(HttpStatus.CONFLICT, controller.create(token, body).getStatusCode());

        doThrow(new SecurityException("Forbidden")).when(service).create(any(), any(), any(), any(), any(), anyInt(), any());
        assertEquals(HttpStatus.FORBIDDEN, controller.create(token, body).getStatusCode());

        doThrow(new IllegalArgumentException("Args")).when(service).create(any(), any(), any(), any(), any(), anyInt(), any());
        assertEquals(HttpStatus.BAD_REQUEST, controller.create(token, body).getStatusCode());

        doThrow(new RuntimeException("Oops")).when(service).create(any(), any(), any(), any(), any(), anyInt(), any());
        assertEquals(HttpStatus.BAD_REQUEST, controller.create(token, body).getStatusCode());
    }


    @Test
    void list_Success_WithTutorIdAndLimit() {
        Review r = mock(Review.class);
        when(r.getId()).thenReturn("r1");
        when(r.getCreatedAt()).thenReturn(Instant.now());
        
        when(service.listByTutor("t1", 20)).thenReturn(List.of(r));

        var result = controller.list("t1", 20);
        assertEquals(1, result.size());
        assertEquals("r1", result.get(0).id());
        assertNotNull(result.get(0).createdAt());
    }

    @Test
    void list_NullTutorId_ShouldTrimToEmpty() {
        when(service.listByTutor("", 20)).thenReturn(Collections.emptyList());
        
        controller.list(null, 20); 
        
        verify(service).listByTutor("", 20);
    }
    
    @Test
    void list_Mapping_NullDate() {
        Review r = mock(Review.class);
        when(r.getCreatedAt()).thenReturn(null); 
        when(service.listByTutor("t1", 1)).thenReturn(List.of(r));
        
        var result = controller.list("t1", 1);
        assertNull(result.get(0).createdAt());
    }

    @Test
    void list_LimitClamping() {
        when(service.listByTutor(anyString(), anyInt())).thenReturn(Collections.emptyList());
        
        controller.list("t1", -5);
        verify(service).listByTutor("t1", 1);
        
        controller.list("t1", 99);
        verify(service).listByTutor("t1", 50);
    }


    @Test
    void summary_Success() {
        ReviewService.TutorSummary sum = new ReviewService.TutorSummary("t1", 4.0, 10);
        when(service.summary("t1")).thenReturn(sum);

        Map<String, Object> result = controller.summary("t1");
        assertEquals("t1", result.get("tutorId"));
        assertEquals(4.0, result.get("avg"));
    }

    @Test
    void summary_NullTutorId() {
        when(service.summary("")).thenReturn(new ReviewService.TutorSummary("", 0, 0));
        
        controller.summary(null);
        verify(service).summary("");
    }
}