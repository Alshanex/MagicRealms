package net.alshanex.magic_realms.item;

import net.alshanex.magic_realms.data.FloatingArrowData;
import net.alshanex.magic_realms.entity.flying_arrow.FloatingArrowEntity;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public class FloatingArrowItem extends Item {

    public FloatingArrowItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide) return;
        if (!(entity instanceof Player player)) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        boolean heldInMain = player.getMainHandItem() == stack;
        FloatingArrowData data = player.getData(MRDataAttachments.FLOATING_ARROW);
        FloatingArrowEntity existing = data.resolve(serverLevel);

        if (heldInMain) {
            if (existing == null) {
                FloatingArrowEntity arrow = new FloatingArrowEntity(level, player);
                level.addFreshEntity(arrow);
                data.setArrow(arrow);
            }
        } else {
            if (existing != null && !(player.getMainHandItem().getItem() instanceof FloatingArrowItem)) {
                existing.discard();
                data.clear();
            }
        }
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack stack, Player player) {
        if (!player.level().isClientSide && player.level() instanceof ServerLevel serverLevel) {
            FloatingArrowData data = player.getData(MRDataAttachments.FLOATING_ARROW);
            FloatingArrowEntity arrow = data.resolve(serverLevel);
            if (arrow != null) {
                arrow.discard();
                data.clear();
            }
        }
        return super.onDroppedByPlayer(stack, player);
    }

    @Nullable
    public static FloatingArrowEntity findArrowForPlayer(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return null;
        FloatingArrowData data = player.getData(MRDataAttachments.FLOATING_ARROW);
        return data.resolve(serverLevel);
    }

    public static void setModeForPlayer(Player player, byte mode) {
        FloatingArrowEntity arrow = findArrowForPlayer(player);
        if (arrow != null) {
            arrow.setMode(mode);
        }
    }
}
