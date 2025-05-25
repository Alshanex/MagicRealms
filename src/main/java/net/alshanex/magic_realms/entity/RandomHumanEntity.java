package net.alshanex.magic_realms.entity;

import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.entity.mobs.IAnimatedAttacker;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.NeutralWizard;
import io.redspace.ironsspellbooks.entity.mobs.goals.PatrolNearLocationGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.WizardRecoverGoal;
import io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss.NotIdioticNavigation;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.util.humans.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import software.bernie.geckolib.animation.*;

import javax.annotation.Nullable;
import java.util.Random;

public class RandomHumanEntity extends NeutralWizard implements IAnimatedAttacker {
    private static final EntityDataAccessor<Integer> GENDER = SynchedEntityData.defineId(RandomHumanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ENTITY_CLASS = SynchedEntityData.defineId(RandomHumanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> INITIALIZED = SynchedEntityData.defineId(RandomHumanEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> ENTITY_NAME = SynchedEntityData.defineId(RandomHumanEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> HAIR_TEXTURE_INDEX = SynchedEntityData.defineId(RandomHumanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STAR_LEVEL = SynchedEntityData.defineId(RandomHumanEntity.class, EntityDataSerializers.INT);

    private EntityTextureConfig textureConfig;
    private boolean appearanceGenerated = false;

    public RandomHumanEntity(EntityType<? extends AbstractSpellCastingMob> entityType, Level level) {
        super(entityType, level);
        this.lookControl = createLookControl();
        this.moveControl = createMoveControl();
    }

    protected LookControl createLookControl() {
        return new LookControl(this) {
            @Override
            protected float rotateTowards(float pFrom, float pTo, float pMaxDelta) {
                return super.rotateTowards(pFrom, pTo, pMaxDelta * 2.5f);
            }

            @Override
            protected boolean resetXRotOnTick() {
                return getTarget() == null;
            }
        };
    }

    protected MoveControl createMoveControl() {
        return new MoveControl(this) {
            @Override
            protected float rotlerp(float pSourceAngle, float pTargetAngle, float pMaximumChange) {
                double d0 = this.wantedX - this.mob.getX();
                double d1 = this.wantedZ - this.mob.getZ();
                if (d0 * d0 + d1 * d1 < .5f) {
                    return pSourceAngle;
                } else {
                    return super.rotlerp(pSourceAngle, pTargetAngle, pMaximumChange * .25f);
                }
            }
        };
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));

        this.goalSelector.addGoal(4, new PatrolNearLocationGoal(this, 30, .75f));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(10, new WizardRecoverGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isHostileTowards));
        this.targetSelector.addGoal(5, new ResetUniversalAngerTargetGoal<>(this, false));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData) {
        RandomSource randomsource = Utils.random;
        this.populateDefaultEquipmentSlots(randomsource, pDifficulty);

        if (!this.entityData.get(INITIALIZED)) {
            initializeStarLevel(randomsource);
            initializeRandomAppearance(randomsource);
            this.entityData.set(INITIALIZED, true);
        }

        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource pRandom, DifficultyInstance pDifficulty) {
        /*
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ItemRegistry.PYROMANCER_HELMET.get()));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ItemRegistry.PYROMANCER_CHESTPLATE.get()));
        this.setDropChance(EquipmentSlot.HEAD, 0.0F);
        this.setDropChance(EquipmentSlot.CHEST, 0.0F);
         */
    }

    public static AttributeSupplier.Builder prepareAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.ATTACK_KNOCKBACK, 0.0)
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.ENTITY_INTERACTION_RANGE, 3)
                .add(Attributes.MOVEMENT_SPEED, .25);
    }

    @Override
    public boolean shouldSheathSword() {
        return true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        super.defineSynchedData(pBuilder);
        pBuilder.define(GENDER, 0);
        pBuilder.define(ENTITY_CLASS, 0);
        pBuilder.define(INITIALIZED, false);
        pBuilder.define(ENTITY_NAME, "");
        pBuilder.define(HAIR_TEXTURE_INDEX, -1);
        pBuilder.define(STAR_LEVEL, 1);
    }

    private void initializeRandomAppearance(RandomSource randomSource) {
        if (appearanceGenerated) {
            return;
        }

        Gender gender = Gender.values()[randomSource.nextInt(Gender.values().length)];
        EntityClass entityClass = EntityClass.values()[randomSource.nextInt(EntityClass.values().length)];

        String randomName = AdvancedNameManager.getRandomName(gender);
        int hairTextureIndex = LayeredTextureManager.getRandomHairTextureIndex("hair_" + gender.getName());

        this.entityData.set(GENDER, gender.ordinal());
        this.entityData.set(ENTITY_CLASS, entityClass.ordinal());
        this.entityData.set(ENTITY_NAME, randomName);
        this.entityData.set(HAIR_TEXTURE_INDEX, hairTextureIndex);

        this.textureConfig = new EntityTextureConfig(this.getUUID().toString(), gender, entityClass);
        this.appearanceGenerated = true;

        updateCustomNameWithStars();

        MagicRealms.LOGGER.debug("Initialized appearance for entity {}: Gender={}, Class={}, Stars={}",
                this.getUUID().toString(), gender.getName(), entityClass.getName(), this.entityData.get(STAR_LEVEL));
    }

    private void initializeStarLevel(RandomSource randomSource) {
        double roll = randomSource.nextDouble();

        int starLevel;
        if (roll < 0.6) {
            starLevel = 1;
        } else if (roll < 0.9) {
            starLevel = 2;
        } else {
            starLevel = 3;
        }

        this.entityData.set(STAR_LEVEL, starLevel);
    }

    private void updateCustomNameWithStars() {
        String entityName = this.entityData.get(ENTITY_NAME);
        int starLevel = this.entityData.get(STAR_LEVEL);

        if (!entityName.isEmpty()) {
            ChatFormatting nameColor = getColorForStarLevel(starLevel);
            String stars = getStarsDisplay(starLevel);

            Component nameComponent = Component.literal(stars + " " + entityName).withStyle(nameColor);
            this.setCustomName(nameComponent);
            this.setCustomNameVisible(true);
        }
    }

    private ChatFormatting getColorForStarLevel(int starLevel) {
        return switch (starLevel) {
            case 2 -> ChatFormatting.AQUA;
            case 3 -> ChatFormatting.GOLD;
            default -> ChatFormatting.WHITE;
        };
    }

    private String getStarsDisplay(int starLevel) {
        return "★".repeat(starLevel);
    }

    public int getStarLevel() {
        return this.entityData.get(STAR_LEVEL);
    }

    public void setStarLevel(int starLevel) {
        if (starLevel >= 1 && starLevel <= 3) {
            this.entityData.set(STAR_LEVEL, starLevel);
            updateCustomNameWithStars();
        }
    }

    public Gender getGender() {
        return Gender.values()[this.entityData.get(GENDER)];
    }

    public String getEntityName() {
        return this.entityData.get(ENTITY_NAME);
    }

    public ResourceLocation getHairTexture() {
        EntityTextureConfig config = getTextureConfig();
        return config != null ? config.getHairTexture() : null;
    }

    public int getHairTextureIndex() {
        return this.entityData.get(HAIR_TEXTURE_INDEX);
    }

    public EntityClass getEntityClass() {
        return EntityClass.values()[this.entityData.get(ENTITY_CLASS)];
    }

    public EntityTextureConfig getTextureConfig() {
        if (textureConfig == null) {
            if (!this.entityData.get(INITIALIZED)) {
                MagicRealms.LOGGER.warn("Trying to get texture config before entity is initialized: {}", this.getUUID().toString());
                return null;
            }
            textureConfig = new EntityTextureConfig(this.getUUID().toString(), getGender(), getEntityClass(), getHairTextureIndex());
        }
        return textureConfig;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("Gender", this.entityData.get(GENDER));
        compound.putInt("EntityClass", this.entityData.get(ENTITY_CLASS));
        compound.putBoolean("Initialized", this.entityData.get(INITIALIZED));
        compound.putBoolean("AppearanceGenerated", this.appearanceGenerated);
        compound.putString("EntityName", this.entityData.get(ENTITY_NAME));
        compound.putInt("HairTextureIndex", this.entityData.get(HAIR_TEXTURE_INDEX));
        compound.putInt("StarLevel", this.entityData.get(STAR_LEVEL));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(GENDER, compound.getInt("Gender"));
        this.entityData.set(ENTITY_CLASS, compound.getInt("EntityClass"));
        this.entityData.set(INITIALIZED, compound.getBoolean("Initialized"));
        this.appearanceGenerated = compound.getBoolean("AppearanceGenerated");

        int hairIndex = compound.getInt("HairTextureIndex");
        this.entityData.set(HAIR_TEXTURE_INDEX, hairIndex);

        int starLevel = compound.contains("StarLevel") ? compound.getInt("StarLevel") : 1;
        this.entityData.set(STAR_LEVEL, starLevel);

        if (this.entityData.get(INITIALIZED)) {
            this.textureConfig = new EntityTextureConfig(this.getUUID().toString(), getGender(), getEntityClass(), hairIndex);
        }

        String savedName = compound.getString("EntityName");
        this.entityData.set(ENTITY_NAME, savedName);

        if (!savedName.isEmpty()) {
            updateCustomNameWithStars();
        }
    }

    @Override
    public void die(net.minecraft.world.damagesource.DamageSource damageSource) {
        String entityUUID = this.getUUID().toString();

        if (!level().isClientSide) {
            MagicManager.spawnParticles(level(), ParticleTypes.POOF, getX(), getY(), getZ(), 25, .4, .8, .4, .03, false);
        }

        this.moveTo(this.getX(), this.getY() + 300, this.getZ());

        if (this.level().isClientSide) {
            CombinedTextureManager.removeEntityTexture(entityUUID);
        }

        super.die(damageSource);
    }

    public void forceInitializeAppearance() {
        if (!this.entityData.get(INITIALIZED)) {
            RandomSource randomSource = this.level().getRandom();
            initializeStarLevel(randomSource);
            initializeRandomAppearance(randomSource);
            this.entityData.set(INITIALIZED, true);
        }
    }

    RawAnimation animationToPlay = null;
    private final AnimationController<RandomHumanEntity> meleeController = new AnimationController<>(this, "keeper_animations", 0, this::predicate);

    @Override
    public void playAnimation(String animationId) {
        try {
            animationToPlay = RawAnimation.begin().thenPlay(animationId);
        } catch (Exception ignored) {
            MagicRealms.LOGGER.error("Entity {} Failed to play animation: {}", this, animationId);
        }
    }

    private PlayState predicate(AnimationState<RandomHumanEntity> animationEvent) {
        var controller = animationEvent.getController();

        if (this.animationToPlay != null) {
            controller.forceAnimationReset();
            controller.setAnimation(animationToPlay);
            animationToPlay = null;
        }
        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(meleeController);
        super.registerControllers(controllerRegistrar);
    }

    @Override
    public boolean isAnimating() {
        return meleeController.getAnimationState() != AnimationController.State.STOPPED || super.isAnimating();
    }

    @Override
    public boolean guardsBlocks() {
        return false;
    }

    @Override
    public boolean isHostileTowards(LivingEntity pTarget) {
        return super.isHostileTowards(pTarget);
    }

    @Override
    protected PathNavigation createNavigation(Level pLevel) {
        return new NotIdioticNavigation(this, pLevel);
    }
}
