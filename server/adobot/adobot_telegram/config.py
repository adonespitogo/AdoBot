"""Configuration for the isolated AdoBot Telegram worker."""

from __future__ import annotations

import os
from dataclasses import dataclass


@dataclass(frozen=True)
class TelegramConfig:
    token: str
    environment: str
    log_level: str


def load_config(*, require_token: bool = True) -> TelegramConfig:
    environment = os.environ.get("ENVIRONMENT", "isolated")
    log_level = os.environ.get("LOG_LEVEL", "info").lower()
    token = os.environ.get("TELEGRAM_BOT_TOKEN", "")

    if environment != "isolated":
        raise RuntimeError(
            f"unsafe ENVIRONMENT={environment!r}; expected 'isolated'"
        )

    if log_level not in {"debug", "info", "warning", "error", "critical"}:
        raise RuntimeError(f"invalid LOG_LEVEL={log_level!r}")

    if require_token and not token:
        raise RuntimeError("TELEGRAM_BOT_TOKEN is required for live mode")

    if token and len(token) < 20:
        raise RuntimeError("TELEGRAM_BOT_TOKEN has invalid length")

    return TelegramConfig(
        token=token,
        environment=environment,
        log_level=log_level,
    )
