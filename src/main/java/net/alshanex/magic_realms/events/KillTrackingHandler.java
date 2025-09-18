package net.alshanex.magic_realms.events;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.entity.spells.devour_jaw.DevourJaw;
import io.redspace.ironsspellbooks.network.particles.ShockwaveParticlesPacket;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import io.redspace.ironsspellbooks.registries.ParticleRegistry;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.KillTrackerData;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.entity.random.RandomHumanEntity;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.registry.MRItems;
import net.alshanex.magic_realms.util.ModTags;
import net.alshanex.magic_realms.util.humans.LevelingStatsManager;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

@EventBusSubscriber(modid = MagicRealms.MODID, bus = EventBusSubscriber.Bus.GAME)
public class KillTrackingHandler {
    @SubscribeEvent
    public static void onHumanDeath(LivingDeathEvent event){
        if(!(event.getEntity() instanceof AbstractMercenaryEntity human && human.isImmortal())){
            return;
        }
        event.setCanceled(true);
        human.setHealth(human.getMaxHealth() / 2);
        knockbackAndStun(event.getSource(), human);
    }

    @SubscribeEvent
    public static void onBossDeath(LivingDeathEvent event){
        if(event.getEntity().getType().is(ModTags.BOSSES_TAG)){
            if(event.getSource().getEntity() instanceof Player player && event.getSource().getDirectEntity() instanceof DevourJaw jaw
                    && jaw.getOwner() != null && jaw.getOwner().is(player)){
                ItemStack bloodPact = new ItemStack(MRItems.BLOOD_PACT, 1);
                if(player.getItemBySlot(EquipmentSlot.OFFHAND).is(MRItems.CONTRACT_MASTER)){
                    player.getItemBySlot(EquipmentSlot.OFFHAND).shrink(1);
                    if(player.getItemBySlot(EquipmentSlot.OFFHAND).isEmpty()){
                        player.setItemSlot(EquipmentSlot.OFFHAND, bloodPact);
                    } else {
                        player.getInventory().add(bloodPact);
                    }
                }
            }
        }
    }

    private static void knockbackAndStun(DamageSource source, AbstractMercenaryEntity entity) {
        // Apply knockback effect
        if (source.getEntity() != null) {
            double knockbackStrength = 1.5;
            double deltaX = entity.getX() - source.getEntity().getX();
            double deltaZ = entity.getZ() - source.getEntity().getZ();
            double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

            if (distance > 0) {
                deltaX /= distance;
                deltaZ /= distance;

                entity.setDeltaMovement(entity.getDeltaMovement().add(
                        deltaX * knockbackStrength,
                        0.4, // Upward knockback
                        deltaZ * knockbackStrength
                ));
            }
        }

        // Set stunned state
        entity.setStunned(true);

        // Clear target and stop attacking
        entity.setTarget(null);

        // Play knockback sound
        entity.playSound(SoundEvents.PLAYER_HURT, 0.8F, 1.2F);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof AbstractMercenaryEntity humanEntity)) {
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
/*
            MagicRealms.LOGGER.info("Entity {} leveled up! Level {} -> {} (Total kills: {})",
                    humanEntity.getEntityName(),
                    previousLevel,
                    newLevel,
                    killData.getTotalKills());
 */
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

        // Don't give XP for other AbstractMercenaryEntity
        if (deadEntity instanceof AbstractMercenaryEntity) {
            return;
        }

        // Find all AbstractMercenaryEntity companions owned by this player within range
        List<AbstractMercenaryEntity> companions = findNearbyCompanions(killer, deadEntity);

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
        for (AbstractMercenaryEntity companion : companions) {
            giveSharedExperience(companion, deadEntity, sharedXp, killer);
        }

        //MagicRealms.LOGGER.debug("Distributed {} shared XP to {} companions of player {} for killing {}", sharedXp, companions.size(), killer.getName().getString(), deadEntity.getType().getDescriptionId());
    }

    private static List<AbstractMercenaryEntity> findNearbyCompanions(Player owner, LivingEntity deadEntity) {
        return deadEntity.level().getEntitiesOfClass(AbstractMercenaryEntity.class,
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

    private static void giveSharedExperience(AbstractMercenaryEntity companion, LivingEntity killedEntity,
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

