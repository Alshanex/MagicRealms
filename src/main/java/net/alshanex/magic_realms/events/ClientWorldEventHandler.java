package net.alshanex.magic_realms.events;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.random.RandomHumanEntityRenderer;
import net.alshanex.magic_realms.util.humans.appearance.DynamicTextureManager;
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
            // Clear texture caches when world unloads on client
            DynamicTextureManager.clearAllTextures();
            RandomHumanEntityRenderer.clearCompositeCache();
            MagicRealms.LOGGER.debug("Cleared client texture caches on world unload");
        }
    }
}
