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
            // Load the base textures (world-specific directories will be set when joining a world)
            LayeredTextureManager.loadTextures();

            // Load names
            AdvancedNameManager.loadNamesFromResources();

            MagicRealms.LOGGER.info("Client setup completed - texture directories will be initialized when joining a world");

            // Log additional texture availability for debugging
            int maleAdditionalCount = LayeredTextureManager.getAdditionalTextureCount(net.alshanex.magic_realms.util.humans.Gender.MALE);
            int femaleAdditionalCount = LayeredTextureManager.getAdditionalTextureCount(net.alshanex.magic_realms.util.humans.Gender.FEMALE);

            MagicRealms.LOGGER.info("Additional textures loaded - Male: {}, Female: {}", maleAdditionalCount, femaleAdditionalCount);
            MagicRealms.LOGGER.info("Additional texture chance: {}%", (int)(CombinedTextureManager.getAdditionalTextureChance() * 100));
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

                // Log reloaded additional texture counts
                int maleAdditionalCount = LayeredTextureManager.getAdditionalTextureCount(net.alshanex.magic_realms.util.humans.Gender.MALE);
                int femaleAdditionalCount = LayeredTextureManager.getAdditionalTextureCount(net.alshanex.magic_realms.util.humans.Gender.FEMALE);

                MagicRealms.LOGGER.debug("Reloaded additional textures - Male: {}, Female: {}", maleAdditionalCount, femaleAdditionalCount);
            }, executor2);
        });
    }
}
