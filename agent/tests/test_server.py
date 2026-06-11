from fastapi.testclient import TestClient

from npc_agent.server import app


client = TestClient(app)


def test_health_endpoint():
    response = client.get("/health")

    assert response.status_code == 200
    assert response.json()["status"] == "ok"


def test_fast_endpoint():
    response = client.post(
        "/agent/fast",
        json={
            "rid": "r1",
            "npc": {"id": "npc", "name": "npc"},
            "evt": [["CHAT_HEARD", 7, "Steve: hello", 1]],
            "tools": ["say"],
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert body["rid"] == "r1"
    assert body["a"] == "call"
    assert body["name"] == "say"


def test_deliberate_endpoint():
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
    assert body["actions"][0]["name"] == "follow_player"
