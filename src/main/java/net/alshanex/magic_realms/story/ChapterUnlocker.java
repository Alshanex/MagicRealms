package net.alshanex.magic_realms.story;

import net.alshanex.magic_realms.data.PageIdentifier;
import net.alshanex.magic_realms.data.PlayerLoreProgress;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.registry.MRRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Single entry point for granting a story chapter to a player.
 * <p>
 * Every unlock route (Lost Page insertion, structure visit, commands, future advancement triggers) should go through here so the requirement checks stay identical and can never drift apart.
 */
public final class ChapterUnlocker {

    private ChapterUnlocker() {}

    /** Why an unlock attempt succeeded or was rejected. */
    public enum Result {
        /** The chapter was granted. */
        UNLOCKED,
        /** The player already owns this chapter - do not grant it twice. */
        ALREADY_OWNED,
        /** One or more {@code requires(...)} prerequisites are missing. */
        MISSING_PREREQUISITES,
        /** The player has not finished the prequel lore, so the book itself is still sealed. */
        BOOK_SEALED,
        /** Unknown story id or out-of-range chapter index. */
        INVALID_CHAPTER;

        public boolean isUnlocked() {
            return this == UNLOCKED;
        }
    }

    /**
     * Seeds any {@code unlockedByDefault()} chapters into the player's attachment.
     * <p>
     * Must run before prerequisite checks: a chapter can list a default-unlocked chapter as its prerequisite,
     * and a player who has never opened the book would otherwise fail that check.
     */
    public static void ensureDefaultUnlocks(Player player) {
        PlayerLoreProgress progress = player.getData(MRDataAttachments.PLAYER_LORE);
        PlayerLoreProgress seeded = progress.withDefaultUnlocks();
        if (seeded != progress) {
            player.setData(MRDataAttachments.PLAYER_LORE, seeded);
        }
    }

    /**
     * Evaluates whether a chapter could be unlocked right now, without mutating anything.
     * Useful for tooltips, previews or cheap early-outs before doing expensive world lookups.
     */
    public static Result canUnlock(Player player, ResourceLocation storyId, int chapter) {
        PlayerLoreProgress progress = player.getData(MRDataAttachments.PLAYER_LORE);

        BossStory story = MRRegistries.BOSS_STORIES.get(storyId);
        if (story == null || !story.isValidChapter(chapter)) {
            return Result.INVALID_CHAPTER;
        }

        if (!LoreProgression.canOpenBook(progress)) {
            return Result.BOOK_SEALED;
        }

        if (progress.hasPage(storyId, chapter)) {
            return Result.ALREADY_OWNED;
        }

        for (PageIdentifier prereq : story.getChapter(chapter).prerequisites()) {
            if (!progress.hasPage(prereq.storyId(), prereq.chapter())) {
                return Result.MISSING_PREREQUISITES;
            }
        }

        return Result.UNLOCKED;
    }

    /**
     * Attempts to grant a chapter to the player.
     *
     * @param message optional chat message sent only on a successful unlock
     * @param sound   optional sound played at the player only on a successful unlock
     * @return the outcome; callers decide whether a rejection deserves feedback
     */
    public static Result tryUnlock(ServerPlayer player, ResourceLocation storyId, int chapter,
                                   @Nullable Component message, @Nullable SoundEvent sound) {
        ensureDefaultUnlocks(player);

        Result check = canUnlock(player, storyId, chapter);
        if (!check.isUnlocked()) {
            return check;
        }

        return apply(player, storyId, chapter, message, sound);
    }

    /**
     * Grants a chapter with no requirement checks: no book seal, no prerequisites.
     * <p>
     * Intended for commands and debugging, not for gameplay routes. It still refuses to grant a chapter the player already owns and still fires story completion,
     * so the attachment cannot be pushed into a state the normal path could not have produced - only reached sooner than intended.
     */
    public static Result grantUnchecked(ServerPlayer player, ResourceLocation storyId, int chapter,
                                        @Nullable Component message, @Nullable SoundEvent sound) {
        ensureDefaultUnlocks(player);

        BossStory story = MRRegistries.BOSS_STORIES.get(storyId);
        if (story == null || !story.isValidChapter(chapter)) {
            return Result.INVALID_CHAPTER;
        }

        if (player.getData(MRDataAttachments.PLAYER_LORE).hasPage(storyId, chapter)) {
            return Result.ALREADY_OWNED;
        }

        return apply(player, storyId, chapter, message, sound);
    }

    /** The actual grant. Both entry points funnel here so completion handling can never diverge. */
    private static Result apply(ServerPlayer player, ResourceLocation storyId, int chapter,
                                @Nullable Component message, @Nullable SoundEvent sound) {
        PlayerLoreProgress before = player.getData(MRDataAttachments.PLAYER_LORE);
        PlayerLoreProgress after = before.withPage(storyId, chapter);
        player.setData(MRDataAttachments.PLAYER_LORE, after);

        if (message != null) {
            player.sendSystemMessage(message);
        }

        if (sound != null) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    sound, SoundSource.PLAYERS, 1.0F, 1.0F);
        }

        if (!before.isStoryComplete(storyId) && after.isStoryComplete(storyId)) {
            handleStoryCompletion(player, storyId);
        }

        return Result.UNLOCKED;
    }

    /**
     * Called once, when the final main chapter of a story lands in the book.
     */
    public static void handleStoryCompletion(ServerPlayer player, ResourceLocation storyId) {
        player.displayClientMessage(
                Component.translatable("item.magic_realms.book.story_complete",
                                BossStory.titleComponent(storyId).withStyle(ChatFormatting.GOLD))
                        .withStyle(ChatFormatting.LIGHT_PURPLE), false);

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
