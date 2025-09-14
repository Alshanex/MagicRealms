package net.alshanex.magic_realms.setup;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.network.RequestEntityLevelPacket;
import net.alshanex.magic_realms.network.SyncEntityLevelPacket;
import net.alshanex.magic_realms.network.SyncPresetTextureNamePacket;
import net.alshanex.magic_realms.network.UpdateEntityNamePacket;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, modid = MagicRealms.MODID)
public class PayloadHandler {
    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar payloadRegistrar = event.registrar(MagicRealms.MODID).versioned("1.0.0").optional();

        payloadRegistrar.playToServer(UpdateEntityNamePacket.TYPE, UpdateEntityNamePacket.STREAM_CODEC, UpdateEntityNamePacket::handle);
        payloadRegistrar.playToServer(RequestEntityLevelPacket.TYPE, RequestEntityLevelPacket.STREAM_CODEC, RequestEntityLevelPacket::handle);
        payloadRegistrar.playToServer(SyncPresetTextureNamePacket.TYPE, SyncPresetTextureNamePacket.STREAM_CODEC, SyncPresetTextureNamePacket::handle);

        payloadRegistrar.playToClient(SyncEntityLevelPacket.TYPE, SyncEntityLevelPacket.STREAM_CODEC, SyncEntityLevelPacket::handle);
    }
}
