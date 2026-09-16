package net.alshanex.magic_realms.util.humans.combat;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.humans.AbstractMercenaryEntity;
import net.alshanex.magic_realms.util.humans.goals.MercenaryGoalManager;
import net.alshanex.magic_realms.util.humans.mercenaries.HumanStatsManager;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public abstract class AbstractCombatClass implements CombatClass {

    private final ResourceLocation id;

    protected AbstractCombatClass(String path) {
        this.id = ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, path);
    }

    @Override public ResourceLocation id() { return id; }

    @Override
    public Component displayName(AbstractMercenaryEntity e) {
        return Component.translatable("gui.magic_realms.human_info.class." + id.getPath());
    }

    @Override
    public ItemStack symbolItem(AbstractMercenaryEntity e) {
        return ItemStack.EMPTY;
    }

    /**
     * Delegates to HumanStatsManager so the modifier-id and rounding logic stays in one place.
     * Names are prefixed with the class path to keep modifiers per-class and idempotent.
     */
    protected void addModifier(AbstractMercenaryEntity e, Holder<Attribute> attr,
                               String name, double amount, AttributeModifier.Operation op) {
        HumanStatsManager.addAttributeModifier(
                e, attr, id.getPath() + "_" + name.toLowerCase(Locale.ROOT).replace(' ', '_'), amount, op);
    }

    protected double starValue(int stars, double min1, double max1,
                               double min2, double max2,
                               double min3, double max3, RandomSource rng) {
        return HumanStatsManager.getStarBasedValue(stars, min1, max1, min2, max2, min3, max3, rng);
    }

    /** Delegates to the single canonical predicate so class implementations can't drift apart. */
    protected void clearOwnGoals(AbstractMercenaryEntity e) {
        MercenaryGoalManager.clearAttackGoals(e);
    }

    protected List<AbstractSpell> withEquipmentSpells(AbstractMercenaryEntity e,
                                                      List<AbstractSpell> spells) {
        List<AbstractSpell> out = new ArrayList<>(spells);
        out.addAll(MercenaryGoalManager.extractSpellsFromEquipment(e));
        return out;
    }

    protected static boolean isMeleeWeapon(ItemStack stack) {
        if (stack.getItem() instanceof SwordItem) return true;
        String itemName = stack.getItem().toString().toLowerCase(Locale.ROOT);
        return itemName.contains("sword") || itemName.contains("blade") || itemName.contains("dagger");
    }
}
