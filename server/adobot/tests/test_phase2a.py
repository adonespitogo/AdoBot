from __future__ import annotations

import json

from adobot_telegram.audit.events import AuditEvent, new_event
from adobot_telegram.core.responses import (
    HELP_TEXT,
    START_TEXT,
    help_response,
    start_response,
    text_response,
)
from adobot_telegram.security.authorization import (
    DEFAULT_POLICY,
    AuthorizationPolicy,
)


def test_start_response_preserves_existing_behavior() -> None:
    response = start_response()
    assert response.text == START_TEXT
    assert response.text == (
        "AdoBot is online. Use /help for available commands."
    )


def test_help_response_preserves_existing_behavior() -> None:
    response = help_response()
    assert response.text == HELP_TEXT
    assert "/start — initialize AdoBot" in response.text
    assert "/help — show this help" in response.text
    assert "/status — show Telegram worker status" in response.text


def test_text_response_rejects_invalid_values() -> None:
    try:
        text_response("")
    except ValueError:
        pass
    else:
        raise AssertionError("empty response must fail closed")


def test_default_public_policy() -> None:
    for command in ("start", "/start", "HELP", "/status"):
        assert DEFAULT_POLICY.allows_public(command)

    assert not DEFAULT_POLICY.allows_public("admin")
    assert not DEFAULT_POLICY.allows_public("")


def test_admin_policy_fails_closed() -> None:
    policy = AuthorizationPolicy(
        public_commands=frozenset({"start"}),
        admin_user_ids=frozenset({123456789}),
    )

    assert policy.allows_admin(123456789)
    assert not policy.allows_admin(None)
    assert not policy.allows_admin(987654321)

    assert policy.allows("start")
    assert not policy.allows("admin", admin_only=True)
    assert policy.allows("admin", user_id=123456789, admin_only=True)


def test_audit_event_is_structured() -> None:
    event = AuditEvent(
        event="command",
        timestamp_utc="2026-09-29T00:00:00Z",
        command="status",
        user_id=123456789,
        chat_id=987654321,
        outcome="accepted",
    )

    payload = event.to_dict()

    assert payload["event"] == "command"
    assert payload["command"] == "status"
    assert payload["outcome"] == "accepted"

    encoded = json.loads(event.to_json())
    assert encoded == payload


def test_new_event_has_utc_timestamp() -> None:
    event = new_event(
        "command",
        command="status",
        user_id=123456789,
        chat_id=987654321,
    )

    assert event.timestamp_utc.endswith("Z")
    assert event.outcome == "accepted"
