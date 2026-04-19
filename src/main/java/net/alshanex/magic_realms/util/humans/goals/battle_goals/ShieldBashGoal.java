package net.alshanex.magic_realms.util.humans.goals.battle_goals;

import io.redspace.ironsspellbooks.entity.spells.EarthquakeAoe;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
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

    private static final double BASH_HIT_RADIUS = 3.0;

    private static final double BASH_CROWD_RADIUS = 3.5;

    /** Base damage multiplier applied to the tank's attack damage attribute. */
    private static final float BASH_DAMAGE_MULT = 0.35f;

    /** Horizontal knockback strength (vanilla knockback units; 0.4 is a standard hit, 1.0 is huge). */
    private static final double BASH_KNOCKBACK = 1.2;

    /** Ticks from impact until the goal ends (recovery — tank is committed, can't attack). */
    private static final int RECOVERY_TICKS = 9;

    /** Cooldowns (ticks) by crowd level. Smaller number = more frequent bashes. */
    private static final int COOLDOWN_SOLO = 150;
    private static final int COOLDOWN_PAIR = 100;
    private static final int COOLDOWN_HORDE = 50;

    /**
     * Cooldown applied when the bash connected with ZERO victims
     */
    private static final int COOLDOWN_WHIFF = 20; // 1 s

    /** Self-buff duration (ticks) applied on successful bash. */
    private static final int BUFF_DURATION_TICKS = 60; // 3 s

    /** Required to even consider bashing: at least this many hostiles within reach. */
    private static final int MIN_HOSTILES_IN_RANGE_TO_BASH = 1;

    // ========== STATE ==========

    private int cooldownRemaining = 60; // small initial delay so bash doesn't fire at combat t=0
    private int stateTicks = 0;
    private boolean impactDone = false;

    /** Hits landed by the most recent impact. Read by stop() to pick between the crowd cooldown and the whiff cooldown. */
    private int lastImpactHits = 0;

    private int crowdAtEngagement = 0;

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

        if (!tank.hasShield()) return false;
        if (tank.isStunned()) return false;
        if (tank.isSittingInChair()) return false;
        if (tank.isInMenuState()) return false;
        if (tank.isCasting()) return false;

        LivingEntity target = tank.getTarget();
        if (target == null || !target.isAlive()) return false;

        // Only bash if we actually have at least one hostile in bash range — no point air-bashing when the enemy is 10 blocks away.
        battlefield.refreshIfStale(10);
        int nearbyCount = countHostilesInBashRange();
        if (nearbyCount < MIN_HOSTILES_IN_RANGE_TO_BASH) return false;

        if (!anyHostileWithinHitRange()) return false;

        this.crowdAtEngagement = nearbyCount;

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        // Run for exactly windup + recovery ticks then release.
        return stateTicks < RECOVERY_TICKS;
    }

    @Override
    public void start() {
        stateTicks = 0;
        impactDone = false;
        lastImpactHits = 0;

        // Kick off the animation immediately — the windup visual should precede the actual impact by WINDUP_TICKS so the effect feels telegraphed.
        tank.serverTriggerAnimation("touch_ground");

        // Face the target hard so the frontal cone lines up with where we're aiming.
        LivingEntity target = tank.getTarget();
        if (target != null) {
            tank.getLookControl().setLookAt(target, 60f, 60f);
        }

        tank.getNavigation().stop();
        var v = tank.getDeltaMovement();
        tank.setDeltaMovement(0.0, v.y, 0.0);
    }

    @Override
    public void tick() {
        stateTicks++;

        LivingEntity target = tank.getTarget();
        if (target != null) {
            tank.getLookControl().setLookAt(target, 60f, 60f);
        }

        // Impact lands on the WINDUP_TICKS-th tick. Using == (rather than >=) plus the impactDone guard ensures the effect only fires once even if ticking lags.
        if (!impactDone) {
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

    private void performBashImpact() {
        if (tank.level().isClientSide()) return;

        // Shield whoosh sound
        tank.playSound(SoundRegistry.EARTHQUAKE_CAST.get(), 1.2f, 0.85f);

        AABB box = tank.getBoundingBox().inflate(BASH_HIT_RADIUS, BASH_HIT_RADIUS + 1.0, BASH_HIT_RADIUS);
        List<LivingEntity> candidates = tank.level().getEntitiesOfClass(
                LivingEntity.class, box,
                e -> e != tank && e.isAlive() && !tank.isAlliedTo(e));

        float bashDamage = computeBashDamage();
        int hits = 0;

        for (LivingEntity victim : candidates) {

            double dx = victim.getX() - tank.getX();
            double dz = victim.getZ() - tank.getZ();
            double horizDistSq = dx * dx + dz * dz;

            DamageSource src = tank.damageSources().mobAttack(tank);
            boolean hurt = victim.hurt(src, bashDamage);

            double kbLen = Math.sqrt(horizDistSq);
            if (kbLen > 0.0001) {
                double kbX = dx / kbLen;
                double kbZ = dz / kbLen;
                victim.knockback(BASH_KNOCKBACK, -kbX, -kbZ);
                victim.setDeltaMovement(victim.getDeltaMovement().add(0, 0.25, 0));
            } else {
                victim.knockback(BASH_KNOCKBACK, -1.0, 0.0);
                victim.setDeltaMovement(victim.getDeltaMovement().add(0, 0.25, 0));
            }

            if (hurt) hits++;
        }

        // Remember for stop() so it can pick COOLDOWN_WHIFF when nothing landed.
        this.lastImpactHits = hits;

        // Survivability buff scales with how many enemies we cleared.
        applySelfBuffs(hits);

        EarthquakeAoe aoeEntity = new EarthquakeAoe(tank.level());
        aoeEntity.moveTo(tank.position());
        aoeEntity.setOwner(tank);
        aoeEntity.setCircular();
        aoeEntity.setRadius((float) BASH_HIT_RADIUS);
        aoeEntity.setDuration(20);
        aoeEntity.setDamage(0);
        aoeEntity.setSlownessAmplifier(0);
        tank.level().addFreshEntity(aoeEntity);
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
        double rSq = BASH_CROWD_RADIUS * BASH_CROWD_RADIUS;
        int count = 0;
        for (LivingEntity e : battlefield.hostiles()) {
            if (e.distanceToSqr(tank) <= rSq) count++;
        }
        return count;
    }

    private boolean anyHostileWithinHitRange() {
        double rSq = BASH_HIT_RADIUS * BASH_HIT_RADIUS;
        for (LivingEntity e : battlefield.hostiles()) {
            double dx = e.getX() - tank.getX();
            double dz = e.getZ() - tank.getZ();
            if (dx * dx + dz * dz <= rSq) return true;
        }
        return false;
    }

    private int computeCooldownForCrowd() {
        if (lastImpactHits == 0) return COOLDOWN_WHIFF;
        int n = crowdAtEngagement;
        if (n >= 3) return COOLDOWN_HORDE;
        if (n == 2) return COOLDOWN_PAIR;
        return COOLDOWN_SOLO;
    }
}
