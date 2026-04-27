package net.alshanex.magic_realms.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.alshanex.magic_realms.entity.random.hostile.HostileRandomHumanEntity;
import net.alshanex.magic_realms.util.humans.bandits.BanditProfile;
import net.alshanex.magic_realms.util.humans.bandits.BanditProfileCatalogHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.CompletableFuture;

/**
 * Operator commands for spawning and inspecting bandit profiles.
 *
 * <p>Usage:
 * <ul>
 *     <li>{@code /bandit summon <profile_id>} — spawn at the executor's position.</li>
 *     <li>{@code /bandit summon <profile_id> <x> <y> <z>} — spawn at a specific position.</li>
 *     <li>{@code /bandit list} — list all loaded profile ids.</li>
 * </ul>
 */
public class BanditCommands {

    private static final SuggestionProvider<CommandSourceStack> PROFILE_ID_SUGGESTIONS =
            (ctx, builder) -> suggestProfileIds(builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bandit")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("summon")
                        .then(Commands.argument("profile_id", StringArgumentType.string())
                                .suggests(PROFILE_ID_SUGGESTIONS)
                                .executes(BanditCommands::summonAtSelf)
                                .then(Commands.argument("position", Vec3Argument.vec3())
                                        .executes(BanditCommands::summonAtPosition))))
                .then(Commands.literal("list")
                        .executes(BanditCommands::listProfiles)));
    }

    private static CompletableFuture<Suggestions> suggestProfileIds(SuggestionsBuilder builder) {
        for (BanditProfile p : BanditProfileCatalogHolder.server().all()) {
            builder.suggest("\"" + p.id() + "\"");
        }
        return builder.buildFuture();
    }

    private static int summonAtSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        Vec3 pos = source.getPosition();
        return doSummon(context, pos);
    }

    private static int summonAtPosition(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Vec3 pos = Vec3Argument.getVec3(context, "position");
        return doSummon(context, pos);
    }

    private static int doSummon(CommandContext<CommandSourceStack> context, Vec3 pos) {
        CommandSourceStack source = context.getSource();
        String profileId = StringArgumentType.getString(context, "profile_id");

        BanditProfile profile = BanditProfileCatalogHolder.server().byId(profileId);
        if (profile == null) {
            source.sendFailure(Component.literal("Unknown bandit profile: " + profileId)
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        if (!(source.getLevel() instanceof ServerLevel level)) {
            source.sendFailure(Component.literal("Cannot summon outside a server level"));
            return 0;
        }

        try {
            HostileRandomHumanEntity entity = new HostileRandomHumanEntity(level, profileId);
            entity.setPos(pos.x, pos.y, pos.z);
            entity.finalizeSpawn(level, level.getCurrentDifficultyAt(entity.blockPosition()),
                    MobSpawnType.COMMAND, null);
            level.addFreshEntity(entity);

            source.sendSuccess(() -> Component.literal(
                            String.format("Summoned bandit '%s' at %.1f, %.1f, %.1f",
                                    profileId, pos.x, pos.y, pos.z))
                    .withStyle(ChatFormatting.GREEN), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to summon bandit: " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    private static int listProfiles(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        var catalog = BanditProfileCatalogHolder.server();
        if (catalog.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No bandit profiles loaded.")
                    .withStyle(ChatFormatting.YELLOW), false);
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                        String.format("Loaded bandit profiles (%d):", catalog.size()))
                .withStyle(ChatFormatting.AQUA), false);
        for (BanditProfile p : catalog.all()) {
            source.sendSuccess(() -> Component.literal("  - " + p.id() +
                            (p.isMiniBoss() ? " [miniboss]" : ""))
                    .withStyle(ChatFormatting.GRAY), false);
        }
        return catalog.size();
    }
}
