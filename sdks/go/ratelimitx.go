package ratelimitx

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"mime/multipart"
	"net/http"
	"time"
)

const (
	DefaultBaseURL = "http://localhost:8080"
	DefaultTimeout = 30 * time.Second
)

// RateLimitX is the main client for RateLimitX API
type RateLimitX struct {
	baseURL     string
	apiKey      string
	accessToken string
	httpClient  *http.Client
}

// New creates a new RateLimitX client
func New(baseURL string, apiKey string) *RateLimitX {
	if baseURL == "" {
		baseURL = DefaultBaseURL
	}
	return &RateLimitX{
		baseURL:    baseURL,
		apiKey:     apiKey,
		httpClient: &http.Client{Timeout: DefaultTimeout},
	}
}

// SetAccessToken sets the access token for authentication
func (c *RateLimitX) SetAccessToken(token string) {
	c.accessToken = token
}

// SetAPIKey sets the API key for authentication
func (c *RateLimitX) SetAPIKey(key string) {
	c.apiKey = key
}

func (c *RateLimitX) doRequest(method, endpoint string, body interface{}) (*http.Response, error) {
	url := c.baseURL + endpoint

	var reqBody io.Reader
	if body != nil {
		jsonData, err := json.Marshal(body)
		if err != nil {
			return nil, err
		}
		reqBody = bytes.NewBuffer(jsonData)
	}

	req, err := http.NewRequest(method, url, reqBody)
	if err != nil {
		return nil, err
	}

	req.Header.Set("Content-Type", "application/json")
	if c.apiKey != "" {
		req.Header.Set("X-API-Key", c.apiKey)
	} else if c.accessToken != "" {
		req.Header.Set("Authorization", "Bearer "+c.accessToken)
	}

	return c.httpClient.Do(req)
}

func (c *RateLimitX) doRequestWithFile(endpoint string, content []byte, filename, format string) (*http.Response, error) {
	url := c.baseURL + endpoint

	var buf bytes.Buffer
	writer := multipart.NewWriter(&buf)

	// Add file
	part, err := writer.CreateFormFile("file", filename)
	if err != nil {
		return nil, err
	}
	_, err = part.Write(content)
	if err != nil {
		return nil, err
	}

	// Add format
	err = writer.WriteField("format", format)
	if err != nil {
		return nil, err
	}

	err = writer.Close()
	if err != nil {
		return nil, err
	}

	req, err := http.NewRequest("POST", url, &buf)
	if err != nil {
		return nil, err
	}

	req.Header.Set("Content-Type", writer.FormDataContentType())
	if c.apiKey != "" {
		req.Header.Set("X-API-Key", c.apiKey)
	} else if c.accessToken != "" {
		req.Header.Set("Authorization", "Bearer "+c.accessToken)
	}

	return c.httpClient.Do(req)
}

func (c *RateLimitX) parseResponse(resp *http.Response, result interface{}) error {
	defer resp.Body.Close()

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return err
	}

	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		var apiResp APIResponse
		if err := json.Unmarshal(body, &apiResp); err == nil && apiResp.Error != "" {
			return &RateLimitXError{
				Message:    apiResp.Error,
				StatusCode: resp.StatusCode,
			}
		}
		return &RateLimitXError{
			Message:    fmt.Sprintf("Request failed with status %d", resp.StatusCode),
			StatusCode: resp.StatusCode,
		}
	}

	// Handle ApiResponse wrapper
	var apiResp APIResponse
	if err := json.Unmarshal(body, &apiResp); err == nil && apiResp.Success {
		if apiResp.Data != nil {
			dataBytes, _ := json.Marshal(apiResp.Data)
			return json.Unmarshal(dataBytes, result)
		}
	}

	// Direct response (for check endpoint)
	return json.Unmarshal(body, result)
}

// RateLimitXError represents an API error
type RateLimitXError struct {
	Message    string
	StatusCode int
}

func (e *RateLimitXError) Error() string {
	return e.Message
}

// APIResponse is the standard API response wrapper
type APIResponse struct {
	Success   bool        `json:"success"`
	Data      interface{} `json:"data"`
	Error     string      `json:"error"`
	Timestamp string      `json:"timestamp"`
}

// RateLimitCheckRequest represents a rate limit check request
type RateLimitCheckRequest struct {
	Identifier string `json:"identifier"`
	Resource   string `json:"resource"`
	Tokens     int    `json:"tokens"`
}

// RateLimitCheckResponse represents a rate limit check response
type RateLimitCheckResponse struct {
	Allowed    bool  `json:"allowed"`
	Remaining  int   `json:"remaining"`
	ResetAt    int64 `json:"resetAt"`
	RetryAfter int   `json:"retryAfter"`
}

// Check checks if a request should be rate limited
func (c *RateLimitX) Check(req RateLimitCheckRequest) (*RateLimitCheckResponse, error) {
	resp, err := c.doRequest("POST", "/api/v1/check", req)
	if err != nil {
		return nil, err
	}

	var result RateLimitCheckResponse
	if err := c.parseResponse(resp, &result); err != nil {
		return nil, err
	}

	return &result, nil
}

// AuthResponse represents authentication response
type AuthResponse struct {
	AccessToken  string `json:"accessToken"`
	RefreshToken string `json:"refreshToken"`
	APIKey       string `json:"apiKey"`
	TenantID     string `json:"tenantId"`
}

// Register registers a new user/tenant
func (c *RateLimitX) Register(email, password string) (*AuthResponse, error) {
	req := map[string]string{
		"email":    email,
		"password": password,
	}

	resp, err := c.doRequest("POST", "/auth/register", req)
	if err != nil {
		return nil, err
	}

	var result AuthResponse
	if err := c.parseResponse(resp, &result); err != nil {
		return nil, err
	}

	if result.AccessToken != "" {
		c.SetAccessToken(result.AccessToken)
	}

	return &result, nil
}

// Login logs in and gets access token
func (c *RateLimitX) Login(email, password string) (*AuthResponse, error) {
	req := map[string]string{
		"email":    email,
		"password": password,
	}

	resp, err := c.doRequest("POST", "/auth/login", req)
	if err != nil {
		return nil, err
	}

	var result AuthResponse
	if err := c.parseResponse(resp, &result); err != nil {
		return nil, err
	}

	if result.AccessToken != "" {
		c.SetAccessToken(result.AccessToken)
	}

	return &result, nil
}

// RefreshToken refreshes the access token
func (c *RateLimitX) RefreshToken(refreshToken string) (*AuthResponse, error) {
	req := map[string]string{
		"refreshToken": refreshToken,
	}

	resp, err := c.doRequest("POST", "/auth/refresh", req)
	if err != nil {
		return nil, err
	}

	var result AuthResponse
	if err := c.parseResponse(resp, &result); err != nil {
		return nil, err
	}

	if result.AccessToken != "" {
		c.SetAccessToken(result.AccessToken)
	}

	return &result, nil
}

// RateLimitRule represents a rate limit rule
type RateLimitRule struct {
	ID             string `json:"id"`
	Resource       string `json:"resource"`
	Algorithm      string `json:"algorithm"`
	MaxRequests    int    `json:"maxRequests"`
	WindowSeconds  int    `json:"windowSeconds"`
	BurstCapacity  *int   `json:"burstCapacity,omitempty"`
	IdentifierType string `json:"identifierType"`
	LimitScope     string `json:"limitScope"`
	Priority       int    `json:"priority"`
	Active         bool   `json:"active"`
	CreatedAt      string `json:"createdAt"`
	UpdatedAt      string `json:"updatedAt"`
}

// GetRules gets all rate limit rules
func (c *RateLimitX) GetRules() ([]RateLimitRule, error) {
	resp, err := c.doRequest("GET", "/rules", nil)
	if err != nil {
		return nil, err
	}

	var result []RateLimitRule
	if err := c.parseResponse(resp, &result); err != nil {
		return nil, err
	}

	return result, nil
}

// CreateRuleRequest represents a create rule request
type CreateRuleRequest struct {
	Resource       string `json:"resource"`
	Algorithm      string `json:"algorithm"`
	MaxRequests    int    `json:"maxRequests"`
	WindowSeconds  int    `json:"windowSeconds"`
	BurstCapacity  *int   `json:"burstCapacity,omitempty"`
	IdentifierType string `json:"identifierType,omitempty"`
}

// CreateRule creates a new rate limit rule
func (c *RateLimitX) CreateRule(req CreateRuleRequest) (*RateLimitRule, error) {
	resp, err := c.doRequest("POST", "/rules", req)
	if err != nil {
		return nil, err
	}

	var result RateLimitRule
	if err := c.parseResponse(resp, &result); err != nil {
		return nil, err
	}

	return &result, nil
}

// UpdateRuleRequest represents an update rule request
type UpdateRuleRequest struct {
	Resource       *string `json:"resource,omitempty"`
	Algorithm      *string `json:"algorithm,omitempty"`
	MaxRequests    *int    `json:"maxRequests,omitempty"`
	WindowSeconds  *int    `json:"windowSeconds,omitempty"`
	BurstCapacity  *int    `json:"burstCapacity,omitempty"`
	Active         *bool   `json:"active,omitempty"`
	IdentifierType *string `json:"identifierType,omitempty"`
}

// UpdateRule updates a rate limit rule
func (c *RateLimitX) UpdateRule(ruleID string, req UpdateRuleRequest) (*RateLimitRule, error) {
	resp, err := c.doRequest("PUT", "/rules/"+ruleID, req)
	if err != nil {
		return nil, err
	}

	var result RateLimitRule
	if err := c.parseResponse(resp, &result); err != nil {
		return nil, err
	}

	return &result, nil
}

// DeleteRule deletes a rate limit rule
func (c *RateLimitX) DeleteRule(ruleID string) error {
	resp, err := c.doRequest("DELETE", "/rules/"+ruleID, nil)
	if err != nil {
		return err
	}

	return c.parseResponse(resp, nil)
}

// APIKey represents an API key
type APIKey struct {
	ID          string `json:"id"`
	Name        string `json:"name"`
	Environment string `json:"environment"`
	KeyHash     string `json:"keyHash"`
	CreatedAt   string `json:"createdAt"`
}

// GetAPIKeys gets all API keys
func (c *RateLimitX) GetAPIKeys() ([]APIKey, error) {
	resp, err := c.doRequest("GET", "/api-keys", nil)
	if err != nil {
		return nil, err
	}

	var result []APIKey
	if err := c.parseResponse(resp, &result); err != nil {
		return nil, err
	}

	return result, nil
}

// CreateAPIKeyRequest represents a create API key request
type CreateAPIKeyRequest struct {
	Name        string `json:"name"`
	Environment string `json:"environment"`
}

// CreateAPIKeyResponse represents a create API key response
type CreateAPIKeyResponse struct {
	ID          string `json:"id"`
	APIKey      string `json:"apiKey"`
	Name        string `json:"name"`
	Environment string `json:"environment"`
}

// CreateAPIKey creates a new API key
func (c *RateLimitX) CreateAPIKey(req CreateAPIKeyRequest) (*CreateAPIKeyResponse, error) {
	resp, err := c.doRequest("POST", "/api-keys", req)
	if err != nil {
		return nil, err
	}

	var result CreateAPIKeyResponse
	if err := c.parseResponse(resp, &result); err != nil {
		return nil, err
	}

	return &result, nil
}

// DeleteAPIKey deletes an API key
func (c *RateLimitX) DeleteAPIKey(keyID string) error {
	resp, err := c.doRequest("DELETE", "/api-keys/"+keyID, nil)
	if err != nil {
		return err
	}

	return c.parseResponse(resp, nil)
}

// RotateAPIKeyResponse represents a rotate API key response
type RotateAPIKeyResponse struct {
	APIKey string `json:"apiKey"`
}

// RotateAPIKey rotates an API key
func (c *RateLimitX) RotateAPIKey(keyID string) (*RotateAPIKeyResponse, error) {
	resp, err := c.doRequest("POST", "/api-keys/"+keyID+"/rotate", nil)
	if err != nil {
		return nil, err
	}

	var result RotateAPIKeyResponse
	if err := c.parseResponse(resp, &result); err != nil {
		return nil, err
	}

	return &result, nil
}

// AnalyticsMetrics represents analytics metrics
type AnalyticsMetrics struct {
	TotalChecks              int64   `json:"totalChecks"`
	RateLimitHits            int64   `json:"rateLimitHits"`
	HitRate                  float64 `json:"hitRate"`
	LatencyP50               *int    `json:"latencyP50,omitempty"`
	LatencyP95               *int    `json:"latencyP95,omitempty"`
	LatencyP99               *int    `json:"latencyP99,omitempty"`
	RemainingChecksThisMonth *int    `json:"remainingChecksThisMonth,omitempty"`
	Timestamp                string  `json:"timestamp"`
}

// GetRealtimeAnalytics gets real-time analytics metrics
func (c *RateLimitX) GetRealtimeAnalytics() (*AnalyticsMetrics, error) {
	resp, err := c.doRequest("GET", "/analytics/realtime", nil)
	if err != nil {
		return nil, err
	}

	var result AnalyticsMetrics
	if err := c.parseResponse(resp, &result); err != nil {
		return nil, err
	}

	return &result, nil
}

// TopIdentifier represents a top rate-limited identifier
type TopIdentifier struct {
	Identifier  string `json:"identifier"`
	DeniedCount int64  `json:"deniedCount"`
}

// GetTopIdentifiers gets top rate-limited identifiers
func (c *RateLimitX) GetTopIdentifiers(limit int) ([]TopIdentifier, error) {
	url := fmt.Sprintf("/analytics/top-identifiers?limit=%d", limit)
	resp, err := c.doRequest("GET", url, nil)
	if err != nil {
		return nil, err
	}

	var result []TopIdentifier
	if err := c.parseResponse(resp, &result); err != nil {
		return nil, err
	}

	return result, nil
}

// GetTrends gets analytics trends
func (c *RateLimitX) GetTrends(start, end string) (map[string]interface{}, error) {
	url := "/analytics/trends"
	if start != "" || end != "" {
		url += "?"
		if start != "" {
			url += "start=" + start
		}
		if end != "" {
			if start != "" {
				url += "&"
			}
			url += "end=" + end
		}
	}

	resp, err := c.doRequest("GET", url, nil)
	if err != nil {
		return nil, err
	}

	var result map[string]interface{}
	if err := c.parseResponse(resp, &result); err != nil {
		return nil, err
	}

	return result, nil
}

// AlertConfiguration represents an alert configuration
type AlertConfiguration struct {
	ID              string `json:"id"`
	AlertType       string `json:"alertType"`
	Destination     string `json:"destination"`
	DestinationType string `json:"destinationType"`
	ThresholdPercent int   `json:"thresholdPercent"`
	Enabled         bool   `json:"enabled"`
}

// GetAlerts gets all alert configurations
func (c *RateLimitX) GetAlerts() ([]AlertConfiguration, error) {
	resp, err := c.doRequest("GET", "/alerts", nil)
	if err != nil {
		return nil, err
	}

	var result []AlertConfiguration
	if err := c.parseResponse(resp, &result); err != nil {
		return nil, err
	}

	return result, nil
}

// CreateAlertRequest represents a create alert request
type CreateAlertRequest struct {
	AlertType       string `json:"alertType"`
	Destination     string `json:"destination"`
	DestinationType string `json:"destinationType"`
	ThresholdPercent int   `json:"thresholdPercent"`
}

// CreateAlert creates an alert configuration
func (c *RateLimitX) CreateAlert(req CreateAlertRequest) (*AlertConfiguration, error) {
	resp, err := c.doRequest("POST", "/alerts", req)
	if err != nil {
		return nil, err
	}

	var result AlertConfiguration
	if err := c.parseResponse(resp, &result); err != nil {
		return nil, err
	}

	return &result, nil
}

// DeleteAlert deletes an alert configuration
func (c *RateLimitX) DeleteAlert(alertID string) error {
	resp, err := c.doRequest("DELETE", "/alerts/"+alertID, nil)
	if err != nil {
		return err
	}

	return c.parseResponse(resp, nil)
}

// ExportRules exports all rules as JSON or YAML
func (c *RateLimitX) ExportRules(format string) (string, error) {
	url := fmt.Sprintf("/bulk/export?format=%s", format)
	resp, err := c.doRequest("GET", url, nil)
	if err != nil {
		return "", err
	}

	defer resp.Body.Close()
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return "", err
	}

	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return "", &RateLimitXError{
			Message:    fmt.Sprintf("Export failed with status %d", resp.StatusCode),
			StatusCode: resp.StatusCode,
		}
	}

	return string(body), nil
}

// ImportRulesResponse represents an import rules response
type ImportRulesResponse struct {
	Created int      `json:"created"`
	Skipped int      `json:"skipped"`
	Errors  []string `json:"errors"`
}

// ImportRules imports rules from JSON or YAML
func (c *RateLimitX) ImportRules(content []byte, format string) (*ImportRulesResponse, error) {
	filename := "rules." + format
	resp, err := c.doRequestWithFile("/bulk/import", content, filename, format)
	if err != nil {
		return nil, err
	}

	var result ImportRulesResponse
	if err := c.parseResponse(resp, &result); err != nil {
		return nil, err
	}

	return &result, nil
}

