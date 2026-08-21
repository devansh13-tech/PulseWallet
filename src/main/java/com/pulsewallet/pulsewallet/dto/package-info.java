/**
 * Data transfer objects - the shapes that cross the HTTP boundary.
 *
 * <p>Implemented as Java {@code record}s: immutable, no boilerplate, and Jackson
 * handles them natively. Request DTOs carry Bean Validation annotations
 * ({@code @NotNull}, {@code @Positive}, {@code @Email}) so invalid input is
 * rejected before any service code runs.
 *
 * <p>Keeping DTOs separate from entities means the API contract the React
 * frontend depends on does not break every time a column is added, and it stops
 * fields like {@code passwordHash} from leaking into a JSON response.
 */
package com.pulsewallet.pulsewallet.dto;
