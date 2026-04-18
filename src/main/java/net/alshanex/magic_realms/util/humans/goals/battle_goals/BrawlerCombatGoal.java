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
 * <p>Role: <b>brawler / frontline contender</b>. The brawler is aggressive by default — it presses the target in melee like the shield tank does — but unlike the tank it
 * will periodically create a small tactical pocket (~4 blocks) to drink a healing potion or to finish a non-attack spell cast without getting interrupted, and then
 * slams back into melee range.
 *
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

    /** HP fraction at or below which the brawler will pocket to drink a potion. */
    private static final float POCKET_HP_THRESHOLD = 0.55f;

    /** HP fraction override: even during boss melee burn, below THIS we still pocket. */
    private static final float POCKET_HP_CRITICAL = 0.30f;

    /** Minimum ticks to stay in POCKET before we'll even consider reengaging. */
    private static final int POCKET_MIN_TICKS = 30;

    /** Hard cap on POCKET duration so we can't get stuck if a cast never resolves. */
    private static final int POCKET_MAX_TICKS = 80;

    /** Cooldown (ticks) after a POCKET ends before we can pocket again. */
    private static final int POCKET_COOLDOWN_TICKS = 80;

    /** How often (ticks) to re-check whether we should enter POCKET while ENGAGEd. */
    private static final int POCKET_CHECK_INTERVAL = 10;

    /** Hard cap on REENGAGE so it can't hang forever if pathing fails. */
    private static final int REENGAGE_MAX_TICKS = 40;

    private int pocketCheckCooldown = 0;
    private int pocketCooldown = 0;
    private Vec3 pocketAnchor = null;
    private int pocketPathTimer = 0;

    /** Tracks whether we saw the parent start a non-attack cast during this ENGAGE tick. */
    private boolean lastTickWasCasting = false;

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
        pocketCheckCooldown = 0;
        pocketCooldown = 0;
        pocketAnchor = null;
        pocketPathTimer = 0;
        lastTickWasCasting = false;
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
        if (pocketCheckCooldown > 0) pocketCheckCooldown--;
        if (pocketCooldown > 0) pocketCooldown--;

        double distSq = merc.distanceToSqr(currentTarget);

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
                // After the parent has ticked, check whether it started a non-attack cast that warrants pocketing to let it resolve safely.
                maybePocketForNewCast(distSq);
                lastTickWasCasting = merc.isCasting();
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
        switch (state) {
            case ENGAGE -> {
                // Transitions into POCKET happen inside maybePocketForNewCast() and in the periodic HP check below — both issue transition(State.POCKET)
                // directly. Nothing to do here that isn't handled elsewhere.
                if (pocketCheckCooldown <= 0) {
                    pocketCheckCooldown = POCKET_CHECK_INTERVAL;
                    if (shouldPocketForHeal(distSq)) {
                        transition(State.POCKET);
                    }
                }
            }
            case POCKET -> {
                // Exit conditions:
                //   1. Min time elapsed AND the reason we pocketed is resolved (HP back up, not casting, not drinking).
                //   2. Hard timeout.
                boolean minTimeMet = stateTicks >= POCKET_MIN_TICKS;
                boolean timeout = stateTicks >= POCKET_MAX_TICKS;
                boolean reasonResolved = !merc.isCasting()
                        && !merc.isUsingItem()  // potion drinking uses the "using item" hand state
                        && merc.getHealth() / merc.getMaxHealth() > POCKET_HP_THRESHOLD;

                if ((minTimeMet && reasonResolved) || timeout) {
                    pocketAnchor = null;
                    pocketCooldown = POCKET_COOLDOWN_TICKS;
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
        }
    }

    /**
     * HP-based pocket trigger. Pocket to heal when:
     *   - HP fraction is at/below POCKET_HP_THRESHOLD (normal), OR HP fraction is at/below POCKET_HP_CRITICAL (override boss aggression).
     *   - We aren't already on pocket cooldown.
     *   - We're actually close enough to the target for the retreat to mean anything (no point pocketing if we're already far away — just engage).
     */
    private boolean shouldPocketForHeal(double distSq) {
        if (pocketCooldown > 0) return false;
        float hpFrac = merc.getHealth() / merc.getMaxHealth();

        // Boss fights: only pocket if we're critically low. Otherwise stay on the boss.
        if (battlefield.isBossEngagement()) {
            if (hpFrac > POCKET_HP_CRITICAL) return false;
        } else {
            if (hpFrac > POCKET_HP_THRESHOLD) return false;
        }

        // Must be roughly in melee/engagement range — if we're already at pocket distance or further, there's nothing to pocket FROM.
        double nearEnoughSq = (POCKET_DIST + POCKET_TOLERANCE) * (POCKET_DIST + POCKET_TOLERANCE);
        return distSq <= nearEnoughSq;
    }

    /**
     * Detects when the parent started a fresh NON-ATTACK cast during this ENGAGE tick (defense / support / movement) and pocket so the cast finishes safely.
     */
    private void maybePocketForNewCast(double distSq) {
        if (pocketCooldown > 0) return;
        boolean isCasting = merc.isCasting();
        if (!isCasting) return;
        // Only react to the *edge* (was not casting last tick, is casting now) so we don't re-pocket every tick of a long cast.
        if (lastTickWasCasting) return;

        AbstractSpell casting = currentlyCastingSpell();
        if (casting == null) return;

        // Attack spells stay in melee — no pocket. Everything else (defense / support / movement) gets a breathing pocket.
        if (canonicalAttackSpells.contains(casting)) return;

        // Must actually be close enough for pocketing to change anything.
        double nearEnoughSq = (POCKET_DIST + POCKET_TOLERANCE) * (POCKET_DIST + POCKET_TOLERANCE);
        if (distSq > nearEnoughSq) return;

        transition(State.POCKET);
    }

    private AbstractSpell currentlyCastingSpell() {
        try {
            var data = merc.getMagicData();
            if (data == null) return null;
            var casting = data.getCastingSpell();
            return casting == null ? null : casting.getSpell();
        } catch (Throwable t) {
            // Defensive: if the magic-data API shape ever shifts we don't want to crash the goal — just skip the cast-based pocket trigger this tick.
            return null;
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
