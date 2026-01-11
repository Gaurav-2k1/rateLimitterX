package com.ratelimitx.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitCheckRequest {
    @NotBlank(message = "Identifier is required")
    private String identifier;
    
    @NotBlank(message = "Resource is required")
    private String resource;
    
    @NotNull(message = "Tokens is required")
    @Positive(message = "Tokens must be positive")
    @Builder.Default
    private Integer tokens = 1;
}

