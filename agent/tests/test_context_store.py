from npc_agent.config import AgentConfig
from npc_agent.context_store import AgentContextStore, TEMPLATE_ROOT
from npc_agent.schemas import ConversationEndRequest, DeliberateAgentRequest, FastAgentRequest


def test_context_store_creates_default_files(tmp_path):
    config = AgentConfig(state_dir=str(tmp_path))
    request = DeliberateAgentRequest.model_validate(
        {"request_id": "r1", "npc": {"uuid": "npc-1", "kind": "npc"}}
    )

    context = AgentContextStore(config).for_deliberate(request)

    assert (context.root / "AGENTS.md").exists()
    assert (context.root / "SOLU.md").exists()
    assert (context.root / "SUMMARY.md").exists()
    assert (context.root / "MEMORY.md").exists()
    assert (context.root / "messages.json").read_text(encoding="utf-8") == "[]"
    assert (context.root / "history.jsonl").exists()


def test_context_store_removes_legacy_memory_and_rag_dirs(tmp_path):
    state_dir = tmp_path / "config" / "npc-system" / "agent-state"
    legacy_memory = state_dir.parent / "memory"
    legacy_rag = state_dir.parent / "rag"
    legacy_state_memory = state_dir / "memory"
    legacy_state_rag = state_dir / "rag"
    for path in (legacy_memory, legacy_rag, legacy_state_memory, legacy_state_rag):
        path.mkdir(parents=True)
        (path / "old.txt").write_text("legacy", encoding="utf-8")
    config = AgentConfig(state_dir=str(state_dir))
    request = DeliberateAgentRequest.model_validate(
        {"request_id": "r1", "npc": {"uuid": "npc-1", "kind": "npc"}}
    )

    AgentContextStore(config).for_deliberate(request)

    assert not legacy_memory.exists()
    assert not legacy_rag.exists()
    assert not legacy_state_memory.exists()
    assert not legacy_state_rag.exists()


def test_context_store_copies_npc_templates(tmp_path):
    config = AgentConfig(state_dir=str(tmp_path))
    request = DeliberateAgentRequest.model_validate(
        {"request_id": "r1", "npc": {"uuid": "npc-1", "kind": "npc"}}
    )

    context = AgentContextStore(config).for_deliberate(request)

    assert (context.root / "AGENTS.md").read_text(encoding="utf-8") == (
        TEMPLATE_ROOT / "npc" / "AGENTS.md"
    ).read_text(encoding="utf-8")
    assert (context.root / "SOLU.md").read_text(encoding="utf-8") == (
        TEMPLATE_ROOT / "common" / "SOLU.md"
    ).read_text(encoding="utf-8")
    assert (context.root / "SUMMARY.md").read_text(encoding="utf-8") == (
        TEMPLATE_ROOT / "common" / "SUMMARY.md"
    ).read_text(encoding="utf-8")
    assert (context.root / "MEMORY.md").read_text(encoding="utf-8") == (
        TEMPLATE_ROOT / "common" / "MEMORY.md"
    ).read_text(encoding="utf-8")


def test_context_store_copies_master_template(tmp_path):
    config = AgentConfig(state_dir=str(tmp_path))
    request = DeliberateAgentRequest.model_validate(
        {"request_id": "r1", "npc": {"uuid": "master-1", "kind": "master", "permission": 3}}
    )

    context = AgentContextStore(config).for_deliberate(request)

    assert (context.root / "AGENTS.md").read_text(encoding="utf-8") == (
        TEMPLATE_ROOT / "master" / "AGENTS.md"
    ).read_text(encoding="utf-8")


def test_context_store_does_not_overwrite_existing_files(tmp_path):
    existing = tmp_path / "npc" / "npc-1"
    existing.mkdir(parents=True)
    (existing / "AGENTS.md").write_text("# Custom Agent\n", encoding="utf-8")
    config = AgentConfig(state_dir=str(tmp_path))
    request = DeliberateAgentRequest.model_validate(
        {"request_id": "r1", "npc": {"uuid": "npc-1", "kind": "npc"}}
    )

    context = AgentContextStore(config).for_deliberate(request)

    assert (context.root / "AGENTS.md").read_text(encoding="utf-8") == "# Custom Agent\n"


def test_context_store_uses_fallback_when_template_is_missing(tmp_path, monkeypatch):
    monkeypatch.setattr("npc_agent.context_store.TEMPLATE_ROOT", tmp_path / "missing")
    config = AgentConfig(state_dir=str(tmp_path / "state"))
    request = DeliberateAgentRequest.model_validate(
        {"request_id": "r1", "npc": {"uuid": "npc-1", "kind": "npc"}}
    )

    context = AgentContextStore(config).for_deliberate(request)

    assert "Minecraft NPC" in (context.root / "AGENTS.md").read_text(encoding="utf-8")


def test_fast_and_master_use_separate_context_directories(tmp_path):
    config = AgentConfig(state_dir=str(tmp_path))
    npc_request = FastAgentRequest.model_validate({"rid": "r1", "npc": {"id": "npc-1", "kind": "npc"}})
    master_request = DeliberateAgentRequest.model_validate(
        {"request_id": "r2", "npc": {"uuid": "master-1", "kind": "master", "permission": 3}}
    )

    npc_context = AgentContextStore(config).for_fast(npc_request)
    master_context = AgentContextStore(config).for_deliberate(master_request)

    assert npc_context.root == tmp_path / "npc" / "npc-1"
    assert master_context.root == tmp_path / "master" / "default"


def test_master_context_ignores_changing_uuid(tmp_path):
    config = AgentConfig(state_dir=str(tmp_path))
    first = DeliberateAgentRequest.model_validate(
        {"request_id": "r1", "npc": {"uuid": "master-1", "kind": "master", "permission": 3}}
    )
    second = DeliberateAgentRequest.model_validate(
        {"request_id": "r2", "npc": {"uuid": "master-2", "kind": "master", "permission": 3}}
    )

    first_context = AgentContextStore(config).for_deliberate(first)
    second_context = AgentContextStore(config).for_deliberate(second)

    assert first_context.root == tmp_path / "master" / "default"
    assert second_context.root == first_context.root
    assert first_context.agent_id == "default"


def test_memory_updates_are_written_to_memory(tmp_path):
    config = AgentConfig(state_dir=str(tmp_path))
    request = DeliberateAgentRequest.model_validate(
        {"request_id": "r1", "npc": {"uuid": "npc-1", "kind": "npc"}}
    )
    context = AgentContextStore(config).for_deliberate(request)

    context.apply_memory_updates(["Steve likes mining."])

    assert "Steve likes mining." in (context.root / "MEMORY.md").read_text(encoding="utf-8")


def test_compacts_messages_when_history_exceeds_limit(tmp_path):
    config = AgentConfig(state_dir=str(tmp_path), max_history_bytes=10)
    request = DeliberateAgentRequest.model_validate(
        {"request_id": "r1", "npc": {"uuid": "npc-1", "kind": "npc"}}
    )
    context = AgentContextStore(config).for_deliberate(request)

    compacted = context.compact_if_needed()

    assert compacted is False
    context.save_messages(b'[{"kind":"request","parts":[{"content":"very long history"}]}]')
    assert "Compressed Context" in (context.root / "SUMMARY.md").read_text(encoding="utf-8")
    assert (context.root / "messages.json").read_text(encoding="utf-8") == "[]"


def test_end_conversation_writes_memory_and_resets_current_context(tmp_path):
    config = AgentConfig(state_dir=str(tmp_path))
    request = ConversationEndRequest.model_validate(
        {"request_id": "end-1", "npc": {"uuid": "npc-1", "kind": "npc"}, "reason": "ended"}
    )
    context = AgentContextStore(config).for_end(request)
    context.save_messages(b'[{"kind":"request","parts":[{"content":"Steve asked for help"}]}]')

    updated = context.end_conversation(request)

    assert updated is True
    assert "Steve asked for help" in (context.root / "MEMORY.md").read_text(encoding="utf-8")
    assert (context.root / "messages.json").read_text(encoding="utf-8") == "[]"
    assert (context.root / "SUMMARY.md").read_text(encoding="utf-8") == "# Summary\n\n"
