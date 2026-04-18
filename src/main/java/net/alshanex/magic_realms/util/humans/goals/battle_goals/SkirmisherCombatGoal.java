package net.alshanex.magic_realms.util.humans.goals.battle_goals;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.entity.mobs.wizards.GenericAnimatedWarlockAttackGoal;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.util.ModTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Combat goal for warriors without shields and for rogue assassins.
 *
 * <p>Role: <b>skirmisher</b>. Hit-and-run: close distance fast, land a short combo,
 * then fully disengage to a safe distance and only re-approach when the cooldown
 * window closes.
 */
public class SkirmisherCombatGoal extends GenericAnimatedWarlockAttackGoal<AbstractMercenaryEntity> {

    public enum State { APPROACH, COMBO, DISENGAGE }

    private final AbstractMercenaryEntity merc;
    private final BattlefieldAnalysis battlefield;
    private final boolean isAssassin;

    private State state = State.APPROACH;
    private int stateTicks = 0;
    private int hitsInCombo = 0;
    private int approachCooldown = 0;

    /** Melee hits to land before switching to DISENGAGE. */
    private static final int COMBO_HITS = 3;

    /** Hard cap on COMBO time — switch even if hits aren't landing. */
    private static final int COMBO_MAX_TICKS = 40;

    /** Minimum DISENGAGE duration before we can even consider re-approaching. */
    private static final int DISENGAGE_MIN_TICKS = 30;

    /** Safety cap on DISENGAGE duration. */
    private static final int DISENGAGE_MAX_TICKS = 80;

    /** Distance we must reach before ending DISENGAGE. */
    private static final double DISENGAGE_SAFE_DIST = 14.0;

    /** Cooldown (ticks) before APPROACH is allowed again — ensures a real gap. */
    private static final int APPROACH_COOLDOWN_AFTER_DISENGAGE = 20;

    /** Distance considered "in melee range". */
    private static final double MELEE_DIST = 3.0;

    /** How far to flee on each disengage pathfind. */
    private static final int FLEE_PATH_DISTANCE = 16;
    private static final int FLEE_PATH_Y = 5;

    private Vec3 disengagePos = null;
    private int disengagePathTimer = 0;
    private int lastMobHurtTick = -1;

    private final List<AbstractSpell> canonicalAttackSpells = new ArrayList<>();

    public SkirmisherCombatGoal(AbstractMercenaryEntity merc, BattlefieldAnalysis battlefield, boolean isAssassin) {
        super(merc, isAssassin ? 1.6f : 1.35f, 50, 75);
        this.merc = merc;
        this.battlefield = battlefield;
        this.isAssassin = isAssassin;

        // Skirmishers are melee-first classes. Like the tank, set a high meleeBias so the parent's "melee vs spell" coinflip favors swinging.
        // Slightly lower than the tank because skirmishers use movement spells (approach / retreat) more often as part of the hit-and-run pattern.
        this.setMeleeBias(0.75f, 0.90f);
    }

    @Override
    public GenericAnimatedWarlockAttackGoal<AbstractMercenaryEntity> setSpells(
            List<AbstractSpell> attackSpells,
            List<AbstractSpell> defenseSpells,
            List<AbstractSpell> movementSpells,
            List<AbstractSpell> supportSpells) {
        // Delegate to parent so its internal lists are populated, then capture the canonical attack list for later range-based filtering.
        super.setSpells(attackSpells, defenseSpells, movementSpells, supportSpells);
        canonicalAttackSpells.clear();
        canonicalAttackSpells.addAll(attackSpells);
        return this;
    }

    /**
     * Replace the parent's {@code attackSpells} field with a subset whose range tags match the current distance to the target.
     * Called at the top of every tick before {@code super.tick()}.
     *
     * <p>Classification by distance-to-target:
     * <ul>
     *   <li>≤ 3 blocks: CLOSE_RANGE only (we're in melee range).</li>
     *   <li>3–6 blocks: MID_RANGE or CLOSE_RANGE.</li>
     *   <li>&gt; 6 blocks: MID_RANGE or LONG_RANGE (no close).</li>
     * </ul>
     */
    private void restrictAttackSpellsByRange(double distSq) {
        if (canonicalAttackSpells.isEmpty()) return;
        double dist = Math.sqrt(distSq);

        List<AbstractSpell> filtered = new ArrayList<>();
        if (dist <= 3.0) {
            // Inside melee range — close-range spells are fine.
            for (AbstractSpell s : canonicalAttackSpells) {
                if (ModTags.isSpellInTag(s, ModTags.CLOSE_RANGE_ATTACKS)) filtered.add(s);
            }
            // Also allow mid-range here (they work close too) to ensure a non-empty pool.
            for (AbstractSpell s : canonicalAttackSpells) {
                if (ModTags.isSpellInTag(s, ModTags.MID_RANGE_ATTACKS)) filtered.add(s);
            }
        } else if (dist <= 6.0) {
            // Medium distance — mid-range primary, close-range fallback.
            for (AbstractSpell s : canonicalAttackSpells) {
                if (ModTags.isSpellInTag(s, ModTags.MID_RANGE_ATTACKS)) filtered.add(s);
            }
            for (AbstractSpell s : canonicalAttackSpells) {
                if (ModTags.isSpellInTag(s, ModTags.CLOSE_RANGE_ATTACKS)) filtered.add(s);
            }
        } else {
            // Far — long and mid range only. Explicitly exclude close-range.
            for (AbstractSpell s : canonicalAttackSpells) {
                if (ModTags.isSpellInTag(s, ModTags.LONG_RANGE_ATTACKS)
                        || ModTags.isSpellInTag(s, ModTags.MID_RANGE_ATTACKS)) {
                    filtered.add(s);
                }
            }
        }

        // Fallback: if filtering produced nothing, use the full list so the weight system doesn't starve.
        // Better to cast a suboptimal spell than to skip the cast window entirely.
        if (filtered.isEmpty()) {
            filtered.addAll(canonicalAttackSpells);
        }

        attackSpells.clear();
        attackSpells.addAll(filtered);
    }

    private void restoreCanonicalAttackSpells() {
        attackSpells.clear();
        attackSpells.addAll(canonicalAttackSpells);
    }

    @Override
    public void start() {
        super.start();
        state = State.APPROACH;
        stateTicks = 0;
        hitsInCombo = 0;
        approachCooldown = 0;
    }

    @Override
    public void stop() {
        super.stop();
        disengagePos = null;
    }

    @Override
    public void tick() {
        battlefield.refreshIfStale(10);

        LivingEntity currentTarget = merc.getTarget();
        if (currentTarget == null) {
            super.tick();
            return;
        }

        stateTicks++;
        if (approachCooldown > 0) approachCooldown--;

        double distSq = merc.distanceToSqr(currentTarget);

        // Track hits landed this state.
        if (state == State.COMBO) {
            int lastHurt = currentTarget.getLastHurtByMobTimestamp();
            if (lastHurt != lastMobHurtTick
                    && currentTarget.getLastHurtByMob() == merc) {
                hitsInCombo++;
                lastMobHurtTick = lastHurt;
            }
        }

        // State transitions BEFORE movement/attack handling.
        handleStateTransitions(currentTarget, distSq);

        // Execute state-specific behavior.
        switch (state) {
            case APPROACH, COMBO -> {
                // Swap attackSpells for a distance-filtered subset so the parent doesn't pick close-range spells at long distance (where they do nothing).
                // Restored after the parent runs.
                restrictAttackSpellsByRange(distSq);
                try {
                    // Let the parent handle melee + spell combat normally.
                    super.tick();
                } finally {
                    restoreCanonicalAttackSpells();
                }
            }
            case DISENGAGE -> {
                // Full takeover: path away, no super.tick() (which would re-engage).
                driveDisengage(currentTarget);
            }
        }
    }

    private void handleStateTransitions(LivingEntity target, double distSq) {
        switch (state) {
            case APPROACH -> {
                if (distSq <= MELEE_DIST * MELEE_DIST) {
                    transition(State.COMBO);
                }
            }
            case COMBO -> {
                if (hitsInCombo >= COMBO_HITS || stateTicks >= COMBO_MAX_TICKS) {
                    planDisengagePath(target);
                    transition(State.DISENGAGE);
                }
            }
            case DISENGAGE -> {
                // Require BOTH a minimum time window AND reaching safe distance (or a timeout).
                boolean minTimeMet = stateTicks >= DISENGAGE_MIN_TICKS;
                boolean safeDistReached = distSq >= DISENGAGE_SAFE_DIST * DISENGAGE_SAFE_DIST;
                boolean timeout = stateTicks >= DISENGAGE_MAX_TICKS;

                if ((minTimeMet && safeDistReached) || timeout) {
                    disengagePos = null;
                    approachCooldown = APPROACH_COOLDOWN_AFTER_DISENGAGE;
                    transition(State.APPROACH);
                }
            }
        }
    }

    private void transition(State next) {
        state = next;
        stateTicks = 0;
        if (next == State.COMBO) {
            hitsInCombo = 0;
            lastMobHurtTick = -1;
        }
    }

    private void planDisengagePath(LivingEntity away) {
        // Directly away from the target, not a random tangent.
        Vec3 dir = merc.position().subtract(away.position());
        if (dir.lengthSqr() < 0.0001) {
            // Pathological: we're on top of the target. Pick an arbitrary direction and go.
            dir = new Vec3(1, 0, 0);
        }
        Vec3 directAway = merc.position().add(dir.normalize().scale(FLEE_PATH_DISTANCE));

        // Prefer the direct-away vector. Only fall back to DefaultRandomPos (which may pick tangential positions) if the direct path is
        // completely unreachable — checked by whether navigation can plan a path to it. This is a minor extra pathfind call but happens only on
        // disengage transitions, not every tick.
        this.disengagePos = directAway;

        // Longer refresh interval so we DON'T keep changing direction mid-flight. Only refresh if we arrive or get stuck.
        this.disengagePathTimer = 60;
    }

    /**
     * Drive the mob away during DISENGAGE. Completely takes over movement — no super.tick(), no strafe commands from the parent, no melee pathfinding.
     */
    private void driveDisengage(LivingEntity currentTarget) {
        boolean stuck = !merc.getNavigation().isInProgress()
                && disengagePos != null
                && merc.position().distanceToSqr(disengagePos) > 4.0;
        boolean arrived = disengagePos != null
                && merc.position().distanceToSqr(disengagePos) < 2.0 * 2.0;

        if (disengagePos == null || arrived || stuck || --disengagePathTimer <= 0) {
            planDisengagePath(currentTarget);
        }

        if (disengagePos != null) {
            boolean pathed = merc.getNavigation().moveTo(
                    disengagePos.x, disengagePos.y, disengagePos.z,
                    speedModifier * 1.5);

            // If the direct-away path is blocked (can't plan), fall back to DefaultRandomPos which picks a reachable nearby-ish flee point.
            if (!pathed) {
                Vec3 fallback = DefaultRandomPos.getPosAway(
                        merc, FLEE_PATH_DISTANCE, FLEE_PATH_Y, currentTarget.position());
                if (fallback != null) {
                    disengagePos = fallback;
                    merc.getNavigation().moveTo(fallback.x, fallback.y, fallback.z,
                            speedModifier * 1.5);
                }
            }
        }
    }

    // =========== WEIGHT OVERRIDES ===========

    @Override
    protected int getAttackWeight() {
        return switch (state) {
            case COMBO -> (int) (super.getAttackWeight() * 1.4f);
            case DISENGAGE -> 0; // never attack while disengaging
            case APPROACH -> super.getAttackWeight();
        };
    }

    @Override
    protected int getMovementWeight() {
        // Huge boost during APPROACH — skirmishers use approach spells to close the gap fast.
        float multiplier = switch (state) {
            case APPROACH -> 2.8f;
            case COMBO -> 0.3f;
            case DISENGAGE -> 0f; // we handle movement manually; don't waste casts
        };
        return (int) (super.getMovementWeight() * multiplier);
    }

    @Override
    protected int getDefenseWeight() {
        return state == State.COMBO
                ? (int) (super.getDefenseWeight() * 1.3f)
                : super.getDefenseWeight();
    }

    @Override
    protected int getSupportWeight() {
        float multiplier = isAssassin && state == State.COMBO ? 1.6f : 0.8f;
        return (int) (super.getSupportWeight() * multiplier);
    }

    public State currentState() { return state; }
}
