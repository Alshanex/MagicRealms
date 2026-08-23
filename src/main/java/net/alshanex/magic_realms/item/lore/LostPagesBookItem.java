package net.alshanex.magic_realms.item.lore;

import net.alshanex.magic_realms.data.PageIdentifier;
import net.alshanex.magic_realms.data.PlayerLoreProgress;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.registry.MRDataComponentRegistry;
import net.alshanex.magic_realms.registry.MRRegistries;
import net.alshanex.magic_realms.story.BossStory;
import net.alshanex.magic_realms.story.ChapterUnlocker;
import net.alshanex.magic_realms.story.LoreProgression;
import net.alshanex.magic_realms.util.MRClientUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;

import java.util.List;
import java.util.Optional;

/**
 * The Lost Pages Book item - the player's lore compendium.
 * <p>
 * Right-click: Opens the book GUI to browse stories and read collected chapters.
 * Right-click while sneaking with a Lost Page in offhand: Inserts the page into the book.
 */
public class LostPagesBookItem extends Item {

    public LostPagesBookItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack bookStack = player.getItemInHand(hand);
        PlayerLoreProgress progress = player.getData(MRDataAttachments.PLAYER_LORE);

        // Has the player unlocked all 7 prequel lore nodes?
        if (!LoreProgression.canOpenBook(progress)) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable("item.magic_realms.book.sealed").withStyle(ChatFormatting.RED),
                        true
                );
            }
            return InteractionResultHolder.fail(bookStack);
        }

        // Seed default-unlocked chapters to the player's attachment
        ensureDefaultUnlocks(player);

        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack otherStack = player.getItemInHand(otherHand);

        // Shift + right-click with a Lost Page in the other hand: insert the page
        if (player.isShiftKeyDown() && !otherStack.isEmpty()) {
            PageIdentifier pageId = otherStack.get(MRDataComponentRegistry.PAGE_IDENTIFIER.get());
            if (pageId != null) {
                return tryInsertPage(level, player, bookStack, otherStack, pageId);
            }
        }

        // Normal right-click: open the book GUI
        if (level.isClientSide) {
            MRClientUtils.openBookScreen(bookStack);
        }

        return InteractionResultHolder.sidedSuccess(bookStack, level.isClientSide);
    }

    private void ensureDefaultUnlocks(Player player) {
        PlayerLoreProgress progress = player.getData(MRDataAttachments.PLAYER_LORE);
        PlayerLoreProgress seeded = progress.withDefaultUnlocks();
        if (seeded != progress) {
            player.setData(MRDataAttachments.PLAYER_LORE, seeded);
        }
    }

    private InteractionResultHolder<ItemStack> tryInsertPage(Level level, Player player,
                                                             ItemStack bookStack, ItemStack pageStack,
                                                             PageIdentifier pageId) {
        ChapterUnlocker.Result check = ChapterUnlocker.canUnlock(player, pageId.storyId(), pageId.chapter());

        // Unknown story or a chapter index the page shouldn't be pointing at: fail quietly.
        if (check == ChapterUnlocker.Result.INVALID_CHAPTER || check == ChapterUnlocker.Result.BOOK_SEALED) {
            return InteractionResultHolder.fail(bookStack);
        }

        if (check == ChapterUnlocker.Result.ALREADY_OWNED) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable("item.magic_realms.book.page_already_collected")
                                .withStyle(ChatFormatting.RED), true);
            }
            return InteractionResultHolder.fail(bookStack);
        }

        if (check == ChapterUnlocker.Result.MISSING_PREREQUISITES) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                        Component.translatable("ui.magic_realms.chapter_page_locked")
                                .withStyle(ChatFormatting.RED)));
            }
            return InteractionResultHolder.fail(bookStack);
        }

        // Requirements met. The client stops here and plays the swing; the server does the real work.
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            ChapterUnlocker.Result result = ChapterUnlocker.tryUnlock(serverPlayer,
                    pageId.storyId(), pageId.chapter(),
                    null, SoundEvents.BOOK_PAGE_TURN);

            // Re-checked server-side, so a desynced client can't consume the page for nothing.
            if (!result.isUnlocked()) {
                return InteractionResultHolder.fail(bookStack);
            }

            pageStack.shrink(1);

            player.displayClientMessage(
                    Component.translatable("item.magic_realms.book.page_added",
                                    BossStory.chapterTitleComponent(pageId.storyId(), pageId.chapter())
                                            .withStyle(ChatFormatting.GOLD),
                                    BossStory.titleComponent(pageId.storyId())
                                            .withStyle(ChatFormatting.GOLD))
                            .withStyle(ChatFormatting.GREEN), true);
        }

        return InteractionResultHolder.sidedSuccess(bookStack, level.isClientSide);
    }

    /**
     * Called when a player completes all chapters of a story.
     * Generates an explorer map to the nearest matching structure.
     */
    private void handleStoryCompletion(Level level, Player player, BossStory story, ResourceLocation storyId) {
        player.displayClientMessage(
                Component.translatable("item.magic_realms.book.story_complete",
                                BossStory.titleComponent(storyId).withStyle(ChatFormatting.GOLD))
                        .withStyle(ChatFormatting.LIGHT_PURPLE), false);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private void openBookScreen(ItemStack bookStack) {
        MRClientUtils.openBookScreen(bookStack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.magic_realms.book.tooltip")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.translatable("item.magic_realms.book.hint")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}