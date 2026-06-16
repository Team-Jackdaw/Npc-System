#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUN_DIR="$ROOT_DIR/run"
LOG_FILE="$RUN_DIR/logs/npc-smoke.log"
COMMAND_FIFO="$RUN_DIR/npc-smoke.stdin"
JAVA_HOME_VALUE="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}"
COUNT="${NPC_SMOKE_COUNT:-20}"
DURATION="${NPC_SMOKE_SECONDS:-10}"
TIMEOUT="${NPC_SMOKE_TIMEOUT:-180}"
WITH_AGENT="${NPC_SMOKE_AGENT:-0}"
AGENT_ENABLED_JSON="false"
if [[ "$WITH_AGENT" == "1" || "$WITH_AGENT" == "true" ]]; then
  AGENT_ENABLED_JSON="true"
fi

mkdir -p "$RUN_DIR/logs" "$RUN_DIR/config/npc-system"
echo "eula=true" > "$RUN_DIR/eula.txt"

cat > "$RUN_DIR/config/npc-system/config.json" <<JSON
{
  "enabled": true,
  "debug": true,
  "agentEnabled": ${AGENT_ENABLED_JSON},
  "agentBaseUrl": "http://127.0.0.1:8765",
  "agentMode": "deliberate",
  "agentAuthToken": "",
  "range": 10.0,
  "isBubble": false,
  "isChatBar": true,
  "textBackgroundColor": "DEFAULT",
  "timeLastingPerChar": 500
}
JSON

AGENT_PID=""
if [[ "$WITH_AGENT" == "1" || "$WITH_AGENT" == "true" ]]; then
  (
    cd "$ROOT_DIR"
    NPC_AGENT_MODE=stub PYTHONPATH=agent python3 -m npc_agent.server
  ) > "$RUN_DIR/logs/npc-agent-smoke.log" 2>&1 &
  AGENT_PID="$!"
fi

cleanup() {
  if [[ -n "${SERVER_PID:-}" ]] && kill -0 "$SERVER_PID" 2>/dev/null; then
    printf 'stop\n' >&3 || true
    wait "$SERVER_PID" || true
  fi
  exec 3>&- || true
  rm -f "$COMMAND_FIFO"
  if [[ -n "$AGENT_PID" ]] && kill -0 "$AGENT_PID" 2>/dev/null; then
    kill "$AGENT_PID" || true
    wait "$AGENT_PID" || true
  fi
}
trap cleanup EXIT

: > "$LOG_FILE"
rm -f "$COMMAND_FIFO"
mkfifo "$COMMAND_FIFO"
cd "$ROOT_DIR"
JAVA_HOME="$JAVA_HOME_VALUE" ./gradlew runServer --no-daemon < "$COMMAND_FIFO" 2>&1 | tee -a "$LOG_FILE" &
SERVER_PID="$!"
exec 3>"$COMMAND_FIFO"

deadline=$((SECONDS + TIMEOUT))
while ! grep -Eq 'Done \(|For help, type "help"' "$LOG_FILE"; do
  if (( SECONDS > deadline )); then
    echo "[npc-smoke] Server did not start within ${TIMEOUT}s." >&2
    exit 1
  fi
  if ! kill -0 "$SERVER_PID" 2>/dev/null; then
    echo "[npc-smoke] Server process exited before startup." >&2
    exit 1
  fi
  sleep 1
done

send_command() {
  local command="$1"
  echo "[npc-smoke] > $command"
  printf '%s\n' "$command" >&3
}

send_command "npc"
send_command "npc debug on"
send_command "npc spawnAt 0 100 0 5"
send_command "npc debug"
if [[ "$WITH_AGENT" == "1" || "$WITH_AGENT" == "true" ]]; then
  send_command "npc master smoke test"
fi
send_command "npc perf $COUNT $DURATION"

sleep "$((DURATION + 3))"
send_command "npc perf status"
sleep 2
send_command "stop"

wait "$SERVER_PID"

if grep -Eiq 'Exception in server tick loop|NoClassDefFoundError|Crash Report|Caused by: java\.lang\.(NullPointerException|IllegalStateException)' "$LOG_FILE"; then
  echo "[npc-smoke] Crash/error keyword found in $LOG_FILE." >&2
  exit 1
fi

if ! grep -q 'Terminal perf sampling finished' "$LOG_FILE"; then
  echo "[npc-smoke] Perf sample did not finish." >&2
  exit 1
fi

echo "[npc-smoke] Passed. Log: $LOG_FILE"
