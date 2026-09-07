package dev.entropy159.taczturrets.registry;

import dev.entropy159.taczturrets.TACZTurrets;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static dev.entropy159.taczturrets.TACZTurrets.MODID;

public class SoundRegistry {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> TURRET_PLACE = SOUNDS.register("turret_place", () -> SoundEvent.createVariableRangeEvent(TACZTurrets.id("turret_place")));
    public static final DeferredHolder<SoundEvent, SoundEvent> TURRET_PICKUP = SOUNDS.register("turret_pickup", () -> SoundEvent.createVariableRangeEvent(TACZTurrets.id("turret_pickup")));

    public static void init(IEventBus bus) {
        SOUNDS.register(bus);
    }
}
