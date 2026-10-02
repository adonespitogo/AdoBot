#!/data/data/com.termux/files/usr/bin/bash

set -uo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
PYTHON="$ROOT/.venv/bin/python"
TS="$(date -u +%Y%m%dT%H%M%SZ)"
EVIDENCE="$ROOT/evidence/operational-gate-$TS"

if [ ! -x "$PYTHON" ]; then
    echo "AUTHORITATIVE_PYTHON=FAIL"
    echo "MISSING=$PYTHON"
    exit 1
fi

mkdir -p "$EVIDENCE"

LOG="$EVIDENCE/gate.log"

PASS=0
FAIL=0

record_pass() {
    echo "$1=PASS" | tee -a "$LOG"
    PASS=$((PASS + 1))
}

record_fail() {
    echo "$1=FAIL" | tee -a "$LOG"
    FAIL=$((FAIL + 1))
}

{
    echo "=== ADOBOT-SERVER OPERATIONAL GATE ==="
    echo "UTC=$(date -u)"
    echo "ROOT=$ROOT"
    echo "EVIDENCE=$EVIDENCE"
    echo

    echo "=== TESTS ==="
    if "$PYTHON" -m pytest -q tests; then
        record_pass "SCOPED_TESTS"
    else
        record_fail "SCOPED_TESTS"
    fi

    echo
    echo "=== COMPILE ==="
    if "$PYTHON" -m compileall -q \
        adobot_telegram \
        app \
        config_validate.py
    then
        record_pass "SCOPED_COMPILE"
    else
        record_fail "SCOPED_COMPILE"
    fi

    echo
    echo "=== ROLLBACK REGRESSION ==="
    ROLLBACK_OUTPUT="$EVIDENCE/rollback.log"

    if ./test_rollback.sh >"$ROLLBACK_OUTPUT" 2>&1; then
        cat "$ROLLBACK_OUTPUT"

        REQUIRED=(
            "PROMOTION=PASS"
            "ROLLBACK=PASS"
            "RESTORE_HASH=PASS"
            "ROLLBACK_ALREADY_ATTEMPTED=YES"
            "IDEMPOTENCY=PASS"
            "ORIGINAL_QUARANTINE=PASS"
            "PROMOTED_QUARANTINE=PASS"
        )

        ROLLBACK_OK=1

        for EXPECTED in "${REQUIRED[@]}"; do
            if ! grep -Fxq "$EXPECTED" "$ROLLBACK_OUTPUT"; then
                echo "MISSING:$EXPECTED"
                ROLLBACK_OK=0
            fi
        done

        if [ "$ROLLBACK_OK" -eq 1 ]; then
            record_pass "ROLLBACK_REGRESSION"
        else
            record_fail "ROLLBACK_REGRESSION"
        fi
    else
        cat "$ROLLBACK_OUTPUT"
        record_fail "ROLLBACK_REGRESSION"
    fi

    echo
    echo "=== RESULT ==="
    echo "PASS_COUNT=$PASS"
    echo "FAIL_COUNT=$FAIL"

    if [ "$FAIL" -eq 0 ]; then
        echo "OPERATIONAL_GATE=PASS"
    else
        echo "OPERATIONAL_GATE=FAIL"
    fi

} 2>&1 | tee "$LOG"

FINAL_STATUS=${PIPESTATUS[0]}

echo
echo "EVIDENCE=$EVIDENCE"

if grep -q '^OPERATIONAL_GATE=PASS$' "$LOG" && [ "$FINAL_STATUS" -eq 0 ]; then
    exit 0
fi

exit 1
