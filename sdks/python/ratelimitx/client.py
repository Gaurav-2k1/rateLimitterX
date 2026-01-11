"""RateLimitX Python Client"""

import requests
from typing import Optional, Dict, Any, List
from .types import (
    RateLimitCheckRequest,
    RateLimitCheckResponse,
    RateLimitRule,
    ApiKey,
    AnalyticsMetrics,
    TopIdentifier,
    AlertConfiguration,
    AuthResponse,
)


class RateLimitXError(Exception):
    """RateLimitX API Error"""

    def __init__(self, message: str, status_code: Optional[int] = None, response: Optional[Dict] = None):
        super().__init__(message)
        self.status_code = status_code
        self.response = response


class RateLimitX:
    """RateLimitX Python Client"""

    def __init__(
        self,
        base_url: str = "http://localhost:8080",
        api_key: Optional[str] = None,
        access_token: Optional[str] = None,
        timeout: int = 30,
    ):
        """
        Initialize RateLimitX client

        Args:
            base_url: Base URL of the RateLimitX API
            api_key: API key for authentication
            access_token: Access token for authentication
            timeout: Request timeout in seconds
        """
        self.base_url = base_url.rstrip("/")
        self.api_key = api_key
        self.access_token = access_token
        self.timeout = timeout
        self.session = requests.Session()
        self.session.headers.update({"Content-Type": "application/json"})

    def _get_headers(self) -> Dict[str, str]:
        """Get request headers with authentication"""
        headers = {}
        if self.api_key:
            headers["X-API-Key"] = self.api_key
        elif self.access_token:
            headers["Authorization"] = f"Bearer {self.access_token}"
        return headers

    def _request(
        self,
        method: str,
        endpoint: str,
        data: Optional[Dict] = None,
               params: Optional[Dict] = None,
        files: Optional[Dict] = None,
    ) -> Dict[str, Any]:
        """Make HTTP request"""
        url = f"{self.base_url}{endpoint}"
        headers = self._get_headers()

        try:
            if files:
                # For file uploads, don't set Content-Type (let requests handle it)
                headers.pop("Content-Type", None)
                response = self.session.request(
                    method, url, json=data, params=params, files=files, headers=headers, timeout=self.timeout
                )
            else:
                response = self.session.request(
                    method, url, json=data, params=params, headers=headers, timeout=self.timeout
                )

            response.raise_for_status()

            # Handle text responses (like export)
            if response.headers.get("content-type", "").startswith("text/"):
                return {"content": response.text}

            result = response.json()

            # Handle ApiResponse wrapper
            if isinstance(result, dict) and "success" in result:
                if result.get("success"):
                    return result.get("data", {})
                else:
                    raise RateLimitXError(
                        result.get("error", "Unknown error"),
                        response.status_code,
                        result,
                    )
            return result

        except requests.exceptions.HTTPError as e:
            try:
                error_data = e.response.json()
                if isinstance(error_data, dict) and "error" in error_data:
                    raise RateLimitXError(
                        error_data.get("error", str(e)),
                        e.response.status_code,
                        error_data,
                    )
            except:
                pass
            raise RateLimitXError(str(e), e.response.status_code if e.response else None)
        except requests.exceptions.RequestException as e:
            raise RateLimitXError(f"Request failed: {str(e)}")

    def set_api_key(self, api_key: str) -> None:
        """Set API key for authentication"""
        self.api_key = api_key

    def set_access_token(self, token: str) -> None:
        """Set access token for authentication"""
        self.access_token = token

    def check(self, request: RateLimitCheckRequest) -> RateLimitCheckResponse:
        """
        Check if a request should be rate limited

        Args:
            request: Rate limit check request

        Returns:
            RateLimitCheckResponse
        """
        data = {
            "identifier": request.identifier,
            "resource": request.resource,
            "tokens": request.tokens,
        }
        result = self._request("POST", "/api/v1/check", data=data)
        return RateLimitCheckResponse(**result)

    def register(self, email: str, password: str) -> AuthResponse:
        """
        Register a new user/tenant

        Args:
            email: User email
            password: User password

        Returns:
            AuthResponse with access token and API key
        """
        data = {"email": email, "password": password}
        result = self._request("POST", "/auth/register", data=data)
        auth = AuthResponse.from_dict(result)
        if auth.accessToken:
            self.set_access_token(auth.accessToken)
        return auth

    def login(self, email: str, password: str) -> AuthResponse:
        """
        Login and get access token

        Args:
            email: User email
            password: User password

        Returns:
            AuthResponse with access token
        """
        data = {"email": email, "password": password}
        result = self._request("POST", "/auth/login", data=data)
        auth = AuthResponse.from_dict(result)
        if auth.accessToken:
            self.set_access_token(auth.accessToken)
        return auth

    def refresh_token(self, refresh_token: str) -> AuthResponse:
        """
        Refresh access token

        Args:
            refresh_token: Refresh token

        Returns:
            AuthResponse with new access token
        """
        data = {"refreshToken": refresh_token}
        result = self._request("POST", "/auth/refresh", data=data)
        auth = AuthResponse.from_dict(result)
        if auth.accessToken:
            self.set_access_token(auth.accessToken)
        return auth

    def get_rules(self) -> List[RateLimitRule]:
        """Get all rate limit rules"""
        result = self._request("GET", "/rules")
        if isinstance(result, list):
            return [RateLimitRule.from_dict(rule) for rule in result]
        return []

    def create_rule(
        self,
        resource: str,
        algorithm: str,
        max_requests: int,
        window_seconds: int,
        burst_capacity: Optional[int] = None,
        identifier_type: Optional[str] = None,
    ) -> RateLimitRule:
        """
        Create a new rate limit rule

        Args:
            resource: Resource identifier
            algorithm: Algorithm type (TOKEN_BUCKET, SLIDING_WINDOW, FIXED_WINDOW)
            max_requests: Maximum requests per window
            window_seconds: Window size in seconds
            burst_capacity: Burst capacity (optional)
            identifier_type: Identifier type (optional)

        Returns:
            Created RateLimitRule
        """
        data = {
            "resource": resource,
            "algorithm": algorithm,
            "maxRequests": max_requests,
            "windowSeconds": window_seconds,
        }
        if burst_capacity is not None:
            data["burstCapacity"] = burst_capacity
        if identifier_type:
            data["identifierType"] = identifier_type

        result = self._request("POST", "/rules", data=data)
        return RateLimitRule.from_dict(result)

    def update_rule(
        self,
        rule_id: str,
        resource: Optional[str] = None,
        algorithm: Optional[str] = None,
        max_requests: Optional[int] = None,
        window_seconds: Optional[int] = None,
        burst_capacity: Optional[int] = None,
        active: Optional[bool] = None,
        identifier_type: Optional[str] = None,
    ) -> RateLimitRule:
        """Update a rate limit rule"""
        data = {}
        if resource is not None:
            data["resource"] = resource
        if algorithm is not None:
            data["algorithm"] = algorithm
        if max_requests is not None:
            data["maxRequests"] = max_requests
        if window_seconds is not None:
            data["windowSeconds"] = window_seconds
        if burst_capacity is not None:
            data["burstCapacity"] = burst_capacity
        if active is not None:
            data["active"] = active
        if identifier_type is not None:
            data["identifierType"] = identifier_type

        result = self._request("PUT", f"/rules/{rule_id}", data=data)
        return RateLimitRule.from_dict(result)

    def delete_rule(self, rule_id: str) -> None:
        """Delete a rate limit rule"""
        self._request("DELETE", f"/rules/{rule_id}")

    def get_api_keys(self) -> List[ApiKey]:
        """Get all API keys"""
        result = self._request("GET", "/api-keys")
        if isinstance(result, list):
            return [ApiKey.from_dict(key) for key in result]
        return []

    def create_api_key(self, name: str, environment: str) -> Dict[str, str]:
        """
        Create a new API key

        Args:
            name: API key name
            environment: Environment (e.g., 'production', 'development')

        Returns:
            Dict with id, apiKey, name, environment
        """
        data = {"name": name, "environment": environment}
        return self._request("POST", "/api-keys", data=data)

    def delete_api_key(self, key_id: str) -> None:
        """Delete an API key"""
        self._request("DELETE", f"/api-keys/{key_id}")

    def rotate_api_key(self, key_id: str) -> Dict[str, str]:
        """Rotate an API key"""
        result = self._request("POST", f"/api-keys/{key_id}/rotate")
        return result

    def get_realtime_analytics(self) -> AnalyticsMetrics:
        """Get real-time analytics metrics"""
        result = self._request("GET", "/analytics/realtime")
        return AnalyticsMetrics.from_dict(result)

    def get_top_identifiers(self, limit: int = 10) -> List[TopIdentifier]:
        """Get top rate-limited identifiers"""
        result = self._request("GET", "/analytics/top-identifiers", params={"limit": limit})
        if isinstance(result, list):
            return [TopIdentifier.from_dict(item) for item in result]
        return []

    def get_trends(self, start: Optional[str] = None, end: Optional[str] = None) -> Dict[str, Any]:
        """Get analytics trends"""
        params = {}
        if start:
            params["start"] = start
        if end:
            params["end"] = end
        return self._request("GET", "/analytics/trends", params=params)

    def get_alerts(self) -> List[AlertConfiguration]:
        """Get all alert configurations"""
        result = self._request("GET", "/alerts")
        if isinstance(result, list):
            return [AlertConfiguration.from_dict(alert) for alert in result]
        return []

    def create_alert(
        self,
        alert_type: str,
        destination: str,
        destination_type: str,
        threshold_percent: int,
    ) -> AlertConfiguration:
        """Create an alert configuration"""
        data = {
            "alertType": alert_type,
            "destination": destination,
            "destinationType": destination_type,
            "thresholdPercent": threshold_percent,
        }
        result = self._request("POST", "/alerts", data=data)
        return AlertConfiguration.from_dict(result)

    def delete_alert(self, alert_id: str) -> None:
        """Delete an alert configuration"""
        self._request("DELETE", f"/alerts/{alert_id}")

    def export_rules(self, format: str = "json") -> str:
        """
        Export all rules as JSON or YAML

        Args:
            format: Export format ('json' or 'yaml')

        Returns:
            Exported rules as string
        """
        result = self._request("GET", "/bulk/export", params={"format": format})
        return result.get("content", "")

    def import_rules(self, content: str, format: str = "json") -> Dict[str, Any]:
        """
        Import rules from JSON or YAML

        Args:
            content: Rules content as string
            format: Format ('json' or 'yaml')

        Returns:
            Dict with created, skipped, errors
        """
        import io
        files = {
            "file": (f"rules.{format}", io.BytesIO(content.encode()), 
                    "application/json" if format == "json" else "application/x-yaml")
        }
        data = {"format": format}
        return self._request("POST", "/bulk/import", data=data, files=files)

