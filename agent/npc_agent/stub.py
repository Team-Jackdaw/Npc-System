from __future__ import annotations

from .schemas import (
    AgentAction,
    DeliberateAgentRequest,
    DeliberateAgentResponse,
    FastAgentRequest,
    FastAgentResponse,
)


def decide_fast_stub(request: FastAgentRequest) -> FastAgentResponse:
    if is_fast_master(request) and "master_reply" in request.tools:
        message = latest_chat_text(request.evt)
        if message:
            return FastAgentResponse(
                rid=request.rid,
                a="call",
                kind="tool",
                name="master_reply",
                args={"message": clamp("我在。请说明你想查看或执行什么。", request.limits.max_reply_chars)},
                note="master_reply",
            )
    if "say" in request.tools:
        message = latest_chat_text(request.evt)
        if message:
            return FastAgentResponse(
                rid=request.rid,
                a="call",
                kind="task",
                name="say",
                args={"message": fast_reply(message, request.limits.max_reply_chars)},
                note="chat_reply",
            )
    return FastAgentResponse(rid=request.rid, a="none", note="no_action")


def decide_deliberate_stub(request: DeliberateAgentRequest) -> DeliberateAgentResponse:
    tool_names = {tool.name for tool in request.available_tools}
    message = request.conversation.message.strip()
    speaker = request.conversation.speaker.strip()
    lower_message = message.lower()

    if is_master(request) and "call_command" in tool_names and command_requested(lower_message):
        command = extract_command(message)
        if command:
            return DeliberateAgentResponse(
                request_id=request.request_id,
                actions=[
                    AgentAction(
                        type="call",
                        kind="tool",
                        name="call_command",
                        arguments={"command": command},
                        label="admin_command",
                    )
                ],
                speech=f"执行命令：/{command}",
                reasoning_summary="The administrator explicitly requested a Minecraft command.",
            )

    if is_master(request) and "master_reply" in tool_names and message:
        return DeliberateAgentResponse(
            request_id=request.request_id,
            action=AgentAction(
                type="call",
                kind="tool",
                name="master_reply",
                arguments={"message": deliberate_master_reply(message, request.limits.max_reply_chars)},
            ),
            speech=deliberate_master_reply(message, request.limits.max_reply_chars),
            reasoning_summary="The administrator sent a normal conversation message.",
        )

    if speaker and "follow_player" in tool_names and any(word in lower_message for word in ["follow", "跟着", "跟随"]):
        actions = [
            AgentAction(
                type="call",
                kind="task",
                name="follow_player",
                arguments={"player": speaker, "seconds": 30},
                label="follow_request",
            )
        ]
        if "say" in tool_names:
            actions.insert(
                0,
                AgentAction(
                    type="call",
                    kind="task",
                    name="say",
                    arguments={"message": "好，我跟着你。"},
                    label="reply",
                ),
            )
        return DeliberateAgentResponse(
            request_id=request.request_id,
            actions=actions,
            speech="好，我跟着你。",
            reasoning_summary="The player asked this NPC to follow.",
        )

    if message and "say" in tool_names:
        return DeliberateAgentResponse(
            request_id=request.request_id,
            action=AgentAction(
                type="call",
                kind="task",
                name="say",
                arguments={"message": deliberate_reply(message, request.limits.max_reply_chars)},
            ),
            speech=deliberate_reply(message, request.limits.max_reply_chars),
            reasoning_summary="The player sent a chat message and say is available.",
        )

    return DeliberateAgentResponse(
        request_id=request.request_id,
        action=AgentAction(type="none"),
        reasoning_summary="No useful action is available.",
    )


def is_master(request: DeliberateAgentRequest) -> bool:
    return request.npc.kind == "master" and request.npc.permission >= 3


def is_fast_master(request: FastAgentRequest) -> bool:
    return request.npc.kind == "master" and request.npc.permission >= 3


def command_requested(message: str) -> bool:
    return message.startswith("/") or "执行命令" in message or "run command" in message or "call command" in message


def extract_command(message: str) -> str:
    stripped = message.strip()
    if stripped.startswith("/"):
        return stripped[1:].strip()
    for marker in ["执行命令", "run command", "call command"]:
        index = stripped.lower().find(marker)
        if index >= 0:
            return stripped[index + len(marker):].strip().removeprefix("/").strip()
    return ""


def latest_chat_text(events: list[list[object]]) -> str:
    for event in reversed(events):
        if len(event) >= 3 and event[0] == "CHAT_HEARD":
            return str(event[2])
    return ""


def fast_reply(message: str, max_chars: int) -> str:
    reply = "我听到了。"
    return clamp(reply, max_chars)


def deliberate_reply(message: str, max_chars: int) -> str:
    reply = "我明白了。"
    return clamp(reply, max_chars)


def deliberate_master_reply(message: str, max_chars: int) -> str:
    reply = "我在。可以帮你查看服务器状态，或在你明确要求时执行管理员命令。"
    return clamp(reply, max_chars)


def clamp(text: str, max_chars: int) -> str:
    if max_chars <= 0 or len(text) <= max_chars:
        return text
    return text[:max_chars]
