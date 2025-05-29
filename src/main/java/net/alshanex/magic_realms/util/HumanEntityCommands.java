package net.alshanex.magic_realms.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.alshanex.magic_realms.data.KillTrackerData;
import net.alshanex.magic_realms.entity.RandomHumanEntity;
import net.alshanex.magic_realms.events.KillTrackingHandler;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

public class HumanEntityCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("human")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("setlevel")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .then(Commands.argument("level", IntegerArgumentType.integer(1, 300))
                                        .executes(HumanEntityCommands::setLevel))))
                .then(Commands.literal("addexp")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .then(Commands.argument("experience", IntegerArgumentType.integer(1, 10000))
                                        .executes(HumanEntityCommands::addExperience))))
                .then(Commands.literal("info")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(HumanEntityCommands::showInfo)))
                .then(Commands.literal("reset")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(HumanEntityCommands::resetLevel))));
    }

    private static int setLevel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Entity target = EntityArgument.getEntity(context, "target");
        int level = IntegerArgumentType.getInteger(context, "level");
        CommandSourceStack source = context.getSource();

        if (!(target instanceof RandomHumanEntity humanEntity)) {
            source.sendFailure(Component.literal("Target must be a RandomHumanEntity!"));
            return 0;
        }

        try {
            KillTrackingHandler.setEntityLevel(humanEntity, level);

            source.sendSuccess(() -> Component.literal(
                    String.format("Set level of %s to %d",
                            humanEntity.getEntityName(), level)), true);
            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to set level: " + e.getMessage()));
            return 0;
        }
    }

    private static int addExperience(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Entity target = EntityArgument.getEntity(context, "target");
        int experience = IntegerArgumentType.getInteger(context, "experience");
        CommandSourceStack source = context.getSource();

        if (!(target instanceof RandomHumanEntity humanEntity)) {
            source.sendFailure(Component.literal("Target must be a RandomHumanEntity!"));
            return 0;
        }

        try {
            KillTrackingHandler.addExperience(humanEntity, experience);

            String message = String.format("Added %d experience to %s", experience, humanEntity.getEntityName());

            source.sendSuccess(() -> Component.literal(message), true);
            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to add experience: " + e.getMessage()));
            return 0;
        }
    }

    private static int showInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Entity target = EntityArgument.getEntity(context, "target");
        CommandSourceStack source = context.getSource();

        if (!(target instanceof RandomHumanEntity humanEntity)) {
            source.sendFailure(Component.literal("Target must be a RandomHumanEntity!"));
            return 0;
        }

        try {
            KillTrackerData killData = humanEntity.getData(MRDataAttachments.KILL_TRACKER);

            source.sendSuccess(() -> Component.literal("=== Human Entity Info ==="), false);
            source.sendSuccess(() -> Component.literal("Name: " + humanEntity.getEntityName()), false);
            source.sendSuccess(() -> Component.literal("UUID: " + humanEntity.getUUID().toString()), false);
            source.sendSuccess(() -> Component.literal("Class: " + humanEntity.getEntityClass().getName()), false);
            source.sendSuccess(() -> Component.literal("Stars: " + humanEntity.getStarLevel()), false);
            source.sendSuccess(() -> Component.literal("Level: " + killData.getCurrentLevel()), false);
            source.sendSuccess(() -> Component.literal("Experience: " + killData.getExperiencePoints()), false);
            source.sendSuccess(() -> Component.literal("Total Kills: " + killData.getTotalKills()), false);
            source.sendSuccess(() -> Component.literal("Exp to Next: " + killData.getExperienceToNextLevel()), false);
            source.sendSuccess(() -> Component.literal("Progress: " + String.format("%.1f%%", killData.getProgressToNextLevel() * 100)), false);

            if (humanEntity.getEntityClass() == net.alshanex.magic_realms.util.humans.EntityClass.ROGUE) {
                source.sendSuccess(() -> Component.literal("Subclass: " + (humanEntity.isArcher() ? "Archer" : "Assassin")), false);
            }

            if (humanEntity.getEntityClass() == net.alshanex.magic_realms.util.humans.EntityClass.MAGE) {
                String schools = humanEntity.getMagicSchools().stream()
                        .map(school -> school.getId().getPath())
                        .collect(java.util.stream.Collectors.joining(", "));
                source.sendSuccess(() -> Component.literal("Magic Schools: " + schools), false);
            }

            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to get info: " + e.getMessage()));
            return 0;
        }
    }

    private static int resetLevel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Entity target = EntityArgument.getEntity(context, "target");
        CommandSourceStack source = context.getSource();

        if (!(target instanceof RandomHumanEntity humanEntity)) {
            source.sendFailure(Component.literal("Target must be a RandomHumanEntity!"));
            return 0;
        }

        try {
            KillTrackerData killData = humanEntity.getData(MRDataAttachments.KILL_TRACKER);
            killData.reset();

            // Aplicar nivel 1
            KillTrackingHandler.setEntityLevel(humanEntity, 1);

            source.sendSuccess(() -> Component.literal(
                    String.format("Reset %s to level 1", humanEntity.getEntityName())), true);
            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to reset level: " + e.getMessage()));
            return 0;
        }
    }
}
