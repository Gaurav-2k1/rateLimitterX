package com.ratelimitx.sdk;

/**
 * RateLimitX API Exception
 */
public class RateLimitXException extends Exception {
    private final int statusCode;
    private final Object response;

    public RateLimitXException(String message, int statusCode, Object response) {
        super(message);
        this.statusCode = statusCode;
        this.response = response;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Object getResponse() {
        return response;
    }
}

