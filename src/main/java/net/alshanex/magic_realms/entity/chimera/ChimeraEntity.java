package net.alshanex.magic_realms.entity.chimera;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.util.CameraShakeData;
import io.redspace.ironsspellbooks.api.util.CameraShakeManager;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.alshanex.magic_realms.data.ChimeraMobDefinition;
import net.alshanex.magic_realms.data.ChimeraSlot;
import net.alshanex.magic_realms.network.SyncChimeraAssemblyPayload;
import net.alshanex.magic_realms.registry.ChimeraPartRegistry;
import net.alshanex.magic_realms.util.chimera.ChimeraAssembly;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The Chimera entity - a Frankenstein's monster assembled from parts of other mobs.
 * <p>
 * Instead of dying, the chimera becomes "downed": its parts scatter on the ground and it remains invulnerable in that state. A player can right-click it to
 * reassemble (revive) it.
 * <p>
 * Animation states:
 * - SPAWNING: parts float from ground to correct positions (invulnerable)
 * - NORMAL: idle/walk/attack (vulnerable)
 * - SCATTERING: parts fall to ground (invulnerable, triggered when HP reaches 0)
 * - DOWNED: parts lie scattered on ground (invulnerable, waiting for interaction)
 * - REASSEMBLING: parts float back up (invulnerable, triggered by right-click)
 */
public class ChimeraEntity extends PathfinderMob implements GeoEntity {

    // GeckoLib
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // Animation timing (ticks)
    public static final int SPAWN_ANIM_LENGTH_TICKS = 60; // 3 seconds
    public static final int SCATTER_ANIM_LENGTH_TICKS = 24; // 1 second more or less
    public static final int REASSEMBLE_ANIM_LENGTH_TICKS = 60; // 3 seconds

    // Attack animation duration: 1.0s = 20 ticks
    public static final int ATTACK_ANIM_LENGTH_TICKS = 20;

    // Synched Data
    private static final EntityDataAccessor<CompoundTag> DATA_ASSEMBLY =
            SynchedEntityData.defineId(ChimeraEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Byte> DATA_ANIM_STATE =
            SynchedEntityData.defineId(ChimeraEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> DATA_ENTITY_SCALE =
            SynchedEntityData.defineId(ChimeraEntity.class, EntityDataSerializers.FLOAT);

    // Animation state enum encoded as byte
    public static final byte STATE_NORMAL = 0;
    public static final byte STATE_SPAWNING = 1;
    public static final byte STATE_SCATTERING = 2;
    public static final byte STATE_DOWNED = 3;
    public static final byte STATE_REASSEMBLING = 4;

    // Assembly
    private ChimeraAssembly assembly = new ChimeraAssembly();
    private ChimeraAssembly.ComputedAttributes cachedAttributes;

    // Dimensions
    public static final float BASE_WIDTH = 0.9f;
    public static final float BASE_HEIGHT = 2.0f;
    private static final float LEG_HEIGHT_FRACTION = 12.0f / 32.0f;
    private float effectiveHeight = BASE_HEIGHT;

    // Tick within the attack animation when the slam connects
    public static final int ATTACK_DAMAGE_DELAY_TICKS = 12;

    // Animation tick counters
    private int animationTick = 0;
    private int attackAnimationTick = 0;
    private boolean hasFinishedSpawn = false;

    // Delayed attack: store the target, deal damage when slam lands
    @Nullable
    private Entity pendingAttackTarget = null;
    private int pendingAttackTick = 0;

    // Health stored before downed state so we can restore it
    private float healthBeforeDown = 0f;

    // Flags
    private boolean suppressPacketSync = false;

    public ChimeraEntity(EntityType<? extends ChimeraEntity> type, Level level) {
        super(type, level);
    }

    //  GeckoLib Animation Controllers

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::mainAnimationPredicate));
        controllers.add(new AnimationController<>(this, "attack", 3, this::attackAnimationPredicate));
    }

    private PlayState mainAnimationPredicate(AnimationState<ChimeraEntity> state) {
        byte animState = getAnimState();

        switch (animState) {
            case STATE_SPAWNING -> {
                state.getController().setAnimation(
                        RawAnimation.begin().thenPlay("animation.chimera.spawn"));
                return PlayState.CONTINUE;
            }
            case STATE_SCATTERING -> {
                state.getController().setAnimation(
                        RawAnimation.begin().thenPlay("animation.chimera.scatter"));
                return PlayState.CONTINUE;
            }
            case STATE_DOWNED -> {
                state.getController().setAnimation(
                        RawAnimation.begin().thenPlayAndHold("animation.chimera.scattered_idle"));
                return PlayState.CONTINUE;
            }
            case STATE_REASSEMBLING -> {
                state.getController().setAnimation(
                        RawAnimation.begin().thenPlay("animation.chimera.reassemble"));
                return PlayState.CONTINUE;
            }
            default -> {
                // STATE_NORMAL
                if (state.isMoving()) {
                    state.getController().setAnimation(
                            RawAnimation.begin().thenLoop("animation.chimera.walk"));
                    return PlayState.CONTINUE;
                }
                state.getController().setAnimation(
                        RawAnimation.begin().thenLoop("animation.chimera.idle"));
                return PlayState.CONTINUE;
            }
        }
    }

    private PlayState attackAnimationPredicate(AnimationState<ChimeraEntity> state) {
        if (this.attackAnimationTick > 0 && getAnimState() == STATE_NORMAL) {
            state.getController().setAnimation(
                    RawAnimation.begin().thenPlay("animation.chimera.attack"));
            return PlayState.CONTINUE;
        }
        state.getController().forceAnimationReset();
        return PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    //  Animation State Helpers

    public byte getAnimState() {
        return this.entityData.get(DATA_ANIM_STATE);
    }

    private void setAnimState(byte state) {
        this.entityData.set(DATA_ANIM_STATE, state);
    }

    public boolean isSpawning() {
        return getAnimState() == STATE_SPAWNING;
    }

    public boolean isScattering() {
        return getAnimState() == STATE_SCATTERING;
    }

    public boolean isDowned() {
        return getAnimState() == STATE_DOWNED;
    }

    public boolean isReassembling() {
        return getAnimState() == STATE_REASSEMBLING;
    }

    /**
     * Whether the entity is in any animation state that should prevent interaction/damage.
     */
    public boolean isInAnimationState() {
        byte state = getAnimState();
        return state != STATE_NORMAL;
    }

    /**
     * Whether the entity is playing a transition animation (not idle states).
     */
    public boolean isPlayingTransitionAnimation() {
        byte state = getAnimState();
        return state == STATE_SPAWNING || state == STATE_SCATTERING || state == STATE_REASSEMBLING;
    }

    //  Spawn Animation

    /**
     * Call this right after adding the entity to the world to trigger
     * the spawn animation. The entity is invulnerable during spawn.
     */
    public void triggerSpawnAnimation() {
        this.animationTick = SPAWN_ANIM_LENGTH_TICKS;
        this.hasFinishedSpawn = false;
        setAnimState(STATE_SPAWNING);
    }

    /**
     * Returns a 0→1 progress value for the spawn animation.
     */
    public float getSpawnProgress() {
        if (!isSpawning()) return 1.0f;
        return 1.0f - ((float) animationTick / SPAWN_ANIM_LENGTH_TICKS);
    }

    //  Scatter (replaces death)

    /**
     * Returns a 0→1 progress value for the scatter animation.
     */
    public float getScatterProgress() {
        if (!isScattering()) return 0.0f;
        return 1.0f - ((float) animationTick / SCATTER_ANIM_LENGTH_TICKS);
    }

    /**
     * Returns a 0→1 progress for the reassemble animation.
     */
    public float getReassembleProgress() {
        if (!isReassembling()) return 1.0f;
        return 1.0f - ((float) animationTick / REASSEMBLE_ANIM_LENGTH_TICKS);
    }

    @Override
    public void die(DamageSource damageSource) {
        // Instead of dying, enter the scatter/downed state
        if (!level().isClientSide && getAnimState() == STATE_NORMAL) {
            this.animationTick = SCATTER_ANIM_LENGTH_TICKS;
            setAnimState(STATE_SCATTERING);
            this.setHealth(this.getMaxHealth()); // Keep alive

            float explosionRadius = this.getBbWidth() * 2;
            MagicManager.spawnParticles(level(), ParticleHelper.BLOOD, this.getX(), this.getY() + .25f, this.getZ(), 100, .03, .4, .03, .4, true);
            MagicManager.spawnParticles(level(), ParticleHelper.BLOOD, this.getX(), this.getY() + .25f, this.getZ(), 100, .03, .4, .03, .4, false);
            MagicManager.spawnParticles(level(), new BlastwaveParticleOptions(SchoolRegistry.BLOOD.get().getTargetingColor(), explosionRadius), this.getX(), this.getBoundingBox().getCenter().y, this.getZ(), 1, 0, 0, 0, 0, true);

            CameraShakeManager.addCameraShake(new CameraShakeData(level(), 10, this.position(), 20));

            level().playSound(null, getX(), getY(), getZ(),
                    SoundRegistry.BLOOD_EXPLOSION, SoundSource.HOSTILE, 1.5f, 0.5f);
            return;
        }
        // Never actually call super.die() — the chimera doesn't truly die
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Invulnerable during any animation state or when downed
        if (isInAnimationState()) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean isInvulnerable() {
        return isInAnimationState() || super.isInvulnerable();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSqr) {
        // Always render within 96 blocks (vanilla default for large entities)
        // This prevents frustum culling from hiding scattered parts
        double renderDist = 96.0;
        return distanceSqr < renderDist * renderDist;
    }

    @Override
    public net.minecraft.world.phys.AABB getBoundingBoxForCulling() {
        if (isDowned() || isScattering() || isReassembling() || isSpawning()) {
            // Inflate by ~2 blocks in every direction to cover scattered parts
            return super.getBoundingBoxForCulling().inflate(3.0);
        }
        return super.getBoundingBoxForCulling();
    }

    //  Player Interaction (Right-click to revive)

    public void reassemble(){
        // Start reassembly animation
        this.animationTick = REASSEMBLE_ANIM_LENGTH_TICKS;
        setAnimState(STATE_REASSEMBLING);
        level().playSound(null, getX(), getY(), getZ(),
                SoundRegistry.HEARTSTOP_CAST, SoundSource.HOSTILE, 1.0f, 1.2f);
    }

    //  Tick & AI

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2f, true) {
            @Override
            protected int getAttackInterval() {
                return 40; // 2 seconds between attacks — full anim + cooldown
            }
        });
        this.targetSelector.addGoal(7, new NearestAttackableTargetGoal<>(this, Mob.class, false));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.ARMOR, 0.0)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.ATTACK_SPEED, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        // Attack animation countdown
        if (this.attackAnimationTick > 0) {
            --this.attackAnimationTick;
        }

        // Delayed attack damage — deal damage when the slam lands
        if (!level().isClientSide && pendingAttackTarget != null) {
            pendingAttackTick++;
            if (pendingAttackTick >= ATTACK_DAMAGE_DELAY_TICKS) {
                Entity target = pendingAttackTarget;
                pendingAttackTarget = null;
                pendingAttackTick = 0;
                if (target.isAlive() && distanceToSqr(target) < (getBbWidth() * 2.0 + target.getBbWidth() + 1.0) * (getBbWidth() * 2.0 + target.getBbWidth() + 1.0)) {
                    super.doHurtTarget(target);
                }
            }
        }

        if (!level().isClientSide) {
            byte state = getAnimState();

            switch (state) {
                case STATE_SPAWNING -> {
                    animationTick--;
                    if (animationTick <= 0) {
                        setAnimState(STATE_NORMAL);
                        this.hasFinishedSpawn = true;
                    }
                }
                case STATE_SCATTERING -> {
                    animationTick--;
                    if (animationTick <= 0) {
                        // Transition to downed state
                        setAnimState(STATE_DOWNED);
                    }
                }
                case STATE_REASSEMBLING -> {
                    animationTick--;
                    if (animationTick <= 0) {
                        // Fully revived
                        setAnimState(STATE_NORMAL);
                        this.setHealth(this.getMaxHealth());
                    }
                }
                case STATE_DOWNED -> {
                    // Stay downed, keep health at max so entity doesn't despawn/die
                    this.setHealth(this.getMaxHealth());
                }
                // STATE_NORMAL: nothing special
            }
        }

        // Don't process AI during non-normal states
        if (getAnimState() != STATE_NORMAL) {
            this.getNavigation().stop();
            this.setTarget(null);
        }
    }

    @Override
    public void tick() {
        if (this.getAnimState() == STATE_SPAWNING || this.getAnimState() == STATE_REASSEMBLING) {
            if (level().isClientSide)
                clientDiggingParticles(this);
        }
        super.tick();
    }

    protected void clientDiggingParticles(LivingEntity livingEntity) {
        RandomSource randomsource = livingEntity.getRandom();
        BlockState blockstate = livingEntity.getBlockStateOn();
        if (blockstate.getRenderShape() != RenderShape.INVISIBLE) {
            for (int i = 0; i < 15; ++i) {
                double d0 = livingEntity.getX() + (double) Mth.randomBetween(randomsource, -2.5F, 2.5F);
                double d1 = livingEntity.getY();
                double d2 = livingEntity.getZ() + (double) Mth.randomBetween(randomsource, -2.5F, 2.5F);
                livingEntity.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, blockstate), d0, d1, d2, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    @Override
    protected boolean isImmobile() {
        return isInAnimationState() || super.isImmobile();
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        // Start the attack animation — damage is delayed until the slam lands
        this.attackAnimationTick = ATTACK_ANIM_LENGTH_TICKS;
        this.level().broadcastEntityEvent(this, (byte) 4);

        if (!level().isClientSide) {
            this.pendingAttackTarget = target;
            this.pendingAttackTick = 0;
        }
        return true; // Tell AI the attack was initiated
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) {
            this.attackAnimationTick = ATTACK_ANIM_LENGTH_TICKS;
        } else {
            super.handleEntityEvent(id);
        }
    }

    public int getAttackAnimationTick() {
        return this.attackAnimationTick;
    }

    //  Synched Data

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ASSEMBLY, new CompoundTag());
        builder.define(DATA_ANIM_STATE, STATE_NORMAL);
        builder.define(DATA_ENTITY_SCALE, 1.5f);
    }

    //  Assembly Management

    public ChimeraAssembly getAssembly() { return assembly; }

    public void setSlot(ChimeraSlot slot, ResourceLocation entityId) {
        assembly.setSlot(slot, entityId);
        onAssemblyChanged();
    }

    public void setAssembly(ChimeraAssembly newAssembly) {
        this.assembly = newAssembly;
        onAssemblyChanged();
    }

    private void onAssemblyChanged() {
        cachedAttributes = null;
        recalculateEffectiveHeight();
        refreshDimensions();
        if (!level().isClientSide) {
            recalculateAttributes();
            if (!suppressPacketSync) {
                syncAssemblyToClients();
            }
        }
    }

    //  Dynamic Dimensions

    private void recalculateEffectiveHeight() {
        float avgLegRatio = getTallestLegHeightRatio();
        float upperFraction = 1.0f - LEG_HEIGHT_FRACTION;
        this.effectiveHeight = BASE_HEIGHT * (upperFraction + LEG_HEIGHT_FRACTION * avgLegRatio);
    }

    public float getTallestLegHeightRatio() {
        float leftRatio = getLegHeightRatio(ChimeraSlot.LEFT_LEG);
        float rightRatio = getLegHeightRatio(ChimeraSlot.RIGHT_LEG);

        boolean hasLeft = assembly.getEntityForSlot(ChimeraSlot.LEFT_LEG).isPresent();
        boolean hasRight = assembly.getEntityForSlot(ChimeraSlot.RIGHT_LEG).isPresent();

        if (hasLeft && hasRight) return Math.max(leftRatio, rightRatio);
        if (hasLeft) return leftRatio;
        if (hasRight) return rightRatio;
        return 1.0f;
    }

    private float getLegHeightRatio(ChimeraSlot legSlot) {
        return assembly.getEntityForSlot(legSlot).map(entityId -> {
            ChimeraPartRegistry registry = ChimeraPartRegistry.getInstance();
            if (registry == null) return 1.0f;
            return registry.getDefinition(entityId)
                    .flatMap(def -> def.getSlotEntry(legSlot))
                    .map(ChimeraMobDefinition.SlotEntry::getLegHeightRatio)
                    .orElse(1.0f);
        }).orElse(1.0f);
    }

    public float getEntityScale() {
        return this.entityData.get(DATA_ENTITY_SCALE);
    }

    public void setEntityScale(float scale) {
        this.entityData.set(DATA_ENTITY_SCALE, Math.max(0.1f, scale));
        refreshDimensions();
    }

    @Override
    protected @NotNull EntityDimensions getDefaultDimensions(@NotNull Pose pose) {
        float scale = getEntityScale();
        // When downed or scattering, use a flatter hitbox
        if (isDowned() || isScattering()) {
            return EntityDimensions.scalable(0.1f, 0.1f);
        }
        return EntityDimensions.scalable(BASE_WIDTH * scale, effectiveHeight * scale);
    }

    //  Attributes

    private void recalculateAttributes() {
        ChimeraAssembly.ComputedAttributes computed = assembly.computeAttributes();
        cachedAttributes = computed;

        getAttribute(Attributes.MAX_HEALTH).setBaseValue(computed.maxHealth());
        getAttribute(Attributes.ARMOR).setBaseValue(computed.armor());
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(computed.attackDamage());
        getAttribute(Attributes.ATTACK_SPEED).setBaseValue(computed.attackSpeed());
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(computed.movementSpeed());

        if (getHealth() > getMaxHealth()) setHealth(getMaxHealth());
    }

    //  Network Sync

    private void syncAssemblyToClients() {
        entityData.set(DATA_ASSEMBLY, assembly.toNbt());
        PacketDistributor.sendToPlayersTrackingEntity(this,
                new SyncChimeraAssemblyPayload(this.getId(), assembly.toNbt()));
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_ASSEMBLY.equals(key) && level().isClientSide) {
            CompoundTag tag = entityData.get(DATA_ASSEMBLY);
            if (!tag.isEmpty()) {
                ChimeraAssembly newAssembly = ChimeraAssembly.fromNbt(tag);
                if (!newAssembly.getAllAssignments().equals(this.assembly.getAllAssignments())) {
                    this.assembly = newAssembly;
                    this.cachedAttributes = null;
                    recalculateEffectiveHeight();
                    refreshDimensions();
                }
            }
        }

        if (DATA_ENTITY_SCALE.equals(key)) {
            refreshDimensions();
        }

        // Refresh dimensions when animation state changes (downed has different hitbox)
        if (DATA_ANIM_STATE.equals(key)) {
            refreshDimensions();
        }
    }

    //  Persistence

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.put("ChimeraAssembly", assembly.toNbt());
        compound.putBoolean("HasFinishedSpawn", hasFinishedSpawn);
        compound.putFloat("EntityScale", getEntityScale());
        compound.putByte("AnimState", getAnimState());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("ChimeraAssembly")) {
            suppressPacketSync = true;
            try {
                this.assembly = ChimeraAssembly.fromNbt(compound.getCompound("ChimeraAssembly"));
                recalculateEffectiveHeight();
                refreshDimensions();
                if (!level().isClientSide) {
                    recalculateAttributes();
                    entityData.set(DATA_ASSEMBLY, assembly.toNbt());
                }
            } finally {
                suppressPacketSync = false;
            }
        }
        this.hasFinishedSpawn = compound.getBoolean("HasFinishedSpawn");
        if (compound.contains("EntityScale")) {
            this.entityData.set(DATA_ENTITY_SCALE, compound.getFloat("EntityScale"));
        }
        if (compound.contains("AnimState")) {
            byte savedState = compound.getByte("AnimState");
            // If it was in a transition animation when saved, resolve it
            if (savedState == STATE_SCATTERING || savedState == STATE_DOWNED) {
                // Restore as downed
                setAnimState(STATE_DOWNED);
                this.setHealth(this.getMaxHealth());
            } else if (savedState == STATE_SPAWNING || savedState == STATE_REASSEMBLING) {
                // Finish the animation immediately
                setAnimState(STATE_NORMAL);
            } else {
                setAnimState(savedState);
            }
        }
    }

    public ChimeraAssembly.ComputedAttributes getComputedAttributes() {
        if (cachedAttributes == null) cachedAttributes = assembly.computeAttributes();
        return cachedAttributes;
    }

    //  Prevent despawning while downed

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        // Never despawn if downed - the player might come back to revive
        if (isDowned()) return false;
        return super.removeWhenFarAway(distanceToClosestPlayer);
    }

    @Override
    public boolean isPushable() {
        // Can't be pushed while downed or in animations
        return getAnimState() == STATE_NORMAL;
    }
}