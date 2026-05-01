package net.alshanex.magic_realms.util;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.item.SpellBook;
import io.redspace.ironsspellbooks.item.weapons.StaffItem;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.*;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.entity.flying_arrow.FloatingArrowEntity;
import net.alshanex.magic_realms.entity.random.RandomHumanEntity;
import net.alshanex.magic_realms.item.FloatingArrowItem;
import net.alshanex.magic_realms.network.*;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.screens.ContractHumanInfoMenu;
import net.alshanex.magic_realms.screens.ContractInventoryMenu;
import net.alshanex.magic_realms.screens.SkinCustomizerScreen;
import net.alshanex.magic_realms.util.humans.mercenaries.skins_management.SkinCatalog;
import net.alshanex.magic_realms.util.humans.mercenaries.skins_management.SkinCatalogHolder;
import net.alshanex.magic_realms.util.humans.mercenaries.skins_management.SkinPart;
import net.alshanex.magic_realms.util.humans.mercenaries.skins_management.SkinPreset;
import net.alshanex.magic_realms.util.contracts.ContractUtils;
import net.alshanex.magic_realms.util.humans.mercenaries.AdvancedNameManager;
import net.alshanex.magic_realms.util.humans.mercenaries.EntityClass;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
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

    public static boolean isWeaponBetter(ItemStack newWeapon, ItemStack currentWeapon, AbstractMercenaryEntity entity) {
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

    public static boolean isSpellbookBetter(ItemStack newSpellbook, ItemStack currentSpellbook, AbstractMercenaryEntity entity) {
        if (!isSpellbook(currentSpellbook)) {
            return true;
        }

        return getSpellbookScore(newSpellbook, entity) > getSpellbookScore(currentSpellbook, entity);
    }

    public static double getSpellbookScore(ItemStack spellbook, AbstractMercenaryEntity entity) {
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

    public static void autoEquipBetterEquipment(AbstractMercenaryEntity entity) {
        SimpleContainer inventory = entity.getInventory();

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;

            if (shouldAutoEquip(stack, entity)) {
                equipItem(stack, i, entity);
            }
        }
    }

    public static boolean shouldAutoEquip(ItemStack stack, AbstractMercenaryEntity entity) {
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

    private static boolean isEquipped(ItemStack stack, AbstractMercenaryEntity entity) {
        return entity.getMainHandItem() == stack ||
                entity.getOffhandItem() == stack ||
                entity.getItemBySlot(EquipmentSlot.HEAD) == stack ||
                entity.getItemBySlot(EquipmentSlot.CHEST) == stack ||
                entity.getItemBySlot(EquipmentSlot.LEGS) == stack ||
                entity.getItemBySlot(EquipmentSlot.FEET) == stack;
    }

    public static void equipItem(ItemStack newItem, int inventorySlot, AbstractMercenaryEntity entity) {
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

            if (entity instanceof AbstractMercenaryEntity humanEntity) {
                // Update the entity name on server side
                humanEntity.setEntityName(entityName);
            }
        }
    }

    public static void handlePresetNameSync (Player player, UUID entityUUID, String presetName){
        if (player instanceof ServerPlayer serverPlayer) {
            Entity entity = serverPlayer.serverLevel().getEntity(entityUUID);

            if (entity instanceof RandomHumanEntity randomHuman) {
                // Update the entity name on server side
                randomHuman.setEntityName(presetName);

                // Update custom name with level
                randomHuman.updateCustomNameWithStars();
            }
        }
    }

    public static void handleTabSwitch(Player player, String tabName) {
        // Handle special case for switching TO inventory menu
        if ("INVENTORY".equals(tabName)) {
            if (player.containerMenu instanceof ContractHumanInfoMenu attributesMenu) {
                AbstractMercenaryEntity entity = attributesMenu.getEntity();
                if (entity != null) {
                    // Store the entity reference before closing
                    final AbstractMercenaryEntity entityRef = entity;

                    // Close current menu
                    player.closeContainer();

                    // Open new menu immediately in next tick (minimal delay)
                    if (player.level() instanceof ServerLevel serverLevel) {
                        serverLevel.getServer().tell(new net.minecraft.server.TickTask(
                                serverLevel.getServer().getTickCount() + 1, () -> {
                            ContractUtils.openInventoryScreen(player, entityRef);
                        }
                        ));
                    }
                }
            }
            return;
        }

        // Handle normal tab switching within attributes menu (no menu change needed)
        if (player.containerMenu instanceof ContractHumanInfoMenu attributesMenu) {
            try {
                ContractHumanInfoMenu.Tab tab = ContractHumanInfoMenu.Tab.valueOf(tabName);
                attributesMenu.switchToTabServerSide(tab);
            } catch (IllegalArgumentException e) {
                MagicRealms.LOGGER.warn("Invalid tab name received: {}", tabName);
            }
        }
    }

    public static void switchToAttributesScreen(Player player, ContractHumanInfoMenu.Tab tab) {
        if (player.containerMenu instanceof ContractInventoryMenu inventoryMenu) {
            AbstractMercenaryEntity entity = inventoryMenu.getEntity();
            if (entity != null) {
                // Store the entity reference and desired tab
                final AbstractMercenaryEntity entityRef = entity;
                final ContractHumanInfoMenu.Tab targetTab = tab;

                // Close current menu
                player.closeContainer();

                // Open new menu immediately in next tick
                if (player.level() instanceof ServerLevel serverLevel) {
                    serverLevel.getServer().tell(new net.minecraft.server.TickTask(
                            serverLevel.getServer().getTickCount() + 1, () -> {
                        ContractUtils.openAttributesScreen(player, entityRef);

                        // Set the tab in the next tick to ensure menu is ready
                        serverLevel.getServer().tell(new net.minecraft.server.TickTask(
                                serverLevel.getServer().getTickCount() + 2, () -> {
                            if (player.containerMenu instanceof ContractHumanInfoMenu attributesMenu) {
                                attributesMenu.switchToTabServerSide(targetTab);
                            }
                        }
                        ));
                    }
                    ));
                }
            }
        }
    }

    public static void handleSkinCatalogSync(List<SkinPart> parts, List<SkinPreset> presets){
        SkinCatalog catalog = new SkinCatalog(parts, presets);
        SkinCatalogHolder.setClient(catalog);
        MagicRealms.LOGGER.debug("Received skin catalog from server: {} parts, {} presets",
                parts.size(), presets.size());
    }

    @OnlyIn(Dist.CLIENT)
    public static void openTextureCustomizationScreen(OpenSkinCustomizerPacket packet){
        Minecraft.getInstance().setScreen(new SkinCustomizerScreen(packet));
    }

    public static void handleTextureCustomizationSave(Player player, String name, UUID entityUUID, String skin, String clothes, String eyes, String hair){
        if (!(player instanceof ServerPlayer sp)) return;
        if (!sp.getAbilities().instabuild && !sp.hasPermissions(2)) return;

        ServerLevel level = sp.serverLevel();
        Entity e = level.getEntity(entityUUID);
        if (!(e instanceof RandomHumanEntity human)) return;

        CompoundTag metadata = human.getTextureMetadata().copy();
        if (metadata.getBoolean("usePreset")) return;

        if (!skin.isEmpty()) metadata.putString("skinTexture", skin);
        if (!clothes.isEmpty()) metadata.putString("clothesTexture", clothes);
        if (!eyes.isEmpty()) metadata.putString("eyesTexture", eyes);
        if (!hair.isEmpty()) metadata.putString("hairTexture", hair);

        human.setTextureMetadata(metadata);

        // Update name if provided and non-empty
        if (!name.isBlank()) {
            String trimmed = name.trim();
            // cap length to avoid abuse
            if (trimmed.length() > 32) trimmed = trimmed.substring(0, 32);
            human.setEntityName(trimmed);

            // Refresh the visible nameplate with the current level
            var killData = human.getData(MRDataAttachments.KILL_TRACKER);
            human.updateCustomNameWithLevel(killData.getCurrentLevel());
        }
    }

    public static void migrateLegacyNbtIfNeeded(CompoundTag compound, MercenaryIdentity id, ChairSittingData chairData, PatrolData patrolData, FearData fearData) {
        // Star level
        if (compound.contains("StarLevel")) {
            id.setStarLevel(compound.getInt("StarLevel"));
        }
        // Spells-generated / goals-initialized flags
        if (compound.contains("SpellsGenerated")) {
            id.setSpellsGenerated(compound.getBoolean("SpellsGenerated"));
        }
        if (compound.contains("GoalsInitialized")) {
            id.setGoalsInitialized(compound.getBoolean("GoalsInitialized"));
        }
        // Magic schools
        if (compound.contains("MagicSchools") && id.getMagicSchools().isEmpty()) {
            ListTag schoolsTag = compound.getList("MagicSchools", Tag.TAG_STRING);
            List<SchoolType> schools = new ArrayList<>();
            for (int i = 0; i < schoolsTag.size(); i++) {
                try {
                    ResourceLocation loc = ResourceLocation.parse(schoolsTag.getString(i));
                    SchoolType school = SchoolRegistry.getSchool(loc);
                    if (school != null) schools.add(school);
                } catch (Exception e) {
                    MagicRealms.LOGGER.warn("Legacy school parse failed: {}", schoolsTag.getString(i), e);
                }
            }
            id.setMagicSchools(schools);
        }
        // Persisted spells
        if (compound.contains("PersistedSpells") && id.getPersistedSpells().isEmpty()) {
            ListTag spellsTag = compound.getList("PersistedSpells", Tag.TAG_STRING);
            List<AbstractSpell> spells = new ArrayList<>();
            for (int i = 0; i < spellsTag.size(); i++) {
                try {
                    ResourceLocation loc = ResourceLocation.parse(spellsTag.getString(i));
                    AbstractSpell spell = SpellRegistry.getSpell(loc);
                    if (spell != null) spells.add(spell);
                } catch (Exception e) {
                    MagicRealms.LOGGER.warn("Legacy spell parse failed: {}", spellsTag.getString(i), e);
                }
            }
            id.setPersistedSpells(spells);
        }

        // Patrol position
        if (compound.contains("PatrolPosition")) {
            patrolData.setPatrolPosition(BlockPos.of(compound.getLong("PatrolPosition")));
        }

        // Chair data
        if (compound.contains("ChairPosition")) {
            chairData.setChairPosition(BlockPos.of(compound.getLong("ChairPosition")));
        }
        if (compound.contains("SittingTime")) {
            chairData.setSittingTime(compound.getInt("SittingTime"));
        }
        if (compound.contains("SitCooldown")) {
            chairData.setSitCooldown(compound.getInt("SitCooldown"));
        }

        // Fear data
        if (compound.contains("FearedEntity") && fearData.getFearedEntityType() == null) {
            try {
                ResourceLocation loc = ResourceLocation.parse(compound.getString("FearedEntity"));
                fearData.setFearedEntityType(BuiltInRegistries.ENTITY_TYPE.get(loc));
            } catch (Exception e) {
                MagicRealms.LOGGER.warn("Legacy feared entity parse failed", e);
            }
        }
        if (compound.contains("FearedEntityTag") && fearData.getFearedEntityTag() == null) {
            try {
                ResourceLocation loc = ResourceLocation.parse(compound.getString("FearedEntityTag"));
                fearData.setFearedEntityTag(TagKey.create(Registries.ENTITY_TYPE, loc));
            } catch (Exception e) {
                MagicRealms.LOGGER.warn("Legacy feared entity tag parse failed", e);
            }
        }
    }

    public static void handlePatrolModePacket(Player player, UUID entityUUID){
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (!(serverPlayer.level() instanceof ServerLevel serverLevel)) return;

        Entity entity = serverLevel.getEntity(entityUUID);
        if (!(entity instanceof AbstractMercenaryEntity mercenary)) return;

        // Only the active contractor may toggle patrol mode.
        ContractData contractData = mercenary.getData(MRDataAttachments.CONTRACT_DATA);
        if (contractData == null || !contractData.isContractor(serverPlayer.getUUID(), mercenary.level())) {
            return;
        }

        boolean wasPatrolling = mercenary.isPatrolMode();
        mercenary.setPatrolMode(!wasPatrolling);

        String key = wasPatrolling ? "ui.magic_realms.patrol_following" : "ui.magic_realms.patrol_active";
        MutableComponent message = Component.translatable(key).withStyle(ChatFormatting.YELLOW);
        serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(message));
    }

    public static void handleFlyingArrowPacket(Player player, byte mode){
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        // Validate: player must actually be holding a FloatingArrowItem in main or offhand.
        ItemStack main = serverPlayer.getMainHandItem();
        ItemStack off = serverPlayer.getOffhandItem();
        boolean holding = main.getItem() instanceof FloatingArrowItem
                || off.getItem() instanceof FloatingArrowItem;
        if (!holding) return;

        // Clamp to valid range.
        if (mode < FloatingArrowEntity.MODE_IDLE || mode > FloatingArrowEntity.MODE_HOLD) {
            mode = FloatingArrowEntity.MODE_IDLE;
        }
        FloatingArrowItem.setModeForPlayer(serverPlayer, mode);
    }
}