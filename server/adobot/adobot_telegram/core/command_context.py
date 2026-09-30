from __future__ import annotations

from telegram import Update

from adobot_telegram.audit.events import new_event


def audit_command(
    update: Update,
    command: str,
    *,
    outcome: str = "accepted",
) -> None:
    """
    Emit a structured audit event through the application's logger.

    No message text, token, or credential is included.
    """
    user = update.effective_user
    chat = update.effective_chat

    event = new_event(
        "command",
        command=command,
        user_id=user.id if user else None,
        chat_id=chat.id if chat else None,
        outcome=outcome,
    )

    import logging

    logging.getLogger("adobot.telegram.audit").info(
        "audit=%s",
        event.to_json(),
    )
