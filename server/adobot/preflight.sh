#!/data/data/com.termux/files/usr/bin/bash
set -Eeuo pipefail
IFS=$'\n\t'
umask 077

ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
VENV="$ROOT/.venv"
PYTHON="$VENV/bin/python"
EVIDENCE="$ROOT/evidence"

mkdir -p "$EVIDENCE"
chmod 700 "$EVIDENCE"

STAMP="$(date -u '+%Y%m%dT%H%M%SZ')"
REPORT="$EVIDENCE/preflight-$STAMP.txt"

exec > >(tee "$REPORT") 2>&1

fail() {
    printf 'PREFLIGHT=FAIL %s\n' "$*" >&2
    exit 1
}

printf '%s\n' '=== AdoBot Server Preflight ==='
printf 'timestamp=%s\n' "$STAMP"
printf 'root=%s\n' "$ROOT"

[ -d "$ROOT" ] || fail "missing root"
[ -x "$PYTHON" ] || fail "missing virtualenv Python"
[ -f "$ROOT/requirements.txt" ] || fail "missing requirements.txt"
[ -f "$ROOT/config/.env" ] || fail "missing config/.env"
[ -f "$ROOT/config_validate.py" ] || fail "missing config validator"
[ -f "$ROOT/app/main.py" ] || fail "missing application"

printf 'FILESYSTEM=PASS\n'

PYTHON_VERSION="$("$PYTHON" -c 'import sys; print(sys.version.split()[0])')"
printf 'PYTHON=PASS %s\n' "$PYTHON_VERSION"

"$PYTHON" "$ROOT/config_validate.py" \
    || fail "configuration validation failed"

"$PYTHON" - <<'PY' \
    || exit 1
import os
import sys

ROOT = os.path.expanduser("~/adobot-server")
sys.path.insert(0, ROOT)

os.environ.setdefault("APP_NAME", "adobot-server")
os.environ.setdefault("ENVIRONMENT", "isolated")
os.environ.setdefault("LOG_LEVEL", "INFO")

try:
    import starlette
    import uvicorn
except Exception as exc:
    print(f"DEPENDENCIES=FAIL {exc}")
    raise SystemExit(1)

from app.main import app

required = {"/", "/health", "/ready", "/info"}
actual = {
    getattr(route, "path", None)
    for route in app.routes
}

missing = required - actual

if missing:
    print(f"ROUTES=FAIL missing={sorted(missing)}")
    raise SystemExit(1)

print(f"STARLETTE=PASS {starlette.__version__}")
print(f"UVICORN=PASS {uvicorn.__version__}")
print("ROUTES=PASS / /health /ready /info")
PY

printf 'IMPORTS=PASS\n'

"$PYTHON" -m compileall -q "$ROOT/app" "$ROOT/config_validate.py" \
    || fail "Python compilation failed"

printf 'COMPILE=PASS\n'

if [ "$(id -u)" = "0" ]; then
    fail "refusing to operate as root"
fi

printf 'USER=PASS uid=%s\n' "$(id -u)"

case "$(python -c 'import platform; print(platform.machine())')" in
    aarch64|arm64)
        printf 'ARCH=PASS aarch64-compatible\n'
        ;;
    *)
        printf 'ARCH=INFO %s\n' "$(python -c 'import platform; print(platform.machine())')"
        ;;
esac

DISK_LINE="$(df -h "$ROOT" | tail -1)"
printf 'DISK=%s\n' "$DISK_LINE"

printf 'PREFLIGHT=PASS\n'
printf 'REPORT=%s\n' "$REPORT"
