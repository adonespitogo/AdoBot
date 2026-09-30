from __future__ import annotations

import json
from dataclasses import asdict, dataclass
from datetime import datetime, timezone


@dataclass(frozen=True, slots=True)
class AuditEvent:
    event: str
    timestamp_utc: str
    command: str | None = None
    user_id: int | None = None
    chat_id: int | None = None
    outcome: str = "accepted"

    def __post_init__(self) -> None:
        if not self.event.strip():
            raise ValueError("audit event name must not be empty")

        if self.outcome not in {
            "accepted",
            "denied",
            "error",
            "ignored",
        }:
            raise ValueError("invalid audit outcome")

        if self.user_id is not None and self.user_id <= 0:
            raise ValueError("user_id must be positive")

        if self.chat_id is not None and self.chat_id <= 0:
            raise ValueError("chat_id must be positive")

    def to_dict(self) -> dict[str, object]:
        return asdict(self)

    def to_json(self) -> str:
        return json.dumps(
            self.to_dict(),
            sort_keys=True,
            separators=(",", ":"),
        )


def new_event(
    event: str,
    *,
    command: str | None = None,
    user_id: int | None = None,
    chat_id: int | None = None,
    outcome: str = "accepted",
) -> AuditEvent:
    return AuditEvent(
        event=event,
        timestamp_utc=datetime.now(timezone.utc)
        .replace(microsecond=0)
        .isoformat()
        .replace("+00:00", "Z"),
        command=command,
        user_id=user_id,
        chat_id=chat_id,
        outcome=outcome,
    )
