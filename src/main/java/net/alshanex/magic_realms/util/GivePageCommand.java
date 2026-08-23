package net.alshanex.magic_realms.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.alshanex.magic_realms.item.lore.LostPageItem;
import net.alshanex.magic_realms.registry.MRItems;
import net.alshanex.magic_realms.registry.MRRegistries;
import net.alshanex.magic_realms.story.BossStory;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Debug/testing command to give Lost Pages to players.
 * <p>
 * Usage: /lostpages givepage <story_id> <chapter>
 * Example: /lostpages givepage lost_pages:the_frozen_king 0
 */
public class GivePageCommand {

    private static final SuggestionProvider<CommandSourceStack> STORY_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggestResource(
                MRRegistries.BOSS_STORIES.entrySet().stream()
                        .map(entry -> entry.getKey().location()),
                builder
        );
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("lostpages")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("givepage")
                        .then(Commands.argument("story", ResourceLocationArgument.id())
                                .suggests(STORY_SUGGESTIONS)
                                .then(Commands.argument("chapter", IntegerArgumentType.integer(0))
                                        .executes(context -> {
                                            ResourceLocation storyId = ResourceLocationArgument.getId(context, "story");
                                            int chapter = IntegerArgumentType.getInteger(context, "chapter");
                                            return givePage(context.getSource(), storyId, chapter);
                                        })
                                )
                        )
                )
                .then(Commands.literal("givebook")
                        .executes(context -> giveBook(context.getSource()))
                )
                .then(Commands.literal("giveallpages")
                        .then(Commands.argument("story", ResourceLocationArgument.id())
                                .suggests(STORY_SUGGESTIONS)
                                .executes(context -> {
                                    ResourceLocation storyId = ResourceLocationArgument.getId(context, "story");
                                    return giveAllPages(context.getSource(), storyId);
                                })
                        )
                )
        );
    }

    private static int givePage(CommandSourceStack source, ResourceLocation storyId, int chapter) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Command must be run by a player"));
            return 0;
        }

        BossStory story = MRRegistries.BOSS_STORIES.get(storyId);
        if (story == null) {
            source.sendFailure(Component.literal("Unknown story: " + storyId));
            return 0;
        }

        if (!story.isValidChapter(chapter)) {
            source.sendFailure(Component.literal("Invalid chapter " + chapter + " for " + story.bossEntity()
                    + " (has " + story.totalPages() + " chapters, 0-indexed)"));
            return 0;
        }

        ItemStack pageStack = LostPageItem.createPage(MRItems.LOST_PAGE.get(), storyId, chapter);
        if (!player.getInventory().add(pageStack)) {
            player.drop(pageStack, false);
        }

        source.sendSuccess(() -> Component.literal("Gave page '" + chapter
                + "' from '" + story.bossEntity() + "' to " + player.getName().getString()), true);
        return 1;
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

    private static int giveAllPages(CommandSourceStack source, ResourceLocation storyId) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Command must be run by a player"));
            return 0;
        }

        BossStory story = MRRegistries.BOSS_STORIES.get(storyId);
        if (story == null) {
            source.sendFailure(Component.literal("Unknown story: " + storyId));
            return 0;
        }

        int given = 0;
        for (int i = 0; i < story.totalPages(); i++) {
            ItemStack pageStack = LostPageItem.createPage(MRItems.LOST_PAGE.get(), storyId, i);
            if (!player.getInventory().add(pageStack)) {
                player.drop(pageStack, false);
            }
            given++;
        }

        int finalGiven = given;
        source.sendSuccess(() -> Component.literal("Gave all " + finalGiven + " pages of '"
                + story.bossEntity() + "' to " + player.getName().getString()), true);
        return 1;
    }
}