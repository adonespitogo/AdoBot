#!/data/data/com.termux/files/usr/bin/bash
set -Eeuo pipefail

ROOT="${ADOBOT_ROOT:?ADOBOT_ROOT required}"

export ADOBOT_ROOT="$ROOT"
export SUPERVISED_TARGET=api
export SUPERVISOR_LOG="$ROOT/runtime/adobot-supervisor.log"
export SUPERVISOR_PID="$ROOT/runtime/adobot-supervisor.pid"
export SUPERVISOR_LOCK="$ROOT/runtime/adobot-supervisor.lock"
export SUPERVISOR_STABLE_RUN_SECONDS=300

exec "$ROOT/.venv/bin/python" \
  "$ROOT/runtime/production-supervisor.py"
