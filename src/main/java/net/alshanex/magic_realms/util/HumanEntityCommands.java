package net.alshanex.magic_realms.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.network.particles.ShockwaveParticlesPacket;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import io.redspace.ironsspellbooks.registries.ParticleRegistry;
import net.alshanex.magic_realms.Config;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.KillTrackerData;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.entity.random.RandomHumanEntity;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.util.humans.LevelingStatsManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public class HumanEntityCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("human")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("addlevels")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .then(Commands.argument("levels", IntegerArgumentType.integer(1, 100))
                                        .executes(HumanEntityCommands::addLevels)))));
    }

    private static int addLevels(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Entity target = EntityArgument.getEntity(context, "target");
        int levelsToAdd = IntegerArgumentType.getInteger(context, "levels");
        CommandSourceStack source = context.getSource();

        if (!(target instanceof AbstractMercenaryEntity humanEntity)) {
            source.sendFailure(Component.literal("Target must be a AbstractMercenaryEntity!"));
            return 0;
        }

        try {
            KillTrackerData killData = humanEntity.getData(MRDataAttachments.KILL_TRACKER);
            int currentLevel = killData.getCurrentLevel();
            int maxLevel = Config.maxLevel;

            // Calculate how many levels we can actually add
            int actualLevelsToAdd = Math.min(levelsToAdd, maxLevel - currentLevel);

            if (actualLevelsToAdd <= 0) {
                source.sendFailure(Component.literal(
                        String.format("%s is already at maximum level (%d)!",
                                humanEntity.getEntityName(), currentLevel)));
                return 0;
            }

            // Add levels one by one to trigger proper level-up effects
            for (int i = 0; i < actualLevelsToAdd; i++) {
                int newLevel = currentLevel + i + 1;

                // Set the level (this sets experience to the required amount for that level)
                killData.setLevel(newLevel);

                // Apply level-based attribute bonuses
                LevelingStatsManager.applyLevelBasedAttributes(humanEntity, newLevel);
            }

            spawnLevelUpEffects(humanEntity);

            // Update the entity's name to reflect the new level
            humanEntity.updateCustomNameWithStars();

            String message = String.format("Added %d level%s to %s (Level %d -> %d)",
                    actualLevelsToAdd,
                    actualLevelsToAdd == 1 ? "" : "s",
                    humanEntity.getEntityName(),
                    currentLevel,
                    killData.getCurrentLevel());

            source.sendSuccess(() -> Component.literal(message), true);

            if (actualLevelsToAdd < levelsToAdd) {
                source.sendSuccess(() -> Component.literal(
                        String.format("Note: Only %d level%s could be added (reached maximum level)",
                                actualLevelsToAdd, actualLevelsToAdd == 1 ? "" : "s")), false);
            }

            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to add levels: " + e.getMessage()));
            return 0;
        }
    }

    private static void spawnLevelUpEffects(AbstractMercenaryEntity entity) {
        if (entity.level().isClientSide) return;

        try {
            MagicManager.spawnParticles(entity.level(), new BlastwaveParticleOptions(SchoolRegistry.HOLY.get().getTargetingColor(), 4), entity.getX(), entity.getY() + .165f, entity.getZ(), 1, 0, 0, 0, 0, true);
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, new ShockwaveParticlesPacket(new Vec3(entity.getX(), entity.getY() + .165f, entity.getZ()), 4, ParticleRegistry.CLEANSE_PARTICLE.get()));

            entity.playSound(
                    net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
                    0.8F,
                    1.2F
            );
        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to spawn level up effects: {}", e.getMessage());
        }
    }
}
