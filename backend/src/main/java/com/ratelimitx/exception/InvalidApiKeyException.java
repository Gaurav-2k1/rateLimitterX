
package com.ratelimitx.exception;

/**
 * Exception thrown when an invalid API key is provided
 */
public class InvalidApiKeyException extends RuntimeException {
    public InvalidApiKeyException(String message) {
        super(message);
    }
    
    public InvalidApiKeyException(String message, Throwable cause) {
        super(message, cause);
    }
}