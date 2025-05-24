package net.alshanex.magic_realms.util.humans;

import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.resources.ResourceLocation;

public class EntityTextureConfig {
    private final ResourceLocation combinedTexture;
    private final ResourceLocation hairTexture;
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
            this.combinedTexture = CombinedTextureManager.getCombinedTextureWithoutHair(entityUUID, gender, entityClass);

            if (hairTextureIndex >= 0) {
                this.hairTexture = LayeredTextureManager.getTextureByIndex("hair_" + gender.getName(), hairTextureIndex);
            } else {
                this.hairTexture = null;
            }

            if (this.combinedTexture == null) {
                MagicRealms.LOGGER.error("Failed to create base texture for entity: {}", entityUUID);
            }

            if (this.hairTexture == null && hairTextureIndex >= 0) {
                MagicRealms.LOGGER.warn("Failed to load hair texture with index {} for entity: {} (gender: {})",
                        hairTextureIndex, entityUUID, gender.getName());
            }

            MagicRealms.LOGGER.debug("Created texture config for entity: {} -> base: {}, hair: {} (index: {})",
                    entityUUID, this.combinedTexture, this.hairTexture, hairTextureIndex);
        } else {
            this.combinedTexture = null;
            this.hairTexture = null;
        }
    }

    public EntityTextureConfig(String entityUUID, Gender gender, EntityClass entityClass) {
        this(entityUUID, gender, entityClass, LayeredTextureManager.getRandomHairTextureIndex("hair_" + gender.getName()));
    }

    public ResourceLocation getSkinTexture() {
        if (combinedTexture == null) {
            MagicRealms.LOGGER.warn("No combined texture available for entity: {}, using fallback", entityUUID);
            return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png");
        }
        return combinedTexture;
    }

    public ResourceLocation getHairTexture() {
        return hairTexture;
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
        return combinedTexture != null;
    }

    public boolean hasHairTexture() {
        return hairTexture != null;
    }
}
