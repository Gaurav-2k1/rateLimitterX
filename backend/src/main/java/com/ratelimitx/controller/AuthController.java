package com.ratelimitx.controller;

import com.ratelimitx.common.dto.ApiResponse;
import com.ratelimitx.service.AuthService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(@RequestBody RegisterRequest request) {
        try {
            Map<String, Object> result = authService.register(request.getEmail(), request.getPassword());
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@RequestBody LoginRequest request) {
        try {
            Map<String, Object> result = authService.login(request.getEmail(), request.getPassword());
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refresh(@RequestBody RefreshRequest request) {
        try {
            // Validate refresh token and generate new access token
            var claims = authService.validateToken(request.getRefreshToken());
            String tenantId = claims.get("tenantId", String.class);
            String email = claims.get("email", String.class);
            
            Map<String, Object> result = Map.of(
                "accessToken", authService.generateAccessToken(
                    java.util.UUID.fromString(tenantId), email)
            );
            
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid refresh token"));
        }
    }
    
    @Data
    static class RegisterRequest {
        private String email;
        private String password;
    }
    
    @Data
    static class LoginRequest {
        private String email;
        private String password;
    }
    
    @Data
    static class RefreshRequest {
        private String refreshToken;
    }
}

