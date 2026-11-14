package com.auth.service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

// The security rules for every request this service receives.
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, @Value("${devpilot.frontend-url}") String frontendUrl) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                // Starting a login, the GitHub callback, and the health check must be open.
                // Everything else, including /me, needs a signed-in user.
                .requestMatchers("/api/auth/login/**", "/login/oauth2/**", "/actuator/health").permitAll()
                .anyRequest().authenticated())

            // For API calls, return 401 Unauthorized instead of redirecting to a login page.
            .exceptionHandling(e -> e.defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    request -> request.getRequestURI().startsWith("/api")))

            .oauth2Login(oauth -> oauth
                    // The login starts at /api/auth/login/github (instead of the default /oauth2/authorization/github).
                    .authorizationEndpoint(a -> a.baseUri("/api/auth/login"))
                    // After login, send the browser to the Angular app.
                    .defaultSuccessUrl(frontendUrl, true));

        return http.build();
    }
}
