from __future__ import annotations

from .config import AgentConfig
from .context_store import AgentContextStore
from .pydantic_ai_runner import decide_deliberate_pydantic_ai, decide_fast_pydantic_ai, summarize_context_pydantic_ai
from .schemas import (
    ConversationEndRequest,
    ConversationEndResponse,
    DeliberateAgentRequest,
    DeliberateAgentResponse,
    FastAgentRequest,
    FastAgentResponse,
)
from .stub import decide_deliberate_stub, decide_fast_stub


async def decide_fast(request: FastAgentRequest, config: AgentConfig) -> FastAgentResponse:
    context = AgentContextStore(config).for_fast(request)
    context.record_request("fast", request.model_dump())
    if config.mode == "pydantic_ai":
        response = await decide_fast_pydantic_ai(request, config)
    else:
        response = decide_fast_stub(request)
    context.record_response("fast", response.model_dump())
    return response


async def decide_deliberate(request: DeliberateAgentRequest, config: AgentConfig) -> DeliberateAgentResponse:
    context = AgentContextStore(config).for_deliberate(request)
    context.record_request("deliberate", request.model_dump())
    if config.mode == "pydantic_ai":
        response = await decide_deliberate_pydantic_ai(request, config, context)
    else:
        response = decide_deliberate_stub(request)
        context.apply_memory_updates(response.memory_updates)
    context.record_response("deliberate", response.model_dump())
    return response


async def end_conversation(request: ConversationEndRequest, config: AgentConfig) -> ConversationEndResponse:
    context = AgentContextStore(config).for_end(request)
    context.record_request("conversation_end", request.model_dump())
    summarizer = None
    if config.mode == "pydantic_ai":
        summarizer = lambda prompt, payload: summarize_context_pydantic_ai(prompt, payload, config)
    memory_updated = await context.end_conversation(request, summarizer)
    response = ConversationEndResponse(
        request_id=request.request_id,
        memory_updated=memory_updated,
    )
    context.record_response("conversation_end", response.model_dump())
    return response
