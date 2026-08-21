package net.alshanex.magic_realms.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.alshanex.magic_realms.data.TitleProgressData;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.util.humans.titles.Title;
import net.alshanex.magic_realms.util.humans.titles.TitleCatalogHolder;
import net.alshanex.magic_realms.util.humans.titles.TitleManager;
import net.alshanex.magic_realms.util.humans.titles.TitleRequirement;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.List;

/**
 * Admin/debug commands for the titles system. Replaces the old {@code /human addlevels}.
 *
 * <pre>
 * /human title grant    &lt;target&gt; &lt;title&gt;
 * /human title revoke   &lt;target&gt; &lt;title&gt;
 * /human title display  &lt;target&gt; &lt;title&gt;
 * /human title clear    &lt;target&gt;
 * /human title list     &lt;target&gt;
 * /human title progress &lt;target&gt;
 * </pre>
 */
public class HumanEntityCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("human")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("title")
                        .then(Commands.literal("grant")
                                .then(Commands.argument("target", EntityArgument.entity())
                                        .then(Commands.argument("title", ResourceLocationArgument.id())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                                        TitleCatalogHolder.server().ids(), builder))
                                                .executes(HumanEntityCommands::grantTitle))))
                        .then(Commands.literal("revoke")
                                .then(Commands.argument("target", EntityArgument.entity())
                                        .then(Commands.argument("title", ResourceLocationArgument.id())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                                        TitleCatalogHolder.server().ids(), builder))
                                                .executes(HumanEntityCommands::revokeTitle))))
                        .then(Commands.literal("display")
                                .then(Commands.argument("target", EntityArgument.entity())
                                        .then(Commands.argument("title", ResourceLocationArgument.id())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                                        TitleCatalogHolder.server().ids(), builder))
                                                .executes(HumanEntityCommands::displayTitle))))
                        .then(Commands.literal("clear")
                                .then(Commands.argument("target", EntityArgument.entity())
                                        .executes(HumanEntityCommands::clearDisplayedTitle)))
                        .then(Commands.literal("list")
                                .then(Commands.argument("target", EntityArgument.entity())
                                        .executes(HumanEntityCommands::listTitles)))
                        .then(Commands.literal("progress")
                                .then(Commands.argument("target", EntityArgument.entity())
                                        .executes(HumanEntityCommands::showProgress)))));
    }

    private static AbstractMercenaryEntity resolve(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        Entity target = EntityArgument.getEntity(context, "target");
        if (!(target instanceof AbstractMercenaryEntity mercenary)) {
            context.getSource().sendFailure(Component.literal("Target must be a mercenary."));
            return null;
        }
        return mercenary;
    }

    private static int grantTitle(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        AbstractMercenaryEntity mercenary = resolve(context);
        if (mercenary == null) return 0;

        ResourceLocation id = ResourceLocationArgument.getId(context, "title");
        if (!TitleCatalogHolder.server().contains(id)) {
            context.getSource().sendFailure(Component.literal("Unknown title: " + id));
            return 0;
        }

        if (!TitleManager.grant(mercenary, id, true)) {
            context.getSource().sendFailure(Component.literal(
                    mercenary.getEntityName() + " already holds " + id));
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.literal(
                "Granted " + id + " to " + mercenary.getEntityName()), true);
        return 1;
    }

    private static int revokeTitle(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        AbstractMercenaryEntity mercenary = resolve(context);
        if (mercenary == null) return 0;

        ResourceLocation id = ResourceLocationArgument.getId(context, "title");
        if (!TitleManager.revoke(mercenary, id)) {
            context.getSource().sendFailure(Component.literal(
                    mercenary.getEntityName() + " does not hold " + id));
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.literal(
                "Revoked " + id + " from " + mercenary.getEntityName()), true);
        return 1;
    }

    private static int displayTitle(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        AbstractMercenaryEntity mercenary = resolve(context);
        if (mercenary == null) return 0;

        ResourceLocation id = ResourceLocationArgument.getId(context, "title");
        if (!TitleManager.progress(mercenary).hasTitle(id)) {
            context.getSource().sendFailure(Component.literal(
                    mercenary.getEntityName() + " has not earned " + id));
            return 0;
        }

        TitleManager.setDisplayedTitle(mercenary, id);
        context.getSource().sendSuccess(() -> Component.literal(
                mercenary.getEntityName() + " is now displaying " + id), true);
        return 1;
    }

    private static int clearDisplayedTitle(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        AbstractMercenaryEntity mercenary = resolve(context);
        if (mercenary == null) return 0;

        TitleManager.setDisplayedTitle(mercenary, null);
        context.getSource().sendSuccess(() -> Component.literal(
                "Cleared the displayed title for " + mercenary.getEntityName()), true);
        return 1;
    }

    private static int listTitles(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        AbstractMercenaryEntity mercenary = resolve(context);
        if (mercenary == null) return 0;

        List<Title> earned = TitleManager.earnedTitles(mercenary);
        if (earned.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal(
                    mercenary.getEntityName() + " has no titles yet."), false);
            return 1;
        }

        Title displayed = TitleManager.displayedTitle(mercenary);
        context.getSource().sendSuccess(() -> Component.literal(
                        mercenary.getEntityName() + " holds " + earned.size() + " title(s):")
                .withStyle(ChatFormatting.GOLD), false);

        for (Title title : earned) {
            boolean shown = displayed != null && displayed.id().equals(title.id());
            context.getSource().sendSuccess(() -> Component.literal(shown ? " > " : "   ")
                    .append(title.displayComponent())
                    .append(Component.literal(" (" + title.id() + ")").withStyle(ChatFormatting.DARK_GRAY)), false);
        }
        return 1;
    }

    private static int showProgress(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        AbstractMercenaryEntity mercenary = resolve(context);
        if (mercenary == null) return 0;

        TitleProgressData data = TitleManager.progress(mercenary);

        context.getSource().sendSuccess(() -> Component.literal(
                        "Unearned title progress for " + mercenary.getEntityName() + ":")
                .withStyle(ChatFormatting.GOLD), false);

        boolean any = false;
        for (Title title : TitleCatalogHolder.server().all()) {
            if (data.hasTitle(title.id()) || title.hidden()) continue;
            any = true;

            int percent = Math.round(title.progressFor(mercenary, data) * 100.0f);
            context.getSource().sendSuccess(() -> Component.literal("   ")
                    .append(title.displayComponent())
                    .append(Component.literal(" - " + percent + "%").withStyle(ChatFormatting.GRAY)), false);

            for (TitleRequirement req : title.requirements()) {
                long current = req.currentValue(mercenary, data);
                long goal = Math.max(1L, req.amount());
                context.getSource().sendSuccess(() -> Component.literal(
                                "      " + req.type().name().toLowerCase()
                                        + (req.target().isEmpty() ? "" : " " + req.target())
                                        + ": " + current + "/" + goal)
                        .withStyle(ChatFormatting.DARK_GRAY), false);
            }
        }

        if (!any) {
            context.getSource().sendSuccess(() -> Component.literal(
                    "   (nothing left to earn)").withStyle(ChatFormatting.DARK_GRAY), false);
        }
        return 1;
    }
}