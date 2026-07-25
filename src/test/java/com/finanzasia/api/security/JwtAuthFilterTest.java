package com.finanzasia.api.security;

import com.finanzasia.domain.model.AuthenticatedUser;
import com.finanzasia.domain.port.in.AuthenticateAccessTokenUseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private AuthenticateAccessTokenUseCase authenticateAccessToken;

    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter(authenticateAccessToken);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("a request with no Authorization header passes through untouched")
    void noAuthorizationHeaderPassesThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("an Authorization header without the Bearer prefix passes through untouched")
    void nonBearerHeaderPassesThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("a Bearer token that fails authentication clears the security context")
    void unusableTokenClearsContext() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("stale", null, List.of()));
        when(authenticateAccessToken.authenticate("bad-token")).thenReturn(Optional.empty());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Nested
    @DisplayName("a Bearer token that authenticates successfully")
    class SuccessfulAuthentication {

        @Test
        @DisplayName("populates the security context when it was previously empty")
        void populatesEmptyContext() throws Exception {
            UUID userId = UUID.randomUUID();
            when(authenticateAccessToken.authenticate("good-token"))
                    .thenReturn(Optional.of(new AuthenticatedUser(userId, "user@example.com")));

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer good-token");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(request, response, chain);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.getPrincipal()).isInstanceOf(UserPrincipal.class);
            assertThat(((UserPrincipal) auth.getPrincipal()).getId()).isEqualTo(userId);
            assertThat(chain.getRequest()).isNotNull();
        }

        @Test
        @DisplayName("does not overwrite an authentication that is already present")
        void doesNotOverwriteExistingAuthentication() throws Exception {
            Authentication existing = new UsernamePasswordAuthenticationToken("already-set", null, List.of());
            SecurityContextHolder.getContext().setAuthentication(existing);
            when(authenticateAccessToken.authenticate("good-token"))
                    .thenReturn(Optional.of(new AuthenticatedUser(UUID.randomUUID(), "user@example.com")));

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer good-token");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(request, response, chain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existing);
        }
    }
}
