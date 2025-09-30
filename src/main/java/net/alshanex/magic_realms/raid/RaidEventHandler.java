package net.alshanex.magic_realms.raid;

import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = MagicRealms.MODID)
public class RaidEventHandler {

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            CustomRaidManager raidManager = serverLevel.getDataStorage()
                    .computeIfAbsent(
                            CustomRaidManager.factory(serverLevel),
                            "magic_realms_raids"
                    );

            raidManager.tick();
        }
    }
}
