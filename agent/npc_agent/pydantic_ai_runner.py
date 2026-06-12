from __future__ import annotations

import json

from pydantic import ValidationError
from pydantic_ai import Agent
from pydantic_ai.models.openai import OpenAIChatModel
from pydantic_ai.models.ollama import OllamaModel
from pydantic_ai.providers.deepseek import DeepSeekProvider
from pydantic_ai.providers.openai import OpenAIProvider
from pydantic_ai.providers.ollama import OllamaProvider

from .config import AgentConfig
from .context_store import AgentContext
from .schemas import (
    AgentAction,
    DeliberateAgentRequest,
    DeliberateAgentResponse,
    FastAgentRequest,
    FastAgentResponse,
)


def build_agent(config: AgentConfig, output_type):
    model = build_model(config)
    return Agent(model=model, output_type=output_type, retries=1)


def build_model(config: AgentConfig):
    if config.provider == "ollama":
        return OllamaModel(
            config.model,
            provider=OllamaProvider(base_url=config.base_url),
        )
    if config.provider == "deepseek":
        return OpenAIChatModel(
            config.model,
            provider=DeepSeekProvider(api_key=config.api_key or None),
        )
    if config.provider in {"openai", "openai-compatible"}:
        return OpenAIChatModel(
            config.model,
            provider=OpenAIProvider(base_url=config.base_url or None, api_key=config.api_key or None),
        )
    raise ValueError(f"Unsupported NPC_AGENT_PROVIDER: {config.provider}")


async def decide_fast_pydantic_ai(request: FastAgentRequest, config: AgentConfig) -> FastAgentResponse:
    agent = build_agent(config, FastAgentResponse)
    try:
        result = await agent.run(
            json.dumps(request.model_dump(), ensure_ascii=False),
            instructions=(
                "You control one Minecraft agent. Return only the structured output. "
                "Use at most two actions. Prefer say followed by one task when replying and acting. "
                "Use exact argument names from available tool parameters and required fields. "
                "For normal Master conversation, call master_reply with a concise message. "
                "Only requests with npc.kind='master' and permission>=3 may call call_command."
            ),
        )
        return normalize_fast_response(result.output, request)
    except ValidationError as exc:
        return FastAgentResponse(rid=request.rid, a="none", note=f"fallback:{type(exc).__name__}")
    except Exception as exc:
        raise RuntimeError(f"pydantic_ai_fast_failed:{type(exc).__name__}") from exc


async def decide_deliberate_pydantic_ai(
    request: DeliberateAgentRequest,
    config: AgentConfig,
    context: AgentContext,
) -> DeliberateAgentResponse:
    agent = build_agent(config, DeliberateAgentResponse)
    try:
        result = await agent.run(
            json.dumps(request.model_dump(), ensure_ascii=False),
            message_history=context.message_history(),
            conversation_id=context.agent_id,
            instructions=(
                context.render_context_instructions()
                + "\n\n"
                "You control one Minecraft agent. Return the structured output only. "
                "Use at most two actions. Prefer say followed by one task when replying and acting. "
                "Use exact argument names from available tool parameters and required fields. "
                "For normal Master conversation, call master_reply with a concise message. "
                "Only requests with npc.kind='master' and permission>=3 may call call_command. "
                "Do not reveal chain-of-thought; provide a concise reasoning_summary."
            ),
        )
        response = normalize_deliberate_response(result.output, request)
        context.save_messages(result.all_messages_json())
        context.apply_memory_updates(response.memory_updates)
        return response
    except ValidationError as exc:
        return DeliberateAgentResponse(
            request_id=request.request_id,
            action=AgentAction(type="none"),
            reasoning_summary=f"fallback:{type(exc).__name__}",
        )
    except Exception as exc:
        raise RuntimeError(f"pydantic_ai_deliberate_failed:{type(exc).__name__}") from exc


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
