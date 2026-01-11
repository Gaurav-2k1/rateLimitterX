package com.ratelimitx.sdk;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.ratelimitx.sdk.Models.*;

/**
 * RateLimitX Java SDK Client
 */
public class RateLimitX {
    private static final String DEFAULT_BASE_URL = "http://localhost:8080";
    private static final int DEFAULT_TIMEOUT = 30;

    private final String baseUrl;
    private final OkHttpClient client;
    private final Gson gson;
    private String apiKey;
    private String accessToken;

    public RateLimitX() {
        this(DEFAULT_BASE_URL, null, null);
    }

    public RateLimitX(String baseUrl) {
        this(baseUrl, null, null);
    }

    public RateLimitX(String baseUrl, String apiKey) {
        this(baseUrl, apiKey, null);
    }

    public RateLimitX(String baseUrl, String apiKey, String accessToken) {
        this.baseUrl = baseUrl != null ? baseUrl.replaceAll("/$", "") : DEFAULT_BASE_URL;
        this.apiKey = apiKey;
        this.accessToken = accessToken;
        this.gson = new GsonBuilder().create();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .build();
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    private Request.Builder buildRequest(String endpoint) {
        Request.Builder builder = new Request.Builder()
                .url(baseUrl + endpoint);

        if (apiKey != null) {
            builder.addHeader("X-API-Key", apiKey);
        } else if (accessToken != null) {
            builder.addHeader("Authorization", "Bearer " + accessToken);
        }

        return builder;
    }

    private <T> T execute(Request request, Class<T> responseClass) throws RateLimitXException {
        try (Response response = client.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : null;

            if (!response.isSuccessful()) {
                ApiResponse<?> errorResponse = gson.fromJson(body, ApiResponse.class);
                throw new RateLimitXException(
                        errorResponse != null && errorResponse.getError() != null
                                ? errorResponse.getError()
                                : "Request failed",
                        response.code(),
                        errorResponse
                );
            }

            if (body == null || body.isEmpty()) {
                return null;
            }

            // Handle ApiResponse wrapper
            try {
                Type responseType = TypeToken.getParameterized(ApiResponse.class, responseClass).getType();
                ApiResponse<T> apiResponse = gson.fromJson(body, responseType);
                if (apiResponse != null && apiResponse.getSuccess() != null && apiResponse.getSuccess()) {
                    return apiResponse.getData();
                }
            } catch (Exception e) {
                // Direct response (for check endpoint)
            }

            // Direct response (for check endpoint)
            return gson.fromJson(body, responseClass);
        } catch (IOException e) {
            throw new RateLimitXException("Request failed: " + e.getMessage(), 0, null);
        }
    }

    private <T> List<T> executeList(Request request, Class<T> itemClass) throws RateLimitXException {
        try (Response response = client.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : null;

            if (!response.isSuccessful()) {
                ApiResponse<?> errorResponse = gson.fromJson(body, ApiResponse.class);
                throw new RateLimitXException(
                        errorResponse != null && errorResponse.getError() != null
                                ? errorResponse.getError()
                                : "Request failed",
                        response.code(),
                        errorResponse
                );
            }

            if (body == null || body.isEmpty()) {
                return null;
            }

            Type listType = TypeToken.getParameterized(List.class, itemClass).getType();
            ApiResponse<List<T>> apiResponse = gson.fromJson(body,
                    TypeToken.getParameterized(ApiResponse.class, listType).getType());
            if (apiResponse != null && apiResponse.getSuccess() != null && apiResponse.getSuccess()) {
                return apiResponse.getData();
            }

            return null;
        } catch (IOException e) {
            throw new RateLimitXException("Request failed: " + e.getMessage(), 0, null);
        }
    }

    /**
     * Check if a request should be rate limited
     */
    public RateLimitCheckResponse check(RateLimitCheckRequest request) throws RateLimitXException {
        Request httpRequest = buildRequest("/api/v1/check")
                .post(RequestBody.create(gson.toJson(request), MediaType.get("application/json")))
                .build();
        return execute(httpRequest, RateLimitCheckResponse.class);
    }

    /**
     * Register a new user/tenant
     */
    public AuthResponse register(String email, String password) throws RateLimitXException {
        Map<String, String> data = new HashMap<>();
        data.put("email", email);
        data.put("password", password);

        Request httpRequest = buildRequest("/auth/register")
                .post(RequestBody.create(gson.toJson(data), MediaType.get("application/json")))
                .build();

        AuthResponse auth = execute(httpRequest, AuthResponse.class);
        if (auth != null && auth.getAccessToken() != null) {
            setAccessToken(auth.getAccessToken());
        }
        return auth;
    }

    /**
     * Login and get access token
     */
    public AuthResponse login(String email, String password) throws RateLimitXException {
        Map<String, String> data = new HashMap<>();
        data.put("email", email);
        data.put("password", password);

        Request httpRequest = buildRequest("/auth/login")
                .post(RequestBody.create(gson.toJson(data), MediaType.get("application/json")))
                .build();

        AuthResponse auth = execute(httpRequest, AuthResponse.class);
        if (auth != null && auth.getAccessToken() != null) {
            setAccessToken(auth.getAccessToken());
        }
        return auth;
    }

    /**
     * Refresh access token
     */
    public AuthResponse refreshToken(String refreshToken) throws RateLimitXException {
        Map<String, String> data = new HashMap<>();
        data.put("refreshToken", refreshToken);

        Request httpRequest = buildRequest("/auth/refresh")
                .post(RequestBody.create(gson.toJson(data), MediaType.get("application/json")))
                .build();

        AuthResponse auth = execute(httpRequest, AuthResponse.class);
        if (auth != null && auth.getAccessToken() != null) {
            setAccessToken(auth.getAccessToken());
        }
        return auth;
    }

    /**
     * Get all rate limit rules
     */
    public List<RateLimitRule> getRules() throws RateLimitXException {
        Request httpRequest = buildRequest("/rules").get().build();
        return executeList(httpRequest, RateLimitRule.class);
    }

    /**
     * Create a new rate limit rule
     */
    public RateLimitRule createRule(CreateRuleRequest request) throws RateLimitXException {
        Request httpRequest = buildRequest("/rules")
                .post(RequestBody.create(gson.toJson(request), MediaType.get("application/json")))
                .build();
        return execute(httpRequest, RateLimitRule.class);
    }

    /**
     * Update a rate limit rule
     */
    public RateLimitRule updateRule(String ruleId, UpdateRuleRequest request) throws RateLimitXException {
        Request httpRequest = buildRequest("/rules/" + ruleId)
                .put(RequestBody.create(gson.toJson(request), MediaType.get("application/json")))
                .build();
        return execute(httpRequest, RateLimitRule.class);
    }

    /**
     * Delete a rate limit rule
     */
    public void deleteRule(String ruleId) throws RateLimitXException {
        Request httpRequest = buildRequest("/rules/" + ruleId).delete().build();
        execute(httpRequest, Void.class);
    }

    /**
     * Get all API keys
     */
    public List<ApiKey> getApiKeys() throws RateLimitXException {
        Request httpRequest = buildRequest("/api-keys").get().build();
        return executeList(httpRequest, ApiKey.class);
    }

    /**
     * Create a new API key
     */
    public CreateApiKeyResponse createApiKey(String name, String environment) throws RateLimitXException {
        Map<String, String> data = new HashMap<>();
        data.put("name", name);
        data.put("environment", environment);

        Request httpRequest = buildRequest("/api-keys")
                .post(RequestBody.create(gson.toJson(data), MediaType.get("application/json")))
                .build();
        return execute(httpRequest, CreateApiKeyResponse.class);
    }

    /**
     * Delete an API key
     */
    public void deleteApiKey(String keyId) throws RateLimitXException {
        Request httpRequest = buildRequest("/api-keys/" + keyId).delete().build();
        execute(httpRequest, Void.class);
    }

    /**
     * Rotate an API key
     */
    public RotateApiKeyResponse rotateApiKey(String keyId) throws RateLimitXException {
        Request httpRequest = buildRequest("/api-keys/" + keyId + "/rotate")
                .post(RequestBody.create("{}", MediaType.get("application/json")))
                .build();
        return execute(httpRequest, RotateApiKeyResponse.class);
    }

    /**
     * Get real-time analytics
     */
    public AnalyticsMetrics getRealtimeAnalytics() throws RateLimitXException {
        Request httpRequest = buildRequest("/analytics/realtime").get().build();
        return execute(httpRequest, AnalyticsMetrics.class);
    }

    /**
     * Get top rate-limited identifiers
     */
    public List<TopIdentifier> getTopIdentifiers(int limit) throws RateLimitXException {
        Request httpRequest = buildRequest("/analytics/top-identifiers?limit=" + limit).get().build();
        return executeList(httpRequest, TopIdentifier.class);
    }

    /**
     * Get analytics trends
     */
    public Map<String, Object> getTrends(String start, String end) throws RateLimitXException {
        String url = "/analytics/trends";
        if (start != null || end != null) {
            url += "?";
            if (start != null) url += "start=" + start;
            if (end != null) url += (start != null ? "&" : "") + "end=" + end;
        }
        Request httpRequest = buildRequest(url).get().build();
        return execute(httpRequest, Map.class);
    }

    /**
     * Get all alert configurations
     */
    public List<AlertConfiguration> getAlerts() throws RateLimitXException {
        Request httpRequest = buildRequest("/alerts").get().build();
        return executeList(httpRequest, AlertConfiguration.class);
    }

    /**
     * Create an alert configuration
     */
    public AlertConfiguration createAlert(CreateAlertRequest request) throws RateLimitXException {
        Request httpRequest = buildRequest("/alerts")
                .post(RequestBody.create(gson.toJson(request), MediaType.get("application/json")))
                .build();
        return execute(httpRequest, AlertConfiguration.class);
    }

    /**
     * Delete an alert configuration
     */
    public void deleteAlert(String alertId) throws RateLimitXException {
        Request httpRequest = buildRequest("/alerts/" + alertId).delete().build();
        execute(httpRequest, Void.class);
    }

    /**
     * Export rules as JSON or YAML
     */
    public String exportRules(String format) throws RateLimitXException {
        Request httpRequest = buildRequest("/bulk/export?format=" + format).get().build();
        try (Response response = client.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new RateLimitXException("Export failed", response.code(), null);
            }
            return response.body() != null ? response.body().string() : "";
        } catch (IOException e) {
            throw new RateLimitXException("Export failed: " + e.getMessage(), 0, null);
        }
    }
}

