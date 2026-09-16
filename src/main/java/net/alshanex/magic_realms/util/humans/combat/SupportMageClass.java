package net.alshanex.magic_realms.util.humans.combat;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.entity.mobs.goals.FindSupportableTargetGoal;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.humans.AbstractMercenaryEntity;
import net.alshanex.magic_realms.util.ModTags;
import net.alshanex.magic_realms.util.humans.goals.HumanGoals;
import net.alshanex.magic_realms.util.humans.goals.MercenaryGoalManager;
import net.alshanex.magic_realms.util.humans.goals.battle_goals.BattlefieldAnalysis;
import net.alshanex.magic_realms.util.humans.goals.battle_goals.MercenarySupportGoal;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class SupportMageClass extends MageClass {

    /** Ally-heal spells guaranteed at each star level (1-3). */
    private static final int[] GUARANTEED_HEALS = { 1, 1, 2 };

    /** Ally-buff spells guaranteed at each star level (1-3). */
    private static final int[] GUARANTEED_BUFFS = { 0, 1, 1 };

    /** Heal an ally once it drops below this fraction of max health. */
    private static final float SUPPORT_THRESHOLD = 0.8f;

    public SupportMageClass() {
        super("support_mage");
    }

    /** Reuse the full mage skin pool - no new textures needed. */
    @Override
    public String skinCategory() {
        return "mage";
    }

    /** Rarer than the three core classes. */
    @Override
    public int spawnWeight() {
        return 5;
    }

    @Override
    public ItemStack symbolItem(AbstractMercenaryEntity e) {
        return new ItemStack(ItemRegistry.PRIEST_HELMET);
    }

    @Override
    public void applyAttributes(AbstractMercenaryEntity e, int stars, RandomSource rng) {
        super.applyAttributes(e, stars, rng);

        addModifier(e, Attributes.MAX_HEALTH, "support_bulk", 6.0, AttributeModifier.Operation.ADD_VALUE);
        e.heal(e.getMaxHealth());

        addModifier(e, AttributeRegistry.COOLDOWN_REDUCTION, "support_cdr",
                starValue(stars, 5, 10, 8, 14, 12, 20, rng) / 100.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

        addModifier(e, AttributeRegistry.CASTING_MOVESPEED, "support_casting_movespeed",
                starValue(stars, 5, 10, 8, 15, 12, 20, rng) / 100.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    /**
     * Normal mage roll, then top up with ally-targeted spells so the class can actually function.
     *
     * <p>Ally spells prefer schools the mercenary already rolled, so the kit stays thematically coherent.
     * If those schools offer none, the pick falls back to the whole tag - a support mage with an off-school heal is better than one with no heal at all.
     */
    @Override
    public List<AbstractSpell> generateSpells(AbstractMercenaryEntity e, RandomSource rng) {
        List<AbstractSpell> spells = new ArrayList<>(super.generateSpells(e, rng));

        int starIndex = Mth.clamp(e.getStarLevel(), 1, 3) - 1;

        List<AbstractSpell> added = new ArrayList<>();
        added.addAll(addFromTag(spells, ModTags.HEAL_ALLIES, GUARANTEED_HEALS[starIndex], e, rng));
        added.addAll(addFromTag(spells, ModTags.BUFF_ALLIES, GUARANTEED_BUFFS[starIndex], e, rng));

        adoptSchoolsOf(e, added, rng);

        if (ModTags.filterSpellsByTag(spells, ModTags.HEAL_ALLIES).isEmpty()) {
            MagicRealms.LOGGER.warn(
                    "Support mage {} generated no ally-heal spells — is the {} tag populated?",
                    e.getEntityName(), ModTags.HEAL_ALLIES.location());
        }

        return spells;
    }

    /**
     * Fold the schools of any newly-granted spell into the mercenary's school list, and grant the matching initial spell-power bonus for each
     * school that's new.
     */
    private void adoptSchoolsOf(AbstractMercenaryEntity e, List<AbstractSpell> added, RandomSource rng) {
        if (added.isEmpty()) return;

        List<SchoolType> current = new ArrayList<>(e.getMagicSchools());
        List<SchoolType> newSchools = new ArrayList<>();

        for (AbstractSpell spell : added) {
            SchoolType school = spell.getSchoolType();
            if (school == null) continue;
            if (current.contains(school) || newSchools.contains(school)) continue;
            if (!ModTags.isSchoolInTag(school, ModTags.SCHOOL_WHITELIST)) continue;
            newSchools.add(school);
        }

        if (newSchools.isEmpty()) return;

        current.addAll(newSchools);
        e.setMagicSchools(current);

        for (SchoolType school : newSchools) {
            applySchoolBonus(e, school, rng);
        }

        MagicRealms.LOGGER.debug("Support mage {} adopted {} school(s) from its ally spells: {}",
                e.getEntityName(), newSchools.size(),
                newSchools.stream().map(s -> s.getId().toString()).toList());
    }

    /**
     * Add up to {@code count} distinct spells carrying {@code tag}, preferring the mercenary's own schools.
     *
     * @return the spells actually added, so the caller can adopt their schools
     */
    private List<AbstractSpell> addFromTag(List<AbstractSpell> target, TagKey<AbstractSpell> tag,
                                           int count, AbstractMercenaryEntity e, RandomSource rng) {
        List<AbstractSpell> added = new ArrayList<>();
        if (count <= 0) return added;

        List<AbstractSpell> tagged = candidatesFromTag(tag);
        if (tagged.isEmpty()) return added;

        List<SchoolType> schools = e.getMagicSchools();

        // On-school candidates first, then everything else in the tag as filler.
        List<AbstractSpell> pool = new ArrayList<>();
        for (AbstractSpell s : tagged) {
            if (schools.contains(s.getSchoolType())) pool.add(s);
        }
        for (AbstractSpell s : tagged) {
            if (!pool.contains(s)) pool.add(s);
        }

        Collections.shuffle(pool, new Random(rng.nextLong()));

        for (AbstractSpell s : pool) {
            if (added.size() >= count) break;
            if (target.contains(s)) continue;
            target.add(s);
            added.add(s);
        }
        return added;
    }

    /** Every enabled, non-blacklisted, whitelisted-school spell carrying this tag. */
    private List<AbstractSpell> candidatesFromTag(TagKey<AbstractSpell> tag) {
        List<AbstractSpell> out = new ArrayList<>();
        for (AbstractSpell spell : SpellRegistry.getEnabledSpells()) {
            if (ModTags.isSpellInTag(spell, ModTags.SPELL_BLACKLIST)) continue;
            if (!ModTags.isSchoolInTag(spell.getSchoolType(), ModTags.SCHOOL_WHITELIST)) continue;
            if (!ModTags.isSpellInTag(spell, tag)) continue;
            out.add(spell);
        }
        return out;
    }

    @Override
    public void installCombatGoals(AbstractMercenaryEntity e, List<AbstractSpell> spells) {
        clearOwnGoals(e); // clears attack goals AND the aux selector

        List<AbstractSpell> all = new ArrayList<>(spells);
        all.addAll(e.getSpellbookSpells());
        all.addAll(MercenaryGoalManager.extractSpellsFromEquipment(e));

        // Split ally-targeted spells out before bucketizing, so the attack goal never fires a heal at an enemy.
        List<AbstractSpell> healAllies = ModTags.filterSpellsByTag(all, ModTags.HEAL_ALLIES);
        List<AbstractSpell> buffAllies = ModTags.filterSpellsByTag(all, ModTags.BUFF_ALLIES);

        List<AbstractSpell> combatSpells = new ArrayList<>(all);
        combatSpells.removeAll(healAllies);
        combatSpells.removeAll(buffAllies);

        var buckets = MercenaryGoalManager.dedupAndBucketize(combatSpells);

        // Priority 2: keeping allies alive pre-empts attacking.
        if (!healAllies.isEmpty() || !buffAllies.isEmpty()) {
            e.goalSelector.addGoal(2, new MercenarySupportGoal(e, 1.25f, 20, 40)
                    .setSpells(healAllies, buffAllies)
                    .setSpellQuality(0.35f, 0.65f));

            // The aux selector decides who to support; the main target selector is busy picking enemies.
            e.getAuxTargetSelector().addGoal(0,
                    new FindSupportableTargetGoal<>(e, LivingEntity.class, true,
                            candidate -> isSupportableAlly(e, candidate)));
        }

        // Priority 3: ordinary offence, at a slower cadence than a damage mage.
        e.goalSelector.addGoal(3, new HumanGoals.HumanWizardAttackGoal(e, 1.0f, 40, 80)
                .setSpells(buckets.attack(), buckets.defense(), buckets.movement(), buckets.support())
                .setDrinksPotions());
    }

    /**
     * MageClass reinstalls only its own attack goal on equipment change, which would silently drop the support goal and the aux target goal.
     * Full reinstall instead.
     */
    @Override
    public void onEquipmentChanged(AbstractMercenaryEntity e) {
        installCombatGoals(e, e.getPersistedSpells());
    }

    /**
     * Who this mercenary will heal: its contractor, or an allied mercenary, below {@link #SUPPORT_THRESHOLD}.
     */
    private static boolean isSupportableAlly(AbstractMercenaryEntity self, LivingEntity candidate) {
        if (candidate == self || !candidate.isAlive()) return false;
        if (candidate.getHealth() >= candidate.getMaxHealth() * SUPPORT_THRESHOLD) return false;
        if (self.isHostileTowards(candidate)) return false;

        if (candidate instanceof Player) {
            return candidate == self.getSummoner();
        }
        return candidate instanceof AbstractMercenaryEntity && self.isAlliedTo(candidate);
    }

    @Override
    public LivingEntity selectPreferredTarget(AbstractMercenaryEntity self, BattlefieldAnalysis battlefield) {
        return null;
    }
}
