from npc_agent.config import load_config


def test_default_host_allows_remote_access(monkeypatch):
    monkeypatch.delenv("NPC_AGENT_HOST", raising=False)

    config = load_config()

    assert config.host == "0.0.0.0"


def test_default_mode_uses_pydantic_ai(monkeypatch):
    monkeypatch.delenv("NPC_AGENT_MODE", raising=False)

    config = load_config()

    assert config.mode == "pydantic_ai"


def test_host_can_be_overridden(monkeypatch):
    monkeypatch.setenv("NPC_AGENT_HOST", "127.0.0.1")

    config = load_config()

    assert config.host == "127.0.0.1"
