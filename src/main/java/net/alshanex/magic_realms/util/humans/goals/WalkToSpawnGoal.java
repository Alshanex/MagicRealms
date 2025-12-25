package net.alshanex.magic_realms.util.humans.goals;

import net.alshanex.magic_realms.entity.tavernkeep.TavernKeeperEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.EnumSet;

public class WalkToSpawnGoal extends Goal {
    private final TavernKeeperEntity mob;
    private final double speedModifier;
    private final double maxDistance;

    public WalkToSpawnGoal(TavernKeeperEntity mob, double speedModifier, double maxDistance) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.maxDistance = maxDistance;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        BlockPos spawnPos = this.mob.getSpawnPos();

        if (spawnPos == null) return false;

        double distanceSq = this.mob.distanceToSqr(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());

        boolean isNear = distanceSq <= (this.maxDistance * this.maxDistance);
        boolean isAlreadyThere = distanceSq < 1.0;

        return isNear && !isAlreadyThere;
    }

    @Override
    public void start() {
        BlockPos pos = this.mob.getSpawnPos();
        if (pos != null) {
            this.mob.getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), this.speedModifier);
        }
    }

    @Override
    public boolean canContinueToUse() {
        BlockPos pos = this.mob.getSpawnPos();
        if (pos == null) return false;

        double distanceSq = this.mob.distanceToSqr(pos.getX(), pos.getY(), pos.getZ());

        return distanceSq > 1.0 && distanceSq <= (this.maxDistance * this.maxDistance) && !this.mob.getNavigation().isDone();
    }
}
