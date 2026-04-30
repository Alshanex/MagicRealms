package net.alshanex.magic_realms.events;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.screens.ContractHumanInfoScreen;
import net.alshanex.magic_realms.screens.ContractInventoryScreen;
import net.alshanex.magic_realms.util.humans.mercenaries.chat.ChatFaceCompositeCache;
import net.alshanex.magic_realms.util.humans.mercenaries.chat.ChatFaceRenderer;
import net.alshanex.magic_realms.util.humans.mercenaries.chat.ChatLineFaceTable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

@EventBusSubscriber(modid = MagicRealms.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientWorldEventHandler {

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onClientWorldUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            ChatFaceCompositeCache.clear();
            ChatFaceRenderer.clearCache();
            ChatLineFaceTable.clear();

            ContractInventoryScreen.clearVirtualEntityCache();
            ContractHumanInfoScreen.clearVirtualEntityCache();

            MagicRealms.LOGGER.debug("Cleared mod caches on world unload");
        }
    }
}
