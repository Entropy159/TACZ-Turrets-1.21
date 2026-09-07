package dev.entropy159.taczturrets.network;

import dev.entropy159.taczturrets.TACZTurrets;
import dev.entropy159.taczturrets.menu.TurretMenu;
import dev.entropy159.taczturrets.util.TurretAllies;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record ToggleAllyPacket(UUID target) implements CustomPacketPayload {
    public static final Type<ToggleAllyPacket> TYPE = new Type<>(TACZTurrets.id("toggle_ally"));
    public static final StreamCodec<FriendlyByteBuf, ToggleAllyPacket> STREAM_CODEC = StreamCodec.of(((buf, val) -> buf.writeUUID(val.target())), buf -> new ToggleAllyPacket(buf.readUUID()));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player && player.getServer() != null) {
                if (player.containerMenu instanceof TurretMenu menu) {
                    if (menu.getTurret() == null || !menu.getTurret().isOwnedBy(player)) return;
                    UUID owner = menu.getTurret().owner;
                    if (owner == null || owner.equals(target)) {
                        return;
                    }
                    TurretAllies.get(player.getServer()).toggleAlly(owner, target);
                }
            }
        });
    }
}
