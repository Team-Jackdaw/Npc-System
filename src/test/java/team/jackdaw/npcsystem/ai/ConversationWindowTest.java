package team.jackdaw.npcsystem.ai;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import team.jackdaw.npcsystem.Config;
import team.jackdaw.npcsystem.ai.npc.NPC;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversationWindowTest {
    private static final Set<String> OPENING_LINES = Set.of(
            "你好，有什么需要我帮忙的吗？",
            "你好，想聊些什么？",
            "我在这儿，需要我做什么？"
    );

    @AfterEach
    void resetConfig() {
        Config.agentEnabled = false;
    }

    @Test
    void openingChatUsesFixedLocalLine() {
        ConversationWindow window = new ConversationWindow(UUID.randomUUID());

        String reply = window.chat();

        assertTrue(OPENING_LINES.contains(reply));
        assertEquals(reply, window.getLastAssistantMessage());
        assertEquals("", window.getLastUserMessage());
    }

    @Test
    void disabledAgentReturnsFixedUnavailableReplyAndRecordsLastMessages() {
        Config.agentEnabled = false;
        NPC npc = new NPC(UUID.randomUUID());
        AgentManager.getInstance().register(npc);
        ConversationWindow window = new ConversationWindow(npc.getUUID());

        String reply = window.chat("你好");

        assertEquals("我现在有点走神，稍后再说。", reply);
        assertEquals("你好", window.getLastUserMessage());
        assertEquals(reply, window.getLastAssistantMessage());
        assertEquals("", window.getLastAgentResultSummary());
    }
}
