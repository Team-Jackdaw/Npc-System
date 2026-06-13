# MC 26.1.2 迁移报告

最后更新：2026-06-13 CST

状态：历史迁移报告。当前架构和实现状态见 `OVERVIEW.md`。

## 摘要

本次迁移将项目从旧的 Minecraft 1.19.4 Fabric/Yarn 环境升级到 Minecraft 26.1.2。当前使用 Fabric Loader 0.19.3、Fabric API 0.151.0+26.1.2、Loom 1.17.7、Gradle 9.5.1 和 Java 25。主服务端与客户端 source set 现在基于 Mojang/official names 编译，不再依赖 Yarn 命名。

## 构建系统变化

- `gradle.properties` 已更新到 MC 26.1.2、Fabric Loader 0.19.3、Fabric API 0.151.0+26.1.2 和 Loom 1.17.7。
- `build.gradle` 已切换到 `net.fabricmc.fabric-loom` 并使用 Java 25。
- 移除 Yarn mappings 依赖，因为当前 Loom 配置使用 official names。
- 添加 Gradle 9 测试运行所需的 JUnit Platform launcher runtime dependency。
- 添加 Gradle 9.5.1 wrapper 文件。

## 源码迁移

- 将 Yarn 类名替换为 official names，例如：
  - `Text` -> `Component`
  - `Formatting` -> `ChatFormatting`
  - `ServerCommandSource` -> `CommandSourceStack`
  - `PlayerEntity` -> `Player`
  - `ServerWorld` -> `ServerLevel`
- 命令注册和命令反馈已适配 26.1 API。
- 实体和玩家相关方法已适配新的 UUID、位置、世界、方块坐标、潜行和聊天消息 API。
- 注册表代码已改用 `BuiltInRegistries` 和新的 `FabricEntityTypeBuilder.build(ResourceKey<?>)` API。
- mixin 已更新到 `PlayerList.broadcastChatMessage` 和 `PersistentEntitySectionManager.addNewEntity`。
- 客户端 mixin target 已从 `MinecraftClient` 更新为 `Minecraft`。

## 行为变化与注意事项

- 旧自定义 villager brain/task 类目前仍是占位。旧社交/聊天跟随 task 不再适配 26.1 Brain API，需要后续重新设计行为树。
- `NPCEntity.updateScheduleFromAgent()` 仍处于禁用状态。
- Minecraft 命令执行函数不再依赖整数返回值；当前以未抛异常视为成功。
- `TextBubbleEntity` 使用反射调用私有 `TextDisplay` setter，因为 26.1 暴露的 public display mutation API 更少。
- 文本气泡已设置 billboard 朝向约束，实机仍应继续验证不同视角、遮挡和刷新表现。
- Java 端 LLM、RAG、embedding、本地长期记忆都已移除；持久上下文由 Python 外部 agent 管理。
- NPC 普通回复现在通过 agent response 的 `speech` 字段发送，不再推荐使用 `say` task。

## 验证记录

- `./gradlew compileJava --no-daemon`：通过。
- `./gradlew test --no-daemon`：通过。
- `./gradlew build --no-daemon`：通过。
- Python agent 单元测试：`PYTHONPATH=agent pytest agent/tests` 通过。
- 实机测试已验证 NPC spawn、`/npc debug`、基础对话、外部 agent 通信和部分任务闭环。

## 外部 Agent 与模型说明

- 当前 Java 不再直接调用 Ollama 或其他模型。
- Python agent 可通过 Pydantic AI 使用 `ollama`、`deepseek` 或 `openai-compatible`。
- 默认配置仍偏向本机 Ollama：`NPC_AGENT_PROVIDER=ollama`、`NPC_AGENT_MODEL=qwen3.5`。
- 使用 DeepSeek 官方 API 时配置：

```bash
NPC_AGENT_PROVIDER=deepseek \
NPC_AGENT_MODEL=deepseek-chat \
NPC_AGENT_API_KEY=sk-... \
PYTHONPATH=agent python3 -m npc_agent.server
```

## 后续事项

- 继续实机验证 mixin 描述符和专用服务器/客户端启动流程。
- 继续验证文本气泡在不同距离、角度、遮挡和资源包环境下的表现。
- 基于当前自定义 `NpcTaskController` 继续扩展行为，不再优先恢复旧 villager Brain task。
- 为外部 agent 增加更强的模型输出修复、错误恢复和集成测试。
- 将需要真实模型的测试保持为环境变量 gated integration test，避免影响普通 CI。
