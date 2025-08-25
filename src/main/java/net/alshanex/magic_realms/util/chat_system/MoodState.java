package net.alshanex.magic_realms.util.chat_system;

public enum MoodState {
    HAPPY, NEUTRAL, SAD, ANGRY, EXCITED, BORED, ANXIOUS;

    public MoodState transition(double change) {
        if (change > 0.5) return HAPPY;
        if (change > 0.2) return EXCITED;
        if (change < -0.5) return ANGRY;
        if (change < -0.2) return SAD;
        return NEUTRAL;
    }
}
