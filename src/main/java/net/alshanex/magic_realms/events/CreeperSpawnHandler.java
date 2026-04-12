package net.alshanex.magic_realms.events;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.creeper.MagicCreeperEntity;
import net.alshanex.magic_realms.registry.MREntityRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber(modid = MagicRealms.MODID)
public class CreeperSpawnHandler {
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (event.getEntity() instanceof Creeper creeper && !creeper.isPowered() && !(event.getEntity() instanceof MagicCreeperEntity)) {
            Level level = event.getLevel();

            if (level.random.nextFloat() < 0.05f) {
                event.setCanceled(true);

                MagicCreeperEntity magicCreeper = new MagicCreeperEntity(MREntityRegistry.MAGIC_CREEPER.get(), level);
                magicCreeper.moveTo(creeper.getX(), creeper.getY(), creeper.getZ(), creeper.getYRot(), creeper.getXRot());
                magicCreeper.finalizeSpawn((ServerLevel) level, level.getCurrentDifficultyAt(creeper.getOnPos()), MobSpawnType.NATURAL, null);

                level.addFreshEntity(magicCreeper);
            }
        }
    }
}
