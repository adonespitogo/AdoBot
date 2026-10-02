#!/usr/bin/env python3

from __future__ import annotations

import fcntl
import os
import signal
import subprocess
import sys
import time
from pathlib import Path


ROOT = Path(os.environ["ADOBOT_ROOT"]).resolve()
VENV = ROOT / ".venv"
PYTHON = VENV / "bin" / "python"

MODE = os.environ.get("SUPERVISED_TARGET", "")
LOG = Path(os.environ["SUPERVISOR_LOG"]).resolve()
PID = Path(os.environ["SUPERVISOR_PID"]).resolve()
LOCK = Path(os.environ["SUPERVISOR_LOCK"]).resolve()

MAX_RESTARTS = 3
BACKOFF = (1, 2, 4)
STABLE_RUN_SECONDS = max(
    1,
    int(os.environ.get("SUPERVISOR_STABLE_RUN_SECONDS", "300")),
)

STOPPING = False
CHILD = None


def write_log(message: str) -> None:
    LOG.parent.mkdir(parents=True, exist_ok=True)
    with LOG.open("a", encoding="utf-8") as fh:
        fh.write(
            time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())
            + " "
            + message
            + "\n"
        )


def signal_handler(signum: int, _frame: object) -> None:
    global STOPPING

    STOPPING = True
    write_log(f"SUPERVISOR=SIGNAL signum={signum}")

    if CHILD is not None and CHILD.poll() is None:
        try:
            CHILD.send_signal(signum)
            write_log(f"SUPERVISOR=FORWARD_SIGNAL signum={signum}")
        except ProcessLookupError:
            pass


def load_dotenv(path: Path) -> dict[str, str]:
    env: dict[str, str] = {}

    if not path.is_file():
        raise RuntimeError("worker .env file missing")

    if path.stat().st_mode & 0o077:
        raise RuntimeError("worker .env permissions too broad")

    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()

        if not line or line.startswith("#"):
            continue

        if "=" not in line:
            raise RuntimeError("invalid .env line")

        key, value = line.split("=", 1)
        key = key.strip()
        value = value.strip()

        if not key:
            raise RuntimeError("empty .env key")

        env[key] = value

    return env


def command_for_target() -> tuple[list[str], dict[str, str]]:
    base = os.environ.copy()

    if not PYTHON.is_file():
        raise RuntimeError("authoritative Python missing")

    if MODE == "api":
        command = [
            str(PYTHON),
            "-m",
            "uvicorn",
            "app.main:app",
            "--host",
            "127.0.0.1",
            "--port",
            "8080",
            "--log-level",
            "info",
        ]
        return command, base

    if MODE == "worker":
        dotenv = load_dotenv(ROOT / ".env")

        token = dotenv.get("TELEGRAM_BOT_TOKEN", "")
        if not token:
            raise RuntimeError("Telegram token missing")

        # ENVIRONMENT is an explicit process-level setting, not a .env key.
        base.update(dotenv)
        base["ENVIRONMENT"] = "isolated"
        base["LOG_LEVEL"] = "info"

        # Never print or expose the token through supervisor logs.
        return [
            str(PYTHON),
            "-m",
            "adobot_telegram.bot",
        ], base

    raise RuntimeError("invalid SUPERVISED_TARGET")


def main() -> int:
    global CHILD

    LOG.parent.mkdir(parents=True, exist_ok=True)
    PID.parent.mkdir(parents=True, exist_ok=True)
    LOCK.parent.mkdir(parents=True, exist_ok=True)

    lock_fh = LOCK.open("a+", encoding="utf-8")

    try:
        try:
            fcntl.flock(lock_fh.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            write_log("SUPERVISOR=LOCK_CONTENTION")
            return 20

        PID.write_text(str(os.getpid()) + "\n", encoding="utf-8")
        write_log(f"SUPERVISOR=START target={MODE}")

        signal.signal(signal.SIGTERM, signal_handler)
        signal.signal(signal.SIGINT, signal_handler)
        signal.signal(signal.SIGHUP, signal_handler)

        restarts = 0

        while True:
            command, child_env = command_for_target()

            write_log(
                f"SUPERVISOR=LAUNCH attempt={restarts + 1} target={MODE}"
            )

            launched_at = time.monotonic()

            CHILD = subprocess.Popen(
                command,
                cwd=str(ROOT),
                env=child_env,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                text=True,
                bufsize=1,
            )

            write_log(f"CHILD=START pid={CHILD.pid}")

            if CHILD.stdout is not None:
                for line in CHILD.stdout:
                    line = line.rstrip("\r\n")

                    # Never copy token-bearing environment material into
                    # supervisor output. The child application itself is
                    # responsible for secret-safe logging.
                    if "TELEGRAM_BOT_TOKEN=" in line:
                        write_log("CHILD=SECRET_OUTPUT_BLOCKED")
                    else:
                        write_log("CHILD=" + line)

            rc = CHILD.wait()
            runtime = time.monotonic() - launched_at
            write_log(
                f"CHILD=RETURN_CODE rc={rc} "
                f"runtime_seconds={runtime:.1f}"
            )

            if STOPPING:
                write_log("SUPERVISOR=GRACEFUL_STOP")
                return 0

            # A sufficiently stable child run earns a fresh restart budget.
            # This prevents unrelated failures separated by long healthy
            # periods from accumulating into a permanent crash-loop stop.
            if runtime >= STABLE_RUN_SECONDS and restarts:
                write_log(
                    "SUPERVISOR=STABLE_RUN_RESET "
                    f"runtime_seconds={runtime:.1f} "
                    f"previous_restarts={restarts}"
                )
                restarts = 0

            # Any child termination not caused by an intentional supervisor
            # shutdown is treated as an unexpected service interruption.
            # This includes rc=0: a production worker disappearing cleanly
            # must not silently take the service offline.
            if rc == 0:
                write_log("SUPERVISOR=UNEXPECTED_CLEAN_EXIT")
            else:
                write_log(f"SUPERVISOR=UNEXPECTED_EXIT rc={rc}")

            if restarts >= MAX_RESTARTS:
                write_log("SUPERVISOR=CRASH_LOOP_STOP")
                return 30

            delay = BACKOFF[restarts]
            restarts += 1

            write_log(
                f"SUPERVISOR=RESTART restart={restarts} backoff={delay}"
            )
            time.sleep(delay)

    finally:
        try:
            if PID.exists():
                PID.unlink()
                write_log("PIDFILE=REMOVED")
        except OSError:
            pass

        try:
            fcntl.flock(lock_fh.fileno(), fcntl.LOCK_UN)
        finally:
            lock_fh.close()

        write_log("SUPERVISOR=EXIT")


if __name__ == "__main__":
    raise SystemExit(main())
