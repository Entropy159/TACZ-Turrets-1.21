package dev.entropy159.taczturrets.client.model;

import dev.entropy159.taczturrets.TACZTurrets;
import dev.entropy159.taczturrets.turret.TurretItem;
import dev.entropy159.taczturrets.turret.state.TurretState;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class TurretItemModel extends GeoModel<TurretItem> {
    @Override
    public ResourceLocation getModelResource(TurretItem turretItem) {
        return TACZTurrets.id("geo/entity/turret.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TurretItem turretItem) {
        return TurretState.NO_GUN.getPath();
    }

    @Override
    public ResourceLocation getAnimationResource(TurretItem turretItem) {
        return TACZTurrets.id("animations/entity/turret.animation.json");
    }

    @Override
    public void setCustomAnimations(TurretItem turretItem, long instanceId, AnimationState<TurretItem> animationState) {
        var head = getAnimationProcessor().getBone("head");
        if (head != null) {
            head.setRotX(0);
            head.setRotY(0);
        }
        var center = getAnimationProcessor().getBone("center");
        if (center != null) {
            center.setRotY(0);
        }
    }
}
