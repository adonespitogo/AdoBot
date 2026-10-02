"""Runtime status helpers for the isolated AdoBot Telegram worker."""

from __future__ import annotations

import os
import time
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class RuntimeStatus:
    """Non-sensitive operational state."""

    pid: int
    transport: str
    uptime_seconds: int
    environment: str = "isolated"

    @property
    def uptime_text(self) -> str:
        total = max(0, self.uptime_seconds)
        hours, remainder = divmod(total, 3600)
        minutes, seconds = divmod(remainder, 60)
        return f"{hours:02d}:{minutes:02d}:{seconds:02d}"

    def render(self) -> str:
        return (
            "AdoBot Telegram worker: online\n"
            f"Transport: {self.transport}\n"
            f"PID: {self.pid}\n"
            f"Uptime: {self.uptime_text}\n"
            f"Environment: {self.environment}"
        )


def read_recorded_pid(pid_file: Path) -> int:
    """Read and validate the worker PID without mutating anything."""

    raw = pid_file.read_text(encoding="utf-8").strip()

    if not raw.isdigit():
        raise RuntimeError("worker PID file contains invalid data")

    pid = int(raw)

    if pid <= 0:
        raise RuntimeError("worker PID file contains invalid PID")

    return pid


def build_runtime_status(
    *,
    started_monotonic: float,
    pid_file: Path,
    transport: str = "long polling",
) -> RuntimeStatus:
    """Build sanitized runtime status from local process state."""

    recorded_pid = read_recorded_pid(pid_file)
    current_pid = os.getpid()

    if recorded_pid != current_pid:
        raise RuntimeError(
            "worker PID ownership mismatch"
        )

    uptime = int(max(0, time.monotonic() - started_monotonic))

    return RuntimeStatus(
        pid=current_pid,
        transport=transport,
        uptime_seconds=uptime,
    )
