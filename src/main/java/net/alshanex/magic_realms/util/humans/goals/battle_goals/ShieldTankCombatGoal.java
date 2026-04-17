package net.alshanex.magic_realms.util.humans.goals.battle_goals;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.entity.mobs.wizards.GenericAnimatedWarlockAttackGoal;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.util.ModTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Combat goal for warriors carrying a shield.
 *
 * <p>Role: <b>frontline tank</b>. The tank's job is to (a) hold position near the frontline center of the ally group, (b) intercept hostiles attacking allies,
 * (c) prioritize support buffs and threatened-buff defense spells over raw damage, and (d) spend movement spells on group-covering approach spells rather than
 * personal repositioning.
 */
public class ShieldTankCombatGoal extends GenericAnimatedWarlockAttackGoal<AbstractMercenaryEntity> {

    private final AbstractMercenaryEntity tank;
    private final BattlefieldAnalysis battlefield;

    /** How far from the frontline center the tank will range before returning. */
    private static final double FRONTLINE_LEASH = 6.0;

    /** How far out to look for hostiles attacking allies (peel range). */
    private static final double PEEL_RANGE = 12.0;

    /** Re-evaluate peel candidates this often. */
    private static final int PEEL_CHECK_INTERVAL = 15;
    private int peelCheckCooldown = 0;

    public ShieldTankCombatGoal(AbstractMercenaryEntity tank, BattlefieldAnalysis battlefield) {
        super(tank, 1.0f, 70, 85);
        this.tank = tank;
        this.battlefield = battlefield;

        // Tanks are melee-first. The parent WarlockAttackGoal's meleeBias() decides "melee mode vs spell mode" every ~60-200 ticks
        // via a coinflip weighted by HP. Default .25-.75 meant the tank flipped into spell mode 25-75% of the time — way too often.
        //
        // .80-.95 means the tank stays in melee mode nearly always, only occasionally (and more so when damaged) rolling a spell window.
        this.setMeleeBias(0.80f, 0.95f);
    }

    @Override
    public void tick() {
        battlefield.refreshIfStale(10);

        // Periodic peel: if an ally is being attacked by someone we could reach faster, switch to intercept that attacker. Bosses override — we stay on the boss.
        if (--peelCheckCooldown <= 0 && !battlefield.isBossEngagement()) {
            peelCheckCooldown = PEEL_CHECK_INTERVAL;
            LivingEntity peelTarget = findPeelTarget();
            if (peelTarget != null && peelTarget != tank.getTarget()) {
                tank.setTarget(peelTarget);
                // Reset the target field the parent class caches
                this.target = peelTarget;
            }
        }

        super.tick();
    }

    /**
     * Scans for hostiles whose target is an ally mercenary within PEEL_RANGE of us.
     * Returns the closest one, or null if none found.
     */
    private LivingEntity findPeelTarget() {
        LivingEntity best = null;
        double bestSq = PEEL_RANGE * PEEL_RANGE;
        for (LivingEntity hostile : battlefield.hostiles()) {
            if (!(hostile instanceof net.minecraft.world.entity.Mob mob)) continue;
            LivingEntity theirTarget = mob.getTarget();
            if (theirTarget == null) continue;
            if (theirTarget == tank) continue; // already us, no peel needed
            if (!tank.isAlliedTo(theirTarget)) continue; // not attacking an ally
            double sq = hostile.distanceToSqr(tank);
            if (sq < bestSq) { bestSq = sq; best = hostile; }
        }
        return best;
    }

    // =========== SPELL-CATEGORY WEIGHTS ===========

    @Override
    protected int getAttackWeight() {
        // Reduce attack-spell preference — if the tank is casting, it should usually be a buff or a retreat counter, not another damage spell.
        return (int) (super.getAttackWeight() * 0.6f);
    }

    @Override
    protected int getDefenseWeight() {
        // Slight boost — counterattack/self-buff windows are valuable.
        return (int) (super.getDefenseWeight() * 1.3f);
    }

    @Override
    protected int getMovementWeight() {
        // Approach-movement spells help reposition to the frontline when off-position, but are wasted when already there.
        Vec3 frontline = battlefield.frontlineCenter();
        if (frontline != null && tank.position().distanceTo(frontline) < FRONTLINE_LEASH) {
            return (int) (super.getMovementWeight() * 0.2f);
        }
        return (int) (super.getMovementWeight() * 1.3f);
    }

    @Override
    protected int getSupportWeight() {
        // Moderate boost — support buffs are the tank's main non-sword contribution. Higher in boss fights where buff uptime matters.
        float multiplier = battlefield.isBossEngagement() ? 1.8f : 1.4f;
        return (int) (super.getSupportWeight() * multiplier);
    }

    // =========== SPELL FILTERING ===========

    /**
     * Remove retreat spells — tanks never disengage voluntarily.
     */
    public static List<AbstractSpell> filterMovementForTank(List<AbstractSpell> spells) {
        return spells.stream()
                .filter(s -> !ModTags.isSpellInTag(s, ModTags.RETREAT_MOVEMENT))
                .toList();
    }
}
