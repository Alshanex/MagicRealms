package net.alshanex.magic_realms.util.humans.goals.battle_goals;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.util.ModTags;
import net.alshanex.magic_realms.util.humans.goals.HumanGoals;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Combat goal for mages.
 *
 * <p>Role: <b>backline AoE damage dealer</b>. Three requirements:
 * <ol>
 *   <li>Never let hostiles reach melee range — kite below MIN_SAFE_RANGE.</li>
 *   <li>Prefer AoE spells when 2+ hostiles are clustered near the target.</li>
 *   <li>Balance attack and kiting: don't sit casting when a threat is closing.</li>
 * </ol>
 */
public class MageCombatGoal extends HumanGoals.HumanWizardAttackGoal {

    /** Below this distance a hostile is dangerously close; flee at full speed. */
    public static final float MIN_SAFE_RANGE = 8.0f;

    /** Start kiting as soon as a threat is closer than this. */
    public static final float KITE_TRIGGER_RANGE = 11.0f;

    /** Ideal engagement distance from the target. */
    public static final float OPTIMAL_RANGE = 14.0f;

    /** Beyond this distance we need to approach to maintain effectiveness. */
    public static final float MAX_COMBAT_RANGE = 20.0f;

    private final AbstractMercenaryEntity mage;
    private final BattlefieldAnalysis battlefield;

    private Vec3 kitePos = null;
    private int kiteRefreshTimer = 0;

    public MageCombatGoal(AbstractMercenaryEntity mage, BattlefieldAnalysis battlefield) {
        super(mage, 1.3f, 25, 50);
        this.mage = mage;
        this.battlefield = battlefield;
        this.spellcastingRange = MAX_COMBAT_RANGE;
        this.spellcastingRangeSqr = MAX_COMBAT_RANGE * MAX_COMBAT_RANGE;
        this.allowFleeing = false; // we handle fleeing ourselves; don't let super trigger it
    }

    @Override
    public void tick() {
        battlefield.refreshIfStale(10);
        super.tick();
    }

    @Override
    protected void doMovement(double distanceSquared) {
        if (target == null) return;

        double nearestThreatSq = getNearestThreatDistSq();
        double distanceToTarget = Math.sqrt(distanceSquared);

        mob.lookAt(target, 30, 30);

        double baseSpeed = speedModifier;
        double castPenalty = spellCastingMob.isCasting() ? 0.75 : 1.0;

        // PRIORITY 1: urgent kite — anything within MIN_SAFE_RANGE flees at full speed.
        if (nearestThreatSq < MIN_SAFE_RANGE * MIN_SAFE_RANGE) {
            doKite(baseSpeed * 1.6);
            return;
        }

        // PRIORITY 2: early-warning kite — threats approaching.
        if (nearestThreatSq < KITE_TRIGGER_RANGE * KITE_TRIGGER_RANGE) {
            doKite(baseSpeed * castPenalty * 1.3);
            return;
        }

        // PRIORITY 3: too far — approach to re-engage.
        if (distanceToTarget > MAX_COMBAT_RANGE || !hasLineOfSight) {
            kitePos = null;
            if (mob.tickCount % 5 == 0) {
                mob.getNavigation().moveTo(target, baseSpeed * castPenalty);
            }
            return;
        }

        // PRIORITY 4: safe range — stop and strafe for unpredictable movement.
        kitePos = null;
        mob.getNavigation().stop();

        if (++strafeTime > 25) {
            if (mob.getRandom().nextDouble() < .2) {
                strafingClockwise = !strafingClockwise;
                strafeTime = 0;
            }
        }
        // Slight backward bias when target is closer than OPTIMAL_RANGE.
        float strafeForward = distanceToTarget < OPTIMAL_RANGE ? -0.35f : 0.1f;
        int strafeDir = strafingClockwise ? 1 : -1;
        double strafeSpeed = baseSpeed * castPenalty;
        mob.getMoveControl().strafe(strafeForward * (float) strafeSpeed,
                (float) strafeSpeed * strafeDir);
    }

    private void doKite(double speed) {
        boolean needNewPath = kitePos == null
                || --kiteRefreshTimer <= 0
                || mob.position().distanceToSqr(kitePos) < 2.0 * 2.0
                || !mob.getNavigation().isInProgress();

        if (needNewPath) {
            // Flee from the nearest threat (not necessarily the target).
            LivingEntity nearestThreat = findNearestThreat();
            Vec3 fleeFrom = nearestThreat != null ? nearestThreat.position() : target.position();
            Vec3 flee = DefaultRandomPos.getPosAway(mob, 14, 6, fleeFrom);
            if (flee != null) {
                kitePos = flee;
                kiteRefreshTimer = 20;
            }
        }
        if (kitePos != null) {
            mob.getNavigation().moveTo(kitePos.x, kitePos.y, kitePos.z, speed);
        }
    }

    /**
     * Cancel active casts when a threat enters MIN_SAFE_RANGE; suppress new casts while actively kiting.
     */
    @Override
    protected void handleAttackLogic(double distanceSquared) {
        double nearestSq = getNearestThreatDistSq();

        if (nearestSq < MIN_SAFE_RANGE * MIN_SAFE_RANGE) {
            if (spellCastingMob.isCasting()) spellCastingMob.cancelCast();
            if (spellAttackDelay <= 1) spellAttackDelay = 10;
            return;
        }

        // Suppress new casts while early-kiting; let existing casts finish.
        if (nearestSq < KITE_TRIGGER_RANGE * KITE_TRIGGER_RANGE
                && !spellCastingMob.isCasting()) {
            if (spellAttackDelay <= 1) spellAttackDelay = 5;
            return;
        }

        super.handleAttackLogic(distanceSquared);
    }

    // =========== ATTACK SPELL FILTERING ===========

    /**
     * Override the parent's distance-range-then-AoE filter. The parent bases its "use AoE?" decision on entities within 3 blocks of the TARGET, which often
     * misses real horde scenarios. We check battlefield cluster data instead: if a cluster of 2+ hostiles exists near the target, force AoE selection.
     */
    @Override
    protected ArrayList<AbstractSpell> getFilteredAttackSpells() {
        if (target == null) return new ArrayList<>(attackSpells);

        double distance = Math.sqrt(mob.distanceToSqr(target));

        List<AbstractSpell> rangeSpells = pickRangeSpells(distance);
        if (rangeSpells.isEmpty()) rangeSpells = new ArrayList<>(attackSpells);

        boolean prefersAoe = shouldUseAoe();

        List<AbstractSpell> finalSpells;
        if (prefersAoe) {
            finalSpells = filterByTag(rangeSpells, ModTags.AOE_ATTACKS);
            if (finalSpells.isEmpty()) {
                finalSpells = filterByTag(rangeSpells, ModTags.SINGLE_TARGET_ATTACKS);
            }
        } else {
            finalSpells = filterByTag(rangeSpells, ModTags.SINGLE_TARGET_ATTACKS);
            if (finalSpells.isEmpty()) {
                finalSpells = filterByTag(rangeSpells, ModTags.AOE_ATTACKS);
            }
        }

        return new ArrayList<>(finalSpells.isEmpty() ? rangeSpells : finalSpells);
    }

    /** True when there's a real cluster worth AoE-ing, based on battlefield scan. */
    private boolean shouldUseAoe() {
        Vec3 targetPos = target.position();
        double aoeRangeSq = 8.0 * 8.0;
        for (BattlefieldAnalysis.Cluster c : battlefield.clusters()) {
            if (c.size() >= 2) {
                Vec3 center = c.center();
                if (center.distanceToSqr(targetPos) <= aoeRangeSq) return true;
            }
        }
        return false;
    }

    private List<AbstractSpell> pickRangeSpells(double distance) {
        List<AbstractSpell> out;
        if (distance <= 3) {
            out = filterByTag(attackSpells, ModTags.CLOSE_RANGE_ATTACKS);
            if (out.isEmpty()) out = filterByTag(attackSpells, ModTags.MID_RANGE_ATTACKS);
            if (out.isEmpty()) out = filterByTag(attackSpells, ModTags.LONG_RANGE_ATTACKS);
        } else if (distance <= 6) {
            out = filterByTag(attackSpells, ModTags.MID_RANGE_ATTACKS);
            if (out.isEmpty()) out = filterByTag(attackSpells, ModTags.LONG_RANGE_ATTACKS);
            if (out.isEmpty()) out = filterByTag(attackSpells, ModTags.CLOSE_RANGE_ATTACKS);
        } else {
            out = filterByTag(attackSpells, ModTags.LONG_RANGE_ATTACKS);
            if (out.isEmpty()) out = filterByTag(attackSpells, ModTags.MID_RANGE_ATTACKS);
            if (out.isEmpty()) out = filterByTag(attackSpells, ModTags.CLOSE_RANGE_ATTACKS);
        }
        return out;
    }

    private static List<AbstractSpell> filterByTag(List<AbstractSpell> spells, TagKey<AbstractSpell> tag) {
        List<AbstractSpell> out = new ArrayList<>();
        for (AbstractSpell s : spells) {
            if (ModTags.isSpellInTag(s, tag)) out.add(s);
        }
        return out;
    }

    // =========== WEIGHT OVERRIDES ===========

    @Override
    protected int getAttackWeight() {
        return (int) (super.getAttackWeight() * 1.4f);
    }

    @Override
    protected int getMovementWeight() {
        // Heavy bias toward retreat-movement spells when threats are closing.
        if (getNearestThreatDistSq() < KITE_TRIGGER_RANGE * KITE_TRIGGER_RANGE) {
            return (int) (super.getMovementWeight() * 2.5f);
        }
        return super.getMovementWeight();
    }

    @Override
    protected int getDefenseWeight() {
        return (int) (super.getDefenseWeight() * 0.7f);
    }

    // =========== HELPERS ===========

    private double getNearestThreatDistSq() {
        double best = Double.MAX_VALUE;
        for (LivingEntity e : battlefield.hostiles()) {
            double sq = e.distanceToSqr(mob);
            if (sq < best) best = sq;
        }
        return best == Double.MAX_VALUE ? 10000.0 : best;
    }

    private LivingEntity findNearestThreat() {
        LivingEntity best = null;
        double bestSq = Double.MAX_VALUE;
        for (LivingEntity e : battlefield.hostiles()) {
            double sq = e.distanceToSqr(mob);
            if (sq < bestSq) { bestSq = sq; best = e; }
        }
        return best;
    }

    @Override
    public void stop() {
        super.stop();
        kitePos = null;
        kiteRefreshTimer = 0;
    }
}
