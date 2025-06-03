package net.alshanex.magic_realms.item;

import net.alshanex.magic_realms.util.ContractTier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class TieredContractItem extends Item {
    private final ContractTier tier;

    public TieredContractItem(Properties properties, ContractTier tier) {
        super(properties);
        this.tier = tier;
    }

    public ContractTier getTier() {
        return tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.translatable("tooltip.magic_realms.contract.level_range")
                .withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal("  Level " + tier.getMinLevel() + " - " + tier.getMaxLevel())
                .withStyle(tier.getColor()));

        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.translatable("tooltip.magic_realms.contract.usage")
                .withStyle(ChatFormatting.YELLOW));
        tooltipComponents.add(Component.translatable("tooltip.magic_realms.contract.usage_desc")
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.magic_realms.contract_" + tier.getName())
                .withStyle(tier.getColor());
    }
}
