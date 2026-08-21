package com.pulsewallet.pulsewallet.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Allows the React dev server (Milestone 6) to call this API from a different
 * origin.
 *
 * <p>Origins come from {@code pulsewallet.cors.allowed-origins}, which resolves
 * from the {@code CORS_ALLOWED_ORIGINS} environment variable. That matters for
 * Milestone 8: the deployed frontend will sit on a hosting domain, and hardcoding
 * localhost here would mean a code change to deploy.
 *
 * <p>Note that {@code allowCredentials(true)} forbids a {@code *} wildcard origin
 * per the CORS specification, which is why origins are listed explicitly.
 *
 * <p>When Spring Security arrives in Milestone 2, this configuration must be
 * referenced from the security filter chain via {@code http.cors(...)}, otherwise
 * the security filters reject preflight {@code OPTIONS} requests before MVC ever
 * applies these rules.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final List<String> allowedOrigins;

    public CorsConfig(
            @Value("${pulsewallet.cors.allowed-origins}") List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                // Lets the browser read the token header on login responses.
                .exposedHeaders("Authorization")
                .allowCredentials(true)
                // Cache preflight for an hour to cut request volume in the SPA.
                .maxAge(3600);
    }
}
