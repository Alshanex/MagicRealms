package net.alshanex.magic_realms.util.humans.goals;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.entity.mobs.goals.melee.AttackAnimationData;
import io.redspace.ironsspellbooks.entity.mobs.wizards.GenericAnimatedWarlockAttackGoal;
import net.alshanex.magic_realms.Config;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.util.ModTags;
import net.alshanex.magic_realms.util.humans.goals.ChargeArrowAttackGoal;
import net.alshanex.magic_realms.util.humans.goals.HumanGoals;
import net.alshanex.magic_realms.util.humans.goals.battle_goals.*;
import net.alshanex.magic_realms.util.humans.mercenaries.EntityClass;
import net.alshanex.magic_realms.util.humans.mercenaries.SpellListGenerator;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Centralized configuration of class-specific combat goals for mercenaries.
 */
public final class MercenaryCombatGoals {

    private MercenaryCombatGoals() {}

    // ======================================================================
    // Public entry points
    // ======================================================================

    /**
     * Installs the appropriate class-specific combat goal for a freshly generated
     * spell list. Used during first-time spawn initialization.
     */
    public static void applyForClass(AbstractMercenaryEntity entity, List<AbstractSpell> spells) {
        EntityClass entityClass = entity.getEntityClass();
        switch (entityClass) {
            case MAGE -> setMageGoal(entity, spells);
            case WARRIOR -> setWarriorGoal(entity, spells);
            case ROGUE -> {
                if (entity.isArcher()) setArcherGoal(entity, spells);
                else setAssassinGoal(entity, spells);
            }
        }
    }

    /**
     * Re-applies goals using the entity's persisted spell list. Used after
     * equipment changes to re-evaluate the combat kit.
     */
    public static void reapplyWithPersistedSpells(AbstractMercenaryEntity entity) {
        clearAttackGoals(entity);
        applyForClass(entity, entity.getPersistedSpells());
    }

    /**
     * Called on world load. Uses persisted spells if available, otherwise
     * regenerates a fresh spell list for already-initialized entities that lost theirs.
     */
    public static void reinitializeAfterLoad(AbstractMercenaryEntity entity) {
        if (entity.areSpellsGenerated() && !entity.getPersistedSpells().isEmpty()) {
            clearAttackGoals(entity);
            applyForClass(entity, entity.getPersistedSpells());
        } else if (entity.isInitialized()) {
            MagicRealms.LOGGER.warn("Entity {} was initialized but has no spells, regenerating...",
                    entity.getEntityName());
            generateAndApplySpells(entity);
        }
    }

    /**
     * Generates a new spell list for this entity, persists it, and installs
     * the class-specific combat goal. Used as a recovery path when persisted
     * spells were lost.
     */
    public static void generateAndApplySpells(AbstractMercenaryEntity entity) {
        RandomSource randomSource = entity.level().getRandom();
        List<AbstractSpell> spells = SpellListGenerator.generateSpellsForEntity(entity, randomSource);
        entity.setPersistedSpells(new ArrayList<>(spells));
        entity.setSpellsGenerated(true);
        applyForClass(entity, spells);
    }

    /**
     * Re-evaluates the mage's combat goal when the equipped spellbook or
     * spell-containing gear changes.
     */
    public static void refreshAfterEquipmentChange(AbstractMercenaryEntity entity) {
        if (!entity.areSpellsGenerated() || entity.getPersistedSpells().isEmpty()) return;

        EntityClass entityClass = entity.getEntityClass();
        switch (entityClass) {
            case MAGE -> updateMageGoalWithSpellbookAndEquipment(entity, entity.getSpellbookSpells());
            case WARRIOR -> setWarriorGoal(entity, entity.getPersistedSpells());
            case ROGUE -> {
                if (entity.isArcher()) setArcherGoal(entity, entity.getPersistedSpells());
                else setAssassinGoal(entity, entity.getPersistedSpells());
            }
        }
    }

    /**
     * Updates the mage goal to reflect a newly-equipped spellbook. Combines the
     * mage's persisted spells, the spellbook's spells, and any other equipment
     * spells into a single deduplicated kit.
     */
    public static void updateMageGoalWithSpellbookAndEquipment(
            AbstractMercenaryEntity entity, List<AbstractSpell> spellbookSpells) {

        List<AbstractSpell> combinedSpells = new ArrayList<>(entity.getPersistedSpells());
        combinedSpells.addAll(spellbookSpells);
        combinedSpells.addAll(extractSpellsFromEquipment(entity));

        List<AbstractSpell> uniqueSpells = new ArrayList<>();
        for (AbstractSpell spell : combinedSpells) {
            if (!uniqueSpells.contains(spell) && !ModTags.isSpellInTag(spell, ModTags.SPELL_BLACKLIST)) {
                uniqueSpells.add(spell);
            }
        }

        entity.goalSelector.removeAllGoals(g -> g instanceof HumanGoals.HumanWizardAttackGoal);

        if (!uniqueSpells.isEmpty()) {
            SpellBuckets buckets = bucketize(uniqueSpells);
            entity.goalSelector.addGoal(2, new HumanGoals.HumanWizardAttackGoal(entity, 1.25f, 25, 50)
                    .setSpells(buckets.attack, buckets.defense, buckets.movement, buckets.support)
                    .setDrinksPotions()
            );
        } else {
            setMageGoal(entity, entity.getPersistedSpells());
        }
    }

    // ======================================================================
    // Equipment spell extraction
    // ======================================================================

    public static List<AbstractSpell> extractSpellsFromEquipment(AbstractMercenaryEntity entity) {
        List<AbstractSpell> equipmentSpells = new ArrayList<>();

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack equipment = entity.getItemBySlot(slot);
            if (!equipment.isEmpty()) {
                equipmentSpells.addAll(extractSpellsFromItem(equipment));
            }
        }

        for (int i = 0; i < entity.getInventory().getContainerSize(); i++) {
            ItemStack item = entity.getInventory().getItem(i);
            if (!item.isEmpty() && ISpellContainer.isSpellContainer(item)) {
                equipmentSpells.addAll(extractSpellsFromItem(item));
            }
        }

        return equipmentSpells;
    }

    private static List<AbstractSpell> extractSpellsFromItem(ItemStack item) {
        List<AbstractSpell> spells = new ArrayList<>();
        if (!ISpellContainer.isSpellContainer(item)) return spells;

        ISpellContainer container = ISpellContainer.get(item);
        if (container != null && !container.isEmpty()) {
            container.getActiveSpells().forEach(spellSlot -> {
                AbstractSpell spell = spellSlot.spellData().getSpell();
                if (!ModTags.isSpellInTag(spell, ModTags.SPELL_BLACKLIST)
                        && ModTags.isSchoolInTag(spell.getSchoolType(), ModTags.SCHOOL_WHITELIST)) {
                    spells.add(spell);
                }
            });
        }
        return spells;
    }

    // ======================================================================
    // Class-specific goal installers
    // ======================================================================

    private static void setMageGoal(AbstractMercenaryEntity entity, List<AbstractSpell> spells) {
        entity.goalSelector.removeAllGoals(g -> g instanceof HumanGoals.HumanWizardAttackGoal);

        List<AbstractSpell> finalSpells = new ArrayList<>(spells);
        if (entity.getEntityClass() == EntityClass.MAGE && !entity.getSpellbookSpells().isEmpty()) {
            finalSpells.addAll(entity.getSpellbookSpells());
        }
        finalSpells.addAll(extractSpellsFromEquipment(entity));

        SpellBuckets buckets = dedupAndBucketize(finalSpells);

        entity.goalSelector.addGoal(2, new MageCombatGoal(entity, entity.getBattlefield())
                .setSpells(buckets.attack, buckets.defense, buckets.movement, buckets.support)
                .setDrinksPotions()
        );
    }

    private static void setArcherGoal(AbstractMercenaryEntity entity, List<AbstractSpell> spells) {
        entity.goalSelector.removeAllGoals(g -> g instanceof HumanGoals.HumanWizardAttackGoal);
        entity.goalSelector.removeAllGoals(g -> g instanceof ChargeArrowAttackGoal);

        entity.goalSelector.addGoal(2,
                new SniperArcherCombatGoal<AbstractMercenaryEntity>(entity, entity.getBattlefield(), 1.0D, 20));

        List<AbstractSpell> finalSpells = new ArrayList<>(spells);
        finalSpells.addAll(extractSpellsFromEquipment(entity));

        SpellBuckets buckets = dedupAndBucketize(finalSpells);

        entity.goalSelector.addGoal(3, new HumanGoals.HumanWizardAttackGoal(entity, 1.0f, 60, 120)
                .setSpells(buckets.attack, buckets.defense, buckets.movement, buckets.support)
                .setDrinksPotions()
        );
    }

    private static void setAssassinGoal(AbstractMercenaryEntity entity, List<AbstractSpell> spells) {
        entity.goalSelector.removeAllGoals(g -> g instanceof GenericAnimatedWarlockAttackGoal);

        List<AbstractSpell> finalSpells = new ArrayList<>(spells);
        finalSpells.addAll(extractSpellsFromEquipment(entity));

        SpellBuckets buckets = dedupAndBucketize(finalSpells);

        entity.goalSelector.addGoal(3, new SkirmisherCombatGoal(entity, entity.getBattlefield(), true)
                .setMoveset(SWORD_MOVESET)
                .setComboChance(.7f)                    // highest combo chance of any class
                .setMeleeAttackInverval(8, 18)          // fastest cadence
                .setMeleeMovespeedModifier(2.0f)        // assassins are the fastest
                .setSpells(buckets.attack, buckets.defense, buckets.movement, buckets.support)
                .setDrinksPotions()
        );
    }

    private static void setWarriorGoal(AbstractMercenaryEntity entity, List<AbstractSpell> spells) {
        entity.goalSelector.removeAllGoals(g ->
                g instanceof GenericAnimatedWarlockAttackGoal || g instanceof ShieldBashGoal);

        List<AbstractSpell> finalSpells = new ArrayList<>(spells);
        finalSpells.addAll(extractSpellsFromEquipment(entity));

        SpellBuckets buckets = dedupAndBucketize(finalSpells);

        if (entity.hasShield()) {
            List<AbstractSpell> tankMovement = ShieldTankCombatGoal.filterMovementForTank(buckets.movement);

            entity.goalSelector.addGoal(3, new ShieldTankCombatGoal(entity, entity.getBattlefield())
                    .setMoveset(SWORD_MOVESET)
                    .setComboChance(.4f)
                    .setMeleeAttackInverval(20, 40)
                    .setMeleeMovespeedModifier(1.3f)
                    .setSpells(buckets.attack, buckets.defense, tankMovement, buckets.support)
                    .setDrinksPotions()
            );

            entity.goalSelector.addGoal(2, new ShieldBashGoal(entity, entity.getBattlefield()));
        } else {
            // No-shield warrior — BRAWLER / frontline contender.
            // Aggressive melee pressure, periodically creating a small 4-block tactical
            // pocket to drink a potion or finish a non-attack cast, then reengaging.
            List<AbstractSpell> brawlerMovement = ShieldTankCombatGoal.filterMovementForTank(buckets.movement);

            entity.goalSelector.addGoal(3, new BrawlerCombatGoal(entity, entity.getBattlefield())
                    .setMoveset(SWORD_MOVESET)
                    .setComboChance(.5f)                 // between tank (.4) and assassin (.7)
                    .setMeleeAttackInverval(14, 28)      // aggressive but not assassin-fast
                    .setMeleeMovespeedModifier(1.45f)    // between tank (1.3) and assassin (2.0)
                    .setSpells(buckets.attack, buckets.defense, brawlerMovement, buckets.support)
                    .setDrinksPotions()
            );
        }
    }

    // ======================================================================
    // Goal removal
    // ======================================================================

    public static void clearAttackGoals(AbstractMercenaryEntity entity) {
        entity.goalSelector.removeAllGoals(goal ->
                goal instanceof HumanGoals.HumanWizardAttackGoal ||
                        goal instanceof RangedBowAttackGoal ||
                        goal instanceof ChargeArrowAttackGoal ||
                        goal instanceof GenericAnimatedWarlockAttackGoal ||
                        goal instanceof SniperArcherCombatGoal ||
                        goal instanceof ShieldBashGoal
        );
    }

    // ======================================================================
    // Internal helpers
    // ======================================================================

    /** Shared sword moveset used by all melee classes. */
    private static final List<AttackAnimationData> SWORD_MOVESET = List.of(
            new AttackAnimationData(9, "simple_sword_upward_swipe", 5),
            new AttackAnimationData(8, "simple_sword_lunge_stab", 6),
            new AttackAnimationData(10, "simple_sword_stab_alternate", 8),
            new AttackAnimationData(10, "simple_sword_horizontal_cross_swipe", 8)
    );

    /** Groups spells by tactical role. */
    private record SpellBuckets(
            List<AbstractSpell> attack,
            List<AbstractSpell> defense,
            List<AbstractSpell> movement,
            List<AbstractSpell> support) {}

    private static SpellBuckets bucketize(List<AbstractSpell> spells) {
        List<AbstractSpell> attack = ModTags.filterAttackSpells(spells);
        List<AbstractSpell> defense = ModTags.filterDefenseSpells(spells);
        List<AbstractSpell> movement = ModTags.filterMovementSpells(spells);
        List<AbstractSpell> support = ModTags.filterSupportSpells(spells);
        return new SpellBuckets(attack, defense, movement, support);
    }

    /**
     * Deduplicates, filters blacklist, bucketizes by role, and re-adds any
     * unclassified spells into the attack bucket if the config allows.
     */
    private static SpellBuckets dedupAndBucketize(List<AbstractSpell> spells) {
        List<AbstractSpell> finalSpells = spells.stream()
                .filter(spell -> !ModTags.isSpellInTag(spell, ModTags.SPELL_BLACKLIST))
                .distinct()
                .collect(Collectors.toList());

        List<AbstractSpell> attack = ModTags.filterAttackSpells(finalSpells);
        List<AbstractSpell> defense = ModTags.filterDefenseSpells(finalSpells);
        List<AbstractSpell> movement = ModTags.filterMovementSpells(finalSpells);
        List<AbstractSpell> support = ModTags.filterSupportSpells(finalSpells);

        // Whatever's left is "unclassified"; treat as attack if config permits.
        finalSpells.removeIf(spell ->
                attack.contains(spell) || defense.contains(spell)
                        || movement.contains(spell) || support.contains(spell));

        List<AbstractSpell> finalAttack = new ArrayList<>(attack);
        if (!finalSpells.isEmpty() && Config.attemptCastUnclassifiedSpells) {
            finalAttack.addAll(finalSpells);
        }

        return new SpellBuckets(finalAttack, defense, movement, support);
    }
}
