package net.alshanex.magic_realms.events;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.registry.MRRegistries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.NewRegistryEvent;

@EventBusSubscriber(modid = MagicRealms.MODID, bus = EventBusSubscriber.Bus.MOD)
public class MRRegistryEvents {
    @SubscribeEvent
    public static void registerRegistries(NewRegistryEvent event) {
        event.register(MRRegistries.BOSS_STORIES);
    }
}
