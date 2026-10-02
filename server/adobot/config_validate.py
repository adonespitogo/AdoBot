#!/usr/bin/env python3

from __future__ import annotations

import os
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent
ENV_FILE = ROOT / "config" / ".env"

REQUIRED = {
    "APP_NAME",
    "ENVIRONMENT",
    "HOST",
    "PORT",
    "LOG_LEVEL",
}

INTEGER_FIELDS = {"PORT"}
ALLOWED_LOG_LEVELS = {"DEBUG", "INFO", "WARNING", "ERROR", "CRITICAL"}

KEY_RE = re.compile(r"^[A-Z][A-Z0-9_]*$")


def fail(message: str) -> None:
    print(f"CONFIG=FAIL {message}", file=sys.stderr)
    raise SystemExit(1)


def load_env(path: Path) -> dict[str, str]:
    if not path.is_file():
        fail(f"missing {path}")

    values: dict[str, str] = {}

    for number, raw in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        line = raw.strip()

        if not line or line.startswith("#"):
            continue

        if "=" not in line:
            fail(f"line {number}: expected KEY=VALUE")

        key, value = line.split("=", 1)
        key = key.strip()
        value = value.strip()

        if not KEY_RE.fullmatch(key):
            fail(f"line {number}: invalid key {key!r}")

        if "\x00" in value:
            fail(f"line {number}: NUL byte")

        values[key] = value

    return values


def main() -> int:
    values = load_env(ENV_FILE)

    missing = sorted(REQUIRED - values.keys())
    if missing:
        fail("missing keys: " + ", ".join(missing))

    empty = sorted(k for k in REQUIRED if not values[k])
    if empty:
        fail("empty keys: " + ", ".join(empty))

    try:
        port = int(values["PORT"])
    except ValueError:
        fail("PORT must be an integer")

    if not 1 <= port <= 65535:
        fail("PORT must be between 1 and 65535")

    if values["LOG_LEVEL"].upper() not in ALLOWED_LOG_LEVELS:
        fail("invalid LOG_LEVEL")

    if values["ENVIRONMENT"] != "isolated":
        fail("ENVIRONMENT must remain 'isolated'")

    # Fail closed: refuse accidental public binding.
    host = values["HOST"]
    if host not in {"127.0.0.1", "localhost", "::1"}:
        fail(f"unsafe HOST binding: {host!r}")

    print("CONFIG=PASS")
    print(f"CONFIG_FILE={ENV_FILE}")
    print(f"APP_NAME={values['APP_NAME']}")
    print(f"ENVIRONMENT={values['ENVIRONMENT']}")
    print(f"HOST={host}")
    print(f"PORT={port}")
    print(f"LOG_LEVEL={values['LOG_LEVEL'].upper()}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
