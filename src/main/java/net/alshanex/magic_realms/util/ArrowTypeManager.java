package net.alshanex.magic_realms.util;

import net.alshanex.magic_realms.Config;
import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ArrowTypeManager {
    private static final List<Item> CACHED_ARROWS = new ArrayList<>();
    private static boolean initialized = false;

    public static void initializeArrowTypes() {
        if (initialized) return;

        CACHED_ARROWS.clear();

        for (Item item : BuiltInRegistries.ITEM) {
            try {
                ItemStack stack = new ItemStack(item);

                // Verificar si está en el tag de flechas de Minecraft
                if (stack.is(ItemTags.ARROWS)) {
                    CACHED_ARROWS.add(item);
                    ResourceLocation itemLocation = BuiltInRegistries.ITEM.getKey(item);
                    MagicRealms.LOGGER.debug("Found arrow via ItemTags.ARROWS: {}", itemLocation);
                    continue;
                }

                // Verificar si es una instancia de ArrowItem
                if (item instanceof ArrowItem) {
                    CACHED_ARROWS.add(item);
                    ResourceLocation itemLocation = BuiltInRegistries.ITEM.getKey(item);
                    MagicRealms.LOGGER.debug("Found arrow via ArrowItem instance: {}", itemLocation);
                    continue;
                }

                ResourceLocation itemLocation = BuiltInRegistries.ITEM.getKey(item);
                if (isArrowByName(itemLocation)) {
                    CACHED_ARROWS.add(item);
                    MagicRealms.LOGGER.debug("Found arrow via name matching: {}", itemLocation);
                }

            } catch (Exception e) {
                // Ignorar errores al procesar items individuales
                MagicRealms.LOGGER.debug("Error processing item for arrow detection: {}", e.getMessage());
            }
        }

        // Asegurar que tengamos al menos las flechas vanilla
        if (!CACHED_ARROWS.contains(Items.ARROW)) {
            CACHED_ARROWS.add(Items.ARROW);
        }
        if (!CACHED_ARROWS.contains(Items.SPECTRAL_ARROW)) {
            CACHED_ARROWS.add(Items.SPECTRAL_ARROW);
        }

        initialized = true;
        MagicRealms.LOGGER.info("Initialized arrow types cache with {} arrow types", CACHED_ARROWS.size());

        String arrowList = CACHED_ARROWS.stream()
                .map(item -> BuiltInRegistries.ITEM.getKey(item).toString())
                .collect(Collectors.joining(", "));
        MagicRealms.LOGGER.debug("Available arrows: [{}]", arrowList);
    }

    private static boolean isArrowByName(ResourceLocation itemLocation) {
        String itemName = itemLocation.getPath().toLowerCase();
        String namespace = itemLocation.getNamespace();

        // No procesar flechas vanilla aquí (ya las tenemos)
        if (namespace.equals("minecraft")) {
            return false;
        }

        // Criterios para identificar flechas por nombre
        boolean nameContainsArrow = itemName.contains("arrow");
        boolean nameContainsBolt = itemName.contains("bolt");
        boolean nameContainsDart = itemName.contains("dart");

        // Verificar si es de mods conocidos que añaden flechas
        boolean isKnownModArrow = isFromKnownArrowMod(namespace, itemName);

        return nameContainsArrow || nameContainsBolt || nameContainsDart || isKnownModArrow;
    }

    private static boolean isFromKnownArrowMod(String namespace, String itemName) {
        // Lista de mods conocidos que añaden flechas
        List<String> knownArrowMods = List.of(
                "supplementaries", "quark", "alexsmobs", "twilightforest",
                "betterarchery", "spartan_weaponry", "tinkers_construct",
                "projectile_damage", "additional_additions", "atmospheric",
                "upgrade_aquatic", "environmental", "autumnity", "bamboo_blocks",
                "buzzier_bees", "enhanced_mushrooms", "windswept", "irons_spellbooks"
        );

        if (knownArrowMods.contains(namespace)) {
            return true;
        }

        // Palabras clave adicionales para detectar proyectiles
        List<String> projectileKeywords = List.of(
                "projectile", "ammunition", "ammo", "javelin", "spear"
        );

        return projectileKeywords.stream().anyMatch(itemName::contains);
    }

    public static ItemStack getRandomArrow(RandomSource random) {
        if (!initialized) {
            initializeArrowTypes();
        }

        if (CACHED_ARROWS.isEmpty()) {
            MagicRealms.LOGGER.warn("No arrows found in cache, using default arrow");
            return new ItemStack(Items.ARROW);
        }

        Item selectedArrow = CACHED_ARROWS.get(random.nextInt(CACHED_ARROWS.size()));
        return new ItemStack(selectedArrow);
    }

    public static ItemStack getWeightedRandomArrow(RandomSource random) {
        if (!initialized) {
            initializeArrowTypes();
        }

        if (CACHED_ARROWS.isEmpty()) {
            return new ItemStack(Items.ARROW);
        }

        // Verificar configuración si está disponible
        double vanillaChance = 0.6; // Por defecto 60%
        try {
            // Usar la configuración integrada
            if (Config.enableRandomArrows) {
                vanillaChance = Config.vanillaArrowChance;
            }
        } catch (Exception e) {
            // Ignorar si Config no está disponible
        }

        // Probabilidad de flecha vanilla
        if (random.nextFloat() < vanillaChance) {
            List<Item> vanillaArrows = CACHED_ARROWS.stream()
                    .filter(item -> BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("minecraft"))
                    .toList();

            if (!vanillaArrows.isEmpty()) {
                return new ItemStack(vanillaArrows.get(random.nextInt(vanillaArrows.size())));
            }
        }

        // Probabilidad de flecha de mod
        List<Item> modArrows = CACHED_ARROWS.stream()
                .filter(item -> !BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("minecraft"))
                .toList();

        if (modArrows.isEmpty()) {
            return new ItemStack(Items.ARROW);
        }

        Item selectedArrow = modArrows.get(random.nextInt(modArrows.size()));
        return new ItemStack(selectedArrow);
    }

    /**
     * Obtiene una flecha basada en el nivel de estrella de la entidad
     */
    public static ItemStack getArrowByStarLevel(int starLevel, RandomSource random) {
        if (!initialized) {
            initializeArrowTypes();
        }

        // Verificar si las flechas aleatorias están habilitadas
        try {
            if (!Config.enableRandomArrows) {
                return new ItemStack(Items.ARROW);
            }

            if (!net.alshanex.magic_realms.Config.starLevelAffectsArrows) {
                return getWeightedRandomArrow(random);
            }
        } catch (Exception e) {
            // Ignorar si Config no está disponible
        }

        return switch (starLevel) {
            case 1 -> {
                // 1 estrella: principalmente flechas básicas
                if (random.nextFloat() < 0.8f) {
                    yield new ItemStack(Items.ARROW);
                } else {
                    yield getRandomArrow(random);
                }
            }
            case 2 -> {
                // 2 estrellas: mezcla equilibrada
                if (random.nextFloat() < 0.4f) {
                    yield new ItemStack(Items.ARROW);
                } else {
                    yield getWeightedRandomArrow(random);
                }
            }
            case 3 -> {
                // 3 estrellas: principalmente flechas especiales
                if (random.nextFloat() < 0.2f) {
                    yield new ItemStack(Items.ARROW);
                } else {
                    yield getWeightedRandomArrow(random);
                }
            }
            default -> new ItemStack(Items.ARROW);
        };
    }

    /**
     * Verifica si un item específico está en la lista negra
     */
    private static boolean isBlacklisted(Item item) {
        try {
            ResourceLocation itemLocation = BuiltInRegistries.ITEM.getKey(item);
            String itemId = itemLocation.toString();

            List<? extends String> blacklist = net.alshanex.magic_realms.Config.blacklistedArrows;
            return blacklist.contains(itemId);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Verifica si un mod está en la lista blanca
     */
    private static boolean isWhitelisted(String namespace) {
        try {
            List<? extends String> whitelist = net.alshanex.magic_realms.Config.whitelistedMods;
            // Si la lista blanca está vacía, todos los mods están permitidos
            return whitelist.isEmpty() || whitelist.contains(namespace);
        } catch (Exception e) {
            return true; // Por defecto permitir si hay error
        }
    }

    /**
     * Reinicia y recarga la cache de flechas
     */
    public static void reloadArrowTypes() {
        initialized = false;
        CACHED_ARROWS.clear();
        initializeArrowTypes();
    }

    /**
     * Obtiene la lista de todas las flechas disponibles (para debug)
     */
    public static List<Item> getAllAvailableArrows() {
        if (!initialized) {
            initializeArrowTypes();
        }
        return new ArrayList<>(CACHED_ARROWS);
    }

    /**
     * Obtiene el número de tipos de flecha disponibles
     */
    public static int getAvailableArrowCount() {
        if (!initialized) {
            initializeArrowTypes();
        }
        return CACHED_ARROWS.size();
    }

    /**
     * Obtiene información detallada sobre las flechas disponibles (para debug)
     */
    public static String getArrowInfo() {
        if (!initialized) {
            initializeArrowTypes();
        }

        StringBuilder info = new StringBuilder();
        info.append("Arrow Types Available: ").append(CACHED_ARROWS.size()).append("\n");

        // Agrupar por namespace
        var arrowsByMod = CACHED_ARROWS.stream()
                .collect(Collectors.groupingBy(
                        item -> BuiltInRegistries.ITEM.getKey(item).getNamespace()
                ));

        for (var entry : arrowsByMod.entrySet()) {
            info.append("  ").append(entry.getKey()).append(": ");
            info.append(entry.getValue().stream()
                    .map(item -> BuiltInRegistries.ITEM.getKey(item).getPath())
                    .collect(Collectors.joining(", ")));
            info.append("\n");
        }

        return info.toString();
    }
}
