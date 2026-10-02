from __future__ import annotations


class AdoBotError(Exception):
    """Base class for expected AdoBot application errors."""


class AuthorizationError(AdoBotError):
    """Raised when an action is not authorized."""


class ConfigurationError(AdoBotError):
    """Raised when application configuration is invalid."""


class AuditError(AdoBotError):
    """Raised when an audit event cannot be represented safely."""
