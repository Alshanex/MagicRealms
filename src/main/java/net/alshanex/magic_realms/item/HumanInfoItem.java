package net.alshanex.magic_realms.item;

import net.alshanex.magic_realms.registry.MRItems;
import net.alshanex.magic_realms.screens.HumanInfoMenu;
import net.alshanex.magic_realms.util.EntitySnapshot;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.IContainerFactory;

import javax.annotation.Nullable;
import java.util.List;

public class HumanInfoItem extends Item {

    public HumanInfoItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            EntitySnapshot snapshot = getEntitySnapshot(stack);
            if (snapshot != null) {
                player.openMenu(new HumanInfoMenuProvider(snapshot, stack), buf -> {
                    CompoundTag tag = snapshot.serialize();
                    buf.writeNbt(tag);
                });
            } else {
                player.sendSystemMessage(Component.literal("No entity data found")
                        .withStyle(ChatFormatting.RED));
            }
        }

        return InteractionResultHolder.success(stack);
    }

    private static class HumanInfoMenuProvider implements MenuProvider {
        private final EntitySnapshot snapshot;
        private final ItemStack itemStack;

        public HumanInfoMenuProvider(EntitySnapshot snapshot, ItemStack itemStack) {
            this.snapshot = snapshot;
            this.itemStack = itemStack;
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("gui.magic_realms.human_info.title");
        }

        @Nullable
        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
            return new HumanInfoMenu(containerId, playerInventory, snapshot, itemStack);
        }
    }

    public static ItemStack createVirtualItem(EntitySnapshot snapshot) {
        ItemStack stack = new ItemStack(MRItems.HUMAN_INFO_ITEM.get());
        CompoundTag tag = snapshot.serialize();
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static EntitySnapshot getEntitySnapshot(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            return EntitySnapshot.deserialize(tag);
        }
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        EntitySnapshot snapshot = getEntitySnapshot(stack);
        if (snapshot != null) {
            tooltipComponents.add(Component.literal("Name: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(snapshot.entityName).withStyle(ChatFormatting.WHITE)));

            tooltipComponents.add(Component.literal("Class: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(snapshot.entityClass.getName()).withStyle(ChatFormatting.AQUA)));

            String stars = "★".repeat(snapshot.starLevel);
            ChatFormatting starColor = switch (snapshot.starLevel) {
                case 2 -> ChatFormatting.AQUA;
                case 3 -> ChatFormatting.GOLD;
                default -> ChatFormatting.WHITE;
            };
            tooltipComponents.add(Component.literal("Stars: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(stars).withStyle(starColor)));

            tooltipComponents.add(Component.literal("Level: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("" + snapshot.currentLevel).withStyle(starColor)));

            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.literal("Right Click to open equipment interface.").withStyle(ChatFormatting.YELLOW));
        } else {
            tooltipComponents.add(Component.literal("No entity data").withStyle(ChatFormatting.RED));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return getEntitySnapshot(stack) != null;
    }
}