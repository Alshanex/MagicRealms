package net.alshanex.magic_realms;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
@EventBusSubscriber(modid = MagicRealms.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue DISTANCE_NUMBER = BUILDER
            .comment("Distance range from where sneaky trait starts dodging attacks.")
            .defineInRange("distance", 6, 0, Integer.MAX_VALUE);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static int distance;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        distance = DISTANCE_NUMBER.get();
    }
}
