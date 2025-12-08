package edu.eci.arsw.service;

import edu.eci.arsw.calls.service.EligibilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EligibilityServiceTest {

    private RestClient restClient;

    @SuppressWarnings("rawtypes")
    private RestClient.RequestHeadersUriSpec uriSpec;
    @SuppressWarnings("rawtypes")
    private RestClient.RequestHeadersSpec headersSpec;

    private RestClient.ResponseSpec responseSpec;
    private EligibilityService service;

    @BeforeEach
    void setUp() {
        service = new EligibilityService("http://scheduler.test");

        restClient   = mock(RestClient.class);
        uriSpec      = mock(RestClient.RequestHeadersUriSpec.class);
        headersSpec  = mock(RestClient.RequestHeadersSpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri("/{id}", "RES-1")).thenReturn(headersSpec);
        when(headersSpec.header("Accept", "application/json")).thenReturn(headersSpec);
        when(headersSpec.header(eq(HttpHeaders.AUTHORIZATION), anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        ReflectionTestUtils.setField(service, "schedulerClient", restClient);
    }

    @SuppressWarnings("unchecked")
    private void mockBody(Map<String, Object> body) {
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(body);
    }

    @Test
    void checkReservation_deberiaSerElegible_casoFeliz1() {
        mockBody(Map.of("status","ACEPTADO","studentId","U1","tutorId","T1"));
        var res = service.checkReservation("RES-1","U1","Bearer TOK");
        assertTrue(res.eligible());
        assertNull(res.reason());
        verify(headersSpec).header(HttpHeaders.AUTHORIZATION,"Bearer TOK");
        verify(headersSpec).header("Accept","application/json");
    }

    @Test
    void checkReservation_deberiaSerElegible_conACTIVE_yCamposSnakeCase_casoFeliz2() {
        mockBody(Map.of("status","ACTIVE","student_id","S9","tutor_id","U9"));
        var res = service.checkReservation("RES-1","U9","bearer   tok2");
        assertTrue(res.eligible());
        verify(headersSpec).header(HttpHeaders.AUTHORIZATION,"Bearer tok2");
    }

    @Test
    void checkReservation_noDeberiaPasar_porEstadoNoActivo() {
        mockBody(Map.of("status","CANCELADO","studentId","U1","tutorId","T1"));
        var res = service.checkReservation("RES-1","U1",null);
        assertFalse(res.eligible());
        assertTrue(res.reason().contains("Not active/participant"));
    }

    @Test
    void checkReservation_noDeberiaPasar_noEsParticipante_aunActivo() {
        mockBody(Map.of("status","ACTIVA","studentId","S1","tutorId","T1"));
        var res = service.checkReservation("RES-1","OTHER",null);
        assertFalse(res.eligible());
        assertTrue(res.reason().contains("Not active/participant"));
    }

    @Test
    void checkReservation_noDeberiaPasar_respuestaNula() {
        mockBody(null);
        var res = service.checkReservation("RES-1","U1",null);
        assertFalse(res.eligible());
        assertTrue(res.reason().toLowerCase().contains("empty response"));
    }

    @Test
    void checkReservation_deberiaBloquearEn4xx_yRetornarNotEligible() {
        var ex = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "bad", HttpHeaders.EMPTY,
                "bad-body".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        when(headersSpec.retrieve()).thenThrow(ex);

        var res = service.checkReservation("RES-1","U1",null);

        assertFalse(res.eligible());
        assertTrue(res.reason().contains("Reservations error: 400"));
    }

    @Test
    void checkReservation_deberiaPermitirEn5xx_fallbackOk() {
        var ex = HttpServerErrorException.create(
                HttpStatus.INTERNAL_SERVER_ERROR, "err", HttpHeaders.EMPTY,
                "oops".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        when(headersSpec.retrieve()).thenThrow(ex);

        var res = service.checkReservation("RES-1","U1",null);

        assertTrue(res.eligible());  
        assertNull(res.reason());
    }

    @Test
    void checkReservation_deberiaPermitirEnResourceAccessException_fallbackOk() {
        when(headersSpec.retrieve()).thenThrow(new ResourceAccessException("dns"));
        var res = service.checkReservation("RES-1","U1",null);
        assertTrue(res.eligible());
    }

    @Test
    void checkReservation_deberiaPermitirEnExcepcionDesconocida_fallbackOk() {
        when(headersSpec.retrieve()).thenThrow(new RuntimeException("boom"));
        var res = service.checkReservation("RES-1","U1",null);
        assertTrue(res.eligible());
    }
}
