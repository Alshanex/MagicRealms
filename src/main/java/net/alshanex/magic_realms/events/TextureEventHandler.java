package net.alshanex.magic_realms.events;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.random.RandomHumanEntityRenderer;
import net.alshanex.magic_realms.util.humans.mercenaries.AdvancedNameManager;
import net.alshanex.magic_realms.util.humans.mercenaries.skins_management.DynamicTextureManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

@EventBusSubscriber(modid = MagicRealms.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class TextureEventHandler {
    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((barrier, rm, pf1, pf2, e1, e2) ->
                barrier.wait(null).thenRunAsync(() -> {
                    MagicRealms.LOGGER.debug("Clearing composite texture caches on resource reload");
                    DynamicTextureManager.clearAllTextures();
                    RandomHumanEntityRenderer.clearCompositeCache();
                    AdvancedNameManager.reloadNames();
                }, e2));
    }
}
