package net.alshanex.magic_realms.util.humans;

import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.resources.ResourceLocation;

public class EntityTextureConfig {
    private final ResourceLocation completeTexture;
    private final Gender gender;
    private final EntityClass entityClass;
    private final String entityUUID;
    private final int hairTextureIndex;

    public EntityTextureConfig(String entityUUID, Gender gender, EntityClass entityClass, int hairTextureIndex) {
        this.entityUUID = entityUUID;
        this.gender = gender;
        this.entityClass = entityClass;
        this.hairTextureIndex = hairTextureIndex;

        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            this.completeTexture = CombinedTextureManager.getCombinedTextureWithHair(entityUUID, gender, entityClass, hairTextureIndex);

            if (this.completeTexture == null) {
                MagicRealms.LOGGER.error("Failed to create complete texture for entity: {}", entityUUID);
            } else {
                MagicRealms.LOGGER.debug("Created complete texture config for entity: {} -> texture: {}, hair index: {}",
                        entityUUID, this.completeTexture, hairTextureIndex);
            }
        } else {
            this.completeTexture = null;
        }
    }

    public EntityTextureConfig(String entityUUID, Gender gender, EntityClass entityClass) {
        this.entityUUID = entityUUID;
        this.gender = gender;
        this.entityClass = entityClass;

        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            this.hairTextureIndex = LayeredTextureManager.getRandomHairTextureIndex("hair_" + gender.getName());
            this.completeTexture = CombinedTextureManager.getCombinedTextureWithHair(entityUUID, gender, entityClass, hairTextureIndex);

            if (this.completeTexture == null) {
                MagicRealms.LOGGER.error("Failed to create complete texture for entity: {}", entityUUID);
            } else {
                MagicRealms.LOGGER.debug("Created complete texture config for entity: {} -> texture: {}, hair index: {}",
                        entityUUID, this.completeTexture, hairTextureIndex);
            }
        } else {
            // Server side - use dummy values
            this.hairTextureIndex = -1;
            this.completeTexture = null;
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
}
