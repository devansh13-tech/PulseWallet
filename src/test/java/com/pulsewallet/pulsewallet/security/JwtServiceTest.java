package com.pulsewallet.pulsewallet.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.pulsewallet.pulsewallet.entity.User;
import com.pulsewallet.pulsewallet.support.TestEntities;

/**
 * Exercises {@link JwtService} directly with no Spring context and no
 * database - it only needs a secret string, exactly like production.
 */
class JwtServiceTest {

    // 32+ bytes, same shape as the placeholder in .env.example.
    private static final String TEST_SECRET = "unit-test-secret-key-at-least-32-bytes-long-enough";

    private JwtService jwtService;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET, 60);
        User user = TestEntities.withId(
                new User("Ada Lovelace", "ada@example.com", "irrelevant-hash"), 42L);
        principal = new UserPrincipal(user);
    }

    @Test
    void generateToken_producesATokenThatIsValidForTheSamePrincipal() {
        String token = jwtService.generateToken(principal);

        assertThat(token).isNotBlank();
        assertThat(jwtService.isValid(token, principal)).isTrue();
    }

    @Test
    void extractUsername_returnsTheEmailUsedAsTheSubject() {
        String token = jwtService.generateToken(principal);

        assertThat(jwtService.extractUsername(token)).isEqualTo("ada@example.com");
    }

    @Test
    void extractUserId_returnsTheIdEncodedAtGeneration() {
        String token = jwtService.generateToken(principal);

        assertThat(jwtService.extractUserId(token)).isEqualTo(42L);
    }

    @Test
    void isValid_rejectsATokenSignedWithADifferentSecret() {
        JwtService otherService = new JwtService("a-completely-different-secret-that-is-also-32-bytes", 60);
        String token = otherService.generateToken(principal);

        assertThat(jwtService.isValid(token, principal)).isFalse();
    }

    @Test
    void isValid_rejectsAnExpiredToken() throws InterruptedException {
        // Expiration is expressed in whole minutes, so the smallest unit we
        // can exercise without mocking the clock is "already past" via a
        // service configured with a negative/zero window.
        JwtService almostExpiredService = new JwtService(TEST_SECRET, 0);
        String token = almostExpiredService.generateToken(principal);

        // A 0-minute expiration means issuedAt == expiration; give the clock
        // a moment to tick past it.
        Thread.sleep(50);

        assertThat(jwtService.isValid(token, principal)).isFalse();
    }

    @Test
    void isValid_rejectsAMalformedToken() {
        assertThat(jwtService.isValid("not-a-real-token", principal)).isFalse();
    }

    @Test
    void isValid_rejectsATokenIssuedForADifferentUser() {
        User otherUser = TestEntities.withId(
                new User("Bob", "bob@example.com", "irrelevant-hash"), 7L);
        UserPrincipal otherPrincipal = new UserPrincipal(otherUser);
        String tokenForBob = jwtService.generateToken(otherPrincipal);

        assertThat(jwtService.isValid(tokenForBob, principal)).isFalse();
    }
}
