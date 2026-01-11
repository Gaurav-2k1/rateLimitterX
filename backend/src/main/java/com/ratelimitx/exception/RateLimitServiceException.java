package com.ratelimitx.exception;

/**
 * Exception thrown when rate limit service encounters an error
 */
public class RateLimitServiceException extends RuntimeException {
    public RateLimitServiceException(String message) {
        super(message);
    }
    
    public RateLimitServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}