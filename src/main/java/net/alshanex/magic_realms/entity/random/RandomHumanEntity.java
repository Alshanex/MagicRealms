package net.alshanex.magic_realms.entity.random;

import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.KillTrackerData;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.network.SyncPresetNamePacket;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.registry.MREntityRegistry;
import net.alshanex.magic_realms.util.humans.*;
import net.alshanex.magic_realms.Config;
import net.alshanex.magic_realms.util.humans.appearance.AdvancedNameManager;
import net.alshanex.magic_realms.util.humans.appearance.LayeredTextureManager;
import net.alshanex.magic_realms.util.humans.appearance.TextureComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

public class RandomHumanEntity extends AbstractMercenaryEntity {

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
        Gender gender = Gender.values()[deterministicRandom.nextInt(Gender.values().length)];
        EntityClass entityClass = EntityClass.values()[deterministicRandom.nextInt(EntityClass.values().length)];

        setGender(gender);
        setEntityClass(entityClass);
        setEntityName(AdvancedNameManager.getRandomName(gender, deterministicRandom));

        // SERVER SIDE: Generate deterministic texture metadata
        CompoundTag textureMetadata = generateTextureMetadata(gender, entityClass, deterministicRandom);
        setTextureMetadata(textureMetadata);

        this.appearanceGenerated = true;

        MagicRealms.LOGGER.debug("Initialized appearance for {} - Gender: {}, Class: {}, Server: {}",
                getEntityName(), gender.getName(), entityClass.getName(), !this.level().isClientSide());
    }

    private CompoundTag generateTextureMetadata(Gender gender, EntityClass entityClass, RandomSource random) {
        CompoundTag metadata = new CompoundTag();

        // Store the basic choices
        metadata.putString("gender", gender.getName());
        metadata.putString("entityClass", entityClass.getName());

        // Determine if it should be preset or layered
        double presetRoll = random.nextDouble();
        boolean usePreset = presetRoll < Config.customTextureChance;
        metadata.putBoolean("usePreset", usePreset);

        if (usePreset) {
            // Store preset index - will be modulo'd by actual count on client
            metadata.putInt("presetIndex", random.nextInt(10000));
            MagicRealms.LOGGER.debug("Generated preset metadata for {} - index: {}",
                    getEntityName(), metadata.getInt("presetIndex"));
        } else {
            // Store indices for layered textures
            metadata.putInt("skinIndex", random.nextInt(10000));
            metadata.putInt("clothesIndex", random.nextInt(10000));
            metadata.putInt("eyesIndex", random.nextInt(10000));
            metadata.putInt("hairIndex", random.nextInt(10000));
            MagicRealms.LOGGER.debug("Generated layered metadata for {} - skin:{}, clothes:{}, eyes:{}, hair:{}",
                    getEntityName(), metadata.getInt("skinIndex"), metadata.getInt("clothesIndex"),
                    metadata.getInt("eyesIndex"), metadata.getInt("hairIndex"));
        }

        return metadata;
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();

        if (this.level().isClientSide() && isInitialized() && !clientTexturesGenerated) {
            // Schedule texture generation for next tick to ensure textures are loaded
            net.minecraft.client.Minecraft.getInstance().execute(this::generateTexturesFromMetadata);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void generateTexturesFromMetadata() {
        if (!this.level().isClientSide()) return;
        if (clientTexturesGenerated) return;

        CompoundTag metadata = getTextureMetadata();
        if (metadata.isEmpty()) {
            MagicRealms.LOGGER.warn("No texture metadata found for entity {}", getEntityName());
            return;
        }

        // Ensure textures are loaded
        LayeredTextureManager.ensureTexturesLoaded();

        try {
            Gender gender = Gender.valueOf(metadata.getString("gender").toUpperCase());
            EntityClass entityClass = EntityClass.valueOf(metadata.getString("entityClass").toUpperCase());

            if (metadata.getBoolean("usePreset")) {
                // Generate preset texture using server-provided index
                int presetIndex = metadata.getInt("presetIndex");
                TextureComponents preset = generatePresetFromIndex(gender, presetIndex);
                if (preset != null) {
                    this.textureComponents = preset;

                    // Handle preset name synchronization
                    if (preset.hasEntityName()) {
                        String presetName = preset.getEntityName();
                        String currentName = getEntityName();

                        // Only update and sync if the name is different
                        if (!presetName.equals(currentName)) {
                            setEntityName(presetName);

                            // Send name sync packet to server
                            syncPresetNameToServer(presetName);

                            MagicRealms.LOGGER.debug("Generated preset texture for {} with name: {} (syncing to server)",
                                    getEntityName(), presetName);
                        }
                    } else {
                        MagicRealms.LOGGER.debug("Generated preset texture for {} without name", getEntityName());
                    }
                } else {
                    // Fallback to layered if preset fails
                    this.textureComponents = generateLayeredFallback(gender, entityClass, metadata.getInt("presetIndex"));
                    MagicRealms.LOGGER.debug("Preset failed, using layered fallback for {}", getEntityName());
                }
            } else {
                // Generate layered texture using server-provided indices
                TextureComponents layered = generateLayeredFromIndices(
                        gender, entityClass,
                        metadata.getInt("skinIndex"),
                        metadata.getInt("clothesIndex"),
                        metadata.getInt("eyesIndex"),
                        metadata.getInt("hairIndex")
                );
                this.textureComponents = layered;
                MagicRealms.LOGGER.debug("Generated layered texture for {}", getEntityName());
            }

            clientTexturesGenerated = true;

        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to generate textures from metadata for {}", getEntityName(), e);
            // Set a basic fallback
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

    @OnlyIn(Dist.CLIENT)
    private TextureComponents generatePresetFromIndex(Gender gender, int index) {
        if (!LayeredTextureManager.hasAdditionalTextures(gender)) {
            MagicRealms.LOGGER.debug("No additional textures available for gender: {}", gender.getName());
            return null;
        }

        int count = LayeredTextureManager.getAdditionalTextureCount(gender);
        if (count == 0) return null;

        int actualIndex = index % count;

        LayeredTextureManager.TextureWithName preset =
                LayeredTextureManager.getAdditionalTextureByIndex(gender, actualIndex);

        if (preset != null) {
            return new TextureComponents(
                    preset.getTexture().toString(),
                    null, null, null,
                    preset.getName(),
                    true
            );
        }
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    private TextureComponents generateLayeredFromIndices(Gender gender, EntityClass entityClass,
                                                         int skinIndex, int clothesIndex,
                                                         int eyesIndex, int hairIndex) {
        String skinTexture = LayeredTextureManager.getTextureByIndex("skin", skinIndex);
        String clothesTexture = LayeredTextureManager.getClothesTextureByIndex(gender, entityClass, clothesIndex);
        String eyesTexture = LayeredTextureManager.getTextureByIndex("eyes", eyesIndex);
        String hairTexture = LayeredTextureManager.getHairTextureByIndex(gender, hairIndex);

        return new TextureComponents(skinTexture, clothesTexture, eyesTexture, hairTexture, null, false);
    }

    @OnlyIn(Dist.CLIENT)
    private TextureComponents generateLayeredFallback(Gender gender, EntityClass entityClass, int seed) {
        // Use the seed to generate consistent fallback indices
        RandomSource fallbackRandom = RandomSource.create(seed);
        return generateLayeredFromIndices(
                gender, entityClass,
                fallbackRandom.nextInt(10000),
                fallbackRandom.nextInt(10000),
                fallbackRandom.nextInt(10000),
                fallbackRandom.nextInt(10000)
        );
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
            this.setImmortal(true);
            // Schedule the name update to happen after all initialization is complete
            this.level().getServer().execute(() -> {
                if (this.isAlive() && !this.isRemoved()) {
                    KillTrackerData killData = this.getData(MRDataAttachments.KILL_TRACKER);
                    int currentLevel = killData.getCurrentLevel();
                    this.updateCustomNameWithLevel(currentLevel);
                }
            });
        }
    }

    @Override
    protected void handleAppearanceSpecificTick() {
        // No longer needed - textures are generated once on client side
    }

    @Override
    public boolean isExclusiveMercenary() {
        return false;
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
        this.clientTexturesGenerated = false; // Always regenerate on client

        if (compound.contains("TextureMetadata")) {
            CompoundTag metadata = compound.getCompound("TextureMetadata");
            setTextureMetadata(metadata);
        }
    }
}