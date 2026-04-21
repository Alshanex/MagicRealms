package net.alshanex.magic_realms.setup;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.network.*;
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
        payloadRegistrar.playToServer(SyncPresetNamePacket.TYPE, SyncPresetNamePacket.STREAM_CODEC, SyncPresetNamePacket::handle);
        payloadRegistrar.playToServer(SwitchTabPacket.TYPE, SwitchTabPacket.STREAM_CODEC, SwitchTabPacket::handle);
        payloadRegistrar.playToServer(SwitchAttributesMenuPacket.TYPE, SwitchAttributesMenuPacket.STREAM_CODEC, SwitchAttributesMenuPacket::handle);
        payloadRegistrar.playToServer(SaveSkinPartsPacket.TYPE, SaveSkinPartsPacket.STREAM_CODEC, SaveSkinPartsPacket::handle);

        payloadRegistrar.playToClient(SyncEntityLevelPacket.TYPE, SyncEntityLevelPacket.STREAM_CODEC, SyncEntityLevelPacket::handle);
        payloadRegistrar.playToClient(SyncSkinCatalogPacket.TYPE, SyncSkinCatalogPacket.STREAM_CODEC, SyncSkinCatalogPacket::handle);
        payloadRegistrar.playToClient(SyncHobbyCatalogPacket.TYPE, SyncHobbyCatalogPacket.STREAM_CODEC, SyncHobbyCatalogPacket::handle);
        payloadRegistrar.playToClient(OpenSkinCustomizerPacket.TYPE, OpenSkinCustomizerPacket.STREAM_CODEC, OpenSkinCustomizerPacket::handle);
    }
}
