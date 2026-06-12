import os

import pytest

from npc_agent.config import AgentConfig
from npc_agent.context_store import AgentContextStore
from npc_agent.pydantic_ai_runner import decide_deliberate_pydantic_ai
from npc_agent.schemas import DeliberateAgentRequest


@pytest.mark.asyncio
@pytest.mark.skipif(
    os.getenv("NPC_AGENT_RUN_MODEL_TESTS") != "1",
    reason="Set NPC_AGENT_RUN_MODEL_TESTS=1 to test the configured Pydantic AI model backend.",
)
async def test_pydantic_ai_ollama_backend_can_reply_as_master(tmp_path):
    config = AgentConfig(
        mode="pydantic_ai",
        model=os.getenv("NPC_AGENT_MODEL", "qwen3.5"),
        ollama_base_url=os.getenv("NPC_AGENT_OLLAMA_BASE_URL", "http://localhost:11434/v1"),
        state_dir=str(tmp_path),
    )
    request = DeliberateAgentRequest.model_validate(
        {
            "request_id": "model-1",
            "npc": {"uuid": "master", "name": "Master", "kind": "master", "permission": 3},
            "conversation": {"speaker": "Admin", "message": "你听得到吗，说句话"},
            "available_tools": [{"name": "master_reply", "kind": "tool"}],
            "limits": {"max_actions": 2, "max_reply_chars": 80},
        }
    )
    context = AgentContextStore(config).for_deliberate(request)

    response = await decide_deliberate_pydantic_ai(request, config, context)

    assert response.request_id == "model-1"
    assert response.speech or response.action.name == "master_reply" or response.actions
