package net.alshanex.magic_realms.util.humans.goals.battle_goals;

import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * Shield bash goal for warriors carrying a shield.
 *
 * <p>Cadence is crowd-sensitive:
 * <ul>
 *   <li><b>Solo target</b> (0–1 hostiles in bash radius): long cooldown. The tank bashes occasionally as a kit-rounding move, but most of combat is swings.</li>
 *   <li><b>Two hostiles nearby</b>: medium cooldown. A sensible rhythm.</li>
 *   <li><b>Three+ hostiles nearby</b> (being actively mobbed): short cooldown — the bash becomes the primary tool for not drowning in a horde.</li>
 * </ul>
 */
public class ShieldBashGoal extends Goal {

    private final AbstractMercenaryEntity tank;
    private final BattlefieldAnalysis battlefield;

    // ========== TUNING CONSTANTS ==========

    /** Radius used both for (a) crowd counting and (b) who the bash actually hits. */
    private static final double BASH_RADIUS = 3.5;

    /** Cone half-width */
    private static final double CONE_DOT_THRESHOLD = 0.5;

    /** Base damage multiplier applied to the tank's attack damage attribute. */
    private static final float BASH_DAMAGE_MULT = 0.35f;

    /** Horizontal knockback strength (vanilla knockback units; 0.4 is a standard hit, 1.0 is huge). */
    private static final double BASH_KNOCKBACK = 1.1;

    /** Ticks from goal start until the bash impact lands (windup). */
    private static final int WINDUP_TICKS = 6;

    /** Ticks from impact until the goal ends (recovery — tank is committed, can't attack). */
    private static final int RECOVERY_TICKS = 9;

    /** Cooldowns (ticks) by crowd level. Smaller number = more frequent bashes. */
    private static final int COOLDOWN_SOLO = 300;          // 15 s — occasional
    private static final int COOLDOWN_PAIR = 160;          // 8  s — regular
    private static final int COOLDOWN_HORDE = 80;          // 4  s — very frequent

    /** Self-buff duration (ticks) applied on successful bash. */
    private static final int BUFF_DURATION_TICKS = 60; // 3 s

    /** Required to even consider bashing: at least this many hostiles within reach. */
    private static final int MIN_HOSTILES_IN_RANGE_TO_BASH = 1;

    // ========== STATE ==========

    private int cooldownRemaining = 60; // small initial delay so bash doesn't fire at combat t=0
    private int stateTicks = 0;
    private boolean impactDone = false;

    public ShieldBashGoal(AbstractMercenaryEntity tank, BattlefieldAnalysis battlefield) {
        this.tank = tank;
        this.battlefield = battlefield;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    // ========== LIFECYCLE ==========

    @Override
    public boolean canUse() {
        if (cooldownRemaining > 0) {
            cooldownRemaining--;
            return false;
        }

        // Fail-fast checks mirroring the existing parry logic in AbstractMercenaryEntity.hurt().
        // If any of these fail we silently keep the cooldown at 0 so the next tick will try again — we don't want to burn a real cooldown window on being stunned.
        if (!tank.hasShield()) return false;
        if (tank.isStunned()) return false;
        if (tank.isSittingInChair()) return false;
        if (tank.isInMenuState()) return false;
        if (tank.isAnimating()) return false; // covers: mid-swing, mid-parry, mid-cast
        if (tank.isCasting()) return false;

        LivingEntity target = tank.getTarget();
        if (target == null || !target.isAlive()) return false;

        // Only bash if we actually have at least one hostile in bash range — no point air-bashing when the enemy is 10 blocks away.
        battlefield.refreshIfStale(10);
        int nearbyCount = countHostilesInBashRange();
        if (nearbyCount < MIN_HOSTILES_IN_RANGE_TO_BASH) return false;

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        // Run for exactly windup + recovery ticks then release.
        return stateTicks < WINDUP_TICKS + RECOVERY_TICKS;
    }

    @Override
    public void start() {
        stateTicks = 0;
        impactDone = false;

        // Kick off the animation immediately — the windup visual should precede the actual impact by WINDUP_TICKS so the effect feels telegraphed.
        tank.serverTriggerAnimation("offhand_parry");

        // Face the target hard so the frontal cone lines up with where we're aiming.
        LivingEntity target = tank.getTarget();
        if (target != null) {
            tank.getLookControl().setLookAt(target, 60f, 60f);
        }

        // Stop any in-flight pathing for the duration of the bash — stationary is the correct pose for a bash and it also keeps the cone direction stable.
        tank.getNavigation().stop();
    }

    @Override
    public void tick() {
        stateTicks++;

        LivingEntity target = tank.getTarget();
        if (target != null) {
            tank.getLookControl().setLookAt(target, 60f, 60f);
        }

        // Impact lands on the WINDUP_TICKS-th tick. Using == (rather than >=) plus the impactDone guard ensures the effect only fires once even if ticking lags.
        if (!impactDone && stateTicks >= WINDUP_TICKS) {
            impactDone = true;
            performBashImpact();
        }
    }

    @Override
    public void stop() {
        // Set the cooldown AFTER the bash completes so a big horde can immediately line up the next short cooldown rather than the long one.
        cooldownRemaining = computeCooldownForCrowd();
        stateTicks = 0;
        impactDone = false;
    }

    // ========== IMPACT ==========

    /**
     * Actually apply the bash: damage + knockback to everything in the frontal cone, plus a short survivability buff on the tank itself.
     */
    private void performBashImpact() {
        if (tank.level().isClientSide()) return;

        // Shield whoosh sound
        tank.playSound(SoundEvents.SHIELD_BLOCK, 1.2f, 0.85f);

        Vec3 tankEye = tank.getEyePosition();
        Vec3 forward = tank.getForward().normalize();

        // Gather candidates in a fat box; the cone + distance checks refine from there.
        AABB box = tank.getBoundingBox().inflate(BASH_RADIUS);
        List<LivingEntity> candidates = tank.level().getEntitiesOfClass(
                LivingEntity.class, box,
                e -> e != tank && e.isAlive() && !tank.isAlliedTo(e));

        double radiusSq = BASH_RADIUS * BASH_RADIUS;
        float bashDamage = computeBashDamage();
        int hits = 0;

        for (LivingEntity victim : candidates) {
            // Distance gate (cone check below is direction-only).
            if (victim.distanceToSqr(tank) > radiusSq) continue;

            // Cone check: vector from tank to victim must mostly align with tank's facing. This keeps the bash in front — enemies behind us don't get hit.
            Vec3 toVictim = victim.position().subtract(tankEye).normalize();
            if (toVictim.dot(forward) < CONE_DOT_THRESHOLD) continue;

            // Damage. Using GENERIC_ATTACK source via the tank so this counts as a mob-on-mob attack for targeting/revenge purposes.
            DamageSource src = tank.damageSources().mobAttack(tank);
            boolean hurt = victim.hurt(src, bashDamage);

            // Knockback: vanilla's knockback() uses directional components and automatically deals with air/ground. Apply even if hurt() returned
            // false (e.g. iframes) — the push still makes the move feel correct.
            double kbX = victim.getX() - tank.getX();
            double kbZ = victim.getZ() - tank.getZ();
            // Normalize to avoid knockback feeling uneven at different distances.
            double kbLen = Math.sqrt(kbX * kbX + kbZ * kbZ);
            if (kbLen > 0.0001) {
                kbX /= kbLen;
                kbZ /= kbLen;
                victim.knockback(BASH_KNOCKBACK, -kbX, -kbZ);
                // Tiny vertical pop so targets peel off cleanly instead of skidding right back into us along the ground.
                victim.setDeltaMovement(victim.getDeltaMovement().add(0, 0.25, 0));
            }

            if (hurt) hits++;
        }

        // Survivability reward scales with how many enemies we cleared: bashing one guy gives a mild buff, bashing a pack gives a stacked (longer) buff so the
        // horde scenario genuinely feels like the tank is riding out the crowd.
        applySelfBuffs(hits);
    }

    private float computeBashDamage() {
        // Read the tank's attack damage attribute if available, fall back to 4.
        double attackDamage = tank.getAttribute(Attributes.ATTACK_DAMAGE) != null
                ? tank.getAttributeValue(Attributes.ATTACK_DAMAGE)
                : 4.0;
        return (float) Math.max(1.0, attackDamage * BASH_DAMAGE_MULT);
    }

    /**
     * Apply short-duration buffs to the tank. Resistance reduces incoming damage by 20% per level and Absorption gives a small buffer of gold hearts. Both match
     * the "increase survivability" ask; Resistance handles sustained melee, Absorption handles the next big incoming hit.
     */
    private void applySelfBuffs(int hits) {
        int resistAmplifier = hits >= 3 ? 1 : 0;    // Resistance II at 3+ hits, else I
        int absorptionAmp = Math.min(hits, 2);       // Absorption I/II/III capped at III (arg = 0/1/2)
        int duration = BUFF_DURATION_TICKS + (hits - 1) * 20; // +1s per extra target hit

        applyOrReplaceEffect(MobEffects.DAMAGE_RESISTANCE, duration, resistAmplifier);
        if (hits >= 1) {
            applyOrReplaceEffect(MobEffects.ABSORPTION, duration, absorptionAmp);
        }
    }

    /**
     * Apply an effect, overriding an existing instance only if the new one is at least as potent. This prevents a weak single-target bash from overwriting a fresh
     * horde-bash buff mid-duration.
     */
    private void applyOrReplaceEffect(Holder<MobEffect> effect, int duration, int amplifier) {
        MobEffectInstance existing = tank.getEffect(effect);
        if (existing != null
                && existing.getAmplifier() >= amplifier
                && existing.getDuration() >= duration) {
            return;
        }
        tank.addEffect(new MobEffectInstance(effect, duration, amplifier, false, true));
    }

    // ========== CROWD HEURISTIC ==========

    private int countHostilesInBashRange() {
        double rSq = BASH_RADIUS * BASH_RADIUS;
        int count = 0;
        for (LivingEntity e : battlefield.hostiles()) {
            if (e.distanceToSqr(tank) <= rSq) count++;
        }
        return count;
    }

    /**
     * Cooldown scales inversely with crowd size:
     *   0–1 nearby  -> long cooldown (solo rhythm)
     *   2 nearby    -> medium cooldown
     *   3+ nearby   -> short cooldown (horde rhythm)
     *
     * <p>Sampled at the end of a bash (in {@link #stop()}) so the next cooldown reflects the current situation, not the situation when we started the windup.
     */
    private int computeCooldownForCrowd() {
        int n = countHostilesInBashRange();
        if (n >= 3) return COOLDOWN_HORDE;
        if (n == 2) return COOLDOWN_PAIR;
        return COOLDOWN_SOLO;
    }
}
