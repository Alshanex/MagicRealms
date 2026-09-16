package net.alshanex.magic_realms.util.humans.combat;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.entity.mobs.wizards.GenericAnimatedWarlockAttackGoal;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.alshanex.magic_realms.entity.humans.AbstractMercenaryEntity;
import net.alshanex.magic_realms.util.ModTags;
import net.alshanex.magic_realms.util.humans.goals.ChargeArrowAttackGoal;
import net.alshanex.magic_realms.util.humans.goals.HumanGoals;
import net.alshanex.magic_realms.util.humans.goals.MercenaryGoalManager;
import net.alshanex.magic_realms.util.humans.goals.battle_goals.BattlefieldAnalysis;
import net.alshanex.magic_realms.util.humans.goals.battle_goals.SkirmisherCombatGoal;
import net.alshanex.magic_realms.util.humans.goals.battle_goals.SniperArcherCombatGoal;
import net.alshanex.magic_realms.util.humans.mercenaries.SpellListGenerator;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class RogueClass extends AbstractCombatClass {

    public RogueClass() { super("rogue"); }

    @Override public int spawnWeight() { return 10; }

    @Override public boolean canGainSpellPower() { return true; }

    @Override public String roleName(ClassLoadout l) { return l.isArcher() ? "archer" : "rogue"; }
    @Override public boolean prefersMelee(ClassLoadout l) { return !l.isArcher(); }
    @Override public boolean prefersRangedWeapons(ClassLoadout l) { return l.isArcher(); }

    @Override
    public WeaponPreference weaponPreference(ClassLoadout l) {
        return l.isArcher() ? WeaponPreference.RAW_DAMAGE : WeaponPreference.DPS;
    }

    @Override
    public boolean acceptsMainHand(ItemStack stack, ClassLoadout l) {
        if (l.isArcher()) {
            return stack.getItem() instanceof BowItem || stack.is(ModTags.BOWS);
        }
        return isMeleeWeapon(stack);
    }

    @Override
    public Component displayName(AbstractMercenaryEntity e) {
        return Component.translatable(e.isArcher()
                ? "gui.magic_realms.human_info.class.archer"
                : "gui.magic_realms.human_info.class.assassin");
    }

    @Override
    public ItemStack symbolItem(AbstractMercenaryEntity e) {
        return new ItemStack(e.isArcher() ? Items.BOW : ItemRegistry.WEAPON_PARTS.get());
    }

    @Override
    public void initializeSpecifics(AbstractMercenaryEntity e, RandomSource rng) {
        if (!e.isExclusiveMercenary()) {
            e.setIsArcher(rng.nextFloat() < 0.25f);
        }
    }

    @Override
    public void applyAttributes(AbstractMercenaryEntity e, int stars, RandomSource rng) {
        addModifier(e, Attributes.MAX_HEALTH, "health", 10.0, Operation.ADD_VALUE);
        e.heal(e.getMaxHealth());
    }

    @Override
    public void applyStartingEquipment(AbstractMercenaryEntity e) {
        if (e.isArcher()) {
            e.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            e.getInventory().addItem(new ItemStack(Items.ARROW, 64));
        } else {
            e.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
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
        return 0.0;
    }

    @Override
    public List<AbstractSpell> generateSpells(AbstractMercenaryEntity e, RandomSource rng) {
        return SpellListGenerator.generateRogueSpells(e, e.getStarLevel(), rng);
    }

    @Override
    public void installCombatGoals(AbstractMercenaryEntity e, List<AbstractSpell> spells) {
        if (e.isArcher()) installArcherGoals(e, spells);
        else              installAssassinGoals(e, spells);
    }

    private void installArcherGoals(AbstractMercenaryEntity e, List<AbstractSpell> spells) {
        e.goalSelector.removeAllGoals(g -> g instanceof HumanGoals.HumanWizardAttackGoal);
        e.goalSelector.removeAllGoals(g -> g instanceof ChargeArrowAttackGoal);

        e.goalSelector.addGoal(2,
                new SniperArcherCombatGoal<AbstractMercenaryEntity>(e, e.getBattlefield(), 1.0D, 20));

        List<AbstractSpell> finalSpells = new ArrayList<>(spells);
        finalSpells.addAll(MercenaryGoalManager.extractSpellsFromEquipment(e));

        var buckets = MercenaryGoalManager.dedupAndBucketize(finalSpells);

        e.goalSelector.addGoal(3, new HumanGoals.HumanWizardAttackGoal(e, 1.0f, 60, 120)
                .setSpells(buckets.attack(), buckets.defense(), buckets.movement(), buckets.support())
                .setDrinksPotions());
    }

    private void installAssassinGoals(AbstractMercenaryEntity e, List<AbstractSpell> spells) {
        e.goalSelector.removeAllGoals(g -> g instanceof GenericAnimatedWarlockAttackGoal);

        List<AbstractSpell> finalSpells = new ArrayList<>(spells);
        finalSpells.addAll(MercenaryGoalManager.extractSpellsFromEquipment(e));

        var buckets = MercenaryGoalManager.dedupAndBucketize(finalSpells);

        e.goalSelector.addGoal(3, new SkirmisherCombatGoal(e, e.getBattlefield(), true)
                .setMoveset(MercenaryGoalManager.SWORD_MOVESET)
                .setComboChance(.7f)
                .setMeleeAttackInverval(8, 18)
                .setMeleeMovespeedModifier(2.0f)
                .setSpells(buckets.attack(), buckets.defense(), buckets.movement(), buckets.support())
                .setDrinksPotions());
    }

    @Override
    public void onEquipmentChanged(AbstractMercenaryEntity e) {
        installCombatGoals(e, e.getPersistedSpells());
    }

    @Override
    public LivingEntity selectPreferredTarget(AbstractMercenaryEntity self, BattlefieldAnalysis battlefield) {
        if (!self.isArcher()) {
            return TargetingStrategies.skirmisher(self, battlefield);
        }

        // Archer: lowest-HP target at range, but only if notably better than the current one.
        LivingEntity low = battlefield.findLowestHealthTarget(20.0);
        if (low == null) return null;

        LivingEntity current = self.getTarget();
        if (current == null
                || low.getHealth() / low.getMaxHealth()
                < current.getHealth() / current.getMaxHealth() - 0.15f) {
            return low;
        }
        return null;
    }
}
