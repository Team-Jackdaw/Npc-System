# External Agent Interface

This document defines the JSON boundary between the Minecraft mod and an
external NPC agent. The transport is expected to be HTTP, but endpoint paths,
authentication, timeout, and retry policy are intentionally left outside this
version.

## Protocol Rules

- `version` / `v` is currently `1`.
- `request_id` / `rid` must be echoed by the response.
- `mode` is either `fast` or `deliberate`.
- Each response may request at most one action.
- Actions call the same registered functions exposed by `FunctionManager`.
- Use `kind: "task"` for functions that start Minecraft-only NPC tasks, and
  `kind: "tool"` for other functions.

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
  "tools": ["say", "look_at_player", "walk_to_player", "follow_player", "wait", "stop_task"],
  "limits": {
    "max_actions": 1,
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
  "a": "call",
  "kind": "task",
  "name": "say",
  "args": {
    "message": "你好。"
  },
  "note": "reply"
}
```

`a` is `none` or `call`. `note` is optional and should stay short.

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
    "max_actions": 1,
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
  "speech": "好，我跟着你。",
  "memory_updates": [],
  "reasoning_summary": "Player asked this NPC to follow."
}
```

`action.type` is `none` or `call`. `reasoning_summary` is a concise decision
summary, not a hidden chain-of-thought transcript.

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
