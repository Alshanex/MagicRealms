package net.alshanex.magic_realms.util.humans.goals.battle_goals;

import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.util.humans.mercenaries.EntityClass;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.phys.Vec3;

/**
 * Re-picks the mercenary's target according to its class role every ~20 ticks.
 * <p>Role targeting rules:
 * <ul>
 *   <li><b>Shield warrior</b>: closest hostile threatening the frontline.</li>
 *   <li><b>Skirmisher</b> (no-shield warrior, assassin): in hordes, isolated low-health targets; bosses always override.</li>
 *   <li><b>Archer</b>: low-health finishers at maximum range.</li>
 *   <li><b>Mage</b>: in hordes, the biggest cluster by AoE value.</li>
 * </ul>
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
        // Boss fights override everything.
        if (battlefield.isBossEngagement() && battlefield.bossTarget() != null) {
            return battlefield.bossTarget();
        }

        EntityClass cls = self.getEntityClass();
        boolean isArcher = self.isArcher();
        boolean hasShield = self.hasShield();

        // Shield warrior: closest threat to the frontline center.
        if (cls == EntityClass.WARRIOR && hasShield) {
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

        // Skirmishers: isolated low-health targets in hordes only.
        if ((cls == EntityClass.WARRIOR && !hasShield)
                || (cls == EntityClass.ROGUE && !isArcher)) {
            if (battlefield.isHordeEngagement()) {
                LivingEntity isolated = battlefield.findMostIsolatedTarget(BattlefieldAnalysis.SCAN_RADIUS);
                if (isolated != null) {
                    LivingEntity current = self.getTarget();
                    // Only retarget if the pick is notably better — prevents thrashing.
                    if (current == null || isolated.getHealth() < current.getHealth() * 0.8f) {
                        return isolated;
                    }
                }
            }
            return null;
        }

        // Archer: lowest-HP target at range.
        if (cls == EntityClass.ROGUE && isArcher) {
            LivingEntity low = battlefield.findLowestHealthTarget(20.0);
            if (low != null) {
                LivingEntity current = self.getTarget();
                if (current == null
                        || low.getHealth() / low.getMaxHealth()
                        < current.getHealth() / current.getMaxHealth() - 0.15f) {
                    return low;
                }
            }
            return null;
        }

        // Mage: biggest cluster's center (approximated by closest cluster member).
        if (cls == EntityClass.MAGE) {
            if (battlefield.isHordeEngagement() && !battlefield.clusters().isEmpty()) {
                BattlefieldAnalysis.Cluster biggest = null;
                double bestValue = -1;
                for (BattlefieldAnalysis.Cluster c : battlefield.clusters()) {
                    double v = c.aoeValue();
                    if (v > bestValue) { bestValue = v; biggest = c; }
                }
                if (biggest != null && !biggest.members.isEmpty()) {
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
            return null;
        }

        return null;
    }
}