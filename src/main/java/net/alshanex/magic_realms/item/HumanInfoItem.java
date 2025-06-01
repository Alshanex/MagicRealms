package net.alshanex.magic_realms.item;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.KillTrackerData;
import net.alshanex.magic_realms.entity.RandomHumanEntity;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.registry.MRItems;
import net.alshanex.magic_realms.screens.HumanInfoScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
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
            UUID entityUUID = getLinkedEntityUUID(stack);
            if (entityUUID != null) {
                RandomHumanEntity entity = findEntityByUUID(level, entityUUID);
                if (entity != null) {
                    openInfoScreen(entity);
                } else {
                    player.sendSystemMessage(Component.literal("Couldn't find entity")
                            .withStyle(ChatFormatting.RED));
                }
            } else {
                player.sendSystemMessage(Component.literal("No entity is linked to this item")
                        .withStyle(ChatFormatting.RED));
            }
        }

        return InteractionResultHolder.success(stack);
    }

    @OnlyIn(Dist.CLIENT)
    private void openInfoScreen(RandomHumanEntity entity) {
        Minecraft.getInstance().setScreen(new HumanInfoScreen(entity));
    }

    private RandomHumanEntity findEntityByUUID(Level level, UUID uuid) {
        if (level instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(uuid);
            if (entity instanceof RandomHumanEntity humanEntity) {
                return humanEntity;
            }
        } else {
            // Cliente: buscar en entidades cargadas usando un área muy amplia
            AABB searchBox = new AABB(-3000, -52, -3000, 3000, 52, 3000);

            List<Entity> entities = level.getEntities((Entity)null, searchBox, (entity) ->
                    entity instanceof RandomHumanEntity && entity.getUUID().equals(uuid));

            if (!entities.isEmpty()) {
                return (RandomHumanEntity) entities.get(0);
            }
        }
        return null;
    }

    public static ItemStack createLinkedItem(RandomHumanEntity entity) {
        ItemStack stack = new ItemStack(MRItems.HUMAN_INFO_ITEM.get());
        setLinkedEntity(stack, entity);
        return stack;
    }

    public static void setLinkedEntity(ItemStack stack, RandomHumanEntity entity) {
        CompoundTag tag = new CompoundTag();
        tag.putString("LinkedEntityUUID", entity.getUUID().toString());
        tag.putString("EntityName", entity.getEntityName());
        tag.putString("EntityClass", entity.getEntityClass().getName());
        tag.putInt("StarLevel", entity.getStarLevel());

        KillTrackerData killData = entity.getData(MRDataAttachments.KILL_TRACKER);
        tag.putInt("Level", killData.getCurrentLevel());

        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static UUID getLinkedEntityUUID(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("LinkedEntityUUID")) {
                try {
                    return UUID.fromString(tag.getString("LinkedEntityUUID"));
                } catch (IllegalArgumentException e) {
                    MagicRealms.LOGGER.warn("Invalid UUID in HumanInfoItem: {}", tag.getString("LinkedEntityUUID"));
                }
            }
        }
        return null;
    }

    public static String getLinkedEntityName(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            return tag.getString("EntityName");
        }
        return "Unknown";
    }

    public static String getLinkedEntityClass(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            return tag.getString("EntityClass");
        }
        return "Unknown";
    }

    public static int getLinkedEntityStarLevel(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            return tag.getInt("StarLevel");
        }
        return 1;
    }

    public static int getLinkedEntityLevel(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            return tag.getInt("Level");
        }
        return 1;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        UUID entityUUID = getLinkedEntityUUID(stack);
        if (entityUUID != null) {
            String entityName = getLinkedEntityName(stack);
            String entityClass = getLinkedEntityClass(stack);
            int starLevel = getLinkedEntityStarLevel(stack);
            int level = getLinkedEntityLevel(stack);

            tooltipComponents.add(Component.literal("Name: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(entityName).withStyle(ChatFormatting.WHITE)));

            tooltipComponents.add(Component.literal("Class: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(entityClass).withStyle(ChatFormatting.AQUA)));

            String stars = "★".repeat(starLevel);
            ChatFormatting starColor = switch (starLevel) {
                case 2 -> ChatFormatting.AQUA;
                case 3 -> ChatFormatting.GOLD;
                default -> ChatFormatting.WHITE;
            };
            tooltipComponents.add(Component.literal("Stars: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(stars).withStyle(starColor)));

            tooltipComponents.add(Component.literal("Level: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("" + level).withStyle(starColor)));

            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.literal("Right Click to see more info.").withStyle(ChatFormatting.YELLOW));
        } else {
            tooltipComponents.add(Component.literal("No entity linked").withStyle(ChatFormatting.RED));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return getLinkedEntityUUID(stack) != null;
    }
}
