package dev.entropy159.taczturrets.event;

import dev.entropy159.taczturrets.config.ServerConfig;
import dev.entropy159.taczturrets.turret.TurretEntity;
import dev.entropy159.taczturrets.turret.state.RetaliateTargeting;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import javax.annotation.Nullable;
import java.util.List;

@EventBusSubscriber
public class TurretEventHandler {
    @SubscribeEvent
    public static void onLivingAttack(LivingIncomingDamageEvent event) {
        TurretEntity turret = getAttackingTurret(event.getSource());
        if (turret == null) return;
        if (turret.isProtectedFromFire(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) return;

        TurretEntity turret = getAttackingTurret(event.getSource());
        if (turret != null) {
            if (ServerConfig.CREDIT_KILLS_TO_OWNER.get()) {
                Player owner = turret.getOwnerPlayer();
                if (owner != null && owner != victim) victim.setLastHurtByPlayer(owner);
            }
            return;
        }

        if (ServerConfig.PROTECT_OWNER.get() && victim instanceof Player owner && event.getSource().getEntity() instanceof LivingEntity attacker) {
            defendOwner(owner, attacker);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (ServerConfig.RETALIATE_TARGETING.get() != RetaliateTargeting.CLEAR_ON_DEATH) return;
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) return;

        double range = Math.max(ServerConfig.TURRET_RANGE.get(), ServerConfig.SNIPER_TURRET_RANGE.get());
        for (TurretEntity turret : player.level().getEntitiesOfClass(TurretEntity.class, AABB.ofSize(player.position(), range * 2, range * 2, range * 2))) {
            turret.forgetRetaliation(player.getUUID());
        }
    }

    @Nullable
    private static TurretEntity getAttackingTurret(DamageSource source) {
        if (source.getEntity() instanceof TurretEntity turret) return turret;
        if (source.getDirectEntity() instanceof TurretEntity turret) return turret;
        return null;
    }

    private static void defendOwner(Player owner, LivingEntity attacker) {
        if (attacker == owner) return;
        double range = Math.max(ServerConfig.TURRET_RANGE.get(), ServerConfig.SNIPER_TURRET_RANGE.get());
        List<TurretEntity> turrets = owner.level().getEntitiesOfClass(TurretEntity.class, AABB.ofSize(owner.position(), range * 2, range, range * 2), turret -> owner.getUUID().equals(turret.owner));
        for (TurretEntity turret : turrets) {
            if (attacker instanceof Player player) turret.markRetaliation(player);
            if (turret.isValidTarget(attacker)) turret.alertTo(attacker);
        }
    }
}
