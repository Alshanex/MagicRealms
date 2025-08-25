package net.alshanex.magic_realms.util.chat_system;

import com.google.gson.*;
import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Centralized knowledge base that stores and retrieves information
 */
public class KnowledgeBase {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, List<KnowledgeEntry>> knowledgeMap = new ConcurrentHashMap<>();
    private final Map<String, Double> termImportance = new ConcurrentHashMap<>();
    private static Path STORAGE_PATH;

    public KnowledgeBase() {
        initializeDefaultKnowledge();
    }

    /**
     * Initialize with default Minecraft and mod knowledge
     */
    private void initializeDefaultKnowledge() {
        // Minecraft basics
        addKnowledge("minecraft", "Minecraft is a sandbox game where you can build and explore.", KnowledgeCategory.MINECRAFT, 1.0);
        addKnowledge("creeper", "Creepers are hostile mobs that explode when near players.", KnowledgeCategory.MINECRAFT, 1.0);
        addKnowledge("diamond", "Diamonds are rare gems found deep underground, used for the best tools.", KnowledgeCategory.MINECRAFT, 1.0);
        addKnowledge("nether", "The Nether is a dangerous dimension filled with lava and hostile mobs.", KnowledgeCategory.MINECRAFT, 1.0);
        addKnowledge("ender_dragon", "The Ender Dragon is the final boss of Minecraft, found in The End.", KnowledgeCategory.MINECRAFT, 1.0);
        addKnowledge("villager", "Villagers are NPCs that trade items with players using emeralds.", KnowledgeCategory.MINECRAFT, 1.0);
        addKnowledge("enchanting", "Enchanting improves tools and armor with special abilities.", KnowledgeCategory.MINECRAFT, 1.0);
        addKnowledge("redstone", "Redstone is used to create mechanical and electrical constructions.", KnowledgeCategory.MINECRAFT, 1.0);

        // Iron's Spells knowledge
        addKnowledge("fire_spell", "Fire spells deal burning damage over time to enemies.", KnowledgeCategory.SPELLS, 1.0);
        addKnowledge("ice_spell", "Ice spells can freeze enemies and create defensive barriers.", KnowledgeCategory.SPELLS, 1.0);
        addKnowledge("lightning_spell", "Lightning spells chain between enemies for area damage.", KnowledgeCategory.SPELLS, 1.0);
        addKnowledge("holy_spell", "Holy spells provide healing and protection to allies.", KnowledgeCategory.SPELLS, 1.0);
        addKnowledge("blood_spell", "Blood magic uses life force for powerful effects.", KnowledgeCategory.SPELLS, 1.0);
        addKnowledge("ender_spell", "Ender spells allow teleportation and dimensional manipulation.", KnowledgeCategory.SPELLS, 1.0);
        addKnowledge("nature_spell", "Nature spells harness the power of plants and earth.", KnowledgeCategory.SPELLS, 1.0);
        addKnowledge("evocation_spell", "Evocation spells summon fangs and vexes to fight.", KnowledgeCategory.SPELLS, 1.0);

        // General conversation topics
        addKnowledge("weather", "The weather changes between sunny, rainy, and thunderstorms.", KnowledgeCategory.GENERAL, 0.5);
        addKnowledge("combat", "Combat requires strategy, timing, and the right equipment.", KnowledgeCategory.GENERAL, 0.7);
        addKnowledge("exploration", "Exploring the world reveals new biomes, structures, and resources.", KnowledgeCategory.GENERAL, 0.6);
        addKnowledge("building", "Building allows creative expression and practical shelter creation.", KnowledgeCategory.GENERAL, 0.6);
    }

    /**
     * Add new knowledge to the base
     */
    public void addKnowledge(String topic, String content, KnowledgeCategory category, double importance) {
        String normalizedTopic = topic.toLowerCase().trim();

        KnowledgeEntry entry = new KnowledgeEntry(content, category, importance, System.currentTimeMillis());

        knowledgeMap.computeIfAbsent(normalizedTopic, k -> new ArrayList<>()).add(entry);
        termImportance.put(normalizedTopic, importance);

        // Also index individual words for better search
        String[] words = content.toLowerCase().split("\\s+");
        for (String word : words) {
            if (word.length() > 3) { // Skip short words
                knowledgeMap.computeIfAbsent(word, k -> new ArrayList<>()).add(entry);
            }
        }

        MagicRealms.LOGGER.debug("Added knowledge: {} -> {}", normalizedTopic, content);
    }

    /**
     * Search for relevant knowledge based on input
     */
    public List<KnowledgeEntry> search(String query, int maxResults) {
        Map<KnowledgeEntry, Double> scores = new HashMap<>();
        String[] queryWords = query.toLowerCase().split("\\s+");

        // Score each knowledge entry based on relevance
        for (String word : queryWords) {
            List<KnowledgeEntry> entries = knowledgeMap.get(word);
            if (entries != null) {
                for (KnowledgeEntry entry : entries) {
                    scores.merge(entry, 1.0, Double::sum);
                }
            }
        }

        // Sort by score and return top results
        return scores.entrySet().stream()
                .sorted(Map.Entry.<KnowledgeEntry, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Save knowledge base to disk
     */
    public void save() {
        if (STORAGE_PATH == null) return;

        try {
            Path knowledgeFile = STORAGE_PATH.resolve("knowledge_base.json");
            Files.createDirectories(STORAGE_PATH);

            JsonObject json = new JsonObject();
            JsonArray entries = new JsonArray();

            for (Map.Entry<String, List<KnowledgeEntry>> entry : knowledgeMap.entrySet()) {
                JsonObject topicJson = new JsonObject();
                topicJson.addProperty("topic", entry.getKey());
                JsonArray topicEntries = new JsonArray();

                for (KnowledgeEntry ke : entry.getValue()) {
                    topicEntries.add(ke.toJson());
                }

                topicJson.add("entries", topicEntries);
                entries.add(topicJson);
            }

            json.add("knowledge", entries);

            try (FileWriter writer = new FileWriter(knowledgeFile.toFile())) {
                GSON.toJson(json, writer);
            }

            MagicRealms.LOGGER.info("Saved knowledge base with {} topics", knowledgeMap.size());

        } catch (IOException e) {
            MagicRealms.LOGGER.error("Failed to save knowledge base", e);
        }
    }

    /**
     * Load knowledge base from disk
     */
    public void load() {
        if (STORAGE_PATH == null) return;

        try {
            Path knowledgeFile = STORAGE_PATH.resolve("knowledge_base.json");
            if (!Files.exists(knowledgeFile)) {
                MagicRealms.LOGGER.info("No existing knowledge base found, using defaults");
                return;
            }

            try (FileReader reader = new FileReader(knowledgeFile.toFile())) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                JsonArray entries = json.getAsJsonArray("knowledge");

                knowledgeMap.clear();

                for (JsonElement element : entries) {
                    JsonObject topicJson = element.getAsJsonObject();
                    String topic = topicJson.get("topic").getAsString();
                    JsonArray topicEntries = topicJson.getAsJsonArray("entries");

                    List<KnowledgeEntry> entryList = new ArrayList<>();
                    for (JsonElement entryElement : topicEntries) {
                        entryList.add(KnowledgeEntry.fromJson(entryElement.getAsJsonObject()));
                    }

                    knowledgeMap.put(topic, entryList);
                }

                MagicRealms.LOGGER.info("Loaded knowledge base with {} topics", knowledgeMap.size());
            }

        } catch (IOException e) {
            MagicRealms.LOGGER.error("Failed to load knowledge base", e);
        }
    }

    public static void setStoragePath(MinecraftServer server) {
        STORAGE_PATH = server.getWorldPath(LevelResource.ROOT).resolve("magic_realms_ai");
    }

    /**
     * Get total count of knowledge entries
     */
    public int getKnowledgeCount() {
        return knowledgeMap.values().stream()
                .mapToInt(List::size)
                .sum();
    }
}
