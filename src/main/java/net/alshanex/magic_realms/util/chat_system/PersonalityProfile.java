package net.alshanex.magic_realms.util.chat_system;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.*;

public class PersonalityProfile {
    private final Map<PersonalityTrait, Double> traits = new HashMap<>();
    private final List<String> quirks = new ArrayList<>();
    private final SpeechPattern speechPattern;

    public PersonalityProfile() {
        this.speechPattern = SpeechPattern.NORMAL;
    }

    public static PersonalityProfile generateRandom(net.alshanex.magic_realms.entity.RandomHumanEntity entity) {
        PersonalityProfile profile = new PersonalityProfile();
        Random random = new Random(entity.getUUID().hashCode());

        // Assign random trait values
        for (PersonalityTrait trait : PersonalityTrait.values()) {
            profile.traits.put(trait, random.nextDouble());
        }

        // Add class-based personality tendencies
        switch (entity.getEntityClass()) {
            case MAGE:
                profile.traits.put(PersonalityTrait.INTELLECTUAL, 0.7 + random.nextDouble() * 0.3);
                profile.traits.put(PersonalityTrait.MYSTICAL, 0.6 + random.nextDouble() * 0.4);
                break;
            case WARRIOR:
                profile.traits.put(PersonalityTrait.BRAVE, 0.7 + random.nextDouble() * 0.3);
                profile.traits.put(PersonalityTrait.CONFIDENT, 0.6 + random.nextDouble() * 0.4);
                break;
            case ROGUE:
                profile.traits.put(PersonalityTrait.MYSTERIOUS, 0.6 + random.nextDouble() * 0.4);
                profile.traits.put(PersonalityTrait.CUNNING, 0.7 + random.nextDouble() * 0.3);
                break;
        }

        // Add gender-influenced traits (subtle differences)
        if (entity.getGender() == net.alshanex.magic_realms.util.humans.Gender.FEMALE) {
            profile.traits.merge(PersonalityTrait.EMPATHETIC, 0.1, Double::sum);
        } else {
            profile.traits.merge(PersonalityTrait.COMPETITIVE, 0.1, Double::sum);
        }

        // Add random quirks
        String[] possibleQuirks = {
                "uses old-fashioned speech",
                "makes puns frequently",
                "references ancient history",
                "speaks in riddles sometimes",
                "has a favorite catchphrase",
                "laughs at own jokes"
        };

        int quirkCount = random.nextInt(3);
        for (int i = 0; i < quirkCount; i++) {
            profile.quirks.add(possibleQuirks[random.nextInt(possibleQuirks.length)]);
        }

        return profile;
    }

    public boolean isTrait(PersonalityTrait trait) {
        return traits.getOrDefault(trait, 0.5) > 0.6;
    }

    public double getTraitValue(PersonalityTrait trait) {
        return traits.getOrDefault(trait, 0.5);
    }

    public void deserialize(JsonObject json) {
        // Implementation for loading personality from JSON
        traits.clear();
        JsonObject traitsJson = json.getAsJsonObject("traits");
        for (Map.Entry<String, JsonElement> entry : traitsJson.entrySet()) {
            traits.put(PersonalityTrait.valueOf(entry.getKey()), entry.getValue().getAsDouble());
        }
    }
}
