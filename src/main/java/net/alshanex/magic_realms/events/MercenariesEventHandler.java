package net.alshanex.magic_realms.events;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss.FireBossEntity;
import io.redspace.ironsspellbooks.entity.spells.devour_jaw.DevourJaw;
import io.redspace.ironsspellbooks.network.particles.ShockwaveParticlesPacket;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import io.redspace.ironsspellbooks.registries.ParticleRegistry;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.KillTrackerData;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.entity.exclusive.aliana.AlianaEntity;
import net.alshanex.magic_realms.entity.exclusive.amadeus.AmadeusEntity;
import net.alshanex.magic_realms.entity.exclusive.gojo_mojo.GojoMojoEntity;
import net.alshanex.magic_realms.entity.exclusive.jara.JaraEntity;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.registry.MRItems;
import net.alshanex.magic_realms.util.ModTags;
import net.alshanex.magic_realms.util.humans.mercenaries.chat.MercenaryMessageFormatter;
import net.alshanex.magic_realms.util.humans.stats.LevelingStatsManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

@EventBusSubscriber(modid = MagicRealms.MODID, bus = EventBusSubscriber.Bus.GAME)
public class MercenariesEventHandler {
    @SubscribeEvent
    public static void amadeusCheerEvent(LivingDamageEvent.Pre event){
        if(event.getEntity() instanceof Player player && !player.level().isClientSide && (player.getHealth() - event.getOriginalDamage()) < 6){
            if(hasContractedAmadeusNearby(player, player.level())){
                AmadeusEntity amadeus = hasContractedAmadeusNearby(player.blockPosition(), player.level());
                player.sendSystemMessage(
                        MercenaryMessageFormatter.buildTwoNamed(amadeus, "Amadeus Voidwalker",
                                player.getDisplayName(),
                                "message.magic_realms.amadeus.combat.contractor_low_health")
                );
            }
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(EntityJoinLevelEvent event){
        if(event.getEntity() instanceof AlianaEntity aliana && !aliana.level().isClientSide && event.getLevel().dimension() == Level.NETHER){
            if(aliana.getSummoner() != null && hasContractorNearby(aliana, aliana.getSummoner(), aliana.level())){
                aliana.getSummoner().sendSystemMessage(
                        MercenaryMessageFormatter.buildFor(aliana, "message.magic_realms.aliana.travel.nether"));
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

    private static GojoMojoEntity hasContractedGojoMojoNearby(BlockPos pos, Level level) {
        double SEARCH_RADIUS = 20.0;
        AABB searchArea = new AABB(
                pos.getX() - SEARCH_RADIUS,
                pos.getY() - SEARCH_RADIUS,
                pos.getZ() - SEARCH_RADIUS,
                pos.getX() + SEARCH_RADIUS,
                pos.getY() + SEARCH_RADIUS,
                pos.getZ() + SEARCH_RADIUS
        );

        List<GojoMojoEntity> nearbyGojoMojo = level.getEntitiesOfClass(
                GojoMojoEntity.class,
                searchArea,
                gojoMojoEntity -> gojoMojoEntity.getSummoner() != null
        );

        if(nearbyGojoMojo.size() == 1){
            return nearbyGojoMojo.getFirst();
        }

        return null;
    }

    private static boolean hasContractorNearby(AbstractMercenaryEntity mercenary, LivingEntity entity, Level level) {
        double SEARCH_RADIUS = 20.0;
        AABB searchArea = new AABB(
                mercenary.getX() - SEARCH_RADIUS,
                mercenary.getY() - SEARCH_RADIUS,
                mercenary.getZ() - SEARCH_RADIUS,
                mercenary.getX() + SEARCH_RADIUS,
                mercenary.getY() + SEARCH_RADIUS,
                mercenary.getZ() + SEARCH_RADIUS
        );

        List<Player> nearbyContractor = level.getEntitiesOfClass(
                Player.class,
                searchArea,
                player1 -> player1.is(entity)
        );

        return !nearbyContractor.isEmpty();
    }

    private static JaraEntity hasContractedJaraNearby(BlockPos pos, Level level) {
        double SEARCH_RADIUS = 20.0;
        AABB searchArea = new AABB(
                pos.getX() - SEARCH_RADIUS,
                pos.getY() - SEARCH_RADIUS,
                pos.getZ() - SEARCH_RADIUS,
                pos.getX() + SEARCH_RADIUS,
                pos.getY() + SEARCH_RADIUS,
                pos.getZ() + SEARCH_RADIUS
        );

        List<JaraEntity> nearbyJara = level.getEntitiesOfClass(
                JaraEntity.class,
                searchArea,
                jaraEntity -> jaraEntity.getSummoner() != null
        );

        if(nearbyJara.size() == 1){
            return nearbyJara.getFirst();
        }

        return null;
    }

    @SubscribeEvent
    public static void onLivingEntityDeath(LivingDeathEvent event){
        if(event.getEntity() instanceof EnderMan enderman && !enderman.level().isClientSide && event.getSource().getEntity() instanceof Player player){
            AmadeusEntity amadeus = hasContractedAmadeusNearby(enderman.blockPosition(), enderman.level());
            if(amadeus != null && amadeus.getSummoner().is(player) && hasContractorNearby(amadeus, player, amadeus.level())){
                player.sendSystemMessage(
                        MercenaryMessageFormatter.buildFor(amadeus, "message.magic_realms.amadeus.enderman.killed"));
            }
        }

        if(event.getEntity().getType().is(ModTags.BOSSES_TAG) && !event.getEntity().level().isClientSide){
            AmadeusEntity amadeus = hasContractedAmadeusNearby(event.getEntity().blockPosition(), event.getEntity().level());
            if(amadeus != null && !amadeus.level().isClientSide && amadeus.getSummoner() != null && hasContractorNearby(amadeus, amadeus.getSummoner(), amadeus.level())){
                amadeus.getSummoner().sendSystemMessage(MercenaryMessageFormatter.buildFor(amadeus, "message.magic_realms.amadeus.boss.killed"));
            }

            GojoMojoEntity gojoMojo = hasContractedGojoMojoNearby(event.getEntity().blockPosition(), event.getEntity().level());
            if(gojoMojo != null && !gojoMojo.level().isClientSide && gojoMojo.getSummoner() != null&& hasContractorNearby(gojoMojo, gojoMojo.getSummoner(), gojoMojo.level())){
                gojoMojo.getSummoner().sendSystemMessage(MercenaryMessageFormatter.buildFor(amadeus, "message.magic_realms.gojo_mojo.special_phrase.2"));
            }
        }

        if(event.getEntity() instanceof AbstractMercenaryEntity mercenary && !mercenary.isImmortal() && !mercenary.level().isClientSide){
            AmadeusEntity amadeus = hasContractedAmadeusNearby(mercenary.blockPosition(), mercenary.level());
            if(amadeus != null && !amadeus.level().isClientSide && amadeus.getSummoner() != null && amadeus.isAlliedTo(mercenary) && hasContractorNearby(amadeus, amadeus.getSummoner(), amadeus.level())){
                amadeus.getSummoner().sendSystemMessage(MercenaryMessageFormatter.buildFor(amadeus, "message.magic_realms.amadeus.ally.killed"));
            }
        }

        if(event.getEntity() instanceof FireBossEntity tyros && !tyros.level().isClientSide && event.getSource().getEntity() instanceof Player player){
            JaraEntity jara = hasContractedJaraNearby(tyros.blockPosition(), tyros.level());
            if(jara != null && jara.getSummoner().is(player) && hasContractorNearby(jara, player, jara.level())){
                player.sendSystemMessage(
                        MercenaryMessageFormatter.buildFor(jara, "message.magic_realms.jara.special_phrase.1"));
            }
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity attacker = event.getSource().getEntity();

        if (!(attacker instanceof GojoMojoEntity gojoMojoEntity)) return;

        ItemStack weapon = gojoMojoEntity.getMainHandItem();
        if (weapon.is(Items.MACE)) {
            event.setAmount(event.getAmount() * 1.15f);
        }
    }
}

