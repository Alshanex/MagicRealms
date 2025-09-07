package net.alshanex.magic_realms.util.humans;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.VillagerOffersData;
import net.alshanex.magic_realms.entity.RandomHumanEntity;
import net.alshanex.magic_realms.util.MRUtils;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.phys.AABB;

import java.util.*;

public class HumanGoals {

    /**
     * Goal for RandomHumanEntity to sell unwanted items to employed villagers
     */
    public static class SellItemsToVillagersGoal extends Goal {
        private final RandomHumanEntity entity;
        private Villager targetVillager;
        private int tradingCooldown = 0;
        private int searchCooldown = 0;
        private boolean isActivelySelling = false;
        private int itemsSoldThisSession = 0; // Track items sold in current session
        private int ticksSinceLastTrade = 0; // Track time since last successful trade

        private static final int SEARCH_RADIUS = 32;
        private static final int TRADING_COOLDOWN = 200; // 10 seconds
        private static final int SEARCH_COOLDOWN = 100; // 5 seconds
        private static final int MAX_ITEMS_PER_SESSION = 10; // Maximum items to sell per interaction
        private static final int TRADE_ATTEMPT_INTERVAL = 40; // Try to trade every 2 seconds
        private static final int MAX_DISTANCE_SQUARED = 256; // 16 blocks squared (more generous)
        private static final int TRADING_DISTANCE_SQUARED = 16; // 4 blocks squared for trading

        public SellItemsToVillagersGoal(RandomHumanEntity entity) {
            this.entity = entity;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (tradingCooldown > 0) {
                tradingCooldown--;
                return false;
            }

            if (searchCooldown > 0) {
                searchCooldown--;
                return false;
            }

            // If we're already actively selling, don't restart the process
            if (isActivelySelling) {
                return false;
            }

            // First auto-equip any better equipment found in inventory
            MRUtils.autoEquipBetterEquipment(entity);

            boolean hasItemToSell = hasItemsToSell();
            boolean hasVillager = false;

            if (hasItemToSell) {
                hasVillager = findNearbyEmployedVillager();
                if (!hasVillager) {
                    MagicRealms.LOGGER.debug("Entity {} has items to sell but no villagers found", entity.getEntityName());
                } else {
                    MagicRealms.LOGGER.debug("Entity {} ready to sell items to villager", entity.getEntityName());
                }
            }

            return hasItemToSell && hasVillager;
        }

        @Override
        public boolean canContinueToUse() {
            // Continue if we have a target villager, haven't sold too many items, and villager is alive and reasonably close
            boolean canContinue = targetVillager != null &&
                    targetVillager.isAlive() &&
                    itemsSoldThisSession < MAX_ITEMS_PER_SESSION &&
                    entity.distanceToSqr(targetVillager) < MAX_DISTANCE_SQUARED &&
                    hasItemsToSell();

            if (!canContinue && isActivelySelling) {
                MagicRealms.LOGGER.debug("Entity {} stopping sell goal - villager alive: {}, items sold: {}/{}, has items: {}, distance OK: {}",
                        entity.getEntityName(),
                        targetVillager != null && targetVillager.isAlive(),
                        itemsSoldThisSession, MAX_ITEMS_PER_SESSION,
                        hasItemsToSell(),
                        targetVillager != null && entity.distanceToSqr(targetVillager) < MAX_DISTANCE_SQUARED);
            }

            return canContinue;
        }

        @Override
        public void start() {
            isActivelySelling = true;
            itemsSoldThisSession = 0;
            ticksSinceLastTrade = 0;

            if (targetVillager != null) {
                entity.getNavigation().moveTo(targetVillager, 1.0);
                MagicRealms.LOGGER.debug("Entity {} starting to sell items to villager at distance {}",
                        entity.getEntityName(),
                        Math.sqrt(entity.distanceToSqr(targetVillager)));
            }
        }

        @Override
        public void tick() {
            if (targetVillager == null) {
                MagicRealms.LOGGER.debug("Entity {} tick failed - no villager", entity.getEntityName());
                return;
            }

            double distanceToVillager = entity.distanceToSqr(targetVillager);
            ticksSinceLastTrade++;

            if (distanceToVillager <= TRADING_DISTANCE_SQUARED) { // Within 4 blocks
                entity.getNavigation().stop();
                entity.getLookControl().setLookAt(targetVillager);

                // Try to trade every 2 seconds
                if (ticksSinceLastTrade >= TRADE_ATTEMPT_INTERVAL) {
                    boolean tradeMade = attemptSellItem();
                    if (tradeMade) {
                        ticksSinceLastTrade = 0; // Reset timer after successful trade
                    }
                }
            } else if (distanceToVillager <= MAX_DISTANCE_SQUARED) { // Within reasonable range
                // Move towards villager
                entity.getNavigation().moveTo(targetVillager, 1.0);

                // Reset navigation if it's been too long since we moved
                if (entity.getNavigation().isDone() && distanceToVillager > TRADING_DISTANCE_SQUARED) {
                    entity.getNavigation().moveTo(targetVillager, 1.0);
                }
            } else {
                MagicRealms.LOGGER.debug("Entity {} villager too far away: {} blocks",
                        entity.getEntityName(), Math.sqrt(distanceToVillager));
                // Don't immediately give up - the canContinueToUse will handle this
            }
        }

        @Override
        public void stop() {
            if (isActivelySelling) {
                MagicRealms.LOGGER.debug("Entity {} stopping sell goal after selling {} items",
                        entity.getEntityName(), itemsSoldThisSession);
            }
            targetVillager = null;
            isActivelySelling = false;
            itemsSoldThisSession = 0;
            ticksSinceLastTrade = 0;
            tradingCooldown = TRADING_COOLDOWN;
            searchCooldown = SEARCH_COOLDOWN;
        }

        private boolean hasItemsToSell() {
            SimpleContainer inventory = entity.getInventory();

            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (shouldSellItem(stack)) {
                    return true;
                }
            }
            return false;
        }

        private ItemStack findNextItemToSell() {
            SimpleContainer inventory = entity.getInventory();

            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (shouldSellItem(stack)) {
                    return stack;
                }
            }
            return null;
        }

        private boolean shouldSellItem(ItemStack stack) {
            if (stack.isEmpty()) return false;

            // Don't sell equipped items
            if (isEquipped(stack)) return false;

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
                EquipmentSlot slot = MRUtils.getSlotForArmorType(armorItem.getType());
                ItemStack currentArmor = entity.getItemBySlot(slot);
                if (currentArmor.isEmpty() || MRUtils.isArmorBetter(armorItem, currentArmor)) {
                    return false; // This armor is better, don't sell it
                }
            }

            // Don't sell weapons if they're better than what we're using AND we're a melee class
            if (MRUtils.isWeapon(stack) &&
                    (entityClass == EntityClass.WARRIOR || (entityClass == EntityClass.ROGUE && !entity.isArcher()))) {
                ItemStack currentWeapon = entity.getMainHandItem();
                if (currentWeapon.isEmpty() || MRUtils.isWeaponBetter(stack, currentWeapon, entity)) {
                    return false; // This weapon is better, don't sell it
                }
            }

            // Don't sell staves if they're better than what we're using AND we're a mage
            if (entityClass == EntityClass.MAGE && MRUtils.isStaff(stack)) {
                ItemStack currentWeapon = entity.getMainHandItem();
                if (currentWeapon.isEmpty() || MRUtils.isStaffBetter(stack, currentWeapon)) {
                    return false; // This staff is better, don't sell it
                }
            }

            // Don't sell ranged weapons if they're better AND we're an archer
            if (entityClass == EntityClass.ROGUE && entity.isArcher() && MRUtils.isRangedWeapon(stack)) {
                ItemStack currentWeapon = entity.getMainHandItem();
                if (currentWeapon.isEmpty() || MRUtils.isRangedWeaponBetter(stack, currentWeapon)) {
                    return false; // This ranged weapon is better, don't sell it
                }
            }

            // Don't sell shields if we need them and don't have one
            if (entityClass == EntityClass.WARRIOR && entity.hasShield() && stack.getItem() instanceof ShieldItem) {
                ItemStack currentShield = entity.getOffhandItem();
                if (currentShield.isEmpty() || !(currentShield.getItem() instanceof ShieldItem)) {
                    return false; // We need this shield, don't sell it
                }
            }

            // For everything else (including rotten flesh, food, misc items), allow selling
            return true;
        }

        private boolean isEquipped(ItemStack stack) {
            return entity.getMainHandItem() == stack ||
                    entity.getOffhandItem() == stack ||
                    entity.getItemBySlot(EquipmentSlot.HEAD) == stack ||
                    entity.getItemBySlot(EquipmentSlot.CHEST) == stack ||
                    entity.getItemBySlot(EquipmentSlot.LEGS) == stack ||
                    entity.getItemBySlot(EquipmentSlot.FEET) == stack;
        }

        private boolean findNearbyEmployedVillager() {
            AABB searchArea = new AABB(entity.blockPosition()).inflate(SEARCH_RADIUS);
            List<Villager> villagers = entity.level().getEntitiesOfClass(Villager.class, searchArea);

            for (Villager villager : villagers) {
                VillagerProfession profession = villager.getVillagerData().getProfession();
                double distance = entity.distanceToSqr(villager);

                if (profession != VillagerProfession.NONE &&
                        profession != VillagerProfession.NITWIT &&
                        distance <= SEARCH_RADIUS * SEARCH_RADIUS) {
                    targetVillager = villager;
                    MagicRealms.LOGGER.debug("Entity {} selected villager with profession {} at distance {} for selling",
                            entity.getEntityName(), profession, Math.sqrt(distance));
                    return true;
                }
            }

            MagicRealms.LOGGER.debug("Entity {} found no suitable villagers for selling", entity.getEntityName());
            return false;
        }

        private boolean attemptSellItem() {
            if (targetVillager == null) {
                MagicRealms.LOGGER.debug("Entity {} attempt sell failed - no villager", entity.getEntityName());
                return false;
            }

            // Check if we've sold enough items this session
            if (itemsSoldThisSession >= MAX_ITEMS_PER_SESSION) {
                MagicRealms.LOGGER.debug("Entity {} reached max items sold this session ({})",
                        entity.getEntityName(), MAX_ITEMS_PER_SESSION);
                return false;
            }

            // Find the next item to sell
            ItemStack itemToSell = findNextItemToSell();
            if (itemToSell == null) {
                MagicRealms.LOGGER.debug("Entity {} no more items to sell", entity.getEntityName());
                return false;
            }

            // Verify the item is still in inventory and get its slot
            SimpleContainer inventory = entity.getInventory();
            int itemSlot = -1;
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                if (inventory.getItem(i) == itemToSell) {
                    itemSlot = i;
                    break;
                }
            }

            if (itemSlot == -1) {
                MagicRealms.LOGGER.debug("Entity {} item to sell no longer exists in inventory", entity.getEntityName());
                return false;
            }

            // Calculate emeralds to get based on rarity
            int emeraldsToGet = calculateEmeraldsForItem(itemToSell);

            if (emeraldsToGet > 0) {
                MagicRealms.LOGGER.debug("Entity {} selling {} for {} emeralds (item {} of {})",
                        entity.getEntityName(), itemToSell.getDisplayName().getString(),
                        emeraldsToGet, itemsSoldThisSession + 1, MAX_ITEMS_PER_SESSION);

                // Remove the item from inventory
                inventory.removeItem(itemSlot, 1);

                // Add emeralds to inventory
                ItemStack emeralds = new ItemStack(Items.EMERALD, emeraldsToGet);
                inventory.addItem(emeralds);

                // Play trading sound
                entity.playSound(SoundEvents.VILLAGER_YES, 1.0F, 1.0F);
                targetVillager.playSound(SoundEvents.VILLAGER_YES, 1.0F, 1.0F);

                // Increase villager experience
                if (targetVillager instanceof Villager villager) {
                    villager.setVillagerXp(villager.getVillagerXp() + 1);
                }

                // Increment items sold counter
                itemsSoldThisSession++;

                // Recheck unaffordable offers since we got emeralds
                recheckUnaffordableOffers();

                MagicRealms.LOGGER.debug("Entity {} completed selling, now has {} emeralds total",
                        entity.getEntityName(), inventory.countItem(Items.EMERALD));

                return true; // Successful trade
            }

            return false; // No trade made
        }

        private void recheckUnaffordableOffers() {
            VillagerOffersData memory = entity.getData(MRDataAttachments.VILLAGER_OFFERS_DATA);

            if (!memory.hasAnyUnaffordableOffers()) {
                return;
            }

            // Create a copy of villager UUIDs to avoid concurrent modification
            Set<UUID> villagerUUIDs = new HashSet<>(memory.getVillagersWithUnaffordableOffers());

            for (UUID villagerUUID : villagerUUIDs) {
                // This will automatically remove affordable offers from the unaffordable list
                List<MerchantOffer> nowAffordable = memory.checkAffordableOffers(villagerUUID, entity.getInventory());

                if (!nowAffordable.isEmpty()) {
                    MagicRealms.LOGGER.debug("Entity {} can now afford {} offers from villager {} after selling",
                            entity.getEntityName(), nowAffordable.size(), villagerUUID);
                }
            }
        }

        private int calculateEmeraldsForItem(ItemStack stack) {
            RandomSource random = entity.getRandom();
            Rarity rarity = stack.getRarity();

            if (rarity == Rarity.COMMON) {
                double roll = random.nextDouble();
                if (roll < 0.85) return 1;
                else if (roll < 0.95) return 2;
                else return 3;
            } else {
                // Non-common rarity
                double roll = random.nextDouble();
                if (roll < 0.30) return 3;
                else if (roll < 0.90) return 2;
                else return 1;
            }
        }
    }

    /**
     * Goal for RandomHumanEntity to buy equipment from villagers with memory system
     */
    public static class BuyEquipmentFromVillagersGoal extends Goal {
        private final RandomHumanEntity entity;
        private Villager targetVillager;
        private MerchantOffer targetOffer;
        private boolean fromMemory = false;
        private int tradingCooldown = 0;
        private int searchCooldown = 0;

        private static final int SEARCH_RADIUS = 32;
        private static final int TRADING_COOLDOWN = 300; // 15 seconds
        private static final int SEARCH_COOLDOWN = 100; // 5 seconds

        public BuyEquipmentFromVillagersGoal(RandomHumanEntity entity) {
            this.entity = entity;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (tradingCooldown > 0) {
                tradingCooldown--;
                return false;
            }

            if (searchCooldown > 0) {
                searchCooldown--;
                return false;
            }

            // First check if we can now afford any previously unaffordable offers
            if (checkMemoryForAffordableOffers()) {
                return true;
            }

            // Then search for new offers
            return findVillagerWithAffordableEquipment();
        }

        @Override
        public boolean canContinueToUse() {
            return targetVillager != null && targetVillager.isAlive() &&
                    targetOffer != null && entity.distanceToSqr(targetVillager) < 64;
        }

        @Override
        public void start() {
            if (targetVillager != null) {
                entity.getNavigation().moveTo(targetVillager, 1.0);
            }
        }

        @Override
        public void tick() {
            if (targetVillager == null || targetOffer == null) return;

            double distanceToVillager = entity.distanceToSqr(targetVillager);

            if (distanceToVillager <= 9.0) { // Within 3 blocks
                entity.getNavigation().stop();
                entity.getLookControl().setLookAt(targetVillager);

                if (entity.tickCount % 60 == 0) { // Try to trade every 3 seconds
                    attemptBuyEquipment();
                }
            } else if (distanceToVillager <= 64) { // Within 8 blocks
                entity.getNavigation().moveTo(targetVillager, 1.0);
            }
        }

        @Override
        public void stop() {
            targetVillager = null;
            targetOffer = null;
            fromMemory = false;
            tradingCooldown = TRADING_COOLDOWN;
            searchCooldown = SEARCH_COOLDOWN;
        }

        private boolean checkMemoryForAffordableOffers() {
            VillagerOffersData memory = entity.getData(MRDataAttachments.VILLAGER_OFFERS_DATA);
            AABB searchArea = new AABB(entity.blockPosition()).inflate(SEARCH_RADIUS);
            List<Villager> nearbyVillagers = entity.level().getEntitiesOfClass(Villager.class, searchArea);

            for (Villager villager : nearbyVillagers) {
                List<MerchantOffer> affordableOffers = memory.checkAffordableOffers(villager.getUUID(), entity.getInventory());

                for (MerchantOffer offer : affordableOffers) {
                    if (isNeededItem(offer.getResult()) && !offer.isOutOfStock()) {
                        targetVillager = villager;
                        targetOffer = offer;
                        fromMemory = true;
                        return true;
                    }
                }
            }

            return false;
        }

        private boolean findVillagerWithAffordableEquipment() {
            AABB searchArea = new AABB(entity.blockPosition()).inflate(SEARCH_RADIUS);
            List<Villager> villagers = entity.level().getEntitiesOfClass(Villager.class, searchArea);
            VillagerOffersData memory = entity.getData(MRDataAttachments.VILLAGER_OFFERS_DATA);

            for (Villager villager : villagers) {
                VillagerProfession profession = villager.getVillagerData().getProfession();

                if (!canVillagerSellNeededItems(profession)) {
                    continue;
                }

                // Check regular offers
                MerchantOffer affordableOffer = findAffordableUsefulOffer(villager);
                if (affordableOffer != null) {
                    targetVillager = villager;
                    targetOffer = affordableOffer;
                    fromMemory = false;
                    return true;
                }

                // Store unaffordable offers for later
                storeUnaffordableOffers(villager, memory);

                // Special case: Warriors can buy shields from weaponsmiths for 10 emeralds
                if (profession == VillagerProfession.WEAPONSMITH &&
                        entity.getEntityClass() == EntityClass.WARRIOR &&
                        entity.hasShield() && needsShield()) {

                    if (hasEmeralds(10)) {
                        targetVillager = villager;
                        targetOffer = createShieldOffer();
                        fromMemory = false;
                        return true;
                    } else {
                        // Store shield offer as unaffordable
                        List<MerchantOffer> shieldOffers = Arrays.asList(createShieldOffer());
                        memory.storeUnaffordableOffers(villager.getUUID(), shieldOffers);
                    }
                }

                // Special case: Mages can buy artificer staff from librarians for 10 emeralds
                if (profession == VillagerProfession.LIBRARIAN &&
                        entity.getEntityClass() == EntityClass.MAGE) {

                    boolean needsStaffResult = needsStaff();
                    int emeraldCount = entity.getInventory().countItem(Items.EMERALD);

                    //MagicRealms.LOGGER.debug("Mage {} checking librarian for staff. Needs staff: {}, Has {} emeralds", entity.getEntityName(), needsStaffResult, emeraldCount);

                    if (needsStaffResult) {
                        if (hasEmeralds(10)) {
                            //MagicRealms.LOGGER.debug("Mage {} attempting to buy artificer staff from librarian", entity.getEntityName());
                            targetVillager = villager;
                            targetOffer = createStaffOffer();
                            fromMemory = false;
                            return true;
                        } else {
                            //MagicRealms.LOGGER.debug("Mage {} cannot afford staff (needs 10 emeralds, has {}), storing offer", entity.getEntityName(), emeraldCount);
                            // Store staff offer as unaffordable
                            List<MerchantOffer> staffOffers = Arrays.asList(createStaffOffer());
                            memory.storeUnaffordableOffers(villager.getUUID(), staffOffers);
                        }
                    } else {
                        //MagicRealms.LOGGER.debug("Mage {} doesn't need staff. Current weapon: {}", entity.getEntityName(), entity.getMainHandItem().getDisplayName().getString());
                    }
                }
            }

            return false;
        }

        private MerchantOffer findAffordableUsefulOffer(Villager villager) {
            MerchantOffers offers = villager.getOffers();

            for (MerchantOffer offer : offers) {
                if (offer.isOutOfStock()) continue;

                ItemStack result = offer.getResult();

                if (isNeededItem(result) && canAffordOffer(offer)) {
                    return offer;
                }
            }

            return null;
        }

        private void storeUnaffordableOffers(Villager villager, VillagerOffersData memory) {
            MerchantOffers offers = villager.getOffers();
            List<MerchantOffer> unaffordableOffers = new ArrayList<>();

            for (MerchantOffer offer : offers) {
                if (offer.isOutOfStock()) continue;

                ItemStack result = offer.getResult();

                // If we need this item but can't afford it, store it
                if (isNeededItem(result) && !canAffordOffer(offer)) {
                    unaffordableOffers.add(offer);
                }
            }

            if (!unaffordableOffers.isEmpty()) {
                memory.storeUnaffordableOffers(villager.getUUID(), unaffordableOffers);
            }
        }

        private boolean canVillagerSellNeededItems(VillagerProfession profession) {
            EntityClass entityClass = entity.getEntityClass();

            // All classes can buy armor
            if (profession == VillagerProfession.LEATHERWORKER ||
                    profession == VillagerProfession.ARMORER) {
                return true;
            }

            // Warriors and assassins can buy weapons
            if ((entityClass == EntityClass.WARRIOR ||
                    (entityClass == EntityClass.ROGUE && !entity.isArcher())) &&
                    profession == VillagerProfession.WEAPONSMITH) {
                return true;
            }

            // Archers can buy from fletchers
            if (entityClass == EntityClass.ROGUE && entity.isArcher() &&
                    profession == VillagerProfession.FLETCHER) {
                return true;
            }

            // Mages can buy staves from librarians
            if (entityClass == EntityClass.MAGE &&
                    profession == VillagerProfession.LIBRARIAN) {
                return true;
            }

            return false;
        }

        private boolean isNeededItem(ItemStack stack) {
            EntityClass entityClass = entity.getEntityClass();

            // Check if it's armor we need and it's better than current
            if (stack.getItem() instanceof ArmorItem armorItem) {
                EquipmentSlot slot = MRUtils.getSlotForArmorType(armorItem.getType());
                ItemStack currentArmor = entity.getItemBySlot(slot);
                return currentArmor.isEmpty() || MRUtils.isArmorBetter(armorItem, currentArmor);
            }

            // Check if it's a weapon we need and it's better than current
            if ((entityClass == EntityClass.WARRIOR ||
                    (entityClass == EntityClass.ROGUE && !entity.isArcher())) &&
                    MRUtils.isWeapon(stack)) {
                ItemStack currentWeapon = entity.getMainHandItem();
                return currentWeapon.isEmpty() || MRUtils.isWeaponBetter(stack, currentWeapon, entity);
            }

            // Check if it's a shield we need
            if (entityClass == EntityClass.WARRIOR && entity.hasShield() &&
                    stack.getItem() instanceof ShieldItem) {
                return needsShield();
            }

            // Check if it's ranged weapon/arrows we need (archers only)
            if (entityClass == EntityClass.ROGUE && entity.isArcher()) {
                if (MRUtils.isRangedWeapon(stack)) {
                    ItemStack currentWeapon = entity.getMainHandItem();
                    return currentWeapon.isEmpty() || MRUtils.isRangedWeaponBetter(stack, currentWeapon);
                }
                if (stack.getItem() instanceof ArrowItem || stack.is(Items.ARROW)) {
                    return needsArrows();
                }
            }

            // Check if it's a staff we need (mages only)
            if (entityClass == EntityClass.MAGE && MRUtils.isStaff(stack)) {
                ItemStack currentWeapon = entity.getMainHandItem();
                return currentWeapon.isEmpty() || MRUtils.isStaffBetter(stack, currentWeapon);
            }

            return false;
        }

        private boolean canAffordOffer(MerchantOffer offer) {
            SimpleContainer inventory = entity.getInventory();

            ItemStack costA = offer.getCostA();
            ItemStack costB = offer.getCostB();

            boolean canAffordA = costA.isEmpty() ||
                    inventory.countItem(costA.getItem()) >= costA.getCount();
            boolean canAffordB = costB.isEmpty() ||
                    inventory.countItem(costB.getItem()) >= costB.getCount();

            return canAffordA && canAffordB;
        }

        private boolean hasEmeralds(int count) {
            return entity.getInventory().countItem(Items.EMERALD) >= count;
        }

        private MerchantOffer createShieldOffer() {
            ItemCost emeraldCost = new ItemCost(Items.EMERALD, 10);
            return new MerchantOffer(emeraldCost, new ItemStack(Items.SHIELD), 1, 1, 0.0f);
        }

        private MerchantOffer createStaffOffer() {
            ItemCost emeraldCost = new ItemCost(Items.EMERALD, 10);
            return new MerchantOffer(emeraldCost, new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.ARTIFICER_STAFF.get()), 1, 1, 0.0f);
        }

        private void attemptBuyEquipment() {
            if (targetVillager == null || targetOffer == null) return;

            // Double-check affordability
            if (!canAffordOffer(targetOffer)) {
                // Store as unaffordable and stop
                VillagerOffersData memory = entity.getData(MRDataAttachments.VILLAGER_OFFERS_DATA);
                memory.storeUnaffordableOffers(targetVillager.getUUID(), Arrays.asList(targetOffer));
                stop();
                return;
            }

            // If this offer came from memory, verify the villager still has it
            if (fromMemory && !villagerHasOffer(targetVillager, targetOffer)) {
                // Offer no longer exists, stop and clear from memory
                stop();
                return;
            }

            SimpleContainer inventory = entity.getInventory();

            // Remove cost items from inventory
            ItemStack costA = targetOffer.getCostA();
            ItemStack costB = targetOffer.getCostB();

            if (!costA.isEmpty()) {
                MRUtils.removeItemsFromInventory(inventory, costA.getItem(), costA.getCount());
            }
            if (!costB.isEmpty()) {
                MRUtils.removeItemsFromInventory(inventory, costB.getItem(), costB.getCount());
            }

            // Add the purchased item to inventory
            ItemStack result = targetOffer.getResult().copy();
            inventory.addItem(result);

            // Use the offer (this increases villager experience) - only if not from memory
            if (!fromMemory) {
                targetOffer.increaseUses();
            }

            // Play trading sound
            entity.playSound(SoundEvents.VILLAGER_YES, 1.0F, 1.0F);
            targetVillager.playSound(SoundEvents.VILLAGER_YES, 1.0F, 1.0F);

            // Reset for next search
            targetOffer = null;
        }

        private boolean villagerHasOffer(Villager villager, MerchantOffer targetOffer) {
            MerchantOffers currentOffers = villager.getOffers();

            for (MerchantOffer currentOffer : currentOffers) {
                // Compare offers by result item and costs
                if (offersMatch(currentOffer, targetOffer)) {
                    return !currentOffer.isOutOfStock();
                }
            }

            return false;
        }

        private boolean offersMatch(MerchantOffer offer1, MerchantOffer offer2) {
            if (offer1 == offer2) return true;
            if (offer1 == null || offer2 == null) return false;

            return offer1.getItemCostA().equals(offer2.getItemCostA()) &&
                    offer1.getItemCostB().equals(offer2.getItemCostB()) &&
                    ItemStack.isSameItemSameComponents(offer1.getResult(), offer2.getResult()) &&
                    offer1.getMaxUses() == offer2.getMaxUses() &&
                    offer1.getXp() == offer2.getXp();
        }

        // Helper methods
        private boolean needsShield() {
            ItemStack offhand = entity.getOffhandItem();
            return offhand.isEmpty() || !(offhand.getItem() instanceof ShieldItem);
        }

        private boolean needsStaff() {
            ItemStack mainHand = entity.getMainHandItem();

            // If we don't have any weapon, we need a staff
            if (mainHand.isEmpty()) {
                return true;
            }

            // If we have a staff but the artificer staff would be better, we need it
            if (MRUtils.isStaff(mainHand)) {
                ItemStack artificerStaff = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.ARTIFICER_STAFF.get());
                return MRUtils.isStaffBetter(artificerStaff, mainHand);
            }

            // If we have a non-staff weapon as a mage, we need a staff
            return true;
        }

        private boolean needsArrows() {
            return !entity.hasArrows() || getArrowCount() < 32;
        }

        private int getArrowCount() {
            SimpleContainer inventory = entity.getInventory();
            int count = 0;
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (stack.getItem() instanceof ArrowItem || stack.is(Items.ARROW)) {
                    count += stack.getCount();
                }
            }
            return count;
        }
    }

    /**
     * Goal for RandomHumanEntity to buy and apply enchanted books from librarians
     */
    public static class EnchantEquipmentFromLibrarianGoal extends Goal {
        private final RandomHumanEntity entity;
        private Villager targetLibrarian;
        private MerchantOffer targetEnchantmentOffer;
        private ItemStack targetEquipment;
        private boolean fromMemory = false;
        private int tradingCooldown = 0;
        private int searchCooldown = 0;

        private static final int SEARCH_RADIUS = 32;
        private static final int TRADING_COOLDOWN = 400; // 20 seconds
        private static final int SEARCH_COOLDOWN = 300; // 15 seconds

        public EnchantEquipmentFromLibrarianGoal(RandomHumanEntity entity) {
            this.entity = entity;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (tradingCooldown > 0) {
                tradingCooldown--;
                return false;
            }

            if (searchCooldown > 0) {
                searchCooldown--;
                return false;
            }

            // First check if we can now afford any previously unaffordable offers
            if (checkMemoryForAffordableOffers()) {
                return true;
            }

            // Then search for new offers
            return findLibrarianWithUsefulEnchantment();
        }

        @Override
        public boolean canContinueToUse() {
            return targetLibrarian != null && targetLibrarian.isAlive() &&
                    targetEnchantmentOffer != null && targetEquipment != null &&
                    entity.distanceToSqr(targetLibrarian) < 64;
        }

        @Override
        public void start() {
            if (targetLibrarian != null) {
                entity.getNavigation().moveTo(targetLibrarian, 1.0);
            }
        }

        @Override
        public void tick() {
            if (targetLibrarian == null || targetEnchantmentOffer == null) return;

            double distanceToLibrarian = entity.distanceToSqr(targetLibrarian);

            if (distanceToLibrarian <= 9.0) { // Within 3 blocks
                entity.getNavigation().stop();
                entity.getLookControl().setLookAt(targetLibrarian);

                if (entity.tickCount % 80 == 0) { // Try to trade every 4 seconds
                    attemptBuyAndApplyEnchantment();
                }
            } else if (distanceToLibrarian <= 64) { // Within 8 blocks
                entity.getNavigation().moveTo(targetLibrarian, 1.0);
            }
        }

        @Override
        public void stop() {
            targetLibrarian = null;
            targetEnchantmentOffer = null;
            targetEquipment = null;
            fromMemory = false;
            tradingCooldown = TRADING_COOLDOWN;
            searchCooldown = SEARCH_COOLDOWN;
        }

        private boolean checkMemoryForAffordableOffers() {
            VillagerOffersData memory = entity.getData(MRDataAttachments.VILLAGER_OFFERS_DATA);
            AABB searchArea = new AABB(entity.blockPosition()).inflate(SEARCH_RADIUS);
            List<Villager> nearbyLibrarians = entity.level().getEntitiesOfClass(Villager.class, searchArea);

            for (Villager villager : nearbyLibrarians) {
                // Only check librarians
                if (villager.getVillagerData().getProfession() != VillagerProfession.LIBRARIAN) {
                    continue;
                }

                List<MerchantOffer> affordableOffers = memory.checkAffordableOffers(villager.getUUID(), entity.getInventory());

                for (MerchantOffer offer : affordableOffers) {
                    ItemStack result = offer.getResult();
                    if (result.getItem() == Items.ENCHANTED_BOOK && !offer.isOutOfStock()) {
                        // Check if this enchantment can still be applied to any of our equipment
                        ItemStack equipmentToEnchant = findEquipmentForEnchantment(result);
                        if (equipmentToEnchant != null) {
                            targetLibrarian = villager;
                            targetEnchantmentOffer = offer;
                            targetEquipment = equipmentToEnchant;
                            fromMemory = true;
                            return true;
                        }
                    }
                }
            }

            return false;
        }

        private boolean findLibrarianWithUsefulEnchantment() {
            AABB searchArea = new AABB(entity.blockPosition()).inflate(SEARCH_RADIUS);
            List<Villager> villagers = entity.level().getEntitiesOfClass(Villager.class, searchArea);
            VillagerOffersData memory = entity.getData(MRDataAttachments.VILLAGER_OFFERS_DATA);

            for (Villager villager : villagers) {
                if (villager.getVillagerData().getProfession() != VillagerProfession.LIBRARIAN) {
                    continue;
                }

                // Check regular offers
                MerchantOffer affordableOffer = findAffordableUsefulEnchantment(villager);
                if (affordableOffer != null) {
                    targetLibrarian = villager;
                    targetEnchantmentOffer = affordableOffer;
                    fromMemory = false;
                    return true;
                }

                // Store unaffordable offers for later
                storeUnaffordableEnchantmentOffers(villager, memory);
            }

            return false;
        }

        private MerchantOffer findAffordableUsefulEnchantment(Villager villager) {
            MerchantOffers offers = villager.getOffers();

            for (MerchantOffer offer : offers) {
                if (offer.isOutOfStock()) continue;

                ItemStack result = offer.getResult();
                if (result.getItem() == Items.ENCHANTED_BOOK && canAffordOffer(offer)) {
                    // Check if this enchantment can be applied to any of our equipment
                    ItemStack equipmentToEnchant = findEquipmentForEnchantment(result);
                    if (equipmentToEnchant != null) {
                        targetEquipment = equipmentToEnchant;
                        return offer;
                    }
                }
            }

            return null;
        }

        private void storeUnaffordableEnchantmentOffers(Villager villager, VillagerOffersData memory) {
            MerchantOffers offers = villager.getOffers();
            List<MerchantOffer> unaffordableOffers = new ArrayList<>();

            for (MerchantOffer offer : offers) {
                if (offer.isOutOfStock()) continue;

                ItemStack result = offer.getResult();
                if (result.getItem() == Items.ENCHANTED_BOOK) {
                    // Check if this enchantment can be applied to any of our equipment
                    ItemStack equipmentToEnchant = findEquipmentForEnchantment(result);
                    if (equipmentToEnchant != null && !canAffordOffer(offer)) {
                        // We need this enchantment but can't afford it, store it
                        unaffordableOffers.add(offer);
                    }
                }
            }

            if (!unaffordableOffers.isEmpty()) {
                memory.storeUnaffordableOffers(villager.getUUID(), unaffordableOffers);
            }
        }

        private ItemStack findEquipmentForEnchantment(ItemStack enchantedBook) {
            ItemEnchantments bookEnchantments = EnchantmentHelper.getEnchantmentsForCrafting(enchantedBook);

            // Check all equipped items and inventory items
            List<ItemStack> allEquipment = getAllEquipment();

            for (ItemStack equipment : allEquipment) {
                if (equipment.isEmpty()) continue;

                for (var entry : bookEnchantments.entrySet()) {
                    Holder<Enchantment> enchantmentHolder = entry.getKey();
                    Enchantment enchantment = enchantmentHolder.value();
                    int bookLevel = entry.getIntValue();

                    // Check if this enchantment can be applied to this item
                    if (enchantment.canEnchant(equipment)) {
                        ItemEnchantments currentEnchantments = EnchantmentHelper.getEnchantmentsForCrafting(equipment);
                        int currentLevel = currentEnchantments.getLevel(enchantmentHolder);

                        // We want to apply if:
                        // 1. The item doesn't have this enchantment
                        // 2. The item has a lower level and we can combine them
                        if (currentLevel == 0 ||
                                (currentLevel == bookLevel && currentLevel < enchantment.getMaxLevel())) {
                            return equipment;
                        }
                    }
                }
            }

            return null;
        }

        private List<ItemStack> getAllEquipment() {
            List<ItemStack> equipment = new ArrayList<>();

            // Add equipped items
            equipment.add(entity.getMainHandItem());
            equipment.add(entity.getOffhandItem());
            equipment.add(entity.getItemBySlot(EquipmentSlot.HEAD));
            equipment.add(entity.getItemBySlot(EquipmentSlot.CHEST));
            equipment.add(entity.getItemBySlot(EquipmentSlot.LEGS));
            equipment.add(entity.getItemBySlot(EquipmentSlot.FEET));

            // Add items from inventory that can be enchanted
            SimpleContainer inventory = entity.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (canBeEnchanted(stack)) {
                    equipment.add(stack);
                }
            }

            return equipment;
        }

        private boolean canBeEnchanted(ItemStack stack) {
            return stack.getItem() instanceof ArmorItem ||
                    stack.getItem() instanceof SwordItem ||
                    stack.getItem() instanceof AxeItem ||
                    stack.getItem() instanceof BowItem ||
                    stack.getItem() instanceof CrossbowItem ||
                    stack.getItem() instanceof TridentItem ||
                    stack.getItem() instanceof ShieldItem;
        }

        private boolean canAffordOffer(MerchantOffer offer) {
            SimpleContainer inventory = entity.getInventory();

            ItemStack costA = offer.getCostA();
            ItemStack costB = offer.getCostB();

            boolean canAffordA = costA.isEmpty() ||
                    inventory.countItem(costA.getItem()) >= costA.getCount();
            boolean canAffordB = costB.isEmpty() ||
                    inventory.countItem(costB.getItem()) >= costB.getCount();

            return canAffordA && canAffordB;
        }

        private void attemptBuyAndApplyEnchantment() {
            if (targetLibrarian == null || targetEnchantmentOffer == null || targetEquipment == null) {
                return;
            }

            // Double-check affordability
            if (!canAffordOffer(targetEnchantmentOffer)) {
                // Store as unaffordable and stop
                VillagerOffersData memory = entity.getData(MRDataAttachments.VILLAGER_OFFERS_DATA);
                memory.storeUnaffordableOffers(targetLibrarian.getUUID(), Arrays.asList(targetEnchantmentOffer));
                stop();
                return;
            }

            // If this offer came from memory, verify the librarian still has it
            if (fromMemory && !villagerHasOffer(targetLibrarian, targetEnchantmentOffer)) {
                // Offer no longer exists, stop and clear from memory
                stop();
                return;
            }

            SimpleContainer inventory = entity.getInventory();

            // Remove cost items from inventory
            ItemStack costA = targetEnchantmentOffer.getCostA();
            ItemStack costB = targetEnchantmentOffer.getCostB();

            if (!costA.isEmpty()) {
                MRUtils.removeItemsFromInventory(inventory, costA.getItem(), costA.getCount());
            }
            if (!costB.isEmpty()) {
                MRUtils.removeItemsFromInventory(inventory, costB.getItem(), costB.getCount());
            }

            // Get the enchanted book
            ItemStack enchantedBook = targetEnchantmentOffer.getResult().copy();
            ItemEnchantments bookEnchantments = EnchantmentHelper.getEnchantmentsForCrafting(enchantedBook);

            // Apply enchantments to the target equipment
            applyEnchantmentsToEquipment(targetEquipment, bookEnchantments);

            // Use the offer (this increases villager experience) - only if not from memory
            if (!fromMemory) {
                targetEnchantmentOffer.increaseUses();
            }

            // Play trading sound
            entity.playSound(SoundEvents.VILLAGER_YES, 1.0F, 1.0F);
            targetLibrarian.playSound(SoundEvents.VILLAGER_YES, 1.0F, 1.0F);

            // Play enchantment sound
            entity.playSound(SoundEvents.ENCHANTMENT_TABLE_USE, 1.0F, 1.0F);

            // Reset for next search
            targetEnchantmentOffer = null;
            targetEquipment = null;
        }

        private boolean villagerHasOffer(Villager villager, MerchantOffer targetOffer) {
            MerchantOffers currentOffers = villager.getOffers();

            for (MerchantOffer currentOffer : currentOffers) {
                // Compare offers by result item and costs
                if (offersMatch(currentOffer, targetOffer)) {
                    return !currentOffer.isOutOfStock();
                }
            }

            return false;
        }

        private boolean offersMatch(MerchantOffer offer1, MerchantOffer offer2) {
            if (offer1 == offer2) return true;
            if (offer1 == null || offer2 == null) return false;

            return offer1.getItemCostA().equals(offer2.getItemCostA()) &&
                    offer1.getItemCostB().equals(offer2.getItemCostB()) &&
                    ItemStack.isSameItemSameComponents(offer1.getResult(), offer2.getResult()) &&
                    offer1.getMaxUses() == offer2.getMaxUses() &&
                    offer1.getXp() == offer2.getXp();
        }

        private void applyEnchantmentsToEquipment(ItemStack equipment, ItemEnchantments newEnchantments) {
            EnchantmentHelper.updateEnchantments(equipment, currentEnchantments -> {
                for (var entry : newEnchantments.entrySet()) {
                    Holder<Enchantment> enchantmentHolder = entry.getKey();
                    Enchantment enchantment = enchantmentHolder.value();
                    int bookLevel = entry.getIntValue();

                    if (enchantment.canEnchant(equipment)) {
                        int currentLevel = currentEnchantments.getLevel(enchantmentHolder);
                        int newLevel;

                        if (currentLevel == 0) {
                            // Item doesn't have this enchantment, add it
                            newLevel = bookLevel;
                        } else if (currentLevel == bookLevel && currentLevel < enchantment.getMaxLevel()) {
                            // Same level enchantments combine to next level
                            newLevel = currentLevel + 1;
                        } else {
                            // Use the higher level
                            newLevel = Math.max(currentLevel, bookLevel);
                        }

                        currentEnchantments.set(enchantmentHolder, newLevel);
                    }
                }
            });
        }
    }
}