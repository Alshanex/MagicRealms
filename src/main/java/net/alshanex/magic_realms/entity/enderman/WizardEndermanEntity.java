package net.alshanex.magic_realms.entity.enderman;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.goals.WizardAttackGoal;
import io.redspace.ironsspellbooks.registries.ComponentRegistry;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

public class WizardEndermanEntity extends AbstractSpellCastingMob {
    public WizardEndermanEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setPathfindingMalus(PathType.WATER, -1.0F);
        this.moveControl = new VexStyleFlyingMoveControl(this);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new WizardEndermanEvadeGazeGoal(this));
        this.goalSelector.addGoal(2, new FlyingWizardAttackGoal(this, 1f, 30, 40)
                .setPreferredHeight(4.0f)
                .setCombatRanges(5.0f, 7.0f, 10.0f)
                .setThreatAwarenessRadius(10.0f)
                .setSpells(
                        List.of(SpellRegistry.MAGIC_MISSILE_SPELL.get(), SpellRegistry.STARFALL_SPELL.get()),
                        List.of(),
                        List.of(),
                        List.of(SpellRegistry.EVASION_SPELL.get())
                )
                .setIsFlying()
                .setAllowFleeing(true)
        );
        this.goalSelector.addGoal(5, new WizardEndermanWanderGoal(this, 0.8, 120));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Endermite.class, true, false));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Player.class, true, false));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3F)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    public void aiStep() {
        if (this.level().isClientSide) {
            for (int i = 0; i < 2; i++) {
                this.level()
                        .addParticle(
                                ParticleTypes.PORTAL,
                                this.getRandomX(0.5),
                                this.getRandomY() - 0.25,
                                this.getRandomZ(0.5),
                                (this.random.nextDouble() - 0.5) * 2.0,
                                -this.random.nextDouble(),
                                (this.random.nextDouble() - 0.5) * 2.0
                        );
            }
        }

        this.jumping = false;

        super.aiStep();
    }

    @Override
    public boolean isSensitiveToWater() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else {
            boolean flag = source.getDirectEntity() instanceof ThrownPotion;
            if (!source.is(DamageTypeTags.IS_PROJECTILE) && !flag) {
                boolean flag2 = super.hurt(source, amount);
                if (!this.level().isClientSide() && !(source.getEntity() instanceof LivingEntity) && this.random.nextInt(10) != 0) {
                    this.teleport();
                }

                return flag2;
            } else {
                boolean flag1 = flag && this.hurtWithCleanWater(source, (ThrownPotion)source.getDirectEntity(), amount);

                for (int i = 0; i < 64; i++) {
                    if (this.teleport()) {
                        return true;
                    }
                }

                return flag1;
            }
        }
    }

    protected boolean teleport() {
        if (!this.level().isClientSide() && this.isAlive()) {
            double d0 = this.getX() + (this.random.nextDouble() - 0.5) * 64.0;
            double d1 = this.getY() + (double)(this.random.nextInt(64) - 32);
            double d2 = this.getZ() + (this.random.nextDouble() - 0.5) * 64.0;
            return this.teleport(d0, d1, d2);
        } else {
            return false;
        }
    }

    private boolean teleport(double x, double y, double z) {
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos(x, y, z);

        while (blockpos$mutableblockpos.getY() > this.level().getMinBuildHeight() && !this.level().getBlockState(blockpos$mutableblockpos).blocksMotion()) {
            blockpos$mutableblockpos.move(Direction.DOWN);
        }

        BlockState blockstate = this.level().getBlockState(blockpos$mutableblockpos);
        boolean flag = blockstate.blocksMotion();
        boolean flag1 = blockstate.getFluidState().is(FluidTags.WATER);
        if (flag && !flag1) {
            net.neoforged.neoforge.event.entity.EntityTeleportEvent.EnderEntity event = net.neoforged.neoforge.event.EventHooks.onEnderTeleport(this, x, y, z);
            if (event.isCanceled()) return false;
            Vec3 vec3 = this.position();
            boolean flag2 = this.randomTeleport(event.getTargetX(), event.getTargetY(), event.getTargetZ(), true);
            if (flag2) {
                this.level().gameEvent(GameEvent.TELEPORT, vec3, GameEvent.Context.of(this));
                if (!this.isSilent()) {
                    this.level().playSound(null, this.xo, this.yo, this.zo, SoundEvents.ENDERMAN_TELEPORT, this.getSoundSource(), 1.0F, 1.0F);
                    this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
                }
            }

            return flag2;
        } else {
            return false;
        }
    }

    private boolean hurtWithCleanWater(DamageSource source, ThrownPotion potion, float amount) {
        ItemStack itemstack = potion.getItem();
        PotionContents potioncontents = itemstack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        return potioncontents.is(Potions.WATER) ? super.hurt(source, amount) : false;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData) {
        RandomSource randomsource = Utils.random;
        this.populateDefaultEquipmentSlots(randomsource, pDifficulty);
        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource pRandom, DifficultyInstance pDifficulty) {
        ItemStack hat = new ItemStack(ItemRegistry.WIZARD_HAT.get());

        hat.set(ComponentRegistry.CLOTHING_VARIANT, "hood");
        hat.set(DataComponents.DYED_COLOR, new DyedItemColor(0x524057, false));

        this.setItemSlot(EquipmentSlot.HEAD, hat);
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ItemRegistry.SHADOWWALKER_CHESTPLATE.get()));
        this.setDropChance(EquipmentSlot.HEAD, 0.0F);
        this.setDropChance(EquipmentSlot.CHEST, 0.0F);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isControlledByLocalInstance()) {
            if (this.isInWater()) {
                this.moveRelative(0.02F, travelVector);
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale(0.8));
            } else if (this.isInLava()) {
                this.moveRelative(0.02F, travelVector);
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
            } else {
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale(0.91));
            }
        }

        this.calculateEntityAnimation(false);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean isDrinkingPotion() {
        return false;
    }

    @Override
    public void startDrinkingPotion() {
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
    }

    @Override
    public boolean shouldAlwaysAnimateLegs() {
        return false;
    }

    @Override
    public boolean bobBodyWhileWalking() {
        return false;
    }

    public static class WizardEndermanWanderGoal extends Goal {
        private final WizardEndermanEntity enderman;
        private final double speed;
        private final int minInterval;
        private final double horizontalRange;
        private final double verticalRange;
        private final double minHeight;
        private final double maxHeight;
        private Vec3 targetPos;

        public WizardEndermanWanderGoal(WizardEndermanEntity enderman, double speed, int minInterval) {
            this.enderman = enderman;
            this.speed = speed;
            this.minInterval = minInterval;
            this.horizontalRange = 16.0;
            this.verticalRange = 8.0;
            this.minHeight = 3.0;
            this.maxHeight = 10.0;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (enderman.getTarget() != null) {
                return false;
            }

            if (enderman.getRandom().nextInt(reducedTickDelay(minInterval)) != 0) {
                return false;
            }

            targetPos = findWanderTarget();
            return targetPos != null;
        }

        @Override
        public boolean canContinueToUse() {
            if (enderman.getTarget() != null) {
                return false;
            }

            // Stop once close enough to target
            double distSqr = enderman.position().distanceToSqr(targetPos);
            if (distSqr < 4.0) {
                return false;
            }

            // Timeout — don't chase a point forever
            return true;
        }

        @Override
        public void start() {
            enderman.getMoveControl().setWantedPosition(targetPos.x, targetPos.y, targetPos.z, speed);
        }

        @Override
        public void tick() {
            // Re-issue the move command since VexStyleFlyingMoveControl
            // sets operation to WAIT once it arrives close
            double distSqr = enderman.position().distanceToSqr(targetPos);
            if (distSqr > 4.0) {
                enderman.getMoveControl().setWantedPosition(targetPos.x, targetPos.y, targetPos.z, speed);
            }
        }

        @Override
        public void stop() {
            targetPos = null;
        }

        /**
         * Finds a valid airborne wander position.
         * Prefers staying at a comfortable height above the ground.
         */
        @Nullable
        private Vec3 findWanderTarget() {
            RandomSource random = enderman.getRandom();
            Level level = enderman.level();

            for (int attempt = 0; attempt < 10; attempt++) {
                // Pick a random horizontal offset
                double dx = (random.nextDouble() - 0.5) * 2.0 * horizontalRange;
                double dz = (random.nextDouble() - 0.5) * 2.0 * horizontalRange;

                double candidateX = enderman.getX() + dx;
                double candidateZ = enderman.getZ() + dz;

                // Find ground level at this XZ
                BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(
                        candidateX, enderman.getY() + verticalRange, candidateZ
                );
                int searched = 0;
                while (mutable.getY() > level.getMinBuildHeight() && searched < 40) {
                    if (level.getBlockState(mutable).blocksMotion()) {
                        break;
                    }
                    mutable.move(Direction.DOWN);
                    searched++;
                }

                if (!level.getBlockState(mutable).blocksMotion()) {
                    continue; // No ground found
                }

                double groundY = mutable.getY() + 1.0;

                // Pick a height above ground within our preferred range
                double flyHeight = groundY + minHeight + random.nextDouble() * (maxHeight - minHeight);

                // Optionally bias toward current Y so movement feels smooth
                double candidateY = Mth.lerp(0.3, flyHeight, enderman.getY() + (random.nextDouble() - 0.5) * verticalRange);
                candidateY = Math.max(candidateY, groundY + minHeight);

                Vec3 candidate = new Vec3(candidateX, candidateY, candidateZ);

                // Make sure we won't suffocate there
                AABB newBB = enderman.getBoundingBox().move(candidate.subtract(enderman.position()));
                if (level.collidesWithSuffocatingBlock(enderman, newBB.deflate(0.05))) {
                    continue;
                }

                return candidate;
            }

            return null;
        }
    }

    public static class WizardEndermanEvadeGazeGoal extends Goal {
        private final WizardEndermanEntity enderman;
        private Player gazer;

        public WizardEndermanEvadeGazeGoal(WizardEndermanEntity enderman) {
            this.enderman = enderman;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            List<Player> players = enderman.level().getEntitiesOfClass(
                    Player.class,
                    enderman.getBoundingBox().inflate(64),
                    p -> p.isAlive() && !p.isSpectator() && p.hasLineOfSight(enderman)
            );

            for (Player player : players) {
                if (isNearlyLookingAt(player)) {
                    this.gazer = player;
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            if (gazer != null) {
                teleportToBlindSpot(gazer);
            }
        }

        /**
         * Detects when the player is CLOSE to looking directly at the enderman, but not necessarily staring right at it yet. Uses a more generous dot-product threshold than vanilla's direct stare check.
         *
         * A dot product of ~0.95 means roughly within 18 degrees of looking at it.
         */
        private boolean isNearlyLookingAt(Player player) {
            Vec3 viewVec = player.getViewVector(1.0F).normalize();
            Vec3 toEnderman = new Vec3(
                    enderman.getX() - player.getX(),
                    enderman.getEyeY() - player.getEyeY(),
                    enderman.getZ() - player.getZ()
            );
            toEnderman = toEnderman.normalize();
            double dot = viewVec.dot(toEnderman);

            // ~0.95 = roughly 18 degrees cone — the player is "about to" look at it
            return dot > 0.95;
        }

        /**
         * Teleports far from the player, specifically to a position that is
         * behind or to the side of the player — outside their field of view.
         */
        private void teleportToBlindSpot(Player player) {
            Vec3 lookDir = player.getViewVector(1.0F).normalize().multiply(1, 0, 1).normalize();
            Level level = enderman.level();

            for (int attempt = 0; attempt < 32; attempt++) {
                // Generate a direction in the player's blind spot (back hemisphere)
                // Start with directly behind, then widen to sides on later attempts
                double angle;
                if (attempt < 8) {
                    // Directly behind: ±30 degrees from back
                    angle = Math.PI + (enderman.getRandom().nextDouble() - 0.5) * (Math.PI / 3);
                } else if (attempt < 20) {
                    // Wider behind: ±90 degrees from back (full back hemisphere)
                    angle = Math.PI + (enderman.getRandom().nextDouble() - 0.5) * Math.PI;
                } else {
                    // Desperate: anywhere except directly in front (±120 degrees from back)
                    angle = Math.PI + (enderman.getRandom().nextDouble() - 0.5) * (Math.PI * 1.3);
                }

                // Rotate the player's look direction by this angle around Y
                double cos = Math.cos(angle);
                double sin = Math.sin(angle);
                Vec3 dir = new Vec3(
                        lookDir.x * cos - lookDir.z * sin,
                        0,
                        lookDir.x * sin + lookDir.z * cos
                ).normalize();

                // Distance: 7 blocks away from the player
                double dist = 7;
                double targetX = player.getX() + dir.x * dist;
                double targetZ = player.getZ() + dir.z * dist;

                // Find valid ground
                double searchY = player.getY() + 8;
                BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(targetX, searchY, targetZ);

                // Search downward for solid ground
                int searched = 0;
                while (mutable.getY() > level.getMinBuildHeight() && searched < 32) {
                    if (level.getBlockState(mutable).blocksMotion()) {
                        break;
                    }
                    mutable.move(Direction.DOWN);
                    searched++;
                }

                BlockState groundState = level.getBlockState(mutable);
                if (!groundState.blocksMotion() || groundState.getFluidState().is(FluidTags.WATER)) {
                    continue;
                }

                double groundY = mutable.getY() + 1.0;
                Vec3 teleportPos = new Vec3(targetX, groundY, targetZ);

                // Verify the enderman won't suffocate
                AABB newBB = enderman.getBoundingBox().move(teleportPos.subtract(enderman.position()));
                if (level.collidesWithSuffocatingBlock(enderman, newBB.deflate(0.05))) {
                    continue;
                }

                // Verify the player can't see this position (not in line of sight)
                // This ensures it's a true blind spot
                Vec3 eyePos = player.getEyePosition(1.0F);
                Vec3 targetEye = teleportPos.add(0, enderman.getEyeHeight(), 0);
                Vec3 toTarget = targetEye.subtract(eyePos).normalize();
                double dotWithView = player.getViewVector(1.0F).normalize().dot(toTarget);

                // Only accept positions clearly outside the player's FOV
                // dot < 0 means more than 90 degrees from where they're looking
                if (dotWithView > 0.3) {
                    continue; // Still too much in front, skip
                }

                // Teleport
                enderman.teleportTo(targetX, groundY, targetZ);
                if (!enderman.isSilent()) {
                    level.playSound(null, enderman.xo, enderman.yo, enderman.zo,
                            SoundEvents.ENDERMAN_TELEPORT, enderman.getSoundSource(), 1.0F, 1.0F);
                    enderman.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
                }
                level.gameEvent(GameEvent.TELEPORT, enderman.position(), GameEvent.Context.of(enderman));
                return;
            }
        }
    }

    public static class VexStyleFlyingMoveControl extends MoveControl {
        private final WizardEndermanEntity entity;

        public VexStyleFlyingMoveControl(WizardEndermanEntity entity) {
            super(entity);
            this.entity = entity;
        }

        @Override
        public void tick() {
            if (this.operation == Operation.MOVE_TO) {
                Vec3 direction = new Vec3(
                        this.wantedX - entity.getX(),
                        this.wantedY - entity.getY(),
                        this.wantedZ - entity.getZ()
                );
                double distance = direction.length();

                if (distance < entity.getBoundingBox().getSize()) {
                    this.operation = Operation.WAIT;
                    entity.setDeltaMovement(entity.getDeltaMovement().scale(0.5));
                } else {
                    // Accelerate toward the wanted position
                    entity.setDeltaMovement(
                            entity.getDeltaMovement().add(direction.scale(this.speedModifier * 0.05 / distance))
                    );

                    // Face the combat target if in combat, otherwise face movement direction
                    if (entity.getTarget() == null) {
                        Vec3 vel = entity.getDeltaMovement();
                        entity.setYRot(-((float) Mth.atan2(vel.x, vel.z)) * (180F / (float) Math.PI));
                    } else {
                        double dx = entity.getTarget().getX() - entity.getX();
                        double dz = entity.getTarget().getZ() - entity.getZ();
                        entity.setYRot(-((float) Mth.atan2(dx, dz)) * (180F / (float) Math.PI));
                    }
                    entity.yBodyRot = entity.getYRot();
                }
            }
        }
    }

    public static class FlyingWizardAttackGoal extends WizardAttackGoal {

        // ==================== CONFIGURATION ====================

        /** Preferred height above the target */
        private float preferredHeight = 4.0f;

        /** Ideal distance to orbit at while casting */
        private float optimalRange = 10.0f;

        /** Minimum safe distance — entity retreats urgently below this */
        private float minSafeRange = 5.0f;

        /** Maximum combat range — entity approaches if farther than this */
        private float maxCombatRange = 20.0f;

        // ==================== THREAT AWARENESS ====================

        /** Radius to scan for hostile mobs targeting this entity */
        private float threatAwarenessRadius = 14.0f;

        /** Cached threat center — recalculated periodically */
        @Nullable
        private Vec3 cachedThreatCenter = null;

        /** Tick counter for periodic threat scans */
        private int threatScanTimer = 0;

        // ==================== MOVEMENT STATE ====================

        /** Orbit angle for smooth circular movement */
        private double orbitAngle = 0.0;

        /** Orbit direction — flips periodically for unpredictability */
        private boolean orbitClockwise = true;

        /** Timer for periodic orbit direction changes */
        private int orbitSwitchTimer = 0;

        // ==================== CONSTRUCTOR ====================

        public FlyingWizardAttackGoal(WizardEndermanEntity abstractSpellCastingMob, double speedModifier, int attackInterval) {
            this(abstractSpellCastingMob, speedModifier, attackInterval, attackInterval);
        }

        public FlyingWizardAttackGoal(WizardEndermanEntity abstractSpellCastingMob, double speedModifier, int attackIntervalMin, int attackIntervalMax) {
            super(abstractSpellCastingMob, speedModifier, attackIntervalMin, attackIntervalMax);
            this.isFlying = true;
            this.allowFleeing = false; // We handle retreat ourselves
        }

        // ==================== BUILDER METHODS ====================

        public FlyingWizardAttackGoal setPreferredHeight(float height) {
            this.preferredHeight = height;
            return this;
        }

        public FlyingWizardAttackGoal setCombatRanges(float min, float optimal, float max) {
            this.minSafeRange = min;
            this.optimalRange = optimal;
            this.maxCombatRange = max;
            this.spellcastingRange = max;
            this.spellcastingRangeSqr = max * max;
            return this;
        }

        public FlyingWizardAttackGoal setThreatAwarenessRadius(float radius) {
            this.threatAwarenessRadius = radius;
            return this;
        }

        // ==================== LIFECYCLE ====================

        @Override
        public void start() {
            super.start();
            this.orbitAngle = mob.getRandom().nextDouble() * Math.PI * 2;
            this.orbitClockwise = mob.getRandom().nextBoolean();
        }

        @Override
        public void stop() {
            super.stop();
            this.cachedThreatCenter = null;
        }

        @Override
        public boolean canUse() {
            return super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return super.canContinueToUse();
        }

        // ==================== MAIN TICK ====================

        @Override
        public void tick() {
            // Periodic threat scan + retargeting
            if (++threatScanTimer >= 5) {
                threatScanTimer = 0;
                updateThreatAwareness();
            }

            // Periodic orbit direction switch
            if (++orbitSwitchTimer > 60) {
                if (mob.getRandom().nextFloat() < 0.15f) {
                    orbitClockwise = !orbitClockwise;
                    orbitSwitchTimer = 0;
                }
            }

            super.tick();
        }

        // ==================== MOVEMENT OVERRIDE ====================

        @Override
        protected void doMovement(double distanceSquared) {
            if (target == null || target.isDeadOrDying()) {
                mob.getNavigation().stop();
                return;
            }

            mob.getLookControl().setLookAt(target, 30, 30);

            double distance = Math.sqrt(distanceSquared);
            double nearestThreatDistSqr = getNearestThreatDistSqr();
            double nearestThreatDist = Math.sqrt(nearestThreatDistSqr);
            double effectiveDist = Math.min(distance, nearestThreatDist);

            if (effectiveDist < minSafeRange) {
                // URGENT RETREAT — full speed, never slowed by casting
                // Cancel any active cast so we move at full speed
                if (spellCastingMob.isCasting()) {
                    spellCastingMob.cancelCast();
                }
                doRetreatMovement(speedModifier * 1.5);
            } else if (effectiveDist < optimalRange * 0.8) {
                // Threats creeping in — retreat at normal speed
                double retreatSpeed = spellCastingMob.isCasting() ? speedModifier * 0.8 : speedModifier * 1.2;
                doRetreatMovement(retreatSpeed);
            } else if (distance > maxCombatRange || !hasLineOfSight) {
                // TOO FAR or no LOS — approach
                double approachSpeed = spellCastingMob.isCasting() ? speedModifier * 0.5 : speedModifier * 1.2;
                doApproachMovement(approachSpeed);
            } else {
                // SAFE RANGE — orbit
                double orbitSpeed = spellCastingMob.isCasting() ? speedModifier * 0.5 : speedModifier;
                doOrbitMovement(distance, orbitSpeed);
            }
        }

        /**
         * Retreat from the threat centroid (all nearby hostiles), gaining altitude.
         */
        private void doRetreatMovement(double speed) {
            Vec3 fleeFrom = cachedThreatCenter != null ? cachedThreatCenter : target.position();

            Vec3 awayFromThreats = mob.position().subtract(fleeFrom);
            double dist = awayFromThreats.horizontalDistance();
            if (dist < 0.01) {
                awayFromThreats = new Vec3(1, 0, 0);
            }
            Vec3 awayNorm = awayFromThreats.normalize();

            // Lateral offset for unpredictable retreat
            Vec3 lateral = new Vec3(-awayNorm.z, 0, awayNorm.x).scale(getOrbitSide());
            Vec3 retreatPos = mob.position()
                    .add(awayNorm.scale(optimalRange * 0.7))
                    .add(lateral.scale(optimalRange * 0.3));

            // Gain altitude while retreating
            double targetY = Math.max(retreatPos.y, target.getY() + preferredHeight + 2);

            mob.getMoveControl().setWantedPosition(retreatPos.x, targetY, retreatPos.z, speed);
        }

        /**
         * Approach the target to get within casting range, maintaining height.
         */
        private void doApproachMovement(double speed) {
            Vec3 toTarget = target.position().subtract(mob.position()).normalize();
            Vec3 approachPos = target.position().subtract(toTarget.scale(optimalRange * 0.8));
            double targetY = target.getY() + preferredHeight;

            mob.getMoveControl().setWantedPosition(approachPos.x, targetY, approachPos.z, speed);
        }

        /**
         * Orbit around the target at optimal range.
         * Actively adjusts distance every tick — if enemies creep closer, biases outward.
         * Produces smooth circular motion via incrementing orbit angle.
         */
        private void doOrbitMovement(double currentDistance, double speed) {
            double orbitSpeed = 0.04 * (orbitClockwise ? 1 : -1);

            // Faster orbit when not casting for harder-to-hit movement
            if (!spellCastingMob.isCasting()) {
                orbitSpeed *= 1.5;
            }

            orbitAngle += orbitSpeed;

            // Determine the orbit radius — stay at optimal range, push out if threats are close
            double orbitRadius = optimalRange;
            double nearestThreatDist = Math.sqrt(getNearestThreatDistSqr());
            if (nearestThreatDist < optimalRange) {
                // Threats are inside our comfort zone — expand orbit outward
                orbitRadius = optimalRange + (optimalRange - nearestThreatDist) * 0.5;
            }

            // Compute orbit position around the target
            double targetX = target.getX() + Math.cos(orbitAngle) * orbitRadius;
            double targetZ = target.getZ() + Math.sin(orbitAngle) * orbitRadius;

            // Vertical bobbing for natural flight feel
            double verticalBob = Math.sin(orbitAngle * 2) * 1.0;
            double targetY = target.getY() + preferredHeight + verticalBob;

            // Casting speed: slower orbit for stable aim
            double orbitMoveSpeed = spellCastingMob.isCasting() ? speed * 0.5 : speed;

            mob.getMoveControl().setWantedPosition(targetX, targetY, targetZ, orbitMoveSpeed);
        }

        // ==================== ATTACK LOGIC ====================

        @Override
        protected void handleAttackLogic(double distanceSquared) {
            double nearestThreatDistSqr = getNearestThreatDistSqr();
            boolean threatsAreClose = nearestThreatDistSqr < optimalRange * optimalRange;
            boolean threatsAreDangerous = nearestThreatDistSqr < minSafeRange * minSafeRange;

            // SURVIVAL FIRST: if any threat is within minSafeRange, cancel any active cast
            // and do NOT start new ones. The entity's only job is to retreat.
            if (threatsAreDangerous) {
                if (spellCastingMob.isCasting()) {
                    spellCastingMob.cancelCast();
                }
                // Reset spell timer so it doesn't fire the instant we're safe
                spellAttackDelay = 10;
                return;
            }

            // CAUTION: if threats are inside optimal range but not yet dangerous,
            // allow ongoing casts to finish but don't start new ones.
            if (threatsAreClose) {
                if (spellCastingMob.isCasting()) {
                    // Let the current cast finish — but cancel if it's a long cast
                    var spellData = MagicData.getPlayerMagicData(mob).getCastingSpell();
                    if (spellData != null && spellData.getSpell().getCastTime(spellData.getLevel()) > 30) {
                        spellCastingMob.cancelCast();
                    }
                    // Also cancel if target died
                    if (target != null && target.isDeadOrDying()) {
                        spellCastingMob.cancelCast();
                    }
                }
                // Don't start new casts — keep the timer frozen
                if (spellAttackDelay <= 1) {
                    spellAttackDelay = 5;
                }
                return;
            }

            // SAFE RANGE: let the parent handle normal spell casting
            super.handleAttackLogic(distanceSquared);
        }

        // ==================== SPELL WEIGHTS ====================

        // Flying casters favor attack spells heavily and use movement/defense
        // more than support since they rely on kiting.

        @Override
        protected int getDefenseWeight() {
            // Slightly reduced — they avoid damage by distance, not shielding
            return (int) (super.getDefenseWeight() * 0.7f);
        }

        @Override
        protected int getMovementWeight() {
            // Increased — repositioning is key for a flying caster
            return (int) (super.getMovementWeight() * 1.3f);
        }

        // ==================== THREAT AWARENESS ====================

        /**
         * Scans for nearby hostiles targeting this entity,
         * updates the cached threat center, and handles reactive retargeting.
         */
        private void updateThreatAwareness() {
            List<LivingEntity> threats = getNearbyThreats();

            if (!threats.isEmpty()) {
                Vec3 center = Vec3.ZERO;
                for (LivingEntity threat : threats) {
                    center = center.add(threat.position());
                }
                cachedThreatCenter = center.scale(1.0 / threats.size());
            } else if (target != null) {
                cachedThreatCenter = target.position();
            } else {
                cachedThreatCenter = null;
            }

            // Reactive retargeting: switch to a closer attacker
            LivingEntity lastAttacker = mob.getLastHurtByMob();
            if (lastAttacker != null && lastAttacker.isAlive() && lastAttacker != target
                    && mob.getLastHurtByMobTimestamp() > mob.tickCount - 20) {
                if (target == null || mob.distanceToSqr(lastAttacker) < mob.distanceToSqr(target)) {
                    mob.setTarget(lastAttacker);
                }
            }
        }

        /**
         * Returns all living entities within threat awareness radius that are targeting this entity.
         */
        private List<LivingEntity> getNearbyThreats() {
            AABB scanBox = mob.getBoundingBox().inflate(threatAwarenessRadius);
            return mob.level().getEntitiesOfClass(LivingEntity.class, scanBox, entity -> {
                if (entity == mob || !entity.isAlive()) return false;
                if (entity instanceof Mob mobEntity && mobEntity.getTarget() == mob) return true;
                return entity == mob.getLastHurtByMob() && mob.getLastHurtByMobTimestamp() > mob.tickCount - 40;
            });
        }

        /**
         * Returns the squared distance to the nearest threat, or Double.MAX_VALUE if no threats.
         */
        private double getNearestThreatDistSqr() {
            List<LivingEntity> threats = getNearbyThreats();
            double nearest = Double.MAX_VALUE;
            for (LivingEntity threat : threats) {
                double distSqr = mob.distanceToSqr(threat);
                if (distSqr < nearest) {
                    nearest = distSqr;
                }
            }
            return nearest;
        }

        // ==================== HELPERS ====================

        private float getOrbitSide() {
            return orbitClockwise ? 1f : -1f;
        }

        @Override
        public float getStrafeMultiplier() {
            return 0.0F; // No ground strafing — all movement via setWantedPosition
        }
    }
}
