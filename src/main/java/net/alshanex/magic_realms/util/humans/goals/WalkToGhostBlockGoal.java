package net.alshanex.magic_realms.util.humans.goals;

import net.alshanex.magic_realms.registry.MRBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public class WalkToGhostBlockGoal extends MoveToBlockGoal {
    private final PathfinderMob mob;

    public WalkToGhostBlockGoal(PathfinderMob mob, double speedModifier, int range) {
        super(mob, speedModifier, range);
        this.mob = mob;
    }

    @Override
    protected boolean isValidTarget(LevelReader level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(MRBlocks.GHOST_TARGET_BLOCK.get());
    }
}
