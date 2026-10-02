package net.alshanex.magic_realms.item;

import net.alshanex.magic_realms.util.MRClientUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.List;

/**
 * Right-click: blows the horn (sound heard by nearby players, goat-horn raise animation) and, once the short toot
 * finishes, opens the contracted mercenaries screen for the user.
 */
public class MercenaryHornItem extends Item {

    private static final int TOOT_DURATION_TICKS = 12;

    private static final int COOLDOWN_TICKS = 120;

    private static final int HORN_VARIANT = 5;

    public MercenaryHornItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        player.startUsingItem(hand);
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);

        if (!level.isClientSide) {
            SoundEvent sound = SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(HORN_VARIANT).value();
            level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 4.0f, 1.0f);
            level.gameEvent(GameEvent.INSTRUMENT_PLAY, player.position(), GameEvent.Context.of(player));
            player.awardStat(Stats.ITEM_USED.get(this));
        }

        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        openScreen(level, entity);
        return stack;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        openScreen(level, entity);
    }

    private static void openScreen(Level level, LivingEntity entity) {
        if (level.isClientSide && entity instanceof Player) {
            MRClientUtils.openContractedMercenariesScreen();
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return TOOT_DURATION_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.TOOT_HORN;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.magic_realms.mercenary_horn").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}