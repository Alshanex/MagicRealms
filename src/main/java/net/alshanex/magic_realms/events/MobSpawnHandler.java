package net.alshanex.magic_realms.events;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.random.hostile.HostileRandomHumanEntity;
import net.alshanex.magic_realms.entity.tim.TimEntity;
import net.alshanex.magic_realms.registry.MREntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.horse.ZombieHorse;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber(modid = MagicRealms.MODID)
public class MobSpawnHandler {
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if(event.getEntity() instanceof Zombie zombie && !(event.getEntity() instanceof ZombifiedPiglin) && !(event.getEntity() instanceof ZombieVillager)
                && !(event.getEntity() instanceof ZombieHorse)){
            BlockPos pos = zombie.blockPosition();

            // Check cave conditions
            if (pos.getY() > 50) return;
            if (event.getLevel().canSeeSky(pos)) return;
            if (event.getLevel().getBrightness(LightLayer.BLOCK, pos) > 0) return;

            // 2% chance
            if (zombie.getRandom().nextFloat() >= 0.02f) return;

            // Spawn Tim in place of the zombie
            TimEntity tim = MREntityRegistry.TIM.get().create(event.getLevel());
            if (tim != null) {
                tim.moveTo(zombie.getX(), zombie.getY(), zombie.getZ(), zombie.getYRot(), zombie.getXRot());
                tim.finalizeSpawn((ServerLevelAccessor) event.getLevel(), event.getLevel().getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null);
                event.getLevel().addFreshEntity(tim);

                // Cancel the original zombie spawn
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onPillagerSpawn(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        if (!(event.getEntity() instanceof Pillager pillager)) return;

        BlockPos pos = pillager.blockPosition();

        // 10% roll
        if (pillager.getRandom().nextFloat() >= 0.10f) return;

        HostileRandomHumanEntity bandit = new HostileRandomHumanEntity(event.getLevel(), "magic_realms:normal_bandit");
        bandit.moveTo(pillager.getX(), pillager.getY(), pillager.getZ(), pillager.getYRot(), pillager.getXRot());
        bandit.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null);

        if (event.getLevel().addFreshEntity(bandit)) {
            event.setCanceled(true);
        }
    }
}
