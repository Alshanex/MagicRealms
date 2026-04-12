package net.alshanex.magic_realms.events;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.tim.TimEntity;
import net.alshanex.magic_realms.registry.MREntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

@EventBusSubscriber(modid = MagicRealms.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModEvents {
    @SubscribeEvent
    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event){
        event.register(MREntityRegistry.MAGIC_SLIME.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (entityType, level, spawnType, pos, random) -> checkDaytimeSpawn(entityType, level, spawnType, pos, random, Mob::checkMobSpawnRules), RegisterSpawnPlacementsEvent.Operation.REPLACE);

        event.register(MREntityRegistry.MAGIC_CREEPER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Creeper::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private static <T extends Mob> boolean checkDaytimeSpawn(EntityType<T> entityType, ServerLevelAccessor level,
                                                         net.minecraft.world.entity.MobSpawnType spawnType,
                                                         BlockPos pos, RandomSource random,
                                                         SpawnPlacements.SpawnPredicate<T> originalSpawnRule) {

        long dayTime = level.getLevelData().getDayTime() % 24000;
        boolean isDaytime = dayTime >= 0 && dayTime < 12000;

        if(!isDaytime){
            return false;
        }

        return originalSpawnRule.test(entityType, level, spawnType, pos, random);
    }
}
