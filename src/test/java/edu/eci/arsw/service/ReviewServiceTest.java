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
import static org.mockito.ArgumentMatchers.argThat;
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

    @Test
    void create_ShouldTrimAllInputs() {
        Map<String, Object> map = Map.of("studentId", "s1", "tutorId", "t1");
        when(reservationsClient.getReservation("res1", "tok")).thenReturn(map);

        service.create("tok", "  res1  ", "  t1  ", "  s1  ", "  David  ", 5, "comment");

        verify(repo).save(argThat(r ->
            r.getReservationId().equals("res1") &&
            r.getTutorId().equals("t1") &&
            r.getStudentId().equals("s1") &&
            r.getStudentName().equals("David")
        ));
    }

    @Test
    void debugTutor_ShouldReturnDetails() {
        String tutorId = "t1";
        
        Review r1 = new Review();
        ReflectionTestUtils.setField(r1, "id", "rev1");
        r1.setTutorId(tutorId);
        r1.setRating(5);
        r1.setReservationId("resA");
        r1.setCreatedAt(Instant.now());

        Review r2 = new Review();
        ReflectionTestUtils.setField(r2, "id", "rev2");
        r2.setTutorId("other");
        r2.setRating(4);
        r2.setCreatedAt(Instant.now());

        when(repo.findTop3ByTutorIdOrderByCreatedAtDesc(tutorId)).thenReturn(List.of(r1));
        when(repo.findTop5ByOrderByCreatedAtDesc()).thenReturn(List.of(r1, r2));
        when(repo.count()).thenReturn(100L);
        when(repo.countByTutorId(tutorId)).thenReturn(50L);

        Map<String, Object> result = service.debugTutor(tutorId);

        assertNotNull(result);
        assertEquals(tutorId, result.get("tutorId"));
        assertEquals(100L, result.get("totalReviews"));
        assertEquals(50L, result.get("countByTutorId"));

        Object last3Obj = result.get("last3ForTutor");
        assertTrue(last3Obj instanceof List, "last3ForTutor should be a List");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> last3 = (List<Map<String, Object>>) last3Obj;
        assertEquals(1, last3.size());
        assertEquals("rev1", last3.get(0).get("id"));
        assertEquals("resA", last3.get(0).get("reservationId"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> last5 = (List<Map<String, Object>>) result.get("last5AnyTutor");
        assertEquals(2, last5.size());
    }
}