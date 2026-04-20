package net.alshanex.magic_realms.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

public class ChairSittingData {
    public static final Codec<ChairSittingData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BlockPos.CODEC.fieldOf("chair_position").forGetter(d -> d.chairPosition),
                    Codec.INT.fieldOf("sitting_time").forGetter(d -> d.sittingTime),
                    Codec.INT.fieldOf("sit_cooldown").forGetter(d -> d.sitCooldown)
            ).apply(instance, ChairSittingData::new)
    );

    private BlockPos chairPosition;
    private int sittingTime;
    private int sitCooldown;

    public ChairSittingData() {
        this.chairPosition = BlockPos.ZERO;
        this.sittingTime = 0;
        this.sitCooldown = 0;
    }

    private ChairSittingData(BlockPos chairPosition, int sittingTime, int sitCooldown) {
        this.chairPosition = chairPosition;
        this.sittingTime = sittingTime;
        this.sitCooldown = sitCooldown;
    }

    public BlockPos getChairPosition() { return chairPosition; }
    public void setChairPosition(BlockPos pos) { this.chairPosition = pos; }

    public int getSittingTime() { return sittingTime; }
    public void setSittingTime(int time) { this.sittingTime = time; }
    public void incrementSittingTime() { this.sittingTime++; }

    public int getSitCooldown() { return sitCooldown; }
    public void setSitCooldown(int cd) { this.sitCooldown = cd; }
    public void tickCooldown() { if (sitCooldown > 0) sitCooldown--; }

    public void reset() {
        this.chairPosition = BlockPos.ZERO;
        this.sittingTime = 0;
    }
}
