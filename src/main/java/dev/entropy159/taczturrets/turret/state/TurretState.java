package dev.entropy159.taczturrets.turret.state;

import dev.entropy159.taczturrets.TACZTurrets;
import dev.entropy159.taczturrets.turret.TurretEntity;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;

public enum TurretState {
    ACTIVE("Active"), RELOADING("Reloading"), NO_AMMO("No Ammo"), NO_GUN("No Gun"), DISABLED("Disabled");

    public final String name;
    private final String texture;

    TurretState(String name) {
        this.name = name;
        texture = name.toLowerCase().replace(" ", "_");
    }

    public void setState(TurretEntity turret) {
        turret.getEntityData().set(TurretEntity.STATE, name);
    }

    public ResourceLocation getPath() {
        return TACZTurrets.id("textures/entity/turret_" + texture + ".png");
    }

    public static TurretState getState(TurretEntity turret) {
        for (TurretState state : values()) {
            if (turret.getEntityData().get(TurretEntity.STATE).equals(state.name)) return state;
        }
        return NO_GUN;
    }
}
