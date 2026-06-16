from npc_agent.schemas import DeliberateAgentRequest, FastAgentRequest
from npc_agent.stub import decide_deliberate_stub, decide_fast_stub


def test_fast_stub_replies_to_chat_with_speech():
    request = FastAgentRequest.model_validate(
        {
            "rid": "r1",
            "npc": {"id": "npc", "name": "npc"},
            "evt": [["CHAT_HEARD", 7, "Steve: hello", 1]],
            "tools": [],
        }
    )

    response = decide_fast_stub(request)

    assert response.a == "none"
    assert response.speech


def test_fast_stub_replies_even_without_tools():
    request = FastAgentRequest.model_validate(
        {
            "rid": "r1",
            "npc": {"id": "npc", "name": "npc"},
            "evt": [["CHAT_HEARD", 7, "Steve: hello", 1]],
            "tools": [],
        }
    )

    response = decide_fast_stub(request)

    assert response.a == "none"
    assert response.speech


def test_fast_stub_does_not_repeat_chat_on_task_follow_up():
    request = FastAgentRequest.model_validate(
        {
            "rid": "r1",
            "npc": {"id": "npc", "name": "npc"},
            "evt": [
                ["CHAT_HEARD", 7, "Steve: hello", 1],
                ["TASK_BATCH_FINISHED", 8, "speak:finished", 2],
            ],
            "tools": [],
        }
    )

    response = decide_fast_stub(request)

    assert response.a == "none"
    assert response.note == "task_batch_finished"


def test_deliberate_stub_follows_player_when_requested():
    request = DeliberateAgentRequest.model_validate(
        {
            "request_id": "r2",
            "npc": {"uuid": "npc", "name": "npc"},
            "conversation": {"speaker": "Steve", "message": "please follow me"},
            "available_tools": [{"name": "follow_player", "kind": "task"}],
        }
    )

    response = decide_deliberate_stub(request)

    assert response.action.name == "follow_player"
    assert response.action.arguments["player"] == "Steve"


def test_deliberate_stub_returns_speech_then_follow_action():
    request = DeliberateAgentRequest.model_validate(
        {
            "request_id": "r2",
            "npc": {"uuid": "npc", "name": "npc"},
            "conversation": {"speaker": "Steve", "message": "please follow me"},
            "available_tools": [{"name": "follow_player", "kind": "task"}],
        }
    )

    response = decide_deliberate_stub(request)

    assert response.speech == "好，我跟着你。"
    assert response.action.name == "follow_player"


def test_deliberate_stub_replies_without_matching_tool():
    request = DeliberateAgentRequest.model_validate(
        {
            "request_id": "r2",
            "npc": {"uuid": "npc", "name": "npc"},
            "conversation": {"speaker": "Steve", "message": "please follow me"},
            "available_tools": [],
        }
    )

    response = decide_deliberate_stub(request)

    assert response.action.type == "none"
    assert response.speech


def test_master_stub_can_call_command_when_authorized():
    request = DeliberateAgentRequest.model_validate(
        {
            "request_id": "master-1",
            "npc": {"uuid": "master", "name": "Master", "kind": "master", "permission": 3},
            "conversation": {"speaker": "Admin", "message": "/time set day"},
            "available_tools": [{"name": "call_command", "kind": "tool"}],
        }
    )

    response = decide_deliberate_stub(request)

    assert response.action.name == "call_command"
    assert response.action.arguments["command"] == "time set day"


def test_master_stub_replies_to_normal_conversation():
    request = DeliberateAgentRequest.model_validate(
        {
            "request_id": "master-2",
            "npc": {"uuid": "master", "name": "Master", "kind": "master", "permission": 3},
            "conversation": {"speaker": "Admin", "message": "你好"},
            "available_tools": [],
        }
    )

    response = decide_deliberate_stub(request)

    assert response.action.type == "none"
    assert response.speech


def test_npc_stub_cannot_call_command_even_if_tool_is_present():
    request = DeliberateAgentRequest.model_validate(
        {
            "request_id": "npc-1",
            "npc": {"uuid": "npc", "name": "npc", "kind": "npc", "permission": 1},
            "conversation": {"speaker": "Steve", "message": "/time set day"},
            "available_tools": [{"name": "call_command", "kind": "tool"}],
        }
    )

    response = decide_deliberate_stub(request)

    assert response.action.type == "none"
    assert response.speech
