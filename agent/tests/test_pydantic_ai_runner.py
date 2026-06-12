import pytest

from npc_agent.config import AgentConfig
from npc_agent.pydantic_ai_runner import build_model, summarize_context_pydantic_ai


def test_builds_ollama_model():
    model = build_model(AgentConfig(provider="ollama", model="qwen3.5"))

    assert type(model).__name__ == "OllamaModel"


def test_builds_deepseek_model():
    model = build_model(AgentConfig(provider="deepseek", model="deepseek-chat", api_key="secret"))

    assert type(model).__name__ == "OpenAIChatModel"


def test_rejects_unknown_provider():
    with pytest.raises(ValueError):
        build_model(AgentConfig(provider="missing"))


@pytest.mark.asyncio
async def test_summarize_context_uses_model_output(monkeypatch):
    class FakeResult:
        output = "玩家请求 NPC 帮忙寻找铁矿。"

    class FakeAgent:
        async def run(self, payload, instructions):
            assert "Steve asked for help" in payload
            assert "不要输出 JSON" in instructions
            return FakeResult()

    monkeypatch.setattr("npc_agent.pydantic_ai_runner.build_agent", lambda config, output_type: FakeAgent())

    summary = await summarize_context_pydantic_ai(
        "请总结。",
        {"messages": "Steve asked for help"},
        AgentConfig(provider="ollama"),
    )

    assert summary == "玩家请求 NPC 帮忙寻找铁矿。"
