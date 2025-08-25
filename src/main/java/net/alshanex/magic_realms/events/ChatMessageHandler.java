package net.alshanex.magic_realms.events;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.util.chat_system.EntityChatManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;

/**
 * Handles chat messages for entity conversations
 */
@EventBusSubscriber(modid = MagicRealms.MODID)
public class ChatMessageHandler {

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        String message = event.getMessage().getString();

        // Check if player is in active chat with entity
        if (EntityChatManager.hasActiveChat(player)) {
            // Cancel the normal chat message
            event.setCanceled(true);

            // Show the player's message in their chat (local echo)
            player.sendSystemMessage(
                    Component.literal("")
                            .append(Component.literal("You: ").withStyle(ChatFormatting.GRAY))
                            .append(Component.literal(message).withStyle(ChatFormatting.WHITE))
            );

            // Process the message through the entity chat system
            EntityChatManager.handleChatMessage(player, message);
        }
    }
}
