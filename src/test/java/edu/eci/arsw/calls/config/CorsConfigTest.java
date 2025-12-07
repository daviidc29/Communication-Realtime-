package edu.eci.arsw.calls.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;

import static org.junit.jupiter.api.Assertions.*;

class CorsConfigTest {

    @Test
    void corsConfigurationSource_deberiaExponerContentTypeYAuthorization() {
        CorsConfig cfg = new CorsConfig();
        ReflectionTestUtils.setField(cfg, "allowedOrigins", "http://localhost:3000");

        CorsConfiguration conf = cfg.corsConfigurationSource()
                .getCorsConfiguration(new MockHttpServletRequest());

        assertTrue(conf.getExposedHeaders().contains("Content-Type"));
        assertTrue(conf.getExposedHeaders().contains("Authorization"));
    }
}
