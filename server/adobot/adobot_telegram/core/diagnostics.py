from __future__ import annotations

from typing import Any, Mapping


REQUIRED_KEYS = frozenset(
    {
        "service",
        "status",
        "environment",
        "version",
        "python",
        "pid",
        "uptime_seconds",
        "endpoints",
    }
)


def validate_diagnostics_payload(
    payload: Mapping[str, Any],
) -> dict[str, Any]:
    """Validate the complete local diagnostics response."""

    if not isinstance(payload, Mapping):
        raise ValueError("diagnostics payload must be an object")

    if set(payload) != REQUIRED_KEYS:
        raise ValueError("diagnostics payload schema mismatch")

    for field in (
        "service",
        "status",
        "environment",
        "version",
        "python",
    ):
        value = payload[field]
        if not isinstance(value, str) or not value:
            raise ValueError(f"invalid diagnostics field: {field}")

    if payload["status"] != "ok":
        raise ValueError("invalid diagnostics status")

    pid = payload["pid"]
    if not isinstance(pid, int) or isinstance(pid, bool) or pid <= 0:
        raise ValueError("invalid diagnostics pid")

    uptime = payload["uptime_seconds"]
    if (
        not isinstance(uptime, int)
        or isinstance(uptime, bool)
        or uptime < 0
    ):
        raise ValueError("invalid diagnostics uptime")

    endpoints = payload["endpoints"]
    if not isinstance(endpoints, list) or not endpoints:
        raise ValueError("invalid diagnostics endpoints")

    if any(
        not isinstance(endpoint, str)
        or not endpoint.startswith("/")
        for endpoint in endpoints
    ):
        raise ValueError("invalid diagnostics endpoint entry")

    return dict(payload)


def format_diagnostics(payload: Mapping[str, Any]) -> str:
    """Render validated diagnostics without exposing secrets."""

    validated = validate_diagnostics_payload(payload)
    endpoints = ", ".join(validated["endpoints"])

    return (
        "AdoBot Diagnostics\n"
        f"Service: {validated['service']}\n"
        f"Status: {validated['status']}\n"
        f"Environment: {validated['environment']}\n"
        f"Version: {validated['version']}\n"
        f"Python: {validated['python']}\n"
        f"PID: {validated['pid']}\n"
        f"Uptime: {validated['uptime_seconds']}s\n"
        f"Endpoints: {endpoints}"
    )
