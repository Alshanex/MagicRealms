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

    private static final Map<Gender, List<String>> DEFAULT_NAMES = new HashMap<>();

    static {
        initializeDefaultNames();
    }

    private static void initializeDefaultNames() {
        List<String> defaultMaleNames = Arrays.asList(
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
                "Thomas", "Timothy", "Victor", "Vincent", "William", "Xavier", "Zachary",

                "Aarav", "Abhay", "Adnan", "Ahmad", "Ajay", "Akash", "Akhil", "Alec", "Alexei", "Alfonso",
                "Alvaro", "Amin", "Amos", "Andreas", "Andres", "Andy", "Anish", "Anthony", "Anton", "Apollo",
                "Armando", "Arnold", "Aryan", "Ashish", "Atticus", "Augustus", "Austin", "Aziz", "Badr", "Barry",
                "Bastian", "Benedict", "Bernard", "Bjorn", "Brad", "Brady", "Bram", "Brendan", "Brett", "Bruce",
                "Bryan", "Cameron", "Carl", "Carson", "Carter", "Casper", "Chad", "Charles", "Chase", "Chester",
                "Chloe", "Chris", "Cian", "Clarence", "Clark", "Clay", "Clement", "Clint", "Cole", "Colin",
                "Colton", "Conrad", "Cooper", "Corey", "Craig", "Curtis", "Damian", "Damon", "Dan", "Dane",
                "Danny", "Dario", "Dave", "Dean", "Dennis", "Derek", "Devin", "Dexter", "Dominic", "Don",
                "Douglas", "Drake", "Drew", "Duncan", "Dustin", "Earl", "Edan", "Edwin", "Eli", "Elliott",
                "Elvis", "Emil", "Emilio", "Eric", "Ernest", "Eugene", "Evan", "Ezra", "Fabian", "Fadi",
                "Fahad", "Felipe", "Finn", "Fletcher", "Floyd", "Franco", "Frank", "Fred", "Frederick", "Gabe",
                "Garrett", "Gary", "Gavin", "Gene", "Geoffrey", "Gerald", "Gerard", "Gilbert", "Glenn", "Gordon",
                "Graham", "Grant", "Greg", "Gregory", "Griffin", "Gunnar", "Guy", "Hank", "Harold", "Harry",
                "Hector", "Henrik", "Herman", "Howard", "Hunter", "Ignacio", "Iker", "Imran", "Irwin", "Jack",

                "Ahmad", "Akiko", "Aleksander", "Alessandro", "Alfonso", "Alfred", "Alonso", "Amadeus", "Amaury", "Ambrose",
                "Anders", "Ander", "Angelo", "Anselm", "Antoine", "Archer", "Ari", "Armand", "Arturo", "Atlas",
                "August", "Aurelius", "Axel", "Badr", "Baptiste", "Barnaby", "Bartholomew", "Basil", "Beau", "Beckett",
                "Bennett", "Blaise", "Bodhi", "Brayden", "Brice", "Bryce", "Cairo", "Cale", "Callan", "Camden",
                "Camilo", "Cannon", "Captain", "Caspian", "Cayden", "Cesar", "Chance", "Chandler", "Charlie", "Chetan",
                "Cody", "Cohen", "Constantine", "Cormac", "Cosmo", "Cruz", "Cyrus", "Dallas", "Dalton", "Dante",
                "Darren", "Darwin", "Davis", "Dawson", "Declan", "Desmond", "Dexter", "Dillon", "Dominick", "Donovan",
                "Dorian", "Duke", "Easton", "Edison", "Edmund", "Elijah", "Ellis", "Elton", "Emerson", "Enzo",
                "Ezekiel", "Fabio", "Falcon", "Felix", "Finnegan", "Fisher", "Ford", "Forest", "Fox", "Gael",
                "Gage", "Galen", "Gareth", "Garrett", "Gaston", "Gideon", "Giles", "Graham", "Grayson", "Griffin",
                "Haden", "Hadrian", "Hamza", "Harley", "Harper", "Harrison", "Hawk", "Hayes", "Heath", "Hendrix",
                "Holden", "Hudson", "Hugh", "Hunter", "Idris", "Indigo", "Iris", "Isaac", "Isaiah", "Ivan",
                "Jace", "Jackson", "Jagger", "Jameson", "Jasper", "Jaxon", "Jayden", "Jensen", "Jett", "Joaquin",
                "Jude", "Julius", "Justice", "Kaden", "Kane", "Karter", "Keegan", "Keith", "Kellan", "Kendrick",
                "Kenzo", "Kieran", "King", "Kingston", "Knox", "Kobe", "Kyler", "Lance", "Landon", "Lawrence",
                "Leander", "Legend", "Leo", "Leon", "Levi", "Lincoln", "Logan", "Lorenzo", "Luca", "Lucian",
                "Magnus", "Malakai", "Malcolm", "Marcel", "Marcelo", "Mars", "Mason", "Mateo", "Maverick", "Maxim",
                "Mekhi", "Milo", "Mitchell", "Montana", "Myles", "Nash", "Nathaniel", "Neo", "Nico", "Nolan",
                "Octavio", "Odin", "Orion", "Owen", "Phoenix", "Pierce", "Princeton", "Quentin", "Quinn", "Rafael",
                "Raphael", "Raven", "Reed", "Reign", "Remington", "Rex", "Riker", "River", "Roman", "Romeo",
                "Ronan", "Rowan", "Sage", "Saint", "Santino", "Sawyer", "Soren", "Sterling", "Storm", "Sullivan",
                "Thaddeus", "Theo", "Theodore", "Titan", "Tobias", "Tucker", "Tyler", "Ulysses", "Valentino", "Vance",
                "Wade", "Walker", "Warren", "Watson", "Wesley", "Weston", "Wilson", "Winston", "Wolf", "Wyatt",

                "Hiroshi", "Takeshi", "Kenji", "Akira", "Yoshida", "Tanaka", "Sato", "Suzuki", "Takahashi", "Watanabe",
                "Ito", "Yamamoto", "Nakamura", "Kobayashi", "Kato", "Yoshida", "Yamada", "Sasaki", "Yamaguchi", "Saito",
                "Matsumoto", "Inoue", "Kimura", "Hayashi", "Shimizu", "Yamazaki", "Mori", "Abe", "Ikeda", "Hashimoto",

                "Chen", "Wang", "Li", "Zhang", "Liu", "Yang", "Huang", "Zhao", "Wu", "Zhou",
                "Xu", "Sun", "Ma", "Zhu", "Hu", "Guo", "He", "Gao", "Lin", "Luo",
                "Zheng", "Liang", "Xie", "Song", "Tang", "Xu", "Deng", "Han", "Feng", "Cao",

                "Mohammed", "Ahmed", "Ali", "Omar", "Hassan", "Ibrahim", "Youssef", "Mahmoud", "Mustafa", "Abdullah",
                "Khalid", "Saeed", "Rashid", "Tariq", "Nasser", "Adel", "Faisal", "Jamal", "Karim", "Salim",
                "Fahad", "Majid", "Sami", "Waleed", "Zaid", "Hamza", "Imran", "Bilal", "Usman", "Amir",

                "Raj", "Arun", "Suresh", "Ravi", "Prakash", "Ashok", "Vijay", "Sanjay", "Ajay", "Manoj",
                "Deepak", "Rahul", "Amit", "Rohit", "Ankit", "Nitin", "Sachin", "Vishal", "Pradeep", "Naveen",
                "Kiran", "Dev", "Arjun", "Krishna", "Ram", "Shyam", "Gopal", "Mohan", "Hari", "Ganesh"
        );

        List<String> defaultFemaleNames = Arrays.asList(
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
                "Susan", "Taylor", "Teresa", "Victoria", "Zoe",

                "Adele", "Adrienne", "Agatha", "Aileen", "Aimee", "Alaina", "Alexa", "Alexia", "Alison", "Allegra",
                "Alyssa", "Amber", "Anastasia", "Andrea", "Angel", "Angelica", "Angie", "Anita", "Annabelle", "Antonia",
                "April", "Ariel", "Ashlee", "Athena", "Aubrey", "Audrey", "Barbara", "Bernadette", "Beverly", "Bridget",
                "Brittany", "Brooke", "Candice", "Cara", "Cassandra", "Cecilia", "Celeste", "Chanel", "Charity", "Chelsea",
                "Cheryl", "Christine", "Cindy", "Claire", "Colleen", "Courtney", "Crystal", "Daisy", "Dawn", "Deborah",
                "Delilah", "Denise", "Destiny", "Diamond", "Dolores", "Donna", "Dorothy", "Eden", "Edith", "Eileen",
                "Elaine", "Eleanor", "Elise", "Ella", "Ellen", "Eloise", "Elsie", "Emerald", "Erin", "Esther",
                "Eugenie", "Evelyn", "Faye", "Francesca", "Gina", "Giselle", "Gloria", "Haley", "Harmony", "Heather",
                "Heidi", "Hope", "Ingrid", "Irene", "Iris", "Isabel", "Ivy", "Jackie", "Jacqueline", "Jade",
                "Jane", "Janet", "Janice", "Jean", "Jenna", "Jenny", "Jill", "Joan", "Joanna", "Jocelyn",
                "Josephine", "Joy", "Joyce", "Judith", "Judy", "Julie", "June", "Karen", "Kate", "Katelyn",
                "Kathy", "Katie", "Kelly", "Kimberly", "Kirsten", "Kristen", "Kylie", "Lana", "Lauren", "Lily",
                "Lola", "Lorraine", "Lucy", "Lydia", "Lynn", "Mackenzie", "Madeline", "Mae", "Maggie", "Malibu",
                "Margaret", "Marie", "Marilyn", "Marina", "Marjorie", "Mary", "Matilda", "Maya", "Megan", "Melanie",
                "Melissa", "Mia", "Michaela", "Miranda", "Monica", "Morgan", "Nancy", "Naomi", "Nora", "Norma",
                "Paige", "Pamela", "Paris", "Paula", "Penny", "Phoebe", "Priscilla", "Quinn", "Rita", "Robin",
                "Rosalie", "Roxanne", "Ruby", "Sabrina", "Sally", "Sandra", "Sara", "Scarlett", "Selena", "Serena",
                "Shannon", "Sharon", "Sheila", "Shelby", "Shirley", "Sienna", "Sierra", "Skylar", "Sonia", "Stacy",
                "Stella", "Summer", "Sydney", "Tamara", "Tara", "Tiffany", "Tina", "Tracy", "Trinity", "Valerie",
                "Vanessa", "Veronica", "Violet", "Virginia", "Vivian", "Wendy", "Whitney", "Willow", "Ximena", "Yasmin",
                "Yvonne", "Zelda",

                "Adelheid", "Anneliese", "Astrid", "Bertha", "Brunhilde", "Dagmar", "Erika", "Frieda", "Gerta", "Gisela",
                "Greta", "Gudrun", "Hedwig", "Helga", "Hilda", "Ilse", "Ingeborg", "Irma", "Liesl", "Lorelei",
                "Margot", "Marlene", "Marta", "Petra", "Sabine", "Sigrid", "Ursula", "Waltraud", "Wilhelmina",

                "Brigitte", "Camille", "Celeste", "Chantal", "Claire", "Colette", "Delphine", "Dominique", "Estelle", "Francoise",
                "Genevieve", "Helene", "Isabelle", "Jacqueline", "Jeanne", "Juliette", "Madeleine", "Marie", "Martine", "Monique",
                "Nathalie", "Nicole", "Odette", "Pascale", "Paulette", "Simone", "Solange", "Stephanie", "Sylvie", "Yvette",

                "Alessandra", "Antonella", "Benedetta", "Carlotta", "Caterina", "Chiara", "Daniela", "Elisabetta", "Federica", "Francesca",
                "Giorgia", "Giulia", "Ilaria", "Laura", "Lucia", "Margherita", "Martina", "Paola", "Roberta", "Serena",
                "Silvia", "Stefania", "Valentina", "Valeria", "Veronica",

                "Alejandra", "Amparo", "Ana", "Andrea", "Angela", "Antonia", "Beatriz", "Carmen", "Catalina", "Cecilia",
                "Clara", "Concepcion", "Cristina", "Dolores", "Elena", "Esperanza", "Francisca", "Gloria", "Guadalupe", "Isabel",
                "Josefa", "Juana", "Laura", "Lourdes", "Lucia", "Manuela", "Margarita", "Maria", "Mercedes", "Montserrat",
                "Nuria", "Paloma", "Pilar", "Raquel", "Rosa", "Rosario", "Silvia", "Soledad", "Teresa", "Veronica",

                "Aiko", "Akiko", "Asuka", "Ayako", "Chiaki", "Emiko", "Fumiko", "Hanako", "Haruko", "Hiroko",
                "Junko", "Kazuko", "Keiko", "Kimiko", "Kyoko", "Machiko", "Mariko", "Mayumi", "Michiko", "Midori",
                "Miki", "Noriko", "Reiko", "Sachiko", "Sakura", "Sayuri", "Shizuko", "Takako", "Tomoko", "Yoko",
                "Yoshiko", "Yuki", "Yukiko", "Yumiko",

                "Ai", "Chen", "Hong", "Hua", "Hui", "Jing", "Jun", "Lei", "Li",
                "Ling", "Liu", "Mei", "Min", "Na", "Ning", "Ping", "Qing", "Rui", "Shan",
                "Shu", "Ting", "Wang", "Wei", "Xia", "Xin", "Yan", "Yang", "Ying", "Yu",
                "Yue", "Yun", "Zhen", "Zhi",

                "Amal", "Amina", "Asma", "Dina", "Fatima", "Hala", "Hanan", "Hind", "Iman", "Khadija",
                "Laila", "Lina", "Maha", "Mariam", "May", "Mona", "Nada", "Nadia", "Najla", "Nour",
                "Rana", "Rania", "Reem", "Rima", "Sahar", "Salma", "Sara", "Sawsan", "Suha", "Widad",
                "Yasmin", "Zahra", "Zeinab",

                "Ananya", "Anjali", "Anita", "Asha", "Deepika", "Devi", "Geeta", "Indira", "Kamala", "Kavya",
                "Lakshmi", "Leela", "Madhuri", "Maya", "Meera", "Nandini", "Nisha", "Pooja", "Priya", "Radha",
                "Rakhi", "Rani", "Rashmi", "Ritu", "Riya", "Sita", "Sneha", "Sonia", "Sunita", "Usha",
                "Vandana", "Vidya", "Vineeta",

                "Anastasia", "Anna", "Ekaterina", "Elena", "Galina", "Irina", "Larisa", "Lyudmila", "Marina", "Natasha",
                "Olga", "Svetlana", "Tatiana", "Valentina", "Vera", "Yelena", "Yulia",

                "Anna", "Astrid", "Birgit", "Britt", "Ebba", "Elsa", "Emma", "Ester", "Eva", "Freja",
                "Greta", "Hanna", "Hedvig", "Ida", "Ingrid", "Johanna", "Kajsa", "Klara", "Lena", "Linnea",
                "Lisa", "Maja", "Maria", "Mia", "Sara", "Sigrid", "Sofia", "Stina", "Tove", "Ulrika",

                "Agnes", "Amelia", "Anne", "Astrid", "Berit", "Birgitte", "Bodil", "Camilla", "Caroline", "Charlotte",
                "Dorthe", "Else", "Emma", "Eva", "Freja", "Gitte", "Hanne", "Helene", "Ida", "Inge",
                "Ingrid", "Janne", "Karen", "Kirsten", "Lene", "Line", "Lise", "Lone", "Louise", "Maja",
                "Maria", "Mette", "Pia", "Rikke", "Signe", "Sofie", "Susanne", "Tine", "Tove", "Vibeke",

                "Aaliyah", "Abbie", "Adelaide", "Ailish", "Aine", "Aoife", "Bronagh", "Caoimhe", "Ciara", "Clodagh",
                "Deirdre", "Eimear", "Fiona", "Grainne", "Maeve", "Niamh", "Orla", "Roisin", "Saoirse", "Siobhan",
                "Sinead", "Una"
        );

        DEFAULT_NAMES.put(Gender.MALE, defaultMaleNames);
        DEFAULT_NAMES.put(Gender.FEMALE, defaultFemaleNames);

        NAMES_BY_GENDER.put(Gender.MALE, new ArrayList<>(defaultMaleNames));
        NAMES_BY_GENDER.put(Gender.FEMALE, new ArrayList<>(defaultFemaleNames));
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
    }
}