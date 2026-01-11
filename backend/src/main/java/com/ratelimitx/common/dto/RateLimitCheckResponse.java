package com.ratelimitx.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitCheckResponse {
    private Boolean allowed;
    private Integer remaining;
    private Long resetAt;
    private Integer retryAfter;
}

