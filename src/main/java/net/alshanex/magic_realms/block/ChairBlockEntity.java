package net.alshanex.magic_realms.block;

import net.alshanex.magic_realms.registry.MRBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ChairBlockEntity extends BlockEntity {
    private int tickCounter = 0;

    public ChairBlockEntity(BlockPos pos, BlockState blockState) {
        super(MRBlockEntities.CHAIR.get(), pos, blockState);
    }

    public void tick() {
        if (level instanceof ServerLevel serverLevel) {
            tickCounter++;

            // Try spawn logic every second (20 ticks)
            if (tickCounter >= 20) {
                tickCounter = 0;

                if (getBlockState().getBlock() instanceof ChairBlock chairBlock) {
                    chairBlock.tickSpawnLogic(serverLevel, getBlockPos(), getBlockState());
                }
            }
        }
    }
}