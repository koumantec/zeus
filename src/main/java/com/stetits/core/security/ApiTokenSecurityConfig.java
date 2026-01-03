package com.stetits.core.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;

@Configuration
public class ApiTokenSecurityConfig {

    @Value("${core.api.token}")
    String token;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        // OpenAPI + Swagger + health
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/actuator/health").permitAll()
                        // READ allowed
                        .requestMatchers(HttpMethod.GET, "/**").permitAll()
                        // WRITE requires token
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new BearerTokenFilter(token), org.springframework.security.web.authentication.AnonymousAuthenticationFilter.class);

        return http.build();
    }

    static final class BearerTokenFilter extends OncePerRequestFilter {
        private final String token;
        BearerTokenFilter(String token) { this.token = token; }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {

            if (HttpMethod.GET.matches(request.getMethod())) {
                filterChain.doFilter(request, response);
                return;
            }

            String auth = request.getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                String provided = auth.substring("Bearer ".length()).trim();
                if (provided.equals(token)) {
                    // authenticated enough for our needs (no user principal)
                    filterChain.doFilter(request, response);
                    return;
                }
            }

            response.setStatus(401);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized\"}");
        }
    }
}
