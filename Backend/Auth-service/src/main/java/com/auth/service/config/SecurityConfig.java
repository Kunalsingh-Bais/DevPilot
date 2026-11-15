package com.auth.service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// The security rules for every request this service receives.
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, @Value("${devpilot.frontend-url}") String frontendUrl) throws Exception {

        // The SPA setup: the cookie holds the raw token, and Angular echoes it back in a header.
        // Setting the attribute name to null makes Spring compare the header with the raw cookie value.
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();

        requestHandler.setCsrfRequestAttributeName(null);

        http
            .csrf(csrf -> csrf
                    // withHttpOnlyFalse lets Angular read the XSRF-TOKEN cookie.
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(requestHandler))

            .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)

            .authorizeHttpRequests(auth -> auth
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
                    .defaultSuccessUrl(frontendUrl, true))

            // Logout is a POST, which the CSRF check protects. The response is 204 with no redirect,
            .logout(logout -> logout
                    .logoutUrl("/api/auth/logout")
                    .deleteCookies("JSESSIONID")
                    .logoutSuccessHandler((request, response, authentication) ->
                            response.setStatus(HttpStatus.NO_CONTENT.value())));

        return http.build();
    }

    // Forces Spring to create the CSRF token on every request, so the cookie is always written.
    static class CsrfCookieFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfToken != null) {
                csrfToken.getToken();
            }

            filterChain.doFilter(request, response);
        }
    }
}
