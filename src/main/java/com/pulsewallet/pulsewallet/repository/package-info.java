/**
 * Spring Data JPA repositories - the only place allowed to talk to the database.
 *
 * <p>Prefer derived query methods ({@code findByUserIdAndDateBetween}) and use
 * {@code @Query} when the derived name would become unreadable. Anything that
 * aggregates across many rows for the dashboard should be a projection or a
 * {@code @Query} returning a DTO interface, not a full entity list filtered in
 * Java.
 *
 * <p>Planned: {@code UserRepository}, {@code TransactionRepository},
 * {@code CategoryRepository}, {@code BudgetRepository} (Milestone 2).
 */
package com.pulsewallet.pulsewallet.repository;
