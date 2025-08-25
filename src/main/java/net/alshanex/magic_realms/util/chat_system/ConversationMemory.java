package net.alshanex.magic_realms.util.chat_system;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ConversationMemory {
    private final UUID entityId;
    private final Map<UUID, List<ConversationEntry>> conversations = new ConcurrentHashMap<>();
    private static final int MAX_MEMORY_PER_PLAYER = 50;

    public ConversationMemory(UUID entityId) {
        this.entityId = entityId;
    }

    public void addMessage(UUID playerId, String message, boolean isPlayerMessage) {
        List<ConversationEntry> history = conversations.computeIfAbsent(playerId, k -> new ArrayList<>());

        history.add(new ConversationEntry(message, isPlayerMessage, System.currentTimeMillis()));

        // Limit memory size
        if (history.size() > MAX_MEMORY_PER_PLAYER) {
            history.remove(0);
        }
    }

    public List<ConversationEntry> getRecentConversation(UUID playerId, int count) {
        List<ConversationEntry> history = conversations.get(playerId);
        if (history == null) return new ArrayList<>();

        int start = Math.max(0, history.size() - count);
        return new ArrayList<>(history.subList(start, history.size()));
    }

    public String getLastPlayerMessage(UUID playerId) {
        List<ConversationEntry> history = conversations.get(playerId);
        if (history == null) return null;

        for (int i = history.size() - 1; i >= 0; i--) {
            if (history.get(i).isPlayerMessage) {
                return history.get(i).message;
            }
        }
        return null;
    }
}
