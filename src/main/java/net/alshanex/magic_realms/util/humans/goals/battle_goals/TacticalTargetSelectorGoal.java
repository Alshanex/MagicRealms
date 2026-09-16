package net.alshanex.magic_realms.util.humans.goals.battle_goals;

import net.alshanex.magic_realms.entity.humans.AbstractMercenaryEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;

/**
 * Re-picks the mercenary's target every ~20 ticks.
 *
 * <p>Boss engagements override everything. Otherwise the refinement is delegated to the entity's
 * {@code CombatClass.selectPreferredTarget}, so a new class defines its own targeting without this file changing.
 * Returning null from that method leaves the current target alone.
 */
public class TacticalTargetSelectorGoal extends TargetGoal {

    private static final int REPICK_INTERVAL = 20;

    private final AbstractMercenaryEntity self;
    private final BattlefieldAnalysis battlefield;
    private int cooldown;
    private LivingEntity newTarget;

    public TacticalTargetSelectorGoal(AbstractMercenaryEntity self, BattlefieldAnalysis battlefield) {
        // mustSee = false: we only refine an existing target, so line-of-sight
        //   should already have been validated by whichever goal acquired the
        //   original target.
        // mustReach = false: same reasoning.
        super(self, false, false);
        this.self = self;
        this.battlefield = battlefield;
    }

    @Override
    public boolean canUse() {
        if (self.isStunned() || self.isSittingInChair() || self.isInMenuState()) return false;
        if (self.getTarget() == null) return false;
        if (--cooldown > 0) return false;

        battlefield.refreshIfStale(10);
        LivingEntity candidate = pickTarget();

        // Only "activate" if we actually want to change the target.
        if (candidate == null
                || candidate == self.getTarget()
                || !candidate.isAlive()
                || self.isAlliedTo(candidate)) {
            cooldown = REPICK_INTERVAL;
            return false;
        }

        this.newTarget = candidate;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        // One-shot per activation — start() sets the target and we're done.
        return false;
    }

    @Override
    public void start() {
        self.setTarget(newTarget);
        cooldown = REPICK_INTERVAL;
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
        newTarget = null;
    }

    private LivingEntity pickTarget() {
        // Boss fights override everything, regardless of class.
        if (battlefield.isBossEngagement() && battlefield.bossTarget() != null) {
            return battlefield.bossTarget();
        }
        return self.getCombatClass().selectPreferredTarget(self, battlefield);
    }
}