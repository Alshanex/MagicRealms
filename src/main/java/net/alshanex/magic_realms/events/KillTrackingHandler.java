package net.alshanex.magic_realms.events;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.network.particles.ShockwaveParticlesPacket;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import io.redspace.ironsspellbooks.registries.ParticleRegistry;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.KillTrackerData;
import net.alshanex.magic_realms.entity.RandomHumanEntity;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.util.humans.LevelingStatsManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

@EventBusSubscriber(modid = MagicRealms.MODID, bus = EventBusSubscriber.Bus.GAME)
public class KillTrackingHandler {

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof RandomHumanEntity humanEntity)) {
            return;
        }

        LivingEntity victim = event.getEntity();
        KillTrackerData killData = humanEntity.getData(MRDataAttachments.KILL_TRACKER);

        int previousLevel = killData.getCurrentLevel();
        killData.addKill(victim);
        int newLevel = killData.getCurrentLevel();

        MagicRealms.LOGGER.debug("Entity {} killed {} | Total kills: {} | Current EXP: {} | EXP to next: {}",
                humanEntity.getEntityName(),
                victim.getType().getDescription().getString(),
                killData.getTotalKills(),
                killData.getExperiencePoints(),
                killData.getExperienceToNextLevel());

        if (newLevel > previousLevel) {
            spawnLevelUpEffects(humanEntity);
            humanEntity.updateCustomNameWithStars();
            LevelingStatsManager.applyLevelBasedAttributes(humanEntity, newLevel);

            MagicRealms.LOGGER.info("Entity {} leveled up! Level {} -> {} (Total kills: {})",
                    humanEntity.getEntityName(),
                    previousLevel,
                    newLevel,
                    killData.getTotalKills());
        }
    }

    private static void spawnLevelUpEffects(RandomHumanEntity entity) {
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

    public static KillTrackerData getKillData(RandomHumanEntity entity) {
        return entity.getData(MRDataAttachments.KILL_TRACKER);
    }

    public static void setEntityLevel(RandomHumanEntity entity, int level) {
        KillTrackerData killData = entity.getData(MRDataAttachments.KILL_TRACKER);
        killData.setLevel(level);

        MagicRealms.LOGGER.info("Manually set entity {} to level {}", entity.getEntityName(), level);
    }

    public static void addExperience(RandomHumanEntity entity, int experience) {
        KillTrackerData killData = entity.getData(MRDataAttachments.KILL_TRACKER);
        int previousLevel = killData.getCurrentLevel();
        killData.addExperience(experience);
        int newLevel = killData.getCurrentLevel();

        if (newLevel > previousLevel) {
            spawnLevelUpEffects(entity);
            entity.updateCustomNameWithStars();
        }

        MagicRealms.LOGGER.debug("Added {} experience to entity {} (Level: {})",
                experience, entity.getEntityName(), newLevel);
    }

    private static final double SHARED_XP_MULTIPLIER = 0.5; // 50% of normal XP
    private static final double MAX_DISTANCE_SQR = 64 * 64; // 64 block radius

    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        // Only process on server side
        if (event.getEntity().level().isClientSide) {
            return;
        }

        LivingEntity deadEntity = event.getEntity();

        // Check if the killer is a player
        if (!(deadEntity.getLastHurtByMob() instanceof Player killer)) {
            return;
        }

        // Don't give XP for other RandomHumanEntities
        if (deadEntity instanceof RandomHumanEntity) {
            return;
        }

        // Find all RandomHumanEntity companions owned by this player within range
        List<RandomHumanEntity> companions = findNearbyCompanions(killer, deadEntity);

        if (companions.isEmpty()) {
            return;
        }

        // Calculate base XP that would be gained (simulate the kill)
        int baseXp = calculateBaseExperience(deadEntity);

        if (baseXp <= 0) {
            return;
        }

        // Calculate shared XP (50% of what they would have gained)
        int sharedXp = Math.max(1, (int) (baseXp * SHARED_XP_MULTIPLIER));

        // Distribute XP to all companions
        for (RandomHumanEntity companion : companions) {
            giveSharedExperience(companion, deadEntity, sharedXp, killer);
        }

        MagicRealms.LOGGER.debug("Distributed {} shared XP to {} companions of player {} for killing {}",
                sharedXp, companions.size(), killer.getName().getString(), deadEntity.getType().getDescriptionId());
    }

    private static List<RandomHumanEntity> findNearbyCompanions(Player owner, LivingEntity deadEntity) {
        return deadEntity.level().getEntitiesOfClass(RandomHumanEntity.class,
                deadEntity.getBoundingBox().inflate(64.0), // 64 block radius
                companion -> {
                    // Check if this companion is owned by the player
                    LivingEntity summoner = companion.getSummoner();
                    if(summoner == null){
                        return false;
                    }
                    if (summoner != owner) {
                        return false;
                    }

                    // Check distance
                    double distanceSqr = companion.distanceToSqr(deadEntity);
                    return distanceSqr <= MAX_DISTANCE_SQR;
                });
    }

    private static int calculateBaseExperience(LivingEntity entity) {
        try {
            if (entity instanceof Player) {
                return 42; // Would never reach here due to earlier check, but kept for completeness
            }

            if (!(entity instanceof net.minecraft.world.entity.monster.Monster)) {
                return 0; // No XP for non-monsters
            }

            float maxHealth = entity.getMaxHealth();
            double baseExp = Math.sqrt(maxHealth) * 3.0;
            int finalExp = (int) Math.round(baseExp);

            return Math.max(1, Math.min(finalExp, 100));

        } catch (Exception e) {
            if (entity instanceof net.minecraft.world.entity.monster.Monster) {
                return Math.max(1, Math.min((int) (entity.getMaxHealth() / 5), 20));
            }
            return 0;
        }
    }

    private static void giveSharedExperience(RandomHumanEntity companion, LivingEntity killedEntity,
                                             int sharedXp, Player owner) {
        KillTrackerData killTracker = companion.getData(MRDataAttachments.KILL_TRACKER);

        // Store previous level to check for level up
        int previousLevel = killTracker.getCurrentLevel();

        // Add the shared experience (using the existing method but with our calculated XP)
        killTracker.addExperience(sharedXp);

        // Check if leveled up and apply attribute bonuses
        int newLevel = killTracker.getCurrentLevel();
        if (newLevel > previousLevel) {
            // Apply level-based attribute bonuses
            LevelingStatsManager.applyLevelBasedAttributes(companion, newLevel);

            // Update the custom name
            companion.updateCustomNameWithStars();

            spawnLevelUpEffects(companion);
        }
    }
}

