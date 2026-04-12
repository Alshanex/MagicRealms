package net.alshanex.magic_realms.registry;

import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MREffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, MagicRealms.MODID);

    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }
}
