package com.api.gateway.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

// Runs for every request to the gateway. It decides the user identity and passes it on as a header.
@Component
public class SessionIdentityFilter extends OncePerRequestFilter {

    // The header the backend services trust. Only this filter may set it.
    static final String USER_HEADER = "X-DevPilot-User";

    // Paths that need a signed-in user.
    private static final List<String> PROTECTED_PREFIXES = List.of("/api/repos", "/api/indexing", "/api/chat");

    private final RestClient authClient;

    public SessionIdentityFilter(@Value("${devpilot.auth-service.base-url}") String authBaseUrl) {
        this.authClient = RestClient.builder().baseUrl(authBaseUrl).build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        // Check whether login is required
        boolean protectedPath = PROTECTED_PREFIXES.stream().anyMatch(prefix -> path.startsWith(prefix));

        // Public paths: the client's identity header is removed, and none is added.
        if (!protectedPath) {
            filterChain.doFilter(new IdentityRequest(request, null), response);
            return;
        }

        // Protected paths: find the signed-in user from the session cookie.
        String login = findSessionLogin(request.getHeader("Cookie"));
        if (login == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":401,\"detail\":\"Sign in with GitHub first.\"}");
            return;
        }

        // Pass the request on, with the header set to the login just verified.
        filterChain.doFilter(new IdentityRequest(request, login), response);
    }

// ------ Asks auth-service who owns this session. ------
    // Returns the GitHub userName (login), or null if nobody is signed in.
    private String findSessionLogin(String cookieHeader) {
        if (cookieHeader == null || cookieHeader.isBlank()) {
            return null;
        }
        try {
            SessionUser user = authClient.get()
                    .uri("/api/auth/me")
                    .header("Cookie", cookieHeader)
                    .retrieve()
                    .body(SessionUser.class);

            return user == null ? null : user.login();
        }
        catch (RestClientException ex) {
            return null;
        }
    }

// ------ The only field we need from auth-service's /api/auth/me answer ------
    @JsonIgnoreProperties(ignoreUnknown = true)
    record SessionUser(
            String login
    ) {
    }

// ------ Wraps the request so the identity header is exactly what we decided ------
    static class IdentityRequest extends HttpServletRequestWrapper {

        // The GitHub login to pass on.
        private final String login;

        IdentityRequest(HttpServletRequest request, String login) {
            super(request);
            this.login = login;
        }

        @Override
        public String getHeader(String name) {
            return USER_HEADER.equalsIgnoreCase(name) ? login : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (USER_HEADER.equalsIgnoreCase(name)) {
                return login == null ? Collections.emptyEnumeration() : Collections.enumeration(List.of(login));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            // Remove the client's copy of the header from the list of names, then add ours if we have one.
            List<String> names = Collections.list(super.getHeaderNames()).stream()
                    .filter(name -> !USER_HEADER.equalsIgnoreCase(name))
                    .collect(Collectors.toCollection(ArrayList::new));
            if (login != null) {
                names.add(USER_HEADER);
            }

            return Collections.enumeration(names);
        }
    }
}
