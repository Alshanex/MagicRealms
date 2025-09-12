package net.alshanex.magic_realms.effects;

import io.redspace.ironsspellbooks.effect.MagicMobEffect;
import net.alshanex.magic_realms.entity.RandomHumanEntity;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;

public class StunEffect extends MagicMobEffect {
    public StunEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF6B6B); // Red color for stun
    }

    @Override
    public boolean applyEffectTick(LivingEntity livingEntity, int amplifier) {
        if (livingEntity instanceof Mob mob) {
            // Clear target every tick
            mob.setTarget(null);

            // Stop all movement and navigation
            mob.getNavigation().stop();
            mob.setDeltaMovement(0, mob.getDeltaMovement().y * 0.1, 0);

            // Disable all goals by stopping them
            for (WrappedGoal wrappedGoal : mob.goalSelector.getAvailableGoals()) {
                Goal goal = wrappedGoal.getGoal();
                if (goal.isInterruptable() && wrappedGoal.isRunning()) {
                    goal.stop();
                }
            }

            // Also disable targeting goals
            for (WrappedGoal wrappedGoal : mob.targetSelector.getAvailableGoals()) {
                Goal goal = wrappedGoal.getGoal();
                if (goal.isInterruptable() && wrappedGoal.isRunning()) {
                    goal.stop();
                }
            }
        }
        return super.applyEffectTick(livingEntity, amplifier);
    }

    @Override
    public void onEffectRemoved(LivingEntity pLivingEntity, int pAmplifier) {
        if(pLivingEntity instanceof RandomHumanEntity human){
            human.setStunned(false);
        }
        super.onEffectRemoved(pLivingEntity, pAmplifier);
    }

    @Override
    public boolean isInstantenous() {
        return false;
    }
}
