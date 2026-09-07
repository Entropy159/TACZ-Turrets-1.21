package dev.entropy159.taczturrets.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.entropy159.taczturrets.client.model.TurretModel;
import dev.entropy159.taczturrets.turret.TurretEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class TurretRenderer extends GeoEntityRenderer<TurretEntity> {
    public TurretRenderer(EntityRendererProvider.Context context) {
        super(context, new TurretModel());
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull TurretEntity animatable) {
        return model.getTextureResource(animatable, this);
    }

    @Override
    protected void applyRotations(TurretEntity turret, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        if (turret == null) {
            return;
        }
        if (turret.deathTime > 0) {
            float deathRotation = (turret.deathTime + partialTick - 1.0F) / 20.0F * 1.6F;
            poseStack.mulPose(Axis.ZP.rotationDegrees(Math.min(Mth.sqrt(deathRotation), 1.0F) * getDeathMaxRotation(turret)));
        } else if (LivingEntityRenderer.isEntityUpsideDown(turret)) {
            poseStack.translate(0.0F, (turret.getBbHeight() + 0.1F) / nativeScale, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        }
    }

    @Override
    public void renderFinal(PoseStack poseStack, TurretEntity turret, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay, int color) {
        super.renderFinal(poseStack, turret, model, bufferSource, buffer, partialTick, packedLight, packedOverlay, color);

        poseStack.pushPose();

        model.getBone("gun").ifPresent(gun -> poseStack.translate(gun.getModelPosition().x / 16f, gun.getModelPosition().y / 16f, gun.getModelPosition().z / 16f));
        poseStack.mulPose(Axis.YN.rotationDegrees(turret.getYHeadRot() + 180));
        poseStack.mulPose(Axis.XN.rotationDegrees(turret.getXRot() - turret.getRecoilDegrees(partialTick)));
        poseStack.translate(0.0F, 0.0F, turret.getRecoilPush(partialTick));

        if (turret.hasGun()) {
            if (turret.hasMinigun()) {
                poseStack.mulPose(Axis.XN.rotationDegrees(90));
            }
            Minecraft.getInstance().getItemRenderer().renderStatic(turret.getGunStack(), ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, packedLight, packedOverlay, poseStack, bufferSource, turret.level(), 0);
        }

        poseStack.popPose();
    }
}
