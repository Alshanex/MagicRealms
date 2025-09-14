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
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.util.OwnerHelper;
import net.alshanex.magic_realms.Config;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.block.ChairBlock;
import net.alshanex.magic_realms.data.ContractData;
import net.alshanex.magic_realms.data.KillTrackerData;
import net.alshanex.magic_realms.data.VillagerOffersData;
import net.alshanex.magic_realms.events.MagicAttributeGainsHandler;
import net.alshanex.magic_realms.item.PermanentContractItem;
import net.alshanex.magic_realms.item.TieredContractItem;
import net.alshanex.magic_realms.network.SyncEntityLevelPacket;
import net.alshanex.magic_realms.particles.StunParticleEffect;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.registry.MREffects;
import net.alshanex.magic_realms.registry.MREntityRegistry;
import net.alshanex.magic_realms.util.ContractUtils;
import net.alshanex.magic_realms.util.MRUtils;
import net.alshanex.magic_realms.util.ModTags;
import net.alshanex.magic_realms.util.humans.ChargeArrowAttackGoal;
import net.alshanex.magic_realms.util.humans.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.animation.AnimationState;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class RandomHumanEntity extends NeutralWizard implements IAnimatedAttacker, RangedAttackMob, InventoryCarrier {
    private static final EntityDataAccessor<Integer> GENDER = SynchedEntityData.defineId(RandomHumanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ENTITY_CLASS = SynchedEntityData.defineId(RandomHumanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> INITIALIZED = SynchedEntityData.defineId(RandomHumanEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> ENTITY_NAME = SynchedEntityData.defineId(RandomHumanEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> STAR_LEVEL = SynchedEntityData.defineId(RandomHumanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> MAGIC_SCHOOLS = SynchedEntityData.defineId(RandomHumanEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> HAS_SHIELD = SynchedEntityData.defineId(RandomHumanEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_ARCHER = SynchedEntityData.defineId(RandomHumanEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> PATROL_MODE = SynchedEntityData.defineId(RandomHumanEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<BlockPos> PATROL_POSITION = SynchedEntityData.defineId(RandomHumanEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<String> FEARED_ENTITY = SynchedEntityData.defineId(RandomHumanEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> HAS_BEEN_INTERACTED = SynchedEntityData.defineId(RandomHumanEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_IMMORTAL = SynchedEntityData.defineId(RandomHumanEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_STUNNED = SynchedEntityData.defineId(RandomHumanEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> STUN_TIMER = SynchedEntityData.defineId(RandomHumanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> EMERALD_BALANCE = SynchedEntityData.defineId(RandomHumanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_SITTING = SynchedEntityData.defineId(RandomHumanEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<BlockPos> CHAIR_POSITION = SynchedEntityData.defineId(RandomHumanEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Integer> SITTING_TIME = SynchedEntityData.defineId(RandomHumanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SIT_COOLDOWN = SynchedEntityData.defineId(RandomHumanEntity.class, EntityDataSerializers.INT);

    private EntityType<?> fearedEntityType = null;
    private boolean fearGoalInitialized = false;

    private EntityTextureConfig textureConfig;
    private boolean appearanceGenerated = false;

    private List<SchoolType> magicSchools = new ArrayList<>();

    private AbstractSpell lastCastingSpell = null;
    private boolean wasCasting = false;
    private boolean goalsInitialized = false;
    private boolean wasChargingArrow = false;
    private boolean shouldPlayChargeAnimation = false;

    private static final int INVENTORY_SIZE = 27;
    private final SimpleContainer inventory = new SimpleContainer(INVENTORY_SIZE);

    private List<AbstractSpell> spellbookSpells = new ArrayList<>();
    private ItemStack lastEquippedSpellbook = ItemStack.EMPTY;

    private int regenTimer = 0;
    private static final int REGEN_INTERVAL = 100;

    private static final int MIN_SITTING_TIME = 200; // 10 seconds
    private static final int SIT_COOLDOWN_TIME = 600; // 30 seconds
    private static final int MAX_SITTING_TIME = 1200; // 60 seconds

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

    public void setPatrolMode(Boolean patrolMode) {
        this.entityData.set(PATROL_MODE, patrolMode);
        if (patrolMode) {
            // Set current position as patrol center when entering patrol mode
            setPatrolPosition(this.blockPosition());
        }
    }

    public Boolean isPatrolMode() {
        return this.entityData.get(PATROL_MODE);
    }

    public void setPatrolPosition(BlockPos position) {
        this.entityData.set(PATROL_POSITION, position);
    }

    public BlockPos getPatrolPosition() {
        return this.entityData.get(PATROL_POSITION);
    }

    public void setInitialized(boolean initialized) {
        this.entityData.set(INITIALIZED, initialized);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new WizardRecoverGoal(this));
        this.goalSelector.addGoal(3, new HumanGoals.PatrolAroundPositionGoal(this, 0.8D, 10));
        this.goalSelector.addGoal(3, new HumanGoals.HumanFollowOwnerGoal(this, this::getSummoner, 1.3f, 15, 5, false, 25));
        this.goalSelector.addGoal(4, new HumanGoals.PickupMobDropsGoal(this));
        this.goalSelector.addGoal(5, new HumanGoals.GatherResourcesGoal(this));

        this.goalSelector.addGoal(6, new HumanGoals.BuyEquipmentFromVillagersGoal(this));
        this.goalSelector.addGoal(7, new HumanGoals.SellItemsToVillagersGoal(this));
        this.goalSelector.addGoal(8, new HumanGoals.EmeraldOverflowGoal(this));

        this.goalSelector.addGoal(9, new HumanGoals.ChairSittingGoal(this));

        this.goalSelector.addGoal(10, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 8.0F));

        this.targetSelector.addGoal(1, new GenericOwnerHurtByTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(2, new GenericOwnerHurtTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(3, new GenericCopyOwnerTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(4, (new GenericHurtByTargetGoal(this, (entity) -> entity == getSummoner())).setAlertOthers());
        this.targetSelector.addGoal(5, new GenericProtectOwnerTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(5, new HumanGoals.HumanHurtByTargetGoal(this));
        this.targetSelector.addGoal(6, new HumanGoals.AlliedHumanDefenseGoal(this));

        this.targetSelector.addGoal(1, new HumanGoals.NoFearTargetGoal(this));
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
            initializeDefaultEquipment();
            initializeFearedEntity(randomsource);

            HumanStatsManager.applyClassAttributes(this);

            // Initialize random level in the data attachment itself
            KillTrackerData killData = this.getData(MRDataAttachments.KILL_TRACKER);
            killData.initializeRandomSpawnLevel(randomsource);

            // Apply level-based bonuses using the initialized level
            int spawnLevel = killData.getCurrentLevel();
            LevelingStatsManager.applyLevelBasedAttributes(this, spawnLevel);

            MagicRealms.LOGGER.info("Entity spawned at level {} (from KillTrackerData)", spawnLevel);

            giveStartingEmeralds();

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

            initializeFearGoal();
            goalsInitialized = true;
        } else {
            if (spellsGenerated && !persistedSpells.isEmpty()) {
                reapplyGoalsWithPersistedSpells();
                goalsInitialized = true;
            }
            initializeFearGoal();
        }

        if (!this.level().isClientSide) {
            // Schedule the name sync request for next tick to ensure entity is fully initialized
            this.level().getServer().execute(() -> {
                if (this.isAlive() && !this.isRemoved()) {
                    requestNameSyncFromClient();
                }
            });
        }

        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData);
    }

    public void giveStartingEmeralds() {
        int emeraldCount = switch (getStarLevel()) {
            case 1 -> 5 + getRandom().nextInt(6); // 5-10 emeralds
            case 2 -> 10 + getRandom().nextInt(11); // 10-20 emeralds
            case 3 -> 20 + getRandom().nextInt(16); // 20-35 emeralds
            default -> 5;
        };

        addEmeralds(emeraldCount);
    }

    public int getEmeraldBalance() {
        return this.entityData.get(EMERALD_BALANCE);
    }

    public void setEmeraldBalance(int balance) {
        this.entityData.set(EMERALD_BALANCE, Math.max(0, balance));
    }

    public void addEmeralds(int amount) {
        if (amount > 0) {
            setEmeraldBalance(getEmeraldBalance() + amount);
            MagicRealms.LOGGER.debug("Entity {} added {} emeralds to balance, new total: {}",
                    getEntityName(), amount, getEmeraldBalance());
        }
    }

    public boolean removeEmeralds(int amount) {
        int current = getEmeraldBalance();
        if (current >= amount) {
            setEmeraldBalance(current - amount);
            MagicRealms.LOGGER.debug("Entity {} removed {} emeralds from balance, remaining: {}",
                    getEntityName(), amount, getEmeraldBalance());
            return true;
        }
        return false;
    }

    public int getTotalEmeralds() {
        return getEmeraldBalance();
    }

    public int getOverflowEmeralds() {
        return Math.max(0, getTotalEmeralds() - Config.emeraldOverflowThreshold);
    }

    public boolean canUseOverflowEmeralds() {
        if (getOverflowEmeralds() <= 0) {
            return false;
        }
        VillagerOffersData offersData = this.getData(MRDataAttachments.VILLAGER_OFFERS_DATA);
        return !offersData.hasAnyUnaffordableOffers();
    }

    public boolean spendEmeralds(int amount) {
        return removeEmeralds(amount);
    }

    public boolean hasEmeralds(int amount) {
        return getEmeraldBalance() >= amount;
    }

    public void consolidateEmeralds() {
        SimpleContainer inventory = getInventory();
        int inventoryEmeralds = inventory.countItem(Items.EMERALD);

        if (inventoryEmeralds > 0) {
            // Remove all emeralds from inventory and add to balance
            MRUtils.removeItemsFromInventory(inventory, Items.EMERALD, inventoryEmeralds);
            addEmeralds(inventoryEmeralds);
        }
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

    private void initializeDefaultEquipment() {
        EntityClass entityClass = getEntityClass();

        switch (entityClass) {
            case WARRIOR -> {
                // Give wooden sword in main hand
                ItemStack sword = new ItemStack(Items.WOODEN_SWORD);
                this.setItemSlot(EquipmentSlot.MAINHAND, sword);

                // Give shield in off hand if they can block
                if (hasShield()) {
                    ItemStack shield = new ItemStack(Items.SHIELD);
                    this.setItemSlot(EquipmentSlot.OFFHAND, shield);
                }
            }

            case ROGUE -> {
                if (isArcher()) {
                    // Give bow in main hand
                    ItemStack bow = new ItemStack(Items.BOW);
                    this.setItemSlot(EquipmentSlot.MAINHAND, bow);

                    // Give 64 arrows in inventory
                    ItemStack arrows = new ItemStack(Items.ARROW, 64);
                    getInventory().addItem(arrows);
                } else {
                    // Assassin - give stone sword in main hand
                    ItemStack sword = new ItemStack(Items.STONE_SWORD);
                    this.setItemSlot(EquipmentSlot.MAINHAND, sword);
                }
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

    @Override
    public SimpleContainer getInventory() {
        return inventory;
    }

    @Override
    public boolean canPickUpLoot() {
        return true;
    }

    @Override
    protected void pickUpItem(ItemEntity itemEntity) {
        ItemStack pickedUpItem = itemEntity.getItem();

        // Handle emerald pickup specially - always convert to stored balance
        if (pickedUpItem.is(Items.EMERALD)) {
            addEmeralds(pickedUpItem.getCount());
            itemEntity.discard();

            // Play pickup sound
            this.playSound(SoundEvents.ITEM_PICKUP, 0.2F,
                    ((this.random.nextFloat() - this.random.nextFloat()) * 0.7F + 1.0F) * 2.0F);

            recheckUnaffordableOffers();
            return;
        }

        // Handle other items normally
        InventoryCarrier.pickUpItem(this, this, itemEntity);
        MRUtils.autoEquipBetterEquipment(this);

        // Auto-consolidate any emeralds that might have been in the picked up item
        consolidateEmeralds();
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

    private void recheckUnaffordableOffers() {
        VillagerOffersData memory = this.getData(MRDataAttachments.VILLAGER_OFFERS_DATA);

        if (!memory.hasAnyUnaffordableOffers()) {
            return;
        }

        // Create a copy of villager UUIDs to avoid concurrent modification
        Set<UUID> villagerUUIDs = new HashSet<>(memory.getVillagersWithUnaffordableOffers());

        for (UUID villagerUUID : villagerUUIDs) {
            // Check affordability using internal emerald balance
            List<MerchantOffer> nowAffordable = memory.checkAffordableOffersWithBalance(
                    villagerUUID, this.getInventory(), this.getEmeraldBalance());
        }
    }

    private void handleNaturalRegeneration() {
        regenTimer++;

        if (regenTimer >= REGEN_INTERVAL) {
            // Calculate regen amount based on star level
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

    public List<AbstractSpell> extractSpellsFromEquipment() {
        List<AbstractSpell> equipmentSpells = new ArrayList<>();

        // Check all equipment slots
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack equipment = this.getItemBySlot(slot);
            if (!equipment.isEmpty()) {
                List<AbstractSpell> spells = extractSpellsFromItem(equipment);
                equipmentSpells.addAll(spells);
            }
        }

        // Also check inventory for items that might have imbued spells
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
                // Only add if not blacklisted
                if (!ModTags.isSpellInTag(spell, ModTags.SPELL_BLACKLIST)) {
                    spells.add(spell);
                }
            });
        }

        return spells;
    }

    private boolean shouldCheckInventoryItem(ItemStack item) {
        return ISpellContainer.isSpellContainer(item);
    }

    private void updateMageGoalWithSpellbookAndEquipment(List<AbstractSpell> spellbookSpells) {
        // Combine persisted spells, spellbook spells, and equipment spells
        List<AbstractSpell> combinedSpells = new ArrayList<>(persistedSpells);
        combinedSpells.addAll(spellbookSpells);

        // Add equipment spells
        List<AbstractSpell> equipmentSpells = extractSpellsFromEquipment();
        combinedSpells.addAll(equipmentSpells);

        // Remove duplicate spells and blacklisted spells
        List<AbstractSpell> uniqueSpells = new ArrayList<>();
        for (AbstractSpell spell : combinedSpells) {
            if (!uniqueSpells.contains(spell) && !ModTags.isSpellInTag(spell, ModTags.SPELL_BLACKLIST)) {
                uniqueSpells.add(spell);
            }
        }

        // Remove existing wizard attack goal
        this.goalSelector.removeAllGoals((goal) -> goal instanceof HumanGoals.HumanWizardAttackGoal);

        // Add new goal with combined spells
        if (!uniqueSpells.isEmpty()) {
            List<AbstractSpell> attackSpells = ModTags.filterAttackSpells(uniqueSpells);
            List<AbstractSpell> defenseSpells = ModTags.filterDefenseSpells(uniqueSpells);
            List<AbstractSpell> movementSpells = ModTags.filterMovementSpells(uniqueSpells);
            List<AbstractSpell> supportSpells = ModTags.filterSupportSpells(uniqueSpells);

            this.goalSelector.addGoal(2, new HumanGoals.HumanWizardAttackGoal(this, 1.25f, 25, 50)
                    .setSpells(attackSpells, defenseSpells, movementSpells, supportSpells)
                    .setDrinksPotions()
            );

            MagicRealms.LOGGER.debug("Mage {} updated spell roster: {} persisted + {} spellbook + {} equipment = {} total unique",
                    getEntityName(), persistedSpells.size(), spellbookSpells.size(), equipmentSpells.size(), uniqueSpells.size());
        } else {
            // Fallback to just persisted spells
            setMageGoal(persistedSpells);
        }
    }

    public void refreshSpellsAfterEquipmentChange() {
        if (!spellsGenerated || persistedSpells.isEmpty()) {
            return;
        }

        EntityClass entityClass = getEntityClass();
        MagicRealms.LOGGER.debug("Equipment changed for {} {}, refreshing spells", getEntityName(), entityClass.getName());

        switch (entityClass) {
            case MAGE -> {
                // For mages, call the updated method that includes equipment spells
                updateMageGoalWithSpellbookAndEquipment(spellbookSpells);
            }
            case WARRIOR -> setWarriorGoal(persistedSpells);
            case ROGUE -> {
                if (isArcher()) {
                    setArcherGoal(persistedSpells);
                } else {
                    setAssassinGoal(persistedSpells);
                }
            }
        }
    }

    public boolean isImmortal() {
        return this.entityData.get(IS_IMMORTAL);
    }

    public void setImmortal(boolean immortal) {
        this.entityData.set(IS_IMMORTAL, immortal);
    }

    public boolean isStunned() {
        return this.entityData.get(IS_STUNNED);
    }

    public void setStunned(boolean stunned) {
        this.entityData.set(IS_STUNNED, stunned);
        if (stunned) {
            int duration = Config.immortalStunDuration * 20; // Convert seconds to ticks
            this.entityData.set(STUN_TIMER, duration);

            this.setPose(Pose.SITTING);

            this.addEffect(new MobEffectInstance(MREffects.STUN, duration, 0, false, false, true));

            // Clear target and stop attacking
            this.setTarget(null);

            // Stop any current actions
            this.stopUsingItem();
            this.getNavigation().stop();
        } else {
            this.entityData.set(STUN_TIMER, 0);

            this.setPose(Pose.STANDING);
        }
    }

    public int getStunTimer() {
        return this.entityData.get(STUN_TIMER);
    }

    private void handleStunTick() {
        if (!isStunned()) return;

        int currentTimer = getStunTimer();
        if (currentTimer > 0) {
            currentTimer--;
            this.entityData.set(STUN_TIMER, currentTimer);

            if (currentTimer <= 0) {
                setStunned(false);
                this.heal(this.getMaxHealth() * 0.5f); // Heal to 50% when recovering
            }
        }
    }

    @Override
    public boolean isImmobile() {
        if (isSittingInChair()) {
            return true; // Sitting entities can't move
        }
        if (isStunned()) {
            return true; // Stunned entities can't move
        }
        return super.isImmobile();
    }

    @Override
    public Pose getPose() {
        if (isSittingInChair()) {
            return Pose.SITTING;
        }
        if (isStunned()) {
            return Pose.SITTING;
        }
        return super.getPose();
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (isSittingInChair()) {
            return false; // Can't attack while sitting
        }
        if (this.hasEffect(MREffects.STUN)) {
            return false;
        }
        return super.canAttack(target);
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (isSittingInChair() && target != null) {
            unsitFromChair();
        }
        if (this.hasEffect(MREffects.STUN) && target != null) {
            return; // Don't set targets while stunned
        }
        super.setTarget(target);
    }

    @Override
    public boolean isPushable() {
        if (isSittingInChair()) {
            return false; // Don't push sitting entities
        }
        return super.isPushable();
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (isSittingInChair()) {
            // Don't allow movement while sitting
            super.travel(Vec3.ZERO);
            return;
        }

        if (this.hasEffect(MREffects.STUN)) {
            // Force entity to stay still
            super.travel(Vec3.ZERO);
            return;
        }

        super.travel(travelVector);
    }

    private void handleSittingTick() {
        // Handle sit cooldown
        int sitCooldown = getSitCooldown();
        if (sitCooldown > 0) {
            setSitCooldown(sitCooldown - 1);
        }

        // Handle sitting behavior
        if (isSittingInChair()) {
            int sittingTime = getSittingTime();
            setSittingTime(sittingTime + 1);

            // Validate chair still exists
            BlockPos chairPos = getChairPosition();
            if (!isValidChair(chairPos)) {
                unsitFromChair();
                return;
            }

            // Keep entity in sitting position
            Vec3 sittingPos = ChairBlock.getSittingPosition(chairPos, level().getBlockState(chairPos));
            if (this.position().distanceTo(sittingPos) > 0.5) {
                this.moveTo(sittingPos.x, sittingPos.y, sittingPos.z, this.getYRot(), this.getXRot());
            }

            // Check if entity wants to unsit (after minimum time)
            if (sittingTime >= MIN_SITTING_TIME) {
                // 1% chance per tick to unsit after minimum time
                // Higher chance if sitting for a long time
                float unsitChance = 0.01f;
                if (sittingTime > MAX_SITTING_TIME) {
                    unsitChance = 0.1f; // 10% chance if sitting too long
                }

                if (this.getRandom().nextFloat() < unsitChance) {
                    unsitFromChair();
                }
            }
        }
    }

    private boolean isValidChair(BlockPos chairPos) {
        if (chairPos == null || chairPos.equals(BlockPos.ZERO)) {
            return false;
        }

        BlockState blockState = level().getBlockState(chairPos);
        return blockState.getBlock() instanceof ChairBlock;
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {
            handleSittingTick();
        }

        if(level().isClientSide() && this.isStunned()){
            StunParticleEffect.spawnStunParticles(this, (ClientLevel) level());
        }

        if (!level().isClientSide && !goalsInitialized && this.entityData.get(INITIALIZED)) {
            reinitializeGoalsAfterLoad();
            goalsInitialized = true;
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

        if (!level().isClientSide && this.tickCount % 200 == 0) {
            consolidateEmeralds();
        }

        if (!level().isClientSide && this.tickCount % 20 == 0) {
            ContractData contractData = this.getData(MRDataAttachments.CONTRACT_DATA);

            if (contractData.hasActiveContract()) {
                if (!contractData.isPermanent()) {
                    if (this.tickCount % 200 == 0) {
                        contractData.periodicTimeUpdate();
                    }
                }
            } else {
                UUID previousContractorUUID = contractData.getContractorUUID();
                if (previousContractorUUID != null && !contractData.isPermanent()) {
                    contractData.clearContract();
                    this.setSummoner(null);

                    Player contractor = this.level().getPlayerByUUID(previousContractorUUID);
                    if (contractor instanceof ServerPlayer serverPlayer) {
                        serverPlayer.sendSystemMessage(Component.translatable("ui.magic_realms.contract_expired", this.getEntityName()));
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

    public void clearContract() {
        ContractData contractData = this.getData(MRDataAttachments.CONTRACT_DATA);
        contractData.clearContract();
        MagicRealms.LOGGER.debug("Contract manually cleared for entity {}", this.getEntityName());
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
        pBuilder.define(PATROL_MODE, false);
        pBuilder.define(PATROL_POSITION, BlockPos.ZERO);
        pBuilder.define(FEARED_ENTITY, "");
        pBuilder.define(HAS_BEEN_INTERACTED, false);
        pBuilder.define(IS_IMMORTAL, false);
        pBuilder.define(IS_STUNNED, false);
        pBuilder.define(STUN_TIMER, 0);
        pBuilder.define(EMERALD_BALANCE, 0);
        pBuilder.define(IS_SITTING, false);
        pBuilder.define(CHAIR_POSITION, BlockPos.ZERO);
        pBuilder.define(SITTING_TIME, 0);
        pBuilder.define(SIT_COOLDOWN, 0);
    }

    private void initializeRandomAppearance(RandomSource randomSource) {
        if (appearanceGenerated) {
            return;
        }

        Gender gender = Gender.values()[randomSource.nextInt(Gender.values().length)];
        EntityClass entityClass = EntityClass.values()[randomSource.nextInt(EntityClass.values().length)];

        // Initialize basic appearance data
        this.entityData.set(GENDER, gender.ordinal());
        this.entityData.set(ENTITY_CLASS, entityClass.ordinal());

        // Only create texture config on client side
        if (this.level().isClientSide()) {
            this.textureConfig = new EntityTextureConfig(this.getUUID().toString(), gender, entityClass);

            // Check if we got a preset texture with a name
            if (this.textureConfig != null && this.textureConfig.hasTextureName()) {
                // Use the texture name as the entity name
                String textureName = this.textureConfig.getTextureName();
                this.entityData.set(ENTITY_NAME, textureName);

                MagicRealms.LOGGER.info("Entity {} assigned preset texture name: {}",
                        this.getUUID().toString(), textureName);
            } else {
                // Use random generated name for layered textures
                String randomName = AdvancedNameManager.getRandomName(gender);
                this.entityData.set(ENTITY_NAME, randomName);

                MagicRealms.LOGGER.debug("Entity {} assigned random name: {} (layered texture)",
                        this.getUUID().toString(), randomName);
            }
        } else {
            // Server side - DON'T assign name yet, wait for client to determine texture type
            this.textureConfig = null;
            // Leave ENTITY_NAME empty for now - it will be set when texture config is created
            this.entityData.set(ENTITY_NAME, ""); // Empty name initially
        }

        this.appearanceGenerated = true;

        MagicRealms.LOGGER.debug("Initialized appearance for entity {}: Gender={}, Class={}, Stars={}, Name={}",
                this.getUUID().toString(), gender.getName(), entityClass.getName(),
                this.entityData.get(STAR_LEVEL), this.entityData.get(ENTITY_NAME));
    }

    public void updateNameFromTexture() {
        if (!this.level().isClientSide()) {
            return; // Only run on client side
        }

        // Only update if name is empty (hasn't been set yet)
        String currentName = this.entityData.get(ENTITY_NAME);
        if (!currentName.isEmpty()) {
            return; // Name already set
        }

        // Get texture config and check for preset texture name
        EntityTextureConfig textureConfig = this.getTextureConfig();
        if (textureConfig != null && textureConfig.hasTextureName()) {
            String textureName = textureConfig.getTextureName();
            this.entityData.set(ENTITY_NAME, textureName);

            MagicRealms.LOGGER.debug("Updated entity {} name to preset texture name: {}",
                    this.getUUID().toString(), textureName);
        } else {
            // No preset texture name available
            MagicRealms.LOGGER.debug("Entity {} has no preset texture name available (layered texture)",
                    this.getUUID().toString());
        }
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

        if (entityName.isEmpty()) {
            return;
        }

        KillTrackerData data = this.getData(MRDataAttachments.KILL_TRACKER);
        int currentLevel = data.getCurrentLevel();

        Component nameComponent = Component.literal(entityName + " Lv. " + currentLevel);
        this.setCustomName(nameComponent);
        this.setCustomNameVisible(true);
    }

    private void requestNameSyncFromClient() {
        if (!this.level().isClientSide && this.entityData.get(INITIALIZED)) {
            String currentName = this.getEntityName();

            // If name is empty on server, request client to generate and send it
            if (currentName == null || currentName.isEmpty()) {
                // Send packet to all players tracking this entity to request name generation
                PacketDistributor.sendToPlayersTrackingEntity(this,
                        new SyncEntityLevelPacket(this.getId(), this.getUUID(),
                                this.getData(MRDataAttachments.KILL_TRACKER).getCurrentLevel()));

                MagicRealms.LOGGER.debug("Requested name sync from clients for entity {}", this.getUUID());
            }
        }
    }

    public int getStarLevel() {
        return this.entityData.get(STAR_LEVEL);
    }

    public void setStarLevel(int starLevel) {
        if (starLevel >= 1 && starLevel <= 3) {
            this.entityData.set(STAR_LEVEL, starLevel);
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

    public boolean isSittingInChair() {
        return this.entityData.get(IS_SITTING);
    }

    public BlockPos getChairPosition() {
        return this.entityData.get(CHAIR_POSITION);
    }

    public int getSittingTime() {
        return this.entityData.get(SITTING_TIME);
    }

    public int getSitCooldown() {
        return this.entityData.get(SIT_COOLDOWN);
    }

    public void setSittingTime(int time) {
        this.entityData.set(SITTING_TIME, time);
    }

    public void setSitCooldown(int cooldown) {
        this.entityData.set(SIT_COOLDOWN, cooldown);
    }

    public void sitInChair(BlockPos chairPos) {
        this.entityData.set(IS_SITTING, true);
        this.entityData.set(CHAIR_POSITION, chairPos);
        this.entityData.set(SITTING_TIME, 0);

        // Set sitting pose
        this.setPose(Pose.SITTING);

        // Stop navigation and clear target
        this.getNavigation().stop();
        this.setTarget(null);

        MagicRealms.LOGGER.debug("Entity {} sat in chair at {}", getEntityName(), chairPos);
    }

    public void unsitFromChair() {
        if (!isSittingInChair()) {
            return;
        }

        this.entityData.set(IS_SITTING, false);
        this.entityData.set(CHAIR_POSITION, BlockPos.ZERO);
        this.entityData.set(SITTING_TIME, 0);
        this.entityData.set(SIT_COOLDOWN, SIT_COOLDOWN_TIME);

        // Reset pose
        this.setPose(Pose.STANDING);

        MagicRealms.LOGGER.debug("Entity {} unsit from chair, cooldown started", getEntityName());
    }

    public boolean canSitInChair() {
        return !isSittingInChair() && getSitCooldown() <= 0 && !isStunned();
    }

    private void initializeFearedEntity(RandomSource randomSource) {
        // 30% chance to fear an entity
        if (randomSource.nextFloat() >= 0.3f) {
            MagicRealms.LOGGER.debug("Entity {} spawned without fear", getEntityName());
            return;
        }

        // Get all registered entity types that make sense to fear
        List<EntityType<?>> fearableEntityTypes = BuiltInRegistries.ENTITY_TYPE.stream()
                .filter(entityType -> {
                    // Only include entities from these categories
                    MobCategory category = entityType.getCategory();
                    return category == MobCategory.MONSTER ||
                            category == MobCategory.CREATURE ||
                            category == MobCategory.WATER_CREATURE ||
                            category == MobCategory.UNDERGROUND_WATER_CREATURE ||
                            category == MobCategory.AXOLOTLS;
                })
                .filter(entityType -> {
                    // Filter out some specific entity types that don't make sense to fear
                    return entityType != EntityType.PLAYER &&
                            entityType != EntityType.ITEM &&
                            entityType != EntityType.EXPERIENCE_ORB &&
                            entityType != EntityType.AREA_EFFECT_CLOUD &&
                            entityType != MREntityRegistry.HUMAN.get();
                })
                .toList();

        if (!fearableEntityTypes.isEmpty()) {
            EntityType<?> randomEntity = fearableEntityTypes.get(randomSource.nextInt(fearableEntityTypes.size()));
            setFearedEntity(randomEntity);

            MagicRealms.LOGGER.debug("Entity {} now fears: {} (Category: {})",
                    getEntityName(),
                    randomEntity.getDescriptionId(),
                    randomEntity.getCategory().getName());
        }
    }

    public void setFearedEntity(EntityType<?> entityType) {
        this.fearedEntityType = entityType;
        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString();
        this.entityData.set(FEARED_ENTITY, entityId);

        // Reinitialize fear goal if entity is already spawned
        if (this.level() != null && !this.level().isClientSide) {
            initializeFearGoal();
        }
    }

    public EntityType<?> getFearedEntity() {
        if (fearedEntityType == null) {
            String entityId = this.entityData.get(FEARED_ENTITY);
            if (!entityId.isEmpty()) {
                try {
                    ResourceLocation location = ResourceLocation.parse(entityId);
                    fearedEntityType = BuiltInRegistries.ENTITY_TYPE.get(location);
                } catch (Exception e) {
                    MagicRealms.LOGGER.warn("Failed to parse feared entity ID: {}", entityId, e);
                }
            }
        }
        return fearedEntityType;
    }

    public boolean isAfraidOf(Entity entity) {
        EntityType<?> feared = getFearedEntity();
        return feared != null && entity.getType() == feared;
    }

    public boolean hasFear() {
        return getFearedEntity() != null;
    }

    private void initializeFearGoal() {
        if (getFearedEntity() != null && !fearGoalInitialized) {
            // Remove existing fear goals to avoid duplicates
            this.goalSelector.removeAllGoals(goal -> goal instanceof HumanGoals.CustomFearGoal);

            // Fear goal with high priority
            this.goalSelector.addGoal(1, new HumanGoals.CustomFearGoal(this, 16.0f, 1.2, 1.6));

            fearGoalInitialized = true;
        }
    }

    public static List<AbstractSpell> extractSpellsFromSpellbook(ItemStack spellbook) {
        List<AbstractSpell> spells = new ArrayList<>();

        if (!MRUtils.isSpellbook(spellbook)) {
            return spells;
        }

        ISpellContainer container = ISpellContainer.get(spellbook);
        if (container != null && !container.isEmpty()) {
            container.getActiveSpells().forEach(spellSlot -> {
                spells.add(spellSlot.spellData().getSpell());
            });
        }

        return spells;
    }

    public void updateSpellbookSpells() {
        if (this.getEntityClass() != EntityClass.MAGE) {
            return;
        }

        ItemStack currentOffhand = this.getOffhandItem();

        // Check if spellbook has changed
        if (!ItemStack.isSameItemSameComponents(currentOffhand, lastEquippedSpellbook)) {
            // Extract new spellbook spells
            List<AbstractSpell> newSpellbookSpells = extractSpellsFromSpellbook(currentOffhand);

            // Update the spell roster (now includes equipment spells)
            updateMageGoalWithSpellbookAndEquipment(newSpellbookSpells);

            // Cache the current state
            this.spellbookSpells = new ArrayList<>(newSpellbookSpells);
            this.lastEquippedSpellbook = currentOffhand.copy();

            MagicRealms.LOGGER.debug("Mage {} updated spellbook spells. New count: {}",
                    getEntityName(), newSpellbookSpells.size());
        }
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

        if(!this.hasBeenInteracted()){
            this.markAsInteracted();
        }

        ItemStack heldItem = player.getItemInHand(hand);
        ContractData contractData = this.getData(MRDataAttachments.CONTRACT_DATA);

        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.FAIL;
        }

        if (heldItem.is(Items.EMERALD)) {
            boolean tradeSuccessful = handleEmeraldTrade(player, heldItem);
            return tradeSuccessful ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }

        if (heldItem.getItem() instanceof PermanentContractItem) {
            ContractUtils.handlePermanentContractCreation(player, this, contractData, heldItem);
        } else if (heldItem.getItem() instanceof TieredContractItem tieredContract) {
            ContractUtils.handleTieredContractCreation(player, this, contractData, heldItem, tieredContract);
        } else {
            ContractUtils.handleContractInteraction(player, this, contractData);
        }

        return super.mobInteract(player, hand);
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

    private boolean handleEmeraldTrade(Player player, ItemStack emeraldStack) {
        if (emeraldStack.getCount() < 1) {
            return false;
        }

        // Get all non-emerald items from inventory
        List<Integer> availableSlots = new ArrayList<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && !stack.is(Items.EMERALD)) {
                availableSlots.add(i);
            }
        }

        if (availableSlots.isEmpty()) {
            // No items to trade, send message to player
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(Component.translatable("ui.magic_realms.no_items_to_trade", this.getEntityName()));
            }
            return false;
        }

        // Select random item slot
        int randomSlot = availableSlots.get(this.getRandom().nextInt(availableSlots.size()));
        ItemStack tradeItem = inventory.getItem(randomSlot);

        if (tradeItem.isEmpty()) {
            return false;
        }

        // Create a copy of the item to trade (only 1 item)
        ItemStack itemToGive = tradeItem.copyWithCount(1);

        // Remove one item from the entity's inventory
        tradeItem.shrink(1);
        if (tradeItem.isEmpty()) {
            inventory.setItem(randomSlot, ItemStack.EMPTY);
        }

        // Consume one emerald from player
        emeraldStack.shrink(1);

        // Add emerald to entity's stored balance instead of inventory
        this.addEmeralds(1);

        // Give item to player
        if (!player.getInventory().add(itemToGive)) {
            // Player inventory full, drop item at player's location
            ItemEntity itemEntity = new ItemEntity(level(),
                    player.getX(), player.getY() + 0.1, player.getZ(), itemToGive);
            itemEntity.setDefaultPickUpDelay();
            level().addFreshEntity(itemEntity);
        }

        // Play trading sound
        this.playSound(SoundEvents.VILLAGER_TRADE, 1.0F, 1.0F);

        // Send success message
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.translatable("ui.magic_realms.trade_success",
                    this.getEntityName()));
        }
        return true;
    }

    public EntityTextureConfig getTextureConfig() {
        if (textureConfig == null) {
            if (!this.entityData.get(INITIALIZED)) {
                MagicRealms.LOGGER.warn("Trying to get texture config before entity is initialized: {}", this.getUUID().toString());
                return null;
            }

            // Only regenerate texture config on client side
            if (!this.level().isClientSide()) {
                MagicRealms.LOGGER.debug("Server side - not creating texture config for entity {}", this.getEntityName());
                return null;
            }

            // Regenerate texture config
            try {
                Gender gender = this.getGender();
                EntityClass entityClass = this.getEntityClass();
                int newHairTextureIndex = LayeredTextureManager.getRandomHairTextureIndex("hair_" + gender.getName());

                this.textureConfig = new EntityTextureConfig(this.getUUID().toString(), gender, entityClass, newHairTextureIndex);
                MagicRealms.LOGGER.debug("Regenerated texture config for entity {} with hair index: {}", this.getEntityName(), newHairTextureIndex);

                // Update name based on texture type
                updateNameFromTexture();

            } catch (Exception e) {
                MagicRealms.LOGGER.error("Failed to regenerate texture config for entity {}", this.getEntityName(), e);
                return null;
            }
        }
        return textureConfig;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return this.entityData.get(HAS_BEEN_INTERACTED);
    }

    @Override
    public boolean removeWhenFarAway(double pDistanceToClosestPlayer) {
        return !this.entityData.get(HAS_BEEN_INTERACTED);
    }

    public void markAsInteracted() {
        this.entityData.set(HAS_BEEN_INTERACTED, true);
    }

    public boolean hasBeenInteracted() {
        return this.entityData.get(HAS_BEEN_INTERACTED);
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
        compound.putBoolean("PatrolMode", this.entityData.get(PATROL_MODE));

        BlockPos patrolPos = this.entityData.get(PATROL_POSITION);
        if (patrolPos != null && !patrolPos.equals(BlockPos.ZERO)) {
            compound.putLong("PatrolPosition", patrolPos.asLong());
        }
        compound.putString("FearedEntity", this.entityData.get(FEARED_ENTITY));

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

        this.writeInventoryToTag(compound, this.registryAccess());

        // Save spellbook spells
        if (!spellbookSpells.isEmpty()) {
            ListTag spellbookSpellsTag = new ListTag();
            for (AbstractSpell spell : spellbookSpells) {
                spellbookSpellsTag.add(StringTag.valueOf(spell.getSpellResource().toString()));
            }
            compound.put("SpellbookSpells", spellbookSpellsTag);
        }

        // Save last equipped spellbook
        if (!lastEquippedSpellbook.isEmpty()) {
            CompoundTag spellbookTag = new CompoundTag();
            lastEquippedSpellbook.save(this.registryAccess(), spellbookTag);
            compound.put("LastEquippedSpellbook", spellbookTag);
        }

        compound.putBoolean("HasBeenInteracted", this.entityData.get(HAS_BEEN_INTERACTED));

        compound.putBoolean("IsImmortal", this.entityData.get(IS_IMMORTAL));
        compound.putBoolean("IsStunned", this.entityData.get(IS_STUNNED));
        compound.putInt("StunTimer", this.entityData.get(STUN_TIMER));

        compound.putInt("EmeraldBalance", getEmeraldBalance());

        compound.putBoolean("IsSitting", isSittingInChair());
        compound.putLong("ChairPosition", getChairPosition().asLong());
        compound.putInt("SittingTime", getSittingTime());
        compound.putInt("SitCooldown", getSitCooldown());
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

        String fearedEntityId = compound.getString("FearedEntity");
        this.entityData.set(FEARED_ENTITY, fearedEntityId);
        initializeFearGoal();

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

        setPatrolMode(compound.getBoolean("PatrolMode"));

        if (compound.contains("PatrolPosition")) {
            BlockPos patrolPos = BlockPos.of(compound.getLong("PatrolPosition"));
            setPatrolPosition(patrolPos);
        }

        this.readInventoryFromTag(compound, this.registryAccess());

        // Load spellbook spells
        this.spellbookSpells.clear();
        if (compound.contains("SpellbookSpells")) {
            ListTag spellbookSpellsTag = compound.getList("SpellbookSpells", 8);
            for (int i = 0; i < spellbookSpellsTag.size(); i++) {
                try {
                    ResourceLocation spellLocation = ResourceLocation.parse(spellbookSpellsTag.getString(i));
                    AbstractSpell spell = SpellRegistry.getSpell(spellLocation);
                    if (spell != null) {
                        spellbookSpells.add(spell);
                    }
                } catch (Exception e) {
                    MagicRealms.LOGGER.warn("Failed to parse spellbook spell from NBT: {}", spellbookSpellsTag.getString(i), e);
                }
            }
        }

        // Load last equipped spellbook
        if (compound.contains("LastEquippedSpellbook")) {
            CompoundTag spellbookTag = compound.getCompound("LastEquippedSpellbook");
            this.lastEquippedSpellbook = ItemStack.parseOptional(this.registryAccess(), spellbookTag);
        } else {
            this.lastEquippedSpellbook = ItemStack.EMPTY;
        }

        this.entityData.set(HAS_BEEN_INTERACTED, compound.getBoolean("HasBeenInteracted"));

        this.entityData.set(IS_IMMORTAL, compound.getBoolean("IsImmortal"));
        this.entityData.set(IS_STUNNED, compound.getBoolean("IsStunned"));
        this.entityData.set(STUN_TIMER, compound.getInt("StunTimer"));

        setEmeraldBalance(compound.getInt("EmeraldBalance"));

        this.entityData.set(IS_SITTING, compound.getBoolean("IsSitting"));
        this.entityData.set(CHAIR_POSITION, BlockPos.of(compound.getLong("ChairPosition")));
        this.entityData.set(SITTING_TIME, compound.getInt("SittingTime"));
        this.entityData.set(SIT_COOLDOWN, compound.getInt("SitCooldown"));

        // Validate chair position on load
        if (isSittingInChair() && !isValidChair(getChairPosition())) {
            unsitFromChair();
        }
    }

    private boolean isAlliedHelper(Entity entity) {
        var owner = getSummoner();
        if (owner == null) {
            return false;
        }
        if (entity instanceof IMagicSummon magicSummon) {
            var otherOwner = magicSummon.getSummoner();
            return otherOwner != null && (owner == otherOwner || otherOwner.isAlliedTo(otherOwner));
        } else if (entity instanceof OwnableEntity tamableAnimal) {
            var otherOwner = tamableAnimal.getOwner();
            return otherOwner != null && (owner == otherOwner || otherOwner.isAlliedTo(otherOwner));
        }
        return false;
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
    public void onRemovedFromLevel() {
        super.onRemovedFromLevel();
    }

    @Override
    public void onAddedToLevel(){
        if (this.level().isClientSide()) {
            MRUtils.syncPresetTextureName(this);
        }
        super.onAddedToLevel();
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (isStunned()) {
            return true;
        }

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
    public boolean canBeAffected(MobEffectInstance pPotioneffect) {
        if (isStunned()) {
            return false;
        }
        return super.canBeAffected(pPotioneffect);
    }

    @Override
    public boolean isPickable() {
        return !isStunned() && super.isPickable();
    }

    @Override
    public boolean canBeSeenAsEnemy() {
        if (isStunned()) {
            return false; // Can't be seen as enemy while stunned
        }
        return super.canBeSeenAsEnemy();
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (isStunned()) {
            return false;
        }

        if (isSittingInChair() && pAmount > 0) {
            unsitFromChair();
        }

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

    public boolean regenerateTexture() {
        if (this.level().isClientSide()) {
            return false;
        }

        try {
            String entityUUID = this.getUUID().toString();
            Gender gender = this.getGender();
            EntityClass entityClass = this.getEntityClass();

            MagicRealms.LOGGER.debug("Starting texture regeneration for entity {} (Gender: {}, Class: {})",
                    this.getEntityName(), gender.getName(), entityClass.getName());

            // Remove old texture from cache and delete file
            CombinedTextureManager.removeEntityTexture(entityUUID, true); // true = delete file

            // Generate new hair texture index
            int newHairTextureIndex = LayeredTextureManager.getRandomHairTextureIndex("hair_" + gender.getName());

            // Force regeneration by clearing cache and letting the system recreate
            this.textureConfig = null;

            MagicRealms.LOGGER.debug("Successfully prepared texture regeneration for entity {} with hair index: {}",
                    this.getEntityName(), newHairTextureIndex);

            return true;

        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to regenerate texture for entity {}", this.getEntityName(), e);
            return false;
        }
    }

    public void forceTextureRegenerationWithName() {
        if (!this.level().isClientSide()) {
            if (this.regenerateTextureAndName()) {
                // Send packet to client to regenerate texture
                this.level().broadcastEntityEvent(this, (byte) 60); // Custom event ID for texture update
                MagicRealms.LOGGER.debug("Broadcasted texture regeneration event for entity {}", this.getEntityName());
            }
        }
    }

    public boolean regenerateTextureAndName() {
        if (!this.level().isClientSide()) {
            return false;
        }

        try {
            String entityUUID = this.getUUID().toString();
            Gender gender = this.getGender();
            EntityClass entityClass = this.getEntityClass();

            MagicRealms.LOGGER.debug("Starting texture and name regeneration for entity {} (Gender: {}, Class: {})",
                    this.getEntityName(), gender.getName(), entityClass.getName());

            // Remove old texture from cache and delete file
            CombinedTextureManager.removeEntityTexture(entityUUID, true); // true = delete file

            // Force regeneration by clearing cache and letting the system recreate
            this.textureConfig = null;

            // Create new texture config (this will generate new texture and potentially new name)
            this.textureConfig = new EntityTextureConfig(entityUUID, gender, entityClass);

            // Update name if we got a preset texture
            if (this.textureConfig != null && this.textureConfig.hasTextureName()) {
                String newTextureName = this.textureConfig.getTextureName();
                this.entityData.set(ENTITY_NAME, newTextureName);
                MagicRealms.LOGGER.info("Entity {} got new preset texture name: {}", entityUUID, newTextureName);
            } else {
                // Generate new random name for layered texture
                String newRandomName = AdvancedNameManager.getRandomName(gender);
                this.entityData.set(ENTITY_NAME, newRandomName);
                MagicRealms.LOGGER.info("Entity {} got new random name: {} (layered texture)", entityUUID, newRandomName);
            }

            this.updateCustomNameWithStars();

            MagicRealms.LOGGER.debug("Successfully completed texture and name regeneration for entity {}", this.getEntityName());
            return true;

        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to regenerate texture and name for entity {}", this.getEntityName(), e);
            return false;
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 60) { // Custom event ID for texture regeneration
            // Only handle on client side
            if (this.level().isClientSide() && this.entityData.get(INITIALIZED)) {
                try {
                    String entityUUID = this.getUUID().toString();
                    Gender gender = this.getGender();
                    EntityClass entityClass = this.getEntityClass();

                    MagicRealms.LOGGER.debug("Client-side texture regeneration started for entity {}", this.getEntityName());

                    // Remove old texture from client cache
                    CombinedTextureManager.removeEntityTexture(entityUUID, true); // true = delete file

                    // Clear current texture config to force regeneration
                    this.textureConfig = null;

                    // The texture will be regenerated automatically

                    MagicRealms.LOGGER.debug("Client-side texture regeneration completed for entity {}", this.getEntityName());

                } catch (Exception e) {
                    MagicRealms.LOGGER.error("Error during client-side texture regeneration for entity {}", this.getEntityName(), e);
                }
            }
        } else {
            super.handleEntityEvent(id);
        }
    }

    //GOALS

    private void setMageGoal(List<AbstractSpell> spells) {
        this.goalSelector.removeAllGoals((goal) -> goal instanceof HumanGoals.HumanWizardAttackGoal);

        // Combine persisted spells, spellbook spells, and equipment spells
        List<AbstractSpell> finalSpells = new ArrayList<>(spells);

        if (this.getEntityClass() == EntityClass.MAGE && !spellbookSpells.isEmpty()) {
            finalSpells.addAll(spellbookSpells);
        }

        // Add equipment spells for all classes
        List<AbstractSpell> equipmentSpells = extractSpellsFromEquipment();
        finalSpells.addAll(equipmentSpells);

        // Remove duplicates and blacklisted spells
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

        if(!finalSpells.isEmpty()){
            attackSpells.addAll(finalSpells);
        }

        this.goalSelector.addGoal(2, new HumanGoals.HumanWizardAttackGoal(this, 1.25f, 25, 50)
                .setSpells(attackSpells, defenseSpells, movementSpells, supportSpells)
                .setDrinksPotions()
        );
    }

    private void setArcherGoal(List<AbstractSpell> spells) {
        this.goalSelector.removeAllGoals((goal) -> goal instanceof HumanGoals.HumanWizardAttackGoal);
        this.goalSelector.removeAllGoals((goal) -> goal instanceof ChargeArrowAttackGoal);

        this.goalSelector.addGoal(2, new ChargeArrowAttackGoal<RandomHumanEntity>(this, 1.0D, 20, 15.0F));

        // Add equipment spells
        List<AbstractSpell> finalSpells = new ArrayList<>(spells);
        List<AbstractSpell> equipmentSpells = extractSpellsFromEquipment();
        finalSpells.addAll(equipmentSpells);

        // Remove duplicates and blacklisted spells
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

        if(!finalSpells.isEmpty()){
            attackSpells.addAll(finalSpells);
        }

        this.goalSelector.addGoal(3, new HumanGoals.HumanWizardAttackGoal(this, 1.0f, 60, 120)
                .setSpells(attackSpells, defenseSpells, movementSpells, supportSpells)
                .setDrinksPotions()
        );
    }

    private void setAssassinGoal(List<AbstractSpell> spells) {
        this.goalSelector.removeAllGoals((goal) -> goal instanceof GenericAnimatedWarlockAttackGoal);

        // Add equipment spells
        List<AbstractSpell> finalSpells = new ArrayList<>(spells);
        List<AbstractSpell> equipmentSpells = extractSpellsFromEquipment();
        finalSpells.addAll(equipmentSpells);

        // Remove duplicates and blacklisted spells
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

        if(!finalSpells.isEmpty()){
            attackSpells.addAll(finalSpells);
        }

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
                .setSpells(attackSpells, defenseSpells, movementSpells, supportSpells)
                .setDrinksPotions()
        );
    }

    private void setWarriorGoal(List<AbstractSpell> spells) {
        this.goalSelector.removeAllGoals((goal) -> goal instanceof GenericAnimatedWarlockAttackGoal);

        // Add equipment spells
        List<AbstractSpell> finalSpells = new ArrayList<>(spells);
        List<AbstractSpell> equipmentSpells = extractSpellsFromEquipment();
        finalSpells.addAll(equipmentSpells);

        // Remove duplicates and blacklisted spells
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

        if(!finalSpells.isEmpty()){
            attackSpells.addAll(finalSpells);
        }

        if (hasShield()) {
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
                    .setSpells(attackSpells, defenseSpells, movementSpells, supportSpells)
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
                    .setSpells(attackSpells, defenseSpells, movementSpells, supportSpells)
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
        } else if (this.entityData.get(INITIALIZED)) {
            MagicRealms.LOGGER.warn("Entity {} was initialized but has no spells, regenerating...", getEntityName());
            generateAndApplySpells();
        }
    }

    private void clearAttackGoals() {
        this.goalSelector.removeAllGoals(goal ->
                goal instanceof HumanGoals.HumanWizardAttackGoal ||
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
        // Check if we have arrows before attacking
        if (!hasArrows()) {
            return;
        }

        AbstractArrow abstractarrow = this.getArrow();
        if (abstractarrow == null) {
            return;
        }

        // Consume an arrow from inventory
        consumeArrow();

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
        // Only create arrow if we have arrows in inventory
        if (!hasArrows()) {
            return null;
        }

        // Find the best arrow type in inventory
        ItemStack arrowStack = getBestArrowFromInventory();
        if (arrowStack.isEmpty()) {
            return new Arrow(this.level(), this, arrowStack.copyWithCount(1), this.getMainHandItem());
        }

        // Create arrow based on item type
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
        ItemStack bestArrow = ItemStack.EMPTY;

        // First pass: look for special arrows (not basic arrows)
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (isArrowItem(stack) && !stack.is(Items.ARROW) && !stack.isEmpty()) {
                return stack;
            }
        }

        // Second pass: look for basic arrows
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(Items.ARROW) && !stack.isEmpty()) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    private void consumeArrow() {
        // First try to consume special arrows
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

        // Then consume basic arrows
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
    public boolean isAlliedTo(Entity pEntity) {
        return super.isAlliedTo(pEntity) || this.isAlliedHelper(pEntity);
    }
}
