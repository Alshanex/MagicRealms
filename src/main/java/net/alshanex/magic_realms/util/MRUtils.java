package net.alshanex.magic_realms.util;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.item.weapons.StaffItem;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.alshanex.magic_realms.entity.RandomHumanEntity;
import net.alshanex.magic_realms.util.humans.EntityClass;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public class MRUtils {
    public static boolean isWeapon(ItemStack stack) {
        return stack.getItem() instanceof SwordItem ||
                stack.getItem() instanceof AxeItem ||
                stack.getItem() instanceof TridentItem;
    }

    public static boolean isRangedWeapon(ItemStack stack) {
        return stack.getItem() instanceof BowItem;
    }

    public static boolean isStaff(ItemStack stack) {
        return stack.getItem() instanceof StaffItem;
    }

    public static boolean isArmorBetter(ArmorItem newArmor, ItemStack currentArmorStack) {
        if (!(currentArmorStack.getItem() instanceof ArmorItem currentArmor)) {
            return true;
        }

        float newValue = newArmor.getDefense() + newArmor.getToughness();
        float currentValue = currentArmor.getDefense() + currentArmor.getToughness();

        return newValue > currentValue;
    }

    public static boolean isWeaponBetter(ItemStack newWeapon, ItemStack currentWeapon, RandomHumanEntity entity) {
        if (!isWeapon(currentWeapon)) {
            return true;
        }

        EntityClass entityClass = entity.getEntityClass();

        if (entityClass == EntityClass.WARRIOR) {
            return getWeaponDamage(newWeapon) > getWeaponDamage(currentWeapon);
        } else if (entityClass == EntityClass.ROGUE && !entity.isArcher()) {
            double newDPS = getWeaponDamage(newWeapon) * getWeaponSpeed(newWeapon);
            double currentDPS = getWeaponDamage(currentWeapon) * getWeaponSpeed(currentWeapon);
            return newDPS > currentDPS;
        }

        return getWeaponDamage(newWeapon) > getWeaponDamage(currentWeapon);
    }

    public static boolean isRangedWeaponBetter(ItemStack newRanged, ItemStack currentRanged) {
        if (!isRangedWeapon(currentRanged)) {
            return true;
        }

        return getRangedWeaponDamage(newRanged) > getRangedWeaponDamage(currentRanged);
    }

    public static boolean isStaffBetter(ItemStack newStaff, ItemStack currentStaff) {
        if (!isStaff(currentStaff)) {
            return true;
        }

        return getStaffSpellPower(newStaff) > getStaffSpellPower(currentStaff);
    }

    public static double getWeaponDamage(ItemStack weapon) {
        if (weapon.getItem() instanceof SwordItem sword) {
            return sword.getDamage(weapon);
        } else if (weapon.getItem() instanceof AxeItem axe) {
            return axe.getDamage(weapon);
        } else if (weapon.getItem() instanceof TridentItem trident) {
            return trident.getDamage(weapon);
        }
        return 1.0;
    }

    public static double getWeaponSpeed(ItemStack weapon) {
        if (weapon.getItem() instanceof SwordItem || weapon.getItem() instanceof AxeItem || weapon.getItem() instanceof TridentItem) {
            Tool toolComponent = weapon.get(DataComponents.TOOL);
            if (toolComponent != null) {
                return toolComponent.defaultMiningSpeed();
            }
        }
        return 1.0;
    }

    public static double getRangedWeaponDamage(ItemStack ranged) {
        if (ranged.getItem() instanceof BowItem) {
            return calculateBowScore(ranged);
        } else if (ranged.getItem() instanceof CrossbowItem crossBow) {
            return crossBow.getDamage(ranged);
        }
        return 1.0;
    }

    public static double calculateBowScore(ItemStack bow) {
        if (!(bow.getItem() instanceof BowItem)) {
            return 0.0;
        }

        double score = 0.0;
        int totalEnchantments = 0;

        // Get all enchantments on the bow
        ItemEnchantments enchantments = bow.getTagEnchantments();

        // Check if bow has any enchantments
        if (enchantments.isEmpty()) {
            return 1.0; // Unenchanted bow gets minimal score
        }

        // Iterate through all enchantments using the correct Entry type
        for (var entry : enchantments.entrySet()) {
            Holder<Enchantment> enchantmentHolder = entry.getKey();
            int level = entry.getIntValue();
            totalEnchantments++;

            // Get the enchantment ID
            String enchantmentId = enchantmentHolder.getKey().location().toString();

            switch (enchantmentId) {
                case "minecraft:power" -> {
                    // Power enchantment gets high priority
                    score += level * 15.0; // Power I = 15, Power V = 75
                }
                case "minecraft:flame" -> {
                    // Flame enchantment gets bonus points regardless of level
                    score += 25.0;
                }
                case "minecraft:infinity" -> {
                    // Infinity is valuable for archers
                    score += 20.0;
                }
                case "minecraft:punch" -> {
                    // Punch adds some value
                    score += level * 5.0;
                }
                case "minecraft:unbreaking" -> {
                    // Unbreaking adds moderate value
                    score += level * 3.0;
                }
                case "minecraft:mending" -> {
                    // Mending is valuable for longevity
                    score += 15.0;
                }
                default -> {
                    // Any other enchantment adds some base value
                    score += level * 2.0;
                }
            }
        }

        // Bonus for having multiple enchantments (prioritize quantity)
        // Each additional enchantment beyond the first adds bonus points
        if (totalEnchantments > 1) {
            score += (totalEnchantments - 1) * 8.0;
        }

        // Base score for having any enchantments at all
        score += 10.0;

        return score;
    }


    public static double getStaffSpellPower(ItemStack staff) {
        if (!isStaff(staff)) {
            return 0.0;
        }

        ItemAttributeModifiers modifiers = staff.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (modifiers == null) {
            return 0.0;
        }

        double spellPower = 0.0;

        // Check for general spell power
        for (var entry : modifiers.modifiers()) {
            if (entry.attribute().equals(AttributeRegistry.SPELL_POWER)) {
                AttributeModifier modifier = entry.modifier();
                if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE) {
                    spellPower += modifier.amount();
                }
            }
        }

        return spellPower;
    }

    public static EquipmentSlot getSlotForArmorType(ArmorItem.Type armorType) {
        return switch (armorType) {
            case HELMET -> EquipmentSlot.HEAD;
            case CHESTPLATE -> EquipmentSlot.CHEST;
            case LEGGINGS -> EquipmentSlot.LEGS;
            case BOOTS -> EquipmentSlot.FEET;
            default -> EquipmentSlot.CHEST;
        };
    }

    public static void autoEquipBetterEquipment(RandomHumanEntity entity) {
        SimpleContainer inventory = entity.getInventory();

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;

            if (shouldAutoEquip(stack, entity)) {
                equipItem(stack, i, entity);
            }
        }
    }

    public static boolean shouldAutoEquip(ItemStack stack, RandomHumanEntity entity) {
        if (stack.getItem() instanceof ArmorItem armorItem) {
            EquipmentSlot slot = getSlotForArmorType(armorItem.getType());
            ItemStack currentArmor = entity.getItemBySlot(slot);
            return currentArmor.isEmpty() || isArmorBetter(armorItem, currentArmor);
        } else if (isWeapon(stack)) {
            ItemStack currentWeapon = entity.getMainHandItem();
            return currentWeapon.isEmpty() || isWeaponBetter(stack, currentWeapon, entity);
        } else if (isRangedWeapon(stack) && entity.getEntityClass() == EntityClass.ROGUE && entity.isArcher()) {
            ItemStack currentWeapon = entity.getMainHandItem();
            return currentWeapon.isEmpty() || isRangedWeaponBetter(stack, currentWeapon);
        } else if (isStaff(stack) && entity.getEntityClass() == EntityClass.MAGE) {
            ItemStack currentWeapon = entity.getMainHandItem();
            return currentWeapon.isEmpty() || isStaffBetter(stack, currentWeapon);
        } else if (stack.getItem() instanceof ShieldItem && entity.getEntityClass() == EntityClass.WARRIOR && entity.hasShield()) {
            ItemStack currentShield = entity.getOffhandItem();
            return currentShield.isEmpty() || !(currentShield.getItem() instanceof ShieldItem);
        }

        return false;
    }

    public static void equipItem(ItemStack newItem, int inventorySlot, RandomHumanEntity entity) {
        ItemStack oldItem = ItemStack.EMPTY;

        if (newItem.getItem() instanceof ArmorItem armorItem) {
            EquipmentSlot slot = getSlotForArmorType(armorItem.getType());
            oldItem = entity.getItemBySlot(slot);
            entity.setItemSlot(slot, newItem.copy());
        } else if (isWeapon(newItem) || isRangedWeapon(newItem) || isStaff(newItem)) {
            oldItem = entity.getMainHandItem();
            entity.setItemSlot(EquipmentSlot.MAINHAND, newItem.copy());
        } else if (newItem.getItem() instanceof ShieldItem) {
            oldItem = entity.getOffhandItem();
            entity.setItemSlot(EquipmentSlot.OFFHAND, newItem.copy());
        }

        // Put old item back in inventory slot
        entity.getInventory().setItem(inventorySlot, oldItem);
    }

    public static void removeItemsFromInventory(SimpleContainer inventory, Item item, int count) {
        int remaining = count;
        for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() == item) {
                int toRemove = Math.min(remaining, stack.getCount());
                stack.shrink(toRemove);
                remaining -= toRemove;
                if (stack.isEmpty()) {
                    inventory.setItem(i, ItemStack.EMPTY);
                }
            }
        }
    }
}