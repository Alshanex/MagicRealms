package net.alshanex.magic_realms.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.*;

public class VillagerOffersData implements INBTSerializable<CompoundTag> {
    // Map of villager UUID -> List of unaffordable offers
    private final Map<UUID, List<MerchantOffer>> unaffordableOffers = new HashMap<>();

    public VillagerOffersData() {}

    /**
     * Store unaffordable offers, checking against emerald balance instead of inventory emeralds
     */
    public void storeUnaffordableOffersWithBalance(UUID villagerUUID, List<MerchantOffer> offers,
                                                   SimpleContainer inventory, int emeraldBalance) {
        List<MerchantOffer> unaffordable = new ArrayList<>();

        for (MerchantOffer offer : offers) {
            if (!canAffordOfferWithBalance(offer, inventory, emeraldBalance)) {
                unaffordable.add(offer);
            }
        }

        if (!unaffordable.isEmpty()) {
            unaffordableOffers.put(villagerUUID, unaffordable);
        }
    }

    /**
     * Get stored unaffordable offers for a villager
     */
    public List<MerchantOffer> getUnaffordableOffers(UUID villagerUUID) {
        return unaffordableOffers.getOrDefault(villagerUUID, new ArrayList<>());
    }

    /**
     * Check affordability using internal emerald balance instead of physical emeralds
     * This method should be added to the VillagerOffersData class
     */
    public List<MerchantOffer> checkAffordableOffersWithBalance(UUID villagerUUID, SimpleContainer inventory, int emeraldBalance) {
        List<MerchantOffer> unaffordableOffersFromVillager = this.unaffordableOffers.get(villagerUUID);
        if (unaffordableOffersFromVillager == null || unaffordableOffersFromVillager.isEmpty()) {
            return new ArrayList<>();
        }

        List<MerchantOffer> nowAffordable = new ArrayList<>();
        Iterator<MerchantOffer> iterator = unaffordableOffersFromVillager.iterator();

        while (iterator.hasNext()) {
            MerchantOffer offer = iterator.next();

            if (canAffordOfferWithBalance(offer, inventory, emeraldBalance)) {
                nowAffordable.add(offer);
                iterator.remove(); // Remove from unaffordable list
            }
        }

        // Clean up empty lists
        if (unaffordableOffersFromVillager.isEmpty()) {
            this.unaffordableOffers.remove(villagerUUID);
        }

        return nowAffordable;
    }

    /**
     * Check if an offer can be afforded using inventory items and internal emerald balance
     */
    private boolean canAffordOfferWithBalance(MerchantOffer offer, SimpleContainer inventory, int emeraldBalance) {
        ItemStack costA = offer.getCostA();
        ItemStack costB = offer.getCostB();

        // Check first cost
        boolean canAffordA = costA.isEmpty() ||
                (costA.is(Items.EMERALD) ?
                        emeraldBalance >= costA.getCount() :
                        inventory.countItem(costA.getItem()) >= costA.getCount());

        // Check second cost
        boolean canAffordB = costB.isEmpty() ||
                (costB.is(Items.EMERALD) ?
                        emeraldBalance >= costB.getCount() :
                        inventory.countItem(costB.getItem()) >= costB.getCount());

        // Handle combined emerald costs (both costs are emeralds)
        if (costA.is(Items.EMERALD) && costB.is(Items.EMERALD)) {
            int totalEmeraldCost = costA.getCount() + costB.getCount();
            return emeraldBalance >= totalEmeraldCost;
        }

        return canAffordA && canAffordB;
    }

    /**
     * Clear all memory of a villager (when they die)
     */
    public void clearVillagerMemory(UUID villagerUUID) {
        unaffordableOffers.remove(villagerUUID);
    }

    /**
     * Check if we have any stored unaffordable offers
     */
    public boolean hasAnyUnaffordableOffers() {
        return !unaffordableOffers.isEmpty();
    }

    /**
     * Get all villagers we have unaffordable offers for
     */
    public Set<UUID> getVillagersWithUnaffordableOffers() {
        return new HashSet<>(unaffordableOffers.keySet());
    }

    /**
     * Clear all memory (cleanup method)
     */
    public void clearAll() {
        unaffordableOffers.clear();
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();

        CompoundTag villagersTag = new CompoundTag();
        for (Map.Entry<UUID, List<MerchantOffer>> entry : unaffordableOffers.entrySet()) {
            String villagerUUIDStr = entry.getKey().toString();
            List<MerchantOffer> offers = entry.getValue();

            ListTag offersListTag = new ListTag();
            for (MerchantOffer offer : offers) {
                CompoundTag offerTag = new CompoundTag();

                // Serialize the offer components using the provider
                offerTag.put("CostA", offer.getCostA().save(provider));
                if (!offer.getCostB().isEmpty()) {
                    offerTag.put("CostB", offer.getCostB().save(provider));
                }
                offerTag.put("Result", offer.getResult().save(provider));
                offerTag.putInt("Uses", offer.getUses());
                offerTag.putInt("MaxUses", offer.getMaxUses());
                offerTag.putInt("Xp", offer.getXp());
                offerTag.putFloat("PriceMultiplier", offer.getPriceMultiplier());
                offerTag.putInt("Demand", offer.getDemand());
                offerTag.putInt("SpecialPrice", offer.getSpecialPriceDiff());

                offersListTag.add(offerTag);
            }

            villagersTag.put(villagerUUIDStr, offersListTag);
        }

        tag.put("UnaffordableOffers", villagersTag);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        unaffordableOffers.clear();

        if (!tag.contains("UnaffordableOffers")) {
            return;
        }

        CompoundTag villagersTag = tag.getCompound("UnaffordableOffers");
        for (String villagerUUIDStr : villagersTag.getAllKeys()) {
            try {
                UUID villagerUUID = UUID.fromString(villagerUUIDStr);
                ListTag offersListTag = villagersTag.getList(villagerUUIDStr, 10); // 10 = CompoundTag

                List<MerchantOffer> offers = new ArrayList<>();
                for (int i = 0; i < offersListTag.size(); i++) {
                    CompoundTag offerTag = offersListTag.getCompound(i);

                    try {
                        // Deserialize offer components using the provider
                        ItemStack costA = ItemStack.parseOptional(provider, offerTag.getCompound("CostA"));
                        ItemStack costB = ItemStack.EMPTY;
                        if (offerTag.contains("CostB")) {
                            costB = ItemStack.parseOptional(provider, offerTag.getCompound("CostB"));
                        }
                        ItemStack result = ItemStack.parseOptional(provider, offerTag.getCompound("Result"));

                        int uses = offerTag.getInt("Uses");
                        int maxUses = offerTag.getInt("MaxUses");
                        int xp = offerTag.getInt("Xp");
                        float priceMultiplier = offerTag.getFloat("PriceMultiplier");
                        int demand = offerTag.getInt("Demand");

                        // Create the offer using ItemCost
                        ItemCost itemCostA = new ItemCost(costA.getItem(), costA.getCount());
                        Optional<ItemCost> itemCostB = costB.isEmpty() ?
                                Optional.empty() :
                                Optional.of(new ItemCost(costB.getItem(), costB.getCount()));

                        MerchantOffer offer = new MerchantOffer(itemCostA, itemCostB, result, uses, maxUses, xp, priceMultiplier, demand);

                        if (offerTag.contains("SpecialPrice")) {
                            offer.setSpecialPriceDiff(offerTag.getInt("SpecialPrice"));
                        }

                        offers.add(offer);
                    } catch (Exception e) {
                        // Skip invalid offers
                    }
                }

                if (!offers.isEmpty()) {
                    unaffordableOffers.put(villagerUUID, offers);
                }
            } catch (Exception e) {
                // Skip invalid villager UUIDs
            }
        }
    }
}
