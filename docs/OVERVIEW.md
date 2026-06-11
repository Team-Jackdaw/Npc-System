# NPC System Overview

Last updated: 2026-06-11 22:11:08 CST

This document is the rolling architecture and implementation overview for the
project. Future feature work should update this file in place.

## Current Architecture

```text
Minecraft NPC Entity
  -> Sensor State
  -> Observation Events
  -> NPC Context Buffers
  -> External Agent Request
  -> Agent-side Conversation Context
  -> Tool / Task Action
  -> Priority Task Queue
  -> NpcTaskController
  -> Default Behavior
```

The project is a Java 25 Fabric mod for Minecraft 26.1.2. Minecraft-side code
lives under `src/main/java/team/jackdaw/npcsystem`. The optional Python external
agent scaffold lives under `agent/`.

Key layers:

- **Entity layer**: `NPCEntity` owns sensor state, task controller, chat display,
  and the bridge to the NPC agent object.
- **Sensor layer**: `NpcSensorState` records passive world state every tick
  window, including nearby players/NPCs/entities, weather, biome, health,
  position, task state, and recent chat.
- **Observe layer**: `ObservationCollector` compares sensor snapshots and emits
  structured `ObservationEvent` records.
- **AI/context layer**: `NPC` stores recent and important observation events and
  exposes context to conversations and external agent requests.
- **External agent layer**: Java DTOs define fast/deliberate JSON protocols;
  Python FastAPI + Pydantic AI scaffold receives those requests and manages
  per-NPC conversation context.
- **Tool layer**: `FunctionManager` exposes callable tools and tool descriptors.
  Tool results use a stable `status/code/message/data/retryable` shape.
- **Task layer**: `NpcTaskController` runs one low-level Minecraft task at a time,
  with source priority and a FIFO queue for interrupted or waiting tasks.
- **Default behavior layer**: idle NPCs can look around, stroll, or wait without
  calling the external agent.

## Implemented

- MC 26.1.2 / Fabric migration with Java 25 and Gradle 9.5.1.
- Local JSON/text memory storage replacing vector embedding storage.
- Basic NPC sensor state and observation-event buffering.
- Event types for players, NPCs, chat, weather, health, and task state changes.
- Project-owned `NpcTask` and `NpcTaskController`.
- Task sources and priorities:
  - `PLAYER`
  - `AGENT`
  - `SYSTEM`
  - `DEFAULT`
- FIFO task queue for player/agent/system tasks, with default behavior kept out
  of the persistent queue.
- Vanilla villager Brain behavior is suppressed for custom NPC entities so the
  custom task controller owns navigation and look behavior.
- Basic task implementations:
  - look at entity
  - walk to entity
  - follow entity
  - speak
  - wait
  - idle look around
  - random stroll
- Tool wrappers for NPC actions:
  - `say`
  - `look_at_player`
  - `look_at_npc`
  - `walk_to_player`
  - `walk_to_npc`
  - `follow_player`
  - `wait`
  - `stop_task`
  - `resume_default_behavior`
- External agent protocol:
  - compact `fast` mode
  - richer `deliberate` mode
  - up to two actions per response
  - AGENT task batch completion callback
- Python external agent scaffold:
  - FastAPI server
  - Pydantic schemas
  - per-NPC/Master context store under `config/npc-system/agent-state`
  - template-based first context creation from `agent/templates`
  - Pydantic AI `message_history` persistence through `messages.json`
  - current conversation compression into `SUMMARY.md`
  - conversation-end long-term memory writing into `MEMORY.md`
  - `agent/skills/dummy_skill.md` as a placeholder for shared skills
  - deterministic stub decision path
  - optional Pydantic AI + Ollama path
- Java external agent integration:
  - `ExternalAgentClient`
  - `AgentRequestBuilder`
  - `AgentActionExecutor`
  - configurable NPC conversation routing to external agent with Ollama fallback.
- Master external agent integration:
  - same fast/deliberate endpoints as NPCs
  - `kind=master`, `permission=3`
  - high-permission `call_command` guarded by Java and agent-side identity.

## Waiting To Implement

- Real in-game validation for MC 26.1.2 runtime behavior:
  - mixins
  - text bubbles
  - task movement and look control
  - external agent HTTP loop
  - vanilla Brain suppression effects
- More sensors:
  - inventory
  - equipment
  - blocks of interest
  - light level
  - hostile threat details
  - item entities
  - status effects
- More observe events:
  - inventory changed
  - item seen/picked up
  - block/workstation discovered
  - danger escalation
  - path failure details
- More task/tool capabilities:
  - sleep / wake
  - drop item
  - give item
  - pick up item
  - move to block/coordinate
  - use block
  - use held item
  - lead player to target
  - inspect inventory
- Agent loop improvements:
  - deliberate mode trigger policy
  - tool execution result reporting back to the Python agent
  - NPC polling mailbox/outbox so agent can queue actions without waiting for a
    Java-initiated request
- Python agent improvements:
  - stronger prompts
  - integration tests against local Ollama `qwen3.5`
  - optional auth test coverage
  - model output repair/fallback strategy

## Current Defaults

- External agent is disabled by default: `Config.agentEnabled = false`.
- Default agent endpoint: `http://127.0.0.1:8765`.
- Default Java agent mode: `fast`.
- Default Python agent mode: `stub`.
- Java can fall back to Ollama when external agent calls fail.

## Documentation Map

- `docs/AGENT_INTERFACE.md`: external agent JSON protocol.
- `docs/FOUNDATION_PLAN.md`: foundation design notes for sensor/observe/tool/task.
- `docs/MIGRATION_REPORT.md`: MC 26.1.2 migration report.
- `docs/NPC_RUNTIME_CAPABILITIES.md`: current in-game NPC runtime capabilities.
- `agent/README.md`: Python external agent scaffold usage.
