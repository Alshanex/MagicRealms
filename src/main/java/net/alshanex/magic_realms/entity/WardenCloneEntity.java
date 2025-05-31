package net.alshanex.magic_realms.entity;

import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.util.OwnerHelper;
import net.alshanex.magic_realms.registry.MREntityRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.UUID;

public class WardenCloneEntity extends Warden implements IMagicSummon {
    public WardenCloneEntity(EntityType<? extends Warden> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public WardenCloneEntity(Level level, LivingEntity owner, LivingEntity target) {
        this(MREntityRegistry.ELDRITCH_CLONE.get(), level);
        setSummoner(owner);
        setCachedTarget(target);
        this.xpReward = 0;
    }

    protected LivingEntity cachedSummoner;
    protected UUID summonerUUID;

    protected LivingEntity cachedTarget;
    protected UUID targetUUID;

    @Override
    public boolean isAlliedTo(Entity pEntity) {
        return super.isAlliedTo(pEntity) || this.isAlliedHelper(pEntity);
    }

    @Override
    public LivingEntity getSummoner() {
        return OwnerHelper.getAndCacheOwner(level(), cachedSummoner, summonerUUID);
    }

    public void setSummoner(@Nullable LivingEntity owner) {
        if (owner != null) {
            this.summonerUUID = owner.getUUID();
            this.cachedSummoner = owner;
        }
    }

    public LivingEntity getCachedTarget() {
        return OwnerHelper.getAndCacheOwner(level(), cachedTarget, targetUUID);
    }

    public void setCachedTarget(@Nullable LivingEntity target) {
        if (target != null) {
            this.targetUUID = target.getUUID();
            this.cachedTarget = target;
        }
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource pSource) {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();

        if(getCachedTarget() == null){
            if (!level().isClientSide) {
                MagicManager.spawnParticles(level(), ParticleTypes.POOF, getX(), getY(), getZ(), 25, .4, .8, .4, .03, false);
                discard();
            }
        } else {
            if(getCachedTarget().getType() == MREntityRegistry.ELDRITCH_CLONE.get() || !getCachedTarget().isAlive()){
                if (!level().isClientSide) {
                    MagicManager.spawnParticles(level(), ParticleTypes.POOF, getX(), getY(), getZ(), 25, .4, .8, .4, .03, false);
                    discard();
                }
            }
        }

        if (getSummoner() != null) {
            if(getCachedTarget() != null){
                this.increaseAngerAt(getCachedTarget());
            }
        } else {
            if (!level().isClientSide) {
                MagicManager.spawnParticles(level(), ParticleTypes.POOF, getX(), getY(), getZ(), 25, .4, .8, .4, .03, false);
                discard();
            }
        }
    }

    @Override
    public void onUnSummon() {
        if (!level().isClientSide) {
            MagicManager.spawnParticles(level(), ParticleTypes.POOF, getX(), getY(), getZ(), 25, .4, .8, .4, .03, false);
            discard();
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        this.summonerUUID = OwnerHelper.deserializeOwner(compoundTag);
        this.targetUUID = deserializeTarget(compoundTag);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        OwnerHelper.serializeOwner(compoundTag, summonerUUID);
        serializeTarget(compoundTag, targetUUID);
    }

    public static void serializeTarget(CompoundTag compoundTag, UUID targetUUID) {
        if (targetUUID != null) {
            compoundTag.putUUID("Target", targetUUID);
        }
    }

    public static UUID deserializeTarget(CompoundTag compoundTag) {
        if (compoundTag.hasUUID("Target")) {
            return compoundTag.getUUID("Target");
        }
        return null;
    }

    public static AttributeSupplier.Builder createCloneAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 500.0D)
                .add(Attributes.MOVEMENT_SPEED, (double)0.3F)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 0D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D);
    }
}
