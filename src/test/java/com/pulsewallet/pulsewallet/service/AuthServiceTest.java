package com.pulsewallet.pulsewallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.pulsewallet.pulsewallet.dto.AuthResponse;
import com.pulsewallet.pulsewallet.dto.LoginRequest;
import com.pulsewallet.pulsewallet.dto.RegisterRequest;
import com.pulsewallet.pulsewallet.entity.User;
import com.pulsewallet.pulsewallet.exception.DuplicateResourceException;
import com.pulsewallet.pulsewallet.repository.UserRepository;
import com.pulsewallet.pulsewallet.security.JwtService;
import com.pulsewallet.pulsewallet.security.UserPrincipal;
import com.pulsewallet.pulsewallet.support.TestEntities;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void register_savesANewUserWithAHashedPasswordAndReturnsAToken() {
        RegisterRequest request = new RegisterRequest("Ada Lovelace", "Ada@Example.com", "correct-horse-1");
        when(userRepository.existsByEmail("ada@example.com")).thenReturn(false);
        when(passwordEncoder.encode("correct-horse-1")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            return TestEntities.withId(saved, 1L);
        });
        when(jwtService.generateToken(any(UserPrincipal.class))).thenReturn("signed.jwt.token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.register(request);

        assertThat(response.token()).isEqualTo("signed.jwt.token");
        assertThat(response.user().email()).isEqualTo("ada@example.com");
        assertThat(response.user().name()).isEqualTo("Ada Lovelace");
        // Password must never leave the service, in any form.
        assertThat(response.toString()).doesNotContain("correct-horse-1", "bcrypt-hash");

        // Email is normalized (trimmed + lowercased) before the uniqueness check and the save.
        verify(userRepository).existsByEmail("ada@example.com");
    }

    @Test
    void register_rejectsADuplicateEmailWithoutHittingTheDatabaseTwice() {
        RegisterRequest request = new RegisterRequest("Ada", "ada@example.com", "correct-horse-1");
        when(userRepository.existsByEmail("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_returnsATokenForCorrectCredentials() {
        User user = TestEntities.withId(new User("Ada", "ada@example.com", "bcrypt-hash"), 1L);
        LoginRequest request = new LoginRequest("ada@example.com", "correct-horse-1");
        when(userRepository.findByEmail("ada@example.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("correct-horse-1", "bcrypt-hash")).thenReturn(true);
        when(jwtService.generateToken(any(UserPrincipal.class))).thenReturn("signed.jwt.token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("signed.jwt.token");
    }

    @Test
    void login_rejectsAnUnknownEmailWithoutRevealingThatItIsUnknown() {
        LoginRequest request = new LoginRequest("nobody@example.com", "whatever");
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void login_rejectsTheWrongPasswordWithTheSameMessageAsAnUnknownEmail() {
        User user = TestEntities.withId(new User("Ada", "ada@example.com", "bcrypt-hash"), 1L);
        LoginRequest request = new LoginRequest("ada@example.com", "wrong-password");
        when(userRepository.findByEmail("ada@example.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "bcrypt-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password");
    }
}
