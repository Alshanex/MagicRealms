package net.alshanex.magic_realms.events;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.random.RandomHumanEntityRenderer;
import net.alshanex.magic_realms.util.humans.appearance.AdvancedNameManager;
import net.alshanex.magic_realms.util.humans.appearance.DynamicTextureManager;
import net.alshanex.magic_realms.util.humans.appearance.LayeredTextureManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

@EventBusSubscriber(modid = MagicRealms.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class TextureEventHandler {
    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((preparationBarrier, resourceManager, profilerFiller, profilerFiller2, executor, executor2) -> {
            return preparationBarrier.wait(null).thenRunAsync(() -> {
                MagicRealms.LOGGER.debug("Reloading textures due to resource pack reload...");
                LayeredTextureManager.clearCache();
                DynamicTextureManager.clearAllTextures();
                RandomHumanEntityRenderer.clearCompositeCache();
                LayeredTextureManager.loadTextures();
                AdvancedNameManager.reloadNames();

                int maleAdditionalCount = LayeredTextureManager.getAdditionalTextureCount(net.alshanex.magic_realms.util.humans.Gender.MALE);
                int femaleAdditionalCount = LayeredTextureManager.getAdditionalTextureCount(net.alshanex.magic_realms.util.humans.Gender.FEMALE);

                MagicRealms.LOGGER.debug("Reloaded additional textures - Male: {}, Female: {}", maleAdditionalCount, femaleAdditionalCount);
            }, executor2);
        });
    }
}
