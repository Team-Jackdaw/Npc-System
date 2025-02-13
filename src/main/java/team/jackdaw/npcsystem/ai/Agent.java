package team.jackdaw.npcsystem.ai;

import team.jackdaw.npcsystem.ai.agent.Action;
import team.jackdaw.npcsystem.ai.agent.ConversationWindows;
import team.jackdaw.npcsystem.ai.agent.Status;

public interface Agent {

    /**
     * Input the latest thing the intelligence has observed (in natural language). And store it in Observation Storage.
     * @param time the time of the observation
     * @param observation the observation
     */
    void observe(long time, String observation);

    /**
     * Get the next action to take.
     * @return the next action to take
     */
    Action nextAction();

    /**
     * Get the current status of the agent.
     * @return the current status of the agent
     */
    Status getStatus();

    /**
     * Open the conversation windows.
     * @return the conversation windows
     */
    ConversationWindows getConversationWindows();
}
