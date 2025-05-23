package net.alshanex.magic_realms.util.humans;

import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.resources.ResourceLocation;

public class EntityTextureConfig {
    private final ResourceLocation combinedTexture;
    private final Gender gender;
    private final EntityClass entityClass;
    private final String entityUUID;

    public EntityTextureConfig(String entityUUID, Gender gender, EntityClass entityClass) {
        this.entityUUID = entityUUID;
        this.gender = gender;
        this.entityClass = entityClass;

        // Solo crear la textura si estamos en el cliente
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            this.combinedTexture = CombinedTextureManager.getCombinedTexture(entityUUID, gender, entityClass);
            if (this.combinedTexture == null) {
                MagicRealms.LOGGER.error("Failed to create texture for entity: {}", entityUUID);
            } else {
                MagicRealms.LOGGER.debug("Created texture config for entity: {} -> {}", entityUUID, this.combinedTexture);
            }
        } else {
            this.combinedTexture = null;
        }
    }

    public ResourceLocation getSkinTexture() {
        if (combinedTexture == null) {
            // Fallback: devolver una textura por defecto si no tenemos la combinada
            MagicRealms.LOGGER.warn("No combined texture available for entity: {}, using fallback", entityUUID);
            return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png");
        }
        return combinedTexture;
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

    public boolean hasValidTexture() {
        return combinedTexture != null;
    }
}
