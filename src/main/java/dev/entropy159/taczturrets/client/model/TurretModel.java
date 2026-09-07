package dev.entropy159.taczturrets.client.model;

import dev.entropy159.taczturrets.TACZTurrets;
import dev.entropy159.taczturrets.turret.TurretEntity;
import dev.entropy159.taczturrets.turret.state.TurretState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class TurretModel extends GeoModel<TurretEntity> {
    @Override
    public ResourceLocation getModelResource(TurretEntity turretEntity) {
        return TACZTurrets.id("geo/entity/turret.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TurretEntity animatable) {
        return TurretState.getState(animatable).getPath();
    }

    @Override
    public ResourceLocation getAnimationResource(TurretEntity turretEntity) {
        return TACZTurrets.id("animations/entity/turret.animation.json");
    }

    @Override
    public void setCustomAnimations(TurretEntity turret, long instanceId, AnimationState<TurretEntity> animationState) {
        var head = getAnimationProcessor().getBone("head");
        EntityModelData data = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        if (data == null) return;
        float yaw = -turret.getYHeadRot() + 180;
        if (head != null) {
            head.setRotX((data.headPitch() - turret.getRecoilDegrees(animationState.getPartialTick())) * Mth.DEG_TO_RAD);
            head.setRotY(yaw * Mth.DEG_TO_RAD);
        }
        var center = getAnimationProcessor().getBone("center");
        if (center != null) {
            center.setRotY(yaw * Mth.DEG_TO_RAD);
        }
    }
}
