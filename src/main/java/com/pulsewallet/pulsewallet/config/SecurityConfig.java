package com.pulsewallet.pulsewallet.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.pulsewallet.pulsewallet.security.CustomUserDetailsService;
import com.pulsewallet.pulsewallet.security.JwtAuthenticationFilter;
import com.pulsewallet.pulsewallet.security.JwtService;
import com.pulsewallet.pulsewallet.security.RestAccessDeniedHandler;
import com.pulsewallet.pulsewallet.security.RestAuthenticationEntryPoint;

/**
 * Milestone 2 security wiring: stateless JWT authentication, BCrypt password
 * hashing, and a single rule set - {@code /api/auth/**}, the health check,
 * and Actuator are public; everything else requires a valid bearer token.
 *
 * <p>
 * Built for Spring Security 7 (ships with Spring Boot 4). Most JWT
 * tutorials online target Security 6 and use APIs removed here (e.g.
 * {@code WebSecurityConfigurerAdapter}); this class intentionally avoids
 * those.
 *
 * <p>
 * Deliberately has no {@code AuthenticationManager}/
 * {@code DaoAuthenticationProvider} bean: {@code AuthService} checks
 * credentials directly against {@link CustomUserDetailsService} and
 * {@link PasswordEncoder} rather than going through
 * {@code AuthenticationManager.authenticate(...)}. Wiring both paths at once
 * is a common source of "why did my password check run twice" bugs; add the
 * manager bean back only if something actually needs to call it.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(
            CustomUserDetailsService userDetailsService,
            JwtService jwtService,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler) {
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtService, userDetailsService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CORS policy lives in CorsConfig#corsConfigurationSource; this just
                // tells Security to apply it before rejecting cross-origin preflights.
                .cors(Customizer.withDefaults())
                // Stateless bearer-token API - no cookies/sessions to forge, so CSRF
                // protection (designed for cookie-based auth) does not apply here.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
