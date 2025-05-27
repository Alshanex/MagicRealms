package net.alshanex.magic_realms.util;

import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

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

                if (stack.is(ItemTags.ARROWS)) {
                    CACHED_ARROWS.add(item);
                    ResourceLocation itemLocation = BuiltInRegistries.ITEM.getKey(item);
                    MagicRealms.LOGGER.debug("Found arrow via ItemTags.ARROWS: {}", itemLocation);
                    continue;
                }

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
                MagicRealms.LOGGER.debug("Error processing item for arrow detection: {}", e.getMessage());
            }
        }

        if (!CACHED_ARROWS.contains(Items.ARROW)) {
            CACHED_ARROWS.add(Items.ARROW);
        }
        if (!CACHED_ARROWS.contains(Items.SPECTRAL_ARROW)) {
            CACHED_ARROWS.add(Items.SPECTRAL_ARROW);
        }

        initialized = true;
        MagicRealms.LOGGER.info("Initialized arrow types cache with {} arrow types", CACHED_ARROWS.size());

        if (MagicRealms.LOGGER.isDebugEnabled()) {
            String arrowList = CACHED_ARROWS.stream()
                    .map(item -> BuiltInRegistries.ITEM.getKey(item).toString())
                    .collect(Collectors.joining(", "));
            MagicRealms.LOGGER.debug("Available arrows: [{}]", arrowList);
        }
    }

    private static boolean isArrowByName(ResourceLocation itemLocation) {
        String itemName = itemLocation.getPath().toLowerCase();
        String namespace = itemLocation.getNamespace();

        if (namespace.equals("minecraft")) {
            return false;
        }

        boolean nameContainsArrow = itemName.contains("arrow");
        boolean nameContainsBolt = itemName.contains("bolt");
        boolean nameContainsDart = itemName.contains("dart");

        boolean isKnownModArrow = isFromKnownArrowMod(namespace, itemName);

        return nameContainsArrow || nameContainsBolt || nameContainsDart || isKnownModArrow;
    }

    private static boolean isFromKnownArrowMod(String namespace, String itemName) {
        List<String> knownArrowMods = List.of(
                "supplementaries", "quark", "alexsmobs", "twilightforest",
                "betterarchery", "spartan_weaponry", "tinkers_construct",
                "projectile_damage", "additional_additions", "atmospheric",
                "upgrade_aquatic", "environmental", "autumnity", "bamboo_blocks",
                "buzzier_bees", "enhanced_mushrooms", "windswept", "irons_spellbooks"
        );

        return knownArrowMods.contains(namespace);
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

        double vanillaChance = 0.6;
        try {
            if (net.alshanex.magic_realms.Config.enableRandomArrows) {
                vanillaChance = net.alshanex.magic_realms.Config.vanillaArrowChance;
            }
        } catch (Exception e) {
        }

        if (random.nextFloat() < vanillaChance) {
            List<Item> vanillaArrows = CACHED_ARROWS.stream()
                    .filter(item -> BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("minecraft"))
                    .toList();

            if (!vanillaArrows.isEmpty()) {
                return new ItemStack(vanillaArrows.get(random.nextInt(vanillaArrows.size())));
            }
        }

        List<Item> modArrows = CACHED_ARROWS.stream()
                .filter(item -> !BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("minecraft"))
                .toList();

        if (modArrows.isEmpty()) {
            return new ItemStack(Items.ARROW);
        }

        Item selectedArrow = modArrows.get(random.nextInt(modArrows.size()));
        return new ItemStack(selectedArrow);
    }

    public static ItemStack getArrowByStarLevel(int starLevel, RandomSource random) {
        if (!initialized) {
            initializeArrowTypes();
        }

        try {
            if (!net.alshanex.magic_realms.Config.enableRandomArrows) {
                return new ItemStack(Items.ARROW);
            }

            if (!net.alshanex.magic_realms.Config.starLevelAffectsArrows) {
                return getWeightedRandomArrow(random);
            }
        } catch (Exception e) {
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

    public static void reloadArrowTypes() {
        initialized = false;
        CACHED_ARROWS.clear();
        initializeArrowTypes();
    }

    public static List<Item> getAllAvailableArrows() {
        if (!initialized) {
            initializeArrowTypes();
        }
        return new ArrayList<>(CACHED_ARROWS);
    }

    public static int getAvailableArrowCount() {
        if (!initialized) {
            initializeArrowTypes();
        }
        return CACHED_ARROWS.size();
    }

    public static AbstractArrow createArrowByStarLevel(int starLevel, RandomSource random,
                                                       net.minecraft.world.level.Level level, net.minecraft.world.entity.LivingEntity shooter,
                                                       ItemStack weaponStack) {
        ItemStack arrowStack = getArrowByStarLevel(starLevel, random);
        return createArrowFromItemStack(arrowStack, level, shooter, weaponStack);
    }

    public static AbstractArrow createArrowFromItemStack(ItemStack arrowStack,
                                                         net.minecraft.world.level.Level level, net.minecraft.world.entity.LivingEntity shooter,
                                                         ItemStack weaponStack) {

        if (arrowStack.getItem() instanceof net.minecraft.world.item.ArrowItem arrowItem) {
            AbstractArrow arrow = arrowItem.createArrow(level, arrowStack, shooter, weaponStack);

            applyBowEnchantments(arrow, weaponStack, shooter, level);
            return arrow;
        }

        MagicRealms.LOGGER.debug("Item {} is not an ArrowItem, creating default arrow",
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(arrowStack.getItem()));

        AbstractArrow arrow = new net.minecraft.world.entity.projectile.Arrow(level, shooter, arrowStack, weaponStack);
        applyBowEnchantments(arrow, weaponStack, shooter, level);
        return arrow;
    }

    public static void applyBowEnchantments(AbstractArrow arrow, ItemStack bow,
                                            net.minecraft.world.entity.LivingEntity shooter, net.minecraft.world.level.Level level) {

        if (bow.isEmpty() || !bow.isEnchanted()) {
            return;
        }

        try {
            applyPowerEnchantment(arrow, bow, level);
            applyKnockbackEnchantment(arrow, bow, shooter, level);
            applyFlameEnchantment(arrow, bow, level);

            MagicRealms.LOGGER.debug("Applied bow enchantments to arrow. Final damage: {}", arrow.getBaseDamage());

        } catch (Exception e) {
            MagicRealms.LOGGER.error("Error applying bow enchantments to arrow", e);
        }
    }

    private static void applyPowerEnchantment(AbstractArrow arrow, ItemStack bow, Level level) {
        Holder<Enchantment> power = getEnchantmentHolder(level, Enchantments.POWER);
        int powerLevel = EnchantmentHelper.getItemEnchantmentLevel(power, bow);
        if (powerLevel > 0) {
            double extraDamage = 0.5 * powerLevel + 1.0;
            arrow.setBaseDamage(arrow.getBaseDamage() + extraDamage);
        }
    }

    private static void applyKnockbackEnchantment(AbstractArrow arrow, ItemStack bow, LivingEntity shooter, Level level) {
        Holder<Enchantment> punch = getEnchantmentHolder(level, Enchantments.PUNCH);
        int punchLevel = EnchantmentHelper.getItemEnchantmentLevel(punch, bow);
        if (punchLevel > 0) {
            double resistance = Math.max(0.0, 1.0 - shooter.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE));
            net.minecraft.world.phys.Vec3 knockbackVec = arrow.getDeltaMovement().normalize().scale(punchLevel * 0.6 * resistance);
            arrow.push(knockbackVec.x, 0.1, knockbackVec.z);
        }
    }

    private static void applyFlameEnchantment(AbstractArrow arrow, ItemStack bow, Level level) {
        Holder<Enchantment> flame = getEnchantmentHolder(level, Enchantments.FLAME);
        int flameLevel = EnchantmentHelper.getItemEnchantmentLevel(flame, bow);
        if (flameLevel > 0) {
            arrow.igniteForSeconds(5);
        }
    }

    private static net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> getEnchantmentHolder(
            net.minecraft.world.level.Level level,
            net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> enchantmentKey) {
        return level.registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getHolderOrThrow(enchantmentKey);
    }
}
