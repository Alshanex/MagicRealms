package net.alshanex.magic_realms.entity;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.entity.mobs.IAnimatedAttacker;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.NeutralWizard;
import io.redspace.ironsspellbooks.entity.mobs.goals.PatrolNearLocationGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.WizardAttackGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.WizardRecoverGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.melee.AttackAnimationData;
import io.redspace.ironsspellbooks.entity.mobs.wizards.GenericAnimatedWarlockAttackGoal;
import io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss.NotIdioticNavigation;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.events.MagicAttributeGainsHandler;
import net.alshanex.magic_realms.util.humans.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.animation.AnimationState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class RandomHumanEntity extends NeutralWizard implements IAnimatedAttacker, RangedAttackMob {
    private static final EntityDataAccessor<Integer> GENDER = SynchedEntityData.defineId(RandomHumanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ENTITY_CLASS = SynchedEntityData.defineId(RandomHumanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> INITIALIZED = SynchedEntityData.defineId(RandomHumanEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> ENTITY_NAME = SynchedEntityData.defineId(RandomHumanEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> STAR_LEVEL = SynchedEntityData.defineId(RandomHumanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> MAGIC_SCHOOLS = SynchedEntityData.defineId(RandomHumanEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> HAS_SHIELD = SynchedEntityData.defineId(RandomHumanEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_ARCHER = SynchedEntityData.defineId(RandomHumanEntity.class, EntityDataSerializers.BOOLEAN);

    private EntityTextureConfig textureConfig;
    private boolean appearanceGenerated = false;

    private List<SchoolType> magicSchools = new ArrayList<>();

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

        if (!this.entityData.get(INITIALIZED)) {
            initializeStarLevel(randomsource);
            initializeRandomAppearance(randomsource);
            initializeClassSpecifics(randomsource);
            this.entityData.set(INITIALIZED, true);

            RandomSource randomSource = Utils.random;
            List<AbstractSpell> spells = SpellListGenerator.generateSpellsForEntity(this, randomSource);

            if(this.getEntityClass() == EntityClass.MAGE){
                setMageGoal(spells);
            } else if (this.getEntityClass() == EntityClass.WARRIOR){
                setWarriorGoal(spells);
            } else if(this.getEntityClass() == EntityClass.ROGUE){
                if(isArcher()){
                    setArcherGoal(spells);
                } else {
                    setAssassinGoal(spells);
                }
            }
        }

        HumanStatsManager.applyClassAttributes(this);
        this.populateDefaultEquipmentSlots(randomsource, pDifficulty);

        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData);
    }

    private void initializeClassSpecifics(RandomSource randomSource) {
        EntityClass entityClass = getEntityClass();

        MagicRealms.LOGGER.debug("Initializing class specifics for {} (Class: {})",
                getEntityName(), entityClass.getName());

        switch (entityClass) {
            case MAGE -> {
                List<SchoolType> schools = generateMagicSchools(randomSource);
                setMagicSchools(schools);
                MagicRealms.LOGGER.debug("Mage {} assigned schools: [{}]",
                        getEntityName(),
                        schools.stream().map(s -> s.getId().toString()).collect(java.util.stream.Collectors.joining(", ")));
            }
            case WARRIOR -> {
                // 75% single weapon, 25% weapon + shield
                boolean hasShield = randomSource.nextFloat() < 0.25f;
                setHasShield(hasShield);
                MagicRealms.LOGGER.debug("Warrior {} has shield: {}", getEntityName(), hasShield);
            }
            case ROGUE -> {
                // 75% assassin, 25% archer
                boolean isArcher = randomSource.nextFloat() < 0.25f;
                setIsArcher(isArcher);
                MagicRealms.LOGGER.debug("Rogue {} is archer: {} (subclass: {})",
                        getEntityName(), isArcher, isArcher ? "Archer" : "Assassin");
            }
        }
    }

    private List<SchoolType> generateMagicSchools(RandomSource random) {
        double roll = random.nextDouble();
        int schoolCount;

        if (roll < 0.65) {
            schoolCount = 1; // 65% single school
        } else if (roll < 0.85) {
            schoolCount = 2; // 20% double school
        } else if (roll < 0.95) {
            schoolCount = 3; // 10% triple school
        } else {
            schoolCount = 4; // 5% quadruple school
        }

        List<SchoolType> availableSchools = SchoolRegistry.REGISTRY.stream().toList();

        List<SchoolType> selectedSchools = new ArrayList<>();
        List<SchoolType> tempAvailable = new ArrayList<>(availableSchools);

        while (selectedSchools.size() < schoolCount && !tempAvailable.isEmpty()) {
            int index = random.nextInt(tempAvailable.size());
            selectedSchools.add(tempAvailable.remove(index));
        }

        return selectedSchools;
    }

    public List<SchoolType> getMagicSchools() {
        if (magicSchools.isEmpty()) {
            String serialized = this.entityData.get(MAGIC_SCHOOLS);
            if (!serialized.isEmpty()) {
                magicSchools = deserializeSchools(serialized);
            }
        }
        return new ArrayList<>(magicSchools);
    }

    public void setMagicSchools(List<SchoolType> schools) {
        this.magicSchools = new ArrayList<>(schools);
        String serialized = serializeSchools(schools);
        this.entityData.set(MAGIC_SCHOOLS, serialized);
    }

    public boolean hasSchool(SchoolType school) {
        return getMagicSchools().contains(school);
    }

    // 8. Métodos de serialización para sync
    private String serializeSchools(List<SchoolType> schools) {
        if (schools.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < schools.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(schools.get(i).getId().toString());
        }
        return sb.toString();
    }

    private List<SchoolType> deserializeSchools(String serialized) {
        List<SchoolType> schools = new ArrayList<>();
        if (serialized.isEmpty()) return schools;

        String[] schoolIds = serialized.split(",");
        for (String schoolId : schoolIds) {
            try {
                ResourceLocation location = ResourceLocation.parse(schoolId.trim());
                SchoolType school = SchoolRegistry.getSchool(location);
                if (school != null) {
                    schools.add(school);
                }
            } catch (Exception e) {
                MagicRealms.LOGGER.warn("Failed to parse school ID: {}", schoolId, e);
            }
        }
        return schools;
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource pRandom, DifficultyInstance pDifficulty) {
        if(isArcher()){
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        }
        /* Shields not showing properly
        if(hasShield()){
            this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        }
        */
    }

    public static AttributeSupplier.Builder prepareAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.ATTACK_KNOCKBACK, 0.0)
                .add(Attributes.MAX_HEALTH, 1.0)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.ENTITY_INTERACTION_RANGE, 3)
                .add(Attributes.MOVEMENT_SPEED, .25);
    }

    private AbstractSpell lastCastingSpell = null;
    private boolean wasCasting = false;

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {
            MagicData data = this.getMagicData();
            boolean isCasting = data.isCasting();

            if (wasCasting && !isCasting && lastCastingSpell != null) {
                MagicRealms.LOGGER.debug("Entity {} completed casting spell: {} (School: {})",
                        getEntityName(), lastCastingSpell.getSpellName(), lastCastingSpell.getSchoolType().getId());

                MagicAttributeGainsHandler.handleSpellCast(this, lastCastingSpell.getSchoolType());

                lastCastingSpell = null;
            }

            if (isCasting && data.getCastingSpell() != null) {
                lastCastingSpell = data.getCastingSpell().getSpell();
            }

            wasCasting = isCasting;
        }
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
        pBuilder.define(STAR_LEVEL, 1);
        pBuilder.define(MAGIC_SCHOOLS, "");
        pBuilder.define(HAS_SHIELD, false);
        pBuilder.define(IS_ARCHER, false);
    }

    private void initializeRandomAppearance(RandomSource randomSource) {
        if (appearanceGenerated) {
            return;
        }

        Gender gender = Gender.values()[randomSource.nextInt(Gender.values().length)];
        EntityClass entityClass = EntityClass.values()[randomSource.nextInt(EntityClass.values().length)];

        String randomName = AdvancedNameManager.getRandomName(gender);

        this.entityData.set(GENDER, gender.ordinal());
        this.entityData.set(ENTITY_CLASS, entityClass.ordinal());
        this.entityData.set(ENTITY_NAME, randomName);

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

    public boolean hasShield() {
        return this.entityData.get(HAS_SHIELD);
    }

    public void setHasShield(boolean hasShield) {
        this.entityData.set(HAS_SHIELD, hasShield);
    }

    public boolean isArcher() {
        return this.entityData.get(IS_ARCHER);
    }

    public void setIsArcher(boolean isArcher) {
        this.entityData.set(IS_ARCHER, isArcher);
    }

    public boolean isAssassin() {
        return !isArcher();
    }

    public String getEntityName() {
        return this.entityData.get(ENTITY_NAME);
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
            textureConfig = new EntityTextureConfig(this.getUUID().toString(), getGender(), getEntityClass());
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
        compound.putInt("StarLevel", this.entityData.get(STAR_LEVEL));

        ListTag schoolsTag = new ListTag();
        for (SchoolType school : getMagicSchools()) {
            schoolsTag.add(StringTag.valueOf(school.getId().toString()));
        }

        compound.put("MagicSchools", schoolsTag);
        compound.putBoolean("HasShield", this.entityData.get(HAS_SHIELD));
        compound.putBoolean("IsArcher", this.entityData.get(IS_ARCHER));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(GENDER, compound.getInt("Gender"));
        this.entityData.set(ENTITY_CLASS, compound.getInt("EntityClass"));
        this.entityData.set(INITIALIZED, compound.getBoolean("Initialized"));
        this.appearanceGenerated = compound.getBoolean("AppearanceGenerated");

        int starLevel = compound.contains("StarLevel") ? compound.getInt("StarLevel") : 1;
        this.entityData.set(STAR_LEVEL, starLevel);

        List<SchoolType> schools = new ArrayList<>();
        if (compound.contains("MagicSchools")) {
            ListTag schoolsTag = compound.getList("MagicSchools", 8); // 8 = STRING_TAG
            for (int i = 0; i < schoolsTag.size(); i++) {
                try {
                    ResourceLocation location = ResourceLocation.parse(schoolsTag.getString(i));
                    SchoolType school = SchoolRegistry.getSchool(location);
                    if (school != null) {
                        schools.add(school);
                    }
                } catch (Exception e) {
                    MagicRealms.LOGGER.warn("Failed to parse school from NBT: {}", schoolsTag.getString(i), e);
                }
            }
        }
        setMagicSchools(schools);

        this.entityData.set(HAS_SHIELD, compound.getBoolean("HasShield"));
        this.entityData.set(IS_ARCHER, compound.getBoolean("IsArcher"));

        if (this.entityData.get(INITIALIZED)) {
            this.textureConfig = new EntityTextureConfig(this.getUUID().toString(), getGender(), getEntityClass());
        }

        String savedName = compound.getString("EntityName");
        this.entityData.set(ENTITY_NAME, savedName);

        if (!savedName.isEmpty()) {
            updateCustomNameWithStars();
        }
    }

    @Override
    public void die(net.minecraft.world.damagesource.DamageSource damageSource) {
        if (!level().isClientSide) {
            MagicManager.spawnParticles(level(), ParticleTypes.POOF, getX(), getY(), getZ(), 25, .4, .8, .4, .03, false);
        }
        super.die(damageSource);
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (level().isClientSide) {
            return false;
        }
        /*
        can parry:
        - serverside
        - in combat
        - we aren't in melee attack anim or spell cast
        - the damage source is caused by an entity (ie not fall damage)
        - the damage is caused within our rough field of vision (117 degrees)
        - the damage is not /kill
         */
        boolean canParry = this.isAggressive() &&
                !isImmobile() &&
                !isAnimating() &&
                pSource.getEntity() != null &&
                pSource.getSourcePosition() != null && pSource.getSourcePosition().subtract(this.position()).normalize().dot(this.getForward()) >= 0.35
                && !pSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        if (canParry && this.random.nextFloat() < 0.25) {
            serverTriggerAnimation("offhand_parry");
            this.playSound(SoundRegistry.FIRE_DAGGER_PARRY.get());
            return false;
        }

        return super.hurt(pSource, pAmount);
    }

    public void forceInitializeAppearance() {
        if (!this.entityData.get(INITIALIZED)) {
            RandomSource randomSource = this.level().getRandom();
            initializeStarLevel(randomSource);
            initializeRandomAppearance(randomSource);
            this.entityData.set(INITIALIZED, true);
        }
    }

    //GOALS

    private void setMageGoal(List<AbstractSpell> spells){
        this.goalSelector.removeAllGoals((goal) -> goal instanceof WizardAttackGoal);
        this.goalSelector.addGoal(2, new WizardAttackGoal(this, 1.25f, 25, 50)
                .setSpells(spells, List.of(), List.of(), List.of())
                .setDrinksPotions()
        );
    }

    private void setArcherGoal(List<AbstractSpell> spells){
        this.goalSelector.removeAllGoals((goal) -> goal instanceof WizardAttackGoal);
        this.goalSelector.removeAllGoals((goal) -> goal instanceof RangedBowAttackGoal);

        this.goalSelector.addGoal(2, new RangedBowAttackGoal<>(this, 1.0D, 20, 15.0F));

        if (!spells.isEmpty()) {
            this.goalSelector.addGoal(4, new WizardAttackGoal(this, 1.0f, 60, 120)
                    .setSpells(spells, List.of(), List.of(), List.of())
                    .setDrinksPotions()
            );
        }
    }

    private void setAssassinGoal(List<AbstractSpell> spells){
        this.goalSelector.removeAllGoals((goal) -> goal instanceof GenericAnimatedWarlockAttackGoal);
        this.goalSelector.addGoal(3, new GenericAnimatedWarlockAttackGoal<>(this, 1.5f, 40, 60)
                .setMoveset(List.of(
                        new AttackAnimationData(9, "simple_sword_upward_swipe", 5),
                        new AttackAnimationData(8, "simple_sword_lunge_stab", 6),
                        new AttackAnimationData(10, "simple_sword_stab_alternate", 8),
                        new AttackAnimationData(10, "simple_sword_horizontal_cross_swipe", 8)
                ))
                .setComboChance(.4f)
                .setMeleeAttackInverval(10, 20)
                .setMeleeMovespeedModifier(1.8f)
                .setSpells(spells, List.of(), List.of(), List.of())
                .setDrinksPotions()
        );
    }

    private void setWarriorGoal(List<AbstractSpell> spells){
        this.goalSelector.removeAllGoals((goal) -> goal instanceof GenericAnimatedWarlockAttackGoal);
        this.goalSelector.addGoal(3, new GenericAnimatedWarlockAttackGoal<>(this, 1.25f, 50, 75)
                .setMoveset(List.of(
                        new AttackAnimationData(9, "simple_sword_upward_swipe", 5),
                        new AttackAnimationData(8, "simple_sword_lunge_stab", 6),
                        new AttackAnimationData(10, "simple_sword_stab_alternate", 8),
                        new AttackAnimationData(10, "simple_sword_horizontal_cross_swipe", 8)
                ))
                .setComboChance(.4f)
                .setMeleeAttackInverval(10, 30)
                .setMeleeMovespeedModifier(1.5f)
                .setSpells(spells, List.of(), List.of(), List.of())
                .setDrinksPotions()
        );
    }

    @Override
    public void performRangedAttack(LivingEntity pTarget, float pDistanceFactor) {
        // Crear flecha directamente sin necesidad de items en inventario
        AbstractArrow abstractarrow = this.getArrow(ItemStack.EMPTY, pDistanceFactor);

        // Calcular trayectoria
        double d0 = pTarget.getX() - this.getX();
        double d1 = pTarget.getY(0.3333333333333333D) - abstractarrow.getY();
        double d2 = pTarget.getZ() - this.getZ();
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);

        // Configurar velocidad y precisión
        abstractarrow.shoot(d0, d1 + d3 * (double)0.2F, d2, 1.6F, (float)(14 - this.level().getDifficulty().getId() * 4));

        // Sonido de disparo
        this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));

        // Añadir flecha al mundo
        this.level().addFreshEntity(abstractarrow);
    }

    protected AbstractArrow getArrow(ItemStack pArrowStack, float pDistanceFactor) {
        AbstractArrow arrow = new Arrow(this.level(), this, new ItemStack(Items.ARROW), this.getMainHandItem());
        return arrow;
    }

    //GECKOLIB ANIMATIONS

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
