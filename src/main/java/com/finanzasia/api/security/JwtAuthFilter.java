package com.finanzasia.api.security;

import com.finanzasia.domain.model.AuthenticatedUser;
import com.finanzasia.domain.port.in.AuthenticateAccessTokenUseCase;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Validates the access token from the Authorization header and populates the security
 * context with a {@link UserPrincipal}. Token validation itself is delegated to
 * {@link AuthenticateAccessTokenUseCase}, which reports an unusable token as an empty
 * result so Spring Security's standard 401 path handles unauthenticated requests.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final AuthenticateAccessTokenUseCase authenticateAccessToken;

    public JwtAuthFilter(AuthenticateAccessTokenUseCase authenticateAccessToken) {
        this.authenticateAccessToken = authenticateAccessToken;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        Optional<AuthenticatedUser> caller = authenticateAccessToken.authenticate(token);

        if (caller.isEmpty()) {
            SecurityContextHolder.clearContext();
        } else if (SecurityContextHolder.getContext().getAuthentication() == null) {
            AuthenticatedUser user = caller.get();
            UserPrincipal principal = new UserPrincipal(user.id(), user.email());
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(principal, null, List.of());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
