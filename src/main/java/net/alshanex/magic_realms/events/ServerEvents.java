package net.alshanex.magic_realms.events;

import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.entity.chimera.ChimeraEntity;
import net.alshanex.magic_realms.entity.exclusive.aliana.AlianaEntity;
import net.alshanex.magic_realms.entity.exclusive.catas.CatasEntity;
import net.alshanex.magic_realms.entity.tavernkeep.TavernKeeperEntity;
import net.alshanex.magic_realms.util.BanditCommands;
import net.alshanex.magic_realms.util.GivePageCommand;
import net.alshanex.magic_realms.util.HumanEntityCommands;
import net.alshanex.magic_realms.util.chimera.BloodSiphonManager;
import net.alshanex.magic_realms.util.humans.mercenaries.EntityClass;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.pixeldreamstudios.rpgdialogue.murmur.MurmurManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@EventBusSubscriber(modid = MagicRealms.MODID)
public class ServerEvents {
    private static final Map<Integer, List<Runnable>> delayedResponses = new HashMap<>();
    private static int currentTick = 0;
    private static final int TICK_RESET_THRESHOLD = 1_000_000; // Reset every ~13.9 hours

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        currentTick++;

        // Execute any delayed responses that are ready
        List<Runnable> responsesToExecute = delayedResponses.remove(currentTick);
        if (responsesToExecute != null) {
            responsesToExecute.forEach(Runnable::run);
        }

        // Reset tick counter periodically to prevent overflow (extremely rare but safe)
        if (currentTick >= TICK_RESET_THRESHOLD) {
            // Only reset if no pending responses exist
            if (delayedResponses.isEmpty()) {
                currentTick = 0;
            }
        }

        BloodSiphonManager.tick();
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        HumanEntityCommands.register(event.getDispatcher());
        BanditCommands.register(event.getDispatcher());
        GivePageCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onEquipmentChangeEvent(LivingEquipmentChangeEvent event){
        if (event.getEntity() instanceof AbstractMercenaryEntity human && !human.level().isClientSide) {
            if(human.getEntityClass() == EntityClass.MAGE && event.getSlot() == EquipmentSlot.OFFHAND){
                human.updateSpellbookSpells();
            }
            human.refreshSpellsAfterEquipmentChange();
        }
    }

    @SubscribeEvent
    public static void onFoodEaten(LivingEntityUseItemEvent.Start event){
        if (event.getEntity() instanceof Player player && !player.level().isClientSide && event.getItem().is(Items.PUMPKIN_PIE)) {
            Level level = player.level();
            CatasEntity catas = isMothNearby(player, level);
            if (catas != null) {
                Component line = Component.translatable("message.magic_realms.catas.pumpkin_pie.response");
                MurmurManager.speak(catas, line, MurmurManager.EARSHOT);
            }
        }

        if (event.getEntity() instanceof Player player && !player.level().isClientSide && event.getItem().is(Items.POISONOUS_POTATO)) {
            Level level = player.level();
            AlianaEntity aliana = isAlianaNearby(player, level);
            if (aliana != null) {
                Component line = Component.translatable("message.magic_realms.aliana.eat.poison_potatoes");
                MurmurManager.speak(aliana, line, MurmurManager.EARSHOT);
            }
        }
    }

    private static final double SEARCH_RADIUS = 8.0;

    private static CatasEntity isMothNearby(Player player, Level level) {
        // Create a bounding box around the player
        AABB searchArea = new AABB(
                player.getX() - SEARCH_RADIUS,
                player.getY() - SEARCH_RADIUS,
                player.getZ() - SEARCH_RADIUS,
                player.getX() + SEARCH_RADIUS,
                player.getY() + SEARCH_RADIUS,
                player.getZ() + SEARCH_RADIUS
        );

        // Get all TavernKeeperEntity instances in the area
        List<CatasEntity> nearbyCatas = level.getEntitiesOfClass(
                CatasEntity.class,
                searchArea
        );

        if(!nearbyCatas.isEmpty()){
            return nearbyCatas.getFirst();
        }

        return null;
    }

    private static AlianaEntity isAlianaNearby(Player player, Level level) {
        // Create a bounding box around the player
        AABB searchArea = new AABB(
                player.getX() - SEARCH_RADIUS,
                player.getY() - SEARCH_RADIUS,
                player.getZ() - SEARCH_RADIUS,
                player.getX() + SEARCH_RADIUS,
                player.getY() + SEARCH_RADIUS,
                player.getZ() + SEARCH_RADIUS
        );

        // Get all TavernKeeperEntity instances in the area
        List<AlianaEntity> nearbyAliana = level.getEntitiesOfClass(
                AlianaEntity.class,
                searchArea
        );

        if(!nearbyAliana.isEmpty()){
            return nearbyAliana.getFirst();
        }

        return null;
    }

    @SubscribeEvent
    public static void onSpellPreCasted(SpellPreCastEvent event){
        if(event.getEntity() instanceof ServerPlayer player && !player.level().isClientSide){
            if(event.getSchoolType() == SchoolRegistry.BLOOD.get()){
                var radius = 5;
                var radiusSqr = radius * radius;
                List<ChimeraEntity> nearbyChimeras = player.level().getEntitiesOfClass(ChimeraEntity.class, new AABB(player.position().subtract(radius, radius, radius), player.position().add(radius, radius, radius)),
                        chimeraEntity -> chimeraEntity.distanceTo(player) <= radiusSqr);

                if(!nearbyChimeras.isEmpty()){
                    for(ChimeraEntity chimera : nearbyChimeras){
                        if (chimera.isDowned()) {
                            event.setCanceled(true);
                            chimera.reassemble();
                            BloodSiphonManager.startSiphon(player, chimera);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        BloodSiphonManager.clear();
    }
}
