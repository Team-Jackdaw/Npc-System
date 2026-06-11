from __future__ import annotations

import uvicorn
from fastapi import Depends, FastAPI, Header, HTTPException

from .config import AgentConfig, load_config
from .decision import decide_deliberate, decide_fast
from .schemas import DeliberateAgentRequest, DeliberateAgentResponse, FastAgentRequest, FastAgentResponse

app = FastAPI(title="NPC External Agent", version="1")


def get_config() -> AgentConfig:
    return load_config()


def authorize(
    config: AgentConfig = Depends(get_config),
    authorization: str | None = Header(default=None),
) -> None:
    if not config.auth_token:
        return
    if authorization != f"Bearer {config.auth_token}":
        raise HTTPException(status_code=401, detail="Unauthorized")


@app.get("/health")
async def health(config: AgentConfig = Depends(get_config)) -> dict[str, str]:
    return {"status": "ok", "mode": config.mode, "model": config.model}


@app.post("/agent/fast", response_model=FastAgentResponse)
async def fast_endpoint(
    request: FastAgentRequest,
    config: AgentConfig = Depends(get_config),
    _: None = Depends(authorize),
) -> FastAgentResponse:
    return await decide_fast(request, config)


@app.post("/agent/deliberate", response_model=DeliberateAgentResponse)
async def deliberate_endpoint(
    request: DeliberateAgentRequest,
    config: AgentConfig = Depends(get_config),
    _: None = Depends(authorize),
) -> DeliberateAgentResponse:
    return await decide_deliberate(request, config)


def main() -> None:
    config = load_config()
    uvicorn.run("npc_agent.server:app", host=config.host, port=config.port, reload=False)


if __name__ == "__main__":
    main()
