# MC 26.1.2 Migration Report

## Summary

This migration updates the project from the old Fabric/Yarn-based Minecraft 1.19.4 setup to Minecraft 26.1.2 with Fabric Loader 0.19.3, Fabric API 0.151.0+26.1.2, Loom 1.17.7, Gradle 9.5.1, and Java 25. The main server and client source sets now compile against Mojang/official names instead of Yarn names.

## Build System Changes

- Updated `gradle.properties` to MC 26.1.2, Fabric Loader 0.19.3, Fabric API 0.151.0+26.1.2, and Loom 1.17.7.
- Updated `build.gradle` to use `net.fabricmc.fabric-loom` and Java 25.
- Removed Yarn mappings from dependencies because the current Loom setup uses official names.
- Added explicit JUnit Platform launcher runtime dependency required by Gradle 9 test execution.
- Added the Gradle wrapper files for Gradle 9.5.1.

## Source Migration

- Replaced Yarn classes such as `Text`, `Formatting`, `ServerCommandSource`, `PlayerEntity`, and `ServerWorld` with official equivalents such as `Component`, `ChatFormatting`, `CommandSourceStack`, `Player`, and `ServerLevel`.
- Updated command registration and command feedback to the 26.1 API.
- Updated entity/player methods including UUID, position, world, block position, sneaking, and chat message APIs.
- Updated registry code to use `BuiltInRegistries` and the newer `FabricEntityTypeBuilder.build(ResourceKey<?>)` API.
- Updated mixins for `PlayerList.broadcastChatMessage` and `PersistentEntitySectionManager.addNewEntity`.
- Updated client mixin target from `MinecraftClient` to `Minecraft`.

## Behavior Changes To Review

- Custom villager brain/task classes are currently placeholders. The old custom social/chat-follow tasks no longer compile against the 26.1 brain API and need a separate behavior-tree rebuild.
- `NPCEntity.updateScheduleFromAgent()` is currently disabled for the same reason.
- Command execution functions no longer receive an integer return value from Minecraft command execution. They now treat no exception as success.
- `TextBubbleEntity` uses reflection to call private `TextDisplay` setters because 26.1 exposes fewer public display mutation APIs. This should be runtime-tested in game.

## Verification

- `./gradlew compileJava --no-daemon`: passed.
- `./gradlew build -x test --no-daemon`: passed.
- RAG local tests passed.
- `NPC_API_TIMEOUT_MILLIS=180000 ./gradlew test --no-daemon`: passed with local Ollama `qwen3.5:latest`.

## Ollama Test Notes

- Test config now defaults to `http://localhost:11434` and `qwen3.5:latest`.
- Tests can be overridden with `NPC_TEST_API_URL`, `NPC_TEST_CHAT_MODEL`, or the matching `-Dnpc.test.*` system properties.
- HTTP timeout can be increased with `NPC_API_TIMEOUT_MILLIS`; `qwen3.5:latest` needs more than the default 30 seconds for some integration tests.
- `CompletionResponse` now reads Ollama's `thinking` field as a fallback when `response` is empty, matching `qwen3.5` structured-output behavior.
- The embedding API wrapper and embedding tests were removed because NPC memory now uses local JSON text storage and keyword scoring instead of vector embeddings.

## Follow-Up Items

- Rebuild custom NPC brain tasks against the 26.1 `Brain`, `Sensor`, and `BehaviorControl` APIs.
- Run a Minecraft dedicated/client startup test to verify mixin descriptors at runtime.
- Verify text bubbles in game, especially text rendering, billboard behavior, and see-through flags.
- Decide whether Ollama-dependent tests should be integration tests gated by an environment variable.
