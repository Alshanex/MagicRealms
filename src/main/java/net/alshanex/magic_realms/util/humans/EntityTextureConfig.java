package net.alshanex.magic_realms.util.humans;

import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.resources.ResourceLocation;

public class EntityTextureConfig {
    private final ResourceLocation completeTexture;
    private final Gender gender;
    private final EntityClass entityClass;
    private final String entityUUID;
    private final int hairTextureIndex;
    private final String textureName; // Name from preset texture
    private final boolean isPresetTexture; // Whether this uses a preset texture

    public EntityTextureConfig(String entityUUID, Gender gender, EntityClass entityClass, int hairTextureIndex) {
        this.entityUUID = entityUUID;
        this.gender = gender;
        this.entityClass = entityClass;
        this.hairTextureIndex = hairTextureIndex;

        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            CombinedTextureManager.TextureResult result = CombinedTextureManager.getCombinedTextureWithHairAndName(entityUUID, gender, entityClass, hairTextureIndex);

            if (result != null) {
                this.completeTexture = result.getTextureLocation();
                this.textureName = result.getTextureName();
                this.isPresetTexture = result.isPresetTexture();

                if (this.completeTexture == null) {
                    MagicRealms.LOGGER.error("Failed to create complete texture for entity: {}", entityUUID);
                } else {
                    MagicRealms.LOGGER.debug("Created complete texture config for entity: {} -> texture: {}, hair index: {}, preset name: {}",
                            entityUUID, this.completeTexture, hairTextureIndex, this.textureName);
                }
            } else {
                this.completeTexture = null;
                this.textureName = null;
                this.isPresetTexture = false;
                MagicRealms.LOGGER.error("Failed to get texture result for entity: {}", entityUUID);
            }
        } else {
            this.completeTexture = null;
            this.textureName = null;
            this.isPresetTexture = false;
        }
    }

    public EntityTextureConfig(String entityUUID, Gender gender, EntityClass entityClass) {
        this.entityUUID = entityUUID;
        this.gender = gender;
        this.entityClass = entityClass;

        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            this.hairTextureIndex = LayeredTextureManager.getRandomHairTextureIndex("hair_" + gender.getName());

            CombinedTextureManager.TextureResult result = CombinedTextureManager.getCombinedTextureWithHairAndName(entityUUID, gender, entityClass, hairTextureIndex);

            if (result != null) {
                this.completeTexture = result.getTextureLocation();
                this.textureName = result.getTextureName();
                this.isPresetTexture = result.isPresetTexture();

                if (this.completeTexture == null) {
                    MagicRealms.LOGGER.error("Failed to create complete texture for entity: {}", entityUUID);
                } else {
                    MagicRealms.LOGGER.debug("Created complete texture config for entity: {} -> texture: {}, hair index: {}, preset name: {}",
                            entityUUID, this.completeTexture, hairTextureIndex, this.textureName);
                }
            } else {
                this.completeTexture = null;
                this.textureName = null;
                this.isPresetTexture = false;
                MagicRealms.LOGGER.error("Failed to get texture result for entity: {}", entityUUID);
            }
        } else {
            // Server side - use dummy values
            this.hairTextureIndex = -1;
            this.completeTexture = null;
            this.textureName = null;
            this.isPresetTexture = false;
        }
    }

    public ResourceLocation getSkinTexture() {
        if (completeTexture == null) {
            MagicRealms.LOGGER.warn("No complete texture available for entity: {}, using fallback", entityUUID);
            return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png");
        }
        return completeTexture;
    }

    public Gender getGender() {
        return gender;
    }

    public EntityClass getEntityClass() {
        return entityClass;
    }

    public String getEntityUUID() {
        return entityUUID;
    }

    public int getHairTextureIndex() {
        return hairTextureIndex;
    }

    public boolean hasValidTexture() {
        return completeTexture != null;
    }

    // New methods to get texture name information
    public String getTextureName() {
        return textureName;
    }

    public boolean isPresetTexture() {
        return isPresetTexture;
    }

    public boolean hasTextureName() {
        return textureName != null && !textureName.isEmpty();
    }
}
