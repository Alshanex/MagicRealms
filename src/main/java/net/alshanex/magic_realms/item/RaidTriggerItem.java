package net.alshanex.magic_realms.item;

import net.alshanex.magic_realms.raid.CustomRaid;
import net.alshanex.magic_realms.raid.CustomRaidManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class RaidTriggerItem extends Item {

    public RaidTriggerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            ServerLevel serverLevel = (ServerLevel) level;

            CustomRaidManager raidManager = serverLevel.getDataStorage()
                    .computeIfAbsent(
                            CustomRaidManager.factory(serverLevel),
                            "magic_realms_raids"
                    );

            CustomRaid existingRaid = raidManager.getActiveRaidForPlayer(player.getUUID());
            if (existingRaid != null) {
                player.displayClientMessage(
                        Component.literal("You already used a coin")
                                .withStyle(ChatFormatting.RED),
                        true
                );
                return InteractionResultHolder.fail(stack);
            }

            CustomRaid raid = raidManager.createRaid(serverPlayer, player.blockPosition());

            level.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.ENDER_DRAGON_GROWL,
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );

            player.displayClientMessage(
                    Component.translatable("message.magic_realms.custom_raid.start")
                            .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                    false
            );

            if (!player.isCreative()) {
                stack.shrink(1);
            }

            return InteractionResultHolder.success(stack);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.magic_realms.raid_item")
                .withStyle(ChatFormatting.GOLD));
    }
}
