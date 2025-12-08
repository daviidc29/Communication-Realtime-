package edu.eci.arsw.service;

import edu.eci.arsw.calls.service.QualityMetricsService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.jupiter.api.Assertions.*;

class QualityMetricsServiceTest {

    private QualityMetricsService service;

    @BeforeEach
    void setUp() {
        service = new QualityMetricsService(new SimpleMeterRegistry());
    }

    @Test
    void recordSuccess_deberiaAgregarMuestra_casoFeliz1() {
        service.recordSuccess(100);
        var s = service.snapshot();
        assertEquals(1, s.samples());
        assertEquals(100L, s.p95ms());
        assertEquals(100L, s.p99ms());
    }

    @Test
    void recordSuccess_deberiaCalcularPercentiles_casoFeliz2() {
        service.recordSuccess(100);
        service.recordSuccess(200);
        service.recordSuccess(300);
        var s = service.snapshot();
        assertEquals(3, s.samples());
        assertTrue(s.p95ms() >= 200 && s.p95ms() <= 300);
        assertTrue(s.p99ms() >= 200 && s.p99ms() <= 300);
    }

    @Test
    void recordSuccess_noDeberiaRomper_cuandoSetupCero() {
        service.recordSuccess(0);
        var s = service.snapshot();
        assertEquals(1, s.samples());
        assertEquals(0L, s.p95ms());
    }

    @Test
    void recordSuccess_noDeberiaRomper_cuandoSetupNegativo() {
        service.recordSuccess(-50);
        var s = service.snapshot();
        assertEquals(1, s.samples());
        assertEquals(-50L, s.p95ms());
    }

    @Test
    void recordFailure_deberiaImpactarSuccessRate_casoFeliz1() {
        service.recordSuccess(100);
        service.recordFailure();
        var s = service.snapshot();
        assertEquals(1, s.samples());
        assertEquals(0.5, s.successRate(), 1e-4);
    }

    @Test
    void recordFailure_deberiaPermitirMultiplesFallos_casoFeliz2() {
        service.recordFailure(); service.recordFailure(); service.recordFailure();
        var s = service.snapshot();
        assertEquals(0, s.samples());
        assertEquals(0.0, s.successRate(), 1e-4);
    }

    @Test
    void snapshot_deberiaSerCorrecto_sinMuestras() {
        var s = service.snapshot();
        assertEquals(0L, s.p95ms());
        assertEquals(0L, s.p99ms());
        assertEquals(1.0, s.successRate(), 1e-4);
        assertEquals(0, s.samples());
    }

    @Test
    void snapshot_deberiaActualizarseTrasExitosYFallos() {
        service.recordSuccess(100);
        service.recordSuccess(200);
        service.recordFailure();
        var s = service.snapshot();
        assertEquals(2, s.samples());
        assertTrue(s.successRate() > 0 && s.successRate() < 1);
    }

    @Test
    @SuppressWarnings({"unchecked","rawtypes"})
    void evictOld_deberiaEliminarEventosFueraDeVentana() throws Exception {
        long now = System.currentTimeMillis();
        long veryOld = now - (10 * 60_000L); 

        Field fSucc = QualityMetricsService.class.getDeclaredField("successes");
        fSucc.setAccessible(true);
        ConcurrentLinkedQueue<Long> succQ = (ConcurrentLinkedQueue<Long>) fSucc.get(service);
        succQ.add(veryOld);

        Field fFail = QualityMetricsService.class.getDeclaredField("failures");
        fFail.setAccessible(true);
        ConcurrentLinkedQueue<Long> failQ = (ConcurrentLinkedQueue<Long>) fFail.get(service);
        failQ.add(veryOld);

        Field fSamples = QualityMetricsService.class.getDeclaredField("samples");
        fSamples.setAccessible(true);
        ConcurrentLinkedQueue samplesQ = (ConcurrentLinkedQueue) fSamples.get(service);
        Class<?> sampleKlass = Class.forName("edu.eci.arsw.calls.service.QualityMetricsService$Sample");
        Constructor<?> ctor = sampleKlass.getDeclaredConstructor(long.class, long.class);
        ctor.setAccessible(true);
        samplesQ.add(ctor.newInstance(veryOld, 123L));

        service.recordSuccess(50);

        var snap = service.snapshot();
        assertEquals(1, snap.samples());         
        assertTrue(snap.successRate() >= 0.0);    
    }
}
