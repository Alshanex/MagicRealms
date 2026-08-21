package net.alshanex.magic_realms;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
@EventBusSubscriber(modid = MagicRealms.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue IMMORTAL_STUN_DURATION = BUILDER
            .comment("Duration in seconds that immortal entities are stunned after being knocked out")
            .defineInRange("immortalStunDuration", 10, 1, 60);

    private static final ModConfigSpec.DoubleValue CUSTOM_TEXTURE_CHANCE = BUILDER
            .comment("Chance of a human spawning with one of the textures from the additional textures directory (0.5 = 50%)")
            .defineInRange("customTextureChance", 0.1, 0, 1.0);

    private static final ModConfigSpec.IntValue MINUTES_UNTIL_PERMANENT = BUILDER
            .comment("Duration in minutes needed to be able to establish a permanent contract with a human")
            .defineInRange("minutesUntilPermanent", 200, 1, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue MINUTES_PER_CONTRACT = BUILDER
            .comment("Duration in minutes contracts will last for")
            .defineInRange("minutesPerContract", 10, 1, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue MAX_MERCENARIES_IN_RADIUS = BUILDER
            .comment("Max amount of mercenaries that can spawn in chairs inside a 20 blocks radius.")
            .defineInRange("maxMercenariesInRadius", 8, 1, Integer.MAX_VALUE);

    private static final ModConfigSpec.BooleanValue ATTEMPT_USE_UNCLASSIFIED_SPELLS = BUILDER
            .comment("If mercenaries should attempt to use unclassified spells (set at your own risk).")
            .define("attemptCastUnclassifiedSpells", false);

    // Name configuration section
    private static final ModConfigSpec.ConfigValue<List<? extends String>> MALE_NAMES = BUILDER
            .comment("List of male names for human entities")
            .defineList("maleNames", getDefaultMaleNames(), obj -> obj instanceof String);

    private static final ModConfigSpec.ConfigValue<List<? extends String>> FEMALE_NAMES = BUILDER
            .comment("List of female names for human entities")
            .defineList("femaleNames", getDefaultFemaleNames(), obj -> obj instanceof String);

    private static final ModConfigSpec.ConfigValue<List<? extends String>> HOMETOWNS = BUILDER
            .comment("List of hometown names for mercenary backstory flavor")
            .defineList("hometowns", getDefaultHometowns(), obj -> obj instanceof String);

    private static final ModConfigSpec.ConfigValue<List<? extends String>> TAVERN_TIPS = BUILDER
            .comment("List of translation keys for the Tavernkeeper's tips.")
            .defineList("tavernTips", getDefaultTavernTips(), obj -> obj instanceof String);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static int immortalStunDuration;
    public static double customTextureChance;
    public static int minutesUntilPermanent;
    public static int minutesPerContract;
    public static int maxMercenariesInRadius;
    public static boolean attemptCastUnclassifiedSpells;

    // Name lists
    public static List<String> maleNames;
    public static List<String> femaleNames;
    public static List<String> hometowns;
    public static List<String> tavernTips;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        immortalStunDuration = IMMORTAL_STUN_DURATION.get();
        customTextureChance = CUSTOM_TEXTURE_CHANCE.get();
        minutesUntilPermanent = MINUTES_UNTIL_PERMANENT.get();
        minutesPerContract = MINUTES_PER_CONTRACT.get();
        maxMercenariesInRadius = MAX_MERCENARIES_IN_RADIUS.get();
        attemptCastUnclassifiedSpells = ATTEMPT_USE_UNCLASSIFIED_SPELLS.get();

        maleNames = MALE_NAMES.get().stream().map(String::valueOf).collect(Collectors.toList());
        femaleNames = FEMALE_NAMES.get().stream().map(String::valueOf).collect(Collectors.toList());
        hometowns = HOMETOWNS.get().stream().map(String::valueOf).collect(Collectors.toList());
        tavernTips = TAVERN_TIPS.get().stream().map(String::valueOf).collect(Collectors.toList());
    }

    private static List<String> getDefaultMaleNames() {
        return Arrays.asList(
                "Aldric", "Bowen", "Cedric", "Dorian", "Edmund", "Felix", "Gareth", "Hugo",
                "Ivan", "Jasper", "Klaus", "Leon", "Magnus", "Nolan", "Oscar", "Preston",
                "Quentin", "Roland", "Sebastian", "Tristan", "Ulric", "Victor", "Winston",
                "Aaron", "Abdul", "Abraham", "Adam", "Adrian", "Ahmed", "Akira", "Alan", "Albert", "Alexander",
                "Ali", "Amir", "Andre", "Andrew", "Angelo", "Antonio", "Ari", "Arthur", "Arjun", "Asher",
                "Ashton", "Axel", "Benjamin", "Blake", "Boris", "Brandon", "Brian", "Bruno", "Caleb", "Carlos",
                "Cedric", "Chen", "Christian", "Christopher", "Connor", "Daniel", "David", "Diego", "Dmitri", "Dylan",
                "Edgar", "Eduardo", "Edward", "Elias", "Emmanuel", "Erik", "Ethan", "Felix", "Fernando", "Francisco",
                "Gabriel", "George", "Giovanni", "Gonzalo", "Gustav", "Hassan", "Henry", "Hugo", "Ian", "Ibrahim",
                "Isaac", "Ivan", "Jack", "Jacob", "James", "Jason", "Javier", "Jean", "Jesus", "John",
                "Jonathan", "Jorge", "Jose", "Joseph", "Joshua", "Juan", "Julian", "Justin", "Kai", "Kevin",
                "Klaus", "Leonardo", "Liam", "Louis", "Lucas", "Luis", "Marco", "Marcus", "Mario", "Mark",
                "Martin", "Matthew", "Max", "Michael", "Miguel", "Nathan", "Nicholas", "Noah", "Oliver", "Omar",
                "Oscar", "Pablo", "Patrick", "Paul", "Pedro", "Peter", "Rafael", "Ramon", "Ricardo", "Richard",
                "Robert", "Roberto", "Ryan", "Samuel", "Santiago", "Sebastian", "Sergio", "Simon", "Stefan", "Stephen",
                "Thomas", "Timothy", "Victor", "Vincent", "William", "Xavier", "Zachary"
        );
    }

    private static List<String> getDefaultFemaleNames() {
        return Arrays.asList(
                "Aria", "Beatrice", "Celeste", "Diana", "Evelyn", "Fiona", "Grace", "Helena",
                "Isabella", "Jasmine", "Katherine", "Luna", "Morgana", "Natalie", "Ophelia",
                "Penelope", "Quinn", "Rose", "Seraphina", "Tessa", "Ursula", "Victoria",
                "Aaliyah", "Abigail", "Ada", "Adriana", "Aisha", "Alana", "Alexandra", "Alice", "Alicia", "Amanda",
                "Amelia", "Amy", "Ana", "Andrea", "Angela", "Anna", "Anne", "Aria", "Ariana", "Ashley",
                "Aurora", "Ava", "Beatrice", "Bella", "Beth", "Bianca", "Brooke", "Camila", "Carla", "Carmen",
                "Caroline", "Catherine", "Celia", "Charlotte", "Chloe", "Christina", "Clara", "Claudia", "Danielle", "Diana",
                "Elena", "Elizabeth", "Emily", "Emma", "Eva", "Faith", "Fatima", "Felicia", "Fiona", "Gabriela",
                "Grace", "Hannah", "Helen", "Isabella", "Jasmine", "Jennifer", "Jessica", "Julia", "Katherine", "Laura",
                "Leah", "Linda", "Lisa", "Luna", "Madison", "Maria", "Michelle", "Natalie", "Nicole", "Olivia",
                "Patricia", "Rachel", "Rebecca", "Rose", "Ruth", "Samantha", "Sarah", "Sofia", "Sophia", "Stephanie",
                "Susan", "Taylor", "Teresa", "Victoria", "Zoe"
        );
    }

    private static List<String> getDefaultHometowns() {
        return Arrays.asList(
                "Westhollow", "Ironwatch", "Greyhaven", "Silverbrook", "Redgate",
                "Blackfen", "Stormkeep", "Oakmere", "Ashford", "Thornvale",
                "Highpine", "Brackenfield", "Coldwater", "Driftmarch", "Emberdale",
                "Foxford", "Glimmerwood", "Hartrest", "Kingsreach", "Lowmere",
                "Marshend", "Nightvale", "Oldbridge", "Pebblestone", "Ravenhold",
                "Swiftbrook", "Tallgrass", "Undermoor", "Willowdeep", "Yewcross",
                "Stonewick", "Grimsby", "Fairhaven", "Mistmoor", "Saltcove"
        );
    }

    private static List<String> getDefaultTavernTips() {
        return Arrays.asList(
                "message.magic_realms.tavernkeep_tip.1",
                "message.magic_realms.tavernkeep_tip.2",
                "message.magic_realms.tavernkeep_tip.3",
                "message.magic_realms.tavernkeep_tip.4",
                "message.magic_realms.tavernkeep_tip.5",
                "message.magic_realms.tavernkeep_tip.6"
        );
    }
}