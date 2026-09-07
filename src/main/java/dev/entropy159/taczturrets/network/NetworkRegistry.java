package dev.entropy159.taczturrets.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber
public class NetworkRegistry {
    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");

        registrar.playToClient(ToggleAllyPacket.TYPE, ToggleAllyPacket.STREAM_CODEC, ToggleAllyPacket::handle);
    }
}
