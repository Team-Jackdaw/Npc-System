from __future__ import annotations

import json

from pydantic import ValidationError
from pydantic_ai import Agent
from pydantic_ai.models.ollama import OllamaModel
from pydantic_ai.providers.ollama import OllamaProvider

from .config import AgentConfig
from .schemas import (
    AgentAction,
    DeliberateAgentRequest,
    DeliberateAgentResponse,
    FastAgentRequest,
    FastAgentResponse,
)


def build_agent(config: AgentConfig, output_type):
    model = OllamaModel(
        config.model,
        provider=OllamaProvider(base_url=config.ollama_base_url),
    )
    return Agent(model=model, output_type=output_type, retries=1)


async def decide_fast_pydantic_ai(request: FastAgentRequest, config: AgentConfig) -> FastAgentResponse:
    agent = build_agent(config, FastAgentResponse)
    try:
        result = await agent.run(
            json.dumps(request.model_dump(), ensure_ascii=False),
            instructions=(
                "You control one Minecraft agent. Return only the structured output. "
                "Use at most two actions. Prefer say followed by one task when replying and acting. "
                "Only requests with npc.kind='master' and permission>=3 may call call_command."
            ),
        )
        return normalize_fast_response(result.output, request)
    except (ValidationError, Exception) as exc:
        return FastAgentResponse(rid=request.rid, a="none", note=f"fallback:{type(exc).__name__}")


async def decide_deliberate_pydantic_ai(
    request: DeliberateAgentRequest,
    config: AgentConfig,
) -> DeliberateAgentResponse:
    agent = build_agent(config, DeliberateAgentResponse)
    try:
        result = await agent.run(
            json.dumps(request.model_dump(), ensure_ascii=False),
            instructions=(
                "You control one Minecraft agent. Return the structured output only. "
                "Use at most two actions. Prefer say followed by one task when replying and acting. "
                "Only requests with npc.kind='master' and permission>=3 may call call_command. "
                "Do not reveal chain-of-thought; provide a concise reasoning_summary."
            ),
        )
        return normalize_deliberate_response(result.output, request)
    except (ValidationError, Exception) as exc:
        return DeliberateAgentResponse(
            request_id=request.request_id,
            action=AgentAction(type="none"),
            reasoning_summary=f"fallback:{type(exc).__name__}",
        )


def normalize_fast_response(response: FastAgentResponse, request: FastAgentRequest) -> FastAgentResponse:
    response.rid = request.rid
    response.v = 1
    response.mode = "fast"
    response.actions = response.actions[:2]
    if response.a == "none":
        response.kind = None
        response.name = None
        response.args = {}
    return response


def normalize_deliberate_response(
    response: DeliberateAgentResponse,
    request: DeliberateAgentRequest,
) -> DeliberateAgentResponse:
    response.request_id = request.request_id
    response.version = 1
    response.mode = "deliberate"
    response.actions = response.actions[:2]
    if response.action.type == "none":
        response.action.kind = None
        response.action.name = None
        response.action.arguments = {}
    return response
