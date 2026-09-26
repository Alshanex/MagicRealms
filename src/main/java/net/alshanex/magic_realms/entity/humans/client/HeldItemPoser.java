package net.alshanex.magic_realms.entity.humans.client;

import net.alshanex.magic_realms.entity.humans.AbstractMercenaryEntity;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;

import javax.annotation.Nullable;

public interface HeldItemPoser {

    boolean appliesTo(ItemStack stack);

    void pose(Context ctx);

    record Context(
            AbstractMercenaryEntity entity,
            ItemStack stack,
            boolean heldInRightArm,
            @Nullable GeoBone shootingArm,
            @Nullable GeoBone supportArm,
            @Nullable GeoBone head,
            float partialTick
    ) {}
}
