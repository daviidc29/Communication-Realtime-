package edu.eci.arsw.service;

import edu.eci.arsw.calls.domain.Review;
import edu.eci.arsw.calls.domain.ReviewRepository;
import edu.eci.arsw.calls.domain.TutorRatingSummaryDoc;
import edu.eci.arsw.calls.service.ReservationsClient;
import edu.eci.arsw.calls.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Collections;
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

    @BeforeEach
    void setup() {
        lenient().when(repo.save(any(Review.class))).thenAnswer(i -> {
            Review r = i.getArgument(0);
            ReflectionTestUtils.setField(r, "id", "mock-id"); 
            return r;
        });
    }


    @Test
    void listByTutor_Limits() {
        List<Review> list = List.of(new Review(), new Review());
        when(repo.findTop50ByTutorIdOrderByCreatedAtDesc("t1")).thenReturn(list);

        assertEquals(1, service.listByTutor("t1", 1).size());
        assertEquals(2, service.listByTutor("t1", 10).size());
    }

    @Test
    void summary_RepoReturnsNullList() {
        when(repo.aggregateSummary("t1")).thenReturn(null);
        var res = service.summary("t1");
        assertEquals("t1", res.tutorId());
        assertEquals(0.0, res.avg());
        assertEquals(0L, res.count());
    }

    @Test
    void summary_RepoReturnsEmptyList() {
        when(repo.aggregateSummary("t1")).thenReturn(Collections.emptyList());
        var res = service.summary("t1");
        assertEquals("t1", res.tutorId());
        assertEquals(0.0, res.avg());
    }

    @Test
    void summary_RepoReturnsData() {
        TutorRatingSummaryDoc doc = new TutorRatingSummaryDoc();
        doc.setTutorId("t1");
        doc.setAvg(4.5);
        doc.setCount(10L);

        when(repo.aggregateSummary("t1")).thenReturn(List.of(doc));

        var res = service.summary("t1");
        assertEquals("t1", res.tutorId());
        assertEquals(4.5, res.avg());
        assertEquals(10L, res.count());
    }

    @Test
    void summary_RepoReturnsObjectWithNullFields() {
        TutorRatingSummaryDoc doc = new TutorRatingSummaryDoc();
        doc.setTutorId(null);
        doc.setAvg(null);
        doc.setCount(null);

        when(repo.aggregateSummary("t1")).thenReturn(List.of(doc));

        var res = service.summary("t1");
        assertEquals("t1", res.tutorId());
        assertEquals(0L, res.count());
        assertEquals(0.0, res.avg());
    }

    @Test
    void summary_RepoReturnsObjectWithEmptyId() {
        TutorRatingSummaryDoc doc = new TutorRatingSummaryDoc();
        doc.setTutorId("");
        when(repo.aggregateSummary("t1")).thenReturn(List.of(doc));
        var res = service.summary("t1");
        assertEquals("t1", res.tutorId());
    }

    @Test
    void summary_ExceptionCaught() {
        when(repo.aggregateSummary("t1")).thenThrow(new RuntimeException("DB error"));
        var res = service.summary("t1");
        assertEquals("t1", res.tutorId());
        assertEquals(0.0, res.avg());
    }

    @Test
    void create_Validations() {
        assertThrows(IllegalArgumentException.class, () -> service.create("t", "r", "t", "s", "n", 0, "c"));
        assertThrows(IllegalArgumentException.class, () -> service.create("t", "r", "t", "s", "n", 6, "c"));
        assertThrows(IllegalArgumentException.class, () -> service.create("t", "", "t", "s", "n", 5, "c"));
        assertThrows(IllegalArgumentException.class, () -> service.create("t", "r", "", "s", "n", 5, "c"));
        assertThrows(SecurityException.class, () -> service.create("t", "r", "t", "", "n", 5, "c"));
    }

    @Test
    void create_ReservationClientReturnsNull() {
        when(reservationsClient.getReservation("res1", "tok")).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> service.create("tok", "res1", "t1", "s1", "n", 5, "c"));
    }

    @Test
    void create_SecurityException_StudentMismatch() {
        when(reservationsClient.getReservation("res1", "tok"))
                .thenReturn(Map.of("studentId", "other", "tutorId", "t1"));
        assertThrows(SecurityException.class, () -> service.create("tok", "res1", "t1", "s1", "n", 5, "c"));
    }

    @Test
    void create_TutorMismatch() {
        when(reservationsClient.getReservation("res1", "tok"))
                .thenReturn(Map.of("studentId", "s1", "tutorId", "otherTutor"));
        assertThrows(IllegalArgumentException.class, () -> service.create("tok", "res1", "t1", "s1", "n", 5, "c"));
    }

    @Test
    void create_Success_SnakeCase_And_Trims() {
        Map<String, Object> map = Map.of("student_id", "s1", "tutor_id", "t1");
        when(reservationsClient.getReservation("res1", "tok")).thenReturn(map);

        service.create("tok", "res1", "t1", "s1", null, 5, "  nice  ");

        verify(repo).save(argThat(r -> 
            r.getStudentName() == null && 
            r.getComment().equals("nice") &&
            r.getReservationId().equals("res1")
        ));
    }
    
    @Test
    void create_Success_NullComment() {
        Map<String, Object> map = Map.of("studentId", "s1", "tutorId", "t1");
        when(reservationsClient.getReservation("res1", "tok")).thenReturn(map);

        service.create("tok", "res1", "t1", "s1", "name", 5, null);

        verify(repo).save(argThat(r -> r.getComment() == null));
    }
}