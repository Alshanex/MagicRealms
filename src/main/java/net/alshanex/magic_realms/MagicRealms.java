package net.alshanex.magic_realms;

import com.mojang.logging.LogUtils;
import net.alshanex.magic_realms.compat.GunArmPoser;
import net.alshanex.magic_realms.entity.humans.AbstractMercenaryEntity;
import net.alshanex.magic_realms.entity.humans.client.HeldItemPosers;
import net.alshanex.magic_realms.registry.*;
import net.alshanex.magic_realms.util.humans.combat.*;
import net.alshanex.magic_realms.util.humans.titles.TitleManager;
import net.alshanex.magic_realms.util.rpgdialogues.actions.TavernkeeperTrading;
import net.alshanex.magic_realms.util.rpgdialogues.conditions.EnityTypeCondition;
import net.alshanex.magic_realms.util.rpgdialogues.conditions.MercenaryClassCondition;
import net.alshanex.magic_realms.util.rpgdialogues.conditions.MercenaryCombatCondition;
import net.alshanex.magic_realms.util.rpgdialogues.conditions.TavernkeeperTipCondition;
import net.alshanex.magic_realms.util.rpgdialogues.values.MercenarySpeech;
import net.alshanex.magic_realms.util.rpgdialogues.values.TimeUntilPermanentContract;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.pixeldreamstudios.rpgdialogue.client.WorldText;
import net.pixeldreamstudios.rpgdialogue.dialogue.DialogueAction;
import net.pixeldreamstudios.rpgdialogue.dialogue.DialogueCondition;
import net.pixeldreamstudios.rpgdialogue.dialogue.DialogueValue;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(MagicRealms.MODID)
public class MagicRealms
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "magic_realms";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    private final ChimeraPartRegistry partRegistry = new ChimeraPartRegistry();

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

        MRCreativeTab.register(modEventBus);

        MREffects.register(modEventBus);

        MRBlocks.register(modEventBus);

        MRBlockEntities.register(modEventBus);

        MRSpellRegistry.register(modEventBus);

        MRSoundRegistry.register(modEventBus);

        MRBiomeModifiers.register(modEventBus);

        MRBossStoryRegistry.register(modEventBus);

        MRDataComponentRegistry.register(modEventBus);

        MRLootRegistry.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (ExampleMod) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        NeoForge.EVENT_BUS.addListener(this::onAddReloadListeners);

        NeoForge.EVENT_BUS.addListener(this::onDatapackSync);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(partRegistry);
    }

    private void onDatapackSync(OnDatapackSyncEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player != null) {
            partRegistry.sendToPlayer(player);
        } else {
            partRegistry.sendToAllPlayers();
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        event.enqueueWork(()->{
            // Dialogues
            DialogueAction.register(ResourceLocation.fromNamespaceAndPath(MODID, "tavernkeeper_trade"),
                    TavernkeeperTrading.CODEC);

            DialogueCondition.register(ResourceLocation.fromNamespaceAndPath(MODID, "tavernkeeper_tip"),
                    TavernkeeperTipCondition.CODEC);
            DialogueCondition.register(ResourceLocation.fromNamespaceAndPath(MODID, "mercenary_in_combat"),
                    MercenaryCombatCondition.CODEC);
            DialogueCondition.register(ResourceLocation.fromNamespaceAndPath(MODID, "mercenary_class"),
                    MercenaryClassCondition.CODEC);
            DialogueCondition.register(ResourceLocation.fromNamespaceAndPath(MODID, "entity_type"),
                    EnityTypeCondition.CODEC);

            DialogueValue.register(ResourceLocation.fromNamespaceAndPath(MODID, "time_until_permanent"),
                    TimeUntilPermanentContract.CODEC);
            DialogueValue.register(ResourceLocation.fromNamespaceAndPath(MODID, "speech_line"),
                    MercenarySpeech.CODEC);

            // Combat classes
            CombatClasses.register(new MageClass());
            CombatClasses.register(new WarriorClass());
            CombatClasses.register(new RogueClass());
            CombatClasses.register(new SupportMageClass());

            OptionalCombatClasses.registerAll();
        });
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {

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
            WorldText.CROWDING.register(entity -> {
                if (entity instanceof AbstractMercenaryEntity mercenary) {
                    if (TitleManager.displayedTitle(mercenary) != null) {
                        return 0.25F;
                    }
                }
                return 0.0F;
            });

            event.enqueueWork(() -> {
                if (ModList.get().isLoaded(OptionalCombatClasses.IRONS_ARTIFICE)) {
                    try {
                        HeldItemPosers.register(new GunArmPoser());
                    } catch (Throwable t) {
                        MagicRealms.LOGGER.error("Failed to register gun arm poser", t);
                    }
                }
            });
        }
    }
}
