package net.alshanex.magic_realms.registry;

import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MRSoundRegistry {
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, MagicRealms.MODID);

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }

    public static DeferredHolder<SoundEvent, SoundEvent> MAGIC_CREEPER_EXPLOSION = registerSoundEvent("magic_creeper_explosion");
    public static DeferredHolder<SoundEvent, SoundEvent> MAGIC_CREEPER_FUSE = registerSoundEvent("magic_creeper_fuse");

    private static DeferredHolder<SoundEvent, SoundEvent> registerSoundEvent(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, name)));
    }
}
