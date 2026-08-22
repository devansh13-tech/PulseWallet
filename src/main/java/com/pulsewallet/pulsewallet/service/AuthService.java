package com.pulsewallet.pulsewallet.service;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pulsewallet.pulsewallet.dto.AuthResponse;
import com.pulsewallet.pulsewallet.dto.LoginRequest;
import com.pulsewallet.pulsewallet.dto.RegisterRequest;
import com.pulsewallet.pulsewallet.entity.User;
import com.pulsewallet.pulsewallet.exception.DuplicateResourceException;
import com.pulsewallet.pulsewallet.repository.UserRepository;
import com.pulsewallet.pulsewallet.security.JwtService;
import com.pulsewallet.pulsewallet.security.UserPrincipal;

/**
 * Registration and login. Kept free of HTTP types so it stays unit-testable
 * with plain Mockito, per the convention in {@code service/package-info.java}.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException("An account with this email already exists");
        }

        User user = new User(
                request.name().trim(),
                normalizedEmail,
                passwordEncoder.encode(request.password()));
        user = userRepository.save(user);

        String token = jwtService.generateToken(new UserPrincipal(user));
        return AuthResponse.of(token, jwtService.getExpirationSeconds(), user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        // Same exception and message whether the email is unknown or the
        // password is wrong - the response must never confirm which emails
        // are registered.
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String token = jwtService.generateToken(new UserPrincipal(user));
        return AuthResponse.of(token, jwtService.getExpirationSeconds(), user);
    }
}
