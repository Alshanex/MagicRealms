package net.alshanex.magic_realms.util.humans.combat;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.alshanex.magic_realms.entity.humans.AbstractMercenaryEntity;
import net.alshanex.magic_realms.util.humans.goals.battle_goals.BattlefieldAnalysis;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

public interface CombatClass {

    ResourceLocation id();

    /** Key used by the skin catalog's class_filter. Defaults to the id path. */
    default String skinCategory() { return id().getPath(); }

    /** Handles its own variants, e.g. warrior vs warrior_shield. */
    Component displayName(AbstractMercenaryEntity entity);

    /** Icon in the contract info screen. Return ItemStack.EMPTY for none. */
    ItemStack symbolItem(AbstractMercenaryEntity entity);

    /** Roll class flags: magic schools, shield, archer. */
    default void initializeSpecifics(AbstractMercenaryEntity entity, RandomSource rng) {}

    /** Attribute modifiers. MUST be idempotent — remove-then-add. */
    void applyAttributes(AbstractMercenaryEntity entity, int starLevel, RandomSource rng);

    /** Starting weapons and inventory. */
    default void applyStartingEquipment(AbstractMercenaryEntity entity) {}

    /** Spell power / resistance gains. Called from MagicAttributeGainsHandler. */
    default double spellPowerBonus(int starLevel, RandomSource rng) { return 0.0; }
    default double spellResistanceBonus(int starLevel, RandomSource rng) { return 0.0; }

    List<AbstractSpell> generateSpells(AbstractMercenaryEntity entity, RandomSource rng);

    /** Install combat goals. Called on spawn, world load, and equipment change. Clears its own goals first. */
    void installCombatGoals(AbstractMercenaryEntity entity, List<AbstractSpell> spells);

    /** Re-evaluate after gear changed. */
    default void onEquipmentChanged(AbstractMercenaryEntity entity) {
        installCombatGoals(entity, entity.getPersistedSpells());
    }

    /** True if this class reacts to an equipped spellbook. */
    default boolean usesSpellbooks() { return false; }

    /** True if this class can gain spell resist. */
    default boolean canGainSpellResist() { return false; }

    /** True if this class can gain spell power. */
    default boolean canGainSpellPower() { return false; }

    /**
     * The role name this entity currently presents as, including variants.
     *
     * <p>Warrior returns "tank" when shielded; rogue returns "archer" when it carries a bow.
     * This is a presentation name, not an identity — {@link #id()} stays constant.
     */
    default String roleName(ClassLoadout loadout) { return id().getPath(); }

    default boolean prefersMelee(ClassLoadout loadout) { return false; }
    default boolean prefersRangedWeapons(ClassLoadout loadout) { return false; }
    default boolean usesStaves(ClassLoadout loadout) { return false; }
    default boolean wantsShield(ClassLoadout loadout) { return false; }

    default WeaponPreference weaponPreference(ClassLoadout loadout) {
        return WeaponPreference.RAW_DAMAGE;
    }

    /** Whether this item may go in the main hand. */
    default boolean acceptsMainHand(ItemStack stack, ClassLoadout loadout) { return false; }

    /** Whether this item may go in the offhand. */
    default boolean acceptsOffHand(ItemStack stack, ClassLoadout loadout) { return false; }

    /** Empty-slot icon sprite for the offhand. Null falls back to the vanilla shield sprite. */
    @Nullable
    default ResourceLocation emptyOffHandIcon(ClassLoadout loadout) { return null; }

    /** Whether the info screen shows a "Schools" section for this class. */
    default boolean usesMagicSchools() { return false; }

    /**
     * Class-specific target refinement, called every ~20 ticks by TacticalTargetSelectorGoal after the boss-engagement override.
     *
     * <p>Return null to leave the current target alone. The battlefield scan has already been refreshed by the caller,
     * so implementations can read it directly.
     */
    @Nullable
    default LivingEntity selectPreferredTarget(AbstractMercenaryEntity self,
                                               BattlefieldAnalysis battlefield) {
        return null;
    }

    default int spawnWeight() { return 10; }
    default boolean inRandomPool() { return true; }
}
