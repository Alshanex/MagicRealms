package net.alshanex.magic_realms.util.humans;

import net.minecraft.resources.ResourceLocation;

public class EntityTextureConfig {
    private final ResourceLocation combinedTexture;
    private final Gender gender;
    private final EntityClass entityClass;

    public EntityTextureConfig(String entityUUID, Gender gender, EntityClass entityClass) {
        this.gender = gender;
        this.entityClass = entityClass;
        this.combinedTexture = CombinedTextureManager.getCombinedTexture(entityUUID, gender, entityClass);
    }

    public ResourceLocation getSkinTexture() {
        return combinedTexture;
    }

    public Gender getGender() {
        return gender;
    }

    public EntityClass getEntityClass() {
        return entityClass;
    }
}
