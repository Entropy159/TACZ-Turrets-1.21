package dev.entropy159.taczturrets.registry;

import com.tterrag.registrate.util.entry.EntityEntry;
import dev.entropy159.taczturrets.client.renderer.TurretRenderer;
import dev.entropy159.taczturrets.turret.TurretEntity;
import net.minecraft.world.entity.MobCategory;

import static dev.entropy159.taczturrets.TACZTurrets.REGISTRATE;

public class EntityTypeRegistry {
    public static final EntityEntry<TurretEntity> TURRET = REGISTRATE.entity("turret", TurretEntity::new, MobCategory.MISC).renderer(() -> TurretRenderer::new).attributes(TurretEntity::createLivingAttributes).register();

    public static void init() {}
}
