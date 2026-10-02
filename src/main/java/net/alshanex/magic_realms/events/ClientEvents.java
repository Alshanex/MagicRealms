package net.alshanex.magic_realms.events;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.util.MRClientUtils;
import net.alshanex.magic_realms.util.humans.mercenaries.ClientContractedMercenaries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import static net.alshanex.magic_realms.setup.KeyMappings.OPEN_ASSEMBLY;

@EventBusSubscriber(modid = MagicRealms.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        handleKeybinds();
    }

    private static void handleKeybinds() {
        while (OPEN_ASSEMBLY.consume()) {
            MRClientUtils.openAssemblyScreen();
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientContractedMercenaries.clear();
    }
}
