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
    void listByTutor_SublistLogic() {
        List<Review> reviews = List.of(new Review(), new Review(), new Review());
        when(repo.findTop50ByTutorIdOrderByCreatedAtDesc("t1")).thenReturn(reviews);

        List<Review> result = service.listByTutor("t1", 2);
        assertEquals(2, result.size());

        List<Review> resultAll = service.listByTutor("t1", 5);
        assertEquals(3, resultAll.size());
    }


    @Test
    void summary_RepoReturnsNull() {
        when(repo.aggregateSummary("t1")).thenReturn(null);
        
        var res = service.summary("t1");
        assertEquals("t1", res.tutorId());
        assertEquals(0.0, res.avg());
        assertEquals(0L, res.count());
    }

    @Test
    void summary_RepoReturnsData() {
        ReviewRepository.TutorRatingSummary mockSum = mock(ReviewRepository.TutorRatingSummary.class);
        when(mockSum.getTutorId()).thenReturn("t1");
        when(mockSum.getAvg()).thenReturn(4.2);
        when(mockSum.getCount()).thenReturn(5L);

        when(repo.aggregateSummary("t1")).thenReturn(mockSum);

        var res = service.summary("t1");
        assertEquals("t1", res.tutorId());
        assertEquals(4.2, res.avg());
        assertEquals(5L, res.count());
    }

    @Test
    void summary_RepoReturnsNullFields() {
        ReviewRepository.TutorRatingSummary mockSum = mock(ReviewRepository.TutorRatingSummary.class);
        when(mockSum.getTutorId()).thenReturn(null); 
        when(mockSum.getAvg()).thenReturn(null);
        when(mockSum.getCount()).thenReturn(null);

        when(repo.aggregateSummary("t1")).thenReturn(mockSum);

        var res = service.summary("t1"); 
        assertEquals("t1", res.tutorId());
        assertEquals(0.0, res.avg());
        assertEquals(0L, res.count());
    }

    @Test
    void summary_ExceptionCaught() {
        when(repo.aggregateSummary("t1")).thenThrow(new RuntimeException("DB Error"));

        var res = service.summary("t1");
        
        assertEquals("t1", res.tutorId());
        assertEquals(0.0, res.avg());
    }

    @Test
    void create_Validations_Basic() {
        assertThrows(IllegalArgumentException.class, () -> 
            service.create("t", "r", "t", "s", "n", 0, "c"));
        
        assertThrows(IllegalArgumentException.class, () -> 
            service.create("t", "r", "t", "s", "n", 6, "c"));

        assertThrows(IllegalArgumentException.class, () -> 
            service.create("t", null, "t", "s", "n", 5, "c"));
        
        assertThrows(IllegalArgumentException.class, () -> 
            service.create("t", "r", "", "s", "n", 5, "c"));
    }

    @Test
    void create_ReservationClient_Null() {
        when(reservationsClient.getReservation("res1", "token")).thenReturn(null);
        
        assertThrows(IllegalStateException.class, () -> 
            service.create("token", "res1", "t1", "s1", "n", 5, "c"));
    }

    @Test
    void create_SecurityException_StudentMismatch() {
        when(reservationsClient.getReservation("res1", "token"))
            .thenReturn(Map.of("studentId", "otroEstudiante", "tutorId", "t1"));

        assertThrows(SecurityException.class, () -> 
            service.create("token", "res1", "t1", "yo", "n", 5, "c"));
    }

    @Test
    void create_ArgumentException_TutorMismatch() {
        when(reservationsClient.getReservation("res1", "token"))
            .thenReturn(Map.of("studentId", "yo", "tutorId", "tutorX"));

        assertThrows(IllegalArgumentException.class, () -> 
            service.create("token", "res1", "tutorY", "yo", "n", 5, "c"));
    }

    @Test
    void create_Success_SnakeCaseKeys() {
        Map<String, Object> legacyMap = Map.of(
            "student_id", "s1", 
            "tutor_id", "t1"
        );
        when(reservationsClient.getReservation("res1", "token")).thenReturn(legacyMap);

        service.create("token", "res1", "t1", "s1", "name", 5, " comment ");

        verify(repo).save(any(Review.class));
    }
    
    @Test
    void create_Success_NormalKeys_NullComment() {
        Map<String, Object> map = Map.of("studentId", "s1", "tutorId", "t1");
        when(reservationsClient.getReservation("res1", "token")).thenReturn(map);

        service.create("token", "res1", "t1", "s1", "name", 5, null);

        verify(repo).save(argThat(review -> 
            review.getComment() == null &&
            review.getRating() == 5
        ));
    }
}