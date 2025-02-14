package team.jackdaw.npcsystem.ai;

import net.minecraft.entity.Entity;
import team.jackdaw.npcsystem.NPCSystem;
import team.jackdaw.npcsystem.SettingManager;
import team.jackdaw.npcsystem.npcentity.NPCEntity;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ConversationManager {
    public static final ConcurrentHashMap<UUID, ConversationWindow> conversationMap = new ConcurrentHashMap<>();
    public static final long outOfTime = NPCSystem.outOfTime;

    /**
     * Start a conversation for an NPC
     *
     * @param agent The Entity to start a conversation with
     */
    public static void startConversation(Agent agent) {
        if (isConversing(agent.getUUID())) return;
        ConversationWindow conversationWindow = agent.getConversationWindows();
        conversationMap.put(agent.getUUID(), conversationWindow);
    }

    /**
     * Check if an NPC is in a conversation
     *
     * @param npcUUID The NPC to check
     * @return True if the NPC is chatting, false otherwise
     */
    public static boolean isConversing(UUID npcUUID) {
        return conversationMap.containsKey(npcUUID);
    }

    /**
     * Get the conversation with a specific UUID
     *
     * @param uuid The UUID of the conversation
     * @return The conversation with the UUID
     */
    public static ConversationWindow getConversation(UUID uuid) {
        return conversationMap.get(uuid);
    }

    /**
     * Get the closest conversation around a entity
     *
     * @param entity The player to check
     * @return The closest conversation to the player
     */
    public static ConversationWindow getConversation(Entity entity) {
        List<ConversationWindow> conversations = getConversations(entity);
        if (conversations.isEmpty()) return null;
        return conversations.stream().sorted((conversation1, conversation2) -> {
            NPC npc1 = (NPC) conversation1.getAgent();
            NPC npc2 = (NPC) conversation2.getAgent();
            double distance1 = npc1.getEntity().distanceTo(entity);
            double distance2 = npc2.getEntity().distanceTo(entity);
            return Double.compare(distance1, distance2);
        }).toList().get(0);
    }

    /**
     * Check if there is a Conversation nearby
     *
     * @param entity The entity to check
     * @return True if there is a Conversation nearby, false otherwise
     */
    public static boolean isConversationNearby(Entity entity) {
        List<UUID> entities = getEntitiesInRange(entity, SettingManager.range).stream().map(Entity::getUuid).toList();
        return conversationMap.keySet().stream().anyMatch(entities::contains);
    }

    /**
     * Get all conversations within a certain range of an entity
     *
     * @param entity The entity to check
     * @return A list of Conversations within the range of the player
     */
    public static List<ConversationWindow> getConversations(Entity entity) {
        List<UUID> entities = getEntitiesInRange(entity, SettingManager.range).stream().map(Entity::getUuid).toList();
        return conversationMap.keySet().stream().filter(entities::contains).map(conversationMap::get).toList();
    }

    public static List<NPCEntity> getEntitiesInRange(Entity theEntity, double range) {
        return theEntity.world.getEntitiesByClass(NPCEntity.class, theEntity.getBoundingBox().expand(range), entity -> entity.getCustomName() != null);
    }

    /**
     * End a conversation with a specific UUID. This will also remove the NPCEntity the UUID represented from NPCEntityManager.
     * @param uuid The UUID of the conversation
     */
    public static void endConversation(UUID uuid) {
        conversationMap.get(uuid).discard();
        conversationMap.remove(uuid);
    }

    /**
     * End all conversations that are out of time
     */
    public static void endOutOfTimeConversations() {
        if (conversationMap.isEmpty()) return;
        conversationMap.forEach((uuid, ConversationWindow) -> {
            if (ConversationWindow.getUpdateTime() + outOfTime < System.currentTimeMillis()) {
                endConversation(uuid);
            }
        });
    }

    /**
     * End all conversations
     */
    public static void endAllConversations() {
        if (conversationMap.isEmpty()) return;
        conversationMap.forEach((uuid, ConversationWindow) -> endConversation(uuid));
    }
}
