# External Agent Interface

Last updated: 2026-06-11 22:11:08 CST

Status: The protocol DTOs exist on the Java side, the Python FastAPI/Pydantic
agent scaffold exists under `agent/`, and Java can call the external agent when
`Config.agentEnabled` is enabled. This document remains the wire-contract
reference.

This document defines the JSON boundary between the Minecraft mod and an
external NPC agent. The transport is expected to be HTTP, but endpoint paths,
authentication, timeout, and retry policy are intentionally left outside this
version.

The Python agent can use different Pydantic AI model providers through
environment variables. Supported providers are `ollama`, `deepseek`, and
`openai-compatible`; see `agent/README.md` for exact configuration.

## Protocol Rules

- `version` / `v` is currently `1`.
- `request_id` / `rid` must be echoed by the response.
- `mode` is either `fast` or `deliberate`.
- Each response may request at most two actions through `actions`.
- Legacy single-action fields are still accepted for compatibility.
- Actions call the same registered functions exposed by `FunctionManager`.
- Use `kind: "task"` for functions that start Minecraft-only NPC tasks, and
  `kind: "tool"` for other functions.
- Request `npc.kind` is `npc` or `master`.
- Request `npc.permission` mirrors Java-side tool permission. Only
  `kind: "master"` with `permission >= 3` may call `call_command`.
- Java remains the Minecraft fact source and executor. The Python agent owns
  per-NPC conversation context when external agent mode is enabled.

## Fast Mode

Fast mode is for chat replies, nearby event reactions, and urgent interrupts.
It uses short keys and compact values to reduce token usage.

Request:

```json
{
  "v": 1,
  "rid": "request-uuid",
  "mode": "fast",
  "npc": {
    "id": "npc-uuid",
    "name": "npc-name",
    "kind": "npc",
    "permission": 1,
    "task": "idle",
    "hp": 20.0,
    "pos": [10, 64, -5],
    "dim": "minecraft:overworld"
  },
  "evt": [
    ["CHAT_HEARD", 7, "Steve: 你好", 12345]
  ],
  "near": {
    "p": ["Steve@3.2"],
    "n": ["Bob@5.1"],
    "e": ["Zombie@8.0"]
  },
  "tools": ["look_at_player", "walk_to_player", "follow_player", "wait", "stop_task"],
  "limits": {
    "max_actions": 2,
    "max_reply_chars": 60
  }
}
```

Response:

```json
{
  "v": 1,
  "rid": "request-uuid",
  "mode": "fast",
  "a": "none",
  "kind": null,
  "name": null,
  "args": {},
  "actions": [
    {
      "type": "call",
      "kind": "task",
      "name": "look_at_player",
      "arguments": {
        "player": "Steve",
        "seconds": 3
      },
      "callback": false,
      "label": "look_at_speaker"
    }
  ],
  "speech": "你好。",
  "note": "reply"
}
```

`speech` is the normal NPC/Master reply and does not count against
`max_actions`. `actions` is for real tools/tasks only. Java still accepts legacy
`say` or `master_reply` actions as a compatibility source for response text, but
does not execute them as tasks/tools. `note` is optional and should stay short.

## Deliberate Mode

Deliberate mode is for complex planning, memory use, and longer interaction.
It sends complete field names and richer context.

Request:

```json
{
  "version": 1,
  "request_id": "request-uuid",
  "mode": "deliberate",
  "npc": {
    "uuid": "npc-uuid",
    "name": "npc-name",
    "kind": "npc",
    "permission": 1,
    "instruction": "Minecraft NPC instruction",
    "status": {
      "task": "running follow_entity",
      "health": 20.0,
      "max_health": 20.0,
      "position": {"x": 10, "y": 64, "z": -5},
      "dimension": "minecraft:overworld",
      "biome": "minecraft:plains",
      "weather": "clear"
    }
  },
  "observations": {
    "summary": "full state summary",
    "recent_events": [],
    "important_events": []
  },
  "conversation": {
    "speaker": "Steve",
    "message": "你能跟着我吗？",
    "history": []
  },
  "memory": {
    "recent": [],
    "relevant": []
  },
  "available_tools": [
    {
      "name": "follow_player",
      "kind": "task",
      "description": "Follow a player.",
      "parameters": {},
      "required": ["player"]
    }
  ],
  "limits": {
    "max_actions": 2,
    "max_reply_chars": 200
  }
}
```

Response:

```json
{
  "version": 1,
  "request_id": "request-uuid",
  "mode": "deliberate",
  "action": {
    "type": "call",
    "kind": "task",
    "name": "follow_player",
    "arguments": {
      "player": "Steve",
      "seconds": 30
    }
  },
  "actions": [
    {
      "type": "call",
      "kind": "task",
      "name": "follow_player",
      "arguments": {
        "player": "Steve",
        "seconds": 30
      },
      "label": "follow_request"
    }
  ],
  "speech": "好，我跟着你。",
  "memory_updates": [],
  "reasoning_summary": "Player asked this NPC to follow."
}
```

`action.type` is `none` or `call`. `actions` is preferred and currently limited
to two real tool/task entries. Put normal text replies in `speech`; do not spend
an action on saying text. `reasoning_summary` is a concise decision summary, not
a hidden chain-of-thought transcript.

## Agent-Side Context

The Python agent stores context under `config/npc-system/agent-state` by
default. Override it with `NPC_AGENT_STATE_DIR`.

Each NPC/Master has its own directory:

```text
agent-state/
  npc/<uuid>/
  master/default/
```

Each directory contains:

- `AGENTS.md`: identity, behavior boundaries, and permission notes.
- `SOLU.md`: current plan and unresolved goals.
- `SUMMARY.md`: LLM-written natural-language summary for the current conversation.
- `MEMORY.md`: LLM-written long-term natural-language memory written when a conversation ends.
- `messages.json`: current Pydantic AI message history.
- `history.jsonl`: debug/audit log only.

When a directory is first created, these Markdown files are copied from
repository templates. Ordinary NPCs use `agent/templates/npc/AGENTS.md`, Master
uses `agent/templates/master/AGENTS.md`, and shared files come from
`agent/templates/common`. Existing runtime files are never overwritten.

Shared skills are reserved under `agent/skills`. The current `dummy_skill.md`
is only a placeholder and is not loaded into prompts yet.

Pydantic AI integration uses `message_history` when calling `Agent.run(...)`
and persists the resulting context with `result.all_messages_json()`. When
`messages.json` exceeds `NPC_AGENT_MAX_HISTORY_BYTES` (default `65536`), the
agent asks the configured model to summarize it into natural language in
`SUMMARY.md` and then resets the current message history. `SUMMARY.md` should
not contain raw JSON, message dumps, or debug logs.

`history.jsonl` is not used as prompt memory; it exists for debugging and
replay.

## Conversation End

Java notifies the agent when a conversation is explicitly ended or removed.

Endpoint:

```text
POST /agent/conversation/end
```

Request:

```json
{
  "version": 1,
  "request_id": "request-uuid",
  "npc": {
    "uuid": "npc-uuid",
    "id": "npc-uuid",
    "name": "npc-name",
    "kind": "npc",
    "permission": 1
  },
  "reason": "end_conversation_tool",
  "snapshot": {
    "task": "idle",
    "last_observation": "..."
  }
}
```

Response:

```json
{
  "version": 1,
  "request_id": "request-uuid",
  "status": "ok",
  "memory_updated": true
}
```

On conversation end, the agent asks the configured model to summarize
`messages.json` plus `SUMMARY.md` into natural-language long-term memory in
`MEMORY.md`, then resets the current conversation context. If summarization
fails, the agent records the failure in `history.jsonl`, keeps the current
context for retry, and does not write raw JSON into memory files.

## Task Completion Callback

When an AGENT task batch finishes, Java sends a follow-up request to the same
fast or deliberate endpoint. Fast mode appends a compact event:

```json
["TASK_BATCH_FINISHED", 8, "Agent task batch ... finished: follow_entity:finished", 12345]
```

Deliberate mode appends a structured `recent_events` entry with:

```json
{
  "type": "TASK_BATCH_FINISHED",
  "importance": 8,
  "text": "Agent task batch ... finished: ...",
  "game_time": 12345,
  "facts": {
    "batch_id": "batch-uuid",
    "status": "finished",
    "tasks": ["follow_entity:finished"]
  }
}
```

The NPC enters a short system wait while this callback is in flight. If the
agent returns new actions, they are queued as a new AGENT batch. If the agent is
done, it should call `resume_default_behavior`.

Available completion-control tool:

```json
{
  "type": "call",
  "kind": "tool",
  "name": "resume_default_behavior",
  "arguments": {}
}
```

## Master Agent

Master uses the same protocol as NPCs, but current Java routes Master chat
through `/agent/deliberate` so server-administrator monitoring and command
decisions can use the richer request. Master has a stable Java UUID and the
Python agent always stores its context under `master/default`, so Master memory
does not split across restarts or changing UUIDs. Master has no Minecraft entity
and cannot run task actions. Its request identity is:

```json
{
  "kind": "master",
  "permission": 3,
  "status": {
    "entity": "none",
    "task": "idle"
  }
}
```

Master may receive `call_command` in `available_tools`. The external agent
should use `speech` for normal Master conversation and only emit
`call_command(command)` when `npc.kind == "master"`, `npc.permission >= 3`, and
the administrator clearly requested a Minecraft command. Java also enforces this
permission through `FunctionManager`, so normal NPCs cannot execute administrator
commands even if a response tries to call the tool.

## Tool Result

All tools and task functions return the same shape:

```json
{
  "status": "success",
  "code": "task_started",
  "message": "Task started.",
  "data": {
    "task": "follow_entity",
    "target": "Steve"
  },
  "retryable": false
}
```

Failure:

```json
{
  "status": "failure",
  "code": "target_not_found",
  "message": "Player Steve was not found.",
  "data": {},
  "retryable": true
}
```

`status`, `code`, `message`, `data`, and `retryable` are always present.
