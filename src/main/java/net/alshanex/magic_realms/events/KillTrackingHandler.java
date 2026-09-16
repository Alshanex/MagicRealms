package net.alshanex.magic_realms.events;

import io.redspace.ironsspellbooks.entity.spells.devour_jaw.DevourJaw;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.humans.AbstractMercenaryEntity;
import net.alshanex.magic_realms.entity.humans.MercenaryBowFakePlayer;
import net.alshanex.magic_realms.entity.slime.MagicSlimeEntity;
import net.alshanex.magic_realms.entity.tavernkeep.TavernKeeperEntity;
import net.alshanex.magic_realms.registry.MREntityRegistry;
import net.alshanex.magic_realms.registry.MRItems;
import net.alshanex.magic_realms.util.ModTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = MagicRealms.MODID, bus = EventBusSubscriber.Bus.GAME)
public class KillTrackingHandler {

    @SubscribeEvent
    public static void onSlimeHurt(LivingDamageEvent.Pre event){
        if(event.getEntity() instanceof MagicSlimeEntity slime){
            if(slime.getWeakSchool() != null && event.getSource().is(slime.getWeakSchool().getDamageType())){
                event.setNewDamage(event.getOriginalDamage() * 1.5f);
            } else {
                event.setNewDamage(event.getOriginalDamage() * 0.2f);
            }
        }
    }

    @SubscribeEvent
    public static void onProjectileDamage(LivingIncomingDamageEvent event) {
        Entity directEntity = event.getSource().getDirectEntity();
        if (!(directEntity instanceof Projectile arrow)) return;

        Entity shooter = arrow.getOwner();
        if (!(shooter instanceof AbstractMercenaryEntity merc)) return;

        LivingEntity victim = event.getEntity();

        // Never hurt our summoner
        if (merc.getSummoner() != null && victim.is(merc.getSummoner())) {
            event.setCanceled(true);
            return;
        }

        // Never hurt allied mercenaries (same summoner)
        if (victim instanceof AbstractMercenaryEntity other
                && merc.getSummoner() != null
                && other.getSummoner() != null
                && merc.getSummoner().is(other.getSummoner())) {
            event.setCanceled(true);
            return;
        }

        // Use the generic ally check for anything else (pets, etc.)
        if (merc.isAlliedTo(victim)) {
            event.setCanceled(true);
            return;
        }
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (!(projectile.getOwner() instanceof AbstractMercenaryEntity merc)) return;

        // Only care about entity hits (not block hits)
        if (!(event.getRayTraceResult() instanceof EntityHitResult entityHit)) return;

        Entity hit = entityHit.getEntity();

        if (isAllyOfMerc(merc, hit)) {
            // Cancel the hit — the arrow keeps flying
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onProjectileSpawn(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        if (!(event.getEntity() instanceof Projectile projectile)) return;
        if (!(projectile.getOwner() instanceof MercenaryBowFakePlayer fakePlayer)) return;

        AbstractMercenaryEntity realShooter = fakePlayer.getBoundMercenary();
        if (realShooter == null) return;
        if (realShooter.isRemoved() || !realShooter.isAlive()) return;  // merc died mid-fire

        projectile.setOwner(realShooter);
    }

    private static boolean isAllyOfMerc(AbstractMercenaryEntity merc, Entity hit) {
        if (merc.getSummoner() != null && hit.is(merc.getSummoner())) return true;
        if (hit instanceof AbstractMercenaryEntity other
                && merc.getSummoner() != null
                && other.getSummoner() != null
                && merc.getSummoner().is(other.getSummoner())) return true;
        if (hit instanceof LivingEntity le && merc.isAlliedTo(le)) return true;
        return false;
    }

    @SubscribeEvent
    public static void onHumanDeath(LivingDeathEvent event){
        if(!(event.getEntity() instanceof AbstractMercenaryEntity human && human.isImmortal())){
            return;
        }

        if(event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD) || event.getSource().is(DamageTypes.OUTSIDE_BORDER) || event.getSource().is(DamageTypes.GENERIC_KILL)){
            return;
        }

        event.setCanceled(true);
        human.setHealth(human.getMaxHealth() / 2);
        knockbackAndStun(event.getSource(), human);
    }

    @SubscribeEvent
    public static void onTavernkeeperDeath(LivingDeathEvent event){
        if(!(event.getEntity() instanceof TavernKeeperEntity tavernKeeper)){
            return;
        }

        if(!tavernKeeper.level().isClientSide){
            TavernKeeperEntity newTavernkeeper = new TavernKeeperEntity(MREntityRegistry.TAVERNKEEP.get(), tavernKeeper.level());
            newTavernkeeper.setPos(tavernKeeper.position());
            newTavernkeeper.setSpawnPos(tavernKeeper.getSpawnPos());
            newTavernkeeper.setHasSetSpawnPos(true);
            newTavernkeeper.finalizeSpawn((ServerLevel) tavernKeeper.level(), tavernKeeper.level().getCurrentDifficultyAt(tavernKeeper.getOnPos()), MobSpawnType.MOB_SUMMONED, null);

            tavernKeeper.level().addFreshEntity(newTavernkeeper);
        }
    }

    @SubscribeEvent
    public static void onBossHurt (LivingIncomingDamageEvent event){
        if(event.getEntity().getType().is(ModTags.BOSSES_TAG)){
            if(event.getSource().getEntity() instanceof Player player && event.getSource().getDirectEntity() instanceof DevourJaw jaw
                    && jaw.getOwner() != null && jaw.getOwner().is(player)){
                ItemStack bloodPact = new ItemStack(MRItems.BLOOD_PACT, 1);
                if(player.getItemBySlot(EquipmentSlot.OFFHAND).is(MRItems.CONTRACT_TEMPORARY)){
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
}
