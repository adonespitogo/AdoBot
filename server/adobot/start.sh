#!/data/data/com.termux/files/usr/bin/bash
set -Eeuo pipefail
IFS=$'\n\t'
umask 077

ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
VENV="$ROOT/.venv"
PYTHON="$VENV/bin/python"
ENV_FILE="$ROOT/config/.env"
PID_FILE="$ROOT/run/adobot-server.pid"
LOG_DIR="$ROOT/log"
LOG_FILE="$LOG_DIR/adobot-server.log"

mkdir -p "$ROOT/run" "$LOG_DIR"
chmod 700 "$ROOT/run" "$LOG_DIR"

fail() {
    printf 'START=FAIL %s\n' "$*" >&2
    exit 1
}

[ -x "$PYTHON" ] || fail "virtualenv Python missing"
[ -f "$ENV_FILE" ] || fail "configuration missing"

set -a
. "$ENV_FILE"
set +a

"$PYTHON" "$ROOT/config_validate.py" >/dev/null \
    || fail "configuration validation failed"

HOST="${HOST:-127.0.0.1}"
PORT="${PORT:-8080}"

case "$HOST" in
    127.0.0.1|localhost|::1)
        ;;
    *)
        fail "refusing non-local HOST=$HOST"
        ;;
esac

if [ -f "$PID_FILE" ]; then
    OLD_PID="$(cat "$PID_FILE" 2>/dev/null || true)"

    if [ -n "$OLD_PID" ] &&
       [[ "$OLD_PID" =~ ^[0-9]+$ ]] &&
       kill -0 "$OLD_PID" 2>/dev/null; then
        fail "server already running pid=$OLD_PID"
    fi

    rm -f "$PID_FILE"
fi

printf '%s\n' "$$" > "$PID_FILE"
chmod 600 "$PID_FILE"

cleanup() {
    rm -f "$PID_FILE"
}

trap cleanup EXIT INT TERM

cd "$ROOT"
export PYTHONPATH="$ROOT${PYTHONPATH:+:$PYTHONPATH}"

printf 'START=PASS pid=%s host=%s port=%s root=%s\n' \
    "$$" "$HOST" "$PORT" "$ROOT"
printf 'LOG=%s\n' "$LOG_FILE"

exec "$PYTHON" -m uvicorn \
    app.main:app \
    --host "$HOST" \
    --port "$PORT" \
    --log-level "${LOG_LEVEL,,}" \
    >>"$LOG_FILE" 2>&1
