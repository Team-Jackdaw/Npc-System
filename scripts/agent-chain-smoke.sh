#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUN_DIR="$ROOT_DIR/run"
LOG_FILE="$RUN_DIR/logs/npc-agent-chain-smoke.log"
AGENT_LOG_FILE="$RUN_DIR/logs/npc-agent-chain-agent.log"
COMMAND_FIFO="$RUN_DIR/npc-agent-chain.stdin"
JAVA_HOME_VALUE="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}"
TIMEOUT="${NPC_CHAIN_TIMEOUT:-360}"
TEST_MESSAGE="${NPC_CHAIN_MESSAGE:-终端复杂流程测试}"
WORLD_NAME="npc-agent-chain-world"

mkdir -p "$RUN_DIR/logs" "$RUN_DIR/config/npc-system"
echo "eula=true" > "$RUN_DIR/eula.txt"
rm -rf "$RUN_DIR/$WORLD_NAME"

touch "$RUN_DIR/server.properties"
set_server_property() {
  local key="$1"
  local value="$2"
  if grep -q "^${key}=" "$RUN_DIR/server.properties"; then
    sed -i "s/^${key}=.*/${key}=${value}/" "$RUN_DIR/server.properties"
  else
    printf '%s=%s\n' "$key" "$value" >> "$RUN_DIR/server.properties"
  fi
}
set_server_property "pause-when-empty-seconds" "-1"
set_server_property "spawn-protection" "0"
set_server_property "level-name" "$WORLD_NAME"

cat > "$RUN_DIR/config/npc-system/config.json" <<JSON
{
  "enabled": true,
  "debug": true,
  "agentEnabled": true,
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

(
  cd "$ROOT_DIR"
  PYTHONPATH=agent python3 -m npc_agent.server
) > "$AGENT_LOG_FILE" 2>&1 &
AGENT_PID="$!"

agent_deadline=$((SECONDS + 60))
until python3 - <<'PY'
import urllib.request

try:
    with urllib.request.urlopen("http://127.0.0.1:8765/health", timeout=2) as response:
        raise SystemExit(0 if response.status == 200 else 1)
except Exception:
    raise SystemExit(1)
PY
do
  if (( SECONDS > agent_deadline )); then
    echo "[npc-agent-chain] Agent did not become healthy within 60s." >&2
    exit 1
  fi
  sleep 1
done

cleanup() {
  if [[ -n "${SERVER_PID:-}" ]] && kill -0 "$SERVER_PID" 2>/dev/null; then
    printf 'stop\n' >&3 || true
    wait "$SERVER_PID" || true
  fi
  exec 3>&- || true
  rm -f "$COMMAND_FIFO"
  if kill -0 "$AGENT_PID" 2>/dev/null; then
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
    echo "[npc-agent-chain] Server did not start within ${TIMEOUT}s." >&2
    exit 1
  fi
  if ! kill -0 "$SERVER_PID" 2>/dev/null; then
    echo "[npc-agent-chain] Server process exited before startup." >&2
    exit 1
  fi
  sleep 1
done

send_command() {
  local command="$1"
  echo "[npc-agent-chain] > $command"
  printf '%s\n' "$command" >&3
}

send_command "npc debug on"
send_command "kill @e[type=npcsystem:npc]"
send_command "kill @e[name=TerminalTester]"
send_command "npc perf 1 1"
sleep 3
send_command "npc test chat $TEST_MESSAGE"

while ! grep -q '终端链路测试完成' "$LOG_FILE"; do
  if (( SECONDS > deadline )); then
    send_command "npc test status"
    echo "[npc-agent-chain] Timed out waiting for terminal chain completion." >&2
    echo "[npc-agent-chain] Server log: $LOG_FILE" >&2
    echo "[npc-agent-chain] Agent log: $AGENT_LOG_FILE" >&2
    exit 1
  fi
  sleep 2
  send_command "npc test status"
done

send_command "npc test status"
sleep 2
send_command "stop"
wait "$SERVER_PID"

if grep -Eiq 'Exception in server tick loop|NoClassDefFoundError|Crash Report|Caused by: java\.lang\.(NullPointerException|IllegalStateException)' "$LOG_FILE"; then
  echo "[npc-agent-chain] Crash/error keyword found in $LOG_FILE." >&2
  exit 1
fi

if ! grep -q 'Agent follow-up result' "$LOG_FILE"; then
  echo "[npc-agent-chain] No agent follow-up result was observed." >&2
  exit 1
fi

echo "[npc-agent-chain] Passed. Server log: $LOG_FILE Agent log: $AGENT_LOG_FILE"
