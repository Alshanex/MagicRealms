package net.alshanex.magic_realms.util.chat_system;

import com.google.gson.JsonObject;

public class KnowledgeEntry {
    private final String content;
    private final KnowledgeCategory category;
    private final double importance;
    private final long timestamp;
    private int accessCount;

    public KnowledgeEntry(String content, KnowledgeCategory category, double importance, long timestamp) {
        this.content = content;
        this.category = category;
        this.importance = importance;
        this.timestamp = timestamp;
        this.accessCount = 0;
    }

    public String getContent() {
        accessCount++;
        return content;
    }

    public KnowledgeCategory getCategory() {
        return category;
    }

    public double getImportance() {
        return importance;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("content", content);
        json.addProperty("category", category.name());
        json.addProperty("importance", importance);
        json.addProperty("timestamp", timestamp);
        json.addProperty("accessCount", accessCount);
        return json;
    }

    public static KnowledgeEntry fromJson(JsonObject json) {
        KnowledgeEntry entry = new KnowledgeEntry(
                json.get("content").getAsString(),
                KnowledgeCategory.valueOf(json.get("category").getAsString()),
                json.get("importance").getAsDouble(),
                json.get("timestamp").getAsLong()
        );
        entry.accessCount = json.get("accessCount").getAsInt();
        return entry;
    }
}
