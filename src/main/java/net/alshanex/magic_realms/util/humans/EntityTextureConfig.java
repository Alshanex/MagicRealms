package net.alshanex.magic_realms.util.humans;

import net.minecraft.resources.ResourceLocation;
import net.alshanex.magic_realms.util.humans.LayeredTextureManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class EntityTextureConfig {
    private final ResourceLocation skinTexture;
    private final ResourceLocation eyesTexture;
    private final ResourceLocation clothesTexture;
    private final ResourceLocation hairTexture;

    public EntityTextureConfig(Gender gender, EntityClass entityClass) {
        this.skinTexture = LayeredTextureManager.getRandomTexture("skin");
        this.eyesTexture = LayeredTextureManager.getRandomTexture("eyes");
        this.hairTexture = LayeredTextureManager.getRandomTexture("hair_" + gender.getName());
        this.clothesTexture = getRandomClothesTexture(gender, entityClass);
    }

    private ResourceLocation getRandomClothesTexture(Gender gender, EntityClass entityClass) {
        List<ResourceLocation> availableTextures = new ArrayList<>();

        List<ResourceLocation> commonTextures = LayeredTextureManager.TEXTURE_CACHE.get("clothes_common_" + gender.getName());
        if (commonTextures != null) {
            availableTextures.addAll(commonTextures);
        }

        List<ResourceLocation> classTextures = LayeredTextureManager.TEXTURE_CACHE.get("clothes_" + entityClass.getName() + "_" + gender.getName());
        if (classTextures != null) {
            availableTextures.addAll(classTextures);
        }

        if (availableTextures.isEmpty()) {
            return null;
        }

        return availableTextures.get(new Random().nextInt(availableTextures.size()));
    }

    public ResourceLocation getSkinTexture() { return skinTexture; }
    public ResourceLocation getEyesTexture() { return eyesTexture; }
    public ResourceLocation getClothesTexture() { return clothesTexture; }
    public ResourceLocation getHairTexture() { return hairTexture; }
}
