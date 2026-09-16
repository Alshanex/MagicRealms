package net.alshanex.magic_realms.util.humans.combat;

import net.alshanex.magic_realms.entity.humans.AbstractMercenaryEntity;
import net.alshanex.magic_realms.util.humans.goals.battle_goals.BattlefieldAnalysis;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;

/** Reusable target-selection routines shared by more than one combat class. */
public final class TargetingStrategies {

    private TargetingStrategies() {}

    /**
     * Isolated low-health targets, hordes only. Used by unshielded warriors and assassins.
     * Retargets only when the pick is notably better, to prevent thrashing.
     */
    @Nullable
    public static LivingEntity skirmisher(AbstractMercenaryEntity self, BattlefieldAnalysis battlefield) {
        if (!battlefield.isHordeEngagement()) return null;

        LivingEntity isolated = battlefield.findMostIsolatedTarget(BattlefieldAnalysis.SCAN_RADIUS);
        if (isolated == null) return null;

        LivingEntity current = self.getTarget();
        if (current == null || isolated.getHealth() < current.getHealth() * 0.8f) {
            return isolated;
        }
        return null;
    }
}
