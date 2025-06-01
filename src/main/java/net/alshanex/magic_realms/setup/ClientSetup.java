package net.alshanex.magic_realms.setup;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.screens.HumanInfoScreen;
import net.alshanex.magic_realms.entity.RandomHumanEntityRenderer;
import net.alshanex.magic_realms.entity.WardenCloneRenderer;
import net.alshanex.magic_realms.registry.MREntityRegistry;
import net.alshanex.magic_realms.registry.MRMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = MagicRealms.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void rendererRegister(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MREntityRegistry.HUMAN.get(), RandomHumanEntityRenderer::new);
        event.registerEntityRenderer(MREntityRegistry.ELDRITCH_CLONE.get(), WardenCloneRenderer::new);
    }

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(MRMenus.HUMAN_INFO_MENU.get(), HumanInfoScreen::new);
    }
}
