/**
 * Authentication and authorisation plumbing (Milestone 2).
 *
 * <p>Empty for now. Planned contents:
 * <ul>
 *   <li>{@code JwtService} - sign and verify tokens</li>
 *   <li>{@code JwtAuthenticationFilter} - a {@code OncePerRequestFilter} that
 *       reads the {@code Authorization: Bearer} header and populates the
 *       {@code SecurityContext}</li>
 *   <li>{@code CustomUserDetailsService} - loads a {@code User} by email</li>
 * </ul>
 *
 * <p>Note for whoever implements this: the project targets Spring Boot 4, which
 * ships Spring Security 7. Most JWT tutorials online target Security 6 and will
 * not compile as written. Check the current reference documentation rather than
 * copying a blog post.
 */
package com.pulsewallet.pulsewallet.security;
