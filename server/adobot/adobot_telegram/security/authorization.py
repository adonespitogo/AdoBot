from __future__ import annotations

from dataclasses import dataclass
from typing import FrozenSet


@dataclass(frozen=True, slots=True)
class AuthorizationPolicy:
    """
    Minimal fail-closed authorization policy.

    Public commands are explicitly enumerated.
    Administrative commands require an allowlisted Telegram user ID.
    """

    public_commands: FrozenSet[str]
    admin_user_ids: FrozenSet[int]

    def __post_init__(self) -> None:
        normalized = frozenset(
            command.strip().lower().lstrip("/")
            for command in self.public_commands
            if command and command.strip()
        )

        if normalized != self.public_commands:
            object.__setattr__(self, "public_commands", normalized)

        for user_id in self.admin_user_ids:
            if not isinstance(user_id, int) or user_id <= 0:
                raise ValueError("admin user IDs must be positive integers")

    def allows_public(self, command: str) -> bool:
        normalized = command.strip().lower().lstrip("/")
        return normalized in self.public_commands

    def allows_admin(self, user_id: int | None) -> bool:
        if user_id is None:
            return False
        return user_id in self.admin_user_ids

    def allows(
        self,
        command: str,
        *,
        user_id: int | None = None,
        admin_only: bool = False,
    ) -> bool:
        if admin_only:
            return self.allows_admin(user_id)
        return self.allows_public(command)


DEFAULT_POLICY = AuthorizationPolicy(
    public_commands=frozenset({
        "start",
        "help",
        "status",
        "api_status",
        "api_ready",
        "api_root",
        "api_info",
    }),
    admin_user_ids=frozenset(),
)
