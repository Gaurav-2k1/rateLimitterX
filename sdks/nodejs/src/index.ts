import axios, { AxiosInstance, AxiosError } from 'axios';

export interface RateLimitXConfig {
  baseUrl?: string;
  apiKey?: string;
  accessToken?: string;
  timeout?: number;
}

export interface RateLimitCheckRequest {
  identifier: string;
  resource: string;
  tokens?: number;
}

export interface RateLimitCheckResponse {
  allowed: boolean;
  remaining: number;
  resetAt: number;
  retryAfter: number;
}

export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: string;
  timestamp?: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken?: string;
  apiKey?: string;
  tenantId?: string;
}

export interface RateLimitRule {
  id?: string;
  resource: string;
  algorithm: 'TOKEN_BUCKET' | 'SLIDING_WINDOW' | 'FIXED_WINDOW';
  maxRequests: number;
  windowSeconds: number;
  burstCapacity?: number;
  identifierType?: 'USER_ID' | 'IP_ADDRESS' | 'API_KEY' | 'CUSTOM';
  limitScope?: 'GLOBAL' | 'RESOURCE' | 'IDENTIFIER';
  priority?: number;
  active?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateRuleRequest {
  resource: string;
  algorithm: string;
  maxRequests: number;
  windowSeconds: number;
  burstCapacity?: number;
  identifierType?: string;
}

export interface UpdateRuleRequest {
  resource?: string;
  algorithm?: string;
  maxRequests?: number;
  windowSeconds?: number;
  burstCapacity?: number;
  active?: boolean;
  identifierType?: string;
}

export interface ApiKey {
  id: string;
  name: string;
  environment: string;
  keyHash?: string;
  createdAt?: string;
}

export interface CreateApiKeyRequest {
  name: string;
  environment: string;
}

export interface AnalyticsMetrics {
  totalChecks: number;
  rateLimitHits: number;
  hitRate: number;
  latencyP50?: number;
  latencyP95?: number;
  latencyP99?: number;
  remainingChecksThisMonth?: number;
  timestamp?: string;
}

export interface TopIdentifier {
  identifier: string;
  deniedCount: number;
}

export interface AlertConfiguration {
  id?: string;
  alertType: 'TIER_LIMIT_APPROACHING' | 'TIER_LIMIT_EXCEEDED' | 'RATE_LIMIT_HIT';
  destination: string;
  destinationType: 'EMAIL' | 'WEBHOOK' | 'SLACK' | 'DISCORD';
  thresholdPercent: number;
  enabled: boolean;
}

export interface CreateAlertRequest {
  alertType: string;
  destination: string;
  destinationType: string;
  thresholdPercent: number;
}

export class RateLimitXError extends Error {
  constructor(
    message: string,
    public statusCode?: number,
    public response?: any
  ) {
    super(message);
    this.name = 'RateLimitXError';
  }
}

export class RateLimitX {
  private client: AxiosInstance;
  private config: RateLimitXConfig;

  constructor(config: RateLimitXConfig = {}) {
    this.config = {
      baseUrl: config.baseUrl || 'http://localhost:8080',
      timeout: config.timeout || 30000,
      ...config,
    };

    this.client = axios.create({
      baseURL: this.config.baseUrl,
      timeout: this.config.timeout,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // Add request interceptor for authentication
    this.client.interceptors.request.use((config) => {
      if (this.config.apiKey) {
        config.headers['X-API-Key'] = this.config.apiKey;
      } else if (this.config.accessToken) {
        config.headers['Authorization'] = `Bearer ${this.config.accessToken}`;
      }
      return config;
    });

    // Add response interceptor for error handling
    this.client.interceptors.response.use(
      (response) => response,
      (error: AxiosError) => {
        if (error.response) {
          const apiResponse = error.response.data as ApiResponse<any>;
          throw new RateLimitXError(
            apiResponse?.error || error.message,
            error.response.status,
            apiResponse
          );
        }
        throw new RateLimitXError(error.message);
      }
    );
  }

  /**
   * Set API key for authentication
   */
  setApiKey(apiKey: string): void {
    this.config.apiKey = apiKey;
  }

  /**
   * Set access token for authentication
   */
  setAccessToken(token: string): void {
    this.config.accessToken = token;
  }

  /**
   * Check if a request should be rate limited
   */
  async check(request: RateLimitCheckRequest): Promise<RateLimitCheckResponse> {
    const response = await this.client.post<RateLimitCheckResponse>(
      '/api/v1/check',
      {
        identifier: request.identifier,
        resource: request.resource,
        tokens: request.tokens || 1,
      }
    );
    return response.data;
  }

  /**
   * Register a new user/tenant
   */
  async register(request: RegisterRequest): Promise<AuthResponse> {
    const response = await this.client.post<ApiResponse<AuthResponse>>(
      '/auth/register',
      request
    );
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new RateLimitXError('Registration failed');
  }

  /**
   * Login and get access token
   */
  async login(request: LoginRequest): Promise<AuthResponse> {
    const response = await this.client.post<ApiResponse<AuthResponse>>(
      '/auth/login',
      request
    );
    if (response.data.success && response.data.data) {
      const authData = response.data.data;
      if (authData.accessToken) {
        this.setAccessToken(authData.accessToken);
      }
      return authData;
    }
    throw new RateLimitXError('Login failed');
  }

  /**
   * Refresh access token
   */
  async refresh(refreshToken: string): Promise<AuthResponse> {
    const response = await this.client.post<ApiResponse<AuthResponse>>(
      '/auth/refresh',
      { refreshToken }
    );
    if (response.data.success && response.data.data) {
      const authData = response.data.data;
      if (authData.accessToken) {
        this.setAccessToken(authData.accessToken);
      }
      return authData;
    }
    throw new RateLimitXError('Token refresh failed');
  }

  /**
   * Get all rate limit rules
   */
  async getRules(): Promise<RateLimitRule[]> {
    const response = await this.client.get<ApiResponse<RateLimitRule[]>>(
      '/rules'
    );
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    return [];
  }

  /**
   * Create a new rate limit rule
   */
  async createRule(request: CreateRuleRequest): Promise<RateLimitRule> {
    const response = await this.client.post<ApiResponse<RateLimitRule>>(
      '/rules',
      request
    );
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new RateLimitXError('Failed to create rule');
  }

  /**
   * Update a rate limit rule
   */
  async updateRule(id: string, request: UpdateRuleRequest): Promise<RateLimitRule> {
    const response = await this.client.put<ApiResponse<RateLimitRule>>(
      `/rules/${id}`,
      request
    );
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new RateLimitXError('Failed to update rule');
  }

  /**
   * Delete a rate limit rule
   */
  async deleteRule(id: string): Promise<void> {
    const response = await this.client.delete<ApiResponse<void>>(`/rules/${id}`);
    if (!response.data.success) {
      throw new RateLimitXError('Failed to delete rule');
    }
  }

  /**
   * Get all API keys
   */
  async getApiKeys(): Promise<ApiKey[]> {
    const response = await this.client.get<ApiResponse<ApiKey[]>>('/api-keys');
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    return [];
  }

  /**
   * Create a new API key
   */
  async createApiKey(request: CreateApiKeyRequest): Promise<{ id: string; apiKey: string; name: string; environment: string }> {
    const response = await this.client.post<ApiResponse<{ id: string; apiKey: string; name: string; environment: string }>>(
      '/api-keys',
      request
    );
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new RateLimitXError('Failed to create API key');
  }

  /**
   * Delete an API key
   */
  async deleteApiKey(id: string): Promise<void> {
    const response = await this.client.delete<ApiResponse<void>>(`/api-keys/${id}`);
    if (!response.data.success) {
      throw new RateLimitXError('Failed to delete API key');
    }
  }

  /**
   * Rotate an API key
   */
  async rotateApiKey(id: string): Promise<{ apiKey: string }> {
    const response = await this.client.post<ApiResponse<{ apiKey: string }>>(
      `/api-keys/${id}/rotate`
    );
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new RateLimitXError('Failed to rotate API key');
  }

  /**
   * Get real-time analytics
   */
  async getRealtimeAnalytics(): Promise<AnalyticsMetrics> {
    const response = await this.client.get<ApiResponse<AnalyticsMetrics>>(
      '/analytics/realtime'
    );
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new RateLimitXError('Failed to get analytics');
  }

  /**
   * Get top rate-limited identifiers
   */
  async getTopIdentifiers(limit: number = 10): Promise<TopIdentifier[]> {
    const response = await this.client.get<ApiResponse<TopIdentifier[]>>(
      '/analytics/top-identifiers',
      { params: { limit } }
    );
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    return [];
  }

  /**
   * Get analytics trends
   */
  async getTrends(start?: string, end?: string): Promise<any> {
    const response = await this.client.get<ApiResponse<any>>(
      '/analytics/trends',
      { params: { start, end } }
    );
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new RateLimitXError('Failed to get trends');
  }

  /**
   * Get all alert configurations
   */
  async getAlerts(): Promise<AlertConfiguration[]> {
    const response = await this.client.get<ApiResponse<AlertConfiguration[]>>(
      '/alerts'
    );
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    return [];
  }

  /**
   * Create an alert configuration
   */
  async createAlert(request: CreateAlertRequest): Promise<AlertConfiguration> {
    const response = await this.client.post<ApiResponse<AlertConfiguration>>(
      '/alerts',
      request
    );
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new RateLimitXError('Failed to create alert');
  }

  /**
   * Delete an alert configuration
   */
  async deleteAlert(id: string): Promise<void> {
    const response = await this.client.delete<ApiResponse<void>>(`/alerts/${id}`);
    if (!response.data.success) {
      throw new RateLimitXError('Failed to delete alert');
    }
  }

  /**
   * Export rules as JSON or YAML
   */
  async exportRules(format: 'json' | 'yaml' = 'json'): Promise<string> {
    const response = await this.client.get<string>('/bulk/export', {
      params: { format },
      responseType: 'text',
    });
    return response.data;
  }

  /**
   * Import rules from JSON or YAML
   * Note: Requires form-data package for Node.js: npm install form-data
   */
  async importRules(content: string | Buffer, format: 'json' | 'yaml' = 'json'): Promise<{ created: number; skipped: number; errors: string[] }> {
    try {
      // Try to use form-data if available
      const FormData = require('form-data');
      const formData = new FormData();
      
      const buffer = typeof content === 'string' ? Buffer.from(content) : content;
      formData.append('file', buffer, {
        filename: `rules.${format}`,
        contentType: format === 'yaml' ? 'application/x-yaml' : 'application/json',
      });
      formData.append('format', format);

      const response = await this.client.post<ApiResponse<{ created: number; skipped: number; errors: string[] }>>(
        '/bulk/import',
        formData,
        {
          headers: formData.getHeaders(),
        }
      );
      if (response.data.success && response.data.data) {
        return response.data.data;
      }
      throw new RateLimitXError('Failed to import rules');
    } catch (error: any) {
      if (error.code === 'MODULE_NOT_FOUND') {
        throw new RateLimitXError('form-data package is required for importRules. Install it with: npm install form-data');
      }
      throw error;
    }
  }
}

export default RateLimitX;

