package net.alshanex.magic_realms.events;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.util.humans.AdvancedNameManager;
import net.alshanex.magic_realms.util.humans.CombinedTextureManager;
import net.alshanex.magic_realms.util.humans.LayeredTextureManager;
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
            CombinedTextureManager.initializeDirectories();
            LayeredTextureManager.loadTextures();

            AdvancedNameManager.loadNamesFromResources();
        });
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((preparationBarrier, resourceManager, profilerFiller, profilerFiller2, executor, executor2) -> {
            return preparationBarrier.wait(null).thenRunAsync(() -> {
                MagicRealms.LOGGER.debug("Reloading textures due to resource pack reload...");
                LayeredTextureManager.clearCache();
                CombinedTextureManager.clearCache();
                LayeredTextureManager.loadTextures();

                AdvancedNameManager.reloadNames();
            }, executor2);
        });
    }
}
