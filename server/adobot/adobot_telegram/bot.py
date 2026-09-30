"""Isolated AdoBot Telegram worker.

The module deliberately separates construction from execution so that
offline validation never contacts Telegram.
"""

from __future__ import annotations

import logging
import os
import signal
from pathlib import Path

from telegram import Update
from telegram.request import HTTPXRequest
from telegram.ext import (
    Application,
    CommandHandler,
    ContextTypes,
)

from .config import load_config
from .runtime import build_runtime_status


ROOT = Path(__file__).resolve().parents[1]
LOG_DIR = ROOT / "log"
LOG_FILE = LOG_DIR / "adobot-telegram.log"
PID_FILE = ROOT / "run" / "adobot-telegram.pid"

LOGGER = logging.getLogger("adobot.telegram")

STARTED_MONOTONIC = __import__("time").monotonic()


def configure_logging(level: str) -> None:
    LOG_DIR.mkdir(mode=0o700, parents=True, exist_ok=True)

    logging.basicConfig(
        level=getattr(logging, level.upper()),
        format="%(asctime)s %(levelname)s %(name)s %(message)s",
        handlers=[
            logging.StreamHandler(),
            logging.FileHandler(LOG_FILE, encoding="utf-8"),
        ],
    )

    # Telegram Bot API request URLs contain the bot token.
    # Suppress httpx/httpcore INFO request logging.
    logging.getLogger("httpx").setLevel(logging.WARNING)
    logging.getLogger("httpcore").setLevel(logging.WARNING)


def acquire_pid() -> None:
    if PID_FILE.exists():
        existing = PID_FILE.read_text(encoding="utf-8").strip()

        if existing:
            try:
                os.kill(int(existing), 0)
            except ProcessLookupError:
                pass
            except PermissionError:
                raise RuntimeError(
                    f"Telegram PID file references inaccessible PID {existing}"
                )
            else:
                raise RuntimeError(
                    f"Telegram worker already appears active: PID {existing}"
                )

        PID_FILE.unlink()

    PID_FILE.write_text(f"{os.getpid()}\n", encoding="utf-8")
    PID_FILE.chmod(0o600)


def release_pid() -> None:
    try:
        if PID_FILE.exists():
            recorded = PID_FILE.read_text(encoding="utf-8").strip()
            if recorded == str(os.getpid()):
                PID_FILE.unlink()
    except OSError:
        LOGGER.exception("failed to remove PID file")


async def start_command(
    update: Update,
    context: ContextTypes.DEFAULT_TYPE,
) -> None:
    del context

    if update.effective_message:
        await update.effective_message.reply_text(
            "AdoBot is online. Use /help for available commands."
        )


async def help_command(
    update: Update,
    context: ContextTypes.DEFAULT_TYPE,
) -> None:
    del context

    if update.effective_message:
        await update.effective_message.reply_text(
            "/start — initialize AdoBot\n"
            "/help — show this help\n"
            "/status — show worker status"
        )


async def error_handler(
    update: object,
    context: ContextTypes.DEFAULT_TYPE,
) -> None:
    del update

    error = context.error
    LOGGER.error(
        "telegram application error type=%s",
        type(error).__name__,
    )


async def status_command(
    update: Update,
    context: ContextTypes.DEFAULT_TYPE,
) -> None:
    del context

    if update.effective_message:
        try:
            status = build_runtime_status(
                started_monotonic=STARTED_MONOTONIC,
                pid_file=PID_FILE,
            )
            message = status.render()
        except Exception:
            LOGGER.exception("status command failed")
            message = "AdoBot Telegram worker: status unavailable"

        await update.effective_message.reply_text(message)


def build_application(token: str) -> Application:
    # Keep ordinary Bot API traffic bounded while giving long-polling its
    # own read timeout that exceeds the Telegram getUpdates poll timeout.
    api_request = HTTPXRequest(
        connection_pool_size=16,
        read_timeout=45.0,
        write_timeout=15.0,
        connect_timeout=10.0,
        pool_timeout=10.0,
        http_version="1.1",
    )

    get_updates_request = HTTPXRequest(
        connection_pool_size=4,
        read_timeout=45.0,
        write_timeout=15.0,
        connect_timeout=10.0,
        pool_timeout=10.0,
        http_version="1.1",
    )

    application = (
        Application.builder()
        .token(token)
        .request(api_request)
        .get_updates_request(get_updates_request)
        .build()
    )

    application.add_handler(CommandHandler("start", start_command))
    application.add_handler(CommandHandler("help", help_command))
    application.add_handler(CommandHandler("status", status_command))
    application.add_error_handler(error_handler)

    return application


def run() -> None:
    config = load_config(require_token=True)
    configure_logging(config.log_level)

    acquire_pid()

    try:
        LOGGER.info("AdoBot Telegram worker starting")
        LOGGER.info("transport=long_polling")

        application = build_application(config.token)

        application.run_polling(
            poll_interval=1.0,
            timeout=30,
            bootstrap_retries=5,
            allowed_updates=Update.ALL_TYPES,
            drop_pending_updates=False,
            close_loop=False,
        )
    finally:
        release_pid()
        LOGGER.info("AdoBot Telegram worker stopped")


def main() -> None:
    try:
        run()
    except KeyboardInterrupt:
        LOGGER.info("Telegram worker interrupted")
    finally:
        release_pid()


if __name__ == "__main__":
    main()
