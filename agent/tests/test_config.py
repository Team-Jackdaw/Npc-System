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


def test_default_provider_is_ollama(monkeypatch):
    monkeypatch.delenv("NPC_AGENT_PROVIDER", raising=False)
    monkeypatch.delenv("NPC_AGENT_BASE_URL", raising=False)
    monkeypatch.delenv("NPC_AGENT_OLLAMA_BASE_URL", raising=False)

    config = load_config()

    assert config.provider == "ollama"
    assert config.base_url == "http://localhost:11434/v1"


def test_deepseek_provider_reads_api_key(monkeypatch):
    monkeypatch.setenv("NPC_AGENT_PROVIDER", "deepseek")
    monkeypatch.setenv("NPC_AGENT_MODEL", "deepseek-chat")
    monkeypatch.setenv("NPC_AGENT_API_KEY", "secret")

    config = load_config()

    assert config.provider == "deepseek"
    assert config.model == "deepseek-chat"
    assert config.api_key == "secret"
    assert config.base_url == ""
