package net.alshanex.magic_realms.item;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.KillTrackerData;
import net.alshanex.magic_realms.entity.RandomHumanEntity;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.registry.MRItems;
import net.alshanex.magic_realms.screens.HumanInfoScreen;
import net.alshanex.magic_realms.util.EntitySnapshot;
import net.alshanex.magic_realms.util.humans.EntityClass;
import net.alshanex.magic_realms.util.humans.Gender;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;
import java.util.UUID;

public class HumanInfoItem extends Item {

    public HumanInfoItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            EntitySnapshot snapshot = getEntitySnapshot(stack);
            if (snapshot != null) {
                openInfoScreen(snapshot);
            } else {
                player.sendSystemMessage(Component.literal("No entity data found")
                        .withStyle(ChatFormatting.RED));
            }
        }

        return InteractionResultHolder.success(stack);
    }

    @OnlyIn(Dist.CLIENT)
    private void openInfoScreen(EntitySnapshot snapshot) {
        Minecraft.getInstance().setScreen(new HumanInfoScreen(snapshot));
    }

    public static ItemStack createLinkedItem(RandomHumanEntity entity) {
        ItemStack stack = new ItemStack(MRItems.HUMAN_INFO_ITEM.get());
        setLinkedEntity(stack, entity);
        return stack;
    }

    public static ItemStack createVirtualItem(EntitySnapshot snapshot) {
        ItemStack stack = new ItemStack(MRItems.HUMAN_INFO_ITEM.get());
        CompoundTag tag = snapshot.serialize();
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static void setLinkedEntity(ItemStack stack, RandomHumanEntity entity) {
        EntitySnapshot snapshot = EntitySnapshot.fromEntity(entity);
        CompoundTag tag = snapshot.serialize();
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static EntitySnapshot getEntitySnapshot(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            return EntitySnapshot.deserialize(tag);
        }
        return null;
    }

    // Métodos legacy para compatibilidad con comandos existentes
    public static UUID getLinkedEntityUUID(ItemStack stack) {
        EntitySnapshot snapshot = getEntitySnapshot(stack);
        return snapshot != null ? snapshot.entityUUID : null;
    }

    public static String getLinkedEntityName(ItemStack stack) {
        EntitySnapshot snapshot = getEntitySnapshot(stack);
        return snapshot != null ? snapshot.entityName : "Unknown";
    }

    public static String getLinkedEntityClass(ItemStack stack) {
        EntitySnapshot snapshot = getEntitySnapshot(stack);
        return snapshot != null ? snapshot.entityClass.getName() : "Unknown";
    }

    public static int getLinkedEntityStarLevel(ItemStack stack) {
        EntitySnapshot snapshot = getEntitySnapshot(stack);
        return snapshot != null ? snapshot.starLevel : 1;
    }

    public static int getLinkedEntityLevel(ItemStack stack) {
        EntitySnapshot snapshot = getEntitySnapshot(stack);
        return snapshot != null ? snapshot.currentLevel : 1;
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
            tooltipComponents.add(Component.literal("Right Click to see more info.").withStyle(ChatFormatting.YELLOW));
        } else {
            tooltipComponents.add(Component.literal("No entity data").withStyle(ChatFormatting.RED));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return getEntitySnapshot(stack) != null;
    }
}
