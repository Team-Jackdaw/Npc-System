from __future__ import annotations

import os
from dataclasses import dataclass


@dataclass(frozen=True)
class AgentConfig:
    mode: str = "pydantic_ai"
    ollama_base_url: str = "http://localhost:11434/v1"
    model: str = "qwen3.5"
    host: str = "0.0.0.0"
    port: int = 8765
    auth_token: str = ""
    state_dir: str = "config/npc-system/agent-state"
    max_history_bytes: int = 65536


def load_config() -> AgentConfig:
    return AgentConfig(
        mode=os.getenv("NPC_AGENT_MODE", "pydantic_ai").strip().lower(),
        ollama_base_url=os.getenv("NPC_AGENT_OLLAMA_BASE_URL", "http://localhost:11434/v1"),
        model=os.getenv("NPC_AGENT_MODEL", "qwen3.5"),
        host=os.getenv("NPC_AGENT_HOST", "0.0.0.0"),
        port=int(os.getenv("NPC_AGENT_PORT", "8765")),
        auth_token=os.getenv("NPC_AGENT_AUTH_TOKEN", ""),
        state_dir=os.getenv("NPC_AGENT_STATE_DIR", "config/npc-system/agent-state"),
        max_history_bytes=int(os.getenv("NPC_AGENT_MAX_HISTORY_BYTES", "65536")),
    )
