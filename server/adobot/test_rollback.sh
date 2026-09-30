#!/data/data/com.termux/files/usr/bin/bash
set -u

R="$HOME/adobot-server/evidence/rollback-test-$(date -u +%Y%m%dT%H%M%SZ)"
mkdir -p "$R/live" "$R/backup" "$R/quarantine"

printf 'KNOWN-GOOD\n' > "$R/live/bot.py"
cp -a "$R/live/." "$R/backup/"

GOOD="$(sha256sum "$R/live/bot.py" | awk '{print $1}')"

printf 'STAGED\n' > "$R/staged.py"

mv "$R/live" "$R/quarantine/original-live"
mkdir "$R/live"
cp "$R/staged.py" "$R/live/bot.py"

echo "PROMOTION=PASS"

ATTEMPTED=0

rollback() {
    if [ "$ATTEMPTED" -eq 1 ]; then
        echo "ROLLBACK_ALREADY_ATTEMPTED=YES"
        return 0
    fi

    ATTEMPTED=1

    mv "$R/live" "$R/quarantine/replaced-live"

    if [ ! -d "$R/backup" ]; then
        echo "ROLLBACK=FAIL_BACKUP_MISSING"
        return 1
    fi

    mv "$R/backup" "$R/live"

    NOW="$(sha256sum "$R/live/bot.py" | awk '{print $1}')"

    [ "$NOW" = "$GOOD" ] || {
        echo "ROLLBACK=FAIL_HASH_MISMATCH"
        return 1
    }

    echo "ROLLBACK=PASS"
}

rollback

[ "$(cat "$R/live/bot.py")" = "KNOWN-GOOD" ] &&
    echo "RESTORE_HASH=PASS" ||
    echo "RESTORE_HASH=FAIL"

rollback

[ "$(cat "$R/live/bot.py")" = "KNOWN-GOOD" ] &&
    echo "IDEMPOTENCY=PASS" ||
    echo "IDEMPOTENCY=FAIL"

[ -d "$R/quarantine/original-live" ] &&
    echo "ORIGINAL_QUARANTINE=PASS" ||
    echo "ORIGINAL_QUARANTINE=FAIL"

[ -d "$R/quarantine/replaced-live" ] &&
    echo "PROMOTED_QUARANTINE=PASS" ||
    echo "PROMOTED_QUARANTINE=FAIL"

echo "EVIDENCE=$R"
