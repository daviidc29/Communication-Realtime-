package edu.eci.arsw.calls.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.cors.CorsConfiguration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigTest.DummyController.class)
@Import(SecurityConfigTest.OverrideChain.class)
class SecurityConfigTest {

    @Controller
    static class DummyController {
        @GetMapping("/api/protegido")
        @ResponseBody
        public String protectedEndpoint() { return "ok"; }
    }

    @TestConfiguration
    static class OverrideChain {
        @Bean @Primary
        SecurityFilterChain filterChain(HttpSecurity http, TokenAuthFilter tokenAuthFilter) throws Exception {
            http
              .cors(c -> c.configurationSource(request -> new CorsConfiguration().applyPermitDefaultValues()))
              .csrf(csrf -> csrf.disable())
              .authorizeHttpRequests(auth -> auth
                  .requestMatchers(new AntPathRequestMatcher("/actuator/**")).permitAll()
                  .requestMatchers(new AntPathRequestMatcher("/v3/api-docs/**")).permitAll()
                  .requestMatchers(new AntPathRequestMatcher("/swagger-ui/**")).permitAll()
                  .requestMatchers(new AntPathRequestMatcher("/api/calls/ice-servers")).permitAll()
                  .requestMatchers(new AntPathRequestMatcher("/ws/call/**")).permitAll()
                  .requestMatchers(new AntPathRequestMatcher("/**", HttpMethod.OPTIONS.name())).permitAll()
                  .anyRequest().authenticated())
              .addFilterBefore(tokenAuthFilter, UsernamePasswordAuthenticationFilter.class)
              .httpBasic(b -> b.disable())
              .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
            return http.build();
        }
    }

    @MockBean
    TokenAuthFilter tokenAuthFilter;

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void setup() throws Exception {
        doAnswer(inv -> {
            ServletRequest req = inv.getArgument(0);
            ServletResponse res = inv.getArgument(1);
            FilterChain chain = inv.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(tokenAuthFilter).doFilter(any(), any(), any());
    }

    @Test
    void optionsDebeSerPermitido() throws Exception {
        mvc.perform(options("/lo-que-sea")
                .header(HttpHeaders.ORIGIN, "https://test.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name()))
           .andExpect(status().isOk());
    }

    @Test
    void rutasPermitidasNoRequierenAuth() throws Exception {
        mvc.perform(get("/ws/call/abc")).andExpect(status().isNotFound());
        mvc.perform(get("/api/calls/ice-servers")).andExpect(status().isNotFound());
    }

    @Test
    void rutaProtegida_sinAuth_regresa401() throws Exception {
        mvc.perform(get("/api/protegido"))
           .andExpect(status().is(HttpStatus.UNAUTHORIZED.value()));
    }

    @Test
    void rutaProtegida_conAuthInvalida_regresa401() throws Exception {
        mvc.perform(get("/api/protegido").header(HttpHeaders.AUTHORIZATION, "Bearer x.y.z"))
           .andExpect(status().is(HttpStatus.UNAUTHORIZED.value()));
    }
}