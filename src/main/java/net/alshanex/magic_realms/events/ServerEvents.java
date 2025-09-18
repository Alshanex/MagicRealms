package net.alshanex.magic_realms.events;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.entity.random.RandomHumanEntity;
import net.alshanex.magic_realms.entity.tavernkeep.TavernKeeperEntity;
import net.alshanex.magic_realms.util.HumanEntityCommands;
import net.alshanex.magic_realms.util.humans.EntityClass;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;

import java.util.List;
import java.util.regex.Pattern;

@EventBusSubscriber(modid = MagicRealms.MODID)
public class ServerEvents {
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

    private static final double SEARCH_RADIUS = 5.0;

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage().getString().toLowerCase();
        Level level = player.level();

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

        // Check if the message contains all required words/phrases
        if (containsAllRequiredWords(message, howPattern, getObtainPattern, tavernkeepPattern, bloodPactPattern)) {
            // Check for nearby TavernKeeperEntity
            if (hasTavernKeeperNearby(player, level)) {
                // Send the response message to the player
                sendTavernKeeperResponse(player);
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
}
