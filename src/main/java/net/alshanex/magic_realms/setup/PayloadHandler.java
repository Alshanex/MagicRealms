package net.alshanex.magic_realms.setup;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.network.SummonAlliesPackage;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, modid = MagicRealms.MODID)
public class PayloadHandler {
    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar payloadRegistrar = event.registrar(MagicRealms.MODID).versioned("1.0.0").optional();

        payloadRegistrar.playToServer(SummonAlliesPackage.TYPE, SummonAlliesPackage.STREAM_CODEC, SummonAlliesPackage::handle);
    }
}
