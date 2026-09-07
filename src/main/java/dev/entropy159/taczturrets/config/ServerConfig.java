package dev.entropy159.taczturrets.config;

import dev.entropy159.taczturrets.turret.state.InaccuracyMode;
import dev.entropy159.taczturrets.turret.state.RetaliateTargeting;
import dev.entropy159.taczturrets.util.ItemFilter;
import dev.entropy159.taczturrets.util.TargetFilter;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class ServerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue CONSUME_AMMO = BUILDER.comment("Whether turrets need ammo.").define("consumeAmmo", true);
    public static final ModConfigSpec.IntValue TURRET_RANGE = BUILDER.comment("Turret detection and engagement range in blocks.").defineInRange("turretRange", 64, 8, 1000);
    public static final ModConfigSpec.IntValue SNIPER_TURRET_RANGE = BUILDER.comment("Turret range for sniper guns.").defineInRange("sniperTurretRange", 128, 8, 1000);
    public static final ModConfigSpec.ConfigValue<List<? extends String>> SNIPER_GUN_TYPES = BUILDER.comment("Gun types that use the sniper range.").defineList("sniperGunTypes", List.of("sniper"), () -> "sniper", entry -> entry instanceof String type && !type.isBlank());
    public static final ModConfigSpec.IntValue TURRET_HEALTH = BUILDER.comment("Health of turret.").defineInRange("turretHealth", 200, 10, 1000);
    public static final ModConfigSpec.DoubleValue TURRET_ARMOR = BUILDER.comment("The armor value of the turret").defineInRange("turretArmor", 6d, 0, 1000);
    public static final ModConfigSpec.IntValue TURRET_SLOT_ROWS = BUILDER.comment("Rows of ammo slots in the turret screen.").defineInRange("turretSlotRows", 2, 1, 6);
    public static final ModConfigSpec.IntValue TURRET_SLOT_LENGTH = BUILDER.comment("Ammo slots per row in the turret screen.").defineInRange("turretSlotLength", 5, 1, 9);
    public static final ModConfigSpec.BooleanValue TURRETS_TAKE_DAMAGE = BUILDER.comment("If false, turrets will be immune to damage.").define("turretsTakeDamage", true);
    public static final ModConfigSpec.BooleanValue TARGET_ALL_MOBS = BUILDER.comment("If true, turrets target all living entities (except players, turrets, and the owner). If false, turrets only target vanilla monsters and entities in the taczturrets:turret_targets entity type tag.").define("targetAllMobs", false);
    public static final ModConfigSpec.ConfigValue<List<? extends String>> TARGET_BLACKLIST = BUILDER.comment("Entities turrets never target. Accepts entity ids and #entity tags.").defineList("targetBlacklist", List.of(), () -> "", TargetFilter::isValidEntry);
    public static final ModConfigSpec.ConfigValue<List<? extends String>> TARGET_WHITELIST = BUILDER.comment("Entities turrets always target. Accepts entity ids and #entity tags.").defineList("targetWhitelist", List.of(), () -> "", TargetFilter::isValidEntry);
    public static final ModConfigSpec.ConfigValue<List<? extends String>> DAMAGEABLE_ENTITIES = BUILDER.comment("Entities turret fire can damage, so bullets pass through bystanders. Leave empty to match what turrets target. Accepts entity ids and #entity tags.").defineList("damageableEntities", List.of(), () -> "", TargetFilter::isValidEntry);
    public static final ModConfigSpec.EnumValue<InaccuracyMode> INACCURACY_MODE = BUILDER.comment("How turret inaccuracy is calculated. DISTANCE gets less accurate the further away the target is, RANDOM is the same at any range.").defineEnum("inaccuracyMode", InaccuracyMode.DISTANCE);
    public static final ModConfigSpec.DoubleValue DISTANCE_INACCURACY = BUILDER.comment("Distance inaccuracy at maximum range. 0 always hits.").defineInRange("distanceInaccuracy", 1.0D, 0.0D, 45.0D);
    public static final ModConfigSpec.DoubleValue RANDOM_INACCURACY = BUILDER.comment("Random inaccuracy. 0 always hits.").defineInRange("randomInaccuracy", 1.0D, 0.0D, 45.0D);
    public static final ModConfigSpec.IntValue ADAPTIVE_RANGE = BUILDER.comment("Distance at which adaptive mode switches from conserving ammo to firing freely.").defineInRange("adaptiveRange", 25, 1, 1000);
    public static final ModConfigSpec.BooleanValue TURRET_RECOIL = BUILDER.comment("Turret recoil.").define("turretRecoil", true);
    public static final ModConfigSpec.BooleanValue REPAIR_PARTICLES = BUILDER.comment("Repair particles.").define("repairParticles", true);
    public static final ModConfigSpec.BooleanValue ENABLE_SOUNDS = BUILDER.comment("Enable sounds.").define("enableSounds", true);
    public static final ModConfigSpec.ConfigValue<List<? extends String>> REPAIR_SOUNDS = BUILDER.comment("Sounds played when a turret is repaired. One is picked at random.").defineList("repairSounds", List.of("minecraft:entity.iron_golem.repair"), () -> "minecraft:entity.iron_golem.repair", ItemFilter::isValidEntry);
    public static final ModConfigSpec.BooleanValue RELOAD_SOUND = BUILDER.comment("Turret reload sound.").define("reloadSound", true);
    public static final ModConfigSpec.BooleanValue FIRST_PERSON_SHOOT_SOUND = BUILDER.comment("Turrets use the first person gunshot sound. Needed for sound packs that only replace first person sounds.").define("firstPersonShootSound", false);
    public static final ModConfigSpec.BooleanValue ALLIES_CANNOT_BE_DAMAGED = BUILDER.comment("Allies cannot be damaged by turret fire.").define("alliesCannotBeDamaged", true);
    public static final ModConfigSpec.EnumValue<RetaliateTargeting> RETALIATE_TARGETING = BUILDER.comment("How turrets treat a player they are retaliating against. CONTINUE_TARGETING keeps shooting them, CLEAR_ON_DEATH forgets them once they die.").defineEnum("retaliateTargeting", RetaliateTargeting.CLEAR_ON_DEATH);
    public static final ModConfigSpec.IntValue RETALIATION_TIMER = BUILDER.comment("Grudge time in seconds when Retaliate Targeting type is Continue Targeting.").defineInRange("retaliationTimer", 30, 1, 3600);
    public static final ModConfigSpec.BooleanValue ALLIES_HAVE_PERMS = BUILDER.comment("Allies can interact with turrets even if they are not owner.").define("alliesHavePerms", true);
    public static final ModConfigSpec.BooleanValue OP_BYPASS = BUILDER.comment("Operators can manage any turret.").define("opBypass", true);
    public static final ModConfigSpec.BooleanValue OWNER_TAKES_NO_DAMAGE = BUILDER.comment("Owners cannot be damaged by their own turrets.").define("ownerTakesNoDamage", true);
    public static final ModConfigSpec.BooleanValue BETTER_TARGETING = BUILDER.comment("Better turret targeting. Turrets pick separate targets instead of focusing one.").define("betterTargeting", true);
    public static final ModConfigSpec.BooleanValue DAMAGE_PLAYERS = BUILDER.comment("Enable player damage.").define("damagePlayers", true);
    public static final ModConfigSpec.BooleanValue PROTECT_OWNER = BUILDER.comment("Turrets defend their owner.").define("protectOwner", true);
    public static final ModConfigSpec.BooleanValue RESPECT_TEAMS = BUILDER.comment("Turrets spare teammates of their owner.").define("respectTeams", true);
    public static final ModConfigSpec.BooleanValue CREDIT_KILLS_TO_OWNER = BUILDER.comment("Turret kills count as owner kills.").define("creditKillsToOwner", true);
    public static final ModConfigSpec.BooleanValue PASSIVE_HEALING = BUILDER.comment("Passive healing.").define("passiveHealing", false);
    public static final ModConfigSpec.DoubleValue PASSIVE_HEAL_AMOUNT = BUILDER.comment("Passive healing amount.").defineInRange("passiveHealAmount", 1.0D, 0.0D, 1000.0D);
    public static final ModConfigSpec.IntValue PASSIVE_HEAL_INTERVAL = BUILDER.comment("Passive healing frequency in ticks.").defineInRange("passiveHealInterval", 100, 1, 72000);
    public static final ModConfigSpec.ConfigValue<List<? extends String>> REPAIR_ITEMS = BUILDER.comment("Items that can repair turrets. Accepts item ids and #item tags.").defineList("repairItems", List.of("minecraft:iron_bars"), () -> "minecraft:iron_bars", ItemFilter::isValidEntry);
    public static final ModConfigSpec.DoubleValue REPAIR_AMOUNT = BUILDER.comment("Health restored per repair item.").defineInRange("repairAmount", 25.0D, 0.0D, 1000.0D);
    public static final ModConfigSpec.BooleanValue REQUIRE_ENERGY = BUILDER.comment("Turrets need FE to run.").define("requireEnergy", false);
    public static final ModConfigSpec.IntValue ENERGY_CAPACITY = BUILDER.comment("Energy buffer size.").defineInRange("energyCapacity", 10000, 1, 1000000000);
    public static final ModConfigSpec.IntValue ENERGY_TRANSFER_RATE = BUILDER.comment("Energy accepted per tick.").defineInRange("energyTransferRate", 200, 1, 1000000000);
    public static final ModConfigSpec.IntValue ENERGY_PER_SHOT = BUILDER.comment("Energy used per shot.").defineInRange("energyPerShot", 10, 0, 1000000);
    public static final ModConfigSpec.IntValue ENERGY_IDLE_DRAIN = BUILDER.comment("Energy used per tick.").defineInRange("energyIdleDrain", 1, 0, 1000000);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static TargetFilter getBlacklist() {
        return TargetFilter.of(TARGET_BLACKLIST.get());
    }

    public static TargetFilter getWhitelist() {
        return TargetFilter.of(TARGET_WHITELIST.get());
    }

    public static TargetFilter getDamageable() {
        return TargetFilter.of(DAMAGEABLE_ENTITIES.get());
    }

    public static ItemFilter getRepairItems() {
        return ItemFilter.of(REPAIR_ITEMS.get());
    }
}
