package net.alshanex.magic_realms.util.humans;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.RandomHumanEntity;
import net.alshanex.magic_realms.util.ModTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;

import java.util.*;
import java.util.stream.Collectors;

public class SpellListGenerator {
    public static List<AbstractSpell> generateSpellsForEntity(RandomHumanEntity entity, RandomSource random) {
        EntityClass entityClass = entity.getEntityClass();
        int starLevel = entity.getStarLevel();

        MagicRealms.LOGGER.debug("Generating spells for entity {} (Class: {}, Stars: {})",
                entity.getUUID(), entityClass.getName(), starLevel);

        List<AbstractSpell> spells = switch (entityClass) {
            case MAGE -> generateMageSpells(entity, starLevel, random);
            case WARRIOR -> generateWarriorSpells(starLevel, random);
            case ROGUE -> generateRogueSpells(entity, starLevel, random);
        };

        MagicRealms.LOGGER.debug("Generated {} spells for {} {}: [{}]",
                spells.size(),
                entityClass.getName(),
                entity.getEntityName(),
                spells.stream().map(spell -> spell.getSpellName()).collect(Collectors.joining(", ")));

        return spells;
    }

    private static List<AbstractSpell> generateMageSpells(RandomHumanEntity entity, int starLevel, RandomSource random) {
        List<SchoolType> magicSchools = entity.getMagicSchools();
        if (magicSchools.isEmpty()) {
            MagicRealms.LOGGER.warn("Mage entity {} has no magic schools assigned", entity.getUUID());
            return new ArrayList<>();
        }

        // Determinar cantidad de spells según el nivel de estrellas
        SpellRange range = getMageSpellRange(starLevel);
        int spellCount = range.getRandomCount(random);

        MagicRealms.LOGGER.debug("Mage {} has schools: [{}], generating {} spells",
                entity.getEntityName(),
                magicSchools.stream().map(s -> s.getId().toString()).collect(Collectors.joining(", ")),
                spellCount);

        List<AbstractSpell> availableSpells = new ArrayList<>();

        // Obtener spells de todas las escuelas del mage
        for (SchoolType school : magicSchools) {
            List<AbstractSpell> schoolSpells = SpellRegistry.getSpellsForSchool(school);
            List<AbstractSpell> enabledSchoolSpells = schoolSpells.stream()
                    .filter(spell -> spell.isEnabled() && !ModTags.isSpellInTag(spell, ModTags.SPELL_BLACKLIST))
                    .toList();
            availableSpells.addAll(enabledSchoolSpells);

            MagicRealms.LOGGER.debug("School {} has {} enabled spells: [{}]",
                    school.getId(),
                    enabledSchoolSpells.size(),
                    enabledSchoolSpells.stream().map(AbstractSpell::getSpellName).collect(Collectors.joining(", ")));
        }

        if (availableSpells.isEmpty()) {
            MagicRealms.LOGGER.warn("No available spells found for mage schools: {}",
                    magicSchools.stream().map(s -> s.getId().toString()).collect(Collectors.joining(", ")));
            return new ArrayList<>();
        }

        // Seleccionar spells aleatorios
        return selectRandomSpells(availableSpells, spellCount, random);
    }

    private static List<AbstractSpell> generateWarriorSpells(int starLevel, RandomSource random) {
        SpellRange range = getWarriorSpellRange(starLevel);
        int spellCount = range.getRandomCount(random);

        MagicRealms.LOGGER.debug("Generating {} spells for Warrior (Stars: {})", spellCount, starLevel);

        // Determinar probabilidades de rareza según estrellas
        SpellRarityChances chances = getWarriorRarityChances(starLevel);

        List<AbstractSpell> selectedSpells = new ArrayList<>();

        for (int i = 0; i < spellCount; i++) {
            TagKey<AbstractSpell> selectedTag = selectWarriorSpellTag(chances, random);
            List<AbstractSpell> tagSpells = getSpellsFromTag(selectedTag);

            MagicRealms.LOGGER.debug("Tag {} has {} spells available: [{}]",
                    selectedTag.location(),
                    tagSpells.size(),
                    tagSpells.stream().map(AbstractSpell::getSpellName).collect(Collectors.joining(", ")));

            if (!tagSpells.isEmpty()) {
                AbstractSpell spell = tagSpells.get(random.nextInt(tagSpells.size()));
                if (!selectedSpells.contains(spell)) {
                    selectedSpells.add(spell);
                    MagicRealms.LOGGER.debug("Selected spell: {}", spell.getSpellName());
                } else {
                    // Si el spell ya está seleccionado, intentar con otro
                    i--;
                }
            } else {
                MagicRealms.LOGGER.warn("No spells found for tag: {}", selectedTag.location());
            }
        }

        return selectedSpells;
    }

    private static List<AbstractSpell> generateRogueSpells(RandomHumanEntity entity, int starLevel, RandomSource random) {
        boolean isArcher = entity.isArcher();
        SpellRange range = getRogueSpellRange(starLevel);
        int spellCount = range.getRandomCount(random);

        MagicRealms.LOGGER.debug("Generating {} spells for {} (Stars: {})",
                spellCount,
                isArcher ? "Archer" : "Assassin",
                starLevel);

        // Determinar probabilidades de rareza según estrellas
        SpellRarityChances chances = getRogueRarityChances(starLevel);

        List<AbstractSpell> selectedSpells = new ArrayList<>();

        for (int i = 0; i < spellCount; i++) {
            TagKey<AbstractSpell> selectedTag = selectRogueSpellTag(chances, isArcher, random);
            List<AbstractSpell> tagSpells = getSpellsFromTag(selectedTag);

            MagicRealms.LOGGER.debug("Tag {} has {} spells available: [{}]",
                    selectedTag.location(),
                    tagSpells.size(),
                    tagSpells.stream().map(AbstractSpell::getSpellName).collect(Collectors.joining(", ")));

            if (!tagSpells.isEmpty()) {
                AbstractSpell spell = tagSpells.get(random.nextInt(tagSpells.size()));
                if (!selectedSpells.contains(spell)) {
                    selectedSpells.add(spell);
                    MagicRealms.LOGGER.debug("Selected spell: {}", spell.getSpellName());
                } else {
                    // Si el spell ya está seleccionado, intentar con otro
                    i--;
                }
            } else {
                MagicRealms.LOGGER.warn("No spells found for tag: {}", selectedTag.location());
            }
        }

        return selectedSpells;
    }

    // === MÉTODOS HELPER PARA RANGOS DE SPELLS ===

    private static SpellRange getMageSpellRange(int starLevel) {
        return switch (starLevel) {
            case 1 -> new SpellRange(1, 4);
            case 2 -> new SpellRange(3, 7);
            case 3 -> new SpellRange(5, 10);
            default -> new SpellRange(1, 4);
        };
    }

    private static SpellRange getWarriorSpellRange(int starLevel) {
        return switch (starLevel) {
            case 1 -> new SpellRange(1, 2);
            case 2 -> new SpellRange(2, 4);
            case 3 -> new SpellRange(3, 5);
            default -> new SpellRange(1, 2);
        };
    }

    private static SpellRange getRogueSpellRange(int starLevel) {
        return switch (starLevel) {
            case 1 -> new SpellRange(1, 3);
            case 2 -> new SpellRange(3, 5);
            case 3 -> new SpellRange(5, 7);
            default -> new SpellRange(1, 3);
        };
    }

    // === MÉTODOS HELPER PARA PROBABILIDADES DE RAREZA ===

    private static SpellRarityChances getWarriorRarityChances(int starLevel) {
        return switch (starLevel) {
            case 1 -> new SpellRarityChances(60, 30, 10); // 60% common, 30% rare, 10% legendary
            case 2 -> new SpellRarityChances(30, 60, 10); // 30% common, 60% rare, 10% legendary
            case 3 -> new SpellRarityChances(25, 50, 25); // 25% common, 50% rare, 25% legendary
            default -> new SpellRarityChances(60, 30, 10);
        };
    }

    private static SpellRarityChances getRogueRarityChances(int starLevel) {
        return switch (starLevel) {
            case 1 -> new SpellRarityChances(60, 30, 10); // 60% common, 30% rare, 10% legendary
            case 2 -> new SpellRarityChances(30, 60, 10); // 30% common, 60% rare, 10% legendary
            case 3 -> new SpellRarityChances(25, 50, 25); // 25% common, 50% rare, 25% legendary
            default -> new SpellRarityChances(60, 30, 10);
        };
    }

    // === MÉTODOS HELPER PARA SELECCIÓN DE TAGS ===

    private static TagKey<AbstractSpell> selectWarriorSpellTag(SpellRarityChances chances, RandomSource random) {
        int roll = random.nextInt(100);

        if (roll < chances.common) {
            return ModTags.COMMON_WARRIOR_SPELLS;
        } else if (roll < chances.common + chances.rare) {
            return ModTags.RARE_WARRIOR_SPELLS;
        } else {
            return ModTags.LEGENDARY_WARRIOR_SPELLS;
        }
    }

    private static TagKey<AbstractSpell> selectRogueSpellTag(SpellRarityChances chances, boolean isArcher, RandomSource random) {
        int roll = random.nextInt(100);

        if (isArcher) {
            if (roll < chances.common) {
                return ModTags.COMMON_ARCHER_SPELLS;
            } else if (roll < chances.common + chances.rare) {
                return ModTags.RARE_ARCHER_SPELLS;
            } else {
                return ModTags.LEGENDARY_ARCHER_SPELLS;
            }
        } else {
            if (roll < chances.common) {
                return ModTags.COMMON_ASSASSIN_SPELLS;
            } else if (roll < chances.common + chances.rare) {
                return ModTags.RARE_ASSASSIN_SPELLS;
            } else {
                return ModTags.LEGENDARY_ASSASSIN_SPELLS;
            }
        }
    }

    // === MÉTODOS HELPER UTILS ===

    private static List<AbstractSpell> getSpellsFromTag(TagKey<AbstractSpell> tag) {
        var list = new ArrayList<AbstractSpell>();

        for (var spell : SpellRegistry.getEnabledSpells()) {
            SpellRegistry.REGISTRY.getHolder(spell.getSpellResource()).ifPresent(a -> {
                if (a.is(tag)) {
                    list.add(spell);
                }
            });
        }

        if (list.isEmpty()) {
            MagicRealms.LOGGER.warn("Tag {} contains no spells or doesn't exist", tag.location());
        }

        return list;
    }

    private static List<AbstractSpell> selectRandomSpells(List<AbstractSpell> availableSpells, int count, RandomSource random) {
        if (availableSpells.isEmpty()) {
            return new ArrayList<>();
        }

        List<AbstractSpell> shuffled = new ArrayList<>(availableSpells);
        Collections.shuffle(shuffled, new Random(random.nextLong()));

        return shuffled.stream()
                .limit(Math.min(count, shuffled.size()))
                .collect(Collectors.toList());
    }

    // === CLASES HELPER ===

    private static class SpellRange {
        final int min, max;

        SpellRange(int min, int max) {
            this.min = min;
            this.max = max;
        }

        int getRandomCount(RandomSource random) {
            return min + random.nextInt(max - min + 1);
        }
    }

    private static class SpellRarityChances {
        final int common, rare, legendary;

        SpellRarityChances(int common, int rare, int legendary) {
            this.common = common;
            this.rare = rare;
            this.legendary = legendary;
        }
    }
}
