package net.alshanex.magic_realms.registry;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.spells.SlimeRainSpell;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

import static io.redspace.ironsspellbooks.api.registry.SpellRegistry.SPELL_REGISTRY_KEY;

public class MRSpellRegistry {
    public static final DeferredRegister<AbstractSpell> SPELLS = DeferredRegister.create(SPELL_REGISTRY_KEY, MagicRealms.MODID);
    public static void register(IEventBus eventBus) {
        SPELLS.register(eventBus);
    }

    private static Supplier<AbstractSpell> registerSpell(AbstractSpell spell) {
        return SPELLS.register(spell.getSpellName(), () -> spell);
    }

    public static final Supplier<AbstractSpell> SLIME_RAIN = registerSpell(new SlimeRainSpell());
}
