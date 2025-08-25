package net.alshanex.magic_realms.util.chat_system;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.RandomHumanEntity;
import net.alshanex.magic_realms.util.humans.EntityClass;
import net.alshanex.magic_realms.util.humans.Gender;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class EntityAIBrain {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Random RANDOM = new Random();

    // Centralized knowledge base shared across all entities
    private static final KnowledgeBase GLOBAL_KNOWLEDGE = new KnowledgeBase();

    // Entity-specific data
    private final UUID entityId;
    private final PersonalityProfile personality;
    private final Map<UUID, RelationshipData> playerRelations = new ConcurrentHashMap<>();
    private MoodState currentMood;
    private final ConversationMemory conversationMemory;
    private final Map<String, Object> personalInfo = new HashMap<>();

    // Response generation weights
    private static final double PERSONALITY_WEIGHT = 0.3;
    private static final double MOOD_WEIGHT = 0.2;
    private static final double RELATIONSHIP_WEIGHT = 0.3;
    private static final double KNOWLEDGE_WEIGHT = 0.2;

    public EntityAIBrain(UUID entityId, RandomHumanEntity entity) {
        this.entityId = entityId;
        this.personality = PersonalityProfile.generateRandom(entity);
        this.currentMood = MoodState.NEUTRAL;
        this.conversationMemory = new ConversationMemory(entityId);
        initializePersonalInfo(entity);
    }

    private void initializePersonalInfo(RandomHumanEntity entity) {
        personalInfo.put("name", entity.getEntityName());
        personalInfo.put("gender", entity.getGender().getName());
        personalInfo.put("class", entity.getEntityClass().getName());
        personalInfo.put("star_level", entity.getStarLevel());
        personalInfo.put("level", entity.getData(net.alshanex.magic_realms.registry.MRDataAttachments.KILL_TRACKER).getCurrentLevel());
        personalInfo.put("spells", entity.getPersistedSpells().stream()
                .map(spell -> spell.getSpellName())
                .collect(Collectors.toList()));
        personalInfo.put("is_archer", entity.isArcher());
        personalInfo.put("has_shield", entity.hasShield());

        // Store magic schools for mages
        if (entity.getEntityClass() == EntityClass.MAGE) {
            personalInfo.put("magic_schools", entity.getMagicSchools().stream()
                    .map(school -> school.getId().toString())
                    .collect(Collectors.toList()));
        }
    }

    /**
     * Main method to generate a response to player input
     */
    public String generateResponse(Player player, String input, RandomHumanEntity entity) {
        UUID playerId = player.getUUID();

        // Update or create relationship data
        RelationshipData relationship = playerRelations.computeIfAbsent(playerId,
                k -> new RelationshipData(playerId));

        // Store the conversation
        conversationMemory.addMessage(playerId, input, true);

        // Analyze input sentiment and intent
        InputAnalysis analysis = analyzeInput(input);

        // Update mood based on interaction
        updateMood(analysis, relationship);

        // Search for relevant knowledge
        List<KnowledgeEntry> relevantKnowledge = GLOBAL_KNOWLEDGE.search(input, 5);

        // Check for personal questions
        String personalResponse = handlePersonalQuestions(input, entity);
        if (personalResponse != null) {
            conversationMemory.addMessage(playerId, personalResponse, false);
            return applyPersonalityAndMood(personalResponse, relationship);
        }

        // Generate contextual response
        String response = generateContextualResponse(
                input,
                analysis,
                relevantKnowledge,
                relationship,
                entity
        );

        // Learn from the interaction
        if (analysis.isCorrection) {
            GLOBAL_KNOWLEDGE.addKnowledge(
                    analysis.correctedTopic,
                    input,
                    KnowledgeCategory.PLAYER_TAUGHT,
                    1.0
            );
        }

        // Update relationship based on interaction quality
        updateRelationship(relationship, analysis);

        // Store response in memory
        conversationMemory.addMessage(playerId, response, false);

        return response;
    }

    private String handlePersonalQuestions(String input, RandomHumanEntity entity) {
        String lowerInput = input.toLowerCase();

        // Questions about class
        if (lowerInput.contains("class") || lowerInput.contains("warrior") ||
                lowerInput.contains("mage") || lowerInput.contains("rogue") || lowerInput.contains("archer")) {
            EntityClass entityClass = (EntityClass) personalInfo.get("class");
            boolean isArcher = (boolean) personalInfo.getOrDefault("is_archer", false);

            if (entityClass == EntityClass.ROGUE && isArcher) {
                return String.format("I'm an archer, a specialized type of rogue. I prefer fighting from a distance with my bow.");
            } else if (entityClass == EntityClass.ROGUE) {
                return String.format("I'm an assassin rogue. Quick strikes and stealth are my specialties.");
            } else if (entityClass == EntityClass.WARRIOR) {
                boolean hasShield = (boolean) personalInfo.getOrDefault("has_shield", false);
                if (hasShield) {
                    return "I'm a warrior with sword and shield. I can take hits and protect others.";
                } else {
                    return "I'm a warrior. I prefer aggressive combat with heavy weapons.";
                }
            } else if (entityClass == EntityClass.MAGE) {
                List<String> schools = (List<String>) personalInfo.getOrDefault("magic_schools", new ArrayList<>());
                if (!schools.isEmpty()) {
                    return String.format("I'm a mage specializing in %s magic.",
                            String.join(" and ", schools));
                }
                return "I'm a mage. I wield powerful spells in combat.";
            }
        }

        // Questions about gender
        if (lowerInput.contains("gender") || lowerInput.contains("male") || lowerInput.contains("female")) {
            String gender = (String) personalInfo.get("gender");
            return String.format("I'm %s.", gender);
        }

        // Questions about level or power
        if (lowerInput.contains("level") || lowerInput.contains("strong") || lowerInput.contains("powerful")) {
            int level = (int) personalInfo.get("level");
            int stars = (int) personalInfo.get("star_level");
            return String.format("I'm level %d with %d star%s. %s",
                    level, stars, stars > 1 ? "s" : "",
                    stars == 3 ? "I'm quite powerful!" : stars == 2 ? "I'm reasonably strong." : "I'm still growing stronger.");
        }

        // Questions about spells
        if (lowerInput.contains("spell") || lowerInput.contains("magic") || lowerInput.contains("ability")) {
            List<String> spells = (List<String>) personalInfo.get("spells");
            if (!spells.isEmpty()) {
                return String.format("I can cast %s.", String.join(", ", spells));
            }
            return "I have various combat abilities at my disposal.";
        }

        // Questions about name
        if (lowerInput.contains("name") || lowerInput.contains("who are you")) {
            String name = (String) personalInfo.get("name");
            return String.format("I'm %s. Nice to meet you properly!", name);
        }

        return null;
    }

    private String generateContextualResponse(String input, InputAnalysis analysis,
                                              List<KnowledgeEntry> knowledge,
                                              RelationshipData relationship,
                                              RandomHumanEntity entity) {

        // Base response generation based on intent
        String baseResponse = generateBaseResponse(analysis, knowledge);

        // Apply personality traits
        baseResponse = applyPersonalityAndMood(baseResponse, relationship);

        // Add contextual elements based on game state
        baseResponse = addGameContext(baseResponse, entity);

        return baseResponse;
    }

    private String generateBaseResponse(InputAnalysis analysis, List<KnowledgeEntry> knowledge) {
        switch (analysis.intent) {
            case GREETING:
                return selectRandomResponse(GREETING_RESPONSES);
            case FAREWELL:
                return selectRandomResponse(FAREWELL_RESPONSES);
            case QUESTION_MINECRAFT:
                return generateMinecraftResponse(analysis.topic, knowledge);
            case QUESTION_SPELLS:
                return generateSpellsResponse(analysis.topic, knowledge);
            case SMALL_TALK:
                return selectRandomResponse(SMALL_TALK_RESPONSES);
            case COMPLIMENT:
                return selectRandomResponse(COMPLIMENT_RESPONSES);
            case INSULT:
                return selectRandomResponse(INSULT_RESPONSES);
            default:
                if (!knowledge.isEmpty()) {
                    return knowledge.get(0).getContent();
                }
                return selectRandomResponse(DEFAULT_RESPONSES);
        }
    }

    private String applyPersonalityAndMood(String response, RelationshipData relationship) {
        // Apply mood modifiers
        if (currentMood == MoodState.HAPPY) {
            response = makeMorePositive(response);
        } else if (currentMood == MoodState.SAD) {
            response = makeMoreNegative(response);
        } else if (currentMood == MoodState.ANGRY) {
            response = makeMoreAggressive(response);
        }

        // Apply relationship modifiers
        if (relationship.getFriendshipLevel() < -50) {
            response = makeRude(response);
        } else if (relationship.getFriendshipLevel() > 50) {
            response = makeFriendly(response);
        }

        // Apply personality traits
        if (personality.isTrait(PersonalityTrait.SHY)) {
            response = makeShy(response);
        } else if (personality.isTrait(PersonalityTrait.CONFIDENT)) {
            response = makeConfident(response);
        }

        if (personality.isTrait(PersonalityTrait.HUMOROUS)) {
            if (RANDOM.nextFloat() < 0.3) {
                response += " " + selectRandomResponse(JOKES);
            }
        }

        return response;
    }

    private void updateMood(InputAnalysis analysis, RelationshipData relationship) {
        // Mood changes based on interaction type and relationship
        double moodChange = 0;

        if (analysis.sentiment > 0.5) {
            moodChange = 0.2;
        } else if (analysis.sentiment < -0.5) {
            moodChange = -0.3;
        }

        // Relationship affects mood change magnitude
        if (relationship.getFriendshipLevel() > 0) {
            moodChange *= 1.5;
        } else if (relationship.getFriendshipLevel() < 0) {
            moodChange *= 0.5;
        }

        currentMood = currentMood.transition(moodChange);
    }

    private void updateRelationship(RelationshipData relationship, InputAnalysis analysis) {
        double change = 0;

        // Positive interactions
        if (analysis.intent == ConversationIntent.COMPLIMENT) {
            change = 5;
        } else if (analysis.intent == ConversationIntent.GREETING) {
            change = 1;
        } else if (analysis.sentiment > 0.3) {
            change = 2;
        }

        // Negative interactions
        if (analysis.intent == ConversationIntent.INSULT) {
            change = -10;
        } else if (analysis.sentiment < -0.3) {
            change = -3;
        }

        // Apply personality influence
        if (personality.isTrait(PersonalityTrait.FORGIVING)) {
            if (change < 0) change *= 0.5;
        } else if (personality.isTrait(PersonalityTrait.GRUDGE_HOLDER)) {
            if (change < 0) change *= 1.5;
        }

        relationship.adjustFriendship(change);
        relationship.recordInteraction();
    }

    // Response modification methods
    private String makeMorePositive(String response) {
        return response.replace(".", "!").replace("okay", "great").replace("fine", "wonderful");
    }

    private String makeMoreNegative(String response) {
        return response.replace("!", ".").replace("great", "okay").replace("wonderful", "alright");
    }

    private String makeMoreAggressive(String response) {
        if (RANDOM.nextFloat() < 0.3) {
            return response + " Not that you'd understand...";
        }
        return response.replace("please", "").replace("thank you", "whatever");
    }

    private String makeRude(String response) {
        if (RANDOM.nextFloat() < 0.2) {
            return "Why should I tell you that?";
        }
        return response.replace("!", ".").replace("friend", "person");
    }

    private String makeFriendly(String response) {
        if (RANDOM.nextFloat() < 0.3) {
            response = "My friend, " + response;
        }
        return response;
    }

    private String makeShy(String response) {
        if (response.length() > 50) {
            response = response.substring(0, 40) + "... um, yeah.";
        }
        return response.replace("I think", "Maybe").replace("definitely", "probably");
    }

    private String makeConfident(String response) {
        return response.replace("I think", "I know").replace("probably", "definitely")
                .replace("maybe", "certainly");
    }

    private String addGameContext(String response, RandomHumanEntity entity) {
        Level level = entity.level();

        // Add time-based context
        if (level.isNight()) {
            if (RANDOM.nextFloat() < 0.1) {
                response += " It's getting dark, we should be careful.";
            }
        } else if (level.isRaining()) {
            if (RANDOM.nextFloat() < 0.1) {
                response += " This rain is quite refreshing.";
            }
        }

        // Add health-based context
        if (entity.getHealth() < entity.getMaxHealth() * 0.3) {
            if (RANDOM.nextFloat() < 0.2) {
                response += " I could use some healing...";
            }
        }

        return response;
    }

    // Response databases
    private static final String[] GREETING_RESPONSES = {
            "Hello there!", "Greetings!", "Hey!", "Good to see you!", "Hi!"
    };

    private static final String[] FAREWELL_RESPONSES = {
            "Goodbye!", "See you later!", "Farewell!", "Take care!", "Until next time!"
    };

    private static final String[] SMALL_TALK_RESPONSES = {
            "That's interesting!", "I see what you mean.", "Tell me more!",
            "Really? That's something.", "Hmm, I hadn't thought of that."
    };

    private static final String[] COMPLIMENT_RESPONSES = {
            "Thank you so much!", "You're too kind!", "That means a lot!",
            "I appreciate that!", "You made my day!"
    };

    private static final String[] INSULT_RESPONSES = {
            "That's not very nice.", "Why would you say that?", "I don't appreciate that.",
            "Let's keep things civil.", "There's no need for that."
    };

    private static final String[] DEFAULT_RESPONSES = {
            "I'm not sure what you mean.", "Could you explain that?", "That's a tough question.",
            "Let me think about that.", "Interesting question!"
    };

    private static final String[] JOKES = {
            "Haha!", "Just kidding!", "I'm joking, of course.", "*laughs*"
    };

    private String selectRandomResponse(String[] responses) {
        return responses[RANDOM.nextInt(responses.length)];
    }

    private String generateMinecraftResponse(String topic, List<KnowledgeEntry> knowledge) {
        // Generate responses about Minecraft topics
        if (topic.contains("creeper")) {
            return "Creepers are so annoying! Always sneaking up when you least expect it.";
        } else if (topic.contains("diamond")) {
            return "Diamonds are precious! I've seen them deep underground, around Y level -59.";
        } else if (topic.contains("nether")) {
            return "The Nether is dangerous but full of valuable resources. Be prepared!";
        } else if (topic.contains("ender")) {
            return "The End is where the Ender Dragon lives. It's the ultimate challenge!";
        }

        if (!knowledge.isEmpty()) {
            return knowledge.get(0).getContent();
        }

        return "Minecraft is full of adventures and mysteries!";
    }

    private String generateSpellsResponse(String topic, List<KnowledgeEntry> knowledge) {
        // Generate responses about Iron's Spells mod
        if (topic.contains("fire")) {
            return "Fire spells are powerful but dangerous. They can burn everything!";
        } else if (topic.contains("ice")) {
            return "Ice magic is great for crowd control. Freeze your enemies!";
        } else if (topic.contains("lightning")) {
            return "Lightning spells are shocking! They chain between enemies.";
        } else if (topic.contains("holy")) {
            return "Holy magic is excellent for healing and protection.";
        }

        if (!knowledge.isEmpty()) {
            return knowledge.get(0).getContent();
        }

        return "Magic is a powerful tool when used wisely!";
    }

    private InputAnalysis analyzeInput(String input) {
        InputAnalysis analysis = new InputAnalysis();
        String lowerInput = input.toLowerCase();

        // Detect intent
        if (lowerInput.matches(".*(hello|hi|hey|greetings).*")) {
            analysis.intent = ConversationIntent.GREETING;
        } else if (lowerInput.matches(".*(bye|goodbye|farewell|see you).*")) {
            analysis.intent = ConversationIntent.FAREWELL;
        } else if (lowerInput.contains("?")) {
            if (lowerInput.matches(".*(minecraft|creeper|diamond|nether|end).*")) {
                analysis.intent = ConversationIntent.QUESTION_MINECRAFT;
                analysis.topic = extractTopic(lowerInput);
            } else if (lowerInput.matches(".*(spell|magic|fire|ice|lightning).*")) {
                analysis.intent = ConversationIntent.QUESTION_SPELLS;
                analysis.topic = extractTopic(lowerInput);
            } else {
                analysis.intent = ConversationIntent.QUESTION_GENERAL;
            }
        } else if (lowerInput.matches(".*(beautiful|smart|great|awesome|love).*")) {
            analysis.intent = ConversationIntent.COMPLIMENT;
        } else if (lowerInput.matches(".*(stupid|ugly|hate|dumb|idiot).*")) {
            analysis.intent = ConversationIntent.INSULT;
        } else {
            analysis.intent = ConversationIntent.SMALL_TALK;
        }

        // Detect sentiment
        analysis.sentiment = calculateSentiment(input);

        // Detect if it's a correction
        if (lowerInput.matches(".*(actually|no,|wrong|incorrect|not right).*")) {
            analysis.isCorrection = true;
            analysis.correctedTopic = extractTopic(lowerInput);
        }

        return analysis;
    }

    private String extractTopic(String input) {
        // Simple topic extraction - can be enhanced with NLP
        String[] words = input.split("\\s+");
        for (String word : words) {
            if (word.length() > 4 && !isCommonWord(word)) {
                return word;
            }
        }
        return "general";
    }

    private boolean isCommonWord(String word) {
        String[] common = {"what", "where", "when", "how", "why", "about", "think", "know"};
        return Arrays.asList(common).contains(word.toLowerCase());
    }

    private double calculateSentiment(String input) {
        double sentiment = 0;
        String lowerInput = input.toLowerCase();

        // Positive words
        String[] positive = {"good", "great", "love", "like", "awesome", "beautiful", "nice", "happy"};
        for (String word : positive) {
            if (lowerInput.contains(word)) sentiment += 0.3;
        }

        // Negative words
        String[] negative = {"bad", "hate", "stupid", "ugly", "terrible", "awful", "sad", "angry"};
        for (String word : negative) {
            if (lowerInput.contains(word)) sentiment -= 0.3;
        }

        return Math.max(-1, Math.min(1, sentiment));
    }

    public MoodState getCurrentMood() {
        return currentMood;
    }

    public PersonalityProfile getPersonality() {
        return personality;
    }

    public RelationshipData getRelationship(UUID playerId) {
        return playerRelations.get(playerId);
    }
    public JsonObject serialize() {
        JsonObject json = new JsonObject();
        json.addProperty("entityId", entityId.toString());
        json.add("personality", GSON.toJsonTree(personality));
        json.addProperty("mood", currentMood.name());
        json.add("relationships", GSON.toJsonTree(playerRelations));
        json.add("memory", GSON.toJsonTree(conversationMemory));
        json.add("personalInfo", GSON.toJsonTree(personalInfo));
        return json;
    }

    public static EntityAIBrain deserialize(JsonObject json, RandomHumanEntity entity) {
        UUID id = UUID.fromString(json.get("entityId").getAsString());
        EntityAIBrain brain = new EntityAIBrain(id, entity);

        // Restore state
        brain.personality.deserialize(json.getAsJsonObject("personality"));
        brain.currentMood = MoodState.valueOf(json.get("mood").getAsString());
        // Restore other data...

        return brain;
    }
}
