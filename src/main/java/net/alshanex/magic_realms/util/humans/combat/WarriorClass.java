package net.alshanex.magic_realms.util.humans.combat;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.entity.mobs.wizards.GenericAnimatedWarlockAttackGoal;
import net.alshanex.magic_realms.entity.humans.AbstractMercenaryEntity;
import net.alshanex.magic_realms.util.humans.goals.MercenaryGoalManager;
import net.alshanex.magic_realms.util.humans.goals.battle_goals.BattlefieldAnalysis;
import net.alshanex.magic_realms.util.humans.goals.battle_goals.BrawlerCombatGoal;
import net.alshanex.magic_realms.util.humans.goals.battle_goals.ShieldBashGoal;
import net.alshanex.magic_realms.util.humans.goals.battle_goals.ShieldTankCombatGoal;
import net.alshanex.magic_realms.util.humans.mercenaries.SpellListGenerator;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class WarriorClass extends AbstractCombatClass {

    public WarriorClass() { super("warrior"); }

    @Override public int spawnWeight() { return 10; }

    @Override public boolean canGainSpellResist() { return true; }

    @Override public String roleName(ClassLoadout l) { return l.hasShield() ? "tank" : "warrior"; }
    @Override public boolean prefersMelee(ClassLoadout l) { return true; }
    @Override public boolean wantsShield(ClassLoadout l) { return l.hasShield(); }

    @Override
    public boolean acceptsMainHand(ItemStack stack, ClassLoadout l) {
        return isMeleeWeapon(stack)
                || stack.getItem() instanceof AxeItem
                || stack.getItem() instanceof TridentItem
                || stack.getItem() instanceof MaceItem;
    }

    @Override
    public boolean acceptsOffHand(ItemStack stack, ClassLoadout l) {
        return l.hasShield() && stack.getItem() instanceof ShieldItem;
    }

    /** Owns the shield variant so no separate "tank" class is needed. */
    @Override
    public Component displayName(AbstractMercenaryEntity e) {
        return Component.translatable(e.hasShield()
                ? "gui.magic_realms.human_info.class.warrior_shield"
                : "gui.magic_realms.human_info.class.warrior");
    }

    @Override
    public ItemStack symbolItem(AbstractMercenaryEntity e) {
        return new ItemStack(e.hasShield() ? Items.SHIELD : Items.IRON_SWORD);
    }

    @Override
    public void initializeSpecifics(AbstractMercenaryEntity e, RandomSource rng) {
        e.setHasShield(rng.nextFloat() < 0.25f);
    }

    @Override
    public void applyAttributes(AbstractMercenaryEntity e, int stars, RandomSource rng) {
        addModifier(e, Attributes.MAX_HEALTH, "health", 20.0, Operation.ADD_VALUE);
        e.heal(e.getMaxHealth());
    }

    @Override
    public void applyStartingEquipment(AbstractMercenaryEntity e) {
        e.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WOODEN_SWORD));
        if (e.hasShield()) {
            e.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        }
    }

    @Override
    public double spellPowerBonus(int stars, RandomSource rng) {
        return 0.0;
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
        return SpellListGenerator.generateWarriorSpells(e.getStarLevel(), rng);
    }

    @Override
    public void installCombatGoals(AbstractMercenaryEntity e, List<AbstractSpell> spells) {
        e.goalSelector.removeAllGoals(g ->
                g instanceof GenericAnimatedWarlockAttackGoal || g instanceof ShieldBashGoal);

        List<AbstractSpell> finalSpells = new ArrayList<>(spells);
        finalSpells.addAll(MercenaryGoalManager.extractSpellsFromEquipment(e));

        var buckets = MercenaryGoalManager.dedupAndBucketize(finalSpells);

        if (e.hasShield()) {
            List<AbstractSpell> tankMovement =
                    ShieldTankCombatGoal.filterMovementForTank(buckets.movement());

            e.goalSelector.addGoal(3, new ShieldTankCombatGoal(e, e.getBattlefield())
                    .setMoveset(MercenaryGoalManager.SWORD_MOVESET)
                    .setComboChance(.4f)
                    .setMeleeAttackInverval(20, 40)
                    .setMeleeMovespeedModifier(1.3f)
                    .setSpells(buckets.attack(), buckets.defense(), tankMovement, buckets.support())
                    .setDrinksPotions());

            e.goalSelector.addGoal(2, new ShieldBashGoal(e, e.getBattlefield()));
        } else {
            List<AbstractSpell> brawlerMovement =
                    ShieldTankCombatGoal.filterMovementForTank(buckets.movement());

            e.goalSelector.addGoal(3, new BrawlerCombatGoal(e, e.getBattlefield())
                    .setMoveset(MercenaryGoalManager.SWORD_MOVESET)
                    .setComboChance(.5f)
                    .setMeleeAttackInverval(14, 28)
                    .setMeleeMovespeedModifier(1.45f)
                    .setSpells(buckets.attack(), buckets.defense(), brawlerMovement, buckets.support())
                    .setDrinksPotions());
        }
    }

    @Override
    public void onEquipmentChanged(AbstractMercenaryEntity e) {
        installCombatGoals(e, e.getPersistedSpells());
    }

    @Override
    public LivingEntity selectPreferredTarget(AbstractMercenaryEntity self, BattlefieldAnalysis battlefield) {
        if (self.hasShield()) {
            Vec3 anchor = battlefield.frontlineCenter();
            if (anchor == null) anchor = self.position();

            LivingEntity closest = null;
            double bestSq = Double.MAX_VALUE;
            for (LivingEntity e : battlefield.hostiles()) {
                double sq = e.distanceToSqr(anchor.x, anchor.y, anchor.z);
                if (sq < bestSq) { bestSq = sq; closest = e; }
            }
            return closest;
        }
        return TargetingStrategies.skirmisher(self, battlefield);
    }
}
