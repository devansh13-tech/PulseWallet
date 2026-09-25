/**
 * Business logic. Everything that makes PulseWallet more than a database with
 * HTTP in front of it lives here.
 *
 * <p>Planned contents by milestone:
 * <ul>
 *   <li>Milestone 2 - {@code AuthService}, {@code TransactionService},
 *       {@code CategoryService}</li>
 *   <li>Milestone 3 - {@code BudgetService} (salary minus expenses to disposable
 *       income), {@code CategorizationService}, {@code ForecastService},
 *       {@code AdvisoryService} (emergency fund, medical fund, SIP suggestions)</li>
 *   <li>Milestone 5 - {@code FraudCheckService} (calls the Python model service),
 *       {@code DashboardService} (aggregates budget plus fraud data)</li>
 * </ul>
 *
 * <p>Services are the unit under test in Milestone 7. Keep them free of HTTP
 * types ({@code ResponseEntity}, {@code HttpServletRequest}) so they can be
 * tested without starting a web context.
 */
package com.pulsewallet.pulsewallet.service;
