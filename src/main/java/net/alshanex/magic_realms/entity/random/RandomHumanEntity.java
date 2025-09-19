package net.alshanex.magic_realms.entity.random;

import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.network.RequestTextureGenerationPacket;
import net.alshanex.magic_realms.network.SyncEntityTexturePacket;
import net.alshanex.magic_realms.registry.MREntityRegistry;
import net.alshanex.magic_realms.util.humans.*;
import net.alshanex.magic_realms.Config;
import net.alshanex.magic_realms.util.MRUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.network.PacketDistributor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class RandomHumanEntity extends AbstractMercenaryEntity {

    // Texture-specific data accessors
    private static final EntityDataAccessor<Boolean> HAS_TEXTURE = SynchedEntityData.defineId(RandomHumanEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> TEXTURE_REQUESTED = SynchedEntityData.defineId(RandomHumanEntity.class, EntityDataSerializers.BOOLEAN);

    // Texture management
    private EntityTextureConfig textureConfig;
    private boolean appearanceGenerated = false;

    public RandomHumanEntity(EntityType<? extends AbstractSpellCastingMob> entityType, Level level) {
        super(entityType, level);
    }

    public RandomHumanEntity(Level level, LivingEntity owner) {
        this(MREntityRegistry.HUMAN.get(), level);
        setSummoner(owner);
    }

    // Implement abstract methods from AbstractMercenaryEntity
    @Override
    protected void initializeAppearance(RandomSource randomSource) {
        if (appearanceGenerated) {
            return;
        }

        // IMPORTANT: Use deterministic random for appearance generation
        RandomSource deterministicRandom = getDeterministicRandom();

        Gender gender = Gender.values()[deterministicRandom.nextInt(Gender.values().length)];
        EntityClass entityClass = EntityClass.values()[deterministicRandom.nextInt(EntityClass.values().length)];

        // Initialize basic appearance data
        setGender(gender);
        setEntityClass(entityClass);

        // Only create texture config on client side with DETERMINISTIC random
        if (this.level().isClientSide()) {
            try {
                // Use deterministic random to check if we'll get a preset texture
                double roll = deterministicRandom.nextDouble();
                boolean willUseAdditionalTexture = roll < Config.customTextureChance;

                if (willUseAdditionalTexture && LayeredTextureManager.hasAdditionalTextures(gender)) {
                    LayeredTextureManager.TextureWithName additionalResult =
                            LayeredTextureManager.getRandomAdditionalTextureWithName(gender, deterministicRandom);
                    if (additionalResult != null && additionalResult.getName() != null) {
                        setEntityName(additionalResult.getName());
                        //MagicRealms.LOGGER.debug("Entity {} will use preset texture name: {}", this.getUUID().toString(), additionalResult.getName());
                    } else {
                        // Use deterministic random for name generation
                        String randomName = AdvancedNameManager.getRandomName(gender, deterministicRandom);
                        setEntityName(randomName);
                    }
                } else {
                    // Use deterministic random for name generation
                    String randomName = AdvancedNameManager.getRandomName(gender, deterministicRandom);
                    setEntityName(randomName);
                    //MagicRealms.LOGGER.debug("Entity {} assigned deterministic name: {} (layered texture)", this.getUUID().toString(), randomName);
                }
            } catch (Exception e) {
                // Fallback to deterministic name generation
                String randomName = AdvancedNameManager.getRandomName(gender, deterministicRandom);
                setEntityName(randomName);
                //MagicRealms.LOGGER.warn("Entity {} used fallback name generation: {}", this.getUUID().toString(), randomName);
            }
        } else {
            // Server side - DON'T assign name yet, wait for client to determine texture type
            this.textureConfig = null;
            setEntityName("");
        }

        this.appearanceGenerated = true;
        /*
        MagicRealms.LOGGER.debug("Initialized appearance for entity {}: Gender={}, Class={}, Stars={}, Name={}",
                this.getUUID().toString(), getGender().getName(), getEntityClass().getName(),
                getStarLevel(), getEntityName());

         */
    }

    @Override
    protected void handlePostSpawnInitialization() {
        // Handle texture generation
        if (!this.level().isClientSide) {
            checkAndRequestTexture();
        }
    }

    @Override
    protected void handleAppearanceSpecificTick() {
        if (!this.level().isClientSide && this.tickCount % 20 == 0) { // Check every second
            handleTextureGeneration();
        }
    }

    @Override
    public boolean isExclusiveMercenary() {
        return false;
    }

    // Texture management methods
    public boolean hasTexture() {
        return this.entityData.get(HAS_TEXTURE);
    }

    public void setHasTexture(boolean hasTexture) {
        this.entityData.set(HAS_TEXTURE, hasTexture);
    }

    public boolean isTextureRequested() {
        return this.entityData.get(TEXTURE_REQUESTED);
    }

    public void setTextureRequested(boolean requested) {
        this.entityData.set(TEXTURE_REQUESTED, requested);
    }

    private void checkAndRequestTexture() {
        if (hasExistingServerTexture()) {
            setHasTexture(true);
            // Don't distribute yet - wait for players to track
        } else {
            // Mark as needing texture, but don't request yet if no players are tracking
            setHasTexture(false);
            setTextureRequested(false);

            //MagicRealms.LOGGER.debug("Entity {} spawned without texture, will generate when first tracked", this.getUUID());
        }
    }

    private boolean hasExistingServerTexture() {
        try {
            ServerLevel serverLevel = (ServerLevel) this.level();
            Path worldDir = serverLevel.getServer().getWorldPath(LevelResource.ROOT);
            Path texturePath = worldDir.resolve("magic_realms_textures")
                    .resolve("entity").resolve("human")
                    .resolve(this.getUUID() + "_complete.png");

            return Files.exists(texturePath) && Files.size(texturePath) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public void requestTextureGeneration() {
        // Send request to the first player tracking this entity
        ServerLevel serverLevel = (ServerLevel) this.level();
        List<ServerPlayer> trackingPlayers = serverLevel.getChunkSource().chunkMap
                .getPlayers(new ChunkPos(this.blockPosition()), false);

        if (!trackingPlayers.isEmpty()) {
            ServerPlayer firstPlayer = trackingPlayers.get(0);
            PacketDistributor.sendToPlayer(firstPlayer,
                    new RequestTextureGenerationPacket(this.getUUID(), getGender(), getEntityClass()));

            //MagicRealms.LOGGER.debug("Requested texture generation from player: {} for entity: {}", firstPlayer.getName().getString(), this.getUUID());
        }
    }

    public void distributeExistingTexture() {
        try {
            ServerLevel serverLevel = (ServerLevel) this.level();
            Path worldDir = serverLevel.getServer().getWorldPath(LevelResource.ROOT);
            Path texturePath = worldDir.resolve("magic_realms_textures")
                    .resolve("entity").resolve("human")
                    .resolve(this.getUUID() + "_complete.png");

            if (Files.exists(texturePath)) {
                byte[] textureData = Files.readAllBytes(texturePath);

                // Distribute to all tracking players
                PacketDistributor.sendToPlayersTrackingEntity(this,
                        new SyncEntityTexturePacket(this.getUUID(), this.getId(), textureData, this.getEntityName(), false));

                //MagicRealms.LOGGER.debug("Distributed existing texture for entity: {}", this.getUUID());
            }
        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to distribute existing texture for entity: {}", this.getUUID(), e);
        }
    }

    private void handleTextureGeneration() {
        // If we already have a texture, nothing to do
        if (hasTexture()) {
            return;
        }

        // If we haven't requested texture generation yet
        if (!isTextureRequested()) {
            // Check if any players are tracking us
            if (hasTrackingPlayers()) {
                if (hasExistingServerTexture()) {
                    // We have a saved texture, distribute it
                    setHasTexture(true);
                    distributeExistingTexture();
                } else {
                    // Request texture generation
                    requestTextureGeneration();
                    setTextureRequested(true);
                }
            }
        }
    }

    private boolean hasTrackingPlayers() {
        try {
            ServerLevel serverLevel = (ServerLevel) this.level();
            List<ServerPlayer> trackingPlayers = serverLevel.getChunkSource().chunkMap
                    .getPlayers(new ChunkPos(this.blockPosition()), false);

            return !trackingPlayers.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public int getDeterministicHairIndex() {
        RandomSource deterministicRandom = getDeterministicRandom();
        Gender gender = getGender();
        return LayeredTextureManager.getRandomHairTextureIndex("hair_" + gender.getName(), deterministicRandom);
    }

    public void updateNameFromTexture() {
        if (!this.level().isClientSide()) {
            return; // Only run on client side
        }

        // Only update if name is empty (hasn't been set yet)
        String currentName = getEntityName();
        if (!currentName.isEmpty()) {
            return; // Name already set
        }

        // Get texture config and check for preset texture name
        EntityTextureConfig textureConfig = this.getTextureConfig();
        if (textureConfig != null && textureConfig.hasTextureName()) {
            String textureName = textureConfig.getTextureName();
            setEntityName(textureName);

            //MagicRealms.LOGGER.debug("Updated entity {} name to preset texture name: {}", this.getUUID().toString(), textureName);
        } else {
            // No preset texture name available
            //MagicRealms.LOGGER.debug("Entity {} has no preset texture name available (layered texture)", this.getUUID().toString());
        }
    }

    public void invalidateTextureConfig() {
        this.textureConfig = null;
        MagicRealms.LOGGER.debug("Invalidated texture config for entity {}", this.getUUID());
    }

    public EntityTextureConfig getTextureConfig() {
        if (textureConfig == null) {
            if (!isInitialized()) {
                //MagicRealms.LOGGER.warn("Trying to get texture config before entity is initialized: {}", this.getUUID().toString());
                return null;
            }

            // Only regenerate texture config on client side
            if (!this.level().isClientSide()) {
                //MagicRealms.LOGGER.debug("Server side - not creating texture config for entity {}", this.getEntityName());
                return null;
            }

            // FIXED: Use deterministic approach instead of non-deterministic
            try {
                // Use the entity-based constructor which uses deterministic random
                this.textureConfig = new EntityTextureConfig(this);
                //MagicRealms.LOGGER.debug("Regenerated DETERMINISTIC texture config for entity {}", this.getEntityName());

                // Update name based on texture type
                updateNameFromTexture();

            } catch (Exception e) {
                MagicRealms.LOGGER.error("Failed to regenerate texture config for entity {}", this.getEntityName(), e);
                return null;
            }
        }
        return textureConfig;
    }

    public boolean regenerateTexture() {
        if (this.level().isClientSide()) {
            return false;
        }

        try {
            String entityUUID = this.getUUID().toString();
            Gender gender = this.getGender();
            EntityClass entityClass = this.getEntityClass();

            //MagicRealms.LOGGER.debug("Starting texture regeneration for entity {} (Gender: {}, Class: {})", this.getEntityName(), gender.getName(), entityClass.getName());

            // Remove old texture from cache and delete file
            CombinedTextureManager.removeEntityTexture(entityUUID, true); // true = delete file

            // Generate new hair texture index
            int newHairTextureIndex = LayeredTextureManager.getRandomHairTextureIndex("hair_" + gender.getName());

            // Force regeneration by clearing cache and letting the system recreate
            this.textureConfig = null;

            //MagicRealms.LOGGER.debug("Successfully prepared texture regeneration for entity {} with hair index: {}", this.getEntityName(), newHairTextureIndex);

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
                //MagicRealms.LOGGER.debug("Broadcasted texture regeneration event for entity {}", this.getEntityName());
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

            //MagicRealms.LOGGER.debug("Starting DETERMINISTIC texture and name regeneration for entity {} (Gender: {}, Class: {})", this.getEntityName(), gender.getName(), entityClass.getName());

            // Remove old texture from cache and delete file
            CombinedTextureManager.removeEntityTexture(entityUUID, true); // true = delete file

            // Force regeneration by clearing cache and letting the system recreate
            this.textureConfig = null;

            // Create new texture config using deterministic method (entity-based constructor)
            this.textureConfig = new EntityTextureConfig(this);

            // Update name if we got a preset texture
            if (this.textureConfig != null && this.textureConfig.hasTextureName()) {
                String newTextureName = this.textureConfig.getTextureName();
                setEntityName(newTextureName);
                //MagicRealms.LOGGER.debug("Entity {} got new preset texture name: {}", entityUUID, newTextureName);
            } else {
                // Generate new random name using deterministic random
                String newRandomName = AdvancedNameManager.getRandomName(gender, getDeterministicRandom());
                setEntityName(newRandomName);
                //MagicRealms.LOGGER.debug("Entity {} got new deterministic random name: {} (layered texture)", entityUUID, newRandomName);
            }

            this.updateCustomNameWithStars();

            //MagicRealms.LOGGER.debug("Successfully completed DETERMINISTIC texture and name regeneration for entity {}", this.getEntityName());
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
            if (this.level().isClientSide() && isInitialized()) {
                try {
                    String entityUUID = this.getUUID().toString();
                    Gender gender = this.getGender();
                    EntityClass entityClass = this.getEntityClass();

                    // Remove old texture from client cache
                    CombinedTextureManager.removeEntityTexture(entityUUID, true); // true = delete file

                    // Clear current texture config to force regeneration
                    this.textureConfig = null;

                    // The texture will be regenerated automatically

                    //MagicRealms.LOGGER.debug("Client-side texture regeneration completed for entity {}", this.getEntityName());

                } catch (Exception e) {
                    MagicRealms.LOGGER.error("Error during client-side texture regeneration for entity {}", this.getEntityName(), e);
                }
            }
        } else {
            super.handleEntityEvent(id);
        }
    }

    public void forceInitializeAppearance() {
        if (!isInitialized()) {
            RandomSource randomSource = this.level().getRandom();
            initializeStarLevel(randomSource);
            initializeAppearance(randomSource);
            setInitialized(true);
        }
    }

    private boolean isDying = false;

    @Override
    public void die(net.minecraft.world.damagesource.DamageSource damageSource) {
        if(!isImmortal()) {
            this.isDying = true;
        }
        // Clean up client-side texture resources
        if (this.level().isClientSide && !isImmortal()) {
            CombinedTextureManager.removeEntityTexture(this.getUUID().toString(), true);
        }

        // Clean up server-side texture files if needed
        if (!this.level().isClientSide && !isImmortal()) {
            cleanupServerSideTexture();
        }
        super.die(damageSource);
    }

    private void cleanupServerSideTexture() {
        try {
            ServerLevel serverLevel = (ServerLevel) this.level();
            Path worldDir = serverLevel.getServer().getWorldPath(LevelResource.ROOT);
            Path texturePath = worldDir.resolve("magic_realms_textures")
                    .resolve("entity").resolve("human")
                    .resolve(this.getUUID() + "_complete.png");

            if (Files.exists(texturePath)) {
                Files.delete(texturePath);
                MagicRealms.LOGGER.debug("Deleted server-side texture file for entity: {}", this.getUUID());
            }
        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to delete server-side texture file: {}", e.getMessage());
        }
    }

    @Override
    public boolean isInvisible() {
        return super.isInvisible() || (isDying && !isImmortal());
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

    // Data synchronization for texture-specific fields
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        super.defineSynchedData(pBuilder);
        pBuilder.define(HAS_TEXTURE, false);
        pBuilder.define(TEXTURE_REQUESTED, false);
    }

    // NBT save/load for texture-specific data
    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("AppearanceGenerated", this.appearanceGenerated);
        compound.putBoolean("HasTexture", hasTexture());
        compound.putBoolean("TextureRequested", isTextureRequested());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.appearanceGenerated = compound.getBoolean("AppearanceGenerated");

        if (isInitialized()) {
            this.textureConfig = new EntityTextureConfig(this.getUUID().toString(), getGender(), getEntityClass());
        }

        setHasTexture(compound.getBoolean("HasTexture"));
        setTextureRequested(compound.getBoolean("TextureRequested"));
    }
}