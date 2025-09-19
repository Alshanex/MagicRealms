package net.alshanex.magic_realms.util.humans.appearance;

import net.minecraft.nbt.CompoundTag;

public class TextureComponents {
    private final String skinTexture;
    private final String clothesTexture;
    private final String eyesTexture;
    private final String hairTexture;
    private final String entityName;
    private final boolean isPresetTexture;

    public TextureComponents(String skinTexture, String clothesTexture, String eyesTexture,
                             String hairTexture, String entityName, boolean isPresetTexture) {
        this.skinTexture = skinTexture;
        this.clothesTexture = clothesTexture;
        this.eyesTexture = eyesTexture;
        this.hairTexture = hairTexture;
        this.entityName = entityName;
        this.isPresetTexture = isPresetTexture;
    }

    // Getters
    public String getSkinTexture() { return skinTexture; }
    public String getClothesTexture() { return clothesTexture; }
    public String getEyesTexture() { return eyesTexture; }
    public String getHairTexture() { return hairTexture; }
    public String getEntityName() { return entityName; }
    public boolean isPresetTexture() { return isPresetTexture; }
    public boolean hasEntityName() { return entityName != null && !entityName.isEmpty(); }

    // Serialize to NBT for persistence
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        if (skinTexture != null) tag.putString("skin", skinTexture);
        if (clothesTexture != null) tag.putString("clothes", clothesTexture);
        if (eyesTexture != null) tag.putString("eyes", eyesTexture);
        if (hairTexture != null) tag.putString("hair", hairTexture);
        if (entityName != null) tag.putString("entityName", entityName);
        tag.putBoolean("isPreset", isPresetTexture);
        return tag;
    }

    // Deserialize from NBT
    public static TextureComponents fromNBT(CompoundTag tag) {
        return new TextureComponents(
                tag.contains("skin") ? tag.getString("skin") : null,
                tag.contains("clothes") ? tag.getString("clothes") : null,
                tag.contains("eyes") ? tag.getString("eyes") : null,
                tag.contains("hair") ? tag.getString("hair") : null,
                tag.contains("entityName") ? tag.getString("entityName") : null,
                tag.getBoolean("isPreset")
        );
    }
}
