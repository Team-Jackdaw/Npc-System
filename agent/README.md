# NPC External Agent

Python scaffold for the external NPC agent described in `../docs/AGENT_INTERFACE.md`.
The default mode uses Pydantic AI with the configured model backend. A deterministic
offline stub is available for tests and local protocol debugging.

## Install

```bash
python3 -m pip install -r agent/requirements.txt
```

## Run

```bash
PYTHONPATH=agent python3 -m npc_agent.server
```

The server listens on `0.0.0.0:8765` by default.

Endpoints:

- `GET /health`
- `POST /agent/fast`
- `POST /agent/deliberate`
- `POST /agent/conversation/end`

## Configuration

Environment variables:

- `NPC_AGENT_MODE`: `pydantic_ai` or `stub`; defaults to `pydantic_ai`.
- `NPC_AGENT_PROVIDER`: `ollama`, `deepseek`, or `openai-compatible`; defaults to `ollama`.
- `NPC_AGENT_MODEL`: defaults to `qwen3.5`.
- `NPC_AGENT_API_KEY`: API key for hosted providers. `DEEPSEEK_API_KEY` is also accepted for DeepSeek.
- `NPC_AGENT_BASE_URL`: base URL for `ollama` or `openai-compatible` providers.
- `NPC_AGENT_OLLAMA_BASE_URL`: legacy Ollama base URL; defaults to `http://localhost:11434/v1` when provider is `ollama`.
- `NPC_AGENT_HOST`: defaults to `0.0.0.0`.
- `NPC_AGENT_PORT`: defaults to `8765`.
- `NPC_AGENT_AUTH_TOKEN`: optional bearer token.
- `NPC_AGENT_STATE_DIR`: defaults to `config/npc-system/agent-state`.
- `NPC_AGENT_MAX_HISTORY_BYTES`: defaults to `65536`.

## Context Storage

The agent stores one context directory per NPC/Master. Current conversation
state is saved as Pydantic AI `messages.json`; long conversations are compacted
into `SUMMARY.md`, and ended conversations are summarized into `MEMORY.md`.
`history.jsonl` is only for debug/audit logs.

On first creation, each context directory copies Markdown files from
`agent/templates`. NPCs use `agent/templates/npc/AGENTS.md`, Master uses
`agent/templates/master/AGENTS.md`, and both use common templates for
`SOLU.md`, `SUMMARY.md`, and `MEMORY.md`. Existing runtime files are never
overwritten.

Shared skills live under `agent/skills`. `dummy_skill.md` is a placeholder for
the future skill-loading system and is not injected into prompts yet.

To use the offline deterministic stub:

```bash
NPC_AGENT_MODE=stub \
PYTHONPATH=agent python3 -m npc_agent.server
```

To use Pydantic AI with Ollama, make sure Ollama is running and serving the
configured model, then start the agent normally or set `NPC_AGENT_MODE=pydantic_ai`
explicitly.

To use the official DeepSeek API:

```bash
NPC_AGENT_PROVIDER=deepseek \
NPC_AGENT_MODEL=deepseek-chat \
NPC_AGENT_API_KEY=sk-... \
PYTHONPATH=agent python3 -m npc_agent.server
```

For DeepSeek reasoner:

```bash
NPC_AGENT_PROVIDER=deepseek \
NPC_AGENT_MODEL=deepseek-reasoner \
NPC_AGENT_API_KEY=sk-... \
PYTHONPATH=agent python3 -m npc_agent.server
```

## Test

```bash
PYTHONPATH=agent pytest agent/tests
```

Model backend connectivity is skipped by default. To verify that Pydantic AI can
actually reach the configured Ollama model:

```bash
NPC_AGENT_RUN_MODEL_TESTS=1 \
NPC_AGENT_MODEL=qwen3.5 \
PYTHONPATH=agent pytest agent/tests/test_model_integration.py
```

For DeepSeek:

```bash
NPC_AGENT_RUN_MODEL_TESTS=1 \
NPC_AGENT_PROVIDER=deepseek \
NPC_AGENT_MODEL=deepseek-chat \
NPC_AGENT_API_KEY=sk-... \
PYTHONPATH=agent pytest agent/tests/test_model_integration.py
```
