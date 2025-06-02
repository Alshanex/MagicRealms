package net.alshanex.magic_realms;

import net.alshanex.magic_realms.util.ArrowTypeManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
@EventBusSubscriber(modid = MagicRealms.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // Arrow system configuration

    private static final ModConfigSpec.BooleanValue ENABLE_RANDOM_ARROWS = BUILDER
            .comment("Enable random arrow types for archer entities")
            .define("enableRandomArrows", true);

    private static final ModConfigSpec.DoubleValue VANILLA_ARROW_CHANCE = BUILDER
            .comment("Chance for vanilla arrows to be used (0.0 = never, 1.0 = always)")
            .defineInRange("vanillaArrowChance", 0.6, 0.0, 1.0);

    private static final ModConfigSpec.BooleanValue STAR_LEVEL_AFFECTS_ARROWS = BUILDER
            .comment("Whether star level affects arrow type selection")
            .define("starLevelAffectsArrows", true);

    private static final ModConfigSpec.ConfigValue<List<? extends String>> BLACKLISTED_ARROWS = BUILDER
            .comment("List of arrow item IDs to exclude from random selection (e.g., ['modname:overpowered_arrow'])")
            .defineList("blacklistedArrows",
                    List.of(),
                    obj -> obj instanceof String);

    private static final ModConfigSpec.ConfigValue<List<? extends String>> WHITELISTED_MODS = BUILDER
            .comment("List of mod IDs whose arrows should be included (empty = all mods allowed)")
            .defineList("whitelistedMods",
                    List.of(),
                    obj -> obj instanceof String);

    static final ModConfigSpec SPEC = BUILDER.build();

    // Arrow config values
    public static boolean enableRandomArrows;
    public static double vanillaArrowChance;
    public static boolean starLevelAffectsArrows;
    public static List<? extends String> blacklistedArrows;
    public static List<? extends String> whitelistedMods;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {

        // Load arrow config
        enableRandomArrows = ENABLE_RANDOM_ARROWS.get();
        vanillaArrowChance = VANILLA_ARROW_CHANCE.get();
        starLevelAffectsArrows = STAR_LEVEL_AFFECTS_ARROWS.get();
        blacklistedArrows = BLACKLISTED_ARROWS.get();
        whitelistedMods = WHITELISTED_MODS.get();

        MagicRealms.LOGGER.info("Config loaded - Random arrows: {}, Vanilla chance: {}",
                enableRandomArrows, vanillaArrowChance);

        // Reload arrow types when config changes
        try {
            ArrowTypeManager.reloadArrowTypes();
        } catch (Exception e) {
            MagicRealms.LOGGER.debug("ArrowTypeManager not yet initialized, will load on setup");
        }
    }
}
