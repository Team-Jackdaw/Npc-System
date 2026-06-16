from npc_agent.schemas import (
    DeliberateAgentRequest,
    DeliberateAgentResponse,
    FastAgentRequest,
    FastAgentResponse,
)


def test_fast_schema_round_trip():
    request = FastAgentRequest.model_validate(
        {
            "v": 1,
            "rid": "r1",
            "mode": "fast",
            "npc": {
                "id": "npc-1",
                "name": "npc",
                "kind": "npc",
                "permission": 1,
                "task": "idle",
                "hp": 20.0,
                "pos": [1, 64, 2],
                "dim": "minecraft:overworld",
            },
            "evt": [["CHAT_HEARD", 7, "Steve: hi", 123]],
            "near": {"p": ["Steve@3.0"], "n": [], "e": []},
            "tools": [],
            "limits": {"max_reply_chars": 60},
            "future_field": "allowed",
        }
    )

    assert request.rid == "r1"
    assert request.npc.kind == "npc"
    assert request.npc.permission == 1
    assert request.evt[0][0] == "CHAT_HEARD"
    assert request.model_dump()["future_field"] == "allowed"

    response = FastAgentResponse(
        rid=request.rid,
        speech="hi",
    )
    assert response.speech == "hi"


def test_deliberate_schema_round_trip():
    request = DeliberateAgentRequest.model_validate(
        {
            "version": 1,
            "request_id": "r2",
            "mode": "deliberate",
            "npc": {
                "uuid": "npc-2",
                "name": "npc",
                "kind": "master",
                "permission": 3,
                "instruction": "act",
                "status": {"task": "idle"},
            },
            "observations": {"summary": "summary", "recent_events": [], "important_events": []},
            "conversation": {"speaker": "Steve", "message": "follow me", "history": []},
            "memory": {"recent": [], "relevant": []},
            "available_tools": [{"name": "follow_player", "kind": "task", "required": ["player"]}],
            "limits": {"max_reply_chars": 200},
            "future_field": "allowed",
        }
    )

    assert request.request_id == "r2"
    assert request.npc.kind == "master"
    assert request.npc.permission == 3
    assert request.available_tools[0].name == "follow_player"
    assert request.model_dump()["future_field"] == "allowed"

    response = DeliberateAgentResponse(
        request_id=request.request_id,
        action={"type": "call", "kind": "task", "name": "follow_player", "arguments": {"player": "Steve"}},
        speech="好。",
        reasoning_summary="follow request",
    )
    assert response.speech == "好。"
    assert response.action.name == "follow_player"
