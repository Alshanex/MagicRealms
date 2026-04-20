package net.alshanex.magic_realms.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

public class PatrolData {
    public static final Codec<PatrolData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BlockPos.CODEC.fieldOf("patrol_position").forGetter(d -> d.patrolPosition)
            ).apply(instance, PatrolData::new)
    );

    private BlockPos patrolPosition;

    public PatrolData() {
        this.patrolPosition = BlockPos.ZERO;
    }

    private PatrolData(BlockPos patrolPosition) {
        this.patrolPosition = patrolPosition;
    }

    public BlockPos getPatrolPosition() { return patrolPosition; }
    public void setPatrolPosition(BlockPos pos) { this.patrolPosition = pos; }
    public boolean hasPatrolPosition() {
        return patrolPosition != null && !patrolPosition.equals(BlockPos.ZERO);
    }
}
