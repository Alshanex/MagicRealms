package net.alshanex.magic_realms.util.chat_system;

public class ConversationEntry {
    final String message;
    final boolean isPlayerMessage;
    final long timestamp;

    public ConversationEntry(String message, boolean isPlayerMessage, long timestamp) {
        this.message = message;
        this.isPlayerMessage = isPlayerMessage;
        this.timestamp = timestamp;
    }
}
