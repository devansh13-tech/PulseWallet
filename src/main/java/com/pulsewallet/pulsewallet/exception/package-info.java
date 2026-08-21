/**
 * Exception types and the single global handler that turns them into HTTP
 * responses.
 *
 * <p>The point of centralising this is that the React frontend can be written
 * against exactly one error shape ({@link com.pulsewallet.pulsewallet.dto.ApiError})
 * instead of guessing whether a failure produced a Spring whitelabel page, a
 * bare string, or a stack trace.
 */
package com.pulsewallet.pulsewallet.exception;
