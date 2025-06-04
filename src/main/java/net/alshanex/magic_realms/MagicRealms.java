package net.alshanex.magic_realms;

import com.mojang.logging.LogUtils;
import net.alshanex.magic_realms.registry.*;
import net.alshanex.magic_realms.util.ArrowTypeManager;
import net.alshanex.magic_realms.util.humans.CombinedTextureManager;
import net.alshanex.magic_realms.events.TextureEventHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(MagicRealms.MODID)
public class MagicRealms
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "magic_realms";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public MagicRealms(IEventBus modEventBus, ModContainer modContainer)
    {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        MREntityRegistry.register(modEventBus);

        MRDataAttachments.register(modEventBus);

        MRItems.register(modEventBus);

        MRMenus.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (ExampleMod) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(TextureEventHandler::onClientSetup);
        modEventBus.addListener(TextureEventHandler::onRegisterReloadListeners);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        event.enqueueWork(() -> {
            ArrowTypeManager.initializeArrowTypes();

            LOGGER.info("Magic Realms common setup completed. Available arrow types: {}",
                    ArrowTypeManager.getAvailableArrowCount());
        });
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(MRItems.CONTRACT_APPRENTICE.get());
            event.accept(MRItems.CONTRACT_EXPERT.get());
            event.accept(MRItems.CONTRACT_JOURNEYMAN.get());
            event.accept(MRItems.CONTRACT_MASTER.get());
            event.accept(MRItems.CONTRACT_NOVICE.get());
            event.accept(MRItems.CONTRACT_PERMANENT.get());
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {

    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            event.enqueueWork(() -> {
                CombinedTextureManager.initializeDirectories();
                MagicRealms.LOGGER.info("Cleaned up orphaned textures from previous sessions");
            });
        }
    }
}
