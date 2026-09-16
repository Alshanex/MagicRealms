package net.alshanex.magic_realms.setup;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.chimera.ChimeraGeoRenderer;
import net.alshanex.magic_realms.entity.creeper.MagicCreeperEntityRenderer;
import net.alshanex.magic_realms.entity.enderman.WizardEndermanRenderer;
import net.alshanex.magic_realms.entity.humans.exclusive.ace.AceEntityRenderer;
import net.alshanex.magic_realms.entity.humans.exclusive.aliana.AlianaEntityRenderer;
import net.alshanex.magic_realms.entity.humans.exclusive.alshanex.AlshanexEntityRenderer;
import net.alshanex.magic_realms.entity.humans.exclusive.amadeus.AmadeusEntityRenderer;
import net.alshanex.magic_realms.entity.humans.exclusive.catas.CatasEntityRenderer;
import net.alshanex.magic_realms.entity.humans.exclusive.gojo_mojo.GojoMojoEntityRenderer;
import net.alshanex.magic_realms.entity.humans.exclusive.jara.JaraEntityRenderer;
import net.alshanex.magic_realms.entity.humans.exclusive.lilac.LilacEntityRenderer;
import net.alshanex.magic_realms.entity.flying_arrow.FloatingArrowEntityRenderer;
import net.alshanex.magic_realms.entity.humans.HostileRandomHumanEntityRenderer;
import net.alshanex.magic_realms.entity.slime.MagicSlimeEntityRenderer;
import net.alshanex.magic_realms.entity.tavernkeep.TavernKeeperEntityRenderer;
import net.alshanex.magic_realms.entity.tim.TimEntityRenderer;
import net.alshanex.magic_realms.registry.MRBlocks;
import net.alshanex.magic_realms.screens.ContractHumanInfoScreen;
import net.alshanex.magic_realms.entity.humans.RandomHumanEntityRenderer;
import net.alshanex.magic_realms.registry.MREntityRegistry;
import net.alshanex.magic_realms.registry.MRMenus;
import net.alshanex.magic_realms.screens.ContractInventoryScreen;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.NoopRenderer;
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
        event.registerEntityRenderer(MREntityRegistry.HOSTILE_HUMAN.get(), HostileRandomHumanEntityRenderer::new);
        event.registerEntityRenderer(MREntityRegistry.TAVERNKEEP.get(), TavernKeeperEntityRenderer::new);
        event.registerEntityRenderer(MREntityRegistry.ALSHANEX.get(), AlshanexEntityRenderer::new);
        event.registerEntityRenderer(MREntityRegistry.ALIANA.get(), AlianaEntityRenderer::new);
        event.registerEntityRenderer(MREntityRegistry.CATAS.get(), CatasEntityRenderer::new);
        event.registerEntityRenderer(MREntityRegistry.AMADEUS.get(), AmadeusEntityRenderer::new);
        event.registerEntityRenderer(MREntityRegistry.ACE.get(), AceEntityRenderer::new);
        event.registerEntityRenderer(MREntityRegistry.LILAC.get(), LilacEntityRenderer::new);
        event.registerEntityRenderer(MREntityRegistry.JARA.get(), JaraEntityRenderer::new);
        event.registerEntityRenderer(MREntityRegistry.GOJO_MOJO.get(), GojoMojoEntityRenderer::new);
        event.registerEntityRenderer(MREntityRegistry.MAGIC_SLIME.get(), MagicSlimeEntityRenderer::new);
        event.registerEntityRenderer(MREntityRegistry.SUMMONED_MAGIC_SLIME.get(), MagicSlimeEntityRenderer::new);
        event.registerEntityRenderer(MREntityRegistry.MAGIC_CREEPER.get(), MagicCreeperEntityRenderer::new);
        event.registerEntityRenderer(MREntityRegistry.TIM.get(), TimEntityRenderer::new);
        event.registerEntityRenderer(MREntityRegistry.WIZARD_ENDERMAN.get(), WizardEndermanRenderer::new);
        event.registerEntityRenderer(MREntityRegistry.CHIMERA_ENTITY.get(), ChimeraGeoRenderer::new);
        event.registerEntityRenderer(MREntityRegistry.HOMING_ICE_SPIKE_RUNNER.get(), NoopRenderer::new);

        event.registerEntityRenderer(MREntityRegistry.SEAT.get(), NoopRenderer::new);

        ItemBlockRenderTypes.setRenderLayer(MRBlocks.WOODEN_CHAIR_SIMPLE.get() , RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(MRBlocks.WOODEN_CHAIR.get() , RenderType.cutout());

        event.registerEntityRenderer(MREntityRegistry.FLOATING_ARROW.get(), FloatingArrowEntityRenderer::new);
    }

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(MRMenus.CONTRACT_HUMAN_INFO_MENU.get(), ContractHumanInfoScreen::new);
        event.register(MRMenus.CONTRACT_INVENTORY_MENU.get(), ContractInventoryScreen::new);
    }
}
