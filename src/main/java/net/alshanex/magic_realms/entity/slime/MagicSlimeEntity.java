package net.alshanex.magic_realms.entity.slime;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import io.redspace.ironsspellbooks.registries.ParticleRegistry;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.registry.MREntityRegistry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class MagicSlimeEntity extends Slime {
    private static final EntityDataAccessor<String> WEAK_SCHOOL = SynchedEntityData.defineId(MagicSlimeEntity.class, EntityDataSerializers.STRING);

    private final Map<ResourceLocation, ParticleOptions> particleTypes = Map.ofEntries(
            Map.entry(SchoolRegistry.BLOOD.get().getId(), ParticleRegistry.BLOOD_PARTICLE.get()),
            Map.entry(SchoolRegistry.HOLY.get().getId(), ParticleRegistry.CLEANSE_PARTICLE.get()),
            Map.entry(SchoolRegistry.EVOCATION.get().getId(), ParticleTypes.CRIT),
            Map.entry(SchoolRegistry.ELDRITCH.get().getId(), ParticleTypes.SCULK_CHARGE_POP),
            Map.entry(SchoolRegistry.ENDER.get().getId(), ParticleRegistry.UNSTABLE_ENDER_PARTICLE.get()),
            Map.entry(SchoolRegistry.FIRE.get().getId(), ParticleRegistry.FIRE_PARTICLE.get()),
            Map.entry(SchoolRegistry.ICE.get().getId(), ParticleRegistry.SNOWFLAKE_PARTICLE.get()),
            Map.entry(SchoolRegistry.LIGHTNING.get().getId(), ParticleRegistry.ELECTRICITY_PARTICLE.get()),
            Map.entry(SchoolRegistry.NATURE.get().getId(), ParticleTypes.COMPOSTER),
            Map.entry(ResourceLocation.fromNamespaceAndPath("aero_additions", "wind"), ParticleTypes.EFFECT),
            Map.entry(ResourceLocation.fromNamespaceAndPath("familiarslib", "sound"), ParticleTypes.NOTE),
            Map.entry(ResourceLocation.fromNamespaceAndPath("cataclysm_spellbooks", "abyssal"), ParticleTypes.LANDING_OBSIDIAN_TEAR),
            Map.entry(ResourceLocation.fromNamespaceAndPath("cataclysm_spellbooks", "technomancy"), ParticleTypes.WAX_ON),
            Map.entry(ResourceLocation.fromNamespaceAndPath("iss_magicfromtheeast", "spirit"), ParticleTypes.SOUL),
            Map.entry(ResourceLocation.fromNamespaceAndPath("iss_magicfromtheeast", "symmetry"), ParticleTypes.TOTEM_OF_UNDYING),
            Map.entry(ResourceLocation.fromNamespaceAndPath("iss_magicfromtheeast", "dune"), ParticleTypes.CAMPFIRE_COSY_SMOKE),
            Map.entry(ResourceLocation.fromNamespaceAndPath("endersequipment", "primeval"), ParticleTypes.CHERRY_LEAVES),
            Map.entry(ResourceLocation.fromNamespaceAndPath("endersequipment", "blade"), ParticleTypes.SWEEP_ATTACK),
            Map.entry(ResourceLocation.fromNamespaceAndPath("ess_requiem", "mind"), ParticleTypes.NAUTILUS)
    );

    private SchoolType weakSchool;

    private boolean hasDivided = false;

    public MagicSlimeEntity(EntityType<? extends Slime> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.initializeSchool();
    }

    public MagicSlimeEntity(EntityType<? extends Slime> pEntityType, Level pLevel, SchoolType parentSchool) {
        super(pEntityType, pLevel);
        if (parentSchool != null) {
            this.setWeakSchool(parentSchool);
        } else {
            this.initializeSchool();
        }
    }

    public MagicSlimeEntity(EntityType<? extends Slime> pEntityType, Level pLevel, int size) {
        super(pEntityType, pLevel);
        this.setSize(size, true);
    }

    private void initializeSchool() {
        if (this.getWeakSchool() == null) {
            List<SchoolType> availableSchools = SchoolRegistry.REGISTRY.stream().toList();
            if (!availableSchools.isEmpty()) {
                int index = random.nextInt(availableSchools.size());
                this.setWeakSchool(availableSchools.get(index));
            }
        }
    }

    public SchoolType getWeakSchool() {
        if (this.weakSchool == null) {
            String schoolId = this.entityData.get(WEAK_SCHOOL);
            if (!schoolId.isEmpty()) {
                ResourceLocation schoolLocation = ResourceLocation.parse(schoolId);
                this.weakSchool = SchoolRegistry.REGISTRY.get(schoolLocation);
            }
        }
        return this.weakSchool;
    }

    public void setWeakSchool(SchoolType weakSchool) {
        this.weakSchool = weakSchool;
        if (weakSchool != null) {
            this.entityData.set(WEAK_SCHOOL, weakSchool.getId().toString());
            MagicRealms.LOGGER.debug("Set weak school to: " + weakSchool.getId());
        } else {
            this.entityData.set(WEAK_SCHOOL, "");
            MagicRealms.LOGGER.debug("Set weak school to null/empty");
        }
    }

    @Override
    protected ParticleOptions getParticleType() {
        if(getWeakSchool() != null) {
            ParticleOptions particleType = particleTypes.get(getWeakSchool().getId());
            if(particleType != null) {
                return particleType;
            }
        }
        return super.getParticleType();
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData) {
        SpawnGroupData spawnData = super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData);

        RandomSource randomsource = pLevel.getRandom();
        int i = randomsource.nextInt(3);
        if (i < 2 && randomsource.nextFloat() < 0.5F * pDifficulty.getSpecialMultiplier()) {
            i++;
        }

        int j = 1 << i;
        this.setSize(j, true);

        return spawnData;
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if(!this.level().isClientSide) {
            if(random.nextFloat() < 0.5f && !hasDivided){
                if(getWeakSchool() != null && !pSource.is(getWeakSchool().getDamageType())) {
                    MagicSlimeEntity newSlime = new MagicSlimeEntity(MREntityRegistry.MAGIC_SLIME.get(), this.level());
                    newSlime.setPos(position());
                    newSlime.finalizeSpawn((ServerLevel) this.level(), this.level().getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null);
                    newSlime.setWeakSchool(getWeakSchool());
                    newSlime.setSize(getSize(), true);

                    this.level().addFreshEntity(newSlime);

                    this.hasDivided = true;
                }
            }

            if(pSource.getEntity() instanceof LivingEntity entity && this.distanceToSqr(entity) < 9.0D) {
                Vec3 entityPos = entity.position();
                Vec3 knockbackDirection = entityPos.subtract(this.position()).normalize();

                if (knockbackDirection.lengthSqr() < 0.001) {
                    knockbackDirection = new Vec3(0, 1, 0);
                }

                double knockbackStrength = 3.0;

                Vec3 knockbackVelocity = knockbackDirection.scale(knockbackStrength);

                knockbackVelocity = knockbackVelocity.add(0, 0.5, 0);

                entity.setDeltaMovement(entity.getDeltaMovement().add(knockbackVelocity));
                entity.hasImpulse = true;
            }
        }

        return super.hurt(pSource, pAmount);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        int i = this.getSize();
        if (!this.level().isClientSide && i > 1 && this.isDeadOrDying()) {
            Component component = this.getCustomName();
            boolean flag = this.isNoAi();
            float f = this.getDimensions(this.getPose()).width();
            float f1 = f / 2.0F;
            int j = i / 2;
            int k = 2 + this.random.nextInt(3);

            var children = new java.util.ArrayList<Mob>();

            for (int l = 0; l < k; l++) {
                float f2 = ((float)(l % 2) - 0.5F) * f1;
                float f3 = ((float)(l / 2) - 0.5F) * f1;

                SchoolType parentSchool = this.getWeakSchool();
                MagicSlimeEntity slime = new MagicSlimeEntity(MREntityRegistry.MAGIC_SLIME.get(), this.level(), parentSchool);

                if (slime != null) {
                    if (this.isPersistenceRequired()) {
                        slime.setPersistenceRequired();
                    }

                    slime.setCustomName(component);
                    slime.setNoAi(flag);
                    slime.setInvulnerable(this.isInvulnerable());
                    slime.setSize(j, true);
                    slime.hasDivided = false;

                    slime.moveTo(this.getX() + (double)f2, this.getY() + 0.5, this.getZ() + (double)f3, this.random.nextFloat() * 360.0F, 0.0F);

                    children.add(slime);
                }
            }

            if (!net.neoforged.neoforge.event.EventHooks.onMobSplit(this, children).isCanceled()) {
                children.forEach(this.level()::addFreshEntity);
            }

            // Temporarily set size to 1 to prevent vanilla splitting
            this.setSize(1, false);
        }
        super.remove(reason);
    }

    public static AttributeSupplier.Builder prepareAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.ATTACK_KNOCKBACK, 0.0)
                .add(Attributes.MAX_HEALTH, 50.0)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.MOVEMENT_SPEED, .2);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putString("weakSchool", this.entityData.get(WEAK_SCHOOL));
        compound.putBoolean("hasDivided", hasDivided);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WEAK_SCHOOL, "");
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("weakSchool")) {
            String schoolIdString = compound.getString("weakSchool");
            this.entityData.set(WEAK_SCHOOL, schoolIdString);
            // Clear the cached value so it gets refreshed from synced data
            this.weakSchool = null;
        }
        if (compound.contains("hasDivided")) {
            this.hasDivided = compound.getBoolean("hasDivided");
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (WEAK_SCHOOL.equals(key)) {
            this.weakSchool = null;
        }
    }
}