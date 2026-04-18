package net.alshanex.magic_realms.util.humans.goals.battle_goals;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.entity.mobs.wizards.GenericAnimatedWarlockAttackGoal;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.util.ModTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Combat goal for warriors WITHOUT a shield.
 *
 * <p>Role: <b>brawler / rhythm fighter</b>. The brawler does NOT brawl indefinitely —
 * it fights in a cyclical rhythm: engage in melee for a few hits, create a small
 * tactical pocket (~4 blocks) to cast a spell, drink a potion, or just catch its
 * breath, then reengage. Over and over. This is what distinguishes it from the
 * shield tank (which never disengages voluntarily) and the assassin/skirmisher
 * (which opens a huge gap and kites at 14 blocks).
 */
public class BrawlerCombatGoal extends GenericAnimatedWarlockAttackGoal<AbstractMercenaryEntity> {

    public enum State { ENGAGE, POCKET, REENGAGE }

    private final AbstractMercenaryEntity merc;
    private final BattlefieldAnalysis battlefield;

    private State state = State.ENGAGE;
    private int stateTicks = 0;

    /** Desired distance from the target while in POCKET state. */
    private static final double POCKET_DIST = 4.0;

    /** Accept POCKET as "reached" within this slack (blocks) around POCKET_DIST. */
    private static final double POCKET_TOLERANCE = 1.0;

    /** Distance considered "in melee range" — ENGAGE is satisfied, REENGAGE ends here. */
    private static final double MELEE_DIST = 3.0;

    // ===== RHYTHM CONSTANTS =====
    // The brawler's combat loop is ENGAGE (hit some) → POCKET (do something safe) → REENGAGE → ENGAGE.
    // These constants drive the "hit some" half.

    /** Melee hits landed during ENGAGE that trigger a proactive POCKET. */
    private static final int ENGAGE_HITS_BEFORE_POCKET = 3;

    /** Hard time cap for an ENGAGE phase — if we haven't landed HITS_BEFORE_POCKET in this many ticks, pocket anyway. Prevents the brawler from getting stuck
     * swinging-and-missing forever against a dodgy target. */
    private static final int ENGAGE_MAX_TICKS = 100; // ~5 s of swinging max before forcing a rhythm beat

    /** Minimum ticks we must spend in ENGAGE before a proactive pocket can fire. Ensures the brawler actually lands hits and doesn't pocket on tick 1. */
    private static final int ENGAGE_MIN_TICKS = 25;

    // ===== POCKET CONSTANTS =====

    /** HP fraction at or below which the brawler will FORCE-pocket immediately regardless of rhythm. Emergency override: heal NOW, don't wait for 3 hits. */
    private static final float POCKET_HP_EMERGENCY = 0.45f;

    /** HP fraction override during boss fights: even boss aggression can't stop us pocketing below this. */
    private static final float POCKET_HP_CRITICAL = 0.25f;

    /** Minimum pocket duration — long enough for a potion chug (32 ticks) plus some slack. */
    private static final int POCKET_MIN_TICKS = 40;

    /** Hard pocket cap — releases us even if a cast never resolves. */
    private static final int POCKET_MAX_TICKS = 90;

    /** Hard cap on REENGAGE so it can't hang forever if pathing fails. */
    private static final int REENGAGE_MAX_TICKS = 40;

    private Vec3 pocketAnchor = null;
    private int pocketPathTimer = 0;
    private int hitsLandedInEngage = 0;
    private int lastMobHurtTick = -1;

    private final List<AbstractSpell> canonicalAttackSpells = new ArrayList<>();

    public BrawlerCombatGoal(AbstractMercenaryEntity merc, BattlefieldAnalysis battlefield) {
        super(merc, 1.2f, 40, 65);
        this.merc = merc;
        this.battlefield = battlefield;

        // Brawlers are melee-first. Slightly less "tanky" in spell selection than the shield tank (they have movement / defense tools to actually use on pockets)
        // but still clearly melee-biased — they should spend most ticks swinging.
        this.setMeleeBias(0.70f, 0.88f);
    }

    @Override
    public GenericAnimatedWarlockAttackGoal<AbstractMercenaryEntity> setSpells(
            List<AbstractSpell> attackSpells,
            List<AbstractSpell> defenseSpells,
            List<AbstractSpell> movementSpells,
            List<AbstractSpell> supportSpells) {
        super.setSpells(attackSpells, defenseSpells, movementSpells, supportSpells);
        canonicalAttackSpells.clear();
        canonicalAttackSpells.addAll(attackSpells);
        return this;
    }

    // =========== LIFECYCLE ===========

    @Override
    public void start() {
        super.start();
        state = State.ENGAGE;
        stateTicks = 0;
        pocketAnchor = null;
        pocketPathTimer = 0;
        hitsLandedInEngage = 0;
        lastMobHurtTick = -1;
    }

    @Override
    public void stop() {
        super.stop();
        pocketAnchor = null;
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

        double distSq = merc.distanceToSqr(currentTarget);

        // Track melee hits landed on the current target during ENGAGE — the primary
        // rhythm-trigger. getLastHurtByMobTimestamp is updated by the vanilla hurt
        // pipeline whenever this mob damages a target, and lastHurtByMob identifies
        // who dealt the hit, so together they let us count OUR hits specifically.
        if (state == State.ENGAGE) {
            int lastHurt = currentTarget.getLastHurtByMobTimestamp();
            if (lastHurt != lastMobHurtTick
                    && currentTarget.getLastHurtByMob() == merc) {
                hitsLandedInEngage++;
                lastMobHurtTick = lastHurt;
            }
        }

        // Decide transitions BEFORE running the parent (which may itself start a cast).
        handleStateTransitions(currentTarget, distSq);

        switch (state) {
            case ENGAGE -> {
                // Let the parent handle swings + spell casts. Range-filter attack spells so a ~4-block cast after a pocket doesn't pick a close-range
                // spell that misses.
                restrictAttackSpellsByRange(distSq);
                try {
                    super.tick();
                } finally {
                    restoreCanonicalAttackSpells();
                }
            }
            case POCKET -> {
                // Still let the parent tick so cast progression, potion drinking, and look-at logic continue — but override movement ourselves to sit at
                // POCKET_DIST from the target rather than marching back in.
                restrictAttackSpellsByRange(distSq);
                try {
                    super.tick();
                } finally {
                    restoreCanonicalAttackSpells();
                }
                drivePocket(currentTarget, distSq);
            }
            case REENGAGE -> {
                // Hard commit: rush back to the target. No cast, no strafe — just close the gap. Parent is skipped so it can't countermand movement.
                driveReengage(currentTarget);
            }
        }
    }

    // =========== STATE TRANSITIONS ===========

    private void handleStateTransitions(LivingEntity target, double distSq) {
        // `target` is kept in the signature for symmetry with callers that pass the
        // live target reference; the current implementation only reads distSq and
        // the merc's own state, but a future extension could use target (e.g. check
        // target's casting state, its debuffs, etc.) without a signature change.
        switch (state) {
            case ENGAGE -> {
                // Rhythm transitions — these are what drive the brawler loop:
                //
                //   1. EMERGENCY: HP dropped below the emergency threshold. Pocket
                //      RIGHT NOW to drink a potion, no matter how few hits we landed.
                //   2. RHYTHM: we've been in ENGAGE at least ENGAGE_MIN_TICKS and
                //      either landed ENGAGE_HITS_BEFORE_POCKET hits OR hit the
                //      ENGAGE_MAX_TICKS cap. Time for a breather beat.
                //
                // The HP threshold is higher than in the original design because the
                // new default is "pocket on rhythm," so the emergency trigger is only
                // for *when the rhythm beat isn't fast enough* — e.g. taking a big
                // spike hit early in an ENGAGE window.

                if (isEmergencyPocketWarranted()) {
                    transition(State.POCKET);
                    return;
                }

                boolean minTimeMet = stateTicks >= ENGAGE_MIN_TICKS;
                boolean hitsHit = hitsLandedInEngage >= ENGAGE_HITS_BEFORE_POCKET;
                boolean timeCap = stateTicks >= ENGAGE_MAX_TICKS;

                if ((minTimeMet && hitsHit) || timeCap) {
                    transition(State.POCKET);
                }
            }
            case POCKET -> {
                // Hold the pocket at least POCKET_MIN_TICKS (enough for a potion or
                // medium-cast spell to actually finish), release once either:
                //   - Min time elapsed AND we're not mid-cast / mid-drink (the reason
                //     for pocketing is resolved; go back to fighting).
                //   - Hard timeout hit.
                //
                // Critically, we NO LONGER require HP to be above a threshold to
                // release — the pocket is a rhythm beat, not a full recovery window.
                // If the heal only brought us from 40% to 60%, that's fine; reengage.

                boolean minTimeMet = stateTicks >= POCKET_MIN_TICKS;
                boolean timeout = stateTicks >= POCKET_MAX_TICKS;
                boolean notMidAction = !merc.isCasting() && !merc.isUsingItem();

                if ((minTimeMet && notMidAction) || timeout) {
                    pocketAnchor = null;
                    transition(State.REENGAGE);
                }
            }
            case REENGAGE -> {
                if (distSq <= MELEE_DIST * MELEE_DIST || stateTicks >= REENGAGE_MAX_TICKS) {
                    transition(State.ENGAGE);
                }
            }
        }
    }

    /**
     * Checks whether we should abort the current ENGAGE rhythm and pocket right now.
     * Currently: low HP override. Can be extended later (e.g. hit with a status effect).
     */
    private boolean isEmergencyPocketWarranted() {
        float hpFrac = merc.getHealth() / merc.getMaxHealth();
        if (battlefield.isBossEngagement()) {
            return hpFrac <= POCKET_HP_CRITICAL;
        }
        return hpFrac <= POCKET_HP_EMERGENCY;
    }

    private void transition(State next) {
        this.state = next;
        this.stateTicks = 0;
        if (next == State.POCKET) {
            // Cancel any in-flight path so drivePocket starts clean.
            merc.getNavigation().stop();
            pocketAnchor = null;
            pocketPathTimer = 0;
        } else if (next == State.REENGAGE) {
            merc.getNavigation().stop();
        } else if (next == State.ENGAGE) {
            // Fresh ENGAGE phase — reset the rhythm counters.
            hitsLandedInEngage = 0;
            lastMobHurtTick = -1;
        }
    }

    // =========== MOVEMENT DRIVERS ===========

    /**
     * POCKET movement: actively maintain POCKET_DIST from the target at all times.
     */
    private void drivePocket(LivingEntity target, double distSq) {
        double dist = Math.sqrt(distSq);

        // Face the target so we can reengage instantly and so any attack spell the parent rolls during POCKET aims correctly.
        merc.getLookControl().setLookAt(target, 30f, 30f);

        pocketAnchor = computePocketAnchor(target);

        boolean stuck = !merc.getNavigation().isInProgress()
                && merc.position().distanceToSqr(pocketAnchor) > 1.5 * 1.5;
        boolean navRefreshDue = --pocketPathTimer <= 0;

        if (navRefreshDue || stuck) {
            // Speed scaling:
            //   - Enemy INSIDE our pocket (dist < POCKET_DIST - tolerance): they're crowding, full escape speed so we can actually open the gap.
            //   - Enemy at/near pocket distance: normal upkeep speed.
            //   - Enemy beyond pocket (we overshot somehow): calm drift back in.
            double speedMult;
            if (dist < POCKET_DIST - POCKET_TOLERANCE) {
                speedMult = 1.5; // crowded — open gap fast
            } else if (dist > POCKET_DIST + POCKET_TOLERANCE) {
                speedMult = 0.9; // overshot — drift back calmly, don't re-enter melee
            } else {
                speedMult = 1.1; // upkeep — match target's tracking speed
            }
            merc.getNavigation().moveTo(
                    pocketAnchor.x, pocketAnchor.y, pocketAnchor.z,
                    speedModifier * speedMult);
            // Short refresh interval — 5 ticks (quarter second) — so a mobile target doesn't outrun a stale destination for long. Too low wastes CPU on
            // redundant pathfind calls; 5 is a reasonable balance.
            pocketPathTimer = 5;
        }
    }

    /**
     * Pick a point exactly POCKET_DIST blocks away from the target along the vector target → self. This is the "stay behind where I currently am" anchor. If we're
     * basically on top of the target (vector degenerate), pick an arbitrary direction so we at least start moving somewhere.
     */
    private Vec3 computePocketAnchor(LivingEntity target) {
        Vec3 away = merc.position().subtract(target.position());
        if (away.lengthSqr() < 0.0001) {
            away = new Vec3(1, 0, 0);
        }
        Vec3 normalized = away.normalize();
        return target.position().add(normalized.scale(POCKET_DIST));
    }

    /**
     * REENGAGE movement: sprint straight at the target. No spells, no strafe.
     */
    private void driveReengage(LivingEntity target) {
        merc.getLookControl().setLookAt(target, 30f, 30f);
        merc.getNavigation().moveTo(target, speedModifier * 1.4);
    }

    // =========== SPELL RANGE FILTERING ===========

    /**
     * Swap the parent's attack spell list for a distance-filtered subset so a 4-block pocket doesn't produce a
     * close-range spell cast (which would whiff) and a melee-range engage doesn't waste a long-range one.
     */
    private void restrictAttackSpellsByRange(double distSq) {
        if (canonicalAttackSpells.isEmpty()) return;
        double dist = Math.sqrt(distSq);

        List<AbstractSpell> filtered = new ArrayList<>();
        if (dist <= 3.0) {
            for (AbstractSpell s : canonicalAttackSpells) {
                if (ModTags.isSpellInTag(s, ModTags.CLOSE_RANGE_ATTACKS)) filtered.add(s);
            }
            for (AbstractSpell s : canonicalAttackSpells) {
                if (ModTags.isSpellInTag(s, ModTags.MID_RANGE_ATTACKS)) filtered.add(s);
            }
        } else if (dist <= 6.0) {
            for (AbstractSpell s : canonicalAttackSpells) {
                if (ModTags.isSpellInTag(s, ModTags.MID_RANGE_ATTACKS)) filtered.add(s);
            }
            for (AbstractSpell s : canonicalAttackSpells) {
                if (ModTags.isSpellInTag(s, ModTags.CLOSE_RANGE_ATTACKS)) filtered.add(s);
            }
        } else {
            for (AbstractSpell s : canonicalAttackSpells) {
                if (ModTags.isSpellInTag(s, ModTags.LONG_RANGE_ATTACKS)
                        || ModTags.isSpellInTag(s, ModTags.MID_RANGE_ATTACKS)) {
                    filtered.add(s);
                }
            }
        }

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

    // =========== WEIGHT OVERRIDES ===========

    @Override
    protected int getAttackWeight() {
        return switch (state) {
            // In ENGAGE, slight boost — we want the brawler throwing damage spells more readily than the ambient mix (the tank downweights these).
            case ENGAGE -> (int) (super.getAttackWeight() * 1.15f);
            // In POCKET, we're here to heal or cast utility, not to DPS.
            case POCKET -> (int) (super.getAttackWeight() * 0.3f);
            case REENGAGE -> 0;
        };
    }

    @Override
    protected int getDefenseWeight() {
        // Pocketing warrants buffing / counter-casting; ENGAGE baseline.
        return switch (state) {
            case POCKET -> (int) (super.getDefenseWeight() * 1.6f);
            case ENGAGE -> super.getDefenseWeight();
            case REENGAGE -> 0;
        };
    }

    @Override
    protected int getMovementWeight() {
        // Brawlers don't use movement spells to keep distance (like skirmishers do).
        // They use them to close gaps — so REENGAGE gets a boost and POCKET a cut.
        return switch (state) {
            case ENGAGE -> (int) (super.getMovementWeight() * 0.6f);
            case POCKET -> (int) (super.getMovementWeight() * 0.2f);
            case REENGAGE -> 0; // we handle reengagement directly; don't waste mana
        };
    }

    @Override
    protected int getSupportWeight() {
        // POCKET is where buffs + potions land; boost heavily so the parent actually rolls them over more damage spells during this window.
        return switch (state) {
            case POCKET -> (int) (super.getSupportWeight() * 2.0f);
            case ENGAGE -> (int) (super.getSupportWeight() * 0.9f);
            case REENGAGE -> 0;
        };
    }

    public State currentState() { return state; }
}
