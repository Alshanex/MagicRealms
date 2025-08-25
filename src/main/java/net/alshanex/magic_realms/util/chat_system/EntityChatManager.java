package net.alshanex.magic_realms.util.chat_system;

import net.alshanex.magic_realms.entity.RandomHumanEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;

public class EntityChatManager {
    private static final Map<UUID, ChatSession> activeSessions = new java.util.concurrent.ConcurrentHashMap<>();

    public static void startChat(ServerPlayer player, RandomHumanEntity entity) {
        ChatSession session = new ChatSession(player.getUUID(), entity.getUUID(), entity);
        activeSessions.put(player.getUUID(), session);

        // Send instruction to player
        player.sendSystemMessage(Component.literal("Type in chat to talk with " + entity.getEntityName() + ". Type 'bye' to end conversation.")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    public static boolean handleChatMessage(ServerPlayer player, String message) {
        ChatSession session = activeSessions.get(player.getUUID());
        if (session == null) return false;

        // Check if player wants to end conversation
        if (message.toLowerCase().matches(".*(bye|goodbye|farewell|see you|exit).*")) {
            endChat(player);
            return true;
        }

        // Get entity if still valid
        RandomHumanEntity entity = session.getEntity(player.level());
        if (entity == null || !entity.isAlive()) {
            player.sendSystemMessage(Component.literal("The entity is no longer available.")
                    .withStyle(ChatFormatting.RED));
            endChat(player);
            return true;
        }

        // Generate response from AI
        EntityAIBrain brain = EntityAIManager.getBrain(entity);
        String response = brain.generateResponse(player, message, entity);

        // Send response to player
        MutableComponent entityName = Component.literal(entity.getEntityName())
                .withStyle(getColorForMood(brain.getCurrentMood()));

        MutableComponent chatMessage = Component.literal("")
                .append(entityName)
                .append(Component.literal(": ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(response).withStyle(ChatFormatting.WHITE));

        player.sendSystemMessage(chatMessage);

        // Add mood indicator if significant
        if (brain.getCurrentMood() != MoodState.NEUTRAL) {
            player.sendSystemMessage(Component.literal("(" + entity.getEntityName() + " seems " +
                            brain.getCurrentMood().toString().toLowerCase() + ")")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }

        return true;
    }

    public static void endChat(ServerPlayer player) {
        ChatSession session = activeSessions.remove(player.getUUID());
        if (session != null) {
            player.sendSystemMessage(Component.literal("Conversation ended.")
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    public static boolean hasActiveChat(ServerPlayer player) {
        return activeSessions.containsKey(player.getUUID());
    }

    private static ChatFormatting getColorForMood(MoodState mood) {
        return switch (mood) {
            case HAPPY, EXCITED -> ChatFormatting.GREEN;
            case SAD, BORED -> ChatFormatting.GRAY;
            case ANGRY -> ChatFormatting.RED;
            case ANXIOUS -> ChatFormatting.YELLOW;
            default -> ChatFormatting.AQUA;
        };
    }

    static class ChatSession {
        private final UUID playerId;
        private final UUID entityUUID;
        private final int entityId;
        private final long startTime;
        private RandomHumanEntity cachedEntity;

        public ChatSession(UUID playerId, UUID entityUUID, RandomHumanEntity entity) {
            this.playerId = playerId;
            this.entityUUID = entityUUID;
            this.entityId = entity.getId();
            this.startTime = System.currentTimeMillis();
            this.cachedEntity = entity;
        }

        public RandomHumanEntity getEntity(net.minecraft.world.level.Level level) {
            if (cachedEntity != null && cachedEntity.isAlive() && !cachedEntity.isRemoved()) {
                return cachedEntity;
            }

            // Try to find entity in world using integer ID
            var entity = level.getEntity(entityId);
            if (entity instanceof RandomHumanEntity humanEntity) {
                // Verify it's the same entity by UUID
                if (humanEntity.getUUID().equals(entityUUID)) {
                    cachedEntity = humanEntity;
                    return humanEntity;
                }
            }

            return null;
        }
    }
}