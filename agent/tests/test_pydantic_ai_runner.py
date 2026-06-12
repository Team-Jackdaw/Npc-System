import pytest

from npc_agent.config import AgentConfig
from npc_agent.pydantic_ai_runner import build_model


def test_builds_ollama_model():
    model = build_model(AgentConfig(provider="ollama", model="qwen3.5"))

    assert type(model).__name__ == "OllamaModel"


def test_builds_deepseek_model():
    model = build_model(AgentConfig(provider="deepseek", model="deepseek-chat", api_key="secret"))

    assert type(model).__name__ == "OpenAIChatModel"


def test_rejects_unknown_provider():
    with pytest.raises(ValueError):
        build_model(AgentConfig(provider="missing"))
