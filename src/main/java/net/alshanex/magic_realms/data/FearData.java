package net.alshanex.magic_realms.data;

import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.common.util.INBTSerializable;

import javax.annotation.Nullable;

public class FearData implements INBTSerializable<CompoundTag> {
    @Nullable private EntityType<?> fearedEntityType;
    @Nullable private TagKey<EntityType<?>> fearedEntityTag;

    public FearData() {}

    @Nullable
    public EntityType<?> getFearedEntityType() { return fearedEntityType; }
    public void setFearedEntityType(@Nullable EntityType<?> type) { this.fearedEntityType = type; }

    @Nullable
    public TagKey<EntityType<?>> getFearedEntityTag() { return fearedEntityTag; }
    public void setFearedEntityTag(@Nullable TagKey<EntityType<?>> tag) { this.fearedEntityTag = tag; }

    public boolean hasAnyFear() {
        return fearedEntityType != null || fearedEntityTag != null;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        if (fearedEntityType != null) {
            tag.putString("feared_entity", BuiltInRegistries.ENTITY_TYPE.getKey(fearedEntityType).toString());
        }
        if (fearedEntityTag != null) {
            tag.putString("feared_entity_tag", fearedEntityTag.location().toString());
        }
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        this.fearedEntityType = null;
        this.fearedEntityTag = null;

        if (tag.contains("feared_entity")) {
            try {
                ResourceLocation loc = ResourceLocation.parse(tag.getString("feared_entity"));
                this.fearedEntityType = BuiltInRegistries.ENTITY_TYPE.get(loc);
            } catch (Exception e) {
                MagicRealms.LOGGER.warn("Failed to parse feared entity: {}", tag.getString("feared_entity"), e);
            }
        }

        if (tag.contains("feared_entity_tag")) {
            try {
                ResourceLocation loc = ResourceLocation.parse(tag.getString("feared_entity_tag"));
                this.fearedEntityTag = TagKey.create(Registries.ENTITY_TYPE, loc);
            } catch (Exception e) {
                MagicRealms.LOGGER.warn("Failed to parse feared entity tag: {}", tag.getString("feared_entity_tag"), e);
            }
        }
    }

    public static class Serializer implements IAttachmentSerializer<CompoundTag, FearData> {
        @Override
        public FearData read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            FearData data = new FearData();
            data.deserializeNBT(provider, tag);
            return data;
        }

        @Nullable
        @Override
        public CompoundTag write(FearData attachment, HolderLookup.Provider provider) {
            return attachment.serializeNBT(provider);
        }
    }
}
