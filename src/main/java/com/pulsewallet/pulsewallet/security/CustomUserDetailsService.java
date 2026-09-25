package com.pulsewallet.pulsewallet.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.pulsewallet.pulsewallet.entity.User;
import com.pulsewallet.pulsewallet.repository.UserRepository;

/**
 * Loads a {@link User} by email (our login identifier) and wraps it as a
 * {@link UserPrincipal}. Used both by {@link JwtAuthenticationFilter} to
 * rebuild the security context from a valid token, and internally by Spring
 * Security if password-based authentication is ever wired through an
 * {@code AuthenticationManager} directly.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No account with email " + email));
        return new UserPrincipal(user);
    }
}
