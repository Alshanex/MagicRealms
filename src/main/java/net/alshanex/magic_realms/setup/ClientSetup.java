package net.alshanex.magic_realms.setup;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.exclusive.aliana.AlianaEntityRenderer;
import net.alshanex.magic_realms.entity.exclusive.alshanex.AlshanexEntityRenderer;
import net.alshanex.magic_realms.entity.exclusive.amadeus.AmadeusEntityRenderer;
import net.alshanex.magic_realms.entity.exclusive.catas.CatasEntityRenderer;
import net.alshanex.magic_realms.entity.slime.MagicSlimeEntityRenderer;
import net.alshanex.magic_realms.entity.tavernkeep.TavernKeeperEntityRenderer;
import net.alshanex.magic_realms.screens.ContractHumanInfoScreen;
import net.alshanex.magic_realms.entity.random.RandomHumanEntityRenderer;
import net.alshanex.magic_realms.registry.MREntityRegistry;
import net.alshanex.magic_realms.registry.MRMenus;
import net.alshanex.magic_realms.screens.ContractInventoryScreen;
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
        event.registerEntityRenderer(MREntityRegistry.TAVERNKEEP.get(), TavernKeeperEntityRenderer::new);
        event.registerEntityRenderer(MREntityRegistry.ALSHANEX.get(), AlshanexEntityRenderer::new);
        event.registerEntityRenderer(MREntityRegistry.ALIANA.get(), AlianaEntityRenderer::new);
        event.registerEntityRenderer(MREntityRegistry.CATAS.get(), CatasEntityRenderer::new);
        event.registerEntityRenderer(MREntityRegistry.AMADEUS.get(), AmadeusEntityRenderer::new);
        event.registerEntityRenderer(MREntityRegistry.MAGIC_SLIME.get(), MagicSlimeEntityRenderer::new);
    }

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(MRMenus.CONTRACT_HUMAN_INFO_MENU.get(), ContractHumanInfoScreen::new);
        event.register(MRMenus.CONTRACT_INVENTORY_MENU.get(), ContractInventoryScreen::new);
    }
}
