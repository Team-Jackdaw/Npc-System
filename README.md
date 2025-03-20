## NPC System (Actively Development)

![test workflow](https://github.com/Team-Jackdaw/npc-system/actions/workflows/build.yml/badge.svg)

> The mod is still actively developing, please submit an issue if you find any problems. If you want to use the latest version,
> please compile the source code yourself, see [Build](#6-build).

## 1. Introduction

**NPC-System** is a brand-new mod based on [**Chat With NPC**](https://github.com/Team-Jackdaw/chat-with-NPC), in addition to supporting player-NPC chatting, the mod will support the exchange of views between NPCs and the control of NPC behavior via LLM. The LLM can control the NPC's Task Schedule and execute unexpected Tasks. In addition, players can investigate and modify the memories and behaviors of all NPCs through the LLM.

## 2. How to use

### Spawn NPC

1. **Spawn a new NPC** by using the command `/npc spawn`, then the NPC will be spawned at the player's location. You don't need to register the NPC, but you need to rename them by using item `name tag`.

### Chat with NPC

1. Any player can **talk to NPCs by shift+clicking** on them. Then the NPC will greet with players.

2. You can **reply to the NPC by the chat bar** near the NPC. The NPC will only listen to the player who is talking to him.

3. The NPC's speech can be seen by everyone (in the **chat bubble** above their head), or by players within a certain range
around NPC (in the **Chat bar**).

### Meeting Activity

1. In `Meeting Activity`, NPCs will search and try to **chat with a nearby NPC or Player**.
2. If an NPC is facing and talking to a player, the player can **reply in the chat bar**.

### Master Agent

1. The Master Agent has no entity in the game. The OPs can talk to him by using the command `/npc master <message>`.
2. The Master Agent can execute commands and control the NPCs' behavior (in dev). And it will also record the conversation by using RAG data storage.

## 3. Requirements

- Minecraft Server 1.19.4
- Fabric Loader 0.12.0 or higher
- Fabric API included

## 4. Commands

- `/npc` - View configuration status
- `/npc help` - View help information
- `/npc spawn` - Spawn a new NPC
- `/npc master <message>` - Send a message to the Master Agent
- `/npc saveAll` - Save all NPCs' data
- `/npc group <group_name> <command> <parameter>` - Manage the group of NPCs
- `/npc addGroup <group_name>` - Add a new group
- `/npc debug` - Debug command

## 5. To do list

- [X] NPCs chat with each other and communicate their opinion of events.
- [ ] NPCs can be controlled by LLM.
- [ ] NPCs have their opinion on every event in their `Group`.

## 6. Build

1. Clone the repository.
2. Run `./gradlew build` in the root directory of the repository.
3. The jar file will be generated in the `build/libs` directory.

## 7. Overall structure:

![Structure](NPC-System.png)