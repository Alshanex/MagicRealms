package net.alshanex.magic_realms.registry;

import io.redspace.ironsspellbooks.registries.CreativeTabRegistry;
import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MRCreativeTab {
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MagicRealms.MODID);

    public static void register(IEventBus eventBus) {
        TABS.register(eventBus);
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = TABS.register("magic_realms_main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + MagicRealms.MODID + ".main_tab"))
            .icon(() -> new ItemStack(MRItems.CONTRACT_TEMPORARY))
            .displayItems((enabledFeatures, entries) -> {
                entries.accept(MRItems.CONTRACT_PERMANENT.get());
                entries.accept(MRItems.CONTRACT_TEMPORARY.get());
                entries.accept(MRItems.SLEEPING_PASS.get());
                entries.accept(MRItems.HELL_PASS.get());
                entries.accept(MRItems.BLOOD_PACT.get());
                entries.accept(MRItems.PERMANENT_BLOOD_PACT.get());
                entries.accept(MRItems.WOODEN_CHAIR.get());
                entries.accept(MRItems.WOODEN_CHAIR_SIMPLE.get());
                entries.accept(MRItems.SKIN_CUSTOMIZER.get());
                entries.accept(MRItems.FLOATING_ARROW_WEAPON.get());
                entries.accept(MRItems.MAGE_ENDERMAN_SPAWN_EGG.get());
                entries.accept(MRItems.MAGIC_CREEPER_SPAWN_EGG.get());
                entries.accept(MRItems.MAGIC_SLIME_SPAWN_EGG.get());
                entries.accept(MRItems.HUMAN_MERCENARY_SPAWN_EGG.get());
                entries.accept(MRItems.BANDIT_SPAWN_EGG.get());
                entries.accept(MRItems.TAVERNKEEPER_SPAWN_EGG.get());
                entries.accept(MRItems.ZOMBIE_MAGE_SPAWN_EGG.get());
                entries.accept(MRItems.CHIMERA_CATALYST.get());
                entries.accept(MRItems.LOST_PAGES_BOOK.get());
                entries.accept(MRItems.LOST_PAGE.get());
                entries.accept(MRItems.ANCIENT_TABLET.get());
                entries.accept(MRItems.RESONANCE_CRYSTAL.get());
            })
            .withTabsBefore(CreativeTabRegistry.EQUIPMENT_TAB.getId())
            .build());
}
