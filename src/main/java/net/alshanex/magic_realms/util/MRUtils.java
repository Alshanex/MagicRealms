package net.alshanex.magic_realms.util;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.item.SpellBook;
import io.redspace.ironsspellbooks.item.weapons.StaffItem;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.KillTrackerData;
import net.alshanex.magic_realms.entity.RandomHumanEntity;
import net.alshanex.magic_realms.network.*;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.util.humans.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

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

    public static boolean isSpellbook(ItemStack stack) {
        return stack.getItem() instanceof SpellBook;
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

    public static boolean isSpellbookBetter(ItemStack newSpellbook, ItemStack currentSpellbook, RandomHumanEntity entity) {
        if (!isSpellbook(currentSpellbook)) {
            return true;
        }

        return getSpellbookScore(newSpellbook, entity) > getSpellbookScore(currentSpellbook, entity);
    }

    public static double getSpellbookScore(ItemStack spellbook, RandomHumanEntity entity) {
        if (!isSpellbook(spellbook)) {
            return 0.0;
        }

        ISpellContainer container = ISpellContainer.get(spellbook);
        if (container == null || container.isEmpty()) {
            return 1.0; // Empty spellbook gets minimal score
        }

        double score = 0.0;
        List<SpellData> spells = container.getActiveSpells().stream()
                .map(SpellSlot::spellData)
                .toList();

        for (SpellData spellData : spells) {
            AbstractSpell spell = spellData.getSpell();
            int level = spellData.getLevel();

            // Base score for having a spell
            score += 10.0;

            // Score based on spell level (higher level = better)
            score += level * 5.0;

            // Score based on spell rarity
            SpellRarity rarity = spell.getRarity(level);
            score += switch (rarity) {
                case COMMON -> 5.0;
                case UNCOMMON -> 15.0;
                case RARE -> 30.0;
                case EPIC -> 50.0;
                case LEGENDARY -> 75.0;
            };

            // Bonus score if the spell matches mage's schools
            if (entity.getEntityClass() == EntityClass.MAGE) {
                if (entity.hasSchool(spell.getSchoolType())) {
                    score += 20.0; // Significant bonus for matching school
                }
            }

            // Additional score for spell power
            float spellPower = spell.getSpellPower(level, entity);
            score += spellPower * 0.5;
        }

        // Bonus for having multiple spells
        int spellCount = spells.size();
        if (spellCount > 1) {
            score += (spellCount - 1) * 15.0;
        }

        return score;
    }

    /**
     * Calculate total weapon damage including enchantments
     */
    public static double getWeaponDamage(ItemStack weapon) {
        if (!isWeapon(weapon)) {
            return 1.0;
        }

        // Get base damage from item attributes
        double baseDamage = getBaseWeaponDamage(weapon);

        // Add enchantment damage by simulating enchantment effects
        double enchantmentDamage = getEnchantmentDamageBonus(weapon);

        return baseDamage + enchantmentDamage;
    }

    /**
     * Get base weapon damage from item attributes
     */
    public static double getBaseWeaponDamage(ItemStack weapon) {
        ItemAttributeModifiers modifiers = weapon.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (modifiers == null) {
            return 1.0;
        }

        double damage = 1.0; // Base damage (fist damage)

        for (var entry : modifiers.modifiers()) {
            if (entry.attribute().equals(Attributes.ATTACK_DAMAGE)) {
                AttributeModifier modifier = entry.modifier();
                if (modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                    damage += modifier.amount();
                } else if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE) {
                    damage *= (1.0 + modifier.amount());
                } else if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                    damage *= (1.0 + modifier.amount());
                }
            }
        }

        return damage;
    }

    /**
     * Calculate damage bonus from enchantments by checking their damage effects
     */
    public static double getEnchantmentDamageBonus(ItemStack weapon) {
        ItemEnchantments enchantments = weapon.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchantments.isEmpty()) {
            return 0.0;
        }

        double totalDamageBonus = 0.0;

        for (var entry : enchantments.entrySet()) {
            Holder<Enchantment> enchantmentHolder = entry.getKey();
            int level = entry.getIntValue();
            Enchantment enchantment = enchantmentHolder.value();

            // Check if this enchantment has damage effects
            var damageEffects = enchantment.getEffects(EnchantmentEffectComponents.DAMAGE);
            if (!damageEffects.isEmpty()) {
                // Calculate damage bonus for this enchantment
                for (var conditionalEffect : damageEffects) {
                    var effect = conditionalEffect.effect();

                    // Simulate the damage calculation with a dummy entity
                    org.apache.commons.lang3.mutable.MutableFloat damageValue = new org.apache.commons.lang3.mutable.MutableFloat(0.0f);

                    // Try to get the damage value from the effect
                    try {
                        // Create a minimal random source for the calculation
                        net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create(0);
                        float calculatedDamage = effect.process(level, random, 0.0f);
                        totalDamageBonus += calculatedDamage;
                    } catch (Exception e) {
                        // Fall back to reasonable estimates
                        totalDamageBonus += estimateDamageBonus(enchantmentHolder, level);
                    }
                }
            }
        }

        return totalDamageBonus;
    }

    private static double estimateDamageBonus(Holder<Enchantment> enchantmentHolder, int level) {
        String enchantmentId = enchantmentHolder.getKey().location().toString();

        // Provide reasonable estimates for common damage enchantments
        return switch (enchantmentId) {
            case "minecraft:sharpness" -> 1.0 + (level - 1) * 0.5; // Sharpness formula
            case "minecraft:smite", "minecraft:bane_of_arthropods", "minecraft:impaling" -> level * 2.5;
            default -> level * 1.0; // Generic estimate for unknown damage enchantments
        };
    }

    public static double getWeaponSpeed(ItemStack weapon) {
        if (!isWeapon(weapon)) {
            return 1.0;
        }

        // Get base attack speed from item attributes
        double attackSpeed = getBaseWeaponSpeed(weapon);

        return attackSpeed;
    }

    public static double getBaseWeaponSpeed(ItemStack weapon) {
        ItemAttributeModifiers modifiers = weapon.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (modifiers == null) {
            return 0.0;
        }

        double speed = 4.0; // Base attack speed

        for (var entry : modifiers.modifiers()) {
            if (entry.attribute().equals(Attributes.ATTACK_SPEED)) {
                AttributeModifier modifier = entry.modifier();
                if (modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                    speed += modifier.amount();
                } else if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE) {
                    speed *= (1.0 + modifier.amount());
                } else if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                    speed *= (1.0 + modifier.amount());
                }
            }
        }

        return speed;
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
        if (stack.isEmpty()) return false;

        // Don't equip equipped items
        if (isEquipped(stack, entity)) return false;

        EntityClass entityClass = entity.getEntityClass();

        // Archers shouldn't sell arrows
        if (entityClass == EntityClass.ROGUE && entity.isArcher() &&
                (stack.getItem() instanceof ArrowItem || stack.is(Items.ARROW))) {
            return false;
        }

        // Don't sell emeralds (we need them for buying)
        if (stack.is(Items.EMERALD)) {
            return false;
        }

        // Don't sell armor if it's better than what we're wearing AND we need armor
        if (stack.getItem() instanceof ArmorItem armorItem) {
            EquipmentSlot slot = getSlotForArmorType(armorItem.getType());
            ItemStack currentArmor = entity.getItemBySlot(slot);
            return currentArmor.isEmpty() || isArmorBetter(armorItem, currentArmor);
        }

        // Don't sell weapons if they're better than what we're using AND we're a melee class
        if (isWeapon(stack) &&
                (entityClass == EntityClass.WARRIOR || (entityClass == EntityClass.ROGUE && !entity.isArcher()))) {
            ItemStack currentWeapon = entity.getMainHandItem();
            return currentWeapon.isEmpty() || isWeaponBetter(stack, currentWeapon, entity);
        }

        // Don't sell staves if they're better than what we're using AND we're a mage
        if (entityClass == EntityClass.MAGE && isStaff(stack)) {
            ItemStack currentWeapon = entity.getMainHandItem();
            return currentWeapon.isEmpty() || isStaffBetter(stack, currentWeapon);
        }

        // Don't sell ranged weapons if they're better AND we're an archer
        if (entityClass == EntityClass.ROGUE && entity.isArcher() && isRangedWeapon(stack)) {
            ItemStack currentWeapon = entity.getMainHandItem();
            return currentWeapon.isEmpty() || isRangedWeaponBetter(stack, currentWeapon);
        }

        // Don't sell shields if we need them and don't have one
        if (entityClass == EntityClass.WARRIOR && entity.hasShield() && stack.getItem() instanceof ShieldItem) {
            ItemStack currentShield = entity.getOffhandItem();
            return currentShield.isEmpty() || !(currentShield.getItem() instanceof ShieldItem);
        }

        // Spellbook handling for mages
        if (entityClass == EntityClass.MAGE && isSpellbook(stack)) {
            ItemStack currentOffhand = entity.getOffhandItem();
            return currentOffhand.isEmpty() || isSpellbookBetter(stack, currentOffhand, entity);
        }

        return false;
    }

    private static boolean isEquipped(ItemStack stack, RandomHumanEntity entity) {
        return entity.getMainHandItem() == stack ||
                entity.getOffhandItem() == stack ||
                entity.getItemBySlot(EquipmentSlot.HEAD) == stack ||
                entity.getItemBySlot(EquipmentSlot.CHEST) == stack ||
                entity.getItemBySlot(EquipmentSlot.LEGS) == stack ||
                entity.getItemBySlot(EquipmentSlot.FEET) == stack;
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
        } else if (isSpellbook(newItem) && entity.getEntityClass() == EntityClass.MAGE) {
            // Equip spellbook in offhand for mages
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

    public static void handleHumanNameUpdate(Player player, UUID entityUUID, String entityName){
        if (player instanceof ServerPlayer serverPlayer) {
            ServerLevel level = serverPlayer.serverLevel();
            Entity entity = level.getEntity(entityUUID);

            if (entity instanceof RandomHumanEntity humanEntity) {
                // Update the entity name on server side
                humanEntity.setEntityName(entityName);

                MagicRealms.LOGGER.debug("Updated entity {} name to: {}",
                        entityUUID, entityName);
            }
        }
    }

    public static void requestEntityLevel(Player player, UUID entityUUID){
        if (player instanceof ServerPlayer serverPlayer) {
            ServerLevel level = serverPlayer.serverLevel();
            Entity entity = level.getEntity(entityUUID);

            if (entity instanceof RandomHumanEntity humanEntity) {
                KillTrackerData killData = humanEntity.getData(MRDataAttachments.KILL_TRACKER);
                int currentLevel = killData.getCurrentLevel();

                // Send the level back to the client with both ID and UUID
                PacketDistributor.sendToPlayer(serverPlayer,
                        new SyncEntityLevelPacket(humanEntity.getId(), humanEntity.getUUID(), currentLevel));

                MagicRealms.LOGGER.debug("Sent level {} for entity {} (ID: {}) to client",
                        currentLevel, entityUUID, humanEntity.getId());
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void handleEntityLevelSync(int entityId, int level, UUID entityUUID){
        Minecraft mc = Minecraft.getInstance();
        ClientLevel world = mc.level;

        if (world != null) {
            // Use entity ID to get the entity on client side
            Entity entity = world.getEntity(entityId);

            if (entity instanceof RandomHumanEntity humanEntity) {
                // Update the custom name with level first
                humanEntity.updateCustomNameWithLevel(level);

                // Check if we need to generate/update the name
                String entityName = humanEntity.getEntityName();
                boolean needsNameUpdate = entityName == null || entityName.isEmpty();

                if (needsNameUpdate) {
                    // Try to get name from texture config first (for preset textures)
                    humanEntity.updateNameFromTexture();
                    entityName = humanEntity.getEntityName();

                    // If still empty, generate a random name (for layered textures)
                    if (entityName == null || entityName.isEmpty()) {
                        entityName = AdvancedNameManager.getRandomName(humanEntity.getGender());
                        humanEntity.setEntityName(entityName);

                        MagicRealms.LOGGER.debug("Generated random name '{}' for layered texture entity {}",
                                entityName, entityUUID);
                    } else {
                        MagicRealms.LOGGER.debug("Used preset texture name '{}' for entity {}",
                                entityName, entityUUID);
                    }

                    // Send the name back to server
                    PacketDistributor.sendToServer(new UpdateEntityNamePacket(entityUUID, entityName));

                    // Update display name again with the new name
                    humanEntity.updateCustomNameWithLevel(level);
                }

                MagicRealms.LOGGER.debug("Synced entity {} (ID: {}) with level {} and name '{}'",
                        entityUUID, entityId, level, entityName);
            }
        }
    }

    public static void handlePresetTextureNameSync(Player player, UUID entityUUID, String presetTextureName, boolean hasPresetTexture){
        if (player instanceof ServerPlayer serverPlayer) {
            ServerLevel level = serverPlayer.serverLevel();
            Entity entity = level.getEntity(entityUUID);

            if (entity instanceof RandomHumanEntity humanEntity) {
                if (hasPresetTexture && !presetTextureName.isEmpty()) {
                    // Set the preset texture name on server side
                    humanEntity.setEntityName(presetTextureName);

                    MagicRealms.LOGGER.debug("Updated entity {} with preset texture name: {}",
                            entityUUID, presetTextureName);
                } else {
                    MagicRealms.LOGGER.debug("Entity {} does not have a preset texture, name will be generated randomly",
                            entityUUID);
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void syncPresetTextureName(RandomHumanEntity humanEntity) {
        try {
            // Get the texture config which contains preset texture information
            EntityTextureConfig textureConfig = humanEntity.getTextureConfig();

            if (textureConfig != null) {
                String presetTextureName = textureConfig.getTextureName();
                boolean hasPresetTexture = textureConfig.isPresetTexture();

                if (hasPresetTexture && presetTextureName != null && !presetTextureName.isEmpty()) {
                    // Send preset texture name to server
                    PacketDistributor.sendToServer(new SyncPresetTextureNamePacket(
                            humanEntity.getUUID(), presetTextureName, true));

                    MagicRealms.LOGGER.debug("Sent preset texture name '{}' to server for entity {}",
                            presetTextureName, humanEntity.getUUID());
                } else {
                    // Notify server that this entity doesn't have a preset texture
                    PacketDistributor.sendToServer(new SyncPresetTextureNamePacket(
                            humanEntity.getUUID(), "", false));

                    MagicRealms.LOGGER.debug("Entity {} does not have preset texture, notified server",
                            humanEntity.getUUID());
                }
            } else {
                MagicRealms.LOGGER.debug("No texture config available for entity {}, skipping preset name sync",
                        humanEntity.getUUID());
            }
        } catch (Exception e) {
            MagicRealms.LOGGER.error("Error syncing preset texture name for entity {}: {}",
                    humanEntity.getUUID(), e.getMessage());
        }
    }

    public static void handleServerSideTextureUpload(Player player, UUID entityUUID, byte[] textureData, String textureName, boolean isPresetTexture) {
        try {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            ServerLevel level = serverPlayer.serverLevel();

            // Find the entity
            Entity entity = level.getEntity(entityUUID);
            if (!(entity instanceof RandomHumanEntity humanEntity)) {
                MagicRealms.LOGGER.warn("Entity not found or not a RandomHumanEntity: {}", entityUUID);
                return;
            }

            // Save texture to server world directory
            Path worldDir = level.getServer().getWorldPath(LevelResource.ROOT);
            Path textureDir = worldDir.resolve("magic_realms_textures").resolve("entity").resolve("human");
            Files.createDirectories(textureDir);

            Path texturePath = textureDir.resolve(entityUUID + "_complete.png");
            Files.write(texturePath, textureData);

            // Mark entity as having texture
            humanEntity.setHasTexture(true);

            // Update entity name if it's a preset texture
            if (isPresetTexture && !textureName.isEmpty()) {
                humanEntity.setEntityName(textureName);
            }

            // Now distribute this texture to all players tracking this entity
            PacketDistributor.sendToPlayersTrackingEntity(humanEntity,
                    new SyncEntityTexturePacket(entityUUID, humanEntity.getId(), textureData, textureName, isPresetTexture));

            MagicRealms.LOGGER.debug("Received, saved and distributed texture for entity: {} from player: {}",
                    entityUUID, player.getName().getString());

        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to handle texture upload for entity: {}", entityUUID, e);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void handleClientSideTextureGeneration(UUID entityUUID, int genderOrdinal, int entityClassOrdinal) {
        try {
            Gender gender = Gender.values()[genderOrdinal];
            EntityClass entityClass = EntityClass.values()[entityClassOrdinal];

            // FIXED: Create deterministic random source based on entity UUID
            long seed = entityUUID.getMostSignificantBits() ^ entityUUID.getLeastSignificantBits();
            RandomSource deterministicRandom = RandomSource.create(seed);

            // Use deterministic texture generation
            CombinedTextureManager.TextureCreationResult result =
                    CombinedTextureManager.createCompleteTextureWithNameDeterministic(gender, entityClass, deterministicRandom);

            if (result != null && result.getImage() != null) {
                // Convert BufferedImage to byte array
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(result.getImage(), "PNG", baos);
                byte[] textureData = baos.toByteArray();

                // Send texture back to server
                PacketDistributor.sendToServer(new UploadEntityTexturePacket(
                        entityUUID, textureData, result.getTextureName(), result.isPresetTexture()));

                MagicRealms.LOGGER.debug("Generated and uploaded DETERMINISTIC texture for entity: {}", entityUUID);
            }
        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to generate deterministic texture for entity: {}", entityUUID, e);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void handleClientSideTextureSync(UUID entityUUID, int entityId, byte[] textureData, String textureName, boolean isPresetTexture) {
        try {
            // Convert byte array back to BufferedImage
            ByteArrayInputStream bais = new ByteArrayInputStream(textureData);
            BufferedImage texture = ImageIO.read(bais);

            if (texture != null) {
                // Register with DynamicTextureManager
                ResourceLocation location = DynamicTextureManager.registerDynamicTexture(
                        entityUUID.toString(), texture);

                if (location != null) {
                    // CRITICAL: Use entity ID to find and invalidate entity's texture config
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.level != null) {
                        Entity entity = mc.level.getEntity(entityId);
                        if (entity instanceof RandomHumanEntity humanEntity) {
                            humanEntity.invalidateTextureConfig();
                            MagicRealms.LOGGER.debug("Invalidated texture config for entity {} (ID: {})",
                                    entityUUID, entityId);
                        }
                    }

                    // Store in client cache with forced invalidation
                    CombinedTextureManager.cacheReceivedTexture(
                            entityUUID.toString(),
                            location,
                            textureName,
                            isPresetTexture
                    );

                    MagicRealms.LOGGER.debug("Received and cached texture for entity: {} (ID: {})", entityUUID, entityId);
                }
            }
        } catch (IOException e) {
            MagicRealms.LOGGER.error("Failed to process received texture for entity: {}", entityUUID, e);
        }
    }
}