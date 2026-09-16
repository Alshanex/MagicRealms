package net.alshanex.magic_realms.entity.humans;

import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.network.SyncPresetNamePacket;
import net.alshanex.magic_realms.registry.MREntityRegistry;
import net.alshanex.magic_realms.Config;
import net.alshanex.magic_realms.util.humans.combat.CombatClass;
import net.alshanex.magic_realms.util.humans.combat.CombatClasses;
import net.alshanex.magic_realms.util.humans.mercenaries.chat.ChatFaceCompositeCache;
import net.alshanex.magic_realms.util.humans.mercenaries.skins_management.*;
import net.alshanex.magic_realms.util.humans.mercenaries.AdvancedNameManager;
import net.alshanex.magic_realms.util.humans.mercenaries.Gender;
import net.alshanex.magic_realms.util.humans.mercenaries.chat.IChatFaceProvider;
import net.alshanex.magic_realms.util.humans.mercenaries.personality_management.FixedPersonalityCatalogHolder;
import net.alshanex.magic_realms.util.humans.mercenaries.personality_management.FixedPersonalityDef;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

public class RandomHumanEntity extends AbstractMercenaryEntity implements IChatFaceProvider {

    private static final EntityDataAccessor<CompoundTag> TEXTURE_METADATA =
            SynchedEntityData.defineId(RandomHumanEntity.class, EntityDataSerializers.COMPOUND_TAG);

    private TextureComponents textureComponents;
    private boolean appearanceGenerated = false;
    private boolean clientTexturesGenerated = false;

    public RandomHumanEntity(EntityType<? extends AbstractSpellCastingMob> entityType, Level level) {
        super(entityType, level);
    }

    public RandomHumanEntity(Level level, LivingEntity owner) {
        this(MREntityRegistry.HUMAN.get(), level);
        setSummoner(owner);
    }

    @Override
    protected void initializeAppearance(RandomSource randomSource) {
        if (appearanceGenerated) return;

        RandomSource deterministicRandom = getDeterministicRandom();

        // Hooks let subclasses pin any of these. Subclasses see the chosen gender/class so e.g. a
        // pinned-gender bandit gets a name (and texture roll) consistent with that gender.
        Gender gender = chooseGender(deterministicRandom);
        CombatClass combatClass = chooseCombatClass(gender, deterministicRandom);
        String name = chooseName(gender, deterministicRandom);

        setGender(gender);
        setCombatClass(combatClass);
        setEntityName(name);

        // SERVER SIDE: Generate deterministic texture metadata using the (now possibly pinned) gender.
        CompoundTag textureMetadata = generateTextureMetadata(gender, combatClass, deterministicRandom);
        setTextureMetadata(textureMetadata);

        this.appearanceGenerated = true;
    }

    /**
     * Picks a gender for this entity. Default behavior is uniform random; subclasses override to consult a profile and pin the value.
     */
    protected Gender chooseGender(RandomSource rng) {
        return Gender.values()[rng.nextInt(Gender.values().length)];
    }

    /**
     * Picks an entity class.
     */
    protected CombatClass chooseCombatClass(Gender gender, RandomSource rng) {
        return CombatClasses.pickRandom(rng);
    }

    /**
     * Picks a name for this entity. Receives the (already-chosen) gender so the default can pull from the gender-appropriate name pool.
     */
    protected String chooseName(Gender gender, RandomSource rng) {
        return AdvancedNameManager.getRandomName(gender, rng);
    }

    private CompoundTag generateTextureMetadata(Gender gender, CombatClass combatClass, RandomSource random) {
        CompoundTag metadata = new CompoundTag();
        metadata.putString("gender", gender.getName());
        metadata.putString("entityClass", combatClass.skinCategory());

        SkinCatalog catalog = SkinCatalogHolder.server();
        boolean wantsPreset = random.nextDouble() < Config.customTextureChance;
        boolean usePreset = wantsPreset && catalog.hasPresets(gender);
        metadata.putBoolean("usePreset", usePreset);

        if (usePreset) {
            SkinPreset preset = catalog.pickPreset(gender, random);
            metadata.putString("presetTexture", preset.texture().toString());

            // If the preset specifies a fixed personality, write its id so PersonalityInitializer can apply it on the server. We also
            // copy the personality's override_entity_name into "presetName" ONLY when the preset itself didn't specify a display_name -
            // that way the existing client-side name-sync flow propagates the chosen name without any new packet plumbing.
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

            // The preset's own display_name still wins if both are present.
            preset.displayName().ifPresent(n -> metadata.putString("presetName", n));
        } else {
            SkinPart skin = catalog.pickPart(SkinCategory.SKIN, gender, combatClass, random);
            SkinPart clothes = catalog.pickPart(SkinCategory.CLOTHES, gender, combatClass, random);
            SkinPart eyes = catalog.pickPart(SkinCategory.EYES, gender, combatClass, random);
            SkinPart hair = catalog.pickPart(SkinCategory.HAIR, gender, combatClass, random);

            if (skin != null) metadata.putString("skinTexture", skin.texture().toString());
            if (clothes != null) metadata.putString("clothesTexture", clothes.texture().toString());
            if (eyes != null) metadata.putString("eyesTexture", eyes.texture().toString());
            if (hair != null) metadata.putString("hairTexture", hair.texture().toString());

            if (skin == null || clothes == null || eyes == null || hair == null) {
                MagicRealms.LOGGER.warn("Skin catalog missing parts for {} {} — entity will render partially defaulted",
                        gender.getName(), combatClass.id());
            }
        }
        return metadata;
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();

        if (this.level().isClientSide() && isInitialized() && !clientTexturesGenerated) {
            // Schedule texture generation for next tick to ensure textures are loaded
            Minecraft.getInstance().execute(this::generateTexturesFromMetadata);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void generateTexturesFromMetadata() {
        if (!this.level().isClientSide()) return;

        CompoundTag metadata = getTextureMetadata();
        if (metadata.isEmpty()) {
            MagicRealms.LOGGER.warn("No texture metadata found for entity {}", getEntityName());
            return;
        }

        try {
            if (metadata.getBoolean("usePreset")) {
                String tex = metadata.getString("presetTexture");
                String name = metadata.contains("presetName") ? metadata.getString("presetName") : null;
                this.textureComponents = new TextureComponents(tex, null, null, null, name, true);

                if (name != null && !name.equals(getEntityName())) {
                    setEntityName(name);
                    syncPresetNameToServer(name);
                }
            } else {
                this.textureComponents = new TextureComponents(
                        metadata.contains("skinTexture") ? metadata.getString("skinTexture") : null,
                        metadata.contains("clothesTexture") ? metadata.getString("clothesTexture") : null,
                        metadata.contains("eyesTexture") ? metadata.getString("eyesTexture") : null,
                        metadata.contains("hairTexture") ? metadata.getString("hairTexture") : null,
                        null, false);
            }
            clientTexturesGenerated = true;
        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to generate textures from metadata for {}", getEntityName(), e);
            this.textureComponents = new TextureComponents(null, null, null, null, null, false);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void syncPresetNameToServer(String presetName) {
        try {
            PacketDistributor.sendToServer(new SyncPresetNamePacket(this.getUUID(), presetName));
        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to sync preset name to server", e);
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (key.equals(TEXTURE_METADATA) && this.level().isClientSide()) {
            onTextureMetadataUpdatedClient();
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void onTextureMetadataUpdatedClient() {
        if (this.textureComponents != null && !this.textureComponents.isPresetTexture()) {
            ChatFaceCompositeCache.clearFor(this.textureComponents);
        }
        this.clientTexturesGenerated = false;
        Minecraft.getInstance().execute(this::generateTexturesFromMetadata);
    }

    // Getters/setters for texture metadata
    public CompoundTag getTextureMetadata() {
        return entityData.get(TEXTURE_METADATA);
    }

    public void setTextureMetadata(CompoundTag metadata) {
        entityData.set(TEXTURE_METADATA, metadata);
    }

    public TextureComponents getTextureComponents() {
        return textureComponents;
    }

    @Override
    protected void handlePostSpawnInitialization() {
        // Just ensure appearance is initialized
        if (!isInitialized()) {
            RandomSource randomSource = this.level().getRandom();
            initializeStarLevel(randomSource);
            initializeAppearance(randomSource);
            setInitialized(true);
        }

        if (!this.level().isClientSide) {
            // Schedule the name update to happen after all initialization is complete
            this.level().getServer().execute(() -> {
                if (this.isAlive() && !this.isRemoved()) {
                    this.refreshDisplayName();
                }
            });
        }
    }

    @Override
    public boolean isExclusiveMercenary() {
        return false;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public ResourceLocation getChatFaceTextureCS() {
        if (!this.level().isClientSide()) return null;
        TextureComponents tc = getTextureComponents();
        if (tc == null) return null;

        // Preset textures: use the preset PNG directly with default 64x64 UV slices.
        if (tc.isPresetTexture()) {
            String tex = tc.getSkinTexture();
            if (tex == null) return null;
            try {
                ResourceLocation parsed = ResourceLocation.parse(tex);
                return ResourceLocation.fromNamespaceAndPath(
                        parsed.getNamespace(), "textures/" + parsed.getPath() + ".png");
            } catch (Exception e) {
                return null;
            }
        }

        return ChatFaceCompositeCache.getOrBuild(tc);
    }

    @Override
    public IChatFaceProvider.UVSlice getChatFaceUV() {
        TextureComponents tc = getTextureComponents();
        if (tc != null && !tc.isPresetTexture()) {
            // Mini-atlas layout: face at (0,0) 8x8 within 16x8 atlas
            return new IChatFaceProvider.UVSlice(0, 0, 8, 8, 16, 8);
        }
        return IChatFaceProvider.DEFAULT_FACE_SLICE;
    }

    @Override
    public IChatFaceProvider.UVSlice getChatFaceHatUV() {
        TextureComponents tc = getTextureComponents();
        if (tc != null && !tc.isPresetTexture()) {
            // Mini-atlas layout: hat at (8,0) 8x8 within 16x8 atlas
            return new IChatFaceProvider.UVSlice(8, 0, 8, 8, 16, 8);
        }
        return IChatFaceProvider.DEFAULT_HAT_SLICE;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        super.defineSynchedData(pBuilder);
        pBuilder.define(TEXTURE_METADATA, new CompoundTag());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("AppearanceGenerated", this.appearanceGenerated);

        // Save texture metadata (not full texture components)
        CompoundTag metadata = getTextureMetadata();
        if (!metadata.isEmpty()) {
            compound.put("TextureMetadata", metadata);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.appearanceGenerated = compound.getBoolean("AppearanceGenerated");
        this.clientTexturesGenerated = false;

        if (compound.contains("TextureMetadata")) {
            CompoundTag metadata = compound.getCompound("TextureMetadata");

            boolean isLegacy = !metadata.contains("presetTexture")
                    && !metadata.contains("skinTexture")
                    && (metadata.contains("skinIndex") || metadata.contains("presetIndex"));

            if (isLegacy && !this.level().isClientSide()) {
                SkinCatalog catalog = SkinCatalogHolder.server();
                if (catalog.allParts().isEmpty() && catalog.allPresets().isEmpty()) {
                    MagicRealms.LOGGER.warn("Skipping texture migration for {} — catalog empty, will retry next load", getUUID());
                    // leave old metadata in place; it'll try again next time
                } else {
                    Gender gender = Gender.valueOf(metadata.getString("gender").toUpperCase());
                    CombatClass cls = CombatClasses.resolve(metadata.getString("entityClass"));
                    if (cls == null) cls = CombatClasses.getOrFallback(null);
                    RandomSource migrationRng = RandomSource.create(this.getUUID().getMostSignificantBits() ^ this.getUUID().getLeastSignificantBits());
                    metadata = generateTextureMetadata(gender, cls, migrationRng);
                }
            }
            setTextureMetadata(metadata);
        }
    }
}