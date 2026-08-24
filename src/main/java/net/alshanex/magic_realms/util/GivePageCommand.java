package net.alshanex.magic_realms.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.alshanex.magic_realms.registry.MRItems;
import net.alshanex.magic_realms.registry.MRRegistries;
import net.alshanex.magic_realms.story.BossStory;
import net.alshanex.magic_realms.story.ChapterUnlocker;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Debug/testing commands for the lore book.
 * <p>
 * Chapters are granted straight into the player's progress attachment
 * <pre>
 * /lostpages unlock &lt;story&gt; &lt;chapter&gt; [force]
 * /lostpages unlockall &lt;story&gt; [force]
 * /lostpages givebook
 * </pre>
 * Without {@code force}, unlocks go through the same requirement checks as inserting a page - useful for verifying that gating works.
 * With {@code force}, the book seal and prerequisites are ignored.
 */
public class GivePageCommand {

    private static final SuggestionProvider<CommandSourceStack> STORY_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggestResource(
                    MRRegistries.BOSS_STORIES.entrySet().stream()
                            .map(entry -> entry.getKey().location()),
                    builder
            );

    /**
     * Suggests the valid chapter indices for whichever story was typed.
     */
    private static final SuggestionProvider<CommandSourceStack> CHAPTER_SUGGESTIONS = (context, builder) -> {
        try {
            BossStory story = MRRegistries.BOSS_STORIES.get(ResourceLocationArgument.getId(context, "story"));
            if (story != null) {
                for (int i = 0; i < story.totalChapters(); i++) {
                    builder.suggest(i);
                }
            }
        } catch (Exception ignored) {
            // Story argument incomplete or unknown; no suggestions to give.
        }
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("lostpages")
                .requires(source -> source.hasPermission(2))

                .then(Commands.literal("unlock")
                        .then(Commands.argument("story", ResourceLocationArgument.id())
                                .suggests(STORY_SUGGESTIONS)
                                .then(Commands.argument("chapter", IntegerArgumentType.integer(0))
                                        .suggests(CHAPTER_SUGGESTIONS)
                                        .executes(context -> unlock(context.getSource(),
                                                ResourceLocationArgument.getId(context, "story"),
                                                IntegerArgumentType.getInteger(context, "chapter"),
                                                false))
                                        .then(Commands.argument("force", BoolArgumentType.bool())
                                                .executes(context -> unlock(context.getSource(),
                                                        ResourceLocationArgument.getId(context, "story"),
                                                        IntegerArgumentType.getInteger(context, "chapter"),
                                                        BoolArgumentType.getBool(context, "force")))
                                        )
                                )
                        )
                )

                .then(Commands.literal("unlockall")
                        .then(Commands.argument("story", ResourceLocationArgument.id())
                                .suggests(STORY_SUGGESTIONS)
                                .executes(context -> unlockAll(context.getSource(),
                                        ResourceLocationArgument.getId(context, "story"), false))
                                .then(Commands.argument("force", BoolArgumentType.bool())
                                        .executes(context -> unlockAll(context.getSource(),
                                                ResourceLocationArgument.getId(context, "story"),
                                                BoolArgumentType.getBool(context, "force")))
                                )
                        )
                )

                .then(Commands.literal("givebook")
                        .executes(context -> giveBook(context.getSource()))
                )
        );
    }

    // Commands

    private static int unlock(CommandSourceStack source, ResourceLocation storyId, int chapter, boolean force) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Command must be run by a player"));
            return 0;
        }

        BossStory story = resolveStory(source, storyId);
        if (story == null) return 0;

        if (!story.isValidChapter(chapter)) {
            source.sendFailure(Component.literal("Invalid chapter " + chapter + " for " + story.bossEntity()
                    + " (has " + story.totalChapters() + " chapters, 0-indexed)"));
            return 0;
        }

        ChapterUnlocker.Result result = grant(player, storyId, chapter, force);

        if (!result.isUnlocked()) {
            source.sendFailure(Component.literal("Could not unlock chapter " + chapter + ": " + reason(result)));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Unlocked chapter " + chapter
                + " of '" + story.bossEntity() + "' for " + player.getName().getString()), true);
        return 1;
    }

    private static int unlockAll(CommandSourceStack source, ResourceLocation storyId, boolean force) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Command must be run by a player"));
            return 0;
        }

        BossStory story = resolveStory(source, storyId);
        if (story == null) return 0;

        int unlocked = 0;
        int skipped = 0;
        ChapterUnlocker.Result lastFailure = null;

        // Ascending order matters even without force: granting chapter N satisfies chapter N+1's prerequisite,
        // so a whole story resolves in one pass without needing the override.
        for (int i = 0; i < story.totalChapters(); i++) {
            ChapterUnlocker.Result result = grant(player, storyId, i, force);

            if (result.isUnlocked()) {
                unlocked++;
            } else {
                skipped++;
                if (result != ChapterUnlocker.Result.ALREADY_OWNED) lastFailure = result;
            }
        }

        if (unlocked == 0 && lastFailure != null) {
            source.sendFailure(Component.literal("Unlocked nothing for '" + story.bossEntity()
                    + "': " + reason(lastFailure)));
            return 0;
        }

        int finalUnlocked = unlocked;
        int finalSkipped = skipped;
        source.sendSuccess(() -> Component.literal("Unlocked " + finalUnlocked + " chapter(s) of '"
                + story.bossEntity() + "' for " + player.getName().getString()
                + (finalSkipped > 0 ? " (" + finalSkipped + " skipped)" : "")), true);
        return finalUnlocked;
    }

    private static int giveBook(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Command must be run by a player"));
            return 0;
        }

        ItemStack bookStack = new ItemStack(MRItems.LOST_PAGES_BOOK.get());
        if (!player.getInventory().add(bookStack)) {
            player.drop(bookStack, false);
        }

        source.sendSuccess(() -> Component.literal("Gave Lost Pages Book to " + player.getName().getString()), true);
        return 1;
    }

    // Helpers

    private static ChapterUnlocker.Result grant(ServerPlayer player, ResourceLocation storyId,
                                                int chapter, boolean force) {
        return force
                ? ChapterUnlocker.grantUnchecked(player, storyId, chapter, null, SoundEvents.BOOK_PAGE_TURN)
                : ChapterUnlocker.tryUnlock(player, storyId, chapter, null, SoundEvents.BOOK_PAGE_TURN);
    }

    @Nullable
    private static BossStory resolveStory(CommandSourceStack source, ResourceLocation storyId) {
        BossStory story = MRRegistries.BOSS_STORIES.get(storyId);
        if (story == null) {
            source.sendFailure(Component.literal("Unknown story: " + storyId));
        }
        return story;
    }

    private static String reason(ChapterUnlocker.Result result) {
        return switch (result) {
            case UNLOCKED -> "unlocked";
            case ALREADY_OWNED -> "already unlocked";
            case MISSING_PREREQUISITES -> "prerequisites not met (pass true to force)";
            case BOOK_SEALED -> "the book is sealed - world lore IV not found (pass true to force)";
            case INVALID_CHAPTER -> "invalid story or chapter index";
        };
    }
}