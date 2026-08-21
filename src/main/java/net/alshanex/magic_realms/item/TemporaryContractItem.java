package net.alshanex.magic_realms.item;

import net.alshanex.magic_realms.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class TemporaryContractItem extends Item {

    public TemporaryContractItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents,
                                TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.translatable("tooltip.magic_realms.contract.duration")
                .withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.magic_realms.contract.duration_value",
                        Config.minutesPerContract)
                .withStyle(ChatFormatting.AQUA));

        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.translatable("tooltip.magic_realms.contract.usage")
                .withStyle(ChatFormatting.YELLOW));
        tooltipComponents.add(Component.translatable("tooltip.magic_realms.contract.usage_desc")
                .withStyle(ChatFormatting.GRAY));
    }
}
