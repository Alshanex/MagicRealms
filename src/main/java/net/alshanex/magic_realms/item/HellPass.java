package net.alshanex.magic_realms.item;

import net.alshanex.magic_realms.entity.RandomHumanEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

public class HellPass extends Item {
    public HellPass() {
        super(new Properties().stacksTo(64).rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity interactionTarget, InteractionHand usedHand) {
        if (!(interactionTarget instanceof RandomHumanEntity humanEntity)) {
            return InteractionResult.PASS;
        }

        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // Check if the entity is already immortal
        if (humanEntity.isImmortal()) {
            player.sendSystemMessage(Component.translatable("message.magic_realms.already_immortal",
                    humanEntity.getEntityName()));
            return InteractionResult.FAIL;
        }

        // Grant immortality
        humanEntity.setImmortal(true);

        // Consume the item
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        // Play sound and send message
        player.playSound(SoundEvents.TOTEM_USE, 1.0F, 1.0F);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.translatable("message.magic_realms.granted_immortality",
                    humanEntity.getEntityName()));
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("tooltip.magic_realms.hell_pass").withStyle(ChatFormatting.GOLD));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
