package net.alshanex.magic_realms.util.humans.goals.battle_goals;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.mobs.goals.WizardSupportGoal;
import net.alshanex.magic_realms.entity.humans.AbstractMercenaryEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Healing / buffing goal for support mercenaries.
 */
public class MercenarySupportGoal extends WizardSupportGoal<AbstractMercenaryEntity> {

    /** Only heal below this fraction of max health. */
    private static final float HEAL_THRESHOLD = 0.9f;

    /** Ticks the goal stays dormant after an activation, so it can't monopolise MOVE/LOOK. */
    private static final int COOLDOWN_AFTER_CAST = 40;

    private final AbstractMercenaryEntity merc;

    private int cooldown;
    private boolean attemptedCast;

    public MercenarySupportGoal(AbstractMercenaryEntity mob, double speedModifier,
                                int attackIntervalMin, int attackIntervalMax) {
        super(mob, speedModifier, attackIntervalMin, attackIntervalMax);
        this.merc = mob;

        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }

        if (merc.isStunned() || merc.isSittingInChair() || merc.isInMenuState()) return false;

        // Nothing to cast: never seize the flags. Guards against an empty HEAL_ALLIES/BUFF_ALLIES tag.
        if (healingSpells.isEmpty() && buffSpells.isEmpty()) return false;

        // Already busy casting something else - let it finish.
        if (merc.isCasting()) return false;

        LivingEntity candidate = merc.getSupportTarget();
        if (candidate == null || !candidate.isAlive()) return false;

        if (!Utils.shouldHealEntity(merc, candidate)) return false;

        if (candidate.getHealth() >= candidate.getMaxHealth() * HEAL_THRESHOLD) return false;

        this.target = candidate;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        // One cast per activation. Once it's been attempted and the cast has resolved, release the flags so the attack goal can run again.
        // Covers the failed-cast case too, which would otherwise loop forever holding MOVE and LOOK.
        if (attemptedCast && !merc.isCasting()) return false;

        if (merc.isStunned() || merc.isSittingInChair() || merc.isInMenuState()) return false;
        if (target == null || !target.isAlive()) return false;

        // Someone else topped them up, or our own heal landed.
        return target.getHealth() < target.getMaxHealth() * HEAL_THRESHOLD;
    }

    @Override
    public void start() {
        super.start();
        this.attackTime = 0;
        this.attemptedCast = false;
    }

    @Override
    public void stop() {
        super.stop();
        this.cooldown = COOLDOWN_AFTER_CAST;
        this.attemptedCast = false;
    }

    @Override
    protected void doSpellAction() {
        AbstractSpell spell = getNextSpellType();

        int spellLevel = Math.max(1, (int) (spell.getMaxLevel()
                * Mth.lerp(merc.getRandom().nextFloat(), minSpellQuality, maxSpellQuality)));

        if (!spell.shouldAIStopCasting(spellLevel, merc, target)) {
            merc.initiateCastSpell(spell, spellLevel);
        }

        // Set regardless of whether the cast went through - a refused cast must still end the activation, or the goal spins holding MOVE and LOOK.
        this.attemptedCast = true;
        merc.setSupportTarget(null);
    }
}
