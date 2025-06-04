package net.alshanex.magic_realms.entity;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.entity.mobs.IAnimatedAttacker;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.NeutralWizard;
import io.redspace.ironsspellbooks.entity.mobs.goals.*;
import io.redspace.ironsspellbooks.entity.mobs.goals.melee.AttackAnimationData;
import io.redspace.ironsspellbooks.entity.mobs.wizards.GenericAnimatedWarlockAttackGoal;
import io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss.NotIdioticNavigation;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.util.OwnerHelper;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.ContractData;
import net.alshanex.magic_realms.data.KillTrackerData;
import net.alshanex.magic_realms.events.MagicAttributeGainsHandler;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.registry.MREntityRegistry;
import net.alshanex.magic_realms.util.ArrowTypeManager;
import net.alshanex.magic_realms.util.ChargeArrowAttackGoal;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.animation.AnimationState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RandomHumanEntity extends NeutralWizard implements IAnimatedAttacker, RangedAttackMob, IMagicSummon {
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
        xpReward = 0;
        this.lookControl = createLookControl();
        this.moveControl = createMoveControl();
    }

    public RandomHumanEntity(Level level, LivingEntity owner) {
        this(MREntityRegistry.HUMAN.get(), level);
        setSummoner(owner);
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
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public boolean isLeftHanded() {
        return false;
    }

    @Override
    public void setLeftHanded(boolean pLeftHanded) {
        super.setLeftHanded(false);
    }

    public void setGender(Gender gender) {
        this.entityData.set(GENDER, gender.ordinal());
    }

    public void setEntityClass(EntityClass entityClass) {
        this.entityData.set(ENTITY_CLASS, entityClass.ordinal());
    }

    public void setEntityName(String name) {
        this.entityData.set(ENTITY_NAME, name);
        updateCustomNameWithStars();
    }

    public void setInitialized(boolean initialized) {
        this.entityData.set(INITIALIZED, initialized);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));

        this.goalSelector.addGoal(7, new GenericFollowOwnerGoal(this, this::getSummoner, 0.9f, 15, 5, false, 25));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(10, new WizardRecoverGoal(this));

        this.targetSelector.addGoal(1, new GenericOwnerHurtByTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(2, new GenericOwnerHurtTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(3, new GenericCopyOwnerTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(4, (new GenericHurtByTargetGoal(this, (entity) -> entity == getSummoner())).setAlertOthers());
        this.targetSelector.addGoal(5, new GenericProtectOwnerTargetGoal(this, this::getSummoner));
    }

    private List<AbstractSpell> persistedSpells = new ArrayList<>();
    private boolean spellsGenerated = false;

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData) {
        RandomSource randomsource = Utils.random;

        if (!this.entityData.get(INITIALIZED)) {
            initializeStarLevel(randomsource);
            initializeRandomAppearance(randomsource);
            initializeClassSpecifics(randomsource);
            this.entityData.set(INITIALIZED, true);

            if (!spellsGenerated) {
                RandomSource randomSource = Utils.random;
                List<AbstractSpell> spells = SpellListGenerator.generateSpellsForEntity(this, randomSource);
                this.persistedSpells = new ArrayList<>(spells);
                this.spellsGenerated = true;

                if (this.getEntityClass() == EntityClass.MAGE) {
                    setMageGoal(spells);
                } else if (this.getEntityClass() == EntityClass.WARRIOR) {
                    setWarriorGoal(spells);
                } else if (this.getEntityClass() == EntityClass.ROGUE) {
                    if (isArcher()) {
                        setArcherGoal(spells);
                    } else {
                        setAssassinGoal(spells);
                    }
                }
            }

            goalsInitialized = true;
        } else {
            if (spellsGenerated && !persistedSpells.isEmpty()) {
                reapplyGoalsWithPersistedSpells();
                goalsInitialized = true;
            }
        }

        HumanStatsManager.applyClassAttributes(this);

        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData);
    }

    public List<AbstractSpell> getPersistedSpells() {
        return new ArrayList<>(persistedSpells);
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
    private boolean goalsInitialized = false;
    private boolean wasChargingArrow = false;
    private boolean shouldPlayChargeAnimation = false;

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide && !goalsInitialized && this.entityData.get(INITIALIZED)) {
            reinitializeGoalsAfterLoad();
            goalsInitialized = true;
        }

        // Verificar contrato y limpiar si ha expirado (no aplicar a contratos permanentes)
        if (!level().isClientSide) {
            ContractData contractData = this.getData(MRDataAttachments.CONTRACT_DATA);

            if (contractData.hasActiveContract()) {
                // Si es un contrato permanente, solo asegurar que el summoner esté configurado
                if (contractData.isPermanent()) {
                    if (this.getSummoner() == null) {
                        UUID contractorUUID = contractData.getContractorUUID();
                        if (contractorUUID != null) {
                            Player contractor = this.level().getPlayerByUUID(contractorUUID);
                            if (contractor != null) {
                                this.setSummoner(contractor);
                                MagicRealms.LOGGER.debug("Restored summoner relationship for permanent contract entity {}", this.getEntityName());
                            }
                        }
                    }
                } else {
                    // El contrato temporal sigue activo, mantener al jugador como summoner
                    if (this.getSummoner() == null) {
                        UUID contractorUUID = contractData.getContractorUUID();
                        if (contractorUUID != null) {
                            Player contractor = this.level().getPlayerByUUID(contractorUUID);
                            if (contractor != null) {
                                this.setSummoner(contractor);
                                MagicRealms.LOGGER.debug("Restored summoner relationship for contracted entity {}", this.getEntityName());
                            }
                        }
                    }
                }
            } else {
                // El contrato ha expirado (solo para contratos temporales), limpiar datos
                UUID previousContractorUUID = contractData.getContractorUUID();
                if (previousContractorUUID != null && !contractData.isPermanent()) {
                    MagicRealms.LOGGER.info("Contract expired for {} star entity {} with player {}",
                            this.getStarLevel(), this.getEntityName(), previousContractorUUID);

                    contractData.clearContract();
                    this.setSummoner(null);

                    // Notificar al jugador si está cerca
                    Player contractor = this.level().getPlayerByUUID(previousContractorUUID);
                    if (contractor instanceof ServerPlayer serverPlayer && contractor.distanceToSqr(this) <= 64) {
                        serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket(
                                Component.translatable("ui.magic_realms.contract_expired", this.getEntityName())
                                        .withStyle(ChatFormatting.RED)
                        ));
                    }
                }
            }
        }

        if (this.isArcher()) {
            handleArcherAnimations();
        }

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

    public boolean canBeContractedBy(Player player) {
        ContractData contractData = this.getData(MRDataAttachments.CONTRACT_DATA);

        // Si no hay contrato activo, cualquiera puede contratar
        if (!contractData.hasActiveContract()) {
            return true;
        }

        // Si hay contrato activo, solo el contratista actual puede renovar
        return contractData.isContractor(player.getUUID());
    }

    public String getContractInfo() {
        ContractData contractData = this.getData(MRDataAttachments.CONTRACT_DATA);

        if (!contractData.hasActiveContract()) {
            int starLevel = this.getStarLevel();
            int potentialMinutes = contractData.getAdditionalMinutesForStarLevel(starLevel);
            return String.format("No active contract (Would last %d minutes for %d stars)",
                    potentialMinutes, starLevel);
        }

        // Si es un contrato permanente
        if (contractData.isPermanent()) {
            return "Permanent contract (Never expires)";
        }

        // Contrato temporal normal
        int minutes = contractData.getRemainingMinutes();
        int seconds = contractData.getRemainingSeconds();
        int starLevel = this.getStarLevel();
        int extensionMinutes = contractData.getAdditionalMinutesForStarLevel(starLevel);

        return String.format("Contract expires in %d:%02d (Extensions add %d min for %d stars)",
                minutes, seconds, extensionMinutes, starLevel);
    }

    public void clearContract() {
        ContractData contractData = this.getData(MRDataAttachments.CONTRACT_DATA);
        contractData.clearContract();
        MagicRealms.LOGGER.info("Contract manually cleared for entity {}", this.getEntityName());
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

    public void updateCustomNameWithStars() {
        String entityName = this.entityData.get(ENTITY_NAME);
        int starLevel = this.entityData.get(STAR_LEVEL);
        KillTrackerData data = this.getData(MRDataAttachments.KILL_TRACKER);

        if (!entityName.isEmpty()) {
            ChatFormatting nameColor = getColorForStarLevel(starLevel);
            String stars = getStarsDisplay(starLevel);

            Component nameComponent = Component.literal(stars + " " + entityName + "   Lv. " + data.getCurrentLevel()).withStyle(nameColor);
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
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double pDistanceToClosestPlayer) {
        return false;
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

        compound.putBoolean("SpellsGenerated", this.spellsGenerated);
        compound.putBoolean("GoalsInitialized", this.goalsInitialized);

        if (spellsGenerated && !persistedSpells.isEmpty()) {
            ListTag spellsTag = new ListTag();
            for (AbstractSpell spell : persistedSpells) {
                spellsTag.add(StringTag.valueOf(spell.getSpellResource().toString()));
            }
            compound.put("PersistedSpells", spellsTag);

            MagicRealms.LOGGER.debug("Saved {} spells for entity {}: [{}]",
                    persistedSpells.size(),
                    getEntityName(),
                    persistedSpells.stream().map(AbstractSpell::getSpellName).collect(java.util.stream.Collectors.joining(", ")));
        }

        OwnerHelper.serializeOwner(compound, summonerUUID);
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

        this.spellsGenerated = compound.getBoolean("SpellsGenerated");
        this.persistedSpells.clear();

        if (compound.contains("PersistedSpells")) {
            ListTag spellsTag = compound.getList("PersistedSpells", 8);
            for (int i = 0; i < spellsTag.size(); i++) {
                try {
                    ResourceLocation spellLocation = ResourceLocation.parse(spellsTag.getString(i));
                    AbstractSpell spell = SpellRegistry.getSpell(spellLocation);
                    if (spell != null) {
                        persistedSpells.add(spell);
                    } else {
                        MagicRealms.LOGGER.warn("Failed to find spell: {}", spellLocation);
                    }
                } catch (Exception e) {
                    MagicRealms.LOGGER.warn("Failed to parse spell from NBT: {}", spellsTag.getString(i), e);
                }
            }

            MagicRealms.LOGGER.debug("Loaded {} spells for entity {}: [{}]",
                    persistedSpells.size(),
                    getEntityName(),
                    persistedSpells.stream().map(AbstractSpell::getSpellName).collect(java.util.stream.Collectors.joining(", ")));
        }

        if (!goalsInitialized) {
            MagicRealms.LOGGER.debug("Entity {} loaded from NBT, goals will be reinitialized", getEntityName());
        }

        this.summonerUUID = OwnerHelper.deserializeOwner(compound);
    }

    @Override
    public void die(net.minecraft.world.damagesource.DamageSource damageSource) {
        this.onDeathHelper();
        super.die(damageSource);
    }

    @Override
    public void onRemovedFromLevel() {
        this.onRemovedHelper(this, MobEffectRegistry.RAISE_DEAD_TIMER);
        super.onRemovedFromLevel();
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.getEntity() != null && source.getEntity().is(this.getSummoner())) {
            return true;
        } else if (source.getEntity() != null && source.getEntity() instanceof RandomHumanEntity human
                && human.getSummoner() != null && human.getSummoner().is(this.getSummoner())){
            return true;
        } else {
            return super.isInvulnerableTo(source);
        }
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
                this.hasShield() &&
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
                .setSpells(spells, spells, spells, spells)
                .setDrinksPotions()
        );
    }

    private void setArcherGoal(List<AbstractSpell> spells){
        this.goalSelector.removeAllGoals((goal) -> goal instanceof WizardAttackGoal);
        this.goalSelector.removeAllGoals((goal) -> goal instanceof ChargeArrowAttackGoal);

        this.goalSelector.addGoal(2, new ChargeArrowAttackGoal<>(this, 1.0D, 20, 15.0F));

        this.goalSelector.addGoal(2, new WizardAttackGoal(this, 1.0f, 60, 120)
                .setSpells(spells, spells, spells, spells)
                .setDrinksPotions()
        );
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
                .setSpells(spells, spells, spells, spells)
                .setDrinksPotions()
        );
    }

    private void setWarriorGoal(List<AbstractSpell> spells){
        this.goalSelector.removeAllGoals((goal) -> goal instanceof GenericAnimatedWarlockAttackGoal);
        if(hasShield()){
            this.goalSelector.addGoal(3, new GenericAnimatedWarlockAttackGoal<>(this, 1f, 70, 85)
                    .setMoveset(List.of(
                            new AttackAnimationData(9, "simple_sword_upward_swipe", 5),
                            new AttackAnimationData(8, "simple_sword_lunge_stab", 6),
                            new AttackAnimationData(10, "simple_sword_stab_alternate", 8),
                            new AttackAnimationData(10, "simple_sword_horizontal_cross_swipe", 8)
                    ))
                    .setComboChance(.4f)
                    .setMeleeAttackInverval(20, 40)
                    .setMeleeMovespeedModifier(1.3f)
                    .setSpells(spells, spells, spells, spells)
                    .setDrinksPotions()
            );
        } else {
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
                    .setSpells(spells, spells, spells, spells)
                    .setDrinksPotions()
            );
        }
    }

    private void reapplyGoalsWithPersistedSpells() {
        MagicRealms.LOGGER.debug("Re-applying goals with {} persisted spells for entity {}",
                persistedSpells.size(), getEntityName());

        clearAttackGoals();

        EntityClass entityClass = getEntityClass();
        switch (entityClass) {
            case MAGE -> setMageGoal(persistedSpells);
            case WARRIOR -> setWarriorGoal(persistedSpells);
            case ROGUE -> {
                if (isArcher()) {
                    setArcherGoal(persistedSpells);
                } else {
                    setAssassinGoal(persistedSpells);
                }
            }
        }

        MagicRealms.LOGGER.info("Successfully reapplied goals for {} {}", getEntityName(), entityClass.getName());
    }

    private void reinitializeGoalsAfterLoad() {
        if (spellsGenerated && !persistedSpells.isEmpty()) {
            MagicRealms.LOGGER.info("Reinitializing goals after world load for entity: {} with {} spells",
                    getEntityName(), persistedSpells.size());

            clearAttackGoals();

            // Reaplizar goals según la clase
            EntityClass entityClass = getEntityClass();
            switch (entityClass) {
                case MAGE -> setMageGoal(persistedSpells);
                case WARRIOR -> setWarriorGoal(persistedSpells);
                case ROGUE -> {
                    if (isArcher()) {
                        setArcherGoal(persistedSpells);
                    } else {
                        setAssassinGoal(persistedSpells);
                    }
                }
            }

            MagicRealms.LOGGER.info("Successfully reinitialized goals for {} {} with class-specific attacks",
                    getEntityName(), entityClass.getName());
        } else if (this.entityData.get(INITIALIZED)) {
            MagicRealms.LOGGER.warn("Entity {} was initialized but has no spells, regenerating...", getEntityName());
            generateAndApplySpells();
        }
    }

    private void clearAttackGoals() {
        this.goalSelector.removeAllGoals(goal ->
                goal instanceof WizardAttackGoal ||
                        goal instanceof RangedBowAttackGoal ||
                        goal instanceof GenericAnimatedWarlockAttackGoal
        );
    }

    private void generateAndApplySpells() {
        RandomSource randomSource = this.level().getRandom();
        List<AbstractSpell> spells = SpellListGenerator.generateSpellsForEntity(this, randomSource);
        this.persistedSpells = new ArrayList<>(spells);
        this.spellsGenerated = true;

        EntityClass entityClass = getEntityClass();
        switch (entityClass) {
            case MAGE -> setMageGoal(spells);
            case WARRIOR -> setWarriorGoal(spells);
            case ROGUE -> {
                if (isArcher()) {
                    setArcherGoal(spells);
                } else {
                    setAssassinGoal(spells);
                }
            }
        }
    }

    @Override
    public void performRangedAttack(LivingEntity pTarget, float pDistanceFactor) {
        AbstractArrow abstractarrow = this.getArrow();

        double d0 = pTarget.getX() - this.getX();
        double d1 = pTarget.getY(0.3333333333333333D) - abstractarrow.getY();
        double d2 = pTarget.getZ() - this.getZ();
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);

        float velocity = getArrowVelocity();
        float inaccuracy = getArrowInaccuracy();

        abstractarrow.shoot(d0, d1 + d3 * (double)0.2F, d2, velocity, inaccuracy);

        this.playSound(SoundEvents.SKELETON_SHOOT, 0.8F, 1.2F);

        this.level().addFreshEntity(abstractarrow);
    }

    protected AbstractArrow getArrow() {
        return ArrowTypeManager.createArrowByStarLevel(
                getStarLevel(),
                this.getRandom(),
                this.level(),
                this,
                this.getMainHandItem()
        );
    }

    private float getArrowVelocity() {
        return switch (getStarLevel()) {
            case 1 -> 1.4f;
            case 2 -> 1.6f;
            case 3 -> 1.8f;
            default -> 1.6f;
        };
    }

    private float getArrowInaccuracy() {
        return switch (getStarLevel()) {
            case 1 -> (float)(16 - this.level().getDifficulty().getId() * 4);
            case 2 -> (float)(12 - this.level().getDifficulty().getId() * 3);
            case 3 -> (float)(8 - this.level().getDifficulty().getId() * 2);
            default -> (float)(14 - this.level().getDifficulty().getId() * 4);
        };
    }

    public boolean isChargingArrow() {
        return this.isUsingItem() && this.getMainHandItem().getItem() instanceof BowItem;
    }

    @Override
    public void startUsingItem(InteractionHand pHand) {
        super.startUsingItem(pHand);
        if (pHand == InteractionHand.MAIN_HAND && this.getMainHandItem().getItem() instanceof BowItem) {
            MagicRealms.LOGGER.debug("Archer {} started charging arrow", this.getEntityName());
        }
    }

    @Override
    public void stopUsingItem() {
        if (this.isChargingArrow()) {
            MagicRealms.LOGGER.debug("Archer {} stopped charging arrow", this.getEntityName());
        }
        super.stopUsingItem();
    }

    //GECKOLIB ANIMATIONS

    RawAnimation animationToPlay = null;
    private final AnimationController<RandomHumanEntity> meleeController = new AnimationController<>(this, "keeper_animations", 0, this::predicate);
    private final AnimationController<RandomHumanEntity> archerController = new AnimationController<>(this, "archer_animations", 0, this::predicateArcher);

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

    private PlayState predicateArcher(AnimationState<RandomHumanEntity> animationEvent) {
        var controller = animationEvent.getController();

        if (this.shouldPlayChargeAnimation && this.isChargingArrow()) {
            var currentAnim = controller.getCurrentAnimation();
            boolean needsNewAnimation = currentAnim == null ||
                    !currentAnim.animation().name().equals("charge_arrow");

            if (needsNewAnimation) {
                controller.forceAnimationReset();
                controller.setAnimation(RawAnimation.begin().thenPlay("charge_arrow"));
                MagicRealms.LOGGER.debug("Started charge_arrow animation for {}", getEntityName());
            }
            return PlayState.CONTINUE;
        }

        if (this.animationToPlay != null) {
            controller.forceAnimationReset();
            controller.setAnimation(animationToPlay);
            animationToPlay = null;
            return PlayState.CONTINUE;
        }

        return PlayState.STOP;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        if (this.isArcher()) {
            controllerRegistrar.add(archerController);
        } else {
            controllerRegistrar.add(meleeController);
        }
        super.registerControllers(controllerRegistrar);
    }

    @Override
    public boolean isAnimating() {
        boolean meleeAnimating = meleeController.getAnimationState() != AnimationController.State.STOPPED;
        boolean archerAnimating = false;

        if (this.isArcher()) {
            archerAnimating = archerController.getAnimationState() != AnimationController.State.STOPPED;
        }

        return meleeAnimating || archerAnimating || super.isAnimating();
    }

    private void handleArcherAnimations() {
        boolean isCurrentlyCharging = this.isChargingArrow();

        if (isCurrentlyCharging && !wasChargingArrow) {
            shouldPlayChargeAnimation = true;
            wasChargingArrow = true;
        }
        else if (!isCurrentlyCharging && wasChargingArrow) {
            shouldPlayChargeAnimation = false;
            wasChargingArrow = false;
        }
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

    //SUMMONING

    protected LivingEntity cachedSummoner;
    protected UUID summonerUUID;

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

    @Override
    public void onUnSummon() {

    }

    @Override
    public boolean isAlliedTo(Entity pEntity) {
        return super.isAlliedTo(pEntity) || this.isAlliedHelper(pEntity);
    }
}
