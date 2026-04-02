package com.shopease.security;

import com.shopease.entity.Role;
import com.shopease.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private User user;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        String secret = "dGVzdFNlY3JldEtleUZvclRlc3RpbmdQdXJwb3Nlc09ubHkxMjM0NTY3ODkwYWJjZGVmZ2hpams=";
        long expirationMs = 3600000;
        long refreshExpirationMs = 86400000;

        tokenProvider = new JwtTokenProvider(secret, expirationMs, refreshExpirationMs);

        user = User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("password")
                .role(Role.USER)
                .enabled(true)
                .build();

        userPrincipal = UserPrincipal.create(user);
    }

    @Test
    @DisplayName("Should generate valid JWT token")
    void shouldGenerateValidJwtToken() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userPrincipal, null, userPrincipal.getAuthorities());

        String token = tokenProvider.generateToken(authentication);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(tokenProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("Should extract user ID from token")
    void shouldExtractUserIdFromToken() {
        String token = tokenProvider.generateToken(userPrincipal);

        Long userId = tokenProvider.getUserIdFromToken(token);

        assertThat(userId).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should extract email from token")
    void shouldExtractEmailFromToken() {
        String token = tokenProvider.generateToken(userPrincipal);

        String email = tokenProvider.getEmailFromToken(token);

        assertThat(email).isEqualTo("john@example.com");
    }

    @Test
    @DisplayName("Should return false for invalid token")
    void shouldReturnFalseForInvalidToken() {
        String invalidToken = "invalid.jwt.token";

        boolean isValid = tokenProvider.validateToken(invalidToken);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should generate refresh token")
    void shouldGenerateRefreshToken() {
        String refreshToken = tokenProvider.generateRefreshToken(userPrincipal);

        assertThat(refreshToken).isNotNull();
        assertThat(refreshToken).isNotEmpty();
        assertThat(tokenProvider.validateToken(refreshToken)).isTrue();
    }
}
