package net.alshanex.magic_realms.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class SummoningUtils {
    public static void handleAlliesSummoning(ServerPlayer player){
        if(CurioUtils.isWearingTablet(player)){
            ItemStack trinket = CurioUtils.getTablet(player);

        }
    }
}
