package net.alshanex.magic_realms.entity;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.mobs.IAnimatedAttacker;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.NeutralWizard;
import io.redspace.ironsspellbooks.entity.mobs.goals.*;
import io.redspace.ironsspellbooks.entity.mobs.goals.melee.AttackAnimationData;
import io.redspace.ironsspellbooks.entity.mobs.wizards.GenericAnimatedWarlockAttackGoal;
import io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss.NotIdioticNavigation;
import io.redspace.ironsspellbooks.entity.spells.devour_jaw.DevourJaw;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.util.OwnerHelper;
import net.alshanex.magic_realms.Config;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.block.ChairBlock;
import net.alshanex.magic_realms.data.*;
import net.alshanex.magic_realms.events.MagicAttributeGainsHandler;
import net.alshanex.magic_realms.item.PermanentContractItem;
import net.alshanex.magic_realms.item.TieredContractItem;
import net.alshanex.magic_realms.network.SyncEntityLevelPacket;
import net.alshanex.magic_realms.particles.StunParticleEffect;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.registry.MRItems;
import net.alshanex.magic_realms.util.contracts.ContractUtils;
import net.alshanex.magic_realms.util.MRUtils;
import net.alshanex.magic_realms.util.ModTags;
import net.alshanex.magic_realms.util.humans.goals.ChargeArrowAttackGoal;
import net.alshanex.magic_realms.util.humans.goals.HumanGoals;
import net.alshanex.magic_realms.util.humans.goals.battle_goals.*;
import net.alshanex.magic_realms.util.humans.mercenaries.EntityClass;
import net.alshanex.magic_realms.util.humans.mercenaries.Gender;
import net.alshanex.magic_realms.util.humans.mercenaries.SpellListGenerator;
import net.alshanex.magic_realms.util.humans.stats.HumanStatsManager;
import net.alshanex.magic_realms.util.humans.stats.LevelingStatsManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.animation.AnimationState;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractMercenaryEntity extends NeutralWizard implements IAnimatedAttacker, RangedAttackMob, InventoryCarrier {

    // Core entity data
    private static final EntityDataAccessor<Byte> DATA_FLAGS_ID = SynchedEntityData.defineId(AbstractMercenaryEntity.class, EntityDataSerializers.BYTE);

    private static final EntityDataAccessor<Integer> GENDER = SynchedEntityData.defineId(AbstractMercenaryEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ENTITY_CLASS = SynchedEntityData.defineId(AbstractMercenaryEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> ENTITY_NAME = SynchedEntityData.defineId(AbstractMercenaryEntity.class, EntityDataSerializers.STRING);

    // Flags
    private static final byte FLAG_INITIALIZED = 1;
    private static final byte FLAG_HAS_SHIELD = 2;
    private static final byte FLAG_IS_ARCHER = 4;
    private static final byte FLAG_PATROL_MODE = 8;
    private static final byte FLAG_IS_IMMORTAL = 16;
    private static final byte FLAG_IS_STUNNED = 32;
    private static final byte FLAG_IS_SITTING = 64;
    private static final byte FLAG_IS_IN_MENU_STATE = -128;

    // Entity state
    private AbstractSpell lastCastingSpell = null;
    private boolean wasCasting = false;
    private boolean wasChargingArrow = false;
    private boolean shouldPlayChargeAnimation = false;
    private int stunTimer = 0;
    private boolean fearGoalInitialized = false;

    // Inventory and items
    public static final int INVENTORY_SIZE = 42;
    private final SimpleContainer inventory = new SimpleContainer(INVENTORY_SIZE);
    private List<AbstractSpell> spellbookSpells = new ArrayList<>();
    private ItemStack lastEquippedSpellbook = ItemStack.EMPTY;

    // Regeneration
    private int regenTimer = 0;
    private static final int REGEN_INTERVAL = 100;

    // Chair sitting constants
    private static final int MIN_SITTING_TIME = 200; // 10 seconds
    private static final int SIT_COOLDOWN_TIME = 600; // 30 seconds
    private static final int MAX_SITTING_TIME = 1200; // 60 seconds

    // Summoning
    protected LivingEntity cachedSummoner;
    protected UUID summonerUUID;

    private BattlefieldAnalysis battlefield;

    private MercenaryBowFakePlayer bowFakePlayer;

    // Animation
    RawAnimation animationToPlay = null;
    private final AnimationController<AbstractMercenaryEntity> meleeController = new AnimationController<>(this, "keeper_animations", 0, this::predicate);
    private final AnimationController<AbstractMercenaryEntity> archerController = new AnimationController<>(this, "archer_animations", 0, this::predicateArcher);

    public AbstractMercenaryEntity(EntityType<? extends AbstractSpellCastingMob> entityType, Level level) {
        super(entityType, level);
        xpReward = 0;
        this.lookControl = createLookControl();
        this.moveControl = createMoveControl();
    }

    public AbstractMercenaryEntity(Level level, LivingEntity owner, EntityType<? extends AbstractSpellCastingMob> entityType) {
        this(entityType, level);
        setSummoner(owner);
    }

    // Abstract methods for subclasses to implement
    protected abstract void initializeAppearance(RandomSource randomSource);
    protected abstract void handlePostSpawnInitialization();
    public abstract boolean isExclusiveMercenary();

    // Core entity behavior methods
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

    public RandomSource getDeterministicRandom() {
        long seed = this.getUUID().getMostSignificantBits() ^ this.getUUID().getLeastSignificantBits();
        return RandomSource.create(seed);
    }

    private ChairSittingData chairData() { return this.getData(MRDataAttachments.CHAIR_SITTING); }
    private PatrolData patrolData() { return this.getData(MRDataAttachments.PATROL); }
    private FearData fearData() { return this.getData(MRDataAttachments.FEAR); }
    private MercenaryIdentity identity() { return this.getData(MRDataAttachments.IDENTITY); }

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

    // Getters and setters
    public BattlefieldAnalysis getBattlefield() {
        if (battlefield == null) battlefield = new BattlefieldAnalysis(this);
        return battlefield;
    }

    public void setGender(Gender gender) {
        this.entityData.set(GENDER, gender.ordinal());
    }

    public Gender getGender() {
        return Gender.values()[this.entityData.get(GENDER)];
    }

    public void setEntityClass(EntityClass entityClass) {
        this.entityData.set(ENTITY_CLASS, entityClass.ordinal());
    }

    public EntityClass getEntityClass() {
        return EntityClass.values()[this.entityData.get(ENTITY_CLASS)];
    }

    public void setEntityName(String name) {
        this.entityData.set(ENTITY_NAME, name);
        updateCustomNameWithStars();
    }

    public String getEntityName() {
        return this.entityData.get(ENTITY_NAME);
    }

    public Boolean isPatrolMode() {
        return getFlag(FLAG_PATROL_MODE);
    }

    public void setPatrolMode(Boolean patrolMode) {
        setFlag(FLAG_PATROL_MODE, patrolMode);
        if (patrolMode) {
            setPatrolPosition(this.blockPosition());
        }
    }

    public void setPatrolPosition(BlockPos position) {
        patrolData().setPatrolPosition(position);
    }

    public BlockPos getPatrolPosition() {
        return patrolData().getPatrolPosition();
    }

    public void setInitialized(boolean initialized) {
        setFlag(FLAG_INITIALIZED, initialized);
    }

    public boolean isInitialized() {
        return getFlag(FLAG_INITIALIZED);
    }

    public boolean isInMenuState() {
        return getFlag(FLAG_IS_IN_MENU_STATE);
    }

    public void setMenuState(boolean inMenu) {
        setFlag(FLAG_IS_IN_MENU_STATE, inMenu);
        if (inMenu) {
            this.getNavigation().stop();
            this.setTarget(null);
        }
    }

    public MercenaryBowFakePlayer getOrCreateBowFakePlayer(ServerLevel level) {
        if (bowFakePlayer == null || bowFakePlayer.level() != level) {
            bowFakePlayer = new MercenaryBowFakePlayer(level);
        }
        return bowFakePlayer;
    }

    // Star level management
    public int getStarLevel() {
        return identity().getStarLevel();
    }

    public void setStarLevel(int starLevel) {
        identity().setStarLevel(starLevel);
    }

    protected void initializeStarLevel(RandomSource randomSource) {
        identity().setStarLevel(getInitialStarLevel(randomSource));
    }

    protected int getInitialStarLevel(RandomSource randomSource) {
        double roll = randomSource.nextDouble();
        int starLevel;
        if (roll < 0.6) {
            starLevel = 1;
        } else if (roll < 0.9) {
            starLevel = 2;
        } else {
            starLevel = 3;
        }
        return starLevel;
    }

    // Shield and archer properties
    public boolean hasShield() {
        return getFlag(FLAG_HAS_SHIELD);
    }

    public void setHasShield(boolean hasShield) {
        setFlag(FLAG_HAS_SHIELD, hasShield);
    }

    public boolean isArcher() {
        return getFlag(FLAG_IS_ARCHER);
    }

    public void setIsArcher(boolean isArcher) {
        setFlag(FLAG_IS_ARCHER, isArcher);
    }

    public boolean isAssassin() {
        return !isArcher();
    }

    // Magic schools management
    public List<SchoolType> getMagicSchools() {
        return identity().getMagicSchools();
    }

    public void setMagicSchools(List<SchoolType> schools) {
        identity().setMagicSchools(schools);
    }

    public boolean hasSchool(SchoolType school) {
        return identity().hasSchool(school);
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

        List<SchoolType> availableSchools = SchoolRegistry.REGISTRY.stream()
                .filter(school -> ModTags.isSchoolInTag(school, ModTags.SCHOOL_WHITELIST))
                .toList();

        List<SchoolType> selectedSchools = new ArrayList<>();
        List<SchoolType> tempAvailable = new ArrayList<>(availableSchools);

        while (selectedSchools.size() < schoolCount && !tempAvailable.isEmpty()) {
            int index = random.nextInt(tempAvailable.size());
            selectedSchools.add(tempAvailable.remove(index));
        }

        return selectedSchools;
    }

    // Chair sitting management
    public boolean isSittingInChair() {
        return getFlag(FLAG_IS_SITTING);
    }

    public BlockPos getChairPosition() {
        return chairData().getChairPosition();
    }

    public int getSittingTime() {
        return chairData().getSittingTime();
    }

    public int getSitCooldown() {
        return chairData().getSitCooldown();
    }

    public void setSittingTime(int time) {
        chairData().setSittingTime(time);
    }

    public void setSitCooldown(int cooldown) {
        chairData().setSitCooldown(cooldown);
    }

    public void sitInChair(BlockPos chairPos) {
        setFlag(FLAG_IS_SITTING, true);
        ChairSittingData data = chairData();
        data.setChairPosition(chairPos);
        data.setSittingTime(0);
        this.setPose(Pose.SITTING);
        this.getNavigation().stop();
        this.setTarget(null);
    }

    public void unsitFromChair() {
        if (!isSittingInChair()) return;
        setFlag(FLAG_IS_SITTING, false);
        ChairSittingData data = chairData();
        data.reset();
        data.setSitCooldown(SIT_COOLDOWN_TIME);
        this.setPose(Pose.STANDING);
        this.moveTo(this.getX(), this.getY() + 0.7, this.getZ());
        notifyChairSittingGoalOfManualUnseat();
    }

    private void notifyChairSittingGoalOfManualUnseat() {
        // Find and notify the chair sitting goal
        this.goalSelector.getAvailableGoals().forEach(wrappedGoal -> {
            if (wrappedGoal.getGoal() instanceof HumanGoals.ChairSittingGoal chairGoal) {
                chairGoal.notifyManualUnseat();
            }
        });
    }

    public boolean canSitInChair() {
        return !isSittingInChair() && getSitCooldown() <= 0 && !isStunned();
    }

    private boolean isValidChair(BlockPos chairPos) {
        if (chairPos == null || chairPos.equals(BlockPos.ZERO)) {
            return false;
        }
        BlockState blockState = level().getBlockState(chairPos);
        return blockState.getBlock() instanceof ChairBlock;
    }

    // Immortality and stunning
    public boolean isImmortal() {
        return getFlag(FLAG_IS_IMMORTAL);
    }

    public void setImmortal(boolean immortal) {
        setFlag(FLAG_IS_IMMORTAL, immortal);
    }

    public boolean isStunned() {
        return getFlag(FLAG_IS_STUNNED);
    }

    public void setStunned(boolean stunned) {
        setFlag(FLAG_IS_STUNNED, stunned);
        if (stunned) {
            int duration = Config.immortalStunDuration * 20;
            this.stunTimer = duration;
            this.setPose(Pose.SITTING);
            this.setTarget(null);
            this.stopUsingItem();
            this.getNavigation().stop();
        } else {
            this.stunTimer = 0;
            this.setPose(Pose.STANDING);
        }
    }

    public int getStunTimer() {
        return this.stunTimer;
    }

    // Fear system
    protected void initializeFearedEntity(RandomSource randomSource) {
        // Only do random fear initialization for non-exclusive mercenaries
        if (this.isExclusiveMercenary()) {
            // Exclusive mercenaries should have their fear set manually in their constructor or through the setFearedEntityTag method
            return;
        }

        // Original random fear logic for regular mercenaries
        if (randomSource.nextFloat() >= 0.3f) {
            //MagicRealms.LOGGER.debug("Entity {} spawned without fear", getEntityName());
            return;
        }

        List<EntityType<?>> fearableEntityTypes = BuiltInRegistries.ENTITY_TYPE.stream()
                .filter(entityType -> {
                    MobCategory category = entityType.getCategory();
                    return category == MobCategory.MONSTER ||
                            category == MobCategory.CREATURE ||
                            category == MobCategory.WATER_CREATURE ||
                            category == MobCategory.UNDERGROUND_WATER_CREATURE ||
                            category == MobCategory.AXOLOTLS;
                })
                .filter(entityType -> {
                    return entityType != EntityType.PLAYER &&
                            entityType != EntityType.ITEM &&
                            entityType != EntityType.EXPERIENCE_ORB &&
                            entityType != EntityType.AREA_EFFECT_CLOUD;
                })
                .toList();

        if (!fearableEntityTypes.isEmpty()) {
            EntityType<?> randomEntity = fearableEntityTypes.get(randomSource.nextInt(fearableEntityTypes.size()));
            setFearedEntity(randomEntity);
            /*
            MagicRealms.LOGGER.debug("Entity {} now fears: {} (Category: {})",
                    getEntityName(), randomEntity.getDescriptionId(), randomEntity.getCategory().getName());

             */
        }
    }

    public void setFearedEntity(EntityType<?> entityType) {
        fearData().setFearedEntityType(entityType);
        if (this.level() != null && !this.level().isClientSide) {
            initializeFearGoal();
        }
    }

    public EntityType<?> getFearedEntity() {
        return fearData().getFearedEntityType();
    }

    public TagKey<EntityType<?>> getFearedEntityTag() {
        if (!this.isExclusiveMercenary()) return null;
        return fearData().getFearedEntityTag();
    }

    public void setFearedEntityTag(@Nullable TagKey<EntityType<?>> entityTag) {
        if (!this.isExclusiveMercenary()) {
            MagicRealms.LOGGER.warn("Attempted to set entity tag fear on non-exclusive mercenary: {}", this.getEntityName());
            return;
        }
        fearData().setFearedEntityTag(entityTag);
        if (this.level() != null && !this.level().isClientSide) {
            initializeFearGoal();
        }
    }

    public boolean hasFear() {
        FearData fd = fearData();
        if (fd.getFearedEntityType() != null) return true;
        return this.isExclusiveMercenary() && fd.getFearedEntityTag() != null;
    }

    public boolean isAfraidOf(Entity entity) {
        // First check individual entity type (for compatibility with random mercenaries)
        EntityType<?> feared = getFearedEntity();
        if (feared != null && entity.getType() == feared) {
            return true;
        }

        // Then check entity tag (for exclusive mercenaries)
        if (this.isExclusiveMercenary()) {
            TagKey<EntityType<?>> fearedTag = getFearedEntityTag();
            if (fearedTag != null) {
                return entity.getType().is(fearedTag);
            }
        }

        return false;
    }

    private void initializeFearGoal() {
        boolean hasFear = (getFearedEntity() != null) || (this.isExclusiveMercenary() && getFearedEntityTag() != null);

        if (hasFear && !fearGoalInitialized) {
            this.goalSelector.removeAllGoals(goal -> goal instanceof HumanGoals.CustomFearGoal);
            this.goalSelector.addGoal(1, new HumanGoals.CustomFearGoal(this, 8.0f, 1.8, 2.0));
            fearGoalInitialized = true;
        }
    }

    // Spell management
    public List<AbstractSpell> getPersistedSpells() {
        return identity().getPersistedSpells();
    }

    public List<AbstractSpell> extractSpellsFromEquipment() {
        List<AbstractSpell> equipmentSpells = new ArrayList<>();

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack equipment = this.getItemBySlot(slot);
            if (!equipment.isEmpty()) {
                List<AbstractSpell> spells = extractSpellsFromItem(equipment);
                equipmentSpells.addAll(spells);
            }
        }

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (!item.isEmpty() && shouldCheckInventoryItem(item)) {
                List<AbstractSpell> spells = extractSpellsFromItem(item);
                equipmentSpells.addAll(spells);
            }
        }

        return equipmentSpells;
    }

    private List<AbstractSpell> extractSpellsFromItem(ItemStack item) {
        List<AbstractSpell> spells = new ArrayList<>();
        if (!ISpellContainer.isSpellContainer(item)) {
            return spells;
        }

        ISpellContainer container = ISpellContainer.get(item);
        if (container != null && !container.isEmpty()) {
            container.getActiveSpells().forEach(spellSlot -> {
                AbstractSpell spell = spellSlot.spellData().getSpell();
                if (!ModTags.isSpellInTag(spell, ModTags.SPELL_BLACKLIST) &&
                        ModTags.isSchoolInTag(spell.getSchoolType(), ModTags.SCHOOL_WHITELIST)) {
                    spells.add(spell);
                }
            });
        }
        return spells;
    }

    private boolean shouldCheckInventoryItem(ItemStack item) {
        return ISpellContainer.isSpellContainer(item);
    }

    public static List<AbstractSpell> extractSpellsFromSpellbook(ItemStack spellbook) {
        List<AbstractSpell> spells = new ArrayList<>();
        if (!MRUtils.isSpellbook(spellbook)) {
            return spells;
        }

        ISpellContainer container = ISpellContainer.get(spellbook);
        if (container != null && !container.isEmpty()) {
            container.getActiveSpells().forEach(spellSlot -> {
                if (!ModTags.isSpellInTag(spellSlot.spellData().getSpell(), ModTags.SPELL_BLACKLIST) &&
                        ModTags.isSchoolInTag(spellSlot.spellData().getSpell().getSchoolType(), ModTags.SCHOOL_WHITELIST)) {
                    spells.add(spellSlot.spellData().getSpell());
                }
            });
        }
        return spells;
    }

    public void updateSpellbookSpells() {
        if (this.getEntityClass() != EntityClass.MAGE) {
            return;
        }

        ItemStack currentOffhand = this.getOffhandItem();
        if (!ItemStack.isSameItemSameComponents(currentOffhand, lastEquippedSpellbook)) {
            List<AbstractSpell> newSpellbookSpells = extractSpellsFromSpellbook(currentOffhand);
            updateMageGoalWithSpellbookAndEquipment(newSpellbookSpells);
            this.spellbookSpells = new ArrayList<>(newSpellbookSpells);
            this.lastEquippedSpellbook = currentOffhand.copy();
            /*
            MagicRealms.LOGGER.debug("Mage {} updated spellbook spells. New count: {}",
                    getEntityName(), newSpellbookSpells.size());

             */
        }
    }

    private void updateMageGoalWithSpellbookAndEquipment(List<AbstractSpell> spellbookSpells) {
        List<AbstractSpell> combinedSpells = new ArrayList<>(getPersistedSpells());
        combinedSpells.addAll(spellbookSpells);

        List<AbstractSpell> equipmentSpells = extractSpellsFromEquipment();
        combinedSpells.addAll(equipmentSpells);

        List<AbstractSpell> uniqueSpells = new ArrayList<>();
        for (AbstractSpell spell : combinedSpells) {
            if (!uniqueSpells.contains(spell) && !ModTags.isSpellInTag(spell, ModTags.SPELL_BLACKLIST)) {
                uniqueSpells.add(spell);
            }
        }

        this.goalSelector.removeAllGoals((goal) -> goal instanceof HumanGoals.HumanWizardAttackGoal);

        if (!uniqueSpells.isEmpty()) {
            List<AbstractSpell> attackSpells = ModTags.filterAttackSpells(uniqueSpells);
            List<AbstractSpell> defenseSpells = ModTags.filterDefenseSpells(uniqueSpells);
            List<AbstractSpell> movementSpells = ModTags.filterMovementSpells(uniqueSpells);
            List<AbstractSpell> supportSpells = ModTags.filterSupportSpells(uniqueSpells);

            this.goalSelector.addGoal(2, new HumanGoals.HumanWizardAttackGoal(this, 1.25f, 25, 50)
                    .setSpells(attackSpells, defenseSpells, movementSpells, supportSpells)
                    .setDrinksPotions()
            );
        } else {
            setMageGoal(getPersistedSpells());
        }
    }

    public void refreshSpellsAfterEquipmentChange() {
        if (!identity().areSpellsGenerated() || getPersistedSpells().isEmpty()) {
            return;
        }

        EntityClass entityClass = getEntityClass();
        //MagicRealms.LOGGER.debug("Equipment changed for {} {}, refreshing spells", getEntityName(), entityClass.getName());

        switch (entityClass) {
            case MAGE -> updateMageGoalWithSpellbookAndEquipment(spellbookSpells);
            case WARRIOR -> setWarriorGoal(getPersistedSpells());
            case ROGUE -> {
                if (isArcher()) {
                    setArcherGoal(getPersistedSpells());
                } else {
                    setAssassinGoal(getPersistedSpells());
                }
            }
        }
    }

    // Inventory management
    @Override
    public SimpleContainer getInventory() {
        return inventory;
    }

    @Override
    public boolean canPickUpLoot() {
        return !isInMenuState();
    }

    @Override
    protected void pickUpItem(ItemEntity itemEntity) {
        InventoryCarrier.pickUpItem(this, this, itemEntity);
        MRUtils.autoEquipBetterEquipment(this);
    }

    @Override
    public boolean wantsToPickUp(ItemStack stack) {
        if (isStunned()) {
            return false;
        }
        return !stack.isEmpty() && this.getInventory().canAddItem(stack);
    }

    public boolean hasItem(ItemStack itemStack, int count) {
        return this.getInventory().countItem(itemStack.getItem()) >= count;
    }

    public int getItemCount(ItemStack itemStack) {
        return this.getInventory().countItem(itemStack.getItem());
    }

    public void clearInventory() {
        this.getInventory().clearContent();
    }

    // Ranged attack implementation
    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        if (!hasArrows()) return;

        ItemStack bow = this.getMainHandItem();
        if (!(bow.getItem() instanceof BowItem bowItem || bow.is(ModTags.BOWS))) return;

        ItemStack arrowStack = getBestArrowFromInventory();
        if (arrowStack.isEmpty()) arrowStack = new ItemStack(Items.ARROW);

        MercenaryBowFakePlayer fp = getOrCreateBowFakePlayer(serverLevel);

        // Aim at target, compensating for arrow drop
        Vec3 shooterPos = new Vec3(this.getX(), this.getEyeY(), this.getZ());
        Vec3 targetPos = new Vec3(target.getX(), target.getY(0.5), target.getZ()); // aim at center mass
        double dx = targetPos.x - shooterPos.x;
        double dy = targetPos.y - shooterPos.y;
        double dz = targetPos.z - shooterPos.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);

        // Arrow physics
        float arrowVelocity = 2.5F;
        double timeTicks = horiz / arrowVelocity;
        double drop = 0.05 * timeTicks * timeTicks * 0.5;

        // Aim point is the target + drop compensation upward
        double aimY = dy + drop;

        float yaw = (float)(Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        float pitch = (float)(-Mth.atan2(aimY, horiz) * (180.0 / Math.PI));

        // Use a COPY of the bow so durability damage doesn't affect our real item mid-flight.
        ItemStack bowCopy = bow.copy();
        fp.syncFrom(this, bowCopy, arrowStack.copyWithCount(1));
        fp.setXRot(pitch);
        fp.setYRot(yaw);
        fp.xRotO = pitch;
        fp.yRotO = yaw;
        fp.yHeadRot = yaw;
        fp.yHeadRotO = yaw;
        fp.yBodyRot = yaw;
        fp.yBodyRotO = yaw;

        boolean fired = false;
        try {
            int useDuration = bow.getUseDuration(fp);
            int chargeTime = useDuration - 20;  // 20 ticks = full draw
            fp.startUsingItem(InteractionHand.MAIN_HAND);
            bow.releaseUsing(serverLevel, fp, chargeTime);
            fired = true;
        } catch (Throwable t) {
            MagicRealms.LOGGER.error("Modded bow {} threw during releaseUsing — falling back to vanilla shot",
                    BuiltInRegistries.ITEM.getKey(bow.getItem()), t);
        } finally {
            try { fp.stopUsingItem(); } catch (Throwable ignored) {}
        }

        // Copy durability damage from bowCopy back to the real bow
        int damageTaken = bowCopy.getDamageValue() - bow.getDamageValue();
        if (damageTaken > 0) {
            bow.setDamageValue(bow.getDamageValue() + damageTaken);
            if (bow.getDamageValue() >= bow.getMaxDamage()) {
                this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                this.playSound(SoundEvents.ITEM_BREAK, 0.8F, 0.8F + this.random.nextFloat() * 0.4F);
            }
        }

        // If modded bow failed, use the vanilla fallback
        if (!fired) {
            fallbackVanillaShoot(target);
            return;
        }

        consumeArrow();
        this.playSound(SoundEvents.SKELETON_SHOOT, 0.8F, 1.2F);
    }

    private void fallbackVanillaShoot(LivingEntity target) {
        // Your ORIGINAL manual shooting code, kept as a safety net
        AbstractArrow abstractarrow = this.getArrow();
        if (abstractarrow == null) return;

        double d0 = target.getX() - this.getX();
        double d1 = target.getY(0.3333) - abstractarrow.getY();
        double d2 = target.getZ() - this.getZ();
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);

        abstractarrow.shoot(d0, d1 + d3 * 0.2, d2, 1.6f, 8f);
        this.playSound(SoundEvents.SKELETON_SHOOT, 0.8F, 1.2F);
        this.level().addFreshEntity(abstractarrow);
        consumeArrow();
    }

    protected AbstractArrow getArrow() {
        if (!hasArrows()) {
            return null;
        }

        ItemStack arrowStack = getBestArrowFromInventory();
        if (arrowStack.isEmpty()) {
            return new Arrow(this.level(), this, arrowStack.copyWithCount(1), this.getMainHandItem());
        }

        if (arrowStack.getItem() instanceof ArrowItem arrowItem) {
            return arrowItem.createArrow(this.level(), arrowStack, this, this.getMainHandItem());
        }

        return new Arrow(this.level(), this, arrowStack.copyWithCount(1), this.getMainHandItem());
    }

    public boolean hasArrows() {
        int count = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (isArrowItem(stack)) {
                count += stack.getCount();
            }
        }
        return count > 0;
    }

    private boolean isArrowItem(ItemStack stack) {
        return stack.getItem() instanceof ArrowItem || stack.is(Items.ARROW);
    }

    private ItemStack getBestArrowFromInventory() {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (isArrowItem(stack) && !stack.is(Items.ARROW) && !stack.isEmpty()) {
                return stack;
            }
        }

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(Items.ARROW) && !stack.isEmpty()) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    private void consumeArrow() {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (isArrowItem(stack) && !stack.is(Items.ARROW) && !stack.isEmpty()) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    inventory.setItem(i, ItemStack.EMPTY);
                }
                return;
            }
        }

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(Items.ARROW) && !stack.isEmpty()) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    inventory.setItem(i, ItemStack.EMPTY);
                }
                return;
            }
        }
    }

    public boolean canPerformRangedAttack() {
        return this.isArcher() && this.hasArrows();
    }

    public boolean isChargingArrow() {
        if (!this.isUsingItem()) return false;
        ItemStack mainHand = this.getMainHandItem();
        return mainHand.getItem() instanceof BowItem || mainHand.is(ModTags.BOWS);
    }

    @Override
    public void startUsingItem(InteractionHand pHand) {
        super.startUsingItem(pHand);
        if (pHand == InteractionHand.MAIN_HAND && this.getMainHandItem().getItem() instanceof BowItem) {
            //MagicRealms.LOGGER.debug("Archer {} started charging arrow", this.getEntityName());
        }
    }

    @Override
    public void stopUsingItem() {
        if (this.isChargingArrow()) {
            //MagicRealms.LOGGER.debug("Archer {} stopped charging arrow", this.getEntityName());
        }
        super.stopUsingItem();
    }

    public void updateCustomNameWithStars() {
        String entityName = this.entityData.get(ENTITY_NAME);
        if (entityName.isEmpty()) {
            return;
        }

        KillTrackerData data = this.getData(MRDataAttachments.KILL_TRACKER);
        int currentLevel = data.getCurrentLevel();
        Component nameComponent = Component.literal(entityName + " Lv. " + currentLevel);
        this.setCustomName(nameComponent);
        this.setCustomNameVisible(true);
    }

    public void updateCustomNameWithLevel(int level) {
        String entityName = this.entityData.get(ENTITY_NAME);
        if (entityName.isEmpty()) {
            return;
        }

        Component nameComponent = Component.literal(entityName + " Lv. " + level);
        this.setCustomName(nameComponent);
        this.setCustomNameVisible(true);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide()) return InteractionResult.FAIL;
        if (player.level().isClientSide()) return InteractionResult.FAIL;

        if (isSittingInChair()) {
            unsitFromChair();
            return InteractionResult.SUCCESS;
        }

        if (isStunned()) {
            return InteractionResult.FAIL;
        }

        ItemStack heldItem = player.getItemInHand(hand);
        ContractData contractData = this.getData(MRDataAttachments.CONTRACT_DATA);

        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.FAIL;
        }

        if (heldItem.is(MRItems.HELL_PASS.get())) {
            if (this.isImmortal()) {
                player.sendSystemMessage(Component.translatable("message.magic_realms.already_immortal",
                        this.getEntityName()).withStyle(ChatFormatting.GOLD));
                return InteractionResult.FAIL;
            }

            this.setImmortal(true);
            if (!player.getAbilities().instabuild) {
                heldItem.shrink(1);
            }

            player.playSound(SoundEvents.TOTEM_USE, 1.0F, 1.0F);
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(Component.translatable("message.magic_realms.granted_immortality",
                        this.getEntityName()).withStyle(ChatFormatting.GOLD));
            }
            return InteractionResult.SUCCESS;
        }

        boolean isContractor = contractData != null && contractData.getContractorUUID() != null && contractData.getContractorUUID().equals(player.getUUID());
        boolean isContractItem = heldItem.getItem() instanceof PermanentContractItem || heldItem.getItem() instanceof TieredContractItem;

        if (isContractor && !isContractItem && this.isInCombat()) {
            if (player instanceof ServerPlayer serverPlayer) {
                Component message = Component.translatable(
                        "message.magic_realms.mercenary.speech",
                        this.getEntityName(),
                        pickCombatRefusalLine()
                ).withStyle(ChatFormatting.RED);
                serverPlayer.sendSystemMessage(message);
            }
            return InteractionResult.SUCCESS;
        }

        handleContractInteraction(player, contractData, heldItem);

        return super.mobInteract(player, hand);
    }

    protected void handleContractInteraction(Player player, ContractData contractData, ItemStack heldItem) {
        if (heldItem.getItem() instanceof PermanentContractItem) {
            if(this.isExclusiveMercenary()){
                if (player instanceof ServerPlayer serverPlayer) {
                    MutableComponent message = Component.translatable("ui.magic_realms.contract_reject_permanent",
                            this.getEntityName()).withStyle(ChatFormatting.GOLD);

                    serverPlayer.sendSystemMessage(message);
                }
            } else {
                ContractUtils.handlePermanentContractCreation(player, this, contractData, heldItem);
            }
        } else if (heldItem.getItem() instanceof TieredContractItem tieredContract) {
            ContractUtils.handleTieredContractCreation(player, this, contractData, heldItem, tieredContract);
        } else {
            ContractUtils.handleContractInteraction(player, this, contractData);
        }
    }

    private Component pickCombatRefusalLine() {
        int variant = this.random.nextInt(4);
        String key = "message.magic_realms.mercenary.busy_fighting." + variant;
        return Component.translatable(key);
    }

    // Core spawn and initialization
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData) {
        RandomSource randomsource = Utils.random;

        if (!this.isInitialized()) {
            initializeStarLevel(randomsource);
            initializeAppearance(randomsource);
            initializeClassSpecifics(randomsource);
            initializeDefaultEquipment();
            initializeFearedEntity(randomsource);

            // Handle post-spawn initialization (abstract method)
            handlePostSpawnInitialization();

            HumanStatsManager.applyClassAttributes(this);

            KillTrackerData killData = this.getData(MRDataAttachments.KILL_TRACKER);
            initializeHumanLevel(randomsource, killData);

            int spawnLevel = killData.getCurrentLevel();
            LevelingStatsManager.applyLevelBasedAttributes(this, spawnLevel);

            //MagicRealms.LOGGER.info("Entity spawned at level {} (from KillTrackerData)", spawnLevel);
            this.setInitialized(true);

            initializeClassSpells();

            initializeFearGoal();
            identity().setGoalsInitialized(true);
        } else {
            if (identity().areSpellsGenerated() && !getPersistedSpells().isEmpty()) {
                reapplyGoalsWithPersistedSpells();
                identity().setGoalsInitialized(true);
            }
            initializeFearGoal();
        }

        if (!this.level().isClientSide) {
            this.level().getServer().execute(() -> {
                if (this.isAlive() && !this.isRemoved()) {
                    requestNameSyncFromClient();
                }
            });
        }

        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData);
    }

    protected void initializeHumanLevel(RandomSource randomSource, KillTrackerData killTrackerData) {
        killTrackerData.initializeRandomSpawnLevel(randomSource);
    }

    protected void requestNameSyncFromClient() {
        if (!this.level().isClientSide && this.isInitialized()) {
            String currentName = this.getEntityName();
            if (currentName == null || currentName.isEmpty()) {
                PacketDistributor.sendToPlayersTrackingEntity(this,
                        new SyncEntityLevelPacket(this.getId(), this.getUUID(),
                                this.getData(MRDataAttachments.KILL_TRACKER).getCurrentLevel()));
                //MagicRealms.LOGGER.debug("Requested name sync from clients for entity {}", this.getUUID());
            }
        }
    }

    protected void initializeClassSpells(){
        if (!identity().areSpellsGenerated()) {
            RandomSource randomSource = Utils.random;
            List<AbstractSpell> spells = generateSpellsForEntity(randomSource);
            identity().setPersistedSpells(new ArrayList<>(spells));
            identity().setSpellsGenerated(true);

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
    }

    protected List<AbstractSpell> generateSpellsForEntity(RandomSource randomSource) {
        return SpellListGenerator.generateSpellsForEntity(this, randomSource);
    }

    protected void initializeClassSpecifics(RandomSource randomSource) {
        EntityClass entityClass = getEntityClass();

        switch (entityClass) {
            case MAGE -> {
                List<SchoolType> schools = generateMagicSchools(randomSource);
                setMagicSchools(schools);
                /*
                MagicRealms.LOGGER.debug("Mage {} assigned schools: [{}]",
                        getEntityName(),
                        schools.stream().map(s -> s.getId().toString()).collect(java.util.stream.Collectors.joining(", ")));

                 */
            }
            case WARRIOR -> {
                boolean hasShield = randomSource.nextFloat() < 0.25f;
                setHasShield(hasShield);
                //MagicRealms.LOGGER.debug("Warrior {} has shield: {}", getEntityName(), hasShield);
            }
            case ROGUE -> {
                boolean isArcher = randomSource.nextFloat() < 0.25f;
                setIsArcher(isArcher);
                /*
                MagicRealms.LOGGER.debug("Rogue {} is archer: {} (subclass: {})",
                        getEntityName(), isArcher, isArcher ? "Archer" : "Assassin");

                 */
            }
        }
    }

    protected void initializeDefaultEquipment() {
        EntityClass entityClass = getEntityClass();

        switch (entityClass) {
            case WARRIOR -> {
                ItemStack sword = new ItemStack(Items.WOODEN_SWORD);
                this.setItemSlot(EquipmentSlot.MAINHAND, sword);
                if (hasShield()) {
                    ItemStack shield = new ItemStack(Items.SHIELD);
                    this.setItemSlot(EquipmentSlot.OFFHAND, shield);
                }
            }
            case ROGUE -> {
                if (isArcher()) {
                    ItemStack bow = new ItemStack(Items.BOW);
                    this.setItemSlot(EquipmentSlot.MAINHAND, bow);
                    ItemStack arrows = new ItemStack(Items.ARROW, 64);
                    getInventory().addItem(arrows);
                } else {
                    ItemStack sword = new ItemStack(Items.STONE_SWORD);
                    this.setItemSlot(EquipmentSlot.MAINHAND, sword);
                }
            }
        }
    }

    // Goal registration
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new WizardRecoverGoal(this));
        this.goalSelector.addGoal(3, new HumanGoals.PatrolAroundPositionGoal(this, 0.8D, 10));
        this.goalSelector.addGoal(3, new HumanGoals.HumanFollowOwnerGoal(this, this::getSummoner, 1.3f, 15, 5, false, 25));
        this.goalSelector.addGoal(4, new HumanGoals.PickupMobDropsGoal(this));
        this.goalSelector.addGoal(9, new HumanGoals.ChairSittingGoal(this));
        this.goalSelector.addGoal(10, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 8.0F));

        this.targetSelector.addGoal(1, new HumanGoals.SafeGenericOwnerHurtByTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(2, new HumanGoals.SafeGenericOwnerHurtTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(3, new HumanGoals.SafeGenericCopyOwnerTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(4, (new GenericHurtByTargetGoal(this, (entity) -> entity == getSummoner())).setAlertOthers());
        this.targetSelector.addGoal(5, new HumanGoals.SafeGenericProtectOwnerTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(5, new HumanGoals.HumanHurtByTargetGoal(this));
        this.targetSelector.addGoal(6, new HumanGoals.AlliedHumanDefenseGoal(this));
        this.targetSelector.addGoal(1, new HumanGoals.NoFearTargetGoal(this));
        this.targetSelector.addGoal(7, new TacticalTargetSelectorGoal(this, getBattlefield()));
    }

    // Core tick method
    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {
            handleSittingTick();
        }

        if (!level().isClientSide && tickCount % 20 == 0 && getTarget() != null) {
            if(this.isAlliedTo(getTarget())) {
                this.setTarget(null);
            }
        }

        if (!level().isClientSide && tickCount % 20 == 0 && isSittingInChair()) {
            resolveChairConflict();
        }

        if(level().isClientSide() && this.isStunned()){
            StunParticleEffect.spawnStunParticles(this, level());
        }

        if (!level().isClientSide && !identity().areGoalsInitialized() && this.isInitialized()) {
            reinitializeGoalsAfterLoad();
            identity().setGoalsInitialized(true);
        }

        if (!level().isClientSide) {
            KillTrackerData killData = this.getData(MRDataAttachments.KILL_TRACKER);
            if(killData.hasNaturalRegen() && this.getHealth() < this.getMaxHealth()){
                handleNaturalRegeneration();
            }

            if (this.isStunned()) {
                handleStunTick();
            }
        }

        if (!level().isClientSide && this.tickCount % 20 == 0) {
            ContractData contractData = this.getData(MRDataAttachments.CONTRACT_DATA);

            if (contractData.hasActiveContract(this.level())) {
                if (!contractData.isPermanent()) {
                    if (this.tickCount % 200 == 0) {
                        contractData.periodicTimeUpdate(this.level());
                    }
                }
            } else {
                UUID previousContractorUUID = contractData.getContractorUUID();
                if (previousContractorUUID != null && !contractData.isPermanent()) {
                    this.clearContract();

                    Player contractor = this.level().getPlayerByUUID(previousContractorUUID);
                    if (contractor instanceof ServerPlayer serverPlayer) {
                        serverPlayer.sendSystemMessage(Component.translatable("ui.magic_realms.contract_expired", this.getEntityName()).withStyle(ChatFormatting.GOLD));
                    }
                }
            }
        }

        if (this.isArcher()) {
            handleArcherAnimations();
            if (!level().isClientSide && this.getTarget() != null && this.tickCount % 10 == 0) {
                if(!this.hasArrows()){
                    this.setTarget(null);
                }
            }
        }

        if (!level().isClientSide) {
            MagicData data = this.getMagicData();
            boolean isCasting = data.isCasting();

            if (wasCasting && !isCasting && lastCastingSpell != null) {
                MagicAttributeGainsHandler.handleSpellCast(this, lastCastingSpell.getSchoolType());
                lastCastingSpell = null;
            }

            if (isCasting && data.getCastingSpell() != null) {
                lastCastingSpell = data.getCastingSpell().getSpell();
            }

            wasCasting = isCasting;
        }
    }

    private void resolveChairConflict() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos chairPos = getChairPosition();
        if (chairPos == null || chairPos.equals(BlockPos.ZERO)) {
            return;
        }

        // Find all entities sitting in this chair
        AABB searchArea = new AABB(chairPos).inflate(1.0);
        List<AbstractMercenaryEntity> sittingEntities = serverLevel.getEntitiesOfClass(
                AbstractMercenaryEntity.class,
                searchArea,
                entity -> entity.isSittingInChair() && chairPos.equals(entity.getChairPosition())
        );

        if (sittingEntities.size() > 1) {

            for (int i = 1; i < sittingEntities.size(); i++) {
                AbstractMercenaryEntity duplicate = sittingEntities.get(i);
                duplicate.unsitFromChair();
            }
        }
    }

    private void handleNaturalRegeneration() {
        regenTimer++;
        if (regenTimer >= REGEN_INTERVAL) {
            float regenAmount = switch (getStarLevel()) {
                case 1 -> 0.5f; // 0.25 hearts
                case 2 -> 1.0f; // 0.5 hearts
                case 3 -> 1.5f; // 0.75 hearts
                default -> 1.0f;
            };
            this.heal(regenAmount);
            regenTimer = 0;
        }
    }

    private void handleStunTick() {
        if (!isStunned()) return;

        if (stunTimer > 0) {
            // Clear target every tick
            this.setTarget(null);

            // Stop all movement and navigation
            this.getNavigation().stop();
            this.setDeltaMovement(0, this.getDeltaMovement().y * 0.1, 0);

            MagicData data = this.getMagicData();
            boolean isCasting = data.isCasting();

            if(isCasting){
                this.cancelCast();
            }

            stunTimer--;
            if (stunTimer <= 0) {
                setStunned(false);
                for (WrappedGoal wrappedGoal : this.goalSelector.getAvailableGoals()) {
                    Goal goal = wrappedGoal.getGoal();
                    if (!wrappedGoal.isRunning() && wrappedGoal.canUse()) {
                        goal.start() ;
                    }
                }
                this.heal(this.getMaxHealth() * 0.5f);
            }
        }
    }

    private void handleSittingTick() {
        chairData().tickCooldown();

        if (isSittingInChair()) {
            chairData().incrementSittingTime();

            BlockPos chairPos = getChairPosition();
            if (!isValidChair(chairPos)) {
                unsitFromChair();
                return;
            }

            Vec3 sittingPos = ChairBlock.getSittingPosition(chairPos, level().getBlockState(chairPos));
            if (this.position().distanceTo(sittingPos) > 0.5) {
                this.moveTo(sittingPos.x, sittingPos.y, sittingPos.z, this.getYRot(), this.getXRot());
            }

            if (chairData().getSittingTime() >= MIN_SITTING_TIME) {
                float unsitChance = 0.01f;
                if (chairData().getSittingTime() > MAX_SITTING_TIME) {
                    unsitChance = 0.1f;
                }
                if (this.getRandom().nextFloat() < unsitChance) {
                    unsitFromChair();
                }
            }
        }
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

    public void clearContract() {
        ContractData contractData = this.getData(MRDataAttachments.CONTRACT_DATA);
        contractData.clearContract(this.level()); // Pass level parameter
        this.setSummoner(null);
        if (this.getTarget() != null) {
            this.setTarget(null);
        }
        this.setLastHurtByMob(null);
        if(this.isPatrolMode()){
            this.setPatrolMode(false);
            this.setPatrolPosition(BlockPos.ZERO);
        }
    }

    public boolean isInCombat() {
        // Currently has a live target
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            return true;
        }
        // Was hurt by a still-alive mob recently (within 5 seconds)
        LivingEntity lastHurt = this.getLastHurtByMob();
        if (lastHurt != null && lastHurt.isAlive()
                && this.tickCount - this.getLastHurtByMobTimestamp() < 100) {
            return true;
        }
        // Currently casting a spell
        if (this.getMagicData() != null && this.getMagicData().isCasting()) {
            return true;
        }
        return false;
    }

    // Overridden behavior methods
    @Override
    public boolean isImmobile() {
        if (isSittingInChair() || isStunned() || isInMenuState()) {
            return true;
        }
        return super.isImmobile();
    }

    @Override
    public Pose getPose() {
        if (isSittingInChair() || isStunned()) {
            return Pose.SITTING;
        }
        return super.getPose();
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (isSittingInChair() || this.isStunned()) {
            return false;
        }
        return super.canAttack(target);
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (isSittingInChair() && target != null) {
            unsitFromChair();
        }
        if (this.isStunned() && target != null) {
            return;
        }
        super.setTarget(target);
    }

    @Override
    public boolean isPushable() {
        if (isSittingInChair()) {
            return false;
        }
        return super.isPushable();
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (isInMenuState() || isSittingInChair() || this.isStunned()) {
            super.travel(Vec3.ZERO);
            return;
        }
        super.travel(travelVector);
    }

    @Override
    public boolean shouldSheathSword() {
        return true;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (isStunned()) {
            if(!(source.getDirectEntity() instanceof DevourJaw)){
                return true;
            }
        }
        if (source.getEntity() != null && source.getEntity().is(this.getSummoner())) {
            return true;
        } else if (source.getEntity() != null && source.getEntity() instanceof AbstractMercenaryEntity human
                && human.getSummoner() != null && human.getSummoner().is(this.getSummoner())){
            return true;
        } else {
            return super.isInvulnerableTo(source);
        }
    }

    @Override
    public boolean canBeAffected(MobEffectInstance pPotioneffect) {
        if (isStunned()) {
            return false;
        }
        return super.canBeAffected(pPotioneffect);
    }

    @Override
    public boolean isPickable() {
        return super.isPickable();
    }

    @Override
    public boolean canBeSeenAsEnemy() {
        if (isStunned()) {
            return false;
        }
        return super.canBeSeenAsEnemy();
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (isStunned()) {
            if(pSource.getDirectEntity() instanceof DevourJaw){
                this.setImmortal(false);
                return super.hurt(pSource, 0);
            }
            return false;
        }

        if (isInMenuState()) {
            forceCloseContractorMenu();
        }

        if (isSittingInChair() && pAmount > 0) {
            unsitFromChair();
        }

        if (level().isClientSide) {
            return false;
        }

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

    private void forceCloseContractorMenu() {
        if (!isInMenuState()) {
            return;
        }

        UUID contractorUUID = this.summonerUUID;
        if (contractorUUID != null && !level().isClientSide) {
            Player contractor = level().getPlayerByUUID(contractorUUID);
            if (contractor instanceof ServerPlayer serverPlayer) {
                serverPlayer.closeContainer();
            }
        }
        setMenuState(false);
    }

    @Override
    public void die(net.minecraft.world.damagesource.DamageSource damageSource) {
        if (!level().isClientSide && !isImmortal()) {
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack itemStack = inventory.getItem(i);
                if (!itemStack.isEmpty()) {
                    ItemEntity itemEntity = new ItemEntity(level(),
                            getX(), getY() + 0.5, getZ(), itemStack);
                    itemEntity.setDefaultPickUpDelay();
                    level().addFreshEntity(itemEntity);
                }
            }
        }
        super.die(damageSource);
    }

    @Override
    public void onAddedToLevel(){
        if (!this.level().isClientSide && this.isInitialized()) {
            if (this.getEntityClass() == EntityClass.MAGE) {
                updateSpellbookSpells();
            }
            refreshSpellsAfterEquipmentChange();
            //MagicRealms.LOGGER.debug("Entity {} refreshed spells on world load", getEntityName());
        }
        super.onAddedToLevel();
    }

    // Goal management methods
    private void setMageGoal(List<AbstractSpell> spells) {
        this.goalSelector.removeAllGoals((goal) -> goal instanceof HumanGoals.HumanWizardAttackGoal);

        List<AbstractSpell> finalSpells = new ArrayList<>(spells);
        if (this.getEntityClass() == EntityClass.MAGE && !spellbookSpells.isEmpty()) {
            finalSpells.addAll(spellbookSpells);
        }

        List<AbstractSpell> equipmentSpells = extractSpellsFromEquipment();
        finalSpells.addAll(equipmentSpells);

        finalSpells = finalSpells.stream()
                .filter(spell -> !ModTags.isSpellInTag(spell, ModTags.SPELL_BLACKLIST))
                .distinct()
                .collect(Collectors.toList());

        List<AbstractSpell> attackSpells = ModTags.filterAttackSpells(finalSpells);
        List<AbstractSpell> defenseSpells = ModTags.filterDefenseSpells(finalSpells);
        List<AbstractSpell> movementSpells = ModTags.filterMovementSpells(finalSpells);
        List<AbstractSpell> supportSpells = ModTags.filterSupportSpells(finalSpells);

        finalSpells.removeIf(
                spell -> attackSpells.contains(spell) || defenseSpells.contains(spell)
                        || movementSpells.contains(spell) || supportSpells.contains(spell)
        );

        if(!finalSpells.isEmpty() && Config.attemptCastUnclassifiedSpells){
            attackSpells.addAll(finalSpells);
        }

        this.goalSelector.addGoal(2, new MageCombatGoal(this, getBattlefield())
                .setSpells(attackSpells, defenseSpells, movementSpells, supportSpells)
                .setDrinksPotions()
        );
    }

    private void setArcherGoal(List<AbstractSpell> spells) {
        this.goalSelector.removeAllGoals((goal) -> goal instanceof HumanGoals.HumanWizardAttackGoal);
        this.goalSelector.removeAllGoals((goal) -> goal instanceof ChargeArrowAttackGoal);

        this.goalSelector.addGoal(2,
                new SniperArcherCombatGoal<AbstractMercenaryEntity>(this, getBattlefield(), 1.0D, 20));

        List<AbstractSpell> finalSpells = new ArrayList<>(spells);
        List<AbstractSpell> equipmentSpells = extractSpellsFromEquipment();
        finalSpells.addAll(equipmentSpells);

        finalSpells = finalSpells.stream()
                .filter(spell -> !ModTags.isSpellInTag(spell, ModTags.SPELL_BLACKLIST))
                .distinct()
                .collect(Collectors.toList());

        List<AbstractSpell> attackSpells = ModTags.filterAttackSpells(finalSpells);
        List<AbstractSpell> defenseSpells = ModTags.filterDefenseSpells(finalSpells);
        List<AbstractSpell> movementSpells = ModTags.filterMovementSpells(finalSpells);
        List<AbstractSpell> supportSpells = ModTags.filterSupportSpells(finalSpells);

        finalSpells.removeIf(
                spell -> attackSpells.contains(spell) || defenseSpells.contains(spell)
                        || movementSpells.contains(spell) || supportSpells.contains(spell)
        );

        if(!finalSpells.isEmpty() && Config.attemptCastUnclassifiedSpells){
            attackSpells.addAll(finalSpells);
        }

        this.goalSelector.addGoal(3, new HumanGoals.HumanWizardAttackGoal(this, 1.0f, 60, 120)
                .setSpells(attackSpells, defenseSpells, movementSpells, supportSpells)
                .setDrinksPotions()
        );
    }

    private void setAssassinGoal(List<AbstractSpell> spells) {
        this.goalSelector.removeAllGoals((goal) -> goal instanceof GenericAnimatedWarlockAttackGoal);

        List<AbstractSpell> finalSpells = new ArrayList<>(spells);
        List<AbstractSpell> equipmentSpells = extractSpellsFromEquipment();
        finalSpells.addAll(equipmentSpells);

        finalSpells = finalSpells.stream()
                .filter(spell -> !ModTags.isSpellInTag(spell, ModTags.SPELL_BLACKLIST))
                .distinct()
                .collect(Collectors.toList());

        List<AbstractSpell> attackSpells = ModTags.filterAttackSpells(finalSpells);
        List<AbstractSpell> defenseSpells = ModTags.filterDefenseSpells(finalSpells);
        List<AbstractSpell> movementSpells = ModTags.filterMovementSpells(finalSpells);
        List<AbstractSpell> supportSpells = ModTags.filterSupportSpells(finalSpells);

        finalSpells.removeIf(
                spell -> attackSpells.contains(spell) || defenseSpells.contains(spell)
                        || movementSpells.contains(spell) || supportSpells.contains(spell)
        );

        if(!finalSpells.isEmpty() && Config.attemptCastUnclassifiedSpells){
            attackSpells.addAll(finalSpells);
        }

        this.goalSelector.addGoal(3, new SkirmisherCombatGoal(this, getBattlefield(), true)
                .setMoveset(List.of(
                        new AttackAnimationData(9, "simple_sword_upward_swipe", 5),
                        new AttackAnimationData(8, "simple_sword_lunge_stab", 6),
                        new AttackAnimationData(10, "simple_sword_stab_alternate", 8),
                        new AttackAnimationData(10, "simple_sword_horizontal_cross_swipe", 8)
                ))
                .setComboChance(.7f)                    // highest combo chance of any class
                .setMeleeAttackInverval(8, 18)          // fastest cadence
                .setMeleeMovespeedModifier(2.0f)        // assassins are the fastest
                .setSpells(attackSpells, defenseSpells, movementSpells, supportSpells)
                .setDrinksPotions()
        );
    }

    private void setWarriorGoal(List<AbstractSpell> spells) {
        this.goalSelector.removeAllGoals((goal) -> goal instanceof GenericAnimatedWarlockAttackGoal || goal instanceof ShieldBashGoal);

        List<AbstractSpell> finalSpells = new ArrayList<>(spells);
        List<AbstractSpell> equipmentSpells = extractSpellsFromEquipment();
        finalSpells.addAll(equipmentSpells);

        finalSpells = finalSpells.stream()
                .filter(spell -> !ModTags.isSpellInTag(spell, ModTags.SPELL_BLACKLIST))
                .distinct()
                .collect(Collectors.toList());

        List<AbstractSpell> attackSpells = ModTags.filterAttackSpells(finalSpells);
        List<AbstractSpell> defenseSpells = ModTags.filterDefenseSpells(finalSpells);
        List<AbstractSpell> movementSpells = ModTags.filterMovementSpells(finalSpells);
        List<AbstractSpell> supportSpells = ModTags.filterSupportSpells(finalSpells);

        finalSpells.removeIf(
                spell -> attackSpells.contains(spell) || defenseSpells.contains(spell)
                        || movementSpells.contains(spell) || supportSpells.contains(spell)
        );

        if(!finalSpells.isEmpty() && Config.attemptCastUnclassifiedSpells){
            attackSpells.addAll(finalSpells);
        }

        if (hasShield()) {
            List<AbstractSpell> tankMovement = ShieldTankCombatGoal.filterMovementForTank(movementSpells);

            this.goalSelector.addGoal(3, new ShieldTankCombatGoal(this, getBattlefield())
                    .setMoveset(List.of(
                            new AttackAnimationData(9, "simple_sword_upward_swipe", 5),
                            new AttackAnimationData(8, "simple_sword_lunge_stab", 6),
                            new AttackAnimationData(10, "simple_sword_stab_alternate", 8),
                            new AttackAnimationData(10, "simple_sword_horizontal_cross_swipe", 8)
                    ))
                    .setComboChance(.4f)
                    .setMeleeAttackInverval(20, 40)
                    .setMeleeMovespeedModifier(1.3f)
                    .setSpells(attackSpells, defenseSpells, tankMovement, supportSpells)
                    .setDrinksPotions()
            );

            this.goalSelector.addGoal(2, new ShieldBashGoal(this, getBattlefield()));
        } else {
            // No-shield warrior — BRAWLER / frontline contender.
            // Aggressive melee pressure like a tank, but periodically creates a small 4-block tactical pocket to drink a potion or finish a non-attack cast, then reengages.
            List<AbstractSpell> brawlerMovement = ShieldTankCombatGoal.filterMovementForTank(movementSpells);

            this.goalSelector.addGoal(3, new BrawlerCombatGoal(this, getBattlefield())
                    .setMoveset(List.of(
                            new AttackAnimationData(9, "simple_sword_upward_swipe", 5),
                            new AttackAnimationData(8, "simple_sword_lunge_stab", 6),
                            new AttackAnimationData(10, "simple_sword_stab_alternate", 8),
                            new AttackAnimationData(10, "simple_sword_horizontal_cross_swipe", 8)
                    ))
                    .setComboChance(.5f)                 // between tank (.4) and assassin (.7)
                    .setMeleeAttackInverval(14, 28)      // aggressive but not assassin-fast
                    .setMeleeMovespeedModifier(1.45f)    // between tank (1.3) and assassin (2.0)
                    .setSpells(attackSpells, defenseSpells, brawlerMovement, supportSpells)
                    .setDrinksPotions()
            );
        }
    }

    private void reapplyGoalsWithPersistedSpells() {
        clearAttackGoals();

        EntityClass entityClass = getEntityClass();
        switch (entityClass) {
            case MAGE -> setMageGoal(getPersistedSpells());
            case WARRIOR -> setWarriorGoal(getPersistedSpells());
            case ROGUE -> {
                if (isArcher()) {
                    setArcherGoal(getPersistedSpells());
                } else {
                    setAssassinGoal(getPersistedSpells());
                }
            }
        }
    }

    private void reinitializeGoalsAfterLoad() {
        if (identity().areSpellsGenerated() && !getPersistedSpells().isEmpty()) {
            clearAttackGoals();

            EntityClass entityClass = getEntityClass();
            switch (entityClass) {
                case MAGE -> setMageGoal(getPersistedSpells());
                case WARRIOR -> setWarriorGoal(getPersistedSpells());
                case ROGUE -> {
                    if (isArcher()) {
                        setArcherGoal(getPersistedSpells());
                    } else {
                        setAssassinGoal(getPersistedSpells());
                    }
                }
            }
        } else if (this.isInitialized()) {
            MagicRealms.LOGGER.warn("Entity {} was initialized but has no spells, regenerating...", getEntityName());
            generateAndApplySpells();
        }
    }

    private void clearAttackGoals() {
        this.goalSelector.removeAllGoals(goal ->
                goal instanceof HumanGoals.HumanWizardAttackGoal ||
                        goal instanceof RangedBowAttackGoal ||
                        goal instanceof ChargeArrowAttackGoal ||
                        goal instanceof GenericAnimatedWarlockAttackGoal ||
                        goal instanceof SniperArcherCombatGoal ||
                        goal instanceof ShieldBashGoal
        );
    }

    private void generateAndApplySpells() {
        RandomSource randomSource = this.level().getRandom();
        List<AbstractSpell> spells = SpellListGenerator.generateSpellsForEntity(this, randomSource);
        identity().setPersistedSpells(new ArrayList<>(spells));
        identity().setSpellsGenerated(true);

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

    // Animation system
    @Override
    public void playAnimation(String animationId) {
        try {
            animationToPlay = RawAnimation.begin().thenPlay(animationId);
        } catch (Exception ignored) {
            MagicRealms.LOGGER.error("Entity {} Failed to play animation: {}", this, animationId);
        }
    }

    private PlayState predicate(AnimationState<AbstractMercenaryEntity> animationEvent) {
        var controller = animationEvent.getController();

        if (this.animationToPlay != null) {
            controller.forceAnimationReset();
            controller.setAnimation(animationToPlay);
            animationToPlay = null;
        }
        return PlayState.CONTINUE;
    }

    private PlayState predicateArcher(AnimationState<AbstractMercenaryEntity> animationEvent) {
        var controller = animationEvent.getController();

        if (this.shouldPlayChargeAnimation && this.isChargingArrow()) {
            var currentAnim = controller.getCurrentAnimation();
            boolean needsNewAnimation = currentAnim == null ||
                    !currentAnim.animation().name().equals("charge_arrow");

            if (needsNewAnimation) {
                controller.forceAnimationReset();
                controller.setAnimation(RawAnimation.begin().thenPlay("charge_arrow"));
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

    // Summoning system
    public LivingEntity getSummoner() {
        return OwnerHelper.getAndCacheOwner(level(), cachedSummoner, summonerUUID);
    }

    public void setSummoner(@Nullable LivingEntity owner) {
        if (owner != null) {
            this.summonerUUID = owner.getUUID();
            this.cachedSummoner = owner;
        } else {
            this.summonerUUID = null;
            this.cachedSummoner = null;
        }
    }

    @Override
    public boolean isAlliedTo(Entity pEntity) {
        return super.isAlliedTo(pEntity) || this.isAlliedHelper(pEntity);
    }

    private boolean isAlliedHelper(Entity entity) {
        var owner = getSummoner();
        if (owner == null) {
            return false;
        }
        if (entity.is(owner)) {
            return true;
        }
        if (entity instanceof IMagicSummon magicSummon) {
            var otherOwner = magicSummon.getSummoner();
            return otherOwner != null && (owner == otherOwner || otherOwner.isAlliedTo(otherOwner));
        } else if (entity instanceof AbstractMercenaryEntity humanEntity) {
            var otherOwner = humanEntity.getSummoner();
            return otherOwner != null && (owner == otherOwner || otherOwner.isAlliedTo(otherOwner));
        } else if (entity instanceof OwnableEntity tamableAnimal) {
            var otherOwner = tamableAnimal.getOwner();
            return otherOwner != null && (owner == otherOwner || otherOwner.isAlliedTo(otherOwner));
        }
        return false;
    }

    protected boolean isHostileHuman(){
        return false;
    }

    // Navigation
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

    // Static methods
    public static AttributeSupplier.Builder prepareAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.ATTACK_KNOCKBACK, 0.0)
                .add(Attributes.MAX_HEALTH, 1.0)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.ENTITY_INTERACTION_RANGE, 3)
                .add(Attributes.MOVEMENT_SPEED, .25);
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double pDistanceToClosestPlayer) {
        return false;
    }

    // Data synchronization
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        super.defineSynchedData(pBuilder);
        pBuilder.define(DATA_FLAGS_ID, (byte)0);
        pBuilder.define(GENDER, 0);
        pBuilder.define(ENTITY_CLASS, 0);
        pBuilder.define(ENTITY_NAME, "");
    }

    // NBT save/load
    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);

        // --- Flags byte ---
        compound.putBoolean("Initialized", isInitialized());
        compound.putBoolean("HasShield", hasShield());
        compound.putBoolean("IsArcher", isArcher());
        compound.putBoolean("PatrolMode", isPatrolMode());
        compound.putBoolean("IsImmortal", isImmortal());
        compound.putBoolean("IsStunned", isStunned());
        compound.putBoolean("IsSitting", isSittingInChair());
        compound.putBoolean("IsInMenuState", isInMenuState());

        // --- Synched entity data we still own ---
        compound.putInt("Gender", this.entityData.get(GENDER));
        compound.putInt("EntityClass", this.entityData.get(ENTITY_CLASS));
        compound.putString("EntityName", this.entityData.get(ENTITY_NAME));

        // --- Transient/runtime-only fields we still persist ---
        compound.putInt("StunTimer", this.stunTimer);

        // --- Owner/inventory/spellbook ---
        OwnerHelper.serializeOwner(compound, summonerUUID);
        this.writeInventoryToTag(compound, this.registryAccess());

        if (!spellbookSpells.isEmpty()) {
            ListTag spellbookSpellsTag = new ListTag();
            for (AbstractSpell spell : spellbookSpells) {
                spellbookSpellsTag.add(StringTag.valueOf(spell.getSpellResource().toString()));
            }
            compound.put("SpellbookSpells", spellbookSpellsTag);
        }
        if (!lastEquippedSpellbook.isEmpty()) {
            CompoundTag spellbookTag = new CompoundTag();
            lastEquippedSpellbook.save(this.registryAccess(), spellbookTag);
            compound.put("LastEquippedSpellbook", spellbookTag);
        }

        compound.putBoolean("RequiresCustomPersistence", true);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);

        // --- Flags byte ---
        byte flags = 0;
        if (compound.getBoolean("Initialized")) flags |= FLAG_INITIALIZED;
        if (compound.getBoolean("HasShield")) flags |= FLAG_HAS_SHIELD;
        if (compound.getBoolean("IsArcher")) flags |= FLAG_IS_ARCHER;
        if (compound.getBoolean("PatrolMode")) flags |= FLAG_PATROL_MODE;
        if (compound.getBoolean("IsImmortal")) flags |= FLAG_IS_IMMORTAL;
        if (compound.getBoolean("IsStunned")) flags |= FLAG_IS_STUNNED;
        if (compound.getBoolean("IsSitting")) flags |= FLAG_IS_SITTING;
        if (compound.getBoolean("IsInMenuState")) flags |= FLAG_IS_IN_MENU_STATE;
        this.entityData.set(DATA_FLAGS_ID, flags);

        this.entityData.set(GENDER, compound.getInt("Gender"));
        this.entityData.set(ENTITY_CLASS, compound.getInt("EntityClass"));

        String savedName = compound.getString("EntityName");
        this.entityData.set(ENTITY_NAME, savedName);
        if (!savedName.isEmpty()) {
            updateCustomNameWithStars();
        }

        this.stunTimer = compound.getInt("StunTimer");

        // --- Legacy NBT migration: copy old keys into new attachments if attachments are empty ---
        MRUtils.migrateLegacyNbtIfNeeded(compound, identity(), chairData(), patrolData(), fearData());

        // --- Owner ---
        this.summonerUUID = OwnerHelper.deserializeOwner(compound);

        // --- Fear goal wiring (attachment is already populated by now) ---
        initializeFearGoal();
    }

    // Helper methods for flag management
    private boolean getFlag(byte flag) {
        return (this.entityData.get(DATA_FLAGS_ID) & flag) != 0;
    }

    private void setFlag(byte flag, boolean value) {
        byte currentFlags = this.entityData.get(DATA_FLAGS_ID);
        if (value) {
            this.entityData.set(DATA_FLAGS_ID, (byte)(currentFlags | flag));
        } else {
            this.entityData.set(DATA_FLAGS_ID, (byte)(currentFlags & ~flag));
        }
    }
}
