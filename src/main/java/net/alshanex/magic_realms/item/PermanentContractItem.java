package net.alshanex.magic_realms.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class PermanentContractItem extends Item {

    public PermanentContractItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.translatable("tooltip.magic_realms.permanent_contract.level_range")
                .withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal("  Any Level")
                .withStyle(ChatFormatting.GOLD));

        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.translatable("tooltip.magic_realms.permanent_contract.duration")
                .withStyle(ChatFormatting.YELLOW));
        tooltipComponents.add(Component.translatable("tooltip.magic_realms.permanent_contract.duration_desc")
                .withStyle(ChatFormatting.GREEN));

        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.translatable("tooltip.magic_realms.contract.usage")
                .withStyle(ChatFormatting.YELLOW));
        tooltipComponents.add(Component.translatable("tooltip.magic_realms.contract.usage_desc")
                .withStyle(ChatFormatting.GRAY));

        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.translatable("tooltip.magic_realms.permanent_contract.warning")
                .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.magic_realms.contract_permanent")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
