package net.alshanex.magic_realms.util.humans.combat;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.item.SpellBook;
import io.redspace.ironsspellbooks.item.weapons.StaffItem;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.alshanex.magic_realms.entity.humans.AbstractMercenaryEntity;
import net.alshanex.magic_realms.util.ModTags;
import net.alshanex.magic_realms.util.humans.goals.HumanGoals;
import net.alshanex.magic_realms.util.humans.goals.MercenaryGoalManager;
import net.alshanex.magic_realms.util.humans.goals.battle_goals.BattlefieldAnalysis;
import net.alshanex.magic_realms.util.humans.goals.battle_goals.MageCombatGoal;
import net.alshanex.magic_realms.util.humans.mercenaries.SpellListGenerator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class MageClass extends AbstractCombatClass {

    public MageClass() { this("mage"); }

    /** For subclasses reusing mage behaviour under a different id (e.g. support_mage). */
    protected MageClass(String path) { super(path); }

    @Override public int spawnWeight() { return 10; }

    @Override
    public ItemStack symbolItem(AbstractMercenaryEntity e) {
        return new ItemStack(ItemRegistry.GOLD_SPELL_BOOK.get());
    }

    @Override public boolean usesSpellbooks() { return true; }

    @Override public boolean usesMagicSchools() { return true; }

    @Override
    public boolean usesStaves(ClassLoadout l) { return true; }

    @Override public boolean canGainSpellResist() { return true; }

    @Override public boolean canGainSpellPower() { return true; }

    @Override
    public boolean acceptsMainHand(ItemStack stack, ClassLoadout l) {
        return stack.getItem() instanceof StaffItem;
    }

    @Override
    public boolean acceptsOffHand(ItemStack stack, ClassLoadout l) {
        return stack.getItem() instanceof SpellBook;
    }

    @Override
    public ResourceLocation emptyOffHandIcon(ClassLoadout l) {
        return ResourceLocation.fromNamespaceAndPath("curios", "slot/spellbook_slot");
    }

    @Override
    public void initializeSpecifics(AbstractMercenaryEntity e, RandomSource rng) {
        e.setMagicSchools(generateMagicSchools(rng));
    }

    protected List<SchoolType> generateMagicSchools(RandomSource random) {
        double roll = random.nextDouble();
        int schoolCount = roll < 0.65 ? 1 : roll < 0.85 ? 2 : roll < 0.95 ? 3 : 4;

        List<SchoolType> availableSchools = SchoolRegistry.REGISTRY.stream()
                .filter(school -> ModTags.isSchoolInTag(school, ModTags.SCHOOL_WHITELIST))
                .toList();

        List<SchoolType> selected = new ArrayList<>();
        List<SchoolType> temp = new ArrayList<>(availableSchools);
        while (selected.size() < schoolCount && !temp.isEmpty()) {
            selected.add(temp.remove(random.nextInt(temp.size())));
        }
        return selected;
    }

    @Override
    public void applyAttributes(AbstractMercenaryEntity e, int stars, RandomSource rng) {
        addModifier(e, Attributes.MAX_HEALTH, "health", 15.0, AttributeModifier.Operation.ADD_VALUE);
        e.heal(e.getMaxHealth());

        addModifier(e, AttributeRegistry.MAX_MANA, "bonus_mana",
                starValue(stars, 10, 50, 30, 70, 50, 100, rng), AttributeModifier.Operation.ADD_VALUE);

        addModifier(e, AttributeRegistry.MANA_REGEN, "mana_regen",
                starValue(stars, 0, 10, 5, 15, 10, 20, rng) / 100.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

        addModifier(e, AttributeRegistry.COOLDOWN_REDUCTION, "cooldown_reduction",
                starValue(stars, 0, 10, 5, 15, 10, 20, rng) / 100.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

        addModifier(e, AttributeRegistry.CASTING_MOVESPEED, "casting_movespeed",
                starValue(stars, 0, 10, 5, 15, 10, 20, rng) / 100.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

        applySchoolBonuses(e, rng);

        double summonDamage = Math.round((rng.nextDouble() * 20.0)) / 100.0;
        addModifier(e, AttributeRegistry.SUMMON_DAMAGE, "summon_damage",
                summonDamage, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    protected void applySchoolBonuses(AbstractMercenaryEntity e, RandomSource rng) {
        for (SchoolType school : e.getMagicSchools()) {
            applySchoolBonus(e, school, rng);
        }
    }

    /**
     * Initial spell-power bonus for one school. Split out so subclasses that add a school after {@link #applyAttributes} has run can grant its bonus too.
     *
     * <p>Idempotent — the modifier id is derived from the class path and school, and {@code addModifier} removes before adding.
     */
        protected void applySchoolBonus(AbstractMercenaryEntity e, SchoolType school, RandomSource rng) {
            double bonus = Math.round((5.0 + rng.nextDouble() * 5.0) * 100.0) / 100.0;
                ResourceLocation powerAttrId = ResourceLocation.fromNamespaceAndPath(
                        school.getId().getNamespace(), school.getId().getPath() + "_spell_power");
                var holder = BuiltInRegistries.ATTRIBUTE.getHolder(powerAttrId).orElse(null);
        if (holder != null) {
            addModifier(e, holder, "initial_" + school.getId().getPath() + "_power",
                    +                    bonus / 100.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        }
        }

    @Override
    public double spellPowerBonus(int stars, RandomSource rng) {
        return switch (stars) {
            case 1 -> rng.nextBoolean() ? 2.0 : 3.0;
            case 2 -> rng.nextBoolean() ? 3.0 : 4.0;
            case 3 -> rng.nextBoolean() ? 4.0 : 5.0;
            default -> 2.0;
        };
    }

    @Override
    public double spellResistanceBonus(int stars, RandomSource rng) {
        return switch (stars) {
            case 1 -> rng.nextBoolean() ? 2.0 : 3.0;
            case 2 -> rng.nextBoolean() ? 3.0 : 4.0;
            case 3 -> rng.nextBoolean() ? 4.0 : 5.0;
            default -> 2.0;
        };
    }

    @Override
    public List<AbstractSpell> generateSpells(AbstractMercenaryEntity e, RandomSource rng) {
        return SpellListGenerator.generateMageSpells(e, e.getStarLevel(), rng);
    }

    @Override
    public void installCombatGoals(AbstractMercenaryEntity e, List<AbstractSpell> spells) {
        e.goalSelector.removeAllGoals(g -> g instanceof HumanGoals.HumanWizardAttackGoal);

        List<AbstractSpell> finalSpells = new ArrayList<>(spells);
        if (!e.getSpellbookSpells().isEmpty()) {
            finalSpells.addAll(e.getSpellbookSpells());
        }
        finalSpells.addAll(MercenaryGoalManager.extractSpellsFromEquipment(e));

        var buckets = MercenaryGoalManager.dedupAndBucketize(finalSpells);

        e.goalSelector.addGoal(2, new MageCombatGoal(e, e.getBattlefield())
                .setSpells(buckets.attack(), buckets.defense(), buckets.movement(), buckets.support())
                .setDrinksPotions());
    }

    @Override
    public void onEquipmentChanged(AbstractMercenaryEntity e) {
        List<AbstractSpell> combined = new ArrayList<>(e.getPersistedSpells());
        combined.addAll(e.getSpellbookSpells());
        combined.addAll(MercenaryGoalManager.extractSpellsFromEquipment(e));

        List<AbstractSpell> unique = new ArrayList<>();
        for (AbstractSpell s : combined) {
            if (!unique.contains(s) && !ModTags.isSpellInTag(s, ModTags.SPELL_BLACKLIST)) unique.add(s);
        }

        e.goalSelector.removeAllGoals(g -> g instanceof HumanGoals.HumanWizardAttackGoal);

        if (!unique.isEmpty()) {
            var buckets = MercenaryGoalManager.bucketize(unique);
            e.goalSelector.addGoal(2, new HumanGoals.HumanWizardAttackGoal(e, 1.25f, 25, 50)
                    .setSpells(buckets.attack(), buckets.defense(), buckets.movement(), buckets.support())
                    .setDrinksPotions());
        } else {
            installCombatGoals(e, e.getPersistedSpells());
        }
    }

    @Override
    public LivingEntity selectPreferredTarget(AbstractMercenaryEntity self,
                                              BattlefieldAnalysis battlefield) {
        if (!battlefield.isHordeEngagement() || battlefield.clusters().isEmpty()) return null;

        BattlefieldAnalysis.Cluster biggest = null;
        double bestValue = -1;
        for (BattlefieldAnalysis.Cluster c : battlefield.clusters()) {
            double v = c.aoeValue();
            if (v > bestValue) { bestValue = v; biggest = c; }
        }
        if (biggest == null || biggest.members.isEmpty()) return null;

        Vec3 center = biggest.center();
        LivingEntity best = null;
        double bestSq = Double.MAX_VALUE;
        for (LivingEntity m : biggest.members) {
            double sq = m.distanceToSqr(center.x, center.y, center.z);
            if (sq < bestSq) { bestSq = sq; best = m; }
        }
        return best;
    }
}
