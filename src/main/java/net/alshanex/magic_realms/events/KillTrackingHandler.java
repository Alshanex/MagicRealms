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
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.network.PacketDistributor;

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
        }

        MagicRealms.LOGGER.debug("Added {} experience to entity {} (Level: {})",
                experience, entity.getEntityName(), newLevel);
    }
}

