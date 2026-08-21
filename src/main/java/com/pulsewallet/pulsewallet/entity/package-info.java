/**
 * JPA entities - the persistent data model.
 *
 * <p>Planned for Milestone 2: {@code User}, {@code Transaction},
 * {@code Category}, {@code Budget}.
 *
 * <p>Conventions to hold to:
 * <ul>
 *   <li>Money is {@code BigDecimal} with an explicit scale, never {@code double}.
 *       Floating point rounding errors in a finance app are indefensible in a
 *       viva.</li>
 *   <li>Timestamps are {@code Instant} (UTC) or {@code LocalDate} for calendar
 *       dates such as a budget month.</li>
 *   <li>Relationships are {@code FetchType.LAZY} by default; {@code EAGER}
 *       fetching is what turns the dashboard query into an N+1 problem.</li>
 *   <li>Entities stay out of controller signatures - map to a DTO instead.</li>
 * </ul>
 */
package com.pulsewallet.pulsewallet.entity;
