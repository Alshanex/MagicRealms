package net.alshanex.magic_realms.entity;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.mobs.IAnimatedAttacker;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.NeutralWizard;
import io.redspace.ironsspellbooks.entity.mobs.goals.*;
import io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss.NotIdioticNavigation;
import io.redspace.ironsspellbooks.entity.spells.devour_jaw.DevourJaw;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.util.OwnerHelper;
import net.alshanex.magic_realms.Config;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.*;
import net.alshanex.magic_realms.entity.tavernkeep.TavernKeeperEntity;
import net.alshanex.magic_realms.events.TavernInteractionHandler;
import net.alshanex.magic_realms.network.SyncEntityLevelPacket;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.util.MRUtils;
import net.alshanex.magic_realms.util.ModTags;
import net.alshanex.magic_realms.util.humans.goals.HumanGoals;
import net.alshanex.magic_realms.util.humans.goals.MercenaryArchery;
import net.alshanex.magic_realms.util.humans.goals.MercenaryGoalManager;
import net.alshanex.magic_realms.util.humans.goals.battle_goals.*;
import net.alshanex.magic_realms.util.humans.mercenaries.*;
import net.alshanex.magic_realms.util.humans.mercenaries.personality.PersonalityInitializer;
import net.alshanex.magic_realms.util.humans.stats.HumanStatsManager;
import net.alshanex.magic_realms.util.humans.stats.LevelingStatsManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
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
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.animation.AnimationState;

import javax.annotation.Nullable;
import java.util.*;

public abstract class AbstractMercenaryEntity extends NeutralWizard implements IAnimatedAttacker, RangedAttackMob, InventoryCarrier {

    private static final EntityDataAccessor<Byte> DATA_FLAGS_ID =
            SynchedEntityData.defineId(AbstractMercenaryEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> GENDER =
            SynchedEntityData.defineId(AbstractMercenaryEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ENTITY_CLASS =
            SynchedEntityData.defineId(AbstractMercenaryEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> ENTITY_NAME =
            SynchedEntityData.defineId(AbstractMercenaryEntity.class, EntityDataSerializers.STRING);

    // Bit flags packed into DATA_FLAGS_ID
    private static final byte FLAG_INITIALIZED = 1;
    private static final byte FLAG_HAS_SHIELD = 2;
    private static final byte FLAG_IS_ARCHER = 4;
    private static final byte FLAG_PATROL_MODE = 8;
    private static final byte FLAG_IS_IMMORTAL = 16;
    private static final byte FLAG_IS_STUNNED = 32;
    private static final byte FLAG_IS_SITTING = 64;
    private static final byte FLAG_IS_IN_MENU_STATE = -128;

    private int stunTimer = 0;

    private boolean fearGoalInitialized = false;

    public AbstractSpell lastCastingSpell = null;
    public boolean wasCasting = false;

    public int regenTimer = 0;

    public static final int INVENTORY_SIZE = 42;
    private final SimpleContainer inventory = new SimpleContainer(INVENTORY_SIZE);
    private List<AbstractSpell> spellbookSpells = new ArrayList<>();
    private ItemStack lastEquippedSpellbook = ItemStack.EMPTY;

    private static final int SIT_COOLDOWN_TIME = 600; // 30 seconds

    protected LivingEntity cachedSummoner;
    protected UUID summonerUUID;

    private BattlefieldAnalysis battlefield;

    private final MercenaryArchery archery = new MercenaryArchery(this);

    private boolean isInsideTavern = false;


    // Animations

    private RawAnimation animationToPlay = null;
    private final AnimationController<AbstractMercenaryEntity> meleeController =
            new AnimationController<>(this, "keeper_animations", 0, this::predicate);
    private final AnimationController<AbstractMercenaryEntity> archerController =
            new AnimationController<>(this, "archer_animations", 0, this::predicateArcher);

    public AbstractMercenaryEntity(EntityType<? extends AbstractSpellCastingMob> entityType, Level level) {
        super(entityType, level);
        xpReward = 0;
        this.lookControl = createLookControl();
        this.moveControl = createMoveControl();
    }

    public AbstractMercenaryEntity(Level level, LivingEntity owner,
                                   EntityType<? extends AbstractSpellCastingMob> entityType) {
        this(entityType, level);
        setSummoner(owner);
    }


    // Abstract contract for subclasses

    protected abstract void initializeAppearance(RandomSource randomSource);
    protected abstract void handlePostSpawnInitialization();
    public abstract boolean isExclusiveMercenary();
    @Nullable
    public PersonalityInitializer.FixedPersonality getFixedPersonality() {
        return null;
    }


    // Control construction

    protected LookControl createLookControl() {
        return new LookControl(this) {
            @Override
            protected float rotateTowards(float pFrom, float pTo, float pMaxDelta) {
                return super.rotateTowards(pFrom, pTo, pMaxDelta * 2.5f);
            }
            @Override
            protected boolean resetXRotOnTick() { return getTarget() == null; }
        };
    }

    protected MoveControl createMoveControl() {
        return new MoveControl(this) {
            @Override
            protected float rotlerp(float pSourceAngle, float pTargetAngle, float pMaximumChange) {
                double d0 = this.wantedX - this.mob.getX();
                double d1 = this.wantedZ - this.mob.getZ();
                if (d0 * d0 + d1 * d1 < .5f) return pSourceAngle;
                return super.rotlerp(pSourceAngle, pTargetAngle, pMaximumChange * .25f);
            }
        };
    }


    // Attachment accessors

    public ChairSittingData getChairData() { return this.getData(MRDataAttachments.CHAIR_SITTING); }
    public PatrolData getPatrolData() { return this.getData(MRDataAttachments.PATROL); }
    public FearData getFearData() { return this.getData(MRDataAttachments.FEAR); }
    public MercenaryIdentity getIdentity() { return this.getData(MRDataAttachments.IDENTITY); }

    public MercenaryArchery getArchery() { return archery; }

    public RandomSource getDeterministicRandom() {
        long seed = this.getUUID().getMostSignificantBits() ^ this.getUUID().getLeastSignificantBits();
        return RandomSource.create(seed);
    }

    @Override public HumanoidArm getMainArm() { return HumanoidArm.RIGHT; }
    @Override public boolean isLeftHanded() { return false; }
    @Override public void setLeftHanded(boolean pLeftHanded) { super.setLeftHanded(false); }


    // Getters/setters

    public BattlefieldAnalysis getBattlefield() {
        if (battlefield == null) battlefield = new BattlefieldAnalysis(this);
        return battlefield;
    }

    public void setGender(Gender gender) { this.entityData.set(GENDER, gender.ordinal()); }
    public Gender getGender() { return Gender.values()[this.entityData.get(GENDER)]; }

    public void setEntityClass(EntityClass entityClass) { this.entityData.set(ENTITY_CLASS, entityClass.ordinal()); }
    public EntityClass getEntityClass() { return EntityClass.values()[this.entityData.get(ENTITY_CLASS)]; }

    public void setEntityName(String name) {
        this.entityData.set(ENTITY_NAME, name);
        updateCustomNameWithStars();
    }
    public String getEntityName() { return this.entityData.get(ENTITY_NAME); }

    // Patrol
    public Boolean isPatrolMode() { return getFlag(FLAG_PATROL_MODE); }
    public void setPatrolMode(Boolean patrolMode) {
        setFlag(FLAG_PATROL_MODE, patrolMode);
        if (patrolMode) setPatrolPosition(this.blockPosition());
    }
    public void setPatrolPosition(BlockPos position) { getPatrolData().setPatrolPosition(position); }
    public BlockPos getPatrolPosition() { return getPatrolData().getPatrolPosition(); }

    // Init / menu
    public void setInitialized(boolean initialized) { setFlag(FLAG_INITIALIZED, initialized); }
    public boolean isInitialized() { return getFlag(FLAG_INITIALIZED); }

    public boolean isInMenuState() { return getFlag(FLAG_IS_IN_MENU_STATE); }
    public void setMenuState(boolean inMenu) {
        setFlag(FLAG_IS_IN_MENU_STATE, inMenu);
        if (inMenu) {
            this.getNavigation().stop();
            this.setTarget(null);
        }
    }

    // Star level
    public int getStarLevel() { return getIdentity().getStarLevel(); }
    public void setStarLevel(int starLevel) { getIdentity().setStarLevel(starLevel); }
    protected void initializeStarLevel(RandomSource randomSource) {
        getIdentity().setStarLevel(getInitialStarLevel(randomSource));
    }
    protected int getInitialStarLevel(RandomSource randomSource) {
        double roll = randomSource.nextDouble();
        if (roll < 0.6) return 1;
        if (roll < 0.9) return 2;
        return 3;
    }

    // Shield / archer
    public boolean hasShield() { return getFlag(FLAG_HAS_SHIELD); }
    public void setHasShield(boolean hasShield) { setFlag(FLAG_HAS_SHIELD, hasShield); }
    public boolean isArcher() { return getFlag(FLAG_IS_ARCHER); }
    public void setIsArcher(boolean isArcher) { setFlag(FLAG_IS_ARCHER, isArcher); }
    public boolean isAssassin() { return !isArcher(); }

    // Magic schools
    public List<SchoolType> getMagicSchools() { return getIdentity().getMagicSchools(); }
    public void setMagicSchools(List<SchoolType> schools) { getIdentity().setMagicSchools(schools); }
    public boolean hasSchool(SchoolType school) { return getIdentity().hasSchool(school); }

    // Persisted spells
    public List<AbstractSpell> getPersistedSpells() { return getIdentity().getPersistedSpells(); }
    public void setPersistedSpells(List<AbstractSpell> spells) { getIdentity().setPersistedSpells(spells); }
    public boolean areSpellsGenerated() { return getIdentity().areSpellsGenerated(); }
    public void setSpellsGenerated(boolean generated) { getIdentity().setSpellsGenerated(generated); }
    public boolean areGoalsInitialized() { return getIdentity().areGoalsInitialized(); }
    public void setGoalsInitialized(boolean initialized) { getIdentity().setGoalsInitialized(initialized); }

    // Spellbook spells
    public List<AbstractSpell> getSpellbookSpells() { return spellbookSpells; }


    // Chair sitting

    public boolean isSittingInChair() { return getFlag(FLAG_IS_SITTING); }
    public BlockPos getChairPosition() { return getChairData().getChairPosition(); }
    public int getSittingTime() { return getChairData().getSittingTime(); }
    public int getSitCooldown() { return getChairData().getSitCooldown(); }
    public void setSittingTime(int time) { getChairData().setSittingTime(time); }
    public void setSitCooldown(int cooldown) { getChairData().setSitCooldown(cooldown); }

    public void sitInChair(BlockPos chairPos) {
        setFlag(FLAG_IS_SITTING, true);
        ChairSittingData data = getChairData();
        data.setChairPosition(chairPos);
        data.setSittingTime(0);
        this.setPose(Pose.SITTING);
        this.getNavigation().stop();
        this.setTarget(null);
    }

    public void unsitFromChair() {
        if (!isSittingInChair()) return;
        setFlag(FLAG_IS_SITTING, false);
        ChairSittingData data = getChairData();
        data.reset();
        data.setSitCooldown(SIT_COOLDOWN_TIME);
        this.setPose(Pose.STANDING);
        this.moveTo(this.getX(), this.getY() + 0.7, this.getZ());
        notifyChairSittingGoalOfManualUnseat();
    }

    private void notifyChairSittingGoalOfManualUnseat() {
        this.goalSelector.getAvailableGoals().forEach(wrappedGoal -> {
            if (wrappedGoal.getGoal() instanceof HumanGoals.ChairSittingGoal chairGoal) {
                chairGoal.notifyManualUnseat();
            }
        });
    }

    public boolean canSitInChair() {
        return !isSittingInChair() && getSitCooldown() <= 0 && !isStunned();
    }


    // Immortality & stun

    public boolean isImmortal() { return getFlag(FLAG_IS_IMMORTAL); }
    public void setImmortal(boolean immortal) { setFlag(FLAG_IS_IMMORTAL, immortal); }

    public boolean isStunned() { return getFlag(FLAG_IS_STUNNED); }
    public void setStunned(boolean stunned) {
        setFlag(FLAG_IS_STUNNED, stunned);
        if (stunned) {
            this.stunTimer = Config.immortalStunDuration * 20;
            this.setPose(Pose.SITTING);
            this.setTarget(null);
            this.stopUsingItem();
            this.getNavigation().stop();
        } else {
            this.stunTimer = 0;
            this.setPose(Pose.STANDING);
        }
    }

    public int getStunTimer() { return stunTimer; }
    /** Called by the tick handler to count down each tick. */
    public void decrementStunTimer() { if (stunTimer > 0) stunTimer--; }


    // Fear

    protected void initializeFearedEntity(RandomSource randomSource) {
        if (this.isExclusiveMercenary()) return;
        if (randomSource.nextFloat() >= 0.3f) return;

        List<EntityType<?>> fearableEntityTypes = BuiltInRegistries.ENTITY_TYPE.stream()
                .filter(entityType -> {
                    MobCategory category = entityType.getCategory();
                    return category == MobCategory.MONSTER
                            || category == MobCategory.CREATURE
                            || category == MobCategory.WATER_CREATURE
                            || category == MobCategory.UNDERGROUND_WATER_CREATURE
                            || category == MobCategory.AXOLOTLS;
                })
                .filter(entityType ->
                        entityType != EntityType.PLAYER
                                && entityType != EntityType.ITEM
                                && entityType != EntityType.EXPERIENCE_ORB
                                && entityType != EntityType.AREA_EFFECT_CLOUD)
                .toList();

        if (!fearableEntityTypes.isEmpty()) {
            EntityType<?> randomEntity = fearableEntityTypes.get(randomSource.nextInt(fearableEntityTypes.size()));
            setFearedEntity(randomEntity);
        }
    }

    public void setFearedEntity(EntityType<?> entityType) {
        getFearData().setFearedEntityType(entityType);
        if (this.level() != null && !this.level().isClientSide) initializeFearGoal();
    }

    public EntityType<?> getFearedEntity() { return getFearData().getFearedEntityType(); }

    public TagKey<EntityType<?>> getFearedEntityTag() {
        if (!this.isExclusiveMercenary()) return null;
        return getFearData().getFearedEntityTag();
    }

    public void setFearedEntityTag(@Nullable TagKey<EntityType<?>> entityTag) {
        if (!this.isExclusiveMercenary()) {
            MagicRealms.LOGGER.warn("Attempted to set entity tag fear on non-exclusive mercenary: {}", getEntityName());
            return;
        }
        getFearData().setFearedEntityTag(entityTag);
        if (this.level() != null && !this.level().isClientSide) initializeFearGoal();
    }

    public boolean hasFear() {
        FearData fd = getFearData();
        if (fd.getFearedEntityType() != null) return true;
        return this.isExclusiveMercenary() && fd.getFearedEntityTag() != null;
    }

    public boolean isAfraidOf(Entity entity) {
        EntityType<?> feared = getFearedEntity();
        if (feared != null && entity.getType() == feared) return true;
        if (this.isExclusiveMercenary()) {
            TagKey<EntityType<?>> fearedTag = getFearedEntityTag();
            if (fearedTag != null) return entity.getType().is(fearedTag);
        }
        return false;
    }

    private void initializeFearGoal() {
        boolean hasFear = (getFearedEntity() != null)
                || (this.isExclusiveMercenary() && getFearedEntityTag() != null);
        if (hasFear && !fearGoalInitialized) {
            this.goalSelector.removeAllGoals(goal -> goal instanceof HumanGoals.CustomFearGoal);
            this.goalSelector.addGoal(1, new HumanGoals.CustomFearGoal(this, 8.0f, 1.8, 2.0));
            fearGoalInitialized = true;
        }
    }


    // Spell / spellbook lifecycle

    public List<AbstractSpell> extractSpellsFromEquipment() {
        return MercenaryGoalManager.extractSpellsFromEquipment(this);
    }

    public void refreshSpellsAfterEquipmentChange() {
        MercenaryGoalManager.refreshAfterEquipmentChange(this);
    }

    public void reinitializeGoalsAfterLoad() {
        MercenaryGoalManager.reinitializeAfterLoad(this);
    }

    public void updateSpellbookSpells() {
        if (this.getEntityClass() != EntityClass.MAGE) return;

        ItemStack currentOffhand = this.getOffhandItem();
        if (!ItemStack.isSameItemSameComponents(currentOffhand, lastEquippedSpellbook)) {
            List<AbstractSpell> newSpellbookSpells = extractSpellsFromSpellbook(currentOffhand);
            MercenaryGoalManager.updateMageGoalWithSpellbookAndEquipment(this, newSpellbookSpells);
            this.spellbookSpells = new ArrayList<>(newSpellbookSpells);
            this.lastEquippedSpellbook = currentOffhand.copy();
        }
    }

    public static List<AbstractSpell> extractSpellsFromSpellbook(ItemStack spellbook) {
        List<AbstractSpell> spells = new ArrayList<>();
        if (!MRUtils.isSpellbook(spellbook)) return spells;

        ISpellContainer container = ISpellContainer.get(spellbook);
        if (container != null && !container.isEmpty()) {
            container.getActiveSpells().forEach(spellSlot -> {
                AbstractSpell spell = spellSlot.spellData().getSpell();
                if (!ModTags.isSpellInTag(spell, ModTags.SPELL_BLACKLIST)
                        && ModTags.isSchoolInTag(spell.getSchoolType(), ModTags.SCHOOL_WHITELIST)) {
                    spells.add(spell);
                }
            });
        }
        return spells;
    }


    // Inventory

    @Override public SimpleContainer getInventory() { return inventory; }
    @Override public boolean canPickUpLoot() { return !isInMenuState(); }

    @Override
    protected void pickUpItem(ItemEntity itemEntity) {
        InventoryCarrier.pickUpItem(this, this, itemEntity);
        MRUtils.autoEquipBetterEquipment(this);
    }

    @Override
    public boolean wantsToPickUp(ItemStack stack) {
        if (isStunned()) return false;
        return !stack.isEmpty() && this.getInventory().canAddItem(stack);
    }

    public boolean hasItem(ItemStack itemStack, int count) {
        return this.getInventory().countItem(itemStack.getItem()) >= count;
    }

    public int getItemCount(ItemStack itemStack) {
        return this.getInventory().countItem(itemStack.getItem());
    }

    public void clearInventory() { this.getInventory().clearContent(); }


    // Ranged attack

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        archery.performRangedAttack(target, distanceFactor);
    }

    public boolean canPerformRangedAttack() { return archery.canPerformRangedAttack(); }
    public boolean isChargingArrow() { return archery.isChargingArrow(); }

    @Override
    public void startUsingItem(InteractionHand pHand) {
        super.startUsingItem(pHand);
    }


    // Name display

    public void updateCustomNameWithStars() {
        String entityName = this.entityData.get(ENTITY_NAME);
        if (entityName.isEmpty()) return;

        KillTrackerData data = this.getData(MRDataAttachments.KILL_TRACKER);
        int currentLevel = data.getCurrentLevel();
        this.setCustomName(Component.literal(entityName + " Lv. " + currentLevel));
        this.setCustomNameVisible(true);
    }

    public void updateCustomNameWithLevel(int level) {
        String entityName = this.entityData.get(ENTITY_NAME);
        if (entityName.isEmpty()) return;
        this.setCustomName(Component.literal(entityName + " Lv. " + level));
        this.setCustomNameVisible(true);
    }


    // Interaction

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide()) return InteractionResult.FAIL;
        if (player.level().isClientSide()) return InteractionResult.FAIL;

        InteractionResult result = MercenaryInteractionHandler.handleInteraction(this, player, hand);
        if (result != InteractionResult.PASS) return result;

        return super.mobInteract(player, hand);
    }

    // Exposed for the interaction handler.
    public void handleContractInteraction(Player player, ContractData contractData, ItemStack heldItem) {
        MercenaryInteractionHandler.handleContractInteraction(this, player, contractData, heldItem);
    }


    // Spawn initialization

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty,
                                        MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData) {
        RandomSource randomsource = Utils.random;

        if (!this.isInitialized()) {
            initializeStarLevel(randomsource);
            initializeAppearance(randomsource);
            initializePersonality(randomsource);
            initializeClassSpecifics(randomsource);
            initializeDefaultEquipment();
            initializeFearedEntity(randomsource);

            handlePostSpawnInitialization();

            HumanStatsManager.applyClassAttributes(this);

            KillTrackerData killData = this.getData(MRDataAttachments.KILL_TRACKER);
            initializeHumanLevel(randomsource, killData);

            int spawnLevel = killData.getCurrentLevel();
            LevelingStatsManager.applyLevelBasedAttributes(this, spawnLevel);

            this.setInitialized(true);

            initializeClassSpells();

            initializeFearGoal();
            setGoalsInitialized(true);
        } else {
            if (areSpellsGenerated() && !getPersistedSpells().isEmpty()) {
                MercenaryGoalManager.reapplyWithPersistedSpells(this);
                setGoalsInitialized(true);
            }
            initializeFearGoal();
        }

        if (!this.level().isClientSide) {
            PersonalityData personality = this.getData(MRDataAttachments.PERSONALITY);
            if (!personality.isInitialized()) {
                initializePersonality(Utils.random);
            }
        }

        if (!this.level().isClientSide) {
            this.level().getServer().execute(() -> {
                if (this.isAlive() && !this.isRemoved()) requestNameSyncFromClient();
            });
        }

        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData);
    }

    protected void initializePersonality(RandomSource randomSource) {
        RandomSource deterministic = getDeterministicRandom();
        PersonalityInitializer.initializeFor(this, deterministic);
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
            }
        }
    }

    protected void initializeClassSpells() {
        if (!areSpellsGenerated()) {
            RandomSource randomSource = Utils.random;
            List<AbstractSpell> spells = generateSpellsForEntity(randomSource);
            setPersistedSpells(new ArrayList<>(spells));
            setSpellsGenerated(true);
            MercenaryGoalManager.applyForClass(this, spells);
        }
    }

    protected List<AbstractSpell> generateSpellsForEntity(RandomSource randomSource) {
        return SpellListGenerator.generateSpellsForEntity(this, randomSource);
    }

    protected void initializeClassSpecifics(RandomSource randomSource) {
        EntityClass entityClass = getEntityClass();
        switch (entityClass) {
            case MAGE -> setMagicSchools(generateMagicSchools(randomSource));
            case WARRIOR -> setHasShield(randomSource.nextFloat() < 0.25f);
            case ROGUE -> setIsArcher(randomSource.nextFloat() < 0.25f);
        }
    }

    private List<SchoolType> generateMagicSchools(RandomSource random) {
        double roll = random.nextDouble();
        int schoolCount = roll < 0.65 ? 1 : roll < 0.85 ? 2 : roll < 0.95 ? 3 : 4;

        List<SchoolType> availableSchools =
                SchoolRegistry.REGISTRY.stream()
                        .filter(school -> ModTags.isSchoolInTag(school, ModTags.SCHOOL_WHITELIST))
                        .toList();

        List<SchoolType> selectedSchools = new ArrayList<>();
        List<SchoolType> tempAvailable = new ArrayList<>(availableSchools);
        while (selectedSchools.size() < schoolCount && !tempAvailable.isEmpty()) {
            selectedSchools.add(tempAvailable.remove(random.nextInt(tempAvailable.size())));
        }
        return selectedSchools;
    }

    protected void initializeDefaultEquipment() {
        EntityClass entityClass = getEntityClass();
        switch (entityClass) {
            case WARRIOR -> {
                this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WOODEN_SWORD));
                if (hasShield()) {
                    this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                }
            }
            case ROGUE -> {
                if (isArcher()) {
                    this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                    getInventory().addItem(new ItemStack(Items.ARROW, 64));
                } else {
                    this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
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
        this.targetSelector.addGoal(4, new GenericHurtByTargetGoal(this, entity -> entity == getSummoner()).setAlertOthers());
        this.targetSelector.addGoal(5, new HumanGoals.SafeGenericProtectOwnerTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(5, new HumanGoals.HumanHurtByTargetGoal(this));
        this.targetSelector.addGoal(6, new HumanGoals.AlliedHumanDefenseGoal(this));
        this.targetSelector.addGoal(1, new HumanGoals.NoFearTargetGoal(this));
        this.targetSelector.addGoal(7, new TacticalTargetSelectorGoal(this, getBattlefield()));
    }


    // Tick

    @Override
    public void tick() {
        super.tick();
        MercenaryTickHandler.tick(this);

        // Check safe-zone status once per second
        if (!this.level().isClientSide() && this.tickCount % 20 == 0) {
            this.isInsideTavern = TavernInteractionHandler.isPositionInTavern((ServerLevel) this.level(), this.blockPosition());

            // If they just entered the tavern, force them to drop aggro on friends/keepers
            if (this.isInsideTavern && this.getTarget() != null) {
                if (this.getTarget() instanceof AbstractMercenaryEntity || this.getTarget() instanceof TavernKeeperEntity) {
                    this.setTarget(null);
                }
            }
        }
    }


    // Contract utilities

    public void clearContract() {
        ContractData contractData = this.getData(MRDataAttachments.CONTRACT_DATA);
        contractData.clearContract(this.level());
        this.setSummoner(null);
        if (this.getTarget() != null) this.setTarget(null);
        this.setLastHurtByMob(null);
        if (this.isPatrolMode()) {
            this.setPatrolMode(false);
            this.setPatrolPosition(BlockPos.ZERO);
        }
    }

    public boolean isInCombat() {
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) return true;

        LivingEntity lastHurt = this.getLastHurtByMob();
        if (lastHurt != null && lastHurt.isAlive()
                && this.tickCount - this.getLastHurtByMobTimestamp() < 100) {
            return true;
        }

        return this.getMagicData() != null && this.getMagicData().isCasting();
    }


    // State checks & damage handling

    @Override
    public boolean isImmobile() {
        if (isSittingInChair() || isStunned() || isInMenuState()) return true;
        return super.isImmobile();
    }

    @Override
    public Pose getPose() {
        if (isSittingInChair() || isStunned()) return Pose.SITTING;
        return super.getPose();
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (isSittingInChair() || this.isStunned()) return false;

        if (this.isInsideTavern) {
            if (target instanceof AbstractMercenaryEntity || target instanceof TavernKeeperEntity) {
                return false;
            }
        }

        return super.canAttack(target);
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (isSittingInChair() && target != null) unsitFromChair();
        if (this.isStunned() && target != null) return;
        super.setTarget(target);
    }

    @Override public boolean isPushable() { return !isSittingInChair() && super.isPushable(); }

    @Override
    public void travel(Vec3 travelVector) {
        if (isInMenuState() || isSittingInChair() || this.isStunned()) {
            super.travel(Vec3.ZERO);
            return;
        }
        super.travel(travelVector);
    }

    @Override public boolean shouldSheathSword() { return true; }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (this.isInsideTavern && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                && (source.getEntity() instanceof AbstractMercenaryEntity || source.getEntity() instanceof TavernKeeperEntity)) {
            return true;
        }

        if (isStunned() && !(source.getDirectEntity() instanceof DevourJaw)) return true;
        if (source.getEntity() != null && source.getEntity().is(this.getSummoner())) return true;
        if (source.getEntity() instanceof AbstractMercenaryEntity human
                && human.getSummoner() != null
                && human.getSummoner().is(this.getSummoner())) {
            return true;
        }
        return super.isInvulnerableTo(source);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance pPotioneffect) {
        if (isStunned()) return false;
        return super.canBeAffected(pPotioneffect);
    }

    @Override public boolean isPickable() { return super.isPickable(); }

    @Override
    public boolean canBeSeenAsEnemy() {
        if (isStunned()) return false;
        return super.canBeSeenAsEnemy();
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (isStunned()) {
            if (pSource.getDirectEntity() instanceof DevourJaw) {
                this.setImmortal(false);
                return super.hurt(pSource, 0);
            }
            return false;
        }

        if (isInMenuState()) forceCloseContractorMenu();
        if (isSittingInChair() && pAmount > 0) unsitFromChair();
        if (level().isClientSide) return false;

        boolean canParry = this.isAggressive()
                && !isImmobile()
                && !isAnimating()
                && this.hasShield()
                && pSource.getEntity() != null
                && pSource.getSourcePosition() != null
                && pSource.getSourcePosition().subtract(this.position()).normalize().dot(this.getForward()) >= 0.35
                && !pSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY);

        if (canParry && this.random.nextFloat() < 0.25) {
            serverTriggerAnimation("offhand_parry");
            this.playSound(SoundRegistry.FIRE_DAGGER_PARRY.get());
            return false;
        }

        // Process the actual damage
        boolean wasHurt = super.hurt(pSource, pAmount);

        // TAVERN BRAWL LOGIC! >:)
        if (wasHurt && this.isInsideTavern) {
            Entity attacker = pSource.getEntity();

            if (attacker instanceof LivingEntity livingAttacker && !(attacker instanceof AbstractMercenaryEntity) &&
                    !(attacker instanceof TavernKeeperEntity)) {

                // Create a 20-block search area around the attacked mercenary
                AABB alertBox = this.getBoundingBox().inflate(20.0D);

                // Sweep for all nearby mercenaries and make them aggressive
                List<AbstractMercenaryEntity> allies = this.level().getEntitiesOfClass(AbstractMercenaryEntity.class, alertBox);
                for (AbstractMercenaryEntity ally : allies) {
                    if (ally != this && ally.isAlive()) {
                        ally.setTarget(livingAttacker);
                    }
                }
            }
        }

        return wasHurt;
    }

    private void forceCloseContractorMenu() {
        if (!isInMenuState()) return;

        UUID contractorUUID = this.summonerUUID;
        if (contractorUUID != null && !level().isClientSide) {
            Player contractor = level().getPlayerByUUID(contractorUUID);
            if (contractor instanceof ServerPlayer serverPlayer) {
                serverPlayer.closeContainer();
            }
        }
        setMenuState(false);
    }


    // Death & add-to-level

    @Override
    public void die(DamageSource damageSource) {
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
    public void onAddedToLevel() {
        if (!this.level().isClientSide) {
            if(this.isInitialized()){
                if (this.getEntityClass() == EntityClass.MAGE) updateSpellbookSpells();
                refreshSpellsAfterEquipmentChange();
            }

            PersonalityData data = this.getData(MRDataAttachments.PERSONALITY);
            if (!data.isInitialized()){
                PersonalityInitializer.initializeFor(this, getDeterministicRandom());
            }
        }
        super.onAddedToLevel();
    }


    // Animation

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

        if (archery.shouldPlayChargeAnimation() && archery.isChargingArrow()) {
            var currentAnim = controller.getCurrentAnimation();
            boolean needsNewAnimation = currentAnim == null
                    || !currentAnim.animation().name().equals("charge_arrow");
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
        if (this.isArcher()) controllerRegistrar.add(archerController);
        else controllerRegistrar.add(meleeController);
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


    // Summoning

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
        return super.isAlliedTo(pEntity) || isAlliedHelper(pEntity);
    }

    private boolean isAlliedHelper(Entity entity) {
        var owner = getSummoner();
        if (owner == null) return false;
        if (entity.is(owner)) return true;

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

    protected boolean isHostileHuman() { return false; }


    // Navigation

    @Override public boolean guardsBlocks() { return false; }
    @Override public boolean isHostileTowards(LivingEntity pTarget) { return super.isHostileTowards(pTarget); }
    @Override protected PathNavigation createNavigation(Level pLevel) { return new NotIdioticNavigation(this, pLevel); }

    public static AttributeSupplier.Builder prepareAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.ATTACK_KNOCKBACK, 0.0)
                .add(Attributes.MAX_HEALTH, 1.0)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.ENTITY_INTERACTION_RANGE, 3)
                .add(Attributes.MOVEMENT_SPEED, .25);
    }

    @Override public boolean requiresCustomPersistence() { return true; }
    @Override public boolean removeWhenFarAway(double pDistanceToClosestPlayer) { return false; }


    // Data sync & NBT

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        super.defineSynchedData(pBuilder);
        pBuilder.define(DATA_FLAGS_ID, (byte) 0);
        pBuilder.define(GENDER, 0);
        pBuilder.define(ENTITY_CLASS, 0);
        pBuilder.define(ENTITY_NAME, "");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);

        // Flags byte
        compound.putBoolean("Initialized", isInitialized());
        compound.putBoolean("HasShield", hasShield());
        compound.putBoolean("IsArcher", isArcher());
        compound.putBoolean("PatrolMode", isPatrolMode());
        compound.putBoolean("IsImmortal", isImmortal());
        compound.putBoolean("IsStunned", isStunned());
        compound.putBoolean("IsSitting", isSittingInChair());
        compound.putBoolean("IsInMenuState", isInMenuState());

        // Synched entity data we still own
        compound.putInt("Gender", this.entityData.get(GENDER));
        compound.putInt("EntityClass", this.entityData.get(ENTITY_CLASS));
        compound.putString("EntityName", this.entityData.get(ENTITY_NAME));

        // Transient runtime fields we still persist
        compound.putInt("StunTimer", this.stunTimer);

        // Owner / inventory / spellbook
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

        // Flags byte
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
        if (!savedName.isEmpty()) updateCustomNameWithStars();

        this.stunTimer = compound.getInt("StunTimer");

        // Legacy NBT migration for pre-attachment saves
        MRUtils.migrateLegacyNbtIfNeeded(compound, getIdentity(), getChairData(), getPatrolData(), getFearData());

        this.summonerUUID = OwnerHelper.deserializeOwner(compound);

        initializeFearGoal();
    }


    // Flag byte helpers

    private boolean getFlag(byte flag) {
        return (this.entityData.get(DATA_FLAGS_ID) & flag) != 0;
    }

    private void setFlag(byte flag, boolean value) {
        byte currentFlags = this.entityData.get(DATA_FLAGS_ID);
        if (value) this.entityData.set(DATA_FLAGS_ID, (byte) (currentFlags | flag));
        else this.entityData.set(DATA_FLAGS_ID, (byte) (currentFlags & ~flag));
    }
}