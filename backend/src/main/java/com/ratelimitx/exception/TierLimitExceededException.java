package com.ratelimitx.exception;

/**
 * Exception thrown when tier limits are exceeded
 */
public class TierLimitExceededException extends RuntimeException {
    private final String tierName;
    private final int currentUsage;
    private final int limit;
    
    public TierLimitExceededException(String tierName, int currentUsage, int limit) {
        super(String.format("Tier limit exceeded for '%s'. Usage: %d/%d", tierName, currentUsage, limit));
        this.tierName = tierName;
        this.currentUsage = currentUsage;
        this.limit = limit;
    }
    
    public TierLimitExceededException(String message) {
        super(message);
        this.tierName = null;
        this.currentUsage = 0;
        this.limit = 0;
    }
    
    public String getTierName() {
        return tierName;
    }
    
    public int getCurrentUsage() {
        return currentUsage;
    }
    
    public int getLimit() {
        return limit;
    }
}