package team.jackdaw.npcsystem.ai.master;

import team.jackdaw.npcsystem.ai.Agent;
import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.api.json.ChatResponse;
import team.jackdaw.npcsystem.api.json.Message;
import team.jackdaw.npcsystem.api.json.Tool;

import java.util.List;

public class MasterCW implements ConversationWindow {
    private List<Message> messages;
    private List<Tool> tools;
    private long updateTime = 0L;

    public MasterCW() {

    }

    @Override
    public Agent getAgent() {
        return Master.getMaster();
    }

    @Override
    public List<Message> getMessages() {
        return messages;
    }

    @Override
    public List<Tool> getTools() {
        return tools;
    }

    @Override
    public ChatResponse chat(String message) {
        return null;
    }

    @Override
    public long getUpdateTime() {
        return 0;
    }

    @Override
    public void discard() {

    }
}
