package com.ratelimitx.service;

import com.ratelimitx.common.entity.ApiKey;
import com.ratelimitx.common.entity.RateLimitRule;
import com.ratelimitx.common.entity.Tenant;
import com.ratelimitx.repository.ApiKeyRepository;
import com.ratelimitx.repository.RateLimitRuleRepository;
import com.ratelimitx.repository.TenantRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final TenantRepository tenantRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final RateLimitRuleRepository ruleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApiKeyService apiKeyService;
    
    @Value("${spring.security.jwt.secret}")
    private String jwtSecret;
    
    @Value("${spring.security.jwt.expiration}")
    private long jwtExpiration;
    
    @Value("${spring.security.jwt.refresh-expiration}")
    private long refreshExpiration;
    
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
    
    @Transactional
    public Map<String, Object> register(String email, String password) {
        if (tenantRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already exists");
        }
        
        Tenant tenant = Tenant.builder()
            .email(email)
            .passwordHash(passwordEncoder.encode(password))
            .tier(Tenant.Tier.FREE)
            .build();
        
        tenant = tenantRepository.save(tenant);
        
        // Generate API key
        String apiKey = apiKeyService.generateApiKey();
        String keyHash = apiKeyService.hashApiKey(apiKey);
        
        ApiKey apiKeyEntity = ApiKey.builder()
            .tenantId(tenant.getId())
            .keyHash(keyHash)
            .name("Default Key")
            .environment("production")
            .active(true)
            .build();
        
        apiKeyRepository.save(apiKeyEntity);
        
        // Create default rate limit rule (100 requests/minute) for FREE tier
        RateLimitRule defaultRule = RateLimitRule.builder()
            .tenantId(tenant.getId())
            .resource("default")
            .algorithm(RateLimitRule.Algorithm.FIXED_WINDOW)
            .maxRequests(100)
            .windowSeconds(60)
            .identifierType(RateLimitRule.IdentifierType.USER_ID)
            .active(true)
            .build();
        
        ruleRepository.save(defaultRule);
        
        Map<String, Object> response = new HashMap<>();
        response.put("tenantId", tenant.getId());
        response.put("apiKey", apiKey);
        response.put("accessToken", generateAccessToken(tenant.getId(), tenant.getEmail()));
        response.put("refreshToken", generateRefreshToken(tenant.getId()));
        
        return response;
    }
    
    public Map<String, Object> login(String email, String password) {
        Tenant tenant = tenantRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Invalid credentials"));
        
        if (!passwordEncoder.matches(password, tenant.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("accessToken", generateAccessToken(tenant.getId(), tenant.getEmail()));
        response.put("refreshToken", generateRefreshToken(tenant.getId()));
        response.put("tenant", Map.of(
            "id", tenant.getId(),
            "email", tenant.getEmail(),
            "tier", tenant.getTier()
        ));
        
        return response;
    }
    
    public String generateAccessToken(UUID tenantId, String email) {
        return generateToken(tenantId, email, jwtExpiration);
    }
    
    public String generateRefreshToken(UUID tenantId) {
        return generateToken(tenantId, null, refreshExpiration);
    }
    
    private String generateToken(UUID tenantId, String email, long expiration) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tenantId", tenantId.toString());
        if (email != null) {
            claims.put("email", email);
        }
        
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(tenantId.toString())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(getSigningKey())
            .compact();
    }
    
    public Claims validateToken(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
}

