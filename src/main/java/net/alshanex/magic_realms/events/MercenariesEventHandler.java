package net.alshanex.magic_realms.events;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.entity.spells.devour_jaw.DevourJaw;
import io.redspace.ironsspellbooks.network.particles.ShockwaveParticlesPacket;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import io.redspace.ironsspellbooks.registries.ParticleRegistry;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.KillTrackerData;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.entity.exclusive.amadeus.AmadeusEntity;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.registry.MRItems;
import net.alshanex.magic_realms.util.ModTags;
import net.alshanex.magic_realms.util.humans.stats.LevelingStatsManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

@EventBusSubscriber(modid = MagicRealms.MODID, bus = EventBusSubscriber.Bus.GAME)
public class MercenariesEventHandler {
    @SubscribeEvent
    public static void amadeusCheerEvent(LivingDamageEvent.Pre event){
        if(event.getEntity() instanceof Player player && !player.level().isClientSide && (player.getHealth() - event.getOriginalDamage()) < 6){
            if(hasContractedAmadeusNearby(player, player.level())){
                player.sendSystemMessage(Component.translatable("message.magic_realms.amadeus.combat.contractor_low_health", "Amadeus Voidwalker", player.getDisplayName()));
            }
        }
    }

    private static boolean hasContractedAmadeusNearby(LivingEntity entity, Level level) {
        double SEARCH_RADIUS = 20.0;
        AABB searchArea = new AABB(
                entity.getX() - SEARCH_RADIUS,
                entity.getY() - SEARCH_RADIUS,
                entity.getZ() - SEARCH_RADIUS,
                entity.getX() + SEARCH_RADIUS,
                entity.getY() + SEARCH_RADIUS,
                entity.getZ() + SEARCH_RADIUS
        );

        List<AmadeusEntity> nearbyAmadeus = level.getEntitiesOfClass(
                AmadeusEntity.class,
                searchArea,
                amadeusEntity -> amadeusEntity.getSummoner() != null && amadeusEntity.getSummoner().is(entity)
        );

        return !nearbyAmadeus.isEmpty();
    }

    private static AmadeusEntity hasContractedAmadeusNearby(BlockPos pos, Level level) {
        double SEARCH_RADIUS = 20.0;
        AABB searchArea = new AABB(
                pos.getX() - SEARCH_RADIUS,
                pos.getY() - SEARCH_RADIUS,
                pos.getZ() - SEARCH_RADIUS,
                pos.getX() + SEARCH_RADIUS,
                pos.getY() + SEARCH_RADIUS,
                pos.getZ() + SEARCH_RADIUS
        );

        List<AmadeusEntity> nearbyAmadeus = level.getEntitiesOfClass(
                AmadeusEntity.class,
                searchArea,
                amadeusEntity -> amadeusEntity.getSummoner() != null
        );

        if(nearbyAmadeus.size() == 1){
            return nearbyAmadeus.getFirst();
        }

        return null;
    }

    private static boolean hasContractorNearby(AmadeusEntity amadeus, LivingEntity entity, Level level) {
        double SEARCH_RADIUS = 20.0;
        AABB searchArea = new AABB(
                amadeus.getX() - SEARCH_RADIUS,
                amadeus.getY() - SEARCH_RADIUS,
                amadeus.getZ() - SEARCH_RADIUS,
                amadeus.getX() + SEARCH_RADIUS,
                amadeus.getY() + SEARCH_RADIUS,
                amadeus.getZ() + SEARCH_RADIUS
        );

        List<Player> nearbyContractor = level.getEntitiesOfClass(
                Player.class,
                searchArea,
                player1 -> player1.is(entity)
        );

        return !nearbyContractor.isEmpty();
    }

    @SubscribeEvent
    public static void onLivingEntityDeath(LivingDeathEvent event){
        if(event.getEntity() instanceof EnderMan enderman && !enderman.level().isClientSide && event.getSource().getEntity() instanceof Player player){
            AmadeusEntity amadeus = hasContractedAmadeusNearby(enderman.blockPosition(), enderman.level());
            if(amadeus != null && amadeus.getSummoner().is(player) && hasContractorNearby(amadeus, player, amadeus.level())){
                player.sendSystemMessage(Component.translatable("message.magic_realms.amadeus.enderman.killed", amadeus.getExclusiveMercenaryName()));
            }
        }

        if(event.getEntity().getType().is(ModTags.BOSSES_TAG) && !event.getEntity().level().isClientSide){
            AmadeusEntity amadeus = hasContractedAmadeusNearby(event.getEntity().blockPosition(), event.getEntity().level());
            if(amadeus != null && !amadeus.level().isClientSide && hasContractorNearby(amadeus, amadeus.getSummoner(), amadeus.level())){
                amadeus.getSummoner().sendSystemMessage(Component.translatable("message.magic_realms.amadeus.boss.killed", amadeus.getExclusiveMercenaryName()));
            }
        }

        if(event.getEntity() instanceof AbstractMercenaryEntity mercenary && !mercenary.isImmortal() && !mercenary.level().isClientSide){
            AmadeusEntity amadeus = hasContractedAmadeusNearby(mercenary.blockPosition(), mercenary.level());
            if(amadeus != null && !amadeus.level().isClientSide && amadeus.isAlliedTo(mercenary) && hasContractorNearby(amadeus, amadeus.getSummoner(), amadeus.level())){
                amadeus.getSummoner().sendSystemMessage(Component.translatable("message.magic_realms.amadeus.ally.killed", amadeus.getExclusiveMercenaryName()));
            }
        }
    }
}

