from __future__ import annotations

import os
from dataclasses import dataclass


@dataclass(frozen=True)
class AgentConfig:
    mode: str = "stub"
    ollama_base_url: str = "http://localhost:11434/v1"
    model: str = "qwen3.5"
    host: str = "127.0.0.1"
    port: int = 8765
    auth_token: str = ""


def load_config() -> AgentConfig:
    return AgentConfig(
        mode=os.getenv("NPC_AGENT_MODE", "stub").strip().lower(),
        ollama_base_url=os.getenv("NPC_AGENT_OLLAMA_BASE_URL", "http://localhost:11434/v1"),
        model=os.getenv("NPC_AGENT_MODEL", "qwen3.5"),
        host=os.getenv("NPC_AGENT_HOST", "127.0.0.1"),
        port=int(os.getenv("NPC_AGENT_PORT", "8765")),
        auth_token=os.getenv("NPC_AGENT_AUTH_TOKEN", ""),
    )
