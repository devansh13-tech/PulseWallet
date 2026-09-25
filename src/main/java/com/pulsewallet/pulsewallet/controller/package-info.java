/**
 * REST controllers: the HTTP boundary of the application.
 *
 * <p>Rules for this layer:
 * <ul>
 *   <li>Accept and return DTOs from {@code com.pulsewallet.pulsewallet.dto} only.
 *       Entities must never appear in a controller signature, otherwise the JSON
 *       contract changes every time the database schema does.</li>
 *   <li>No business logic and no repository access. Controllers validate input,
 *       delegate to a service, and map the result to a response.</li>
 *   <li>Do not catch exceptions here. Let them propagate to
 *       {@code GlobalExceptionHandler} so every error has one shape.</li>
 * </ul>
 */
package com.pulsewallet.pulsewallet.controller;
