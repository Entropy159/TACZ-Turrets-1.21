package dev.entropy159.taczturrets;

import com.mojang.logging.LogUtils;
import com.tterrag.registrate.Registrate;
import dev.entropy159.taczturrets.config.ClientConfig;
import dev.entropy159.taczturrets.config.ServerConfig;
import dev.entropy159.taczturrets.registry.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.slf4j.Logger;

@Mod(TACZTurrets.MODID)
public class TACZTurrets {
    public static final String MODID = "taczturrets";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final Registrate REGISTRATE = Registrate.create(MODID).defaultCreativeTab((ResourceKey<CreativeModeTab>) null);

    public TACZTurrets(IEventBus bus, ModContainer container) {
        bus.addListener(this::registerCapabilities);

        EntityTypeRegistry.init();
        ItemRegistry.init();
        MenuRegistry.init();
        SoundRegistry.init(bus);
        TagRegistry.init();

        addLang();

        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        container.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerEntity(Capabilities.ItemHandler.ENTITY, EntityTypeRegistry.TURRET.get(), (entity, context) -> entity.getInventory());
        event.registerEntity(Capabilities.EnergyStorage.ENTITY, EntityTypeRegistry.TURRET.get(), (entity, context) -> entity.getEnergyStorage());
    }

    private static void addLang() {
        REGISTRATE.addRawLang("gui.taczturrets.health", "Health: %s / %s");
        REGISTRATE.addRawLang("gui.taczturrets.energy", "Energy: %s / %s FE");
        REGISTRATE.addRawLang("gui.taczturrets.enable_type.always_on", "Always On");
        REGISTRATE.addRawLang("gui.taczturrets.enable_type.redstone_on", "Redstone On");
        REGISTRATE.addRawLang("gui.taczturrets.enable_type.redstone_off", "Redstone Off");
        REGISTRATE.addRawLang("gui.taczturrets.enable_type.always_off", "Always Off");
        REGISTRATE.addRawLang("gui.taczturrets.enable_type.always_on.tooltip", "The turret always runs.");
        REGISTRATE.addRawLang("gui.taczturrets.enable_type.redstone_on.tooltip", "The turret only runs while it has a redstone signal.");
        REGISTRATE.addRawLang("gui.taczturrets.enable_type.redstone_off.tooltip", "The turret stops while it has a redstone signal.");
        REGISTRATE.addRawLang("gui.taczturrets.enable_type.always_off.tooltip", "The turret is switched off.");
        REGISTRATE.addRawLang("gui.taczturrets.mode.aggressive", "Aggressive");
        REGISTRATE.addRawLang("gui.taczturrets.mode.conservative", "Conservative");
        REGISTRATE.addRawLang("gui.taczturrets.mode.adaptive", "Adaptive");
        REGISTRATE.addRawLang("gui.taczturrets.mode.aggressive.tooltip", "Fires as fast as the gun allows and moves straight onto the next target.");
        REGISTRATE.addRawLang("gui.taczturrets.mode.conservative.tooltip", "Spaces its shots out to save ammo.");
        REGISTRATE.addRawLang("gui.taczturrets.mode.adaptive.tooltip", "Saves ammo at range. Aggressive when a target is close.");
        REGISTRATE.addRawLang("gui.taczturrets.owner", "Owner: %s");
        REGISTRATE.addRawLang("gui.taczturrets.player_targeting.never", "Never Target");
        REGISTRATE.addRawLang("gui.taczturrets.player_targeting.retaliate", "Retaliate");
        REGISTRATE.addRawLang("gui.taczturrets.player_targeting.all", "Always Target");
        REGISTRATE.addRawLang("gui.taczturrets.player_targeting.never.tooltip", "Never target players.");
        REGISTRATE.addRawLang("gui.taczturrets.player_targeting.retaliate.tooltip", "Retaliate when damaged by non-ally players.");
        REGISTRATE.addRawLang("gui.taczturrets.player_targeting.all.tooltip", "Always target non-ally players.");
        REGISTRATE.addRawLang("command.taczturrets.trusted", "Trusted %s player(s).");
        REGISTRATE.addRawLang("command.taczturrets.untrusted", "Removed trust from %s player(s).");
        REGISTRATE.addRawLang("command.taczturrets.trusted_none", "You have no trusted players.");
        REGISTRATE.addRawLang("command.taczturrets.trusted_list", "%s trusted player(s): %s");
        REGISTRATE.addRawLang("gui.taczturrets.allies", "Allies");
        REGISTRATE.addRawLang("gui.taczturrets.allies.ally", "Ally");
        REGISTRATE.addRawLang("gui.taczturrets.allies.empty", "No other players online.");
        REGISTRATE.addRawLang("subtitles.taczturrets.turret_place", "Turret deployed");
        REGISTRATE.addRawLang("subtitles.taczturrets.turret_pickup", "Turret retrieved");
    }
}
