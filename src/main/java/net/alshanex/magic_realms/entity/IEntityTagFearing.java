package net.alshanex.magic_realms.entity;

import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

import javax.annotation.Nullable;

public interface IEntityTagFearing {
    @Nullable
    TagKey<EntityType<?>> getFearedEntityTag();

    void setFearedEntityTag(@Nullable TagKey<EntityType<?>> entityTag);
}
