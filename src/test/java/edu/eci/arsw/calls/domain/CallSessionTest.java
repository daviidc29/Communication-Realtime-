package edu.eci.arsw.calls.domain;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CallSessionTest {

    @Test
    void create_deberiaInicializarCamposCorrectamente() {
        String sessId = "sess-123";
        String resId = "res-456";
        Instant ttl = Instant.now().plusSeconds(3600);

        CallSession cs = CallSession.create(sessId, resId, ttl);

        assertNotNull(cs);
        assertEquals(sessId, cs.getSessionId());
        assertEquals(resId, cs.getReservationId());
        assertEquals("CREATED", cs.getStatus());
        assertEquals(ttl, cs.getTtl());
        assertTrue(cs.getCreatedAt() > 0);
        assertNotNull(cs.getParticipants());
        assertTrue(cs.getParticipants().isEmpty());
        assertNotNull(cs.getMetrics());
    }

    @Test
    void settersYGetters_deberianFuncionarParaTodosLosCampos() {
        CallSession cs = new CallSession();

        assertNull(cs.getId()); 

        cs.setSessionId("new-sess-id");
        assertEquals("new-sess-id", cs.getSessionId());

        cs.setReservationId("new-res-id");
        assertEquals("new-res-id", cs.getReservationId());

        List<Participant> parts = new ArrayList<>();
        Participant participant = new Participant();
        participant.setUserId("p1");
        parts.add(participant);
        cs.setParticipants(parts);
        assertEquals(1, cs.getParticipants().size());
        assertEquals("p1", cs.getParticipants().get(0).getUserId());

        cs.setStatus("ENDED");
        assertEquals("ENDED", cs.getStatus());

        long now = System.currentTimeMillis();
        cs.setCreatedAt(now);
        assertEquals(now, cs.getCreatedAt());

        cs.setConnectedAt(now + 100);
        assertEquals(now + 100, cs.getConnectedAt());

        cs.setEndedAt(now + 200);
        assertEquals(now + 200, cs.getEndedAt());

        cs.setTurnUsed(true);
        assertTrue(cs.isTurnUsed());
        cs.setTurnUsed(false);
        assertFalse(cs.isTurnUsed());

        Metrics m = new Metrics();
        cs.setMetrics(m);
        assertEquals(m, cs.getMetrics());

        Instant future = Instant.now().plusSeconds(60);
        cs.setTtl(future);
        assertEquals(future, cs.getTtl());
    }
}