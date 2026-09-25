package edu.eci.arsw.calls.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
public class SecurityConfig {

    private final TokenAuthFilter tokenAuthFilter;

    public SecurityConfig(TokenAuthFilter tokenAuthFilter) {
        this.tokenAuthFilter = tokenAuthFilter;
    }

    private RequestMatcher csrfIgnoredMatcher() {
        return new OrRequestMatcher(
        PathPatternRequestMatcher.pathPattern("/api/**"),
        PathPatternRequestMatcher.pathPattern("/ws/**"),
        PathPatternRequestMatcher.pathPattern("/actuator/**"),
        PathPatternRequestMatcher.pathPattern("/v3/api-docs/**"),
        PathPatternRequestMatcher.pathPattern("/swagger-ui/**")
        );
    }

    @Bean
    @Order(1)
    public SecurityFilterChain publicChain(HttpSecurity http) throws Exception {
        RequestMatcher publicMatcher = new OrRequestMatcher(
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/api/reviews/tutor/**"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/api/calls/ice-servers"),
            PathPatternRequestMatcher.pathPattern("/actuator/**"),
            PathPatternRequestMatcher.pathPattern("/v3/api-docs/**"),
            PathPatternRequestMatcher.pathPattern("/swagger-ui/**"),
            PathPatternRequestMatcher.pathPattern("/ws/call/**"),
            PathPatternRequestMatcher.pathPattern("/error")
        );

        http
            .securityMatcher(publicMatcher)
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.ignoringRequestMatchers(csrfIgnoredMatcher()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .addFilterBefore(tokenAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .httpBasic(basic -> basic.disable());

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain protectedChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.ignoringRequestMatchers(csrfIgnoredMatcher()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(tokenAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .httpBasic(basic -> basic.disable())
            .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));

        return http.build();
    }
}
