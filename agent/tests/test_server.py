from fastapi.testclient import TestClient

from npc_agent.server import app


client = TestClient(app)


def test_health_endpoint():
    response = client.get("/health")

    assert response.status_code == 200
    assert response.json()["status"] == "ok"


def test_fast_endpoint(monkeypatch, tmp_path):
    monkeypatch.setenv("NPC_AGENT_MODE", "stub")
    monkeypatch.setenv("NPC_AGENT_STATE_DIR", str(tmp_path))
    response = client.post(
        "/agent/fast",
        json={
            "rid": "r1",
            "npc": {"id": "npc", "name": "npc"},
            "evt": [["CHAT_HEARD", 7, "Steve: hello", 1]],
            "tools": [],
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert body["rid"] == "r1"
    assert body["a"] == "none"
    assert body["speech"]


def test_deliberate_endpoint(monkeypatch, tmp_path):
    monkeypatch.setenv("NPC_AGENT_MODE", "stub")
    monkeypatch.setenv("NPC_AGENT_STATE_DIR", str(tmp_path))
    response = client.post(
        "/agent/deliberate",
        json={
            "request_id": "r2",
            "npc": {"uuid": "npc", "name": "npc"},
            "conversation": {"speaker": "Steve", "message": "please follow me"},
            "available_tools": [{"name": "follow_player", "kind": "task"}],
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert body["request_id"] == "r2"
    assert body["speech"]
    assert body["action"]["name"] == "follow_player"
    assert "actions" not in body


def test_conversation_end_endpoint(monkeypatch, tmp_path):
    monkeypatch.setenv("NPC_AGENT_MODE", "stub")
    monkeypatch.setenv("NPC_AGENT_STATE_DIR", str(tmp_path))
    npc = tmp_path / "npc" / "npc"
    npc.mkdir(parents=True)
    (npc / "messages.json").write_text('[{"kind":"request","parts":[]}]', encoding="utf-8")

    response = client.post(
        "/agent/conversation/end",
        json={
            "request_id": "end-1",
            "npc": {"uuid": "npc", "name": "npc", "kind": "npc"},
            "reason": "test",
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert body["request_id"] == "end-1"
    assert body["memory_updated"] is True
    assert "Conversation Memory" in (npc / "MEMORY.md").read_text(encoding="utf-8")
