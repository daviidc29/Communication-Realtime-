package edu.eci.arsw.calls.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TutorRatingSummaryDocTest {

    @Test
    void testTutorId_SetAndGet() {
        TutorRatingSummaryDoc doc = new TutorRatingSummaryDoc();
        String expectedId = "tutor-123";
        
        doc.setTutorId(expectedId);
        
        assertEquals(expectedId, doc.getTutorId(), "El tutorId no coincide con el valor asignado");
    }

    @Test
    void testCount_SetAndGet() {
        TutorRatingSummaryDoc doc = new TutorRatingSummaryDoc();
        Long expectedCount = 42L;
        
        doc.setCount(expectedCount);
        
        assertEquals(expectedCount, doc.getCount(), "El count no coincide con el valor asignado");
    }

    @Test
    void testAvg_SetAndGet() {
        TutorRatingSummaryDoc doc = new TutorRatingSummaryDoc();
        Double expectedAvg = 4.5;
        
        doc.setAvg(expectedAvg);
        
        assertEquals(expectedAvg, doc.getAvg(), "El avg no coincide con el valor asignado");
    }

    @Test
    void testNullValues() {
        TutorRatingSummaryDoc doc = new TutorRatingSummaryDoc();
        
        doc.setTutorId(null);
        doc.setCount(null);
        doc.setAvg(null);
        
        assertNull(doc.getTutorId());
        assertNull(doc.getCount());
        assertNull(doc.getAvg());
    }
}