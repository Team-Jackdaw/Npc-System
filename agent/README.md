# NPC External Agent

Python scaffold for the external NPC agent described in `../docs/AGENT_INTERFACE.md`.
The default mode is deterministic and offline; real model calls are opt-in.

## Install

```bash
python3 -m pip install -r agent/requirements.txt
```

## Run

```bash
PYTHONPATH=agent python3 -m npc_agent.server
```

The server listens on `127.0.0.1:8765` by default.

Endpoints:

- `GET /health`
- `POST /agent/fast`
- `POST /agent/deliberate`
- `POST /agent/conversation/end`

## Configuration

Environment variables:

- `NPC_AGENT_MODE`: `stub` or `pydantic_ai`; defaults to `stub`.
- `NPC_AGENT_OLLAMA_BASE_URL`: defaults to `http://localhost:11434/v1`.
- `NPC_AGENT_MODEL`: defaults to `qwen3.5`.
- `NPC_AGENT_HOST`: defaults to `127.0.0.1`.
- `NPC_AGENT_PORT`: defaults to `8765`.
- `NPC_AGENT_AUTH_TOKEN`: optional bearer token.
- `NPC_AGENT_STATE_DIR`: defaults to `config/npc-system/agent-state`.
- `NPC_AGENT_MAX_HISTORY_BYTES`: defaults to `65536`.

## Context Storage

The agent stores one context directory per NPC/Master. Current conversation
state is saved as Pydantic AI `messages.json`; long conversations are compacted
into `SUMMARY.md`, and ended conversations are summarized into `MEMORY.md`.
`history.jsonl` is only for debug/audit logs.

To use Pydantic AI with Ollama:

```bash
NPC_AGENT_MODE=pydantic_ai \
NPC_AGENT_MODEL=qwen3.5 \
PYTHONPATH=agent python3 -m npc_agent.server
```

## Test

```bash
PYTHONPATH=agent pytest agent/tests
```
