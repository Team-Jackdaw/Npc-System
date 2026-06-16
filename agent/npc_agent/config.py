from __future__ import annotations

import os
from dataclasses import dataclass


@dataclass(frozen=True)
class AgentConfig:
    mode: str = "pydantic_ai"
    provider: str = "ollama"
    model: str = "qwen3.5"
    api_key: str = ""
    base_url: str = "http://localhost:11434/v1"
    host: str = "0.0.0.0"
    port: int = 8765
    auth_token: str = ""
    state_dir: str = "config/npc-system/agent-state"
    skills_dir: str = "agent/skills"
    max_history_bytes: int = 65536


def load_config() -> AgentConfig:
    provider = os.getenv("NPC_AGENT_PROVIDER", "ollama").strip().lower()
    return AgentConfig(
        mode=os.getenv("NPC_AGENT_MODE", "pydantic_ai").strip().lower(),
        provider=provider,
        model=os.getenv("NPC_AGENT_MODEL", "qwen3.5"),
        api_key=os.getenv("NPC_AGENT_API_KEY", os.getenv("DEEPSEEK_API_KEY", "")),
        base_url=os.getenv(
            "NPC_AGENT_BASE_URL",
            os.getenv("NPC_AGENT_OLLAMA_BASE_URL", default_base_url(provider)),
        ),
        host=os.getenv("NPC_AGENT_HOST", "0.0.0.0"),
        port=int(os.getenv("NPC_AGENT_PORT", "8765")),
        auth_token=os.getenv("NPC_AGENT_AUTH_TOKEN", ""),
        state_dir=os.getenv("NPC_AGENT_STATE_DIR", "config/npc-system/agent-state"),
        skills_dir=os.getenv("NPC_AGENT_SKILLS_DIR", "agent/skills"),
        max_history_bytes=int(os.getenv("NPC_AGENT_MAX_HISTORY_BYTES", "65536")),
    )


def default_base_url(provider: str) -> str:
    if provider == "ollama":
        return "http://localhost:11434/v1"
    return ""
