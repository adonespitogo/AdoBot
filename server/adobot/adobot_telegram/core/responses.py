from __future__ import annotations

from dataclasses import dataclass
from typing import Final


@dataclass(frozen=True, slots=True)
class BotResponse:
    text: str
    parse_mode: str | None = None


START_TEXT: Final[str] = (
    "AdoBot is online. Use /help for available commands."
)

HELP_TEXT: Final[str] = (
    "/start — initialize AdoBot\n"
    "/help — show this help\n"
    "/status — show Telegram worker status\n"
    "/api_status — show AdoBot API health\n"
    "/api_root — show AdoBot API surface\n"
    "/api_ready — show AdoBot API readiness\n"
    "/api_info — show AdoBot API information"
)


def start_response() -> BotResponse:
    return BotResponse(text=START_TEXT)


def help_response() -> BotResponse:
    return BotResponse(text=HELP_TEXT)


def text_response(text: str) -> BotResponse:
    if not isinstance(text, str):
        raise TypeError("response text must be a string")
    if not text:
        raise ValueError("response text must not be empty")
    return BotResponse(text=text)
