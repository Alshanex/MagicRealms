package net.alshanex.magic_realms.item;

import net.alshanex.magic_realms.registry.MRItems;
import net.alshanex.magic_realms.screens.HumanTeamMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import javax.annotation.Nullable;
import java.util.List;

public class HumanTeamItem extends Item implements ICurioItem {

    public HumanTeamItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            player.openMenu(new HumanTeamMenuProvider(stack));
        }

        return InteractionResultHolder.success(stack);
    }

    private static class HumanTeamMenuProvider implements MenuProvider {
        private final ItemStack itemStack;

        public HumanTeamMenuProvider(ItemStack itemStack) {
            this.itemStack = itemStack;
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("gui.magic_realms.human_team.title");
        }

        @Nullable
        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
            return new HumanTeamMenu(containerId, playerInventory, itemStack);
        }
    }

    public static void saveTeamToItem(ItemStack teamItem, ItemStack[] teamMembers) {
        CompoundTag tag = new CompoundTag();
        ListTag teamList = new ListTag();

        for (int i = 0; i < 4; i++) {
            CompoundTag memberTag = new CompoundTag();
            if (i < teamMembers.length && !teamMembers[i].isEmpty()) {
                memberTag.putString("item_data", serializeItemStack(teamMembers[i]));
            }
            teamList.add(memberTag);
        }

        tag.put("team_members", teamList);
        tag.putLong("last_updated", System.currentTimeMillis());

        teamItem.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        // Log para debug
        int memberCount = 0;
        for (ItemStack member : teamMembers) {
            if (!member.isEmpty()) memberCount++;
        }

        net.alshanex.magic_realms.MagicRealms.LOGGER.debug("Saved team to item: {} members", memberCount);
    }

    public static ItemStack[] loadTeamFromItem(ItemStack teamItem) {
        ItemStack[] teamMembers = new ItemStack[4];

        // Inicializar con stacks vacíos
        for (int i = 0; i < 4; i++) {
            teamMembers[i] = ItemStack.EMPTY;
        }

        CustomData customData = teamItem.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("team_members")) {
                ListTag teamList = tag.getList("team_members", 10);

                for (int i = 0; i < Math.min(teamList.size(), 4); i++) {
                    CompoundTag memberTag = teamList.getCompound(i);
                    if (memberTag.contains("item_data")) {
                        String itemData = memberTag.getString("item_data");
                        ItemStack member = deserializeItemStack(itemData);
                        if (!member.isEmpty()) {
                            teamMembers[i] = member;
                        }
                    }
                }
            }
        }

        return teamMembers;
    }

    private static String serializeItemStack(ItemStack stack) {
        try {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            tag.putByte("count", (byte) stack.getCount());

            if (stack.has(DataComponents.CUSTOM_DATA)) {
                tag.put("custom_data", stack.get(DataComponents.CUSTOM_DATA).copyTag());
            }

            return tag.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static ItemStack deserializeItemStack(String data) {
        try {
            if (data.isEmpty()) return ItemStack.EMPTY;

            CompoundTag tag = net.minecraft.nbt.TagParser.parseTag(data);
            String itemId = tag.getString("id");
            int count = tag.getByte("count");

            ResourceLocation itemLocation = ResourceLocation.parse(itemId);
            Item item = BuiltInRegistries.ITEM.get(itemLocation);

            if (item != null && item != Items.AIR) {
                ItemStack stack = new ItemStack(item, count);

                if (tag.contains("custom_data")) {
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag.getCompound("custom_data")));
                }

                return stack;
            }
        } catch (Exception e) {
            // Error deserializando, devolver stack vacío
        }

        return ItemStack.EMPTY;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        ItemStack[] teamMembers = loadTeamFromItem(stack);
        int memberCount = 0;

        for (ItemStack member : teamMembers) {
            if (!member.isEmpty()) {
                memberCount++;
            }
        }

        tooltipComponents.add(Component.literal("Team Members: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(memberCount + "/4").withStyle(ChatFormatting.WHITE)));

        if (memberCount > 0) {
            tooltipComponents.add(Component.empty());
            for (int i = 0; i < teamMembers.length; i++) {
                if (!teamMembers[i].isEmpty()) {
                    // Extraer información básica del HumanInfoItem
                    String memberInfo = extractMemberInfo(teamMembers[i]);
                    tooltipComponents.add(Component.literal("• " + memberInfo).withStyle(ChatFormatting.AQUA));
                }
            }
        }

        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.literal("Right Click to manage team.").withStyle(ChatFormatting.YELLOW));
        tooltipComponents.add(Component.literal("Equip in Curio slot and press the corresponding keybind to summon allies.").withStyle(ChatFormatting.GREEN));
    }

    private String extractMemberInfo(ItemStack humanInfoItem) {
        try {
            var snapshot = HumanInfoItem.getEntitySnapshot(humanInfoItem);
            if (snapshot != null) {
                String stars = "★".repeat(snapshot.starLevel);
                return String.format("%s %s (%s)", stars, snapshot.entityName, snapshot.entityClass.getName());
            }
        } catch (Exception e) {
            // Fallback si hay algún error
        }
        return "Unknown Member";
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        ItemStack[] teamMembers = loadTeamFromItem(stack);
        for (ItemStack member : teamMembers) {
            if (!member.isEmpty()) {
                return true; // Brillar si tiene al menos un miembro
            }
        }
        return false;
    }
}
