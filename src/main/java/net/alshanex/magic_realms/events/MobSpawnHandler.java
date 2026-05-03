package net.alshanex.magic_realms.events;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.random.hostile.HostileRandomHumanEntity;
import net.alshanex.magic_realms.entity.tim.TimEntity;
import net.alshanex.magic_realms.registry.MREntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

@EventBusSubscriber(modid = MagicRealms.MODID)
public class MobSpawnHandler {
    @SubscribeEvent
    public static void onZombieSpawn(FinalizeSpawnEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (zombie instanceof ZombifiedPiglin) return;
        if (zombie instanceof ZombieVillager) return;

        if (event.getSpawnType() != MobSpawnType.NATURAL) return;

        ServerLevelAccessor levelAccessor = event.getLevel();
        ServerLevel serverLevel = levelAccessor.getLevel();

        BlockPos pos = zombie.blockPosition();

        // Cave conditions
        if (pos.getY() > 50) return;
        if (serverLevel.canSeeSky(pos)) return;
        if (serverLevel.getBrightness(LightLayer.BLOCK, pos) > 0) return;

        // 2% chance
        if (zombie.getRandom().nextFloat() >= 0.02f) return;

        TimEntity tim = MREntityRegistry.TIM.get().create(serverLevel);
        if (tim == null) return;

        tim.moveTo(zombie.getX(), zombie.getY(), zombie.getZ(),
                zombie.getYRot(), zombie.getXRot());
        tim.finalizeSpawn(levelAccessor,
                serverLevel.getCurrentDifficultyAt(pos),
                MobSpawnType.NATURAL, null);

        if (serverLevel.addFreshEntity(tim)) {
            event.setSpawnCancelled(true);
        }
    }

    @SubscribeEvent
    public static void onPillagerSpawn(FinalizeSpawnEvent event) {
        if (!(event.getEntity() instanceof Pillager pillager)) return;

        // Only swap on natural/structure spawns. This excludes EVENT (raids), PATROL (patrol leaders), SPAWNER, MOB_SUMMONED, COMMAND, etc.
        MobSpawnType type = event.getSpawnType();
        if (type != MobSpawnType.NATURAL && type != MobSpawnType.STRUCTURE) return;

        ServerLevelAccessor levelAccessor = event.getLevel();
        if (!(levelAccessor.getLevel() instanceof ServerLevel serverLevel)) return;

        BlockPos pos = pillager.blockPosition();

        // Confirm we're actually inside a Pillager Outpost.
        Structure outpost = serverLevel.registryAccess()
                .registryOrThrow(Registries.STRUCTURE)
                .get(BuiltinStructures.PILLAGER_OUTPOST);
        if (outpost == null) return;

        StructureStart outpostStart = serverLevel.structureManager()
                .getStructureWithPieceAt(pos, outpost);
        if (!outpostStart.isValid()) return;

        // 10% roll
        if (pillager.getRandom().nextFloat() >= 0.10f) return;

        HostileRandomHumanEntity bandit = new HostileRandomHumanEntity(serverLevel.getLevel(), "magic_realms:normal_bandit");
        bandit.moveTo(pillager.getX(), pillager.getY(), pillager.getZ(), pillager.getYRot(), pillager.getXRot());
        bandit.finalizeSpawn(levelAccessor, serverLevel.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);

        if (serverLevel.addFreshEntity(bandit)) {
            event.setSpawnCancelled(true);
        }
    }
}
