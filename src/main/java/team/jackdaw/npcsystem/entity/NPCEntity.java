package team.jackdaw.npcsystem.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.*;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.World;
import net.minecraft.world.poi.PointOfInterestType;
import net.minecraft.world.poi.PointOfInterestTypes;
import team.jackdaw.npcsystem.Config;
import team.jackdaw.npcsystem.ai.AgentManager;
import team.jackdaw.npcsystem.ai.npc.Action;
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
                NPCRegistration.MEMORY_NEAREST_NPC,
                NPCRegistration.MEMORY_NEAREST_VISIBLE_NPC,
                NPCRegistration.MEMORY_NEAREST_VISIBLE_TARGETABLE_NPC
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
                NPCRegistration.SENSOR_NEAREST_NPC

        );
        POINTS_OF_INTEREST = ImmutableMap.of(
                MemoryModuleType.HOME, (villager, registryEntry) -> registryEntry.matchesKey(PointOfInterestTypes.HOME),
                MemoryModuleType.JOB_SITE, (villager, registryEntry) -> villager.getVillagerData().getProfession().heldWorkstation().test(registryEntry),
                MemoryModuleType.POTENTIAL_JOB_SITE, (villager, registryEntry) -> VillagerProfession.IS_ACQUIRABLE_JOB_SITE.test(registryEntry),
                MemoryModuleType.MEETING_POINT, (villager, registryEntry) -> registryEntry.matchesKey(PointOfInterestTypes.MEETING)
        );
    }

    public NPCEntity(EntityType<? extends VillagerEntity> entityType, World world) {
        super(entityType, world);
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

    private static Activity scheduleMapping(Action action) {
        return null;
    }

    private Schedule getSchedule() {
        if (getOrRegisterNPC().getSchedule() == null) {
            return NPCRegistration.SCHEDULE_NPC_DEFAULT;
        }
        Schedule schedule = new Schedule();
        ScheduleBuilder builder = new ScheduleBuilder(schedule);
        getOrRegisterNPC().getSchedule().forEach((time, action) -> builder.withActivity(time, scheduleMapping(action)));
        return builder.build();

    }

    protected Brain<?> deserializeBrain(Dynamic<?> dynamic) {
        getOrRegisterNPC();
        Brain<VillagerEntity> brain = this.createBrainProfile().deserialize(dynamic);
        this.initBrain(brain);
        return brain;
    }

    protected Brain.Profile<VillagerEntity> createBrainProfile() {
        return Brain.createProfile(MEMORY_MODULES, SENSORS);
    }

    private void initBrain(Brain<VillagerEntity> brain) {
        brain.setSchedule(getSchedule());
        VillagerProfession npcProfession = this.getVillagerData().getProfession();
        brain.setTaskList(Activity.CORE, NPCTaskListProvider.createCoreTasks(npcProfession, 0.5F));
        brain.setTaskList(NPCRegistration.ACTIVITY_DONOTHING, NPCTaskListProvider.creatDoNoThingTasks(npcProfession, 0.5F));
        brain.setTaskList(Activity.WORK, NPCTaskListProvider.createWorkTasks(npcProfession, 0.5F), ImmutableSet.of(Pair.of(MemoryModuleType.JOB_SITE, MemoryModuleState.VALUE_PRESENT)));
        brain.setTaskList(Activity.REST, NPCTaskListProvider.createRestTasks(npcProfession, 0.5F));
        brain.setTaskList(Activity.IDLE, NPCTaskListProvider.createIdleTasks(npcProfession, 0.5F));
        brain.setTaskList(NPCRegistration.ACTIVITY_SOCIAL, NPCTaskListProvider.createSocialTasks(npcProfession, 0.5F));
        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(NPCRegistration.ACTIVITY_DONOTHING);
        brain.doExclusively(NPCRegistration.ACTIVITY_DONOTHING);
        brain.refreshActivities(this.world.getTimeOfDay(), this.world.getTime());
    }

    public void updateScheduleFromAgent() {
        Schedule npcSchedule = getSchedule();
        this.brain.setSchedule(npcSchedule);
        brain.doExclusively(NPCRegistration.ACTIVITY_DONOTHING);
        brain.refreshActivities(this.world.getTimeOfDay(), this.world.getTime());
    }

    public void sendMessage(String message, double range) {
        if (Config.isBubble) {
            if(this.textBubble.isRemoved()) {
                this.textBubble = new TextBubbleEntity(this);
            }
            textBubble.setTextBackgroundColor(Config.bubbleColor);
            textBubble.setTimeLastingPerChar(Config.timeLastingPerChar);
            textBubble.update(message);
        }
        if (Config.isChatBar) {
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
