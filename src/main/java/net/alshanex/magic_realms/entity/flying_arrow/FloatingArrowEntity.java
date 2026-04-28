package net.alshanex.magic_realms.entity.flying_arrow;

import net.alshanex.magic_realms.registry.MREntityRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;

/**
 * A persistent "telekinetic arrow" entity that follows its owner's look direction.
 * <p>
 * Modes (driven by the controlling item via {@link #setMode(byte)}):
 * <ul>
 *     <li>{@link #MODE_IDLE} (0) — hovers at the top of the owner's head.</li>
 *     <li>{@link #MODE_FORWARD} (1) — flies forward along the owner's look vector,
 *         while the "anchor" tracks the crosshair so the arrow gently steers.</li>
 *     <li>{@link #MODE_BACKWARD} (2) — same as forward but inverted velocity.</li>
 * </ul>
 * The arrow does not despawn on contact — it sweeps through anything in its path, dealing damage to any {@link LivingEntity} other than the owner, with a small
 * per-victim cooldown so it doesn't shred them every tick.
 */
public class FloatingArrowEntity extends Entity {

    public static final byte MODE_IDLE = 0;
    public static final byte MODE_FORWARD = 1;
    public static final byte MODE_BACKWARD = 2;
    public static final byte MODE_HOLD = 3;

    private static final EntityDataAccessor<Byte> DATA_MODE =
            SynchedEntityData.defineId(FloatingArrowEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> DATA_RENDER_YAW =
            SynchedEntityData.defineId(FloatingArrowEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_RENDER_PITCH =
            SynchedEntityData.defineId(FloatingArrowEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_ID =
            SynchedEntityData.defineId(FloatingArrowEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    /** Distance the arrow holds in front of the player when flying. */
    public static final double FORWARD_REACH = 30.0;
    /** How quickly the arrow lerps toward its target position (0..1 per tick). */
    private static final double FOLLOW_LERP = 0.1;
    /** How quickly the arrow returns to the idle position. */
    private static final double IDLE_LERP = 0.1;
    /** Damage per hit. */
    private static final float DAMAGE = 4.0f;
    /** Ticks an entity is immune after being hit by this arrow (so we don't damage every tick). */
    private static final int HIT_COOLDOWN_TICKS = 10;
    /** Sweep radius for hit detection. */
    private static final double HIT_RADIUS = 0.4;

    private double holdRadius = -1.0;

    // Per-victim hit cooldowns (server-side only).
    private final Map<UUID, Integer> hitCooldowns = new HashMap<>();

    @Nullable
    private Player cachedOwner;

    public FloatingArrowEntity(EntityType<? extends FloatingArrowEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true; // never collide with blocks
    }

    public FloatingArrowEntity(Level level, Player owner) {
        this(MREntityRegistry.FLOATING_ARROW.get(), level);
        this.cachedOwner = owner;
        this.entityData.set(DATA_OWNER_ID, Optional.of(owner.getUUID()));
        Vec3 idle = computeIdlePos(owner);
        this.setPos(idle.x, idle.y, idle.z);
        this.xo = idle.x; this.yo = idle.y; this.zo = idle.z;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_MODE, MODE_IDLE);
        builder.define(DATA_RENDER_YAW, 0f);
        builder.define(DATA_RENDER_PITCH, 0f);
        builder.define(DATA_OWNER_ID, Optional.empty());
    }

    public byte getMode() {
        return this.entityData.get(DATA_MODE);
    }

    public void setMode(byte mode) {
        this.entityData.set(DATA_MODE, mode);
    }

    public float getRenderYaw() {
        return this.entityData.get(DATA_RENDER_YAW);
    }

    public float getRenderPitch() {
        return this.entityData.get(DATA_RENDER_PITCH);
    }

    @Nullable
    public UUID getOwnerUUID() {
        return this.entityData.get(DATA_OWNER_ID).orElse(null);
    }

    @Nullable
    public Player getOwnerPlayer() {
        if (cachedOwner != null && !cachedOwner.isRemoved()) return cachedOwner;
        UUID id = getOwnerUUID();
        if (id == null) return null;
        Player p = this.level().getPlayerByUUID(id);
        if (p != null) cachedOwner = p;
        return p;
    }

    /** Idle hover position: top of the player's head, slightly above. */
    private static Vec3 computeIdlePos(Player owner) {
        return new Vec3(owner.getX(), owner.getEyeY() + 0.9, owner.getZ());
    }

    @Override
    public void tick() {
        super.tick();

        Player owner = getOwnerPlayer();
        if (owner == null || !owner.isAlive()) {
            // No valid owner — drift to a stop, and on server, remove ourselves.
            if (!this.level().isClientSide) {
                this.discard();
            }
            return;
        }

        if (!this.level().isClientSide) {
            byte currentMode = getMode();
            if (currentMode == MODE_HOLD && holdRadius < 0) {
                // Just entered HOLD — snapshot current distance from the player's eye.
                Vec3 eye = owner.getEyePosition();
                holdRadius = this.position().distanceTo(eye);
                // Clamp to a sensible range so weird edge cases don't break things.
                holdRadius = Mth.clamp(holdRadius, 1.0, FORWARD_REACH);
            } else if (currentMode != MODE_HOLD) {
                // Reset so the next HOLD entry captures a fresh radius.
                holdRadius = -1.0;
            }
        }

        Vec3 currentPos = this.position();
        Vec3 targetPos;
        Vec3 movementDir;

        byte mode = getMode();
        Vec3 look = owner.getLookAngle();

        switch (mode) {
            case MODE_FORWARD -> {
                Vec3 eye = owner.getEyePosition();
                targetPos = eye.add(look.scale(FORWARD_REACH));
                movementDir = look;
            }
            case MODE_BACKWARD -> {
                Vec3 playerPos = new Vec3(owner.getX(), owner.getEyeY(), owner.getZ());
                // If the arrow is essentially on top of the player, treat it like idle to prevent jitter.
                if (currentPos.distanceToSqr(playerPos) < 1.0) {
                    targetPos = computeIdlePos(owner);
                } else {
                    targetPos = playerPos;
                }
                Vec3 toPlayer = playerPos.subtract(currentPos);
                movementDir = toPlayer.lengthSqr() > 1.0E-6 ? toPlayer.normalize() : look.scale(-1);
            }
            case MODE_IDLE -> {
                // Recall: hover above head.
                targetPos = computeIdlePos(owner);
                movementDir = look;
            }
            default -> {
                // MODE_HOLD: orbit the player at the captured radius, tracking the crosshair.
                Vec3 eye = owner.getEyePosition();
                double radius = holdRadius > 0 ? holdRadius : 3.0;
                targetPos = eye.add(look.scale(radius));
                movementDir = look;
            }
        }

        double lerp = (mode == MODE_IDLE) ? IDLE_LERP : FOLLOW_LERP;
        double newX = Mth.lerp(lerp, currentPos.x, targetPos.x);
        double newY = Mth.lerp(lerp, currentPos.y, targetPos.y);
        double newZ = Mth.lerp(lerp, currentPos.z, targetPos.z);

        Vec3 newPos = new Vec3(newX, newY, newZ);
        Vec3 actualMotion = newPos.subtract(currentPos);

        // Sweep-damage anything between currentPos and newPos
        if (!this.level().isClientSide) {
            doSweepDamage(currentPos, newPos, owner);
            tickHitCooldowns();
        }

        this.setPos(newX, newY, newZ);
        this.setDeltaMovement(actualMotion);

        Vec3 facing;
        if (actualMotion.lengthSqr() > 1.0E-4) {
            facing = actualMotion.normalize();
        } else {
            facing = movementDir.lengthSqr() > 1.0E-6 ? movementDir.normalize() : new Vec3(0, 0, 1);
        }

        if (!this.level().isClientSide) {
            float yaw = (float) (Mth.atan2(-facing.x, facing.z) * (180.0 / Math.PI));
            float horiz = (float) Math.sqrt(facing.x * facing.x + facing.z * facing.z);
            float pitch = (float) (-Mth.atan2(facing.y, horiz) * (180.0 / Math.PI));
            this.entityData.set(DATA_RENDER_YAW, yaw);
            this.entityData.set(DATA_RENDER_PITCH, pitch);
            // Also stamp the entity's actual rotation so that any vanilla logic relying on it works.
            this.setYRot(yaw);
            this.setXRot(pitch);
        }
    }

    /** Damages every LivingEntity (other than the owner) intersected by the swept AABB. */
    private void doSweepDamage(Vec3 from, Vec3 to, Player owner) {
        // Build a swept AABB around the segment from->to, expanded by HIT_RADIUS.
        AABB sweep = new AABB(
                Math.min(from.x, to.x) - HIT_RADIUS,
                Math.min(from.y, to.y) - HIT_RADIUS,
                Math.min(from.z, to.z) - HIT_RADIUS,
                Math.max(from.x, to.x) + HIT_RADIUS,
                Math.max(from.y, to.y) + HIT_RADIUS,
                Math.max(from.z, to.z) + HIT_RADIUS
        );

        List<LivingEntity> candidates = this.level().getEntitiesOfClass(
                LivingEntity.class, sweep, e -> e != owner && e.isAlive() && !e.isSpectator());

        if (candidates.isEmpty()) return;

        DamageSource source = damageSources().source(DamageTypes.MAGIC, this, owner);

        for (LivingEntity victim : candidates) {
            UUID id = victim.getUUID();
            if (hitCooldowns.containsKey(id)) continue; // recently hit, skip
            victim.hurt(source, DAMAGE);
            hitCooldowns.put(id, HIT_COOLDOWN_TICKS);
        }
    }

    private void tickHitCooldowns() {
        if (hitCooldowns.isEmpty()) return;
        Set<UUID> expired = new HashSet<>();
        for (var e : hitCooldowns.entrySet()) {
            int remaining = e.getValue() - 1;
            if (remaining <= 0) expired.add(e.getKey());
            else e.setValue(remaining);
        }
        expired.forEach(hitCooldowns::remove);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distSqr) {
        return distSqr < (64 * 64);
    }

    @Override
    public boolean isPickable() {
        return false; // can't be hit/clicked by the player
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
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean isInvulnerable() {
        return true; // can't be killed by anything
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.entityData.set(DATA_OWNER_ID, Optional.of(tag.getUUID("Owner")));
        }
        this.entityData.set(DATA_MODE, tag.getByte("Mode"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        UUID id = getOwnerUUID();
        if (id != null) tag.putUUID("Owner", id);
        tag.putByte("Mode", getMode());
    }
}
