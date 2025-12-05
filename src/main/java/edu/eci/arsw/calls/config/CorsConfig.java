package edu.eci.arsw.calls.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * Configuración de CORS para la aplicación.
 */
@Configuration
public class CorsConfig {

    @Value("${uplearn.cors.allowed-origins:http://localhost:5173,http://localhost:3000,https://nice-mud-05a4c8f10.3.azurestaticapps.net/}")
    private String allowedOrigins;

    /**
     * Configura la fuente de configuración CORS.
     *
     * @return La fuente de configuración CORS.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();

        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        cfg.setAllowedOriginPatterns(origins);

        cfg.setAllowCredentials(true);

        cfg.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        cfg.setAllowedHeaders(List.of("*"));

        cfg.setExposedHeaders(Arrays.asList("Content-Type", "Authorization"));

        cfg.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
