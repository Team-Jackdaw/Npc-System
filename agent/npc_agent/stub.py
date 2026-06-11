from __future__ import annotations

from .schemas import (
    AgentAction,
    DeliberateAgentRequest,
    DeliberateAgentResponse,
    FastAgentRequest,
    FastAgentResponse,
)


def decide_fast_stub(request: FastAgentRequest) -> FastAgentResponse:
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

    if speaker and "follow_player" in tool_names and any(word in lower_message for word in ["follow", "跟着", "跟随"]):
        return DeliberateAgentResponse(
            request_id=request.request_id,
            action=AgentAction(
                type="call",
                kind="task",
                name="follow_player",
                arguments={"player": speaker, "seconds": 30},
            ),
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


def clamp(text: str, max_chars: int) -> str:
    if max_chars <= 0 or len(text) <= max_chars:
        return text
    return text[:max_chars]
