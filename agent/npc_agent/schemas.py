from __future__ import annotations

from typing import Any, Literal

from pydantic import BaseModel, ConfigDict, Field


class AgentLimits(BaseModel):
    model_config = ConfigDict(extra="allow")

    max_actions: int = 1
    max_reply_chars: int = 0


class FastNpc(BaseModel):
    model_config = ConfigDict(extra="allow")

    id: str = ""
    name: str = ""
    kind: Literal["npc", "master"] = "npc"
    permission: int = 1
    task: str = "idle"
    hp: float = 0.0
    pos: list[float] = Field(default_factory=list)
    dim: str = ""


class FastNear(BaseModel):
    model_config = ConfigDict(extra="allow")

    p: list[str] = Field(default_factory=list)
    n: list[str] = Field(default_factory=list)
    e: list[str] = Field(default_factory=list)


class FastAgentRequest(BaseModel):
    model_config = ConfigDict(extra="allow")

    v: int = 1
    rid: str
    mode: Literal["fast"] = "fast"
    npc: FastNpc
    evt: list[list[Any]] = Field(default_factory=list)
    near: FastNear = Field(default_factory=FastNear)
    tools: list[str] = Field(default_factory=list)
    limits: AgentLimits = Field(default_factory=AgentLimits)


class FastAgentResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    v: int = 1
    rid: str
    mode: Literal["fast"] = "fast"
    a: Literal["none", "call"] = "none"
    kind: Literal["tool", "task"] | None = None
    name: str | None = None
    args: dict[str, Any] = Field(default_factory=dict)
    actions: list["AgentAction"] = Field(default_factory=list)
    note: str | None = None


class AgentAction(BaseModel):
    model_config = ConfigDict(extra="forbid")

    type: Literal["none", "call"] = "none"
    kind: Literal["tool", "task"] | None = None
    name: str | None = None
    arguments: dict[str, Any] = Field(default_factory=dict)
    callback: bool | None = None
    label: str | None = None


class AgentToolDescriptor(BaseModel):
    model_config = ConfigDict(extra="allow")

    name: str
    kind: Literal["tool", "task"] = "tool"
    description: str = ""
    parameters: dict[str, Any] = Field(default_factory=dict)
    required: list[str] = Field(default_factory=list)


class DeliberateNpc(BaseModel):
    model_config = ConfigDict(extra="allow")

    uuid: str = ""
    name: str = ""
    kind: Literal["npc", "master"] = "npc"
    permission: int = 1
    instruction: str = ""
    status: dict[str, Any] = Field(default_factory=dict)


class DeliberateObservations(BaseModel):
    model_config = ConfigDict(extra="allow")

    summary: str = ""
    recent_events: list[dict[str, Any]] = Field(default_factory=list)
    important_events: list[dict[str, Any]] = Field(default_factory=list)


class DeliberateConversation(BaseModel):
    model_config = ConfigDict(extra="allow")

    speaker: str = ""
    message: str = ""
    history: list[dict[str, str]] = Field(default_factory=list)


class DeliberateMemory(BaseModel):
    model_config = ConfigDict(extra="allow")

    recent: list[str] = Field(default_factory=list)
    relevant: list[str] = Field(default_factory=list)


class DeliberateAgentRequest(BaseModel):
    model_config = ConfigDict(extra="allow")

    version: int = 1
    request_id: str
    mode: Literal["deliberate"] = "deliberate"
    npc: DeliberateNpc
    observations: DeliberateObservations = Field(default_factory=DeliberateObservations)
    conversation: DeliberateConversation = Field(default_factory=DeliberateConversation)
    memory: DeliberateMemory = Field(default_factory=DeliberateMemory)
    available_tools: list[AgentToolDescriptor] = Field(default_factory=list)
    limits: AgentLimits = Field(default_factory=AgentLimits)


class DeliberateAgentResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    version: int = 1
    request_id: str
    mode: Literal["deliberate"] = "deliberate"
    action: AgentAction = Field(default_factory=AgentAction)
    actions: list[AgentAction] = Field(default_factory=list)
    speech: str | None = None
    memory_updates: list[str] = Field(default_factory=list)
    reasoning_summary: str = ""


class ConversationEndNpc(BaseModel):
    model_config = ConfigDict(extra="allow")

    uuid: str = ""
    id: str = ""
    name: str = ""
    kind: Literal["npc", "master"] = "npc"
    permission: int = 1


class ConversationEndRequest(BaseModel):
    model_config = ConfigDict(extra="allow")

    version: int = 1
    request_id: str = ""
    npc: ConversationEndNpc
    reason: str = "ended"
    snapshot: dict[str, Any] = Field(default_factory=dict)


class ConversationEndResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    version: int = 1
    request_id: str = ""
    status: Literal["ok"] = "ok"
    memory_updated: bool = False
