package net.alshanex.magic_realms.util.humans.mercenaries;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.humans.AbstractMercenaryEntity;
import net.alshanex.magic_realms.util.ModTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;

import java.util.*;
import java.util.stream.Collectors;

public class SpellListGenerator {
    public static List<AbstractSpell> generateSpellsForEntity(AbstractMercenaryEntity entity, RandomSource random) {
        return entity.getCombatClass().generateSpells(entity, random);
    }

    public static List<AbstractSpell> generateMageSpells(AbstractMercenaryEntity entity, int starLevel, RandomSource random) {
        List<SchoolType> magicSchools = entity.getMagicSchools();
        if (magicSchools.isEmpty()) {
            MagicRealms.LOGGER.warn("Mage entity {} has no magic schools assigned", entity.getUUID());
            return new ArrayList<>();
        }

        SpellRange range = getMageSpellRange(starLevel);
        int spellCount = range.getRandomCount(random);

        List<AbstractSpell> availableSpells = new ArrayList<>();
        List<AbstractSpell> availableAttackSpells = new ArrayList<>();

        for (SchoolType school : magicSchools) {
            List<AbstractSpell> schoolSpells = SpellRegistry.getSpellsForSchool(school);
            List<AbstractSpell> enabledSchoolSpells = schoolSpells.stream()
                    .filter(spell -> spell.isEnabled() && !ModTags.isSpellInTag(spell, ModTags.SPELL_BLACKLIST) && ModTags.isSpellInTag(spell, ModTags.MAGE_SPELLS))
                    .toList();

            availableSpells.addAll(enabledSchoolSpells);

            List<AbstractSpell> schoolAttackSpells = ModTags.filterAttackSpells(enabledSchoolSpells);
            availableAttackSpells.addAll(schoolAttackSpells);
        }

        if (availableSpells.isEmpty()) {
            MagicRealms.LOGGER.warn("No available spells found for mage schools: {}",
                    magicSchools.stream().map(s -> s.getId().toString()).collect(Collectors.joining(", ")));
            return new ArrayList<>();
        }

        List<AbstractSpell> selectedSpells = new ArrayList<>();

        if (!availableAttackSpells.isEmpty()) {
            AbstractSpell attackSpell = availableAttackSpells.get(random.nextInt(availableAttackSpells.size()));
            selectedSpells.add(attackSpell);

            availableSpells.remove(attackSpell);
            spellCount--;
        } else {
            MagicRealms.LOGGER.warn("No attack spells available for mage with schools: {}",
                    magicSchools.stream().map(s -> s.getId().toString()).collect(Collectors.joining(", ")));
        }

        if (spellCount > 0 && !availableSpells.isEmpty()) {
            List<AbstractSpell> remainingSpells = selectRandomSpells(availableSpells, spellCount, random);
            selectedSpells.addAll(remainingSpells);
        }

        return selectedSpells;
    }

    public static List<AbstractSpell> generateWarriorSpells(int starLevel, RandomSource random) {
        SpellRange range = getWarriorSpellRange(starLevel);
        int spellCount = range.getRandomCount(random);

        SpellRarityChances chances = getWarriorRarityChances(starLevel);

        List<AbstractSpell> selectedSpells = new ArrayList<>();

        for (int i = 0; i < spellCount; i++) {
            TagKey<AbstractSpell> selectedTag = selectWarriorSpellTag(chances, random);
            List<AbstractSpell> tagSpells = getSpellsFromTag(selectedTag);

            if (!tagSpells.isEmpty()) {
                AbstractSpell spell = tagSpells.get(random.nextInt(tagSpells.size()));
                if (!selectedSpells.contains(spell)) {
                    selectedSpells.add(spell);
                } else {
                    i--;
                }
            } else {
                MagicRealms.LOGGER.warn("No spells found for tag: {}", selectedTag.location());
            }
        }

        return selectedSpells;
    }

    public static List<AbstractSpell> generateRogueSpells(AbstractMercenaryEntity entity, int starLevel, RandomSource random) {
        boolean isArcher = entity.isArcher();
        SpellRange range = getRogueSpellRange(starLevel);
        int spellCount = range.getRandomCount(random);

        SpellRarityChances chances = getRogueRarityChances(starLevel);

        List<AbstractSpell> selectedSpells = new ArrayList<>();

        for (int i = 0; i < spellCount; i++) {
            TagKey<AbstractSpell> selectedTag = selectRogueSpellTag(chances, isArcher, random);
            List<AbstractSpell> tagSpells = getSpellsFromTag(selectedTag);

            if (!tagSpells.isEmpty()) {
                AbstractSpell spell = tagSpells.get(random.nextInt(tagSpells.size()));
                if (!selectedSpells.contains(spell)) {
                    selectedSpells.add(spell);
                } else {
                    i--;
                }
            } else {
                MagicRealms.LOGGER.warn("No spells found for tag: {}", selectedTag.location());
            }
        }

        return selectedSpells;
    }

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

    public static List<AbstractSpell> getSpellsFromTag(TagKey<AbstractSpell> tag) {
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
