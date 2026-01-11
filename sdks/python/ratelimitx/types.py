"""Type definitions for RateLimitX SDK"""

from dataclasses import dataclass
from typing import Optional, List, Dict, Any
from enum import Enum


class Algorithm(str, Enum):
    TOKEN_BUCKET = "TOKEN_BUCKET"
    SLIDING_WINDOW = "SLIDING_WINDOW"
    FIXED_WINDOW = "FIXED_WINDOW"


class IdentifierType(str, Enum):
    USER_ID = "USER_ID"
    IP_ADDRESS = "IP_ADDRESS"
    API_KEY = "API_KEY"
    CUSTOM = "CUSTOM"


class LimitScope(str, Enum):
    GLOBAL = "GLOBAL"
    RESOURCE = "RESOURCE"
    IDENTIFIER = "IDENTIFIER"


@dataclass
class RateLimitCheckRequest:
    identifier: str
    resource: str
    tokens: int = 1


@dataclass
class RateLimitCheckResponse:
    allowed: bool
    remaining: int
    resetAt: int
    retryAfter: int


@dataclass
class RateLimitRule:
    id: Optional[str] = None
    resource: Optional[str] = None
    algorithm: Optional[str] = None
    maxRequests: Optional[int] = None
    windowSeconds: Optional[int] = None
    burstCapacity: Optional[int] = None
    identifierType: Optional[str] = None
    limitScope: Optional[str] = None
    priority: Optional[int] = None
    active: Optional[bool] = None
    createdAt: Optional[str] = None
    updatedAt: Optional[str] = None

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "RateLimitRule":
        return cls(**{k: v for k, v in data.items() if hasattr(cls, k)})


@dataclass
class ApiKey:
    id: str
    name: str
    environment: str
    keyHash: Optional[str] = None
    createdAt: Optional[str] = None

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "ApiKey":
        return cls(**{k: v for k, v in data.items() if hasattr(cls, k)})


@dataclass
class AnalyticsMetrics:
    totalChecks: int
    rateLimitHits: int
    hitRate: float
    latencyP50: Optional[int] = None
    latencyP95: Optional[int] = None
    latencyP99: Optional[int] = None
    remainingChecksThisMonth: Optional[int] = None
    timestamp: Optional[str] = None

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "AnalyticsMetrics":
        return cls(**{k: v for k, v in data.items() if hasattr(cls, k)})


@dataclass
class TopIdentifier:
    identifier: str
    deniedCount: int

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "TopIdentifier":
        return cls(**data)


@dataclass
class AlertConfiguration:
    id: Optional[str] = None
    alertType: Optional[str] = None
    destination: Optional[str] = None
    destinationType: Optional[str] = None
    thresholdPercent: Optional[int] = None
    enabled: Optional[bool] = None

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "AlertConfiguration":
        return cls(**{k: v for k, v in data.items() if hasattr(cls, k)})


@dataclass
class AuthResponse:
    accessToken: str
    refreshToken: Optional[str] = None
    apiKey: Optional[str] = None
    tenantId: Optional[str] = None

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "AuthResponse":
        return cls(**{k: v for k, v in data.items() if hasattr(cls, k)})

