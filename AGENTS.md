# Repository Guidelines

## Project Structure & Module Organization

This is a Java 17 Fabric mod for Minecraft 1.19.4. Main server-side code lives in `src/main/java/team/jackdaw/npcsystem`, with resources in `src/main/resources`. Client-only entrypoints and mixins are under `src/client/java` and `src/client/resources`. Tests are in `src/test/java/team/jackdaw/npcsystem`. Key packages include `ai` for agent/conversation logic, `entity` for NPC entities and tasks, `function` for LLM-callable tools, `group` for group state, `rag` for local memory storage, and `api` for Ollama HTTP bindings. Static assets are under `src/main/resources/assets/npc-system`.

## Build, Test, and Development Commands

- `./gradlew build`: compile, run tests, remap, and create mod jars in `build/libs`.
- `./gradlew compileJava`: compile main sources only.
- `./gradlew test`: run all JUnit 5 tests.
- `./gradlew test --tests team.jackdaw.npcsystem.rag.RAGTest`: run one test class.

If `./gradlew` is unavailable, use the Gradle version from `gradle/wrapper/gradle-wrapper.properties` and Java 17. Avoid running this project with very old system Gradle versions.

## Coding Style & Naming Conventions

Use Java 17, 4-space indentation, and package names under `team.jackdaw.npcsystem`. Classes use `PascalCase`, methods and fields use `camelCase`, and constants use `UPPER_SNAKE_CASE` where appropriate. Keep public APIs small and prefer existing package patterns over introducing new abstractions. Use Gson for JSON serialization, as existing config, group, and memory code already do. Keep comments brief and only add them when they clarify non-obvious behavior.

## Testing Guidelines

Tests use JUnit Jupiter. Name test classes after the unit under test, for example `RAGTest` or `SimpleChunkingTest`, and keep test methods behavior-focused. Prefer local, deterministic tests; tests that require Ollama, Minecraft runtime state, or external services should be clearly isolated and not mixed into simple unit tests. Run targeted tests before full `./gradlew test` when changing a single package.

## Commit & Pull Request Guidelines

Current history uses short messages such as `update: README.md` and `update: add Functions`. Follow the `type: summary` shape, but make the summary specific, for example `update: replace rag storage with local json`. Pull requests should include a concise description, changed behavior, test commands run, and any required configuration notes. For UI or in-game behavior changes, include screenshots or reproduction steps when possible.

## Security & Configuration Tips

Runtime config is stored under `config/npc-system`. Do not commit local config, generated memory files, build outputs, or secrets. Ollama endpoint and model settings are configured through `Config` / `config.json`; keep network-dependent behavior optional where possible.
