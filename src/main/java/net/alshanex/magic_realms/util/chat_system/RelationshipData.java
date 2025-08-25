package net.alshanex.magic_realms.util.chat_system;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RelationshipData {
    private final UUID playerId;
    private double friendshipLevel;
    private int interactionCount;
    private long lastInteraction;
    private final Map<String, Integer> topicCounts = new HashMap<>();

    public RelationshipData(UUID playerId) {
        this.playerId = playerId;
        this.friendshipLevel = 0;
        this.interactionCount = 0;
        this.lastInteraction = System.currentTimeMillis();
    }

    public void adjustFriendship(double amount) {
        friendshipLevel = Math.max(-100, Math.min(100, friendshipLevel + amount));
    }

    public double getFriendshipLevel() {
        return friendshipLevel;
    }

    public void recordInteraction() {
        interactionCount++;
        lastInteraction = System.currentTimeMillis();
    }

    public void recordTopic(String topic) {
        topicCounts.merge(topic, 1, Integer::sum);
    }

    public int getInteractionCount() {
        return interactionCount;
    }

    public boolean isNewFriend() {
        return interactionCount < 5;
    }

    public boolean isCloseFriend() {
        return friendshipLevel > 75 && interactionCount > 20;
    }

    public boolean isEnemy() {
        return friendshipLevel < -50;
    }
}
