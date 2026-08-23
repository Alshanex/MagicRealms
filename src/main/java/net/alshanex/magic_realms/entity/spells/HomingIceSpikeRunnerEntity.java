package net.alshanex.magic_realms.entity.spells;

import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.spells.ice_spike.IceSpikeEntity;
import net.alshanex.magic_realms.registry.MREntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

public class HomingIceSpikeRunnerEntity extends Entity {

    // Persisted state (saved to NBT)

    @Nullable private UUID casterUUID;
    @Nullable private UUID targetUUID;
    private float damage = 5f;
    private int maxSpikes = 10;
    private float speed = 0.55f; // blocks per tick
    private int maxLifetimeTicks = 120; // ~6 seconds
    private float turnSharpness = 0.5f; // 0 = straight line, 1 = instant snap to target

    // Transient runtime state

    @Nullable private LivingEntity cachedCaster;
    @Nullable private LivingEntity cachedTarget;
    private int ticksAlive = 0;
    private int spikesSpawned = 0;
    private int spikeSpawnInterval = 3;    // spawn a spike every N ticks
    private Vec3 currentDirection = Vec3.ZERO;
    private boolean hasInitializedDirection = false;

    // Spike size config

    private static final float MIN_SPIKE_SCALE = 1.0f;
    private static final float MAX_SPIKE_SCALE = 3.0f;
    private static final float FINAL_SPIKE_SCALE = 4.5f;
    private static final float HALF_DAMAGE_MULTIPLIER = 0.5f;
    private static final double HIT_DISTANCE = 2.0;
    private static final float CLOSE_RANGE_BOOST = 5.0f; // distance at which acceleration kicks in
    private static final float MAX_SPEED_MULTIPLIER = 2.0f;


    public HomingIceSpikeRunnerEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;  // no collision
        this.setInvisible(true);
    }

    public HomingIceSpikeRunnerEntity(Level level,
                                      LivingEntity caster, LivingEntity target) {
        this(MREntityRegistry.HOMING_ICE_SPIKE_RUNNER.get(), level);
        setCaster(caster);
        setTarget(target);
    }

    //  Setup helpers

    /**
     * Full setup method — call after construction, before adding to the level.
     */
    public void setup(LivingEntity caster, LivingEntity target,
                      float damage, int maxSpikes, float speed, int maxLifetimeTicks) {
        setCaster(caster);
        setTarget(target);
        this.damage = damage;
        this.maxSpikes = maxSpikes;
        this.speed = speed;
        this.maxLifetimeTicks = maxLifetimeTicks;
    }

    public void setCaster(LivingEntity caster) {
        this.cachedCaster = caster;
        this.casterUUID = caster.getUUID();
    }

    public void setTarget(LivingEntity target) {
        this.cachedTarget = target;
        this.targetUUID = target.getUUID();
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setMaxSpikes(int maxSpikes) {
        this.maxSpikes = maxSpikes;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public void setMaxLifetimeTicks(int maxLifetimeTicks) {
        this.maxLifetimeTicks = maxLifetimeTicks;
    }

    public void setTurnSharpness(float turnSharpness) {
        this.turnSharpness = Mth.clamp(turnSharpness, 0f, 1f);
    }

    public void setSpikeSpawnInterval(int interval) {
        this.spikeSpawnInterval = Math.max(1, interval);
    }


    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {

    }


    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) return;

        ticksAlive++;

        LivingEntity target = getOrResolveTarget();
        LivingEntity caster = getOrResolveCaster();

        if (target == null || !target.isAlive() || ticksAlive > maxLifetimeTicks) {
            spawnSpikeAt(position(), true, caster);
            discard();
            return;
        }

        // Compute homing direction — pure chase, always aim at target
        Vec3 desiredDirection = target.position().subtract(position())
                .multiply(1, 0, 1)
                .normalize();

        // Move along the ground — accelerate when closing in
        double dist = horizontalDistanceTo(target);
        float currentSpeed = speed;
        if (dist < CLOSE_RANGE_BOOST) {
            float closeness = 1f - (float) (dist / CLOSE_RANGE_BOOST);
            currentSpeed = Mth.lerp(closeness, speed, speed * MAX_SPEED_MULTIPLIER);
        }
        float currentTurnSharpness = turnSharpness;
        if (dist < CLOSE_RANGE_BOOST) {
            currentTurnSharpness = Mth.lerp(1f - (float)(dist / CLOSE_RANGE_BOOST), turnSharpness, 1.0f);
        }

        if (!hasInitializedDirection) {
            currentDirection = desiredDirection;
            hasInitializedDirection = true;
        } else {
            currentDirection = lerpDirection(currentDirection, desiredDirection, currentTurnSharpness);
        }

        Vec3 prevPos = position();
        Vec3 nextPos = prevPos.add(currentDirection.scale(currentSpeed));
        nextPos = Utils.moveToRelativeGroundLevel(level(), nextPos, 8);
        setPos(nextPos);

        Vec3 targetPos = target.position();
        if (closestDistanceOnSegmentXZ(prevPos, nextPos, targetPos) < HIT_DISTANCE) {
            spawnSpikeAt(targetPos, true, caster);
            discard();
            return;
        }

        // Spawn trailing spikes at intervals
        if (ticksAlive % spikeSpawnInterval == 0 && spikesSpawned < maxSpikes) {
            spawnSpikeAt(position(), false, caster);
            spikesSpawned++;
        }
    }

    //  Spike spawning

    private void spawnSpikeAt(Vec3 pos, boolean isFinal, LivingEntity caster) {
        if (level().isClientSide) return;

        BlockPos below = BlockPos.containing(pos).below();
        if (!level().getBlockState(below).isFaceSturdy(level(), below, Direction.UP)) {
            return; // no solid ground — skip this spike
        }

        IceSpikeEntity spike = new IceSpikeEntity(level(), caster);
        // Scale interpolates from small to large as more spikes are spawned
        float scale;
        if (isFinal) {
            scale = FINAL_SPIKE_SCALE;
        } else {
            float progress = maxSpikes > 1 ? spikesSpawned / (float) (maxSpikes - 1) : 1f;
            scale = Mth.lerp(progress, MIN_SPIKE_SCALE, MAX_SPIKE_SCALE);
        }
        spike.setSpikeSize(scale);
        spike.moveTo(pos.add(0, 0.1, 0));
        spike.setDamage(damage * (isFinal ? 1f : HALF_DAMAGE_MULTIPLIER));

        // Random rotation for visual variety
        float baseYaw = (float) (Mth.atan2(currentDirection.z, currentDirection.x) * Mth.RAD_TO_DEG) - 90f;
        spike.setYRot(baseYaw + Utils.random.nextIntBetweenInclusive(-25, 25));
        spike.setXRot(Utils.random.nextIntBetweenInclusive(-15, 15));

        // Alternate silence for sound variety
        if (spikesSpawned % 2 == 0) {
            spike.setSilent(true);
        }

        level().addFreshEntity(spike);
    }

    //  Direction interpolation

    /**
     * Smoothly interpolates between two horizontal direction vectors.
     * This creates the curving/arcing visual as the spikes chase the target.
     */
    private Vec3 lerpDirection(Vec3 current, Vec3 desired, float sharpness) {
        double x = Mth.lerp(sharpness, current.x, desired.x);
        double z = Mth.lerp(sharpness, current.z, desired.z);
        Vec3 result = new Vec3(x, 0, z);
        double length = result.horizontalDistance();
        if (length < 0.001) {
            return current; // avoid zero-length direction
        }
        return result.normalize();
    }

    @Nullable
    private LivingEntity getOrResolveTarget() {
        if (cachedTarget != null && cachedTarget.isAlive()) return cachedTarget;
        cachedTarget = resolveEntity(targetUUID);
        return cachedTarget;
    }

    @Nullable
    private LivingEntity getOrResolveCaster() {
        if (cachedCaster != null && cachedCaster.isAlive()) return cachedCaster;
        cachedCaster = resolveEntity(casterUUID);
        return cachedCaster;
    }

    @Nullable
    private LivingEntity resolveEntity(@Nullable UUID uuid) {
        if (uuid == null || !(level() instanceof ServerLevel serverLevel)) return null;
        Entity entity = serverLevel.getEntity(uuid);
        return entity instanceof LivingEntity living ? living : null;
    }

    //  NBT save / load

    private static final String TAG_CASTER = "CasterUUID";
    private static final String TAG_TARGET = "TargetUUID";
    private static final String TAG_DAMAGE = "Damage";
    private static final String TAG_MAX_SPIKES = "MaxSpikes";
    private static final String TAG_SPEED = "Speed";
    private static final String TAG_MAX_LIFETIME = "MaxLifetime";
    private static final String TAG_TURN_SHARPNESS = "TurnSharpness";
    private static final String TAG_TICKS_ALIVE = "TicksAlive";
    private static final String TAG_SPIKES_SPAWNED = "SpikesSpawned";
    private static final String TAG_SPAWN_INTERVAL = "SpikeSpawnInterval";
    private static final String TAG_DIRECTION_X = "DirX";
    private static final String TAG_DIRECTION_Z = "DirZ";

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID(TAG_CASTER)) this.casterUUID = tag.getUUID(TAG_CASTER);
        if (tag.hasUUID(TAG_TARGET)) this.targetUUID = tag.getUUID(TAG_TARGET);
        this.damage = tag.getFloat(TAG_DAMAGE);
        this.maxSpikes = tag.getInt(TAG_MAX_SPIKES);
        this.speed = tag.getFloat(TAG_SPEED);
        this.maxLifetimeTicks = tag.getInt(TAG_MAX_LIFETIME);
        this.turnSharpness = tag.getFloat(TAG_TURN_SHARPNESS);
        this.ticksAlive = tag.getInt(TAG_TICKS_ALIVE);
        this.spikesSpawned = tag.getInt(TAG_SPIKES_SPAWNED);
        this.spikeSpawnInterval = tag.getInt(TAG_SPAWN_INTERVAL);

        if (tag.contains(TAG_DIRECTION_X)) {
            this.currentDirection = new Vec3(tag.getDouble(TAG_DIRECTION_X), 0, tag.getDouble(TAG_DIRECTION_Z));
            this.hasInitializedDirection = true;
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (casterUUID != null) tag.putUUID(TAG_CASTER, casterUUID);
        if (targetUUID != null) tag.putUUID(TAG_TARGET, targetUUID);
        tag.putFloat(TAG_DAMAGE, damage);
        tag.putInt(TAG_MAX_SPIKES, maxSpikes);
        tag.putFloat(TAG_SPEED, speed);
        tag.putInt(TAG_MAX_LIFETIME, maxLifetimeTicks);
        tag.putFloat(TAG_TURN_SHARPNESS, turnSharpness);
        tag.putInt(TAG_TICKS_ALIVE, ticksAlive);
        tag.putInt(TAG_SPIKES_SPAWNED, spikesSpawned);
        tag.putInt(TAG_SPAWN_INTERVAL, spikeSpawnInterval);

        if (hasInitializedDirection) {
            tag.putDouble(TAG_DIRECTION_X, currentDirection.x);
            tag.putDouble(TAG_DIRECTION_Z, currentDirection.z);
        }
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    /**
     * Horizontal distance to another entity - used for speed ramping.
     */
    private double horizontalDistanceTo(Entity other) {
        double dx = this.getX() - other.getX();
        double dz = this.getZ() - other.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Returns the closest horizontal (XZ) distance from point P to the line segment [A, B].
     */
    private static double closestDistanceOnSegmentXZ(Vec3 a, Vec3 b, Vec3 p) {
        double ax = a.x, az = a.z;
        double bx = b.x, bz = b.z;
        double px = p.x, pz = p.z;

        double abx = bx - ax;
        double abz = bz - az;
        double apx = px - ax;
        double apz = pz - az;

        double lengthSq = abx * abx + abz * abz;
        if (lengthSq < 1e-8) {
            // A and B are the same point
            return Math.sqrt(apx * apx + apz * apz);
        }

        // Project P onto AB, clamped to [0, 1] so we stay on the segment
        double t = Mth.clamp((apx * abx + apz * abz) / lengthSq, 0.0, 1.0);

        double closestX = ax + t * abx;
        double closestZ = az + t * abz;

        double dx = px - closestX;
        double dz = pz - closestZ;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
