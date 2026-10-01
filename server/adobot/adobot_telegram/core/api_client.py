"""Bounded local client for the AdoBot HTTP API."""

from __future__ import annotations

import asyncio
import json
import urllib.error
import urllib.request
from dataclasses import dataclass
from typing import Any


API_BASE_URL = "http://127.0.0.1:8080"
DEFAULT_TIMEOUT_SECONDS = 3.0
ALLOWED_PATHS = frozenset({"/health", "/info"})


class AdoBotAPIError(RuntimeError):
    """Raised when the local AdoBot API cannot be queried safely."""


@dataclass(frozen=True, slots=True)
class APIResult:
    path: str
    status_code: int
    payload: dict[str, Any]


def _request_sync(path: str, timeout: float) -> APIResult:
    if path not in ALLOWED_PATHS:
        raise ValueError(f"API path not allowed: {path}")

    url = API_BASE_URL + path

    request = urllib.request.Request(
        url,
        method="GET",
        headers={
            "Accept": "application/json",
            "User-Agent": "AdoBot-Telegram-Bridge/1.0",
        },
    )

    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            raw = response.read()
            status_code = int(response.status)
    except (urllib.error.URLError, TimeoutError, OSError) as exc:
        raise AdoBotAPIError(
            f"AdoBot API unavailable at {path}"
        ) from exc

    if status_code < 200 or status_code >= 300:
        raise AdoBotAPIError(
            f"AdoBot API returned HTTP {status_code}"
        )

    try:
        payload = json.loads(raw.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise AdoBotAPIError(
            "AdoBot API returned invalid JSON"
        ) from exc

    if not isinstance(payload, dict):
        raise AdoBotAPIError(
            "AdoBot API returned a non-object response"
        )

    return APIResult(
        path=path,
        status_code=status_code,
        payload=payload,
    )


async def get(path: str, *, timeout: float = DEFAULT_TIMEOUT_SECONDS) -> APIResult:
    if timeout <= 0:
        raise ValueError("timeout must be positive")

    return await asyncio.to_thread(
        _request_sync,
        path,
        timeout,
    )
