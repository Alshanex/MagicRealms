package net.alshanex.magic_realms.util.humans;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainerMutable;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.item.SpellBook;
import io.redspace.ironsspellbooks.registries.ComponentRegistry;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.alshanex.magic_realms.Config;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.KillTrackerData;
import net.alshanex.magic_realms.data.VillagerOffersData;
import net.alshanex.magic_realms.entity.RandomHumanEntity;
import net.alshanex.magic_realms.util.MRUtils;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;

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

            if(this.entity.isPatrolMode()){
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
                    //MagicRealms.LOGGER.debug("Entity {} has items to sell but no villagers found", entity.getEntityName());
                } else {
                    //MagicRealms.LOGGER.debug("Entity {} ready to sell items to villager", entity.getEntityName());
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

            // Don't sell spellbooks if they're better than what we're using AND we're a mage
            if (entityClass == EntityClass.MAGE && MRUtils.isSpellbook(stack)) {
                ItemStack currentOffhand = entity.getOffhandItem();
                if (currentOffhand.isEmpty() || MRUtils.isSpellbookBetter(stack, currentOffhand, entity)) {
                    return false; // This spellbook is better, don't sell it
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
                //MagicRealms.LOGGER.debug("Entity {} trading cooldown: {}", entity.getEntityName(), tradingCooldown);
                return false;
            }

            if (searchCooldown > 0) {
                searchCooldown--;
                //MagicRealms.LOGGER.debug("Entity {} search cooldown: {}", entity.getEntityName(), searchCooldown);
                return false;
            }

            if(this.entity.isPatrolMode()){
                return false;
            }

            if (!hasInventorySpace()) {
                return false;
            }

            //MagicRealms.LOGGER.debug("Entity {} attempting to use BuyEquipmentFromVillagersGoal", entity.getEntityName());

            // First check if we can now afford any previously unaffordable offers
            if (checkMemoryForAffordableOffers()) {
                //MagicRealms.LOGGER.debug("Entity {} found affordable offer in memory", entity.getEntityName());
                return true;
            }

            // Then search for new offers
            boolean result = findVillagerWithAffordableEquipment();
            //MagicRealms.LOGGER.debug("Entity {} findVillagerWithAffordableEquipment result: {}", entity.getEntityName(), result);
            return result;
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

        private boolean hasInventorySpace() {
            SimpleContainer inventory = entity.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                if (inventory.getItem(i).isEmpty()) {
                    return true;
                }
            }
            return false;
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
                    boolean needsSpellbookResult = needsSpellbook();
                    int emeraldCount = entity.getInventory().countItem(Items.EMERALD);

                    // Check for staff first
                    if (needsStaffResult) {
                        if (hasEmeralds(10)) {
                            targetVillager = villager;
                            targetOffer = createStaffOffer();
                            fromMemory = false;
                            return true;
                        } else {
                            // Store staff offer as unaffordable
                            List<MerchantOffer> staffOffers = Arrays.asList(createStaffOffer());
                            memory.storeUnaffordableOffers(villager.getUUID(), staffOffers);
                        }
                    }

                    // Check for spellbook if no staff needed or couldn't afford staff
                    if (needsSpellbookResult) {
                        int spellbookCost = 15;
                        if (hasEmeralds(spellbookCost)) {
                            targetVillager = villager;
                            targetOffer = createSpellbookOffer();
                            fromMemory = false;
                            return true;
                        } else {
                            // Store spellbook offer as unaffordable
                            List<MerchantOffer> spellbookOffers = Arrays.asList(createSpellbookOffer());
                            memory.storeUnaffordableOffers(villager.getUUID(), spellbookOffers);
                        }
                    }
                }
            }

            return false;
        }

        private boolean needsSpellbook() {
            ItemStack offhand = entity.getOffhandItem();

            //MagicRealms.LOGGER.debug("Entity {} needsSpellbook check: offhand={}", entity.getEntityName(), offhand.isEmpty() ? "EMPTY" : offhand.getDisplayName().getString());

            // If we don't have any spellbook, we need one
            boolean result = offhand.isEmpty() || !MRUtils.isSpellbook(offhand);
            //MagicRealms.LOGGER.debug("Entity {} needsSpellbook result: {}", entity.getEntityName(), result);
            return result;
        }

        private MerchantOffer createSpellbookOffer() {
            int cost = 15;
            ItemCost emeraldCost = new ItemCost(Items.EMERALD, cost);

            // Create a basic spellbook - you might want to create one with random spells
            ItemStack spellbook = new ItemStack(ItemRegistry.COPPER_SPELL_BOOK.get());

            // You could add logic here to generate spells for the spellbook based on the mage's schools and level
            initializeSpellbookWithRandomSpells(spellbook);

            return new MerchantOffer(emeraldCost, spellbook, 1, 1, 0.0f);
        }

        private void initializeSpellbookWithRandomSpells(ItemStack spellbook) {
            // Initialize the spellbook with spells that match the mage's schools
            if (spellbook.getItem() instanceof SpellBook spellBookItem) {
                spellBookItem.initializeSpellContainer(spellbook);

                ISpellContainer container = ISpellContainer.get(spellbook);
                if (container != null) {
                    ISpellContainerMutable mutableContainer = container.mutableCopy();

                    // Get the mage's magic schools
                    List<SchoolType> mageSchools = entity.getMagicSchools();

                    // Determine number of spells based on star level
                    int maxSpells = Math.min(spellBookItem.getMaxSpellSlots(), getSpellCountForStarLevel());

                    RandomSource random = entity.getRandom();
                    List<AbstractSpell> availableSpells = new ArrayList<>();

                    // Collect spells from mage's schools
                    for (SchoolType school : mageSchools) {
                        SpellRegistry.REGISTRY.stream()
                                .filter(spell -> spell.getSchoolType() == school)
                                .filter(AbstractSpell::isEnabled)
                                .filter(spell -> !spell.requiresLearning() || spell.allowLooting())
                                .forEach(availableSpells::add);
                    }

                    // If no school-specific spells available, use any available spell
                    if (availableSpells.isEmpty()) {
                        SpellRegistry.REGISTRY.stream()
                                .filter(AbstractSpell::isEnabled)
                                .filter(spell -> !spell.requiresLearning() || spell.allowLooting())
                                .forEach(availableSpells::add);
                    }

                    // Add random spells to the spellbook
                    for (int i = 0; i < maxSpells && !availableSpells.isEmpty(); i++) {
                        AbstractSpell randomSpell = availableSpells.get(random.nextInt(availableSpells.size()));
                        int spellLevel = getRandomSpellLevel(randomSpell, random);

                        mutableContainer.addSpellAtIndex(randomSpell, spellLevel, i, true);

                        // Remove spell to avoid duplicates
                        availableSpells.remove(randomSpell);
                    }

                    // Set the modified container back to the item using the component registry
                    spellbook.set(ComponentRegistry.SPELL_CONTAINER, mutableContainer.toImmutable());
                }
            }
        }

        private int getSpellCountForStarLevel() {
            return switch (entity.getStarLevel()) {
                case 1 -> 1 + entity.getRandom().nextInt(2); // 1-2 spells
                case 2 -> 2 + entity.getRandom().nextInt(2); // 2-3 spells
                case 3 -> 3 + entity.getRandom().nextInt(2); // 3-4 spells
                default -> 2;
            };
        }

        private int getRandomSpellLevel(AbstractSpell spell, RandomSource random) {
            int maxLevel = spell.getMaxLevel();
            int starLevel = entity.getStarLevel();

            // Higher star level entities get better spell levels
            int baseLevel = switch (starLevel) {
                case 1 -> 1 + random.nextInt(Math.min(2, maxLevel)); // Level 1-2
                case 2 -> 1 + random.nextInt(Math.min(3, maxLevel)); // Level 1-3
                case 3 -> 2 + random.nextInt(Math.min(3, maxLevel - 1)); // Level 2-4
                default -> 1;
            };

            return Math.min(baseLevel, maxLevel);
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

            //MagicRealms.LOGGER.debug("Entity {} canVillagerSellNeededItems: profession={}, entityClass={}", entity.getEntityName(), profession, entityClass);

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

            // Mages can buy staves and spellbooks from librarians
            if (entityClass == EntityClass.MAGE &&
                    profession == VillagerProfession.LIBRARIAN) {
                //MagicRealms.LOGGER.debug("Entity {} - Mage can buy from librarian", entity.getEntityName());
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

            // Check if it's a spellbook we need (mages only)
            if (entityClass == EntityClass.MAGE && MRUtils.isSpellbook(stack)) {
                ItemStack currentOffhand = entity.getOffhandItem();
                return currentOffhand.isEmpty() || MRUtils.isSpellbookBetter(stack, currentOffhand, entity);
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
                //MagicRealms.LOGGER.debug("Entity {} needs staff - no main hand weapon", entity.getEntityName());
                return true;
            }

            // If we have a staff but the artificer staff would be better, we need it
            if (MRUtils.isStaff(mainHand)) {
                ItemStack artificerStaff = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.ARTIFICER_STAFF.get());
                boolean isBetter = MRUtils.isStaffBetter(artificerStaff, mainHand);
                //MagicRealms.LOGGER.debug("Entity {} has staff, artificer staff better: {}", entity.getEntityName(), isBetter);
                return isBetter;
            }

            // If we have a non-staff weapon as a mage, we need a staff
            //MagicRealms.LOGGER.debug("Entity {} needs staff - has non-staff weapon", entity.getEntityName());
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

    public static class CustomFearGoal extends AvoidEntityGoal<LivingEntity> {
        private final RandomHumanEntity humanEntity;

        public CustomFearGoal(RandomHumanEntity humanEntity, float maxDistance, double walkSpeedModifier, double sprintSpeedModifier) {
            super(humanEntity, LivingEntity.class, maxDistance, walkSpeedModifier, sprintSpeedModifier,
                    (livingEntity) -> humanEntity.isAfraidOf(livingEntity));
            this.humanEntity = humanEntity;
        }

        @Override
        public boolean canUse() {
            boolean canUse = super.canUse();
            if (canUse) {
                humanEntity.setTarget(null);
            }
            return canUse;
        }

        @Override
        public void tick() {
            super.tick();
            if (humanEntity.getTarget() != null && humanEntity.isAfraidOf(humanEntity.getTarget())) {
                humanEntity.setTarget(null);
            }
        }
    }

    public static class NoFearTargetGoal extends NearestAttackableTargetGoal<Monster> {
        private final RandomHumanEntity humanEntity;

        public NoFearTargetGoal(RandomHumanEntity humanEntity) {
            super(humanEntity, Monster.class, true);
            this.humanEntity = humanEntity;
        }

        @Override
        public boolean canUse() {
            boolean canUse = super.canUse();
            if (canUse && target != null && humanEntity.isAfraidOf(target)) {
                return false; // Don't target feared entities
            }
            return canUse;
        }

        @Override
        protected boolean canAttack(LivingEntity potentialTarget, TargetingConditions targetPredicate) {
            if (humanEntity.isAfraidOf(potentialTarget)) {
                return false; // Never attack feared entities
            }
            return super.canAttack(potentialTarget, targetPredicate);
        }
    }

    public static class PickupMobDropsGoal extends Goal {
        private final RandomHumanEntity entity;
        private ItemEntity targetItem;
        private int searchCooldown = 0;
        private int stuckTicks = 0;
        private Vec3 lastPosition;

        private static final int SEARCH_RADIUS = 16;
        private static final int SEARCH_COOLDOWN = 40; // 2 seconds between searches
        private static final int MAX_STUCK_TICKS = 100; // 5 seconds before giving up on stuck item
        private static final double PICKUP_DISTANCE_SQ = 4.0; // 2 blocks
        private static final double MOVEMENT_THRESHOLD = 0.1; // Minimum movement to not be considered stuck

        public PickupMobDropsGoal(RandomHumanEntity entity) {
            this.entity = entity;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            // Don't run if entity is in combat
            if (entity.getTarget() != null) {
                return false;
            }

            // Don't run if entity is in standby mode
            if (entity.isPatrolMode()) {
                return false;
            }

            // Apply search cooldown
            if (searchCooldown > 0) {
                searchCooldown--;
                return false;
            }

            // Only run if entity can pick up loot
            if (!entity.canPickUpLoot()) {
                return false;
            }

            // Find nearby item entities
            return findNearestPickupableItem();
        }

        @Override
        public boolean canContinueToUse() {
            // Stop if we now have a target to fight
            if (entity.getTarget() != null) {
                return false;
            }

            // Stop if entity goes into standby
            if (entity.isPatrolMode()) {
                return false;
            }

            // Stop if target item is gone or too far away
            if (targetItem == null || !targetItem.isAlive() ||
                    entity.distanceToSqr(targetItem) > SEARCH_RADIUS * SEARCH_RADIUS) {
                return false;
            }

            // Stop if we've been stuck for too long
            if (stuckTicks >= MAX_STUCK_TICKS) {
                return false;
            }

            // Stop if item is no longer worth picking up
            if (!isItemWorthPickingUp(targetItem)) {
                return false;
            }

            // Stop if we can no longer fit this item in our inventory
            if (!canPickupItem(targetItem)) {
                return false;
            }

            return true;
        }

        @Override
        public void start() {
            if (targetItem != null) {
                entity.getNavigation().moveTo(targetItem, 1.0);
                stuckTicks = 0;
                lastPosition = entity.position();
            }
        }

        @Override
        public void tick() {
            if (targetItem == null) {
                return;
            }

            double distanceToItem = entity.distanceToSqr(targetItem);

            // Check if we're close enough to pick up the item
            if (distanceToItem <= PICKUP_DISTANCE_SQ) {
                // The entity's built-in pickup logic should handle this
                // But we can trigger it manually if needed
                if (entity.wantsToPickUp(targetItem.getItem())) {
                    entity.take(targetItem, targetItem.getItem().getCount());
                }
                return;
            }

            // Check if we're stuck
            Vec3 currentPosition = entity.position();
            if (lastPosition != null) {
                double movementDistance = currentPosition.distanceTo(lastPosition);
                if (movementDistance < MOVEMENT_THRESHOLD) {
                    stuckTicks++;
                } else {
                    stuckTicks = 0; // Reset if we're moving
                }
            }
            lastPosition = currentPosition;

            // Try to navigate to the item
            if (entity.getNavigation().isDone() || stuckTicks > 20) {
                // Recalculate path if navigation is done or we've been stuck for a bit
                entity.getNavigation().moveTo(targetItem, 1.0);

                // If we're really stuck, try moving to a slightly different position
                if (stuckTicks > 40) {
                    double offsetX = (entity.getRandom().nextDouble() - 0.5) * 2.0;
                    double offsetZ = (entity.getRandom().nextDouble() - 0.5) * 2.0;
                    entity.getNavigation().moveTo(
                            targetItem.getX() + offsetX,
                            targetItem.getY(),
                            targetItem.getZ() + offsetZ,
                            1.0
                    );
                }
            }
        }

        @Override
        public void stop() {
            targetItem = null;
            searchCooldown = SEARCH_COOLDOWN;
            stuckTicks = 0;
            lastPosition = null;
            entity.getNavigation().stop();
        }

        private boolean canPickupItem(ItemEntity itemEntity) {
            if (itemEntity == null || !itemEntity.isAlive()) {
                return false;
            }

            ItemStack itemStack = itemEntity.getItem();
            if (itemStack.isEmpty()) {
                return false;
            }

            // Check if the inventory can add this item
            SimpleContainer inventory = entity.getInventory();
            return inventory.canAddItem(itemStack);
        }

        private boolean findNearestPickupableItem() {
            AABB searchArea = new AABB(entity.blockPosition()).inflate(SEARCH_RADIUS);
            List<ItemEntity> nearbyItems = entity.level().getEntitiesOfClass(ItemEntity.class, searchArea);

            ItemEntity closestItem = null;
            double closestDistance = Double.MAX_VALUE;

            for (ItemEntity itemEntity : nearbyItems) {
                // Skip if item is not worth picking up
                if (!isItemWorthPickingUp(itemEntity)) {
                    continue;
                }

                // Skip if entity doesn't want to pick up this item
                if (!entity.wantsToPickUp(itemEntity.getItem())) {
                    continue;
                }

                // Skip if we can't fit this item in our inventory
                if (!canPickupItem(itemEntity)) {
                    continue;
                }

                // Skip items that are too young (just dropped)
                if (itemEntity.getAge() < 20) { // 1 second old minimum
                    continue;
                }

                double distance = entity.distanceToSqr(itemEntity);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestItem = itemEntity;
                }
            }

            if (closestItem != null) {
                targetItem = closestItem;
                return true;
            }

            return false;
        }

        private boolean isItemWorthPickingUp(ItemEntity itemEntity) {
            if (itemEntity == null || !itemEntity.isAlive()) {
                return false;
            }

            // Don't pick up items that are about to despawn
            if (itemEntity.getAge() > 5400) { // 4.5 minutes (items despawn at 6000 ticks)
                return false;
            }

            return true;
        }
    }

    public static class AlliedHumanDefenseGoal extends TargetGoal {
        private final RandomHumanEntity human;
        private LivingEntity targetMob;
        private int timestamp;
        private static final double HELP_RANGE = 16.0D;

        public AlliedHumanDefenseGoal(RandomHumanEntity human) {
            super(human, false);
            this.human = human;
            this.setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            // Don't help if we already have a target
            if (human.getTarget() != null) {
                return false;
            }

            // Look for allied humans in combat within range
            AABB searchArea = human.getBoundingBox().inflate(HELP_RANGE);
            List<RandomHumanEntity> nearbyHumans = human.level().getEntitiesOfClass(
                    RandomHumanEntity.class,
                    searchArea,
                    alliedHuman -> alliedHuman != human && // Not ourselves
                            human.isAlliedTo(alliedHuman) && // Allied to us
                            alliedHuman.getTarget() != null && // Has a target
                            alliedHuman.getTarget().isAlive() && // Target is alive
                            canHelpAgainst(alliedHuman.getTarget()) // We can help against this target
            );

            // Find the closest allied humans that needs help
            RandomHumanEntity closestAlliedHuman = null;
            double closestDistance = Double.MAX_VALUE;

            for (RandomHumanEntity alliedHuman : nearbyHumans) {
                double distance = human.distanceToSqr(alliedHuman);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestAlliedHuman = alliedHuman;
                }
            }

            if (closestAlliedHuman != null && closestAlliedHuman.getTarget() != null) {
                this.targetMob = closestAlliedHuman.getTarget();
                return true;
            }

            return false;
        }

        @Override
        public boolean canContinueToUse() {
            // Stop if target is no longer valid
            if (targetMob == null || !targetMob.isAlive()) {
                return false;
            }

            // Stop if we can no longer help against this target
            if (!canHelpAgainst(targetMob)) {
                return false;
            }

            // Stop if target is too far away
            if (human.distanceToSqr(targetMob) > HELP_RANGE * HELP_RANGE * 2) {
                return false;
            }

            // Stop if no allied human is still fighting this target
            AABB searchArea = human.getBoundingBox().inflate(HELP_RANGE * 1.5);
            List<RandomHumanEntity> alliedHumans = human.level().getEntitiesOfClass(
                    RandomHumanEntity.class,
                    searchArea,
                    alliedHuman -> alliedHuman != human &&
                            human.isAlliedTo(alliedHuman) &&
                            alliedHuman.getTarget() == targetMob
            );

            return !alliedHumans.isEmpty();
        }

        @Override
        public void start() {
            human.setTarget(this.targetMob);
            this.timestamp = human.tickCount;
            super.start();
        }

        @Override
        public void stop() {
            human.setTarget(null);
            this.targetMob = null;
            super.stop();
        }

        /**
         * Check if this human can help against the given target
         */
        private boolean canHelpAgainst(LivingEntity target) {
            // Don't attack allied entities
            if (human.isAlliedTo(target)) {
                return false;
            }

            // Don't attack entities we can't see
            if (!human.hasLineOfSight(target)) {
                return false;
            }

            // Use the same targeting conditions as other combat goals
            TargetingConditions conditions = TargetingConditions.forCombat()
                    .range(HELP_RANGE)
                    .selector(entity -> entity instanceof LivingEntity &&
                            !human.isAlliedTo(entity) &&
                            entity.isAlive());

            return conditions.test(human, target);
        }
    }

    public static class HumanHurtByTargetGoal extends HurtByTargetGoal {
        private final RandomHumanEntity human;
        private static final double ALERT_RANGE = 20.0D;

        public HumanHurtByTargetGoal(RandomHumanEntity human) {
            super(human);
            this.human = human;
        }

        @Override
        public void start() {
            super.start();

            // Alert nearby allied humans when we get hurt
            if (human.getLastHurtByMob() != null) {
                alertAlliedHumans(human.getLastHurtByMob());
            }
        }

        /**
         * Alert all allied humans in the area about the attacker
         */
        private void alertAlliedHumans(LivingEntity attacker) {
            if (attacker == null || !attacker.isAlive()) {
                return;
            }

            AABB searchArea = human.getBoundingBox().inflate(ALERT_RANGE);
            List<RandomHumanEntity> nearbyHumans = human.level().getEntitiesOfClass(
                    RandomHumanEntity.class,
                    searchArea,
                    alliedHuman -> alliedHuman != human && // Not ourselves
                            human.isAlliedTo(alliedHuman) && // Allied to us
                            (alliedHuman.getTarget() == null || // No current target
                                    alliedHuman.getTarget() == attacker) // Or already targeting this attacker
            );

            for (RandomHumanEntity alliedHuman : nearbyHumans) {
                // Only set target if they can attack this entity
                if (!alliedHuman.isAlliedTo(attacker) &&
                        alliedHuman.hasLineOfSight(attacker) &&
                        alliedHuman.getTarget() == null) {

                    alliedHuman.setTarget(attacker);

                    // Also set their last hurt by mob so they know who to be angry at
                    if (alliedHuman.getLastHurtByMob() == null) {
                        alliedHuman.setLastHurtByMob(attacker);
                    }
                }
            }
        }
    }

    public static class HumanWizardAttackGoal extends Goal {

        protected LivingEntity target;
        protected final double speedModifier;
        protected final int spellAttackIntervalMin;
        protected final int spellAttackIntervalMax;
        protected float spellcastingRange;
        protected float spellcastingRangeSqr;
        protected boolean shortCircuitTemp = false;

        protected boolean hasLineOfSight;
        protected int seeTime = 0;
        protected int strafeTime;
        protected boolean strafingClockwise;
        protected int spellAttackDelay = -1;
        protected int projectileCount;

        protected AbstractSpell singleUseSpell = SpellRegistry.none();
        protected int singleUseDelay;
        protected int singleUseLevel;

        protected boolean isFlying;
        protected boolean allowFleeing;
        protected int fleeCooldown;
        protected int flyingMovementTimer;
        protected Vec3 flyingTarget;
        protected int lastHurtTime = -1;

        protected final ArrayList<AbstractSpell> attackSpells = new ArrayList<>();
        protected final ArrayList<AbstractSpell> defenseSpells = new ArrayList<>();
        protected final ArrayList<AbstractSpell> movementSpells = new ArrayList<>();
        protected final ArrayList<AbstractSpell> supportSpells = new ArrayList<>();
        protected ArrayList<AbstractSpell> lastSpellCategory = attackSpells;

        protected float minSpellQuality = .1f;
        protected float maxSpellQuality = .4f;

        protected boolean drinksPotions;
        protected final PathfinderMob mob;
        protected final AbstractSpellCastingMob spellCastingMob;

        private Map<AbstractSpell, Holder<MobEffect>> buffs = Map.ofEntries(
                Map.entry(SpellRegistry.EVASION_SPELL.get(), MobEffectRegistry.EVASION),
                Map.entry(SpellRegistry.HEARTSTOP_SPELL.get(), MobEffectRegistry.HEARTSTOP),
                Map.entry(SpellRegistry.CHARGE_SPELL.get(), MobEffectRegistry.CHARGED),
                Map.entry(SpellRegistry.INVISIBILITY_SPELL.get(), MobEffectRegistry.TRUE_INVISIBILITY),
                Map.entry(SpellRegistry.OAKSKIN_SPELL.get(), MobEffectRegistry.OAKSKIN),
                Map.entry(SpellRegistry.HASTE_SPELL.get(), MobEffectRegistry.HASTENED),
                Map.entry(SpellRegistry.FROSTBITE_SPELL.get(), MobEffectRegistry.FROSTBITTEN_STRIKES),
                Map.entry(SpellRegistry.ABYSSAL_SHROUD_SPELL.get(), MobEffectRegistry.ABYSSAL_SHROUD),
                Map.entry(SpellRegistry.ECHOING_STRIKES_SPELL.get(), MobEffectRegistry.ECHOING_STRIKES),
                Map.entry(SpellRegistry.FORTIFY_SPELL.get(), MobEffectRegistry.FORTIFY),
                Map.entry(SpellRegistry.THUNDERSTORM_SPELL.get(), MobEffectRegistry.THUNDERSTORM),
                Map.entry(SpellRegistry.SPIDER_ASPECT_SPELL.get(), MobEffectRegistry.SPIDER_ASPECT)
        );
        private Map<AbstractSpell, Holder<MobEffect>> debuffs = Map.of(
                SpellRegistry.BLIGHT_SPELL.get(), MobEffectRegistry.BLIGHT,
                SpellRegistry.SLOW_SPELL.get(), MobEffectRegistry.SLOWED
        );

        public HumanWizardAttackGoal(AbstractSpellCastingMob abstractSpellCastingMob, double pSpeedModifier, int pAttackInterval) {
            this(abstractSpellCastingMob, pSpeedModifier, pAttackInterval, pAttackInterval);
        }

        public HumanWizardAttackGoal(AbstractSpellCastingMob abstractSpellCastingMob, double pSpeedModifier, int pAttackIntervalMin, int pAttackIntervalMax) {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Flag.TARGET));
            this.spellCastingMob = abstractSpellCastingMob;
            if (abstractSpellCastingMob instanceof PathfinderMob m) {
                this.mob = m;
            } else
                throw new IllegalStateException("Unable to add " + this.getClass().getSimpleName() + "to entity, must extend PathfinderMob.");

            this.speedModifier = pSpeedModifier;
            this.spellAttackIntervalMin = pAttackIntervalMin;
            this.spellAttackIntervalMax = pAttackIntervalMax;
            this.spellcastingRange = 20;
            this.spellcastingRangeSqr = spellcastingRange * spellcastingRange;
            allowFleeing = true;
            flyingMovementTimer = 0;
        }

        public HumanWizardAttackGoal setSpells(List<AbstractSpell> attackSpells, List<AbstractSpell> defenseSpells, List<AbstractSpell> movementSpells, List<AbstractSpell> supportSpells) {
            this.attackSpells.clear();
            this.defenseSpells.clear();
            this.movementSpells.clear();
            this.supportSpells.clear();

            this.attackSpells.addAll(attackSpells);
            this.defenseSpells.addAll(defenseSpells);
            this.movementSpells.addAll(movementSpells);
            this.supportSpells.addAll(supportSpells);

            return this;
        }

        public HumanWizardAttackGoal setSpellQuality(float minSpellQuality, float maxSpellQuality) {
            this.minSpellQuality = minSpellQuality;
            this.maxSpellQuality = maxSpellQuality;
            return this;
        }

        public HumanWizardAttackGoal setSingleUseSpell(AbstractSpell abstractSpell, int minDelay, int maxDelay, int minLevel, int maxLevel) {
            this.singleUseSpell = abstractSpell;
            this.singleUseDelay = Utils.random.nextIntBetweenInclusive(minDelay, maxDelay);
            this.singleUseLevel = Utils.random.nextIntBetweenInclusive(minLevel, maxLevel);
            return this;
        }

        public HumanWizardAttackGoal setIsFlying() {
            isFlying = true;
            return this;
        }

        public HumanWizardAttackGoal setDrinksPotions() {
            drinksPotions = true;
            return this;
        }

        public HumanWizardAttackGoal setAllowFleeing(boolean allowFleeing) {
            this.allowFleeing = allowFleeing;
            return this;
        }

        public boolean canUse() {
            LivingEntity livingentity = this.mob.getTarget();
            if (livingentity != null && livingentity.isAlive()) {
                this.target = livingentity;
                return mob.canAttack(target);
            } else {
                return false;
            }
        }

        public boolean canContinueToUse() {
            return this.canUse();
        }

        public void stop() {
            this.target = null;
            this.seeTime = 0;
            this.spellAttackDelay = -1;
            this.mob.setAggressive(false);
            this.mob.getMoveControl().strafe(0, 0);
            this.mob.getNavigation().stop();
            this.flyingTarget = null;
            this.flyingMovementTimer = 0;
            this.lastHurtTime = -1;
        }

        public boolean requiresUpdateEveryTick() {
            return true;
        }

        public void tick() {
            if (target == null) {
                return;
            }

            if (target.isDeadOrDying()) {
                LivingEntity newTarget = findNearbyTarget();
                if (newTarget != null) {
                    this.target = newTarget;
                    this.mob.setTarget(newTarget);
                    this.seeTime = 0;
                    this.spellAttackDelay = Math.max(this.spellAttackDelay, 10);
                } else {
                    return;
                }
            }

            double distanceSquared = this.mob.distanceToSqr(this.target.getX(), this.target.getY(), this.target.getZ());
            hasLineOfSight = this.mob.getSensing().hasLineOfSight(this.target);
            if (hasLineOfSight) {
                this.seeTime++;
            } else {
                this.seeTime--;
            }

            //default mage movement
            doMovement(distanceSquared);

            //do attacks
            if (mob.getLastHurtByMobTimestamp() == mob.tickCount - 1) {
                spellAttackDelay = (int) (Mth.lerp(.6f, spellAttackDelay, 0) + 1);
                lastHurtTime = mob.tickCount;
            }

            //default attack timer
            handleAttackLogic(distanceSquared);

            singleUseDelay--;
            flyingMovementTimer--;
        }

        protected void handleAttackLogic(double distanceSquared) {
            if (seeTime < -50) {
                return;
            }
            if (--this.spellAttackDelay == 0) {
                resetSpellAttackTimer(distanceSquared);
                if (!spellCastingMob.isCasting() && !spellCastingMob.isDrinkingPotion()) {
                    doSpellAction();
                }

            } else if (this.spellAttackDelay < 0) {
                resetSpellAttackTimer(distanceSquared);
            }
            if (spellCastingMob.isCasting()) {
                var spellData = MagicData.getPlayerMagicData(mob).getCastingSpell();
                if (target.isDeadOrDying() || spellData.getSpell().shouldAIStopCasting(spellData.getLevel(), mob, target)) {
                    spellCastingMob.cancelCast();
                }
            }
        }

        public boolean isActing() {
            return spellCastingMob.isCasting() || spellCastingMob.isDrinkingPotion();
        }

        protected void resetSpellAttackTimer(double distanceSquared) {
            float f = (float) Math.sqrt(distanceSquared) / this.spellcastingRange;
            this.spellAttackDelay = Math.max(1, Mth.floor(f * (float) (this.spellAttackIntervalMax - this.spellAttackIntervalMin) + (float) this.spellAttackIntervalMin));
        }

        protected void doMovement(double distanceSquared) {
            double speed = (spellCastingMob.isCasting() ? .75f : 1f) * movementSpeed();

            if (target != null) {
                mob.lookAt(target, 30, 30);
                if (isFlying && spellCastingMob.isCasting()) {
                    forceLookAtTarget(target);
                }
            }

            if (isFlying) {
                doFlyingMovement(distanceSquared, speed);
            } else {
                doGroundMovement(distanceSquared, speed);
            }
        }

        protected void doFlyingMovement(double distanceSquared, double speed) {
            float fleeDist = .275f;

            // Fleeing movement
            if (allowFleeing && (!spellCastingMob.isCasting() && spellAttackDelay > 10) && --fleeCooldown <= 0 && distanceSquared < spellcastingRangeSqr * (fleeDist * fleeDist)) {
                Vec3 flee = DefaultRandomPos.getPosAway(this.mob, 16, 7, target.position());
                if (flee != null) {
                    flyingTarget = new Vec3(flee.x, flee.y + 3, flee.z);
                    flyingMovementTimer = 60;
                }
            }
            // In range movement
            else if (distanceSquared < spellcastingRangeSqr && seeTime >= 5) {
                boolean shouldGenerateNewTarget = !spellCastingMob.isCasting() &&
                        (flyingTarget == null || flyingMovementTimer <= 0 || mob.position().distanceTo(flyingTarget) < 2);

                if (spellCastingMob.isCasting() && mob.getRandom().nextInt(20) == 0) {
                    shouldGenerateNewTarget = true;
                }

                if (shouldGenerateNewTarget) {
                    double angle = mob.getRandom().nextDouble() * 2 * Math.PI;
                    double radius = 5 + mob.getRandom().nextDouble() * 10;
                    double x = target.getX() + Math.cos(angle) * radius;
                    double z = target.getZ() + Math.sin(angle) * radius;

                    double baseHeight = target.getY();
                    double heightVariation = (mob.getRandom().nextDouble() - 0.5) * 8;
                    double y = Math.max(baseHeight + 2, baseHeight + heightVariation + 3);

                    flyingTarget = new Vec3(x, y, z);
                    flyingMovementTimer = spellCastingMob.isCasting() ? 60 : 30 + mob.getRandom().nextInt(30);
                }

                if (flyingTarget != null) {
                    double flyingSpeed = spellCastingMob.isCasting() ? speed * 0.4 : speed;

                    Vec3 direction = flyingTarget.subtract(mob.position()).normalize();
                    Vec3 movement = direction.scale(flyingSpeed * 0.1);
                    mob.setDeltaMovement(movement);
                }
            }
            // Out of range movement
            else {
                if (mob.tickCount % 5 == 0 || flyingTarget == null) {
                    double targetY = target.getY() + 2 + mob.getRandom().nextDouble() * 3;
                    flyingTarget = new Vec3(target.getX(), targetY, target.getZ());
                    flyingMovementTimer = 20;
                }
            }
        }

        protected void doGroundMovement(double distanceSquared, double speed) {
            // Default movement
            float fleeDist = .275f;
            float ss = getStrafeMultiplier();
            if (allowFleeing && (!spellCastingMob.isCasting() && spellAttackDelay > 10) && --fleeCooldown <= 0 && distanceSquared < spellcastingRangeSqr * (fleeDist * fleeDist)) {
                Vec3 flee = DefaultRandomPos.getPosAway(this.mob, 16, 7, target.position());
                if (flee != null) {
                    this.mob.getNavigation().moveTo(flee.x, flee.y, flee.z, speed * 1.5);
                } else {
                    mob.getMoveControl().strafe(-(float) speed * ss, (float) speed * ss);
                }
            } else if (distanceSquared < spellcastingRangeSqr && seeTime >= 5) {
                this.mob.getNavigation().stop();
                if (++strafeTime > 25) {
                    if (mob.getRandom().nextDouble() < .1) {
                        strafingClockwise = !strafingClockwise;
                        strafeTime = 0;
                    }
                }
                float strafeForward = (distanceSquared * 6 < spellcastingRangeSqr ? -1 : .5f) * .2f * (float) speedModifier;
                int strafeDir = strafingClockwise ? 1 : -1;
                mob.getMoveControl().strafe(strafeForward * ss, (float) speed * strafeDir * ss);
                if (mob.horizontalCollision && mob.getRandom().nextFloat() < .1f) {
                    tryJump();
                }
            } else {
                if (mob.tickCount % 5 == 0) {
                    this.mob.getNavigation().moveTo(this.target, speedModifier);
                }
            }
        }

        protected double movementSpeed() {
            return speedModifier * mob.getAttributeValue(Attributes.MOVEMENT_SPEED) * 2;
        }

        protected void tryJump() {
            Vec3 nextBlock = new Vec3(mob.xxa, 0, mob.zza).normalize();
            BlockPos blockpos = BlockPos.containing(mob.position().add(nextBlock));
            BlockState blockstate = this.mob.level().getBlockState(blockpos);
            VoxelShape voxelshape = blockstate.getCollisionShape(this.mob.level(), blockpos);
            if (!voxelshape.isEmpty() && !blockstate.is(BlockTags.DOORS) && !blockstate.is(BlockTags.FENCES)) {
                BlockPos blockposAbove = blockpos.above();
                BlockState blockstateAbove = this.mob.level().getBlockState(blockposAbove);
                VoxelShape voxelshapeAbove = blockstateAbove.getCollisionShape(this.mob.level(), blockposAbove);
                if (voxelshapeAbove.isEmpty()) {
                    this.mob.getJumpControl().jump();
                    mob.setXxa(mob.xxa * 5);
                    mob.setZza(mob.zza * 5);
                }
            }
        }

        protected void doSpellAction() {
            if (!spellCastingMob.getHasUsedSingleAttack() && singleUseSpell != SpellRegistry.none() && singleUseDelay <= 0) {
                spellCastingMob.setHasUsedSingleAttack(true);
                spellCastingMob.initiateCastSpell(singleUseSpell, singleUseLevel);
                fleeCooldown = 7 + singleUseSpell.getCastTime(singleUseLevel);
            } else {
                var spell = getNextSpellType();
                int spellLevel = (int) (spell.getMaxLevel() * Mth.lerp(mob.getRandom().nextFloat(), minSpellQuality, maxSpellQuality));
                spellLevel = Math.max(spellLevel, 1);

                if (!spell.shouldAIStopCasting(spellLevel, mob, target)) {
                    spellCastingMob.initiateCastSpell(spell, spellLevel);
                    fleeCooldown = 7 + spell.getCastTime(spellLevel);
                } else {
                    spellAttackDelay = 5;
                }
            }
        }

        protected AbstractSpell getNextSpellType() {
            NavigableMap<Integer, ArrayList<AbstractSpell>> weightedSpells = new TreeMap<>();
            int attackWeight = getAttackWeight();
            int defenseWeight = getDefenseWeight() - (lastSpellCategory == defenseSpells ? 100 : 0);
            int movementWeight = getMovementWeight() - (lastSpellCategory == movementSpells ? 50 : 0);
            int supportWeight = getSupportWeight() - (lastSpellCategory == supportSpells ? 100 : 0);
            int total = 0;

            if (!attackSpells.isEmpty() && attackWeight > 0) {
                total += attackWeight;
                weightedSpells.put(total, getFilteredAttackSpells());
            }
            if (!defenseSpells.isEmpty() && defenseWeight > 0) {
                total += defenseWeight;
                weightedSpells.put(total, getFilteredDefenseSpells());
            }
            if (!movementSpells.isEmpty() && movementWeight > 0) {
                total += movementWeight;
                weightedSpells.put(total, getFilteredMovementSpells());
            }
            if ((!supportSpells.isEmpty() || drinksPotions) && supportWeight > 0) {
                total += supportWeight;
                weightedSpells.put(total, getFilteredSupportSpells());
            }

            if (total > 0) {
                int seed = mob.getRandom().nextInt(total);
                var spellList = weightedSpells.higherEntry(seed).getValue();
                lastSpellCategory = spellList;

                if (drinksPotions && spellList == supportSpells) {
                    if (supportSpells.isEmpty() || mob.getRandom().nextFloat() < .5f) {
                        spellCastingMob.startDrinkingPotion();
                        return SpellRegistry.none();
                    }
                }
                return spellList.get(mob.getRandom().nextInt(spellList.size()));
            } else {
                return SpellRegistry.none();
            }
        }

        protected ArrayList<AbstractSpell> getFilteredAttackSpells() {
            if (target == null) return new ArrayList<>(attackSpells);

            double distance = Math.sqrt(mob.distanceToSqr(target));

            // Choose priority
            List<AbstractSpell> rangeSpells = new ArrayList<>();
            if (distance <= 3) {
                rangeSpells = filterSpellsByTags(attackSpells, ModTags.CLOSE_RANGE_ATTACKS);
                if (rangeSpells.isEmpty()) {
                    rangeSpells = filterSpellsByTags(attackSpells, ModTags.MID_RANGE_ATTACKS);
                    if (rangeSpells.isEmpty()) {
                        rangeSpells = filterSpellsByTags(attackSpells, ModTags.LONG_RANGE_ATTACKS);
                    }
                }
            } else if (distance <= 6) {
                rangeSpells = filterSpellsByTags(attackSpells, ModTags.MID_RANGE_ATTACKS);
                if (rangeSpells.isEmpty()) {
                    rangeSpells = filterSpellsByTags(attackSpells, ModTags.LONG_RANGE_ATTACKS);
                    if (rangeSpells.isEmpty()) {
                        rangeSpells = filterSpellsByTags(attackSpells, ModTags.CLOSE_RANGE_ATTACKS);
                    }
                }
            } else {
                rangeSpells = filterSpellsByTags(attackSpells, ModTags.LONG_RANGE_ATTACKS);
                if (rangeSpells.isEmpty()) {
                    rangeSpells = filterSpellsByTags(attackSpells, ModTags.MID_RANGE_ATTACKS);
                    if (rangeSpells.isEmpty()) {
                        rangeSpells = filterSpellsByTags(attackSpells, ModTags.CLOSE_RANGE_ATTACKS);
                    }
                }
            }

            if (rangeSpells.isEmpty()) {
                rangeSpells = new ArrayList<>(attackSpells);
            }

            int entitiesNearTarget = getEntitiesNearTarget();
            List<AbstractSpell> finalSpells = new ArrayList<>();

            if (entitiesNearTarget >= 2) {
                // Priority AOE
                finalSpells = filterSpellsByTags(rangeSpells, ModTags.AOE_ATTACKS);
                if (finalSpells.isEmpty()) {
                    finalSpells = filterSpellsByTags(rangeSpells, ModTags.SINGLE_TARGET_ATTACKS);
                }
            } else {
                // Priority Single Target
                finalSpells = filterSpellsByTags(rangeSpells, ModTags.SINGLE_TARGET_ATTACKS);
                if (finalSpells.isEmpty()) {
                    finalSpells = filterSpellsByTags(rangeSpells, ModTags.AOE_ATTACKS);
                }
            }

            return finalSpells.isEmpty() ? new ArrayList<>(rangeSpells) : new ArrayList<>(finalSpells);
        }

        protected ArrayList<AbstractSpell> getFilteredDefenseSpells() {
            List<AbstractSpell> filteredSpells = new ArrayList<>();

            int timeSinceHurt = mob.tickCount - lastHurtTime;
            if (lastHurtTime == -1 || timeSinceHurt > 100) {
                return new ArrayList<>();
            }

            if (timeSinceHurt < 20) {
                return new ArrayList<>();
            }

            boolean hasCloseEnemies = hasEntitiesInRange(3);

            if (hasCloseEnemies) {
                filteredSpells = filterSpellsByTags(defenseSpells, ModTags.ATTACK_BACK_DEFENSE);

                if (filteredSpells.isEmpty()) {
                    List<AbstractSpell> escapeSpells = filterSpellsByTags(movementSpells, ModTags.ESCAPE_MOVEMENT);
                    if (!escapeSpells.isEmpty()) {
                        filteredSpells = escapeSpells;
                    } else {
                        List<AbstractSpell> selfBuffSpells = filterSpellsByTags(defenseSpells, ModTags.SELF_BUFF_DEFENSE);
                        List<AbstractSpell> availableSelfBuffs = filterSpellsWithoutExistingBuffs(selfBuffSpells, mob);

                        if (!availableSelfBuffs.isEmpty()) {
                            filteredSpells = availableSelfBuffs;
                        }
                    }
                }
            } else {
                List<AbstractSpell> selfBuffSpells = filterSpellsByTags(defenseSpells, ModTags.SELF_BUFF_DEFENSE);
                List<AbstractSpell> availableSelfBuffs = filterSpellsWithoutExistingBuffs(selfBuffSpells, mob);

                if (!availableSelfBuffs.isEmpty()) {
                    filteredSpells = availableSelfBuffs;
                } else {
                    filteredSpells = filterSpellsByTags(defenseSpells, ModTags.ATTACK_BACK_DEFENSE);
                }
            }

            if(mob.getHealth() >= mob.getMaxHealth()){
                filteredSpells.remove(SpellRegistry.HEAL_SPELL.get());
                filteredSpells.remove(SpellRegistry.GREATER_HEAL_SPELL.get());
            }

            return filteredSpells.isEmpty() ? new ArrayList<>() : new ArrayList<>(filteredSpells);
        }

        protected ArrayList<AbstractSpell> getFilteredMovementSpells() {
            if (target == null) return new ArrayList<>(movementSpells);

            double targetDistance = Math.sqrt(mob.distanceToSqr(target));
            boolean hasCloseHostiles = hasHostileEntitiesInRange(3);

            List<AbstractSpell> filteredSpells = new ArrayList<>();

            if (hasCloseHostiles) {
                filteredSpells = filterSpellsByTags(movementSpells, ModTags.ESCAPE_MOVEMENT);
                if (filteredSpells.isEmpty()) {
                    filteredSpells = filterSpellsByTags(movementSpells, ModTags.CLOSE_DISTANCE_MOVEMENT);
                }
            } else if (targetDistance > 5) {
                filteredSpells = filterSpellsByTags(movementSpells, ModTags.CLOSE_DISTANCE_MOVEMENT);
                if (filteredSpells.isEmpty()) {
                    filteredSpells = filterSpellsByTags(movementSpells, ModTags.ESCAPE_MOVEMENT);
                }
            } else {
                filteredSpells = filterSpellsByTags(movementSpells, ModTags.CLOSE_DISTANCE_MOVEMENT);
                if (filteredSpells.isEmpty()) {
                    filteredSpells = filterSpellsByTags(movementSpells, ModTags.ESCAPE_MOVEMENT);
                }
            }

            return filteredSpells.isEmpty() ? new ArrayList<>(movementSpells) : new ArrayList<>(filteredSpells);
        }

        protected ArrayList<AbstractSpell> getFilteredSupportSpells() {
            float healthPercentage = mob.getHealth() / mob.getMaxHealth();
            List<AbstractSpell> filteredSpells = new ArrayList<>();

            if (healthPercentage > 0.5f) {
                // More than 50% health
                List<AbstractSpell> safeBuffs = filterSpellsByTags(supportSpells, ModTags.SAFE_BUFF_BUFFING);
                List<AbstractSpell> availableSafeBuffs = filterSpellsWithoutExistingBuffs(safeBuffs, mob);

                List<AbstractSpell> debuffs = filterSpellsByTags(supportSpells, ModTags.DEBUFF_BUFFING);
                List<AbstractSpell> availableDebuffs = filterSpellsWithoutExistingDebuffs(debuffs, target);

                filteredSpells.addAll(availableSafeBuffs);
                filteredSpells.addAll(availableDebuffs);
            } else {
                // Less than 50% health
                List<AbstractSpell> unsafeBuffs = filterSpellsByTags(supportSpells, ModTags.UNSAFE_BUFF_BUFFING);
                List<AbstractSpell> availableUnsafeBuffs = filterSpellsWithoutExistingBuffs(unsafeBuffs, mob);

                List<AbstractSpell> debuffs = filterSpellsByTags(supportSpells, ModTags.DEBUFF_BUFFING);
                List<AbstractSpell> availableDebuffs = filterSpellsWithoutExistingDebuffs(debuffs, target);

                // Unsafe buffs have more chance (only if available)
                for (int i = 0; i < 3; i++) {
                    filteredSpells.addAll(availableUnsafeBuffs);
                }
                filteredSpells.addAll(availableDebuffs);
            }

            if(mob.getHealth() >= mob.getMaxHealth()){
                filteredSpells.remove(SpellRegistry.HEAL_SPELL.get());
            }

            if(!hasHarmfulEffects(mob)){
                filteredSpells.remove(SpellRegistry.CLEANSE_SPELL.get());
            }

            return filteredSpells.isEmpty() ? new ArrayList<>(supportSpells) : new ArrayList<>(filteredSpells);
        }

        public static boolean hasHarmfulEffects(LivingEntity entity) {
            for (MobEffectInstance effect : entity.getActiveEffects()) {
                Holder<MobEffect> mobEffect = effect.getEffect();

                if (!mobEffect.value().isBeneficial()) {
                    return true;
                }
            }
            return false;
        }

        protected List<AbstractSpell> filterSpellsByTags(List<AbstractSpell> spells, TagKey<AbstractSpell> tag) {
            var list = new ArrayList<AbstractSpell>();

            for (var spell : spells) {
                SpellRegistry.REGISTRY.getHolder(spell.getSpellResource()).ifPresent(a -> {
                    if (a.is(tag)) {
                        list.add(spell);
                    }
                });
            }

            return list;
        }

        protected List<AbstractSpell> filterSpellsWithoutExistingBuffs(List<AbstractSpell> spells, LivingEntity entity) {
            if (entity == null) return new ArrayList<>(spells);

            List<AbstractSpell> availableSpells = new ArrayList<>();

            for (AbstractSpell spell : spells) {
                Holder<MobEffect> effect = buffs.get(spell);
                if (effect == null || !entity.hasEffect(effect)) {
                    availableSpells.add(spell);
                }
            }

            return availableSpells;
        }

        protected List<AbstractSpell> filterSpellsWithoutExistingDebuffs(List<AbstractSpell> spells, LivingEntity targetEntity) {
            if (targetEntity == null) return new ArrayList<>(spells);

            List<AbstractSpell> availableSpells = new ArrayList<>();

            for (AbstractSpell spell : spells) {
                if(spell == SpellRegistry.ROOT_SPELL.get()){
                    if(!target.getType().is(io.redspace.ironsspellbooks.util.ModTags.CANT_ROOT)){
                        availableSpells.add(spell);
                    }
                } else {
                    Holder<MobEffect> effect = debuffs.get(spell);
                    if (effect == null || !targetEntity.hasEffect(effect)) {
                        availableSpells.add(spell);
                    }
                }
            }

            return availableSpells;
        }

        protected int getEntitiesNearTarget() {
            if (target == null) return 0;

            AABB area = target.getBoundingBox().inflate(3.0);
            return mob.level().getEntitiesOfClass(LivingEntity.class, area,
                    entity -> entity != target && entity != mob && entity.isAlive()).size();
        }

        protected boolean hasEntitiesInRange(double range) {
            AABB area = mob.getBoundingBox().inflate(range);
            return !mob.level().getEntitiesOfClass(LivingEntity.class, area,
                    entity -> entity != mob && entity.isAlive()).isEmpty();
        }

        protected boolean hasHostileEntitiesInRange(double range) {
            AABB area = mob.getBoundingBox().inflate(range);
            return !mob.level().getEntitiesOfClass(Mob.class, area,
                    entity -> entity != mob && entity.isAlive() &&
                            (entity instanceof Enemy || entity.getTarget() == mob)).isEmpty();
        }

        @Override
        public void start() {
            super.start();
            this.mob.setAggressive(true);
        }

        protected int getAttackWeight() {
            int baseWeight = 80;
            if (!hasLineOfSight || target == null) {
                return 0;
            }

            float targetHealth = target.getHealth() / target.getMaxHealth();
            int targetHealthWeight = (int) ((1 - targetHealth) * baseWeight * .75f);

            double distanceSquared = this.mob.distanceToSqr(this.target.getX(), this.target.getY(), this.target.getZ());
            int distanceWeight = (int) (1 - (distanceSquared / spellcastingRangeSqr) * -60);

            return baseWeight + targetHealthWeight + distanceWeight;
        }

        protected int getDefenseWeight() {
            int baseWeight = -20;

            if (target == null) {
                return baseWeight;
            }

            int timeSinceHurt = mob.tickCount - lastHurtTime;
            if (lastHurtTime == -1 || timeSinceHurt > 100 || timeSinceHurt < 20) {
                return 0;
            }

            float x = mob.getHealth();
            float m = mob.getMaxHealth();
            int healthWeight = (int) (50 * (-(x * x * x) / (m * m * m) + 1));

            float targetHealth = target.getHealth() / target.getMaxHealth();
            int targetHealthWeight = (int) (1 - targetHealth) * -35;

            int threatWeight = projectileCount * 95;

            int recentAttackBonus = 150;

            return baseWeight + healthWeight + targetHealthWeight + threatWeight + recentAttackBonus;
        }

        protected int getMovementWeight() {
            if (target == null) {
                return 0;
            }

            double distanceSquared = this.mob.distanceToSqr(this.target.getX(), this.target.getY(), this.target.getZ());
            double distancePercent = Mth.clamp(distanceSquared / spellcastingRangeSqr, 0, 1);

            int distanceWeight = (int) ((distancePercent) * 50);
            int losWeight = hasLineOfSight ? 0 : 80;

            float healthInverted = 1 - mob.getHealth() / mob.getMaxHealth();
            float distanceInverted = (float) (1 - distancePercent);
            int runWeight = (int) (400 * healthInverted * healthInverted * distanceInverted * distanceInverted);

            return distanceWeight + losWeight + runWeight;
        }

        protected int getSupportWeight() {
            int baseWeight = -15;

            if (target == null) {
                return baseWeight;
            }

            float health = 1 - mob.getHealth() / mob.getMaxHealth();
            int healthWeight = (int) (200 * health);

            double distanceSquared = this.mob.distanceToSqr(this.target.getX(), this.target.getY(), this.target.getZ());
            double distancePercent = Mth.clamp(distanceSquared / spellcastingRangeSqr, 0, 1);
            int distanceWeight = (int) ((1 - distancePercent) * -75);

            return baseWeight + healthWeight + distanceWeight;
        }

        @Override
        public boolean isInterruptable() {
            return !isActing();
        }

        public float getStrafeMultiplier(){
            return 1f;
        }

        protected void forceLookAtTarget(LivingEntity target) {
            if (target != null) {
                double d0 = target.getX() - this.mob.getX();
                double d2 = target.getZ() - this.mob.getZ();
                double d1 = target.getEyeY() - this.mob.getEyeY();

                double d3 = Math.sqrt(d0 * d0 + d2 * d2);
                float f = (float) (Mth.atan2(d2, d0) * (double) (180F / (float) Math.PI)) - 90.0F;
                float f1 = (float) (-(Mth.atan2(d1, d3) * (double) (180F / (float) Math.PI)));
                this.mob.setXRot(f1 % 360);
                this.mob.setYRot(f % 360);
            }
        }

        protected LivingEntity findNearbyTarget() {
            if (mob == null) return null;

            AABB searchArea = mob.getBoundingBox().inflate(10.0);
            List<LivingEntity> nearbyHostiles = mob.level().getEntitiesOfClass(
                    LivingEntity.class,
                    searchArea,
                    entity -> isValidTargetForContinuation(entity)
            );

            if (nearbyHostiles.isEmpty()) {
                return null;
            }

            return findPriorityTarget(nearbyHostiles);
        }

        protected boolean isValidTargetForContinuation(LivingEntity entity) {
            if (entity == null || entity == mob || entity.isDeadOrDying()) {
                return false;
            }

            if (mob instanceof RandomHumanEntity human && human.isAlliedTo(entity)) {
                return false;
            }

            if (entity instanceof Mob hostileMob) {
                LivingEntity hostileTarget = hostileMob.getTarget();
                if (hostileTarget == null) {
                    return false;
                }

                if (hostileTarget == mob) {
                    return true;
                }

                if (mob instanceof RandomHumanEntity human && human.isAlliedTo(hostileTarget)) {
                    return true;
                }

                return false;
            }

            return false;
        }

        protected LivingEntity findPriorityTarget(List<LivingEntity> potentialTargets) {
            if (mob instanceof RandomHumanEntity human) {
                for (LivingEntity entity : potentialTargets) {
                    if (entity instanceof Mob hostileMob) {
                        LivingEntity hostileTarget = hostileMob.getTarget();
                        if (hostileTarget == null) {
                            continue;
                        }

                        if (hostileTarget == mob) {
                            return entity;
                        }

                        if (human.isAlliedTo(hostileTarget)) {
                            return entity;
                        }
                    }
                }
            }
            return null;
        }
    }

    public static class GatherResourcesGoal extends Goal {
        protected final RandomHumanEntity entity;
        protected BlockPos targetPos;
        protected Entity targetEntity;
        protected int gatheringTicks = 0;
        protected int maxGatheringTicks = 60; // 3 seconds
        protected boolean isGathering = false;
        protected GatheringTask currentTask;
        protected int searchCooldown = 0;
        protected int maxSearchCooldown = 200; // 10 seconds between searches
        protected int failedAttempts = 0;
        protected int maxFailedAttempts = 3;

        public GatherResourcesGoal(RandomHumanEntity entity) {
            this.entity = entity;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            // Don't gather if in combat, on standby, or recently failed
            if (entity.getTarget() != null) return false;
            if (entity.isPatrolMode()) return false;
            if (searchCooldown > 0) return false;
            if (failedAttempts >= maxFailedAttempts) return false;

            if (!hasInventorySpace()) {
                return false;
            }

            // First check if we can craft with existing materials
            if (canCraftSomething()) {
                currentTask = null; // No gathering needed, just crafting
                return true;
            }

            // Otherwise check if we need to gather resources
            currentTask = determineNeededTask();
            return currentTask != null;
        }

        @Override
        public boolean canContinueToUse() {
            if (entity.getTarget() != null) return false;
            if (isGathering) return gatheringTicks < maxGatheringTicks;

            // Continue if we have a task or can craft
            return currentTask != null || canCraftSomething();
        }

        @Override
        public void start() {
            if (currentTask != null) {
                findTarget();
            }
            // If currentTask is null, we'll handle crafting in tick()
        }

        @Override
        public void stop() {
            targetPos = null;
            targetEntity = null;
            isGathering = false;
            gatheringTicks = 0;

            // Set cooldown if we failed to find a target
            if (currentTask != null && targetPos == null && targetEntity == null) {
                failedAttempts++;
                searchCooldown = maxSearchCooldown;
            }

            currentTask = null;
        }

        @Override
        public void tick() {
            if (searchCooldown > 0) {
                searchCooldown--;
                return;
            }

            // If we don't have a gathering task, try crafting
            if (currentTask == null) {
                attemptCrafting();
                return;
            }

            if (isGathering) {
                gatheringTicks++;
                if (gatheringTicks >= maxGatheringTicks) {
                    completeGathering();
                }
                return;
            }

            if (targetPos != null) {
                handleBlockTarget();
            } else if (targetEntity != null) {
                handleEntityTarget();
            } else {
                findTarget();
            }
        }

        private boolean hasInventorySpace() {
            SimpleContainer inventory = entity.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                if (inventory.getItem(i).isEmpty()) {
                    return true;
                }
            }
            return false;
        }

        protected void handleBlockTarget() {
            // Verify block still exists and is valid
            if (!isValidBlockTarget(targetPos)) {
                targetPos = null;
                return;
            }

            double distance = entity.distanceToSqr(targetPos.getX(), targetPos.getY(), targetPos.getZ());

            if (distance <= 4.0) { // 2 blocks distance
                entity.getNavigation().stop();
                entity.getLookControl().setLookAt(targetPos.getX(), targetPos.getY(), targetPos.getZ());
                startGathering();
            } else {
                // Check if we can reach the target
                if (!entity.getNavigation().moveTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), 1.0)) {
                    // Can't reach target, find a new one
                    targetPos = null;
                }
            }
        }

        protected void handleEntityTarget() {
            if (!targetEntity.isAlive()) {
                targetEntity = null;
                return;
            }

            double distance = entity.distanceToSqr(targetEntity);

            if (distance <= 4.0) { // 2 blocks distance
                entity.getNavigation().stop();
                entity.getLookControl().setLookAt(targetEntity);
                startGathering();
            } else {
                if (!entity.getNavigation().moveTo(targetEntity, 1.0)) {
                    // Can't reach target, find a new one
                    targetEntity = null;
                }
            }
        }

        protected boolean isValidBlockTarget(BlockPos pos) {
            if (pos == null) return false;

            BlockState state = entity.level().getBlockState(pos);
            Block block = state.getBlock();

            switch (currentTask) {
                case GATHER_LOG -> { return state.is(BlockTags.LOGS); }
                case GATHER_GRAVEL -> { return block == Blocks.GRAVEL; }
                case GATHER_STRING_FROM_WEB -> { return block instanceof WebBlock; }
                default -> { return false; }
            }
        }

        protected void startGathering() {
            if (!isGathering) {
                isGathering = true;
                gatheringTicks = 0;
                entity.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F, 1.0F);
            }
        }

        protected void completeGathering() {
            boolean success = false;

            if (targetPos != null) {
                success = gatherFromBlock();
            } else if (targetEntity != null) {
                success = gatherFromEntity();
            }

            if (success) {
                failedAttempts = 0; // Reset failed attempts on success
                attemptCrafting();
            }

            isGathering = false;
            gatheringTicks = 0;
            targetPos = null;
            targetEntity = null;
            currentTask = null;
        }

        protected boolean gatherFromBlock() {
            if (targetPos == null) return false;

            Level level = entity.level();
            BlockState blockState = level.getBlockState(targetPos);
            Block block = blockState.getBlock();

            // Double-check we can still gather from this block
            if (!isValidBlockTarget(targetPos)) {
                return false;
            }

            if (block.defaultBlockState().is(BlockTags.LOGS)) {
                // Give 1-2 logs
                ItemStack logStack = new ItemStack(block.asItem(), 1 + entity.getRandom().nextInt(2));
                entity.getInventory().addItem(logStack);
                level.destroyBlock(targetPos, false);
                entity.playSound(SoundEvents.WOOD_BREAK, 1.0F, 1.0F);
                return true;
            } else if (block == Blocks.GRAVEL) {
                // Give 1-3 gravel
                ItemStack gravelStack = new ItemStack(Items.GRAVEL, 1 + entity.getRandom().nextInt(3));
                entity.getInventory().addItem(gravelStack);
                level.destroyBlock(targetPos, false);
                entity.playSound(SoundEvents.GRAVEL_BREAK, 1.0F, 1.0F);
                return true;
            } else if (block instanceof WebBlock) {
                // Give 3 string from cobweb
                ItemStack stringStack = new ItemStack(Items.STRING, 3);
                entity.getInventory().addItem(stringStack);
                level.destroyBlock(targetPos, false);
                entity.playSound(SoundEvents.WOOL_BREAK, 1.0F, 1.0F);
                return true;
            }

            return false;
        }

        protected boolean gatherFromEntity() {
            if (targetEntity == null || !targetEntity.isAlive()) return false;

            if (targetEntity instanceof Spider spider) {
                // Get 1-2 string from spider (don't kill it, just hurt it)
                ItemStack stringStack = new ItemStack(Items.STRING, 1 + entity.getRandom().nextInt(2));
                entity.getInventory().addItem(stringStack);
                spider.hurt(entity.level().damageSources().mobAttack(entity), 2.0F);
                entity.playSound(SoundEvents.SPIDER_HURT, 1.0F, 1.0F);
                return true;
            } else if (targetEntity instanceof Chicken chicken) {
                // Get 1-2 feathers from chicken (don't kill it, just hurt it)
                ItemStack featherStack = new ItemStack(Items.FEATHER, 1 + entity.getRandom().nextInt(2));
                entity.getInventory().addItem(featherStack);
                chicken.hurt(entity.level().damageSources().mobAttack(entity), 2.0F);
                entity.playSound(SoundEvents.CHICKEN_HURT, 1.0F, 1.0F);
                return true;
            }

            return false;
        }

        protected void findTarget() {
            if (currentTask == null) return;

            switch (currentTask) {
                case GATHER_LOG -> findLogBlock();
                case GATHER_GRAVEL -> findGravelBlock();
                case GATHER_STRING_FROM_WEB -> findCobwebBlock();
                case GATHER_STRING_FROM_SPIDER -> findSpider();
                case GATHER_FEATHER -> findChicken();
            }
        }

        protected void findLogBlock() {
            targetPos = findNearestBlock(BlockTags.LOGS, 32);
        }

        protected void findGravelBlock() {
            targetPos = findNearestBlockOfType(Blocks.GRAVEL, 32);
        }

        protected void findCobwebBlock() {
            targetPos = findNearestBlockOfType(Blocks.COBWEB, 32);
        }

        protected void findSpider() {
            targetEntity = findNearestEntity(Spider.class, 32);
        }

        protected void findChicken() {
            targetEntity = findNearestEntity(Chicken.class, 32);
        }

        protected BlockPos findNearestBlock(net.minecraft.tags.TagKey<Block> blockTag, int radius) {
            BlockPos entityPos = entity.blockPosition();
            BlockPos closest = null;
            double closestDistance = Double.MAX_VALUE;

            for (int r = 1; r <= radius; r++) {
                for (int x = -r; x <= r; x++) {
                    for (int z = -r; z <= r; z++) {
                        for (int y = -3; y <= 3; y++) {
                            BlockPos checkPos = entityPos.offset(x, y, z);
                            if (entity.level().getBlockState(checkPos).is(blockTag)) {
                                double distance = entityPos.distSqr(checkPos);
                                if (distance < closestDistance) {
                                    closest = checkPos;
                                    closestDistance = distance;
                                }
                            }
                        }
                    }
                }
                // If we found something within this radius, return it
                if (closest != null) break;
            }
            return closest;
        }

        protected BlockPos findNearestBlockOfType(Block targetBlock, int radius) {
            BlockPos entityPos = entity.blockPosition();
            BlockPos closest = null;
            double closestDistance = Double.MAX_VALUE;

            for (int r = 1; r <= radius; r++) {
                for (int x = -r; x <= r; x++) {
                    for (int z = -r; z <= r; z++) {
                        for (int y = -3; y <= 3; y++) {
                            BlockPos checkPos = entityPos.offset(x, y, z);
                            if (entity.level().getBlockState(checkPos).getBlock() == targetBlock) {
                                double distance = entityPos.distSqr(checkPos);
                                if (distance < closestDistance) {
                                    closest = checkPos;
                                    closestDistance = distance;
                                }
                            }
                        }
                    }
                }
                // If we found something within this radius, return it
                if (closest != null) break;
            }
            return closest;
        }

        protected <T extends Entity> T findNearestEntity(Class<T> entityClass, int radius) {
            AABB searchArea = AABB.ofSize(entity.position(), radius * 2, radius * 2, radius * 2);

            return entity.level().getEntitiesOfClass(entityClass, searchArea)
                    .stream()
                    .filter(e -> e != entity && e.isAlive())
                    .filter(e -> !isProtectedEntity(e))
                    .min(Comparator.comparing(e -> e.distanceToSqr(entity)))
                    .orElse(null);
        }

        protected boolean isProtectedEntity(Entity entity) {
            if (entity instanceof LivingEntity livingEntity) {
                return livingEntity.isAlliedTo(this.entity)
                        || (this.entity.getSummoner() != null && livingEntity.isAlliedTo(this.entity.getSummoner()));
            }
            return false;
        }

        protected boolean canCraftSomething() {
            switch (entity.getEntityClass()) {
                case WARRIOR -> {
                    if (entity.getMainHandItem().isEmpty() && hasAnyLog()) {
                        return true; // Can craft sword
                    }
                    if (entity.hasShield() && entity.getOffhandItem().isEmpty() && hasLogs(2)) {
                        return true; // Can craft shield
                    }
                }
                case ROGUE -> {
                    if (entity.isArcher()) {
                        if (entity.getMainHandItem().isEmpty() && hasAnyLog() && hasString(3)) {
                            return true; // Can craft bow
                        }
                        if (!entity.hasArrows() && hasAnyLog() && hasGravel() && hasFeather()) {
                            return true; // Can craft arrows
                        }
                    } else {
                        if (entity.getMainHandItem().isEmpty() && hasAnyLog()) {
                            return true; // Can craft sword
                        }
                    }
                }
            }
            return false;
        }

        protected GatheringTask determineNeededTask() {
            switch (entity.getEntityClass()) {
                case WARRIOR -> {
                    // Warrior needs wooden sword
                    if (entity.getMainHandItem().isEmpty() && !hasAnyLog()) {
                        return GatheringTask.GATHER_LOG;
                    }
                    // Warrior with shield needs more logs for shield
                    if (entity.hasShield() && entity.getOffhandItem().isEmpty() && hasLogs(1) && !hasLogs(2)) {
                        return GatheringTask.GATHER_LOG;
                    }
                }
                case ROGUE -> {
                    if (entity.isArcher()) {
                        // Archer needs bow materials
                        if (entity.getMainHandItem().isEmpty() && !hasAnyLog()) {
                            return GatheringTask.GATHER_LOG;
                        }
                        if (entity.getMainHandItem().isEmpty() && hasAnyLog() && !hasString(3)) {
                            // Need string for bow - prefer cobwebs over spiders
                            if (entity.getRandom().nextFloat() < 0.7f) {
                                return GatheringTask.GATHER_STRING_FROM_WEB;
                            } else {
                                return GatheringTask.GATHER_STRING_FROM_SPIDER;
                            }
                        }
                        // Need arrow materials
                        if (!entity.hasArrows() && hasAnyLog() && !hasGravel()) {
                            return GatheringTask.GATHER_GRAVEL;
                        }
                        if (!entity.hasArrows() && hasAnyLog() && hasGravel() && !hasFeather()) {
                            return GatheringTask.GATHER_FEATHER;
                        }
                    } else {
                        // Assassin needs sword
                        if (entity.getMainHandItem().isEmpty() && !hasAnyLog()) {
                            return GatheringTask.GATHER_LOG;
                        }
                    }
                }
                case MAGE -> {
                    // Mages don't need to gather resources for weapons
                    return null;
                }
            }
            return null;
        }

        protected void attemptCrafting() {
            boolean crafted = false;

            switch (entity.getEntityClass()) {
                case WARRIOR -> {
                    if (entity.getMainHandItem().isEmpty() && hasAnyLog()) {
                        craftWoodenSword();
                        crafted = true;
                    }
                    if (entity.hasShield() && entity.getOffhandItem().isEmpty() && hasLogs(2)) {
                        craftShield();
                        crafted = true;
                    }
                }
                case ROGUE -> {
                    if (entity.isArcher()) {
                        if (entity.getMainHandItem().isEmpty() && hasAnyLog() && hasString(3)) {
                            craftBow();
                            crafted = true;
                        }
                        if (!entity.hasArrows() && hasAnyLog() && hasGravel() && hasFeather()) {
                            craftArrows();
                            crafted = true;
                        }
                    } else {
                        if (entity.getMainHandItem().isEmpty() && hasAnyLog()) {
                            craftWoodenSword();
                            crafted = true;
                        }
                    }
                }
            }

            if (crafted) {
                failedAttempts = 0; // Reset failed attempts on successful crafting
            }
        }

        // Resource checking methods
        protected boolean hasAnyLog() {
            for (int i = 0; i < entity.getInventory().getContainerSize(); i++) {
                ItemStack stack = entity.getInventory().getItem(i);
                if (stack.is(ItemTags.LOGS) && !stack.isEmpty()) {
                    return true;
                }
            }
            return false;
        }

        protected boolean hasLogs(int count) {
            int totalLogs = 0;
            for (int i = 0; i < entity.getInventory().getContainerSize(); i++) {
                ItemStack stack = entity.getInventory().getItem(i);
                if (stack.is(ItemTags.LOGS)) {
                    totalLogs += stack.getCount();
                }
            }
            return totalLogs >= count;
        }

        protected boolean hasString(int count) {
            return entity.getInventory().countItem(Items.STRING) >= count;
        }

        protected boolean hasGravel() {
            return entity.getInventory().countItem(Items.GRAVEL) > 0;
        }

        protected boolean hasFeather() {
            return entity.getInventory().countItem(Items.FEATHER) > 0;
        }

        protected ItemStack getAnyLog() {
            for (int i = 0; i < entity.getInventory().getContainerSize(); i++) {
                ItemStack stack = entity.getInventory().getItem(i);
                if (stack.is(ItemTags.LOGS) && !stack.isEmpty()) {
                    return stack;
                }
            }
            return ItemStack.EMPTY;
        }

        protected void consumeItem(ItemStack item, int count) {
            int remaining = count;
            for (int i = 0; i < entity.getInventory().getContainerSize() && remaining > 0; i++) {
                ItemStack stack = entity.getInventory().getItem(i);
                if (stack.is(item.getItem())) {
                    int toConsume = Math.min(remaining, stack.getCount());
                    stack.shrink(toConsume);
                    remaining -= toConsume;
                    if (stack.isEmpty()) {
                        entity.getInventory().setItem(i, ItemStack.EMPTY);
                    }
                }
            }
        }

        // Crafting methods
        protected void craftWoodenSword() {
            ItemStack log = getAnyLog();
            if (!log.isEmpty()) {
                consumeItem(log, 1);
                ItemStack sword = new ItemStack(Items.WOODEN_SWORD);
                entity.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, sword);
                entity.playSound(SoundEvents.CRAFTER_CRAFT, 0.8F, 1.2F);
                MagicRealms.LOGGER.debug("Entity {} crafted wooden sword", entity.getEntityName());
            }
        }

        protected void craftShield() {
            ItemStack log = getAnyLog();
            if (!log.isEmpty() && hasLogs(2)) {
                consumeItem(log, 2);
                ItemStack shield = new ItemStack(Items.SHIELD);
                entity.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, shield);
                entity.playSound(SoundEvents.CRAFTER_CRAFT, 0.8F, 1.2F);
                MagicRealms.LOGGER.debug("Entity {} crafted shield", entity.getEntityName());
            }
        }

        protected void craftBow() {
            ItemStack log = getAnyLog();
            if (!log.isEmpty() && hasString(3)) {
                consumeItem(log, 1);
                consumeItem(new ItemStack(Items.STRING), 3);
                ItemStack bow = new ItemStack(Items.BOW);
                entity.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, bow);
                entity.playSound(SoundEvents.CRAFTER_CRAFT, 0.8F, 1.2F);
                MagicRealms.LOGGER.debug("Entity {} crafted bow", entity.getEntityName());
            }
        }

        protected void craftArrows() {
            ItemStack log = getAnyLog();
            if (!log.isEmpty() && hasGravel() && hasFeather()) {
                consumeItem(log, 1);
                consumeItem(new ItemStack(Items.GRAVEL), 1);
                consumeItem(new ItemStack(Items.FEATHER), 1);
                ItemStack arrows = new ItemStack(Items.ARROW, 10);
                entity.getInventory().addItem(arrows);
                entity.playSound(SoundEvents.CRAFTER_CRAFT, 0.8F, 1.2F);
                MagicRealms.LOGGER.debug("Entity {} crafted 10 arrows", entity.getEntityName());
            }
        }

        protected enum GatheringTask {
            GATHER_LOG,
            GATHER_GRAVEL,
            GATHER_STRING_FROM_WEB,
            GATHER_STRING_FROM_SPIDER,
            GATHER_FEATHER
        }
    }

    public static class HumanFollowOwnerGoal extends Goal {
        private final RandomHumanEntity humanEntity;
        @Nullable
        private Entity owner;
        private Supplier<Entity> ownerGetter;
        private final double speedModifier;
        private final PathNavigation navigation;
        private int timeToRecalcPath;
        private final float stopDistance;
        private final float startDistance;
        private float oldWaterCost;
        private float teleportDistance;
        private boolean canFly;

        public HumanFollowOwnerGoal(RandomHumanEntity humanEntity, Supplier<Entity> ownerGetter, double speedModifier,
                                    float startDistance, float stopDistance, boolean canFly, float teleportDistance) {
            this.humanEntity = humanEntity;
            this.ownerGetter = ownerGetter;
            this.speedModifier = speedModifier;
            this.navigation = humanEntity.getNavigation();
            this.startDistance = startDistance;
            this.stopDistance = stopDistance;
            this.teleportDistance = teleportDistance;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
            this.canFly = canFly;
        }

        @Override
        public boolean canUse() {
            // Don't follow if in patrol mode
            if (humanEntity.isPatrolMode()) {
                return false;
            }

            Entity livingentity = this.ownerGetter.get();
            if (livingentity == null) {
                return false;
            } else if (this.humanEntity.distanceToSqr(livingentity) < (double) (this.startDistance * this.startDistance)) {
                return false;
            } else {
                this.owner = livingentity;
                return true;
            }
        }

        @Override
        public boolean canContinueToUse() {
            // Stop following if entering patrol mode
            if (humanEntity.isPatrolMode()) {
                return false;
            }

            if (this.navigation.isDone()) {
                return false;
            } else {
                return !(this.humanEntity.distanceToSqr(this.owner) <= (double) (this.stopDistance * this.stopDistance));
            }
        }

        @Override
        public void start() {
            this.timeToRecalcPath = 0;
            this.oldWaterCost = this.humanEntity.getPathfindingMalus(PathType.WATER);
            this.humanEntity.setPathfindingMalus(PathType.WATER, 0.0F);
        }

        @Override
        public void stop() {
            this.owner = null;
            this.navigation.stop();
            this.humanEntity.setPathfindingMalus(PathType.WATER, this.oldWaterCost);
        }

        @Override
        public void tick() {
            boolean flag = this.shouldTryTeleportToOwner();
            if (!flag) {
                this.humanEntity.getLookControl().setLookAt(this.owner, 10.0F, (float) this.humanEntity.getMaxHeadXRot());
            }

            if (--this.timeToRecalcPath <= 0) {
                this.timeToRecalcPath = this.adjustedTickDelay(10);
                if (flag) {
                    this.tryToTeleportToOwner();
                } else {
                    if (false && canFly && !humanEntity.onGround()) {
                        Vec3 vec3 = owner.position();
                        this.humanEntity.getMoveControl().setWantedPosition(vec3.x, vec3.y + 2, vec3.z, this.speedModifier);
                    } else {
                        this.navigation.moveTo(this.owner, this.speedModifier);
                    }
                }
            }
        }

        public void tryToTeleportToOwner() {
            Entity livingentity = this.ownerGetter.get();
            if (livingentity != null) {
                this.teleportToAroundBlockPos(livingentity.blockPosition());
            }
        }

        public boolean shouldTryTeleportToOwner() {
            Entity livingentity = this.ownerGetter.get();
            return livingentity != null && humanEntity.distanceToSqr(livingentity) >= teleportDistance * teleportDistance;
        }

        private void teleportToAroundBlockPos(BlockPos pPos) {
            for (int i = 0; i < 10; i++) {
                int j = humanEntity.getRandom().nextIntBetweenInclusive(-3, 3);
                int k = humanEntity.getRandom().nextIntBetweenInclusive(-3, 3);
                if (Math.abs(j) >= 2 || Math.abs(k) >= 2) {
                    int l = humanEntity.getRandom().nextIntBetweenInclusive(-1, 1);
                    if (this.maybeTeleportTo(pPos.getX() + j, pPos.getY() + l, pPos.getZ() + k)) {
                        return;
                    }
                }
            }
        }

        private boolean maybeTeleportTo(int pX, int pY, int pZ) {
            if (!this.canTeleportTo(new BlockPos(pX, pY, pZ))) {
                return false;
            } else {
                humanEntity.moveTo((double) pX + 0.5, (double) pY, (double) pZ + 0.5, humanEntity.getYRot(), humanEntity.getXRot());
                this.navigation.stop();
                return true;
            }
        }

        private boolean canTeleportTo(BlockPos pPos) {
            PathType pathtype = WalkNodeEvaluator.getPathTypeStatic(humanEntity, pPos);
            if (pathtype != PathType.WALKABLE) {
                return false;
            } else {
                BlockState blockstate = humanEntity.level().getBlockState(pPos.below());
                if (!this.canFly && blockstate.getBlock() instanceof LeavesBlock) {
                    return false;
                } else {
                    BlockPos blockpos = pPos.subtract(humanEntity.blockPosition());
                    return humanEntity.level().noCollision(humanEntity, humanEntity.getBoundingBox().move(blockpos));
                }
            }
        }
    }

    public static class PatrolAroundPositionGoal extends Goal {
        private final RandomHumanEntity entity;
        private final double speedModifier;
        private final int patrolRadius;
        private BlockPos targetPos;
        private int patrolCooldown = 0;
        private int stuckTicks = 0;
        private Vec3 lastPosition;

        private static final int PATROL_INTERVAL = 100; // 5 seconds between patrol moves
        private static final int MAX_STUCK_TICKS = 60; // 3 seconds before trying new position
        private static final double MOVEMENT_THRESHOLD = 0.1; // Minimum movement to not be considered stuck
        private static final double ARRIVAL_DISTANCE = 2.0; // How close to get to target position

        public PatrolAroundPositionGoal(RandomHumanEntity entity, double speedModifier, int patrolRadius) {
            this.entity = entity;
            this.speedModifier = speedModifier;
            this.patrolRadius = patrolRadius;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            // Only patrol if in patrol mode and not in combat
            if (!entity.isPatrolMode()) {
                return false;
            }

            if (entity.getTarget() != null) {
                return false; // Don't patrol while in combat
            }

            if (patrolCooldown > 0) {
                patrolCooldown--;
                return false;
            }

            // Check if we have a valid patrol position
            BlockPos patrolCenter = entity.getPatrolPosition();
            if (patrolCenter == null || patrolCenter.equals(BlockPos.ZERO)) {
                // Set current position as patrol center if none set
                entity.setPatrolPosition(entity.blockPosition());
                patrolCenter = entity.getPatrolPosition();
            }

            // Generate a new patrol target position
            return generatePatrolTarget(patrolCenter);
        }

        @Override
        public boolean canContinueToUse() {
            // Stop if no longer in patrol mode or entered combat
            if (!entity.isPatrolMode() || entity.getTarget() != null) {
                return false;
            }

            // Stop if we've reached our target
            if (targetPos != null && entity.distanceToSqr(targetPos.getX(), targetPos.getY(), targetPos.getZ()) <= ARRIVAL_DISTANCE * ARRIVAL_DISTANCE) {
                return false;
            }

            // Stop if we've been stuck for too long
            if (stuckTicks >= MAX_STUCK_TICKS) {
                return false;
            }

            return targetPos != null;
        }

        @Override
        public void start() {
            if (targetPos != null) {
                entity.getNavigation().moveTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), speedModifier);
                stuckTicks = 0;
                lastPosition = entity.position();
            }
        }

        @Override
        public void tick() {
            if (targetPos == null) {
                return;
            }

            // Check if we're stuck
            Vec3 currentPosition = entity.position();
            if (lastPosition != null) {
                double movementDistance = currentPosition.distanceTo(lastPosition);
                if (movementDistance < MOVEMENT_THRESHOLD) {
                    stuckTicks++;
                } else {
                    stuckTicks = 0; // Reset if we're moving
                }
            }
            lastPosition = currentPosition;

            // Try to navigate to the target
            if (entity.getNavigation().isDone() || stuckTicks > 20) {
                // Recalculate path if navigation is done or we've been stuck for a bit
                entity.getNavigation().moveTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), speedModifier);

                // If we're really stuck, try a different position
                if (stuckTicks > 40) {
                    BlockPos patrolCenter = entity.getPatrolPosition();
                    if (patrolCenter != null) {
                        generatePatrolTarget(patrolCenter);
                        if (targetPos != null) {
                            entity.getNavigation().moveTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), speedModifier);
                        }
                    }
                }
            }
        }

        @Override
        public void stop() {
            targetPos = null;
            patrolCooldown = PATROL_INTERVAL;
            stuckTicks = 0;
            lastPosition = null;
            entity.getNavigation().stop();
        }

        private boolean generatePatrolTarget(BlockPos patrolCenter) {
            // Try to find a valid patrol position within the radius
            for (int attempts = 0; attempts < 10; attempts++) {
                double angle = entity.getRandom().nextDouble() * 2 * Math.PI;
                int distance = 3 + entity.getRandom().nextInt(patrolRadius - 2); // Between 3 and patrolRadius blocks

                int xOffset = (int) (Math.cos(angle) * distance);
                int zOffset = (int) (Math.sin(angle) * distance);

                BlockPos candidatePos = patrolCenter.offset(xOffset, 0, zOffset);

                // Find a suitable Y level (ground level)
                candidatePos = findGroundLevel(candidatePos);

                // Check if the position is valid and pathable
                if (isValidPatrolPosition(candidatePos, patrolCenter)) {
                    targetPos = candidatePos;
                    return true;
                }
            }

            // If we can't find a valid position, just stay near the patrol center
            targetPos = findGroundLevel(patrolCenter.offset(
                    entity.getRandom().nextInt(5) - 2,
                    0,
                    entity.getRandom().nextInt(5) - 2
            ));

            return targetPos != null;
        }

        private BlockPos findGroundLevel(BlockPos startPos) {
            Level level = entity.level();

            // Search down for ground
            for (int y = startPos.getY(); y >= startPos.getY() - 5; y--) {
                BlockPos checkPos = new BlockPos(startPos.getX(), y, startPos.getZ());
                BlockPos abovePos = checkPos.above();

                // Check if this is a valid ground position
                if (!level.getBlockState(checkPos).isAir() &&
                        level.getBlockState(abovePos).isAir() &&
                        level.getBlockState(abovePos.above()).isAir()) {
                    return abovePos;
                }
            }

            // Search up for ground if we didn't find any below
            for (int y = startPos.getY() + 1; y <= startPos.getY() + 5; y++) {
                BlockPos checkPos = new BlockPos(startPos.getX(), y, startPos.getZ());
                BlockPos abovePos = checkPos.above();

                if (!level.getBlockState(checkPos).isAir() &&
                        level.getBlockState(abovePos).isAir() &&
                        level.getBlockState(abovePos.above()).isAir()) {
                    return abovePos;
                }
            }

            // If all else fails, return the original position
            return startPos;
        }

        private boolean isValidPatrolPosition(BlockPos pos, BlockPos patrolCenter) {
            if (pos == null) return false;

            Level level = entity.level();

            // Check if position is within patrol radius
            double distanceFromCenter = Math.sqrt(pos.distSqr(patrolCenter));
            if (distanceFromCenter > patrolRadius) {
                return false;
            }

            // Check if the position has solid ground and space above
            BlockState groundState = level.getBlockState(pos.below());
            BlockState posState = level.getBlockState(pos);
            BlockState aboveState = level.getBlockState(pos.above());

            return !groundState.isAir() && // Has solid ground
                    posState.isAir() &&     // Position is air
                    aboveState.isAir() &&   // Space above is air
                    groundState.isSolid();  // Ground is solid
        }
    }

    public static class EmeraldOverflowGoal extends Goal {
        private final RandomHumanEntity entity;
        private Villager targetVillager;
        private int searchCooldown = 0;
        private int interactionCooldown = 0;
        private ExchangeType currentExchangeType = ExchangeType.NONE;

        private static final int SEARCH_INTERVAL = 100; // Search every 5 seconds
        private static final int INTERACTION_INTERVAL = 300; // 15 seconds in ticks
        private static final double SEARCH_RANGE = 32.0;
        private static final int MIN_OVERFLOW_THRESHOLD = 1; // Minimum overflow emeralds to trigger

        private enum ExchangeType {
            NONE,
            CLERIC_BUFF,
            LIBRARIAN_XP
        }

        // Beneficial effects that clerics can provide
        private static final List<Holder<MobEffect>> BENEFICIAL_EFFECTS = List.of(
                MobEffects.REGENERATION,
                MobEffects.DAMAGE_BOOST,
                MobEffects.DAMAGE_RESISTANCE,
                MobEffects.FIRE_RESISTANCE,
                MobEffects.WATER_BREATHING,
                MobEffects.NIGHT_VISION,
                MobEffects.MOVEMENT_SPEED,
                MobEffects.LUCK,
                MobEffects.HEALTH_BOOST,
                MobEffects.ABSORPTION
        );

        public EmeraldOverflowGoal(RandomHumanEntity entity) {
            this.entity = entity;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            // Don't run if on cooldown
            if (searchCooldown > 0 || interactionCooldown > 0) {
                return false;
            }

            if(entity.isPatrolMode()){
                return false;
            }

            // Check if entity has overflow emeralds and no unaffordable offers
            if (!hasOverflowEmeralds()) {
                return false;
            }

            VillagerOffersData offersData = entity.getData(MRDataAttachments.VILLAGER_OFFERS_DATA);
            if (offersData.hasAnyUnaffordableOffers()) {
                return false;
            }

            // Find a suitable villager and determine exchange type
            return findSuitableVillagerAndExchangeType();
        }

        @Override
        public boolean canContinueToUse() {
            return targetVillager != null &&
                    targetVillager.isAlive() &&
                    entity.distanceToSqr(targetVillager) <= SEARCH_RANGE * SEARCH_RANGE &&
                    hasOverflowEmeralds() &&
                    currentExchangeType != ExchangeType.NONE;
        }

        @Override
        public void start() {
            MagicRealms.LOGGER.debug("Entity {} starting emerald overflow goal with villager {} for {}",
                    entity.getEntityName(),
                    targetVillager.getDisplayName().getString(),
                    currentExchangeType);
        }

        @Override
        public void stop() {
            targetVillager = null;
            currentExchangeType = ExchangeType.NONE;
            searchCooldown = SEARCH_INTERVAL;
            interactionCooldown = INTERACTION_INTERVAL;
        }

        @Override
        public void tick() {
            if (searchCooldown > 0) {
                searchCooldown--;
            }
            if (interactionCooldown > 0) {
                interactionCooldown--;
            }

            if (targetVillager == null || currentExchangeType == ExchangeType.NONE) {
                return;
            }

            double distanceToVillager = entity.distanceToSqr(targetVillager);

            // If close enough, interact
            if (distanceToVillager <= 9.0) { // 3 block radius
                if (interactionCooldown <= 0) {
                    performEmeraldExchange();
                    interactionCooldown = INTERACTION_INTERVAL;
                }
            } else {
                // Move towards villager
                Path path = entity.getNavigation().createPath(targetVillager, 0);
                if (path != null) {
                    entity.getNavigation().moveTo(path, 1.0);
                    entity.getLookControl().setLookAt(targetVillager, 30.0F, 30.0F);
                }
            }
        }

        private boolean hasOverflowEmeralds() {
            int totalEmeralds = entity.getInventory().countItem(Items.EMERALD);
            return totalEmeralds > Config.emeraldOverflowThreshold;
        }

        private int getOverflowEmeraldCount() {
            int totalEmeralds = entity.getInventory().countItem(Items.EMERALD);
            return Math.max(0, totalEmeralds - Config.emeraldOverflowThreshold);
        }

        private boolean findSuitableVillagerAndExchangeType() {
            if (!(entity.level() instanceof ServerLevel serverLevel)) {
                return false;
            }

            BlockPos entityPos = entity.blockPosition();
            List<VillagerCandidate> candidates = new ArrayList<>();
            int overflowCount = getOverflowEmeraldCount();

            // Search for villagers in range
            for (Villager villager : serverLevel.getEntitiesOfClass(Villager.class,
                    entity.getBoundingBox().inflate(SEARCH_RANGE))) {

                if (!villager.isAlive() || villager.isBaby()) {
                    continue;
                }

                VillagerProfession profession = villager.getVillagerData().getProfession();
                double distance = entityPos.distSqr(villager.blockPosition());

                // Check if it's a cleric and we have enough emeralds for the buff
                if (profession == VillagerProfession.CLERIC && overflowCount >= Config.clericEmeraldCost) {
                    // Prioritize cleric exchanges as they provide more valuable benefits
                    candidates.add(new VillagerCandidate(villager, ExchangeType.CLERIC_BUFF, distance, 1.0));
                }
            }

            if (candidates.isEmpty()) {
                return false;
            }

            // Sort by priority (higher first), then by distance (closer first)
            candidates.sort((c1, c2) -> {
                int priorityCompare = Double.compare(c2.priority, c1.priority);
                if (priorityCompare != 0) return priorityCompare;
                return Double.compare(c1.distance, c2.distance);
            });

            VillagerCandidate best = candidates.get(0);
            targetVillager = best.villager;
            currentExchangeType = best.exchangeType;

            return true;
        }

        private void performEmeraldExchange() {
            if (targetVillager == null || !targetVillager.isAlive()) {
                return;
            }

            if (Objects.requireNonNull(currentExchangeType) == ExchangeType.CLERIC_BUFF) {
                performClericExchange();
            } else {
                MagicRealms.LOGGER.warn("Entity {} attempted exchange with no valid type", entity.getEntityName());
            }
        }

        private void performClericExchange() {
            int overflowEmeralds = getOverflowEmeraldCount();

            if (overflowEmeralds < Config.clericEmeraldCost) {
                MagicRealms.LOGGER.debug("Entity {} doesn't have enough overflow emeralds for cleric (has: {}, needs: {})",
                        entity.getEntityName(), overflowEmeralds, Config.clericEmeraldCost);
                return;
            }

            // Remove emeralds from inventory
            if (!removeEmeralds(Config.clericEmeraldCost)) {
                return;
            }

            // Give cleric trading experience (simulate a trade)
            if (targetVillager.getVillagerData().getLevel() < 5) {
                targetVillager.setVillagerXp(targetVillager.getVillagerXp() + 2);
            }

            // Play trading sound
            targetVillager.playSound(SoundEvents.VILLAGER_YES, 1.0F, 1.0F);

            // Apply random beneficial effect
            applyClericBuff();

            MagicRealms.LOGGER.info("Entity {} completed cleric exchange: {} emeralds for beneficial effect",
                    entity.getEntityName(), Config.clericEmeraldCost);
        }

        private void applyClericBuff() {
            Holder<MobEffect> randomEffect = BENEFICIAL_EFFECTS.get(entity.getRandom().nextInt(BENEFICIAL_EFFECTS.size()));
            int duration = Config.clericBuffDurationMinutes * 1200;

            // Random amplifier between 0-2 (levels 1-3)
            int amplifier = entity.getRandom().nextInt(3);

            MobEffectInstance effectInstance = new MobEffectInstance(randomEffect, duration, amplifier, false, true, true);
            entity.addEffect(effectInstance);

            MagicRealms.LOGGER.debug("Applied effect {} (Level {}, Duration: {}s) to entity {}",
                    randomEffect.getKey(), amplifier + 1, duration / 20, entity.getEntityName());
        }

        private boolean removeEmeralds(int count) {
            int remaining = count;

            for (int i = 0; i < entity.getInventory().getContainerSize() && remaining > 0; i++) {
                ItemStack stack = entity.getInventory().getItem(i);

                if (stack.is(Items.EMERALD)) {
                    int toRemove = Math.min(remaining, stack.getCount());
                    stack.shrink(toRemove);
                    remaining -= toRemove;

                    if (stack.isEmpty()) {
                        entity.getInventory().setItem(i, ItemStack.EMPTY);
                    }
                }
            }

            boolean success = remaining == 0;
            if (!success) {
                MagicRealms.LOGGER.warn("Entity {} failed to remove {} emeralds from inventory",
                        entity.getEntityName(), count);
            }

            return success;
        }

        private static class VillagerCandidate {
            final Villager villager;
            final ExchangeType exchangeType;
            final double distance;
            final double priority;

            VillagerCandidate(Villager villager, ExchangeType exchangeType, double distance, double priority) {
                this.villager = villager;
                this.exchangeType = exchangeType;
                this.distance = distance;
                this.priority = priority;
            }
        }
    }
}