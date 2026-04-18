package net.alshanex.magic_realms.util.humans.goals.battle_goals;

import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.util.ModTags;
import net.alshanex.magic_realms.util.humans.goals.ChargeArrowAttackGoal;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Combat goal for rogue archers.
 *
 * <p>Role: <b>long-range sniper</b>. Top priority: maintain distance. The archer
 * should almost never get caught in melee.
 * <p>The bow charge/release logic is kept minimal: start using the bow when we
 * can see the target and the attack cooldown has elapsed, release after 20 ticks
 * (full charge), reset.
 */
public class SniperArcherCombatGoal<T extends AbstractMercenaryEntity & RangedAttackMob> extends Goal {

    /**
     * "Prefer to kite" threshold. When a threat is within this range, the archer prefers backing up — but does NOT cancel an in-progress draw or
     * block a release. This gives the archer a buffer window: it can finish the current shot even as the enemy closes, and only breaks contact
     * below {@link #CANCEL_DRAW_RANGE}. Without this gap, the archer would start a draw at 10 blocks, the enemy would step to 9.9, draw cancels,
     * kite starts, enemy still at 9.9 (can't outrun in one tick), draw never completes — the archer gets stuck and dies.
     */
    public static final float KITE_PREFER_RANGE = 11.0f;

    /**
     * "Hard cancel" threshold. Below this, the archer MUST break the draw and kite — no shot completion is worth getting melee'd. Should be
     * noticeably smaller than {@link #KITE_PREFER_RANGE} so there's a real window for the archer to finish shots while slowly backing off.
     */
    public static final float CANCEL_DRAW_RANGE = 8.0f;

    /**
     * Full-speed panic kite. Below this the archer runs flat-out and does
     * not attempt to aim or draw.
     */
    public static final float URGENT_KITE_RANGE = 6.0f;

    /** Maximum range at which we can reliably engage. */
    public static final float MAX_RANGE = 22.0f;

    private final T archer;
    private final BattlefieldAnalysis battlefield;
    private final double speedModifier;
    private final int attackIntervalMin;

    private int attackCooldown = -1;
    private int seeTime = 0;

    private Vec3 kitePos = null;
    private int kiteRefreshTimer = 0;

    public SniperArcherCombatGoal(T archer, BattlefieldAnalysis battlefield,
                                  double speedModifier, int attackInterval) {
        this.archer = archer;
        this.battlefield = battlefield;
        this.speedModifier = speedModifier;
        this.attackIntervalMin = attackInterval;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (archer.isStunned() || archer.isSittingInChair() || archer.isInMenuState()) return false;
        return archer.getTarget() != null && isHoldingBow() && archer.canPerformRangedAttack();
    }

    @Override
    public boolean canContinueToUse() {
        return (canUse() || !archer.getNavigation().isDone())
                && isHoldingBow()
                && archer.canPerformRangedAttack();
    }

    @Override
    public void start() {
        super.start();
        archer.setAggressive(true);
    }

    @Override
    public void stop() {
        super.stop();
        archer.setAggressive(false);
        archer.stopUsingItem();
        attackCooldown = -1;
        seeTime = 0;
        kitePos = null;
        archer.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void tick() {
        battlefield.refreshIfStale(10);

        LivingEntity target = archer.getTarget();
        if (target == null) return;

        double distToTargetSq = archer.distanceToSqr(target);
        double nearestThreatSq = getNearestThreatDistSq();
        boolean canSee = archer.getSensing().hasLineOfSight(target);

        if (canSee) seeTime = Math.max(1, seeTime + 1);
        else seeTime = Math.min(-1, seeTime - 1);

        // Classify the threat situation once so the rest of the method uses the same view. Thresholds (widest → narrowest):
        //   KITE_PREFER_RANGE  — bias toward backing up; do NOT cancel draws.
        //   CANCEL_DRAW_RANGE  — hard cancel any in-progress draw; kite.
        //   URGENT_KITE_RANGE  — panic kite at full speed, no aim attempted.
        boolean panicKite = nearestThreatSq < URGENT_KITE_RANGE * URGENT_KITE_RANGE;
        boolean hardKite = nearestThreatSq < CANCEL_DRAW_RANGE * CANCEL_DRAW_RANGE;
        boolean softKite = nearestThreatSq < KITE_PREFER_RANGE * KITE_PREFER_RANGE;
        boolean tooFar = distToTargetSq > MAX_RANGE * MAX_RANGE || !canSee;

        // ---- Movement decision ----
        if (panicKite) {
            // Full panic — cancel draw, sprint away.
            if (archer.isUsingItem()) archer.stopUsingItem();
            doKite(1.8);
        } else if (hardKite) {
            // Threat is inside the hard-cancel zone. Break the draw and kite.
            if (archer.isUsingItem()) archer.stopUsingItem();
            doKite(1.5);
        } else if (softKite) {
            // Threat is approaching but not close enough to abandon the shot. Move backward at moderate speed, but DO NOT cancel an existing
            // draw — this is the buffer window that lets the archer finish the current shot before being forced to actually run.
            //
            // If we aren't currently drawing, don't start a new one either (startUsingItem is gated further down); just back up.
            doKite(1.1);
        } else if (tooFar) {
            // Need to approach to regain LOS / range.
            if (archer.isUsingItem()) archer.stopUsingItem();
            kitePos = null;
            if (archer.tickCount % 5 == 0) {
                archer.getNavigation().moveTo(target, speedModifier);
            }
        } else {
            // Sweet spot — hold position. Dead still for accuracy.
            kitePos = null;
            archer.getNavigation().stop();
            archer.getMoveControl().strafe(0f, 0f);
        }

        // ---- Look at target ----
        // Fast rotation (60°/tick) so aim keeps up with moving targets.
        archer.getLookControl().setLookAt(
                target.getX(), target.getEyeY(), target.getZ(),
                60.0F, 60.0F);

        // ---- Bow charge/release ----
        if (archer.isUsingItem()) {
            if (!canSee && seeTime < -60) {
                archer.stopUsingItem();
            } else if (canSee) {
                int useTime = archer.getTicksUsingItem();
                if (useTime == 1) {
                    archer.playSound(SoundEvents.CROSSBOW_LOADING_START.value(), 0.7F, 1.0F);
                }
                if (useTime >= 20) {
                    // Full charge — ready to release. Release condition:
                    //   1. Not in panic/hard kite (already cancelled above).
                    //   2. Either stationary, OR in the soft-kite window.
                    //
                    // Releasing during soft-kite is acceptable: the archer is backing up slowly and we want the shot to land
                    // before the enemy gets into hard-kite range. Accuracy will be slightly reduced by the backward motion
                    boolean canRelease = softKite
                            || (archer.getNavigation().isDone()
                            && archer.getDeltaMovement().horizontalDistanceSqr() < 0.02);

                    if (canRelease) {
                        // Snap body yaw to the target for the arrow spawn.
                        double dx = target.getX() - archer.getX();
                        double dz = target.getZ() - archer.getZ();
                        float yaw = (float)(net.minecraft.util.Mth.atan2(dz, dx)
                                * (180D / Math.PI)) - 90.0F;
                        archer.setYRot(yaw);
                        archer.yBodyRot = yaw;
                        archer.yHeadRot = yaw;

                        archer.stopUsingItem();
                        archer.performRangedAttack(target, BowItem.getPowerForTime(useTime));
                        attackCooldown = attackIntervalMin;
                    }
                    // Otherwise hold the draw at max and wait.
                }
            }
        } else if (--attackCooldown <= 0 && seeTime >= -60) {
            // Start drawing the next shot. Allowed if:
            //   - We're outside the hard-cancel zone (not hardKite/panicKite).
            //   - We're either stationary OR in the soft-kite window (where
            //     we're backing off slowly but still willing to aim).
            boolean canStartDraw = !hardKite && !panicKite
                    && (softKite || archer.getNavigation().isDone());

            if (canStartDraw) {
                archer.startUsingItem(InteractionHand.MAIN_HAND);
            }
        }
    }

    private void doKite(double speedMul) {
        boolean needNewPath = kitePos == null
                || --kiteRefreshTimer <= 0
                || archer.position().distanceToSqr(kitePos) < 2.0 * 2.0
                || !archer.getNavigation().isInProgress();

        if (needNewPath) {
            LivingEntity nearestThreat = findNearestThreat();
            LivingEntity target = archer.getTarget();
            Vec3 fleeFrom = nearestThreat != null ? nearestThreat.position()
                    : (target != null ? target.position() : archer.position());
            Vec3 flee = DefaultRandomPos.getPosAway(archer, 14, 5, fleeFrom);
            if (flee != null) {
                kitePos = flee;
                kiteRefreshTimer = 20;
            }
        }
        if (kitePos != null) {
            archer.getNavigation().moveTo(kitePos.x, kitePos.y, kitePos.z, speedModifier * speedMul);
        }
    }

    private boolean isHoldingBow() {
        return archer.isHolding(is -> is.getItem() instanceof BowItem || is.is(ModTags.BOWS));
    }

    private double getNearestThreatDistSq() {
        double best = Double.MAX_VALUE;
        for (LivingEntity e : battlefield.hostiles()) {
            double sq = e.distanceToSqr(archer);
            if (sq < best) best = sq;
        }
        return best == Double.MAX_VALUE ? 10000.0 : best;
    }

    private LivingEntity findNearestThreat() {
        LivingEntity best = null;
        double bestSq = Double.MAX_VALUE;
        for (LivingEntity e : battlefield.hostiles()) {
            double sq = e.distanceToSqr(archer);
            if (sq < bestSq) { bestSq = sq; best = e; }
        }
        return best;
    }
}
