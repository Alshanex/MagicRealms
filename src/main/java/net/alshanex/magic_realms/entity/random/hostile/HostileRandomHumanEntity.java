package net.alshanex.magic_realms.entity.random.hostile;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.entity.mobs.goals.WizardRecoverGoal;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.ContractData;
import net.alshanex.magic_realms.data.KillTrackerData;
import net.alshanex.magic_realms.entity.random.RandomHumanEntity;
import net.alshanex.magic_realms.item.PermanentContractItem;
import net.alshanex.magic_realms.item.TieredContractItem;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.registry.MREntityRegistry;
import net.alshanex.magic_realms.registry.MRItems;
import net.alshanex.magic_realms.util.humans.bandits.BanditProfile;
import net.alshanex.magic_realms.util.humans.bandits.BanditProfileApplier;
import net.alshanex.magic_realms.util.humans.bandits.BanditProfileCatalogHolder;
import net.alshanex.magic_realms.util.humans.goals.HumanGoals;
import net.alshanex.magic_realms.util.humans.mercenaries.AdvancedNameManager;
import net.alshanex.magic_realms.util.humans.mercenaries.EntityClass;
import net.alshanex.magic_realms.util.humans.mercenaries.Gender;
import net.alshanex.magic_realms.util.humans.mercenaries.chat.MercenarySpeechHelper;
import net.alshanex.magic_realms.util.humans.mercenaries.personality_management.FixedPersonalityCatalogHolder;
import net.alshanex.magic_realms.util.humans.mercenaries.personality_management.FixedPersonalityDef;
import net.alshanex.magic_realms.util.humans.mercenaries.personality_management.PersonalityInitializer;
import net.alshanex.magic_realms.util.humans.mercenaries.skins_management.SkinCatalogHolder;
import net.alshanex.magic_realms.util.humans.mercenaries.skins_management.SkinPreset;
import net.alshanex.magic_realms.util.humans.stats.LevelingStatsManager;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HostileRandomHumanEntity extends RandomHumanEntity {

    private static final EntityDataAccessor<String> PROFILE_ID =
            SynchedEntityData.defineId(HostileRandomHumanEntity.class, EntityDataSerializers.STRING);

    @Nullable
    private String pendingProfileId;

    public HostileRandomHumanEntity(EntityType<? extends RandomHumanEntity> entityType, Level level) {
        super(entityType, level);
    }

    public HostileRandomHumanEntity(Level level, int wave) {
        this(MREntityRegistry.HOSTILE_HUMAN.get(), level);
    }

    public HostileRandomHumanEntity(Level level, String profileId) {
        this(MREntityRegistry.HOSTILE_HUMAN.get(), level);
        this.pendingProfileId = profileId;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        super.defineSynchedData(pBuilder);
        pBuilder.define(PROFILE_ID, "");
    }

    public String getProfileId() {
        return this.entityData.get(PROFILE_ID);
    }

    public void setProfileId(String profileId) {
        this.entityData.set(PROFILE_ID, profileId == null ? "" : profileId);
    }

    public boolean hasProfile() {
        String id = getProfileId();
        return id != null && !id.isEmpty();
    }

    @Nullable
    public BanditProfile getProfile() {
        if (!hasProfile()) return null;
        return BanditProfileApplier.resolve(getProfileId(), this.level().isClientSide());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new WizardRecoverGoal(this));
        this.goalSelector.addGoal(4, new HumanGoals.PickupMobDropsGoal(this));
        this.goalSelector.addGoal(10, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 8.0F));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, true, false));
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide()) return InteractionResult.FAIL;
        if (player.level().isClientSide()) return InteractionResult.FAIL;

        ItemStack heldItem = player.getItemInHand(hand);

        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.FAIL;
        }
        if (heldItem.is(Items.EMERALD)) {
            return InteractionResult.FAIL;
        }
        if (heldItem.is(MRItems.HELL_PASS.get())) {
            return InteractionResult.FAIL;
        }
        if (heldItem.is(MRItems.SKIN_CUSTOMIZER.get())) {
            return InteractionResult.FAIL;
        }
        if (heldItem.getItem() instanceof TieredContractItem || heldItem.getItem() instanceof PermanentContractItem) {
            return InteractionResult.FAIL;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            MercenarySpeechHelper.trySpeak(this, serverPlayer);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    @Override
    public void handleContractInteraction(Player player, ContractData contractData, ItemStack heldItem) {
        // Bandits cannot be contracted.
    }

    // Profile-aware appearance hooks

    @Override
    protected Gender chooseGender(RandomSource rng) {
        BanditProfile profile = resolveProfile();
        if (profile != null) {
            Gender g = profile.gender().orElse(null);
            if (g != null) return g;
        }
        return super.chooseGender(rng);
    }

    @Override
    protected EntityClass chooseEntityClass(Gender gender, RandomSource rng) {
        BanditProfile profile = resolveProfile();
        if (profile != null) {
            EntityClass c = profile.entityClass().orElse(null);
            if (c != null) return c;
        }
        return super.chooseEntityClass(gender, rng);
    }

    @Override
    protected String chooseName(Gender gender, RandomSource rng) {
        BanditProfile profile = resolveProfile();
        if (profile != null) {
            String n = profile.overrideName().orElse(null);
            if (n != null && !n.isEmpty()) return n;
        }
        return super.chooseName(gender, rng);
    }

    // Other profile-aware initialization overrides

    @Override
    protected void initializeAppearance(RandomSource randomSource) {
        super.initializeAppearance(randomSource);

        // Consume the pending profile id (set via constructor or NBT) and fold it into the synced
        // PROFILE_ID. The hooks above already pinned gender/class/name; this step applies the
        // skin preset (if any), which has to happen AFTER the random texture metadata generation
        // so we replace it cleanly.
        BanditProfile profile = resolvePendingProfile();
        if (profile != null) {
            applyPresetTextureOverride(profile);
        }
    }

    @Override
    protected void initializeClassSpecifics(RandomSource randomSource) {
        BanditProfile profile = resolveProfile();
        super.initializeClassSpecifics(randomSource);
        if (profile != null) {
            BanditProfileApplier.applyClassSpecifics(this, profile, randomSource);
        }
    }

    @Override
    protected void initializeDefaultEquipment() {
        super.initializeDefaultEquipment();
        BanditProfile profile = resolveProfile();
        if (profile != null) {
            BanditProfileApplier.applyEquipment(this, profile);
        }
    }

    @Override
    protected void initializeHumanLevel(RandomSource randomSource, KillTrackerData killTrackerData) {
        BanditProfile profile = resolveProfile();
        if (profile != null) {
            boolean handled = BanditProfileApplier.applyLevelRoll(this, profile, killTrackerData, randomSource);
            if (handled) {
                LevelingStatsManager.applyLevelBasedAttributes(this, killTrackerData.getCurrentLevel());
                return;
            }
        }
        super.initializeHumanLevel(randomSource, killTrackerData);
    }

    @Override
    protected List<AbstractSpell> generateSpellsForEntity(RandomSource randomSource) {
        BanditProfile profile = resolveProfile();
        if (profile != null) {
            List<AbstractSpell> override = BanditProfileApplier.resolveSpells(this, profile, randomSource);
            if (override != null) return override;
        }
        return super.generateSpellsForEntity(randomSource);
    }

    @Override
    protected void initializePersonality(RandomSource randomSource) {
        BanditProfile profile = resolveProfile();
        if (profile != null && profile.fixedPersonalityId().isPresent()) {
            String fpId = profile.fixedPersonalityId().get();
            FixedPersonalityDef def = FixedPersonalityCatalogHolder.server().byId(fpId);
            if (def != null) {
                PersonalityInitializer.FixedPersonality fixed = def.toRuntime();
                this.getData(MRDataAttachments.PERSONALITY).initialize(
                        fixed.archetypeId(), fixed.hobbyId(), fixed.hometown(), fixed.quirks());
                if (def.overrideEntityName().isPresent() && !def.overrideEntityName().get().isEmpty()) {
                    this.setEntityName(def.overrideEntityName().get());
                }
                return;
            } else {
                MagicRealms.LOGGER.warn("Bandit profile {} references unknown fixed_personality_id {}",
                        profile.id(), fpId);
            }
        }
        super.initializePersonality(randomSource);
    }

    @Override
    protected void handlePostSpawnInitialization() {
        super.handlePostSpawnInitialization();
        BanditProfile profile = resolveProfile();
        if (profile != null) {
            BanditProfileApplier.applyPostInit(this, profile);
            this.updateCustomNameWithStars();
        }
    }

    @Override
    protected void initializeFearedEntity(RandomSource randomSource) {
        // Bandits don't get a feared entity by default.
    }

    @Override
    protected @NotNull ResourceKey<LootTable> getDefaultLootTable() {
        BanditProfile profile = resolveProfile();
        if (profile != null) {
            ResourceKey<LootTable> key = profile.lootTableKey().orElse(null);
            if (key != null) return key;
        }
        return super.getDefaultLootTable();
    }

    @Override
    protected boolean isHostileHuman() {
        return true;
    }

    @Override
    public LivingEntity getSummoner() {
        return null;
    }

    @Override
    public void setSummoner(@Nullable LivingEntity owner) {
        // Bandits have no summoner.
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        String profileId = getProfileId();
        if (profileId != null && !profileId.isEmpty()) {
            compound.putString("BanditProfileId", profileId);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("BanditProfileId")) {
            String saved = compound.getString("BanditProfileId");
            if (!saved.isEmpty()) {
                setProfileId(saved);
                this.pendingProfileId = null;
                if (this.isInitialized() && !this.level().isClientSide()) {
                    BanditProfile profile = BanditProfileCatalogHolder.server().byId(saved);
                    if (profile != null) {
                        BanditProfileApplier.applyPostInit(this, profile);
                    }
                }
            }
        }
    }

    @Override
    public boolean isAlliedTo(Entity pEntity) {
        return super.isAlliedTo(pEntity) || this.isAlliedHelper(pEntity);
    }

    private boolean isAlliedHelper(Entity entity) {
        return entity instanceof HostileRandomHumanEntity;
    }

    // Profile resolution helpers

    /**
     * Folds the pending profile id (set via constructor or NBT) into the synced PROFILE_ID and returns
     * the resolved profile. Called once during {@code initializeAppearance}.
     */
    @Nullable
    private BanditProfile resolvePendingProfile() {
        if (this.level().isClientSide()) return null;
        if (pendingProfileId != null && !pendingProfileId.isEmpty()) {
            setProfileId(pendingProfileId);
            BanditProfile profile = BanditProfileCatalogHolder.server().byId(pendingProfileId);
            if (profile == null) {
                MagicRealms.LOGGER.warn("Bandit spawned with unknown profile id '{}'; falling back to random.",
                        pendingProfileId);
            }
            pendingProfileId = null;
            return profile;
        }
        if (hasProfile()) {
            return BanditProfileCatalogHolder.server().byId(getProfileId());
        }
        return null;
    }

    /**
     * Resolves the active profile without consuming the pending id. Safe to call multiple times.
     */
    @Nullable
    private BanditProfile resolveProfile() {
        if (this.level().isClientSide()) return null;
        if (hasProfile()) {
            return BanditProfileCatalogHolder.server().byId(getProfileId());
        }
        if (pendingProfileId != null && !pendingProfileId.isEmpty()) {
            return BanditProfileCatalogHolder.server().byId(pendingProfileId);
        }
        return null;
    }

    /**
     * If the profile specifies a {@code skin_preset} (the id of an entry in the skin_presets/ catalog,
     * e.g. {@code "magic_realms:vex"} for {@code data/.../skin_presets/vex.json}, or
     * {@code "magic_realms:male/leon"} for a file in a subdirectory), look up the matching SkinPreset
     * and apply its full metadata block: preset texture, optional display name, and optional
     * fixed-personality binding.
     *
     * <p>If no preset matches the given id, logs a warning and leaves the metadata as the layered
     * random skin already produced by {@code RandomHumanEntity.initializeAppearance}.
     */
    private void applyPresetTextureOverride(BanditProfile profile) {
        if (profile.skinPreset().isEmpty()) return;
        if (this.level().isClientSide()) return;

        ResourceLocation presetId = profile.skinPreset().get();
        SkinPreset preset = findPresetById(presetId);
        if (preset == null) {
            MagicRealms.LOGGER.warn("Bandit profile {} references unknown skin_preset '{}' (no preset with that id is loaded); falling back to random skin",
                    profile.id(), presetId);
            return;
        }

        CompoundTag metadata = this.getTextureMetadata().copy();
        metadata.putBoolean("usePreset", true);
        metadata.putString("presetTexture", preset.texture().toString());

        preset.fixedPersonalityId().ifPresent(fpId -> {
            metadata.putString("presetFixedPersonalityId", fpId);
            if (preset.displayName().isEmpty()) {
                FixedPersonalityDef def = FixedPersonalityCatalogHolder.server().byId(fpId);
                if (def != null) {
                    def.overrideEntityName().ifPresent(name -> {
                        if (!name.isEmpty()) metadata.putString("presetName", name);
                    });
                }
            }
        });
        preset.displayName().ifPresent(n -> metadata.putString("presetName", n));

        metadata.remove("skinTexture");
        metadata.remove("clothesTexture");
        metadata.remove("eyesTexture");
        metadata.remove("hairTexture");

        this.setTextureMetadata(metadata);
    }

    /**
     * Look up a skin preset by its id (the resource location of its JSON file). Requires SkinPreset
     * to have been migrated to use the id-based lookup (see SkinCatalog.presetById).
     */
    @Nullable
    private static SkinPreset findPresetById(ResourceLocation presetId) {
        return SkinCatalogHolder.server().presetById(presetId);
    }
}