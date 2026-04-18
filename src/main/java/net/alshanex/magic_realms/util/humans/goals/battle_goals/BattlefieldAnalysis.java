package net.alshanex.magic_realms.util.humans.goals.battle_goals;

import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.util.ModTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared battlefield analysis utility used by every class-specific combat goal.
 *
 * <p>A {@code BattlefieldAnalysis} is a snapshot of the combat situation around a mercenary:
 * who the hostiles are, where they cluster, who is squishy, who is a boss, and where
 * friendly frontliners are positioned. Each class's combat goal uses the snapshot to
 * pick its stance (frontline tank, skirmisher, backline caster, sniper).
 */
public class BattlefieldAnalysis {

    /** How far out to scan for hostiles and allies. */
    public static final double SCAN_RADIUS = 24.0;

    /** Two hostiles within this distance of each other count as part of the same cluster. */
    public static final double CLUSTER_RADIUS = 5.0;

    /** A "horde" is at least this many hostiles clustered together. */
    public static final int HORDE_THRESHOLD = 4;

    /** A target with health fraction below this is considered "low health" (finisher priority). */
    public static final float LOW_HEALTH_THRESHOLD = 0.35f;

    private final AbstractMercenaryEntity self;

    private final List<LivingEntity> hostiles = new ArrayList<>();
    private final List<AbstractMercenaryEntity> alliedMercs = new ArrayList<>();
    private final List<AbstractMercenaryEntity> shieldTanks = new ArrayList<>();
    private final List<Cluster> clusters = new ArrayList<>();

    private LivingEntity bossTarget = null;
    private Vec3 largestClusterCenter = null;
    private int largestClusterSize = 0;
    private Vec3 frontlineCenter = null;
    private boolean isHordeEngagement = false;
    private boolean isBossEngagement = false;

    private int lastRefreshTick = -9999;

    public BattlefieldAnalysis(AbstractMercenaryEntity self) {
        this.self = self;
    }

    /**
     * Refreshes the analysis if the cached snapshot is older than {@code maxAgeTicks}.
     * Call this at the start of a goal's tick.
     */
    public void refreshIfStale(int maxAgeTicks) {
        if (self.tickCount - lastRefreshTick >= maxAgeTicks) {
            refresh();
        }
    }

    /** Forces an immediate re-scan. */
    public void refresh() {
        lastRefreshTick = self.tickCount;
        hostiles.clear();
        alliedMercs.clear();
        shieldTanks.clear();
        clusters.clear();
        bossTarget = null;
        largestClusterCenter = null;
        largestClusterSize = 0;
        frontlineCenter = null;
        isHordeEngagement = false;
        isBossEngagement = false;

        AABB scanArea = self.getBoundingBox().inflate(SCAN_RADIUS);
        List<LivingEntity> nearby = self.level().getEntitiesOfClass(LivingEntity.class, scanArea,
                e -> e != self && e.isAlive());

        for (LivingEntity e : nearby) {
            if (self.isAlliedTo(e)) {
                if (e instanceof AbstractMercenaryEntity merc) {
                    alliedMercs.add(merc);
                    if (merc.hasShield()) {
                        shieldTanks.add(merc);
                    }
                }
            } else if (isHostile(e)) {
                hostiles.add(e);
                if (ModTags.BOSSES_TAG != null
                        && e.getType().is(ModTags.BOSSES_TAG)) {
                    if (bossTarget == null || e.getMaxHealth() > bossTarget.getMaxHealth()) {
                        bossTarget = e;
                    }
                }
            }
        }

        isBossEngagement = bossTarget != null;

        // Cluster hostiles by simple greedy grouping.
        computeClusters();

        // Identify the biggest cluster center for horde routing decisions.
        for (Cluster c : clusters) {
            if (c.size() > largestClusterSize) {
                largestClusterSize = c.size();
                largestClusterCenter = c.center();
            }
        }

        isHordeEngagement = hostiles.size() >= HORDE_THRESHOLD;

        // Frontline center = average position of shield tanks if any, else of all allied mercs.
        if (!shieldTanks.isEmpty()) {
            frontlineCenter = averagePosition(shieldTanks);
        } else if (!alliedMercs.isEmpty()) {
            frontlineCenter = averagePosition(alliedMercs);
        }
    }

    private boolean isHostile(LivingEntity e) {
        if (!(e instanceof Mob mob)) {
            // Players, etc.
            return e instanceof Enemy || (self.getTarget() != null && self.getTarget() == e);
        }
        return mob instanceof Enemy
                || (mob.getTarget() != null && self.isAlliedTo(mob.getTarget()))
                || mob.getTarget() == self;
    }

    private void computeClusters() {
        List<LivingEntity> remaining = new ArrayList<>(hostiles);
        while (!remaining.isEmpty()) {
            LivingEntity seed = remaining.remove(0);
            Cluster c = new Cluster();
            c.members.add(seed);
            // Include any hostile within CLUSTER_RADIUS of any existing cluster member.
            boolean grew;
            do {
                grew = false;
                var iter = remaining.iterator();
                while (iter.hasNext()) {
                    LivingEntity candidate = iter.next();
                    for (LivingEntity m : c.members) {
                        if (candidate.distanceToSqr(m) <= CLUSTER_RADIUS * CLUSTER_RADIUS) {
                            c.members.add(candidate);
                            iter.remove();
                            grew = true;
                            break;
                        }
                    }
                }
            } while (grew);
            clusters.add(c);
        }
    }

    private static Vec3 averagePosition(List<? extends LivingEntity> entities) {
        double x = 0, y = 0, z = 0;
        for (LivingEntity e : entities) {
            x += e.getX();
            y += e.getY();
            z += e.getZ();
        }
        int n = entities.size();
        return new Vec3(x / n, y / n, z / n);
    }

    // ================= PUBLIC QUERY API =================

    public List<LivingEntity> hostiles() { return hostiles; }
    public List<AbstractMercenaryEntity> alliedMercs() { return alliedMercs; }
    public List<AbstractMercenaryEntity> shieldTanks() { return shieldTanks; }
    public List<Cluster> clusters() { return clusters; }
    public boolean isHordeEngagement() { return isHordeEngagement; }
    public boolean isBossEngagement() { return isBossEngagement; }

    /** The biggest clump of enemies, or {@code null} if no enemies were seen. */
    public Vec3 largestClusterCenter() { return largestClusterCenter; }
    public int largestClusterSize() { return largestClusterSize; }

    /** Average position of friendly shield tanks (or all allied mercs if there are no tanks). */
    public Vec3 frontlineCenter() { return frontlineCenter; }

    public LivingEntity bossTarget() { return bossTarget; }

    /**
     * Returns the weakest hostile (lowest health fraction) within {@code maxRange}.
     * Used by archers and assassins for finisher target selection.
     */
    public LivingEntity findLowestHealthTarget(double maxRange) {
        LivingEntity best = null;
        float bestFrac = Float.MAX_VALUE;
        double maxRangeSq = maxRange * maxRange;
        for (LivingEntity e : hostiles) {
            if (e.distanceToSqr(self) > maxRangeSq) continue;
            float frac = e.getHealth() / e.getMaxHealth();
            if (frac < bestFrac) {
                bestFrac = frac;
                best = e;
            }
        }
        return best;
    }

    /**
     * Returns the most isolated hostile within {@code maxRange} — fewest allies within
     * {@link #CLUSTER_RADIUS}. This is the assassin's preferred target: a straggler.
     */
    public LivingEntity findMostIsolatedTarget(double maxRange) {
        LivingEntity best = null;
        int bestNeighbors = Integer.MAX_VALUE;
        double maxRangeSq = maxRange * maxRange;
        double neighborSq = CLUSTER_RADIUS * CLUSTER_RADIUS;
        for (LivingEntity e : hostiles) {
            if (e.distanceToSqr(self) > maxRangeSq) continue;
            int neighbors = 0;
            for (LivingEntity other : hostiles) {
                if (other == e) continue;
                if (other.distanceToSqr(e) <= neighborSq) neighbors++;
            }
            if (neighbors < bestNeighbors) {
                bestNeighbors = neighbors;
                best = e;
            }
        }
        return best;
    }

    /**
     * Returns the smallest cluster's center (assassin pick — go where the horde is thin).
     */
    public Vec3 smallestClusterCenter() {
        Cluster smallest = null;
        for (Cluster c : clusters) {
            if (smallest == null || c.size() < smallest.size()) {
                smallest = c;
            }
        }
        return smallest == null ? null : smallest.center();
    }

    /** A single connected group of hostiles, suitable for AoE targeting. */
    public static class Cluster {
        public final List<LivingEntity> members = new ArrayList<>();
        public int size() { return members.size(); }
        public Vec3 center() { return averagePosition(members); }
        /** Health-weighted score — high-HP clusters are juicier mage AoE targets. */
        public double aoeValue() {
            double total = 0;
            for (LivingEntity m : members) total += m.getHealth();
            return total;
        }
    }
}
