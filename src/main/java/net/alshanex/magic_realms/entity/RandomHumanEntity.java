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
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.ContractData;
import net.alshanex.magic_realms.data.KillTrackerData;
import net.alshanex.magic_realms.data.VillagerOffersData;
import net.alshanex.magic_realms.events.MagicAttributeGainsHandler;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.registry.MREntityRegistry;
import net.alshanex.magic_realms.util.MRUtils;
import net.alshanex.magic_realms.util.ModTags;
import net.alshanex.magic_realms.util.humans.ChargeArrowAttackGoal;
import net.alshanex.magic_realms.util.humans.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
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
import net.minecraft.world.SimpleContainer;
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
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
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
    private static final float REGEN_AMOUNT = 1.0f;

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

        this.goalSelector.addGoal(2, new HumanGoals.GatherResourcesGoal(this));

        this.goalSelector.addGoal(6, new HumanGoals.SellItemsToVillagersGoal(this));
        this.goalSelector.addGoal(5, new HumanGoals.BuyEquipmentFromVillagersGoal(this));
        this.goalSelector.addGoal(7, new HumanGoals.EnchantEquipmentFromLibrarianGoal(this));
        this.goalSelector.addGoal(8, new HumanGoals.PickupMobDropsGoal(this));

        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new WizardRecoverGoal(this));

        this.targetSelector.addGoal(1, new GenericOwnerHurtByTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(2, new GenericOwnerHurtTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(3, new GenericCopyOwnerTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(4, (new GenericHurtByTargetGoal(this, (entity) -> entity == getSummoner())).setAlertOthers());
        this.targetSelector.addGoal(5, new GenericProtectOwnerTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(5, new HumanGoals.HumanHurtByTargetGoal(this));
        this.targetSelector.addGoal(6, new HumanGoals.AlliedHumanDefenseGoal(this));

        this.targetSelector.addGoal(1, new HumanGoals.NoFearTargetGoal(this));

        this.goalSelector.addGoal(4, new HumanGoals.HumanFollowOwnerGoal(this, this::getSummoner, 1.3f, 15, 5, false, 25));

        this.goalSelector.addGoal(7, new HumanGoals.PatrolAroundPositionGoal(this, 0.8D, 10));
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

        HumanStatsManager.applyClassAttributes(this);
        giveStartingEmeralds();

        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData);
    }

    public void giveStartingEmeralds() {
        int emeraldCount = switch (getStarLevel()) {
            case 1 -> 5 + getRandom().nextInt(6); // 5-10 emeralds
            case 2 -> 10 + getRandom().nextInt(11); // 10-20 emeralds
            case 3 -> 20 + getRandom().nextInt(16); // 20-35 emeralds
            default -> 5;
        };

        ItemStack emeralds = new ItemStack(Items.EMERALD, emeraldCount);
        getInventory().addItem(emeralds);
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
        InventoryCarrier.pickUpItem(this, this, itemEntity);
        MRUtils.autoEquipBetterEquipment(this);

        if (pickedUpItem.is(Items.EMERALD)) {
            recheckUnaffordableOffers();
        }
    }

    @Override
    public boolean wantsToPickUp(ItemStack stack) {
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
            List<MerchantOffer> nowAffordable = memory.checkAffordableOffers(villagerUUID, this.getInventory());

            if (!nowAffordable.isEmpty()) {
                MagicRealms.LOGGER.debug("Entity {} can now afford {} offers from villager {}",
                        this.getEntityName(), nowAffordable.size(), villagerUUID);
            }
        }
    }

    private void handleNaturalRegeneration() {
        KillTrackerData killData = this.getData(MRDataAttachments.KILL_TRACKER);

        // Only regenerate if entity has unlocked natural regen and is not at full health
        if (!killData.hasNaturalRegen() || this.getHealth() >= this.getMaxHealth()) {
            return;
        }

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

    public List<AbstractSpell> getAllAvailableSpellsWithEquipment() {
        List<AbstractSpell> allSpells = new ArrayList<>(persistedSpells);

        // Add spellbook spells (for mages)
        if (this.getEntityClass() == EntityClass.MAGE) {
            allSpells.addAll(spellbookSpells);
        }

        // Add equipment spells
        List<AbstractSpell> equipmentSpells = extractSpellsFromEquipment();
        allSpells.addAll(equipmentSpells);

        // Remove duplicates and blacklisted spells
        return allSpells.stream()
                .distinct()
                .filter(spell -> !ModTags.isSpellInTag(spell, ModTags.SPELL_BLACKLIST))
                .collect(Collectors.toList());
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

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide && !goalsInitialized && this.entityData.get(INITIALIZED)) {
            reinitializeGoalsAfterLoad();
            goalsInitialized = true;
        }

        if (!level().isClientSide) {
            handleNaturalRegeneration();
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

                    if (this.tickCount % 200 == 0) {
                        contractData.periodicTimeUpdate();
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
        pBuilder.define(PATROL_MODE, false);
        pBuilder.define(PATROL_POSITION, BlockPos.ZERO);
        pBuilder.define(FEARED_ENTITY, "");
        pBuilder.define(HAS_BEEN_INTERACTED, false);
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
        KillTrackerData data = this.getData(MRDataAttachments.KILL_TRACKER);

        if (!entityName.isEmpty()) {

            Component nameComponent = Component.literal(entityName + " Lv. " + data.getCurrentLevel());
            this.setCustomName(nameComponent);
            this.setCustomNameVisible(true);
        }
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

    private void updateMageGoalWithSpellbook(List<AbstractSpell> spellbookSpells) {
        // Combine persisted spells with spellbook spells
        List<AbstractSpell> combinedSpells = new ArrayList<>(persistedSpells);
        combinedSpells.addAll(spellbookSpells);

        // Remove duplicate spells (keep the first occurrence)
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

            MagicRealms.LOGGER.debug("Mage {} updated spell roster: {} persisted + {} spellbook = {} total unique",
                    getEntityName(), persistedSpells.size(), spellbookSpells.size(), uniqueSpells.size());
        } else {
            // Fallback to just persisted spells
            setMageGoal(persistedSpells);
        }
    }

    public List<AbstractSpell> getAllAvailableSpells() {
        List<AbstractSpell> allSpells = new ArrayList<>(persistedSpells);
        allSpells.addAll(spellbookSpells);

        // Remove duplicates
        return allSpells.stream().distinct().collect(Collectors.toList());
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
        if (!level().isClientSide) {
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
