package net.alshanex.magic_realms.events;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.entity.exclusive.aliana.AlianaEntity;
import net.alshanex.magic_realms.entity.exclusive.catas.CatasEntity;
import net.alshanex.magic_realms.entity.tavernkeep.TavernKeeperEntity;
import net.alshanex.magic_realms.util.HumanEntityCommands;
import net.alshanex.magic_realms.util.humans.mercenaries.EntityClass;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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
import net.neoforged.neoforge.event.tick.ServerTickEvent;

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
    }

    private static void scheduleDelayedResponse(Runnable responseAction, int tickDelay) {
        int executionTick = currentTick + tickDelay;
        delayedResponses.computeIfAbsent(executionTick, k -> new ArrayList<>()).add(responseAction);
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        HumanEntityCommands.register(event.getDispatcher());
        MagicRealms.LOGGER.info("Registered Magic Realms commands");
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
            if (isMothNearby(player, level)) {
                sendPlayerResponse(player, Component.translatable("message.magic_realms.catas.pumpkin_pie.response", "Catas"));
            }
        }

        if (event.getEntity() instanceof Player player && !player.level().isClientSide && event.getItem().is(Items.POISONOUS_POTATO)) {
            Level level = player.level();
            if (isAlianaNearby(player, level)) {
                sendPlayerResponse(player, Component.translatable("message.magic_realms.aliana.eat.poison_potatoes", "Aliana"));
            }
        }
    }

    private static final double SEARCH_RADIUS = 8.0;

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage().getString().toLowerCase();
        Level level = player.level();

        //Tavernkeep
        Pattern howPattern = Pattern.compile("\\b" +
                        Component.translatable("message.magic_realms.tavernkeep_tip.question_key").getString() + "\\b",
                Pattern.CASE_INSENSITIVE);

        Pattern getObtainPattern = Pattern.compile("\\b(" +
                Component.translatable("message.magic_realms.tavernkeep_tip.action_key_1").getString() +
                "|" +
                Component.translatable("message.magic_realms.tavernkeep_tip.action_key_2").getString() +
                ")\\b", Pattern.CASE_INSENSITIVE);

        Pattern tavernkeepPattern = Pattern.compile("\\b(" +
                Component.translatable("message.magic_realms.tavernkeep_tip.subject_key_1").getString() +
                "|" +
                Component.translatable("message.magic_realms.tavernkeep_tip.subject_key_2").getString() +
                ")\\b", Pattern.CASE_INSENSITIVE);

        Pattern bloodPactPattern = Pattern.compile("\\b" +
                        Component.translatable("item.magic_realms.blood_pact").getString() + "\\b",
                Pattern.CASE_INSENSITIVE);

        // Catas
        Pattern mothPattern = Pattern.compile("\\b" +
                        Component.translatable("message.magic_realms.catas.moth.keyword").getString() + "\\b",
                Pattern.CASE_INSENSITIVE);

        Pattern geologyPattern = Pattern.compile("\\b(" +
                Component.translatable("message.magic_realms.catas.geology.keyword_1").getString() +
                "|" +
                Component.translatable("message.magic_realms.catas.geology.keyword_2").getString() +
                "|" +
                Component.translatable("message.magic_realms.catas.geology.keyword_3").getString() +
                "|" +
                Component.translatable("message.magic_realms.catas.geology.keyword_4").getString() +
                "|" +
                Component.translatable("message.magic_realms.catas.geology.keyword_5").getString() +
                ")\\b", Pattern.CASE_INSENSITIVE);

        // Check if the message contains all required words/phrases for tavernkeep
        if (containsAllRequiredWords(message, howPattern, getObtainPattern, tavernkeepPattern, bloodPactPattern)) {
            // Check for nearby TavernKeeperEntity
            if (hasTavernKeeperNearby(player, level)) {
                // Schedule the response to be sent after a delay
                scheduleDelayedResponse(() -> sendTavernKeeperResponse(player), 20);
            }
        }

        if(mothPattern.matcher(message).find()){
            if (isMothNearby(player, level)) {
                Component response = Component.translatable("message.magic_realms.catas.moth.response", "Catas").withStyle(ChatFormatting.GOLD);
                scheduleDelayedResponse(() -> sendPlayerResponse(player, response), 20);
            }
        }

        if(geologyPattern.matcher(message).find()){
            if (isMothNearby(player, level)) {
                Component response = Component.translatable("message.magic_realms.catas.geology.response", "Catas").withStyle(ChatFormatting.GOLD);
                scheduleDelayedResponse(() -> sendPlayerResponse(player, response), 20);
            }
        }
    }

    private static boolean containsAllRequiredWords(String message, Pattern howPattern, Pattern getObtainPattern,
                                                    Pattern tavernkeepPattern, Pattern bloodPactPattern) {
        return howPattern.matcher(message).find() &&
                getObtainPattern.matcher(message).find() &&
                tavernkeepPattern.matcher(message).find() &&
                bloodPactPattern.matcher(message).find();
    }

    private static boolean hasTavernKeeperNearby(Player player, Level level) {
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
        List<TavernKeeperEntity> nearbyTavernKeepers = level.getEntitiesOfClass(
                TavernKeeperEntity.class,
                searchArea
        );

        return !nearbyTavernKeepers.isEmpty();
    }

    private static void sendTavernKeeperResponse(Player player) {
        Component responseMessage = Component.translatable("message.magic_realms.tavernkeep_tip").withStyle(ChatFormatting.GOLD);

        player.sendSystemMessage(responseMessage);
    }

    private static void sendPlayerResponse(Player player, Component message) {
        player.sendSystemMessage(message);
    }

    private static boolean isMothNearby(Player player, Level level) {
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

        return !nearbyCatas.isEmpty();
    }

    private static boolean isAlianaNearby(Player player, Level level) {
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

        return !nearbyAliana.isEmpty();
    }
}
