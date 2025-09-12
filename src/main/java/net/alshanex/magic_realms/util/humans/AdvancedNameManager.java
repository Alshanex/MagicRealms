package net.alshanex.magic_realms.util.humans;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class AdvancedNameManager {
    private static final Map<Gender, List<String>> NAMES_BY_GENDER = new HashMap<>();
    private static final Map<String, List<String>> NAMES_BY_CULTURE = new HashMap<>();
    private static final Random RANDOM = new Random();
    private static final Gson GSON = new Gson();

    private static final Map<Gender, List<String>> DEFAULT_NAMES = new HashMap<>();

    static {
        initializeDefaultNames();
        if (FMLEnvironment.dist.isClient()) {
            loadNamesFromResources();
        }
    }

    private static void initializeDefaultNames() {
        List<String> defaultMaleNames = Arrays.asList(
                "Aldric", "Bowen", "Cedric", "Dorian", "Edmund", "Felix", "Gareth", "Hugo",
                "Ivan", "Jasper", "Klaus", "Leon", "Magnus", "Nolan", "Oscar", "Preston",
                "Quentin", "Roland", "Sebastian", "Tristan", "Ulric", "Victor", "Winston"
        );

        List<String> defaultFemaleNames = Arrays.asList(
                "Aria", "Beatrice", "Celeste", "Diana", "Evelyn", "Fiona", "Grace", "Helena",
                "Isabella", "Jasmine", "Katherine", "Luna", "Morgana", "Natalie", "Ophelia",
                "Penelope", "Quinn", "Rose", "Seraphina", "Tessa", "Ursula", "Victoria"
        );

        DEFAULT_NAMES.put(Gender.MALE, defaultMaleNames);
        DEFAULT_NAMES.put(Gender.FEMALE, defaultFemaleNames);

        NAMES_BY_GENDER.put(Gender.MALE, new ArrayList<>(defaultMaleNames));
        NAMES_BY_GENDER.put(Gender.FEMALE, new ArrayList<>(defaultFemaleNames));
    }

    @OnlyIn(Dist.CLIENT)
    public static void loadNamesFromResources() {
        try {
            // Cambiar la ruta para que apunte a assets en lugar de data
            loadNamesFromJson("names/male_names.json", Gender.MALE);
            loadNamesFromJson("names/female_names.json", Gender.FEMALE);

            MagicRealms.LOGGER.debug("Successfully loaded {} male names and {} female names",
                    NAMES_BY_GENDER.get(Gender.MALE).size(),
                    NAMES_BY_GENDER.get(Gender.FEMALE).size());

        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to load names from resources, using defaults", e);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void loadNamesFromJson(String resourcePath, Gender gender) {
        try {
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, resourcePath);

            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.getResourceManager() != null) {
                ResourceManager resourceManager = mc.getResourceManager();
                Optional<Resource> resource = resourceManager.getResource(location);

                if (resource.isPresent()) {
                    try (InputStreamReader reader = new InputStreamReader(
                            resource.get().open(), StandardCharsets.UTF_8)) {

                        JsonObject jsonObject = GSON.fromJson(reader, JsonObject.class);
                        if (jsonObject != null && jsonObject.has("names")) {
                            JsonArray namesArray = jsonObject.getAsJsonArray("names");

                            List<String> loadedNames = new ArrayList<>();
                            for (JsonElement element : namesArray) {
                                loadedNames.add(element.getAsString());
                            }

                            if (!loadedNames.isEmpty()) {
                                NAMES_BY_GENDER.get(gender).clear();
                                NAMES_BY_GENDER.get(gender).addAll(loadedNames);
                                MagicRealms.LOGGER.debug("Loaded {} names for {} from {}",
                                        loadedNames.size(), gender.getName(), resourcePath);
                            }
                        } else {
                            MagicRealms.LOGGER.warn("Invalid JSON structure in {}", resourcePath);
                        }
                    }
                } else {
                    MagicRealms.LOGGER.warn("Resource not found: {}", location);
                }
            } else {
                MagicRealms.LOGGER.warn("Minecraft instance or ResourceManager is null");
            }
        } catch (Exception e) {
            MagicRealms.LOGGER.warn("Could not load names from {}: {}", resourcePath, e.getMessage());
        }
    }

    public static String getRandomName(Gender gender) {
        List<String> names = NAMES_BY_GENDER.get(gender);
        if (names == null || names.isEmpty()) {
            names = DEFAULT_NAMES.get(gender);
            MagicRealms.LOGGER.warn("Using default names for gender: {}", gender.getName());
        }

        if (names == null || names.isEmpty()) {
            return "Unknown";
        }

        String selectedName = names.get(RANDOM.nextInt(names.size()));
        MagicRealms.LOGGER.debug("Selected name '{}' for gender '{}'", selectedName, gender.getName());
        return selectedName;
    }

    @OnlyIn(Dist.CLIENT)
    public static void reloadNames() {
        NAMES_BY_GENDER.clear();
        NAMES_BY_CULTURE.clear();
        initializeDefaultNames();
        loadNamesFromResources();
        MagicRealms.LOGGER.info("Reloaded all names from resources");
    }
}
