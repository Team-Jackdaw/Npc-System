package team.jackdaw.npcsystem.npcentity;

import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import team.jackdaw.npcsystem.npcentity.sensor.NearestNPCSensor;

import java.util.function.Supplier;

public class NPCSensorType {

    public static final SensorType<NearestNPCSensor> NEAREST_NPC = register("nearest_npc", NearestNPCSensor::new);

    private static <U extends Sensor<?>> SensorType<U> register(String id, Supplier<U> factory) {
        return (SensorType) Registry.register(Registries.SENSOR_TYPE, new Identifier(id), new SensorType(factory));
    }
}
