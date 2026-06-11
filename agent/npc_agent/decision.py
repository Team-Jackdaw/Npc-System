from __future__ import annotations

from .config import AgentConfig
from .pydantic_ai_runner import decide_deliberate_pydantic_ai, decide_fast_pydantic_ai
from .schemas import DeliberateAgentRequest, DeliberateAgentResponse, FastAgentRequest, FastAgentResponse
from .stub import decide_deliberate_stub, decide_fast_stub


async def decide_fast(request: FastAgentRequest, config: AgentConfig) -> FastAgentResponse:
    if config.mode == "pydantic_ai":
        return await decide_fast_pydantic_ai(request, config)
    return decide_fast_stub(request)


async def decide_deliberate(request: DeliberateAgentRequest, config: AgentConfig) -> DeliberateAgentResponse:
    if config.mode == "pydantic_ai":
        return await decide_deliberate_pydantic_ai(request, config)
    return decide_deliberate_stub(request)
