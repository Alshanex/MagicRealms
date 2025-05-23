package net.alshanex.magic_realms.util.humans;

import net.alshanex.magic_realms.MagicRealms;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

public class TextureEventHandler {

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            LayeredTextureManager.loadTextures();
        });
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((preparationBarrier, resourceManager, profilerFiller, profilerFiller2, executor, executor2) -> {
            return preparationBarrier.wait(null).thenRunAsync(() -> {
                MagicRealms.LOGGER.debug("Reloading textures due to resource pack reload...");
                LayeredTextureManager.clearCache();
                LayeredTextureManager.loadTextures();
            }, executor2);
        });
    }
}
