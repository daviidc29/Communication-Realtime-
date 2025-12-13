package edu.eci.arsw.service;

import edu.eci.arsw.calls.domain.Review;
import edu.eci.arsw.calls.domain.ReviewRepository;
import edu.eci.arsw.calls.service.ReservationsClient;
import edu.eci.arsw.calls.service.ReviewService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private ReviewRepository repo;
    @Mock private ReservationsClient reservationsClient;
    @InjectMocks private ReviewService service;

    @Test
    void listByTutor_Limits() {
        Review r1 = new Review();
        Review r2 = new Review();
        when(repo.findTop50ByTutorIdOrderByCreatedAtDesc("t1")).thenReturn(List.of(r1, r2));

        List<Review> res1 = service.listByTutor("t1", 1);
        assertEquals(1, res1.size());

        List<Review> res2 = service.listByTutor("t1", 10);
        assertEquals(2, res2.size());
    }

@Test
    void summary_Found() {
        ReviewRepository.TutorRatingSummary mockSum = mock(ReviewRepository.TutorRatingSummary.class);
        when(repo.aggregateSummary("t1")).thenReturn(mockSum);
        
        var result = service.summary("t1");
        assertSame(mockSum, result);
    }

    @Test
    void summary_NotFound_Default() {
        when(repo.aggregateSummary("t1")).thenReturn(null);
        
        var result = service.summary("t1");
        assertEquals("t1", result.getTutorId());
        assertEquals(0.0, result.getAvg());
        assertEquals(0L, result.getCount());
    }

    @Test
    void create_Success() {
        when(reservationsClient.getReservation("res1", "token"))
                .thenReturn(Map.of("studentId", "s1", "tutorId", "t1"));
        
        service.create("token", "res1", "t1", "s1", "Pepe", 5, "Nice");
        
        verify(repo).save(any(Review.class));
    }

    @Test
    void create_Validations() {
        assertThrows(IllegalArgumentException.class, () -> service.create("t", "r", "t", "s", "n", 0, "c"));
        assertThrows(IllegalArgumentException.class, () -> service.create("t", "r", "t", "s", "n", 6, "c"));
        
        assertThrows(IllegalArgumentException.class, () -> service.create("t", null, "t", "s", "n", 5, "c"));
        assertThrows(IllegalArgumentException.class, () -> service.create("t", "r", "", "s", "n", 5, "c"));
    }

    @Test
    void create_ReservationValidationErrors() {
        when(reservationsClient.getReservation("resX", "token")).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> service.create("token", "resX", "t1", "s1", "n", 5, "c"));

        when(reservationsClient.getReservation("resY", "token"))
                .thenReturn(Map.of("studentId", "otherStudent", "tutorId", "t1"));
        assertThrows(SecurityException.class, () -> service.create("token", "resY", "t1", "s1", "n", 5, "c"));

        when(reservationsClient.getReservation("resZ", "token"))
                .thenReturn(Map.of("studentId", "s1", "tutorId", "otherTutor"));
        assertThrows(IllegalArgumentException.class, () -> service.create("token", "resZ", "t1", "s1", "n", 5, "c"));
    }
}