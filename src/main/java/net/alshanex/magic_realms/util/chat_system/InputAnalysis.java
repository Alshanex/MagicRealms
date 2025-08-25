package net.alshanex.magic_realms.util.chat_system;

public class InputAnalysis {
    ConversationIntent intent = ConversationIntent.UNKNOWN;
    double sentiment = 0;
    String topic = "";
    boolean isCorrection = false;
    String correctedTopic = "";
}
