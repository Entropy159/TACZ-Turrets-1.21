package dev.entropy159.taczturrets.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.context.CommandContext;
import dev.entropy159.taczturrets.util.TurretAllies;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

@EventBusSubscriber
public class TurretCommand {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("turret")
                .then(Commands.literal("trust")
                        .then(Commands.argument("players", GameProfileArgument.gameProfile())
                                .executes(context -> setTrusted(context, true))))
                .then(Commands.literal("untrust")
                        .then(Commands.argument("players", GameProfileArgument.gameProfile())
                                .executes(context -> setTrusted(context, false))))
                .then(Commands.literal("trusted")
                        .executes(TurretCommand::listTrusted)));
    }

    private static int setTrusted(CommandContext<CommandSourceStack> context, boolean trust) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer owner = context.getSource().getPlayerOrException();
        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(context, "players");
        TurretAllies allies = TurretAllies.get(context.getSource().getServer());

        int changed = 0;
        for (GameProfile profile : profiles) {
            boolean updated = trust ? allies.addAlly(owner.getUUID(), profile.getId()) : allies.removeAlly(owner.getUUID(), profile.getId());
            if (updated) changed++;
        }

        int count = changed;
        context.getSource().sendSuccess(() -> Component.translatable(trust ? "command.taczturrets.trusted" : "command.taczturrets.untrusted", count), false);
        return changed;
    }

    private static int listTrusted(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer owner = context.getSource().getPlayerOrException();
        MinecraftServer server = context.getSource().getServer();
        java.util.Set<UUID> trusted = TurretAllies.get(server).getAllies(owner.getUUID());

        if (trusted.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.translatable("command.taczturrets.trusted_none"), false);
            return 0;
        }

        String names = trusted.stream()
                .map(id -> server.getProfileCache() == null ? null : server.getProfileCache().get(id).map(GameProfile::getName).orElse(null))
                .map(name -> name == null ? "?" : name)
                .collect(Collectors.joining(", "));
        context.getSource().sendSuccess(() -> Component.translatable("command.taczturrets.trusted_list", trusted.size(), names), false);
        return trusted.size();
    }
}
