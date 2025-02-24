package team.jackdaw.npcsystem.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.Schedule;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.village.VillagerProfession;
import net.minecraft.village.VillagerType;
import net.minecraft.world.World;
import net.minecraft.world.poi.PointOfInterestType;
import net.minecraft.world.poi.PointOfInterestTypes;
import team.jackdaw.npcsystem.SettingManager;
import team.jackdaw.npcsystem.ai.AgentManager;
import team.jackdaw.npcsystem.ai.npc.NPC;

import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

public class NPCEntity extends VillagerEntity {
    public static final Map<MemoryModuleType<GlobalPos>, BiPredicate<NPCEntity, RegistryEntry<PointOfInterestType>>> POINTS_OF_INTEREST;
    private static final ImmutableList<MemoryModuleType<?>> MEMORY_MODULES;
    private static final ImmutableList<SensorType<? extends Sensor<? super VillagerEntity>>> SENSORS;
    protected long updateTime;
    protected TextBubbleEntity textBubble;

    static {
        MEMORY_MODULES = ImmutableList.of(
                MemoryModuleType.HOME,
                MemoryModuleType.JOB_SITE,
                MemoryModuleType.POTENTIAL_JOB_SITE,
                MemoryModuleType.MEETING_POINT,
                MemoryModuleType.MOBS,
                MemoryModuleType.VISIBLE_MOBS,
                MemoryModuleType.VISIBLE_VILLAGER_BABIES,
                MemoryModuleType.NEAREST_PLAYERS,
                MemoryModuleType.NEAREST_VISIBLE_PLAYER,
                MemoryModuleType.NEAREST_VISIBLE_TARGETABLE_PLAYER,
                MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM,
                MemoryModuleType.ITEM_PICKUP_COOLDOWN_TICKS,
                MemoryModuleType.WALK_TARGET,
                MemoryModuleType.LOOK_TARGET,
                MemoryModuleType.INTERACTION_TARGET,
                MemoryModuleType.BREED_TARGET,
                MemoryModuleType.PATH,
                MemoryModuleType.DOORS_TO_CLOSE,
                MemoryModuleType.NEAREST_BED,
                MemoryModuleType.HURT_BY,
                MemoryModuleType.HURT_BY_ENTITY,
                MemoryModuleType.NEAREST_HOSTILE,
                MemoryModuleType.SECONDARY_JOB_SITE,
                MemoryModuleType.HIDING_PLACE,
                MemoryModuleType.HEARD_BELL_TIME,
                MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
                MemoryModuleType.LAST_SLEPT,
                MemoryModuleType.LAST_WOKEN,
                MemoryModuleType.LAST_WORKED_AT_POI,
                MemoryModuleType.GOLEM_DETECTED_RECENTLY,
                NPCMemoryModuleType.NEAREST_NPC,
                NPCMemoryModuleType.NEAREST_VISIBLE_NPC,
                NPCMemoryModuleType.NEAREST_VISIBLE_TARGETABLE_NPC
        );
        SENSORS = ImmutableList.of(
                SensorType.NEAREST_LIVING_ENTITIES,
                SensorType.NEAREST_PLAYERS,
                SensorType.NEAREST_ITEMS,
                SensorType.NEAREST_BED,
                SensorType.HURT_BY,
                SensorType.VILLAGER_HOSTILES,
                SensorType.VILLAGER_BABIES,
                SensorType.SECONDARY_POIS,
                SensorType.GOLEM_DETECTED,
                NPCSensorType.NEAREST_NPC

        );
        POINTS_OF_INTEREST = ImmutableMap.of(
                MemoryModuleType.HOME, (villager, registryEntry) -> registryEntry.matchesKey(PointOfInterestTypes.HOME),
                MemoryModuleType.JOB_SITE, (villager, registryEntry) -> villager.getVillagerData().getProfession().heldWorkstation().test(registryEntry),
                MemoryModuleType.POTENTIAL_JOB_SITE, (villager, registryEntry) -> VillagerProfession.IS_ACQUIRABLE_JOB_SITE.test(registryEntry),
                MemoryModuleType.MEETING_POINT, (villager, registryEntry) -> registryEntry.matchesKey(PointOfInterestTypes.MEETING)
        );
    }

    public NPCEntity(EntityType<? extends NPCEntity> entityType, World world) {
        super(entityType, world);
        getOrRegisterNPC();
    }

    public NPCEntity(EntityType<? extends NPCEntity> entityType, World world, VillagerType type) {
        super(entityType, world, type);
        getOrRegisterNPC();
    }

    private NPC getOrRegisterNPC() {
        NPC ai = (NPC) AgentManager.getInstance().get(uuid);
        if (ai == null) {
            ai = new NPC(this);
            AgentManager.getInstance().register(ai);
        }
        if (!ai.getEntity().equals(this)) {
            ai.setEntity(this);
        }
        return ai;
    }

    protected Brain<?> deserializeBrain(Dynamic<?> dynamic) {
        Brain<VillagerEntity> brain = this.createBrainProfile().deserialize(dynamic);
        Schedule npcSchedule;
        if (getOrRegisterNPC().getSchedule() != null) {
            npcSchedule = NPCSchedule.get(getOrRegisterNPC());
        } else {
            npcSchedule = NPCSchedule.NPC_DEFAULT;
        }
        this.initBrain(brain, npcSchedule);
        return brain;
    }

    protected Brain.Profile<VillagerEntity> createBrainProfile() {
        return Brain.createProfile(MEMORY_MODULES, SENSORS);
    }

    private void initBrain(Brain<VillagerEntity> brain, Schedule npcSchedule) {
        brain.setSchedule(npcSchedule);
        VillagerProfession npcProfession = this.getVillagerData().getProfession();
        brain.setTaskList(NPCActivity.CORE, NPCTaskListProvider.createCoreTasks(npcProfession, 0.5F));
        brain.setTaskList(NPCActivity.DONOTHING, NPCTaskListProvider.creatDoNoThingTasks(npcProfession, 0.5F));
        brain.setTaskList(NPCActivity.WORK, NPCTaskListProvider.createWorkTasks(npcProfession, 0.5F), ImmutableSet.of(Pair.of(MemoryModuleType.JOB_SITE, MemoryModuleState.VALUE_PRESENT)));
        brain.setTaskList(NPCActivity.REST, NPCTaskListProvider.createRestTasks(npcProfession, 0.5F));
        brain.setTaskList(NPCActivity.IDLE, NPCTaskListProvider.createIdleTasks(npcProfession, 0.5F));
        brain.setTaskList(NPCActivity.SOCIAL, NPCTaskListProvider.createSocialTasks(npcProfession, 0.5F));
        brain.setCoreActivities(ImmutableSet.of(NPCActivity.CORE));
        brain.setDefaultActivity(NPCActivity.DONOTHING);
        brain.doExclusively(NPCActivity.DONOTHING);
        brain.refreshActivities(this.world.getTimeOfDay(), this.world.getTime());
    }

    public void updateScheduleFromAgent() {
        Schedule npcSchedule = NPCSchedule.get(getOrRegisterNPC());
        this.brain.setSchedule(npcSchedule);
        brain.doExclusively(NPCActivity.DONOTHING);
        brain.refreshActivities(this.world.getTimeOfDay(), this.world.getTime());
    }

    public void sendMessage(String message, double range) {
        if (SettingManager.isBubble) {
            if(this.textBubble.isRemoved()) {
                this.textBubble = new TextBubbleEntity(this);
            }
            textBubble.setTextBackgroundColor(SettingManager.bubbleColor);
            textBubble.setTimeLastingPerChar(SettingManager.timeLastingPerChar);
            textBubble.update(message);
        }
        if (SettingManager.isChatBar) {
            String npcName = this.getCustomName() != null ? this.getCustomName().toString() : "SomeOne";
            findNearbyPlayers(range).forEach(player -> player.sendMessage(Text.of("<" + npcName + "> " + message)));
        }
        this.updateTime = System.currentTimeMillis();
    }

    public List<PlayerEntity> findNearbyPlayers(double range) {
        return this.world.getEntitiesByClass(PlayerEntity.class, this.getBoundingBox().expand(range), player -> true);
    }

    public List<NPCEntity> findNearbyNPC(double range) {
        return this.world
                .getEntitiesByClass(VillagerEntity.class, this.getBoundingBox().expand(range), villager -> true)
                .stream()
                .filter(villagerEntity -> villagerEntity instanceof NPCEntity)
                .map(npc -> (NPCEntity) npc)
                .toList();
    }


}
