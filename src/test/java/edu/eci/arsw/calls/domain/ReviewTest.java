package edu.eci.arsw.calls.domain;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class ReviewTest {

    @Test
    void testGettersAndSetters() {
        Review r = new Review();
        Instant now = Instant.now();

        r.setTutorId("tutor1");
        r.setStudentId("student1");
        r.setReservationId("res1");
        r.setStudentName("Pepito");
        r.setRating(5);
        r.setComment("Excellent");
        r.setCreatedAt(now);

        assertNull(r.getId()); 
        assertEquals("tutor1", r.getTutorId());
        assertEquals("student1", r.getStudentId());
        assertEquals("res1", r.getReservationId());
        assertEquals("Pepito", r.getStudentName());
        assertEquals(5, r.getRating());
        assertEquals("Excellent", r.getComment());
        assertEquals(now, r.getCreatedAt());
    }
}