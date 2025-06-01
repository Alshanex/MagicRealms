package net.alshanex.magic_realms.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.KillTrackerData;
import net.alshanex.magic_realms.entity.RandomHumanEntity;
import net.alshanex.magic_realms.events.KillTrackingHandler;
import net.alshanex.magic_realms.item.HumanInfoItem;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.util.humans.*;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.fml.loading.FMLEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

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
                                .executes(HumanEntityCommands::resetLevel)))
                .then(Commands.literal("createitem")
                        .then(Commands.argument("summoner", EntityArgument.entity())
                                .executes(HumanEntityCommands::createRandomVirtualItem))));
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

    private static int createRandomVirtualItem(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof Player player)) {
            source.sendFailure(Component.literal("Only players can use this command!"));
            return 0;
        }

        try {
            Entity summoner = EntityArgument.getEntity(context, "summoner");

            if (!(summoner instanceof LivingEntity livingSummoner)) {
                source.sendFailure(Component.literal("Summoner must be a living entity!"));
                return 0;
            }

            RandomHumanEntity virtualEntity = new RandomHumanEntity(source.getLevel(), livingSummoner);

            initializeVirtualEntity(virtualEntity, source.getLevel());

            EntitySnapshot virtualSnapshot = EntitySnapshot.fromEntity(virtualEntity);

            ItemStack infoItem = HumanInfoItem.createVirtualItem(virtualSnapshot);

            if (player.getInventory().add(infoItem)) {
                source.sendSuccess(() -> Component.literal(
                        String.format("Created random virtual info item: %s %s %s (%d★)",
                                virtualSnapshot.gender.getName(),
                                virtualSnapshot.entityClass.getName(),
                                virtualSnapshot.entityName,
                                virtualSnapshot.starLevel)), true);
            } else {
                // Si el inventario está lleno, dropear el item
                player.drop(infoItem, false);
                source.sendSuccess(() -> Component.literal(
                        String.format("Created and dropped random virtual info item: %s %s %s (%d★)",
                                virtualSnapshot.gender.getName(),
                                virtualSnapshot.entityClass.getName(),
                                virtualSnapshot.entityName,
                                virtualSnapshot.starLevel)), true);
            }

            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to create random virtual info item: " + e.getMessage()));
            MagicRealms.LOGGER.error("Error creating random virtual item", e);
            return 0;
        }
    }

    private static void initializeVirtualEntity(RandomHumanEntity entity, Level level) {
        try {
            DifficultyInstance difficulty = level.getCurrentDifficultyAt(entity.blockPosition());

            entity.finalizeSpawn(
                    (ServerLevelAccessor) level,
                    difficulty,
                    MobSpawnType.COMMAND,
                    null
            );

            MagicRealms.LOGGER.debug("Successfully initialized virtual entity: {} {} {} ({}★)",
                    entity.getGender().getName(),
                    entity.getEntityClass().getName(),
                    entity.getEntityName(),
                    entity.getStarLevel());

        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to initialize virtual entity: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize virtual entity", e);
        }
    }
}
