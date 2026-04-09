package net.alshanex.magic_realms.entity;

import net.alshanex.magic_realms.block.ChairBlockSimple;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class SeatEntity extends Entity {

    private BlockPos chairPos;

    public SeatEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public void setChairPos(BlockPos pos) {
        this.chairPos = pos;
    }

    public BlockPos getChairPos() {
        return this.chairPos;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            // Discard if no passenger
            if (!this.isVehicle()) {
                this.discard();
                return;
            }

            // Discard if the chair block was broken
            if (chairPos != null) {
                if (!(this.level().getBlockState(chairPos).getBlock() instanceof ChairBlockSimple)) {
                    this.ejectPassengers();
                    this.discard();
                }
            }
        }
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity entity, EntityDimensions dimensions, float partialTick) {
        return new Vec3(0.0, 0.0, 0.0);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean saveAsPassenger(CompoundTag compound) {
        return false;
    }

    @Override
    public boolean save(CompoundTag compound) {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
    }

    // Utility to check if a chair block already has a seat
    public static boolean isOccupied(Level level, BlockPos chairPos) {
        List<SeatEntity> seats = level.getEntitiesOfClass(SeatEntity.class,
                new net.minecraft.world.phys.AABB(chairPos).inflate(0.5),
                seat -> chairPos.equals(seat.getChairPos()));
        return !seats.isEmpty();
    }

    // Utility to remove all seats at a given chair position
    public static void removeSeatsAt(Level level, BlockPos chairPos) {
        List<SeatEntity> seats = level.getEntitiesOfClass(SeatEntity.class,
                new net.minecraft.world.phys.AABB(chairPos).inflate(0.5),
                seat -> chairPos.equals(seat.getChairPos()));
        for (SeatEntity seat : seats) {
            seat.ejectPassengers();
            seat.discard();
        }
    }
}
