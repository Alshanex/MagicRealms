package net.alshanex.magic_realms.util.humans.mercenaries.personality;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.PersonalityData;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.minecraft.util.RandomSource;

import java.util.Set;

/**
 * Entry point for stamping personality onto a mercenary.
 *
 * For exclusive mercenaries with a fixed archetype (Catas, Amadeus, etc.) the entity's {@link AbstractMercenaryEntity#getFixedPersonality} hook returns
 * non-null and we use that preset instead of rolling.
 */
public final class PersonalityInitializer {

    private PersonalityInitializer() {}

    public static void initializeFor(AbstractMercenaryEntity entity, RandomSource random) {
        PersonalityData data = entity.getData(MRDataAttachments.PERSONALITY);
        if (data.isInitialized()) return;

        FixedPersonality fixed = entity.getFixedPersonality();
        if (fixed != null) {
            data.initialize(
                    fixed.archetype(),
                    fixed.favoriteFoodTagId(),
                    fixed.dislikedFoodTagId(),
                    fixed.hobbyId(),
                    fixed.hometown(),
                    fixed.birthdayDayOfYear(),
                    fixed.quirks()
            );
            return;
        }

        // Random roll path
        PersonalityArchetype archetype = PersonalityArchetype.roll(entity.getEntityClass(), random);
        String favoriteTag = FoodPreferenceRoller.rollFavoriteTagId(entity.getEntityClass(), random);
        String dislikedTag = FoodPreferenceRoller.rollDislikedTagId(favoriteTag, random);
        String hobbyId = rollHobbyId(random);
        String hometown = HometownRoller.roll(random);
        int birthday = random.nextInt(365);
        Set<Quirk> quirks = Quirk.rollSet(random);

        data.initialize(archetype, favoriteTag, dislikedTag, hobbyId, hometown, birthday, quirks);
    }

    /**
     * Pick a random hobby ID from the server catalog. Returns null if the catalog is empty (datapack load failure).
     */
    private static String rollHobbyId(RandomSource random) {
        HobbyCatalog catalog = HobbyCatalogHolder.server();
        if (catalog.isEmpty()) {
            MagicRealms.LOGGER.warn("Hobby catalog empty during personality init - mercenary will have no hobby");
            return null;
        }
        Hobby pick = catalog.pickRandom(random);
        return pick != null ? pick.id() : null;
    }

    /**
     * Preset for exclusive mercenaries. Stores tag IDs and hobby IDs as strings so exclusive overrides can reference tags that might be added by
     * datapacks without needing compile-time TagKey references.
     */
    public record FixedPersonality(
            PersonalityArchetype archetype,
            String favoriteFoodTagId,
            String dislikedFoodTagId,
            String hobbyId,
            String hometown,
            int birthdayDayOfYear,
            Set<Quirk> quirks
    ) {}
}
