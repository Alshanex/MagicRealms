package net.alshanex.magic_realms.screens;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.util.EntitySnapshot;
import net.alshanex.magic_realms.util.humans.EntityClass;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;

public class HumanInfoMenu extends AbstractContainerMenu {
    private final Container equipmentContainer;
    private final EntitySnapshot snapshot;
    private final ItemStack sourceItem;
    private final Player player;

    // Slot indices
    private static final int EQUIPMENT_SLOTS = 6; // head, chest, legs, boots, mainhand, offhand
    private static final int PLAYER_INVENTORY_SLOTS = 36;
    private static final int TOTAL_SLOTS = EQUIPMENT_SLOTS + PLAYER_INVENTORY_SLOTS;

    public HumanInfoMenu(int containerId, Inventory playerInventory, EntitySnapshot snapshot, ItemStack sourceItem) {
        super(net.alshanex.magic_realms.registry.MRMenus.HUMAN_INFO_MENU.get(), containerId);
        this.snapshot = snapshot;
        this.sourceItem = sourceItem;
        this.player = playerInventory.player;
        this.equipmentContainer = new SimpleContainer(EQUIPMENT_SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
                // Save equipment changes to snapshot and item - but do it safely
                try {
                    saveEquipmentToSnapshot();
                } catch (Exception e) {
                    net.alshanex.magic_realms.MagicRealms.LOGGER.error("Error auto-saving equipment: {}", e.getMessage());
                }
            }
        };

        // Load initial equipment from snapshot
        loadEquipmentFromSnapshot();

        // Add equipment slots with restrictions
        addEquipmentSlots();

        // Add player inventory slots
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    public HumanInfoMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, EntitySnapshot.deserialize(buf.readNbt()), ItemStack.EMPTY);
    }

    private void saveEquipmentToSnapshot() {
        if (snapshot == null || snapshot.equipment == null) return;

        try {
            // Get the registry access from the player's level
            var registryAccess = player.level().registryAccess();

            // Clear existing equipment
            snapshot.equipment.remove("head");
            snapshot.equipment.remove("chest");
            snapshot.equipment.remove("legs");
            snapshot.equipment.remove("boots");
            snapshot.equipment.remove("main_hand");
            snapshot.equipment.remove("off_hand");

            // Save current equipment
            ItemStack helmet = equipmentContainer.getItem(0);
            if (!helmet.isEmpty()) {
                snapshot.equipment.put("head", helmet.save(registryAccess));
            }

            ItemStack chest = equipmentContainer.getItem(1);
            if (!chest.isEmpty()) {
                snapshot.equipment.put("chest", chest.save(registryAccess));
            }

            ItemStack legs = equipmentContainer.getItem(2);
            if (!legs.isEmpty()) {
                snapshot.equipment.put("legs", legs.save(registryAccess));
            }

            ItemStack boots = equipmentContainer.getItem(3);
            if (!boots.isEmpty()) {
                snapshot.equipment.put("boots", boots.save(registryAccess));
            }

            ItemStack mainHand = equipmentContainer.getItem(4);
            if (!mainHand.isEmpty()) {
                snapshot.equipment.put("main_hand", mainHand.save(registryAccess));
            }

            ItemStack offHand = equipmentContainer.getItem(5);
            if (!offHand.isEmpty()) {
                snapshot.equipment.put("off_hand", offHand.save(registryAccess));
            }

            // Update the source item with the new snapshot data
            if (!sourceItem.isEmpty()) {
                CompoundTag updatedTag = snapshot.serialize();
                sourceItem.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                        net.minecraft.world.item.component.CustomData.of(updatedTag));
            }
        } catch (Exception e) {
            // Log the error but don't crash
            net.alshanex.magic_realms.MagicRealms.LOGGER.error("Failed to save equipment to snapshot: {}", e.getMessage());
        }
    }

    private void loadEquipmentFromSnapshot() {
        if (snapshot != null && snapshot.equipment != null) {
            try {
                // Get the registry access from the player's level
                var registryAccess = player.level().registryAccess();

                // Load helmet
                if (snapshot.equipment.contains("head")) {
                    try {
                        ItemStack helmet = ItemStack.parseOptional(
                                registryAccess,
                                snapshot.equipment.getCompound("head")
                        );
                        if (!helmet.isEmpty()) {
                            equipmentContainer.setItem(0, helmet);
                        }
                    } catch (Exception e) {
                        // Ignore loading errors for this item
                    }
                }

                // Load chestplate
                if (snapshot.equipment.contains("chest")) {
                    try {
                        ItemStack chest = ItemStack.parseOptional(
                                registryAccess,
                                snapshot.equipment.getCompound("chest")
                        );
                        if (!chest.isEmpty()) {
                            equipmentContainer.setItem(1, chest);
                        }
                    } catch (Exception e) {
                        // Ignore loading errors for this item
                    }
                }

                // Load leggings
                if (snapshot.equipment.contains("legs")) {
                    try {
                        ItemStack legs = ItemStack.parseOptional(
                                registryAccess,
                                snapshot.equipment.getCompound("legs")
                        );
                        if (!legs.isEmpty()) {
                            equipmentContainer.setItem(2, legs);
                        }
                    } catch (Exception e) {
                        // Ignore loading errors for this item
                    }
                }

                // Load boots
                if (snapshot.equipment.contains("boots")) {
                    try {
                        ItemStack boots = ItemStack.parseOptional(
                                registryAccess,
                                snapshot.equipment.getCompound("boots")
                        );
                        if (!boots.isEmpty()) {
                            equipmentContainer.setItem(3, boots);
                        }
                    } catch (Exception e) {
                        // Ignore loading errors for this item
                    }
                }

                // Load main hand
                if (snapshot.equipment.contains("main_hand")) {
                    try {
                        ItemStack mainHand = ItemStack.parseOptional(
                                registryAccess,
                                snapshot.equipment.getCompound("main_hand")
                        );
                        if (!mainHand.isEmpty()) {
                            equipmentContainer.setItem(4, mainHand);
                        }
                    } catch (Exception e) {
                        // Ignore loading errors for this item
                    }
                }

                // Load off hand
                if (snapshot.equipment.contains("off_hand")) {
                    try {
                        ItemStack offHand = ItemStack.parseOptional(
                                registryAccess,
                                snapshot.equipment.getCompound("off_hand")
                        );
                        if (!offHand.isEmpty()) {
                            equipmentContainer.setItem(5, offHand);
                        }
                    } catch (Exception e) {
                        // Ignore loading errors for this item
                    }
                }
            } catch (Exception e) {
                net.alshanex.magic_realms.MagicRealms.LOGGER.error("Failed to load equipment from snapshot: {}", e.getMessage());
            }
        }
    }

    private void addEquipmentSlots() {
        // Head slot - only helmets
        this.addSlot(new RestrictedSlot(equipmentContainer, 0, 73, 42, EquipmentSlot.HEAD));

        // Chest slot - only chestplates
        this.addSlot(new RestrictedSlot(equipmentContainer, 1, 73, 60, EquipmentSlot.CHEST));

        // Legs slot - only leggings
        this.addSlot(new RestrictedSlot(equipmentContainer, 2, 73, 78, EquipmentSlot.LEGS));

        // Boots slot - only boots
        this.addSlot(new RestrictedSlot(equipmentContainer, 3, 73, 96, EquipmentSlot.FEET));

        // Main hand slot - weapons with class restrictions
        this.addSlot(new MainHandSlot(equipmentContainer, 4, 91, 78, snapshot));

        // Off hand slot - shields only for warriors with shield
        this.addSlot(new OffHandSlot(equipmentContainer, 5, 91, 96, snapshot));
    }

    // Custom slot for armor restrictions
    private static class RestrictedSlot extends Slot {
        private final EquipmentSlot equipmentSlot;

        public RestrictedSlot(Container container, int slot, int x, int y, EquipmentSlot equipmentSlot) {
            super(container, slot, x, y);
            this.equipmentSlot = equipmentSlot;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (stack.isEmpty()) return true;

            Item item = stack.getItem();

            // Check if the item is equipable in this slot
            if (item instanceof ArmorItem armorItem) {
                return armorItem.getEquipmentSlot() == equipmentSlot;
            }

            return false;
        }

        @Override
        public int getMaxStackSize() {
            return 1; // Equipment slots should only hold one item
        }
    }

    // Custom slot for main hand with class restrictions
    private static class MainHandSlot extends Slot {
        private final EntitySnapshot snapshot;

        public MainHandSlot(Container container, int slot, int x, int y, EntitySnapshot snapshot) {
            super(container, slot, x, y);
            this.snapshot = snapshot;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (stack.isEmpty()) return true;

            Item item = stack.getItem();

            // All classes can use most weapons
            boolean isWeapon = item instanceof SwordItem ||
                    item instanceof AxeItem ||
                    item instanceof PickaxeItem ||
                    item instanceof ShovelItem ||
                    item instanceof HoeItem ||
                    item instanceof TridentItem ||
                    item instanceof BowItem ||
                    item instanceof CrossbowItem;

            // Add support for modded weapons that might extend different classes
            String itemName = item.toString().toLowerCase();
            boolean isModdedWeapon = itemName.contains("sword") ||
                    itemName.contains("axe") ||
                    itemName.contains("bow") ||
                    itemName.contains("staff") ||
                    itemName.contains("wand") ||
                    itemName.contains("blade") ||
                    itemName.contains("dagger");

            // If it's an archer (rogue + isArcher), only allow bows
            if (snapshot != null && snapshot.entityClass == EntityClass.ROGUE && snapshot.isArcher) {
                return item instanceof BowItem || itemName.contains("bow");
            }

            // For non-archers, allow any weapon
            return isWeapon || isModdedWeapon;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    // Custom slot for off hand with restrictions
    private static class OffHandSlot extends Slot {
        private final EntitySnapshot snapshot;

        public OffHandSlot(Container container, int slot, int x, int y, EntitySnapshot snapshot) {
            super(container, slot, x, y);
            this.snapshot = snapshot;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (stack.isEmpty()) return true;

            // Only warriors with shield can use offhand for shields
            if (snapshot != null &&
                    snapshot.entityClass == EntityClass.WARRIOR &&
                    snapshot.hasShield) {
                return stack.getItem() instanceof ShieldItem;
            }

            // No other class can use offhand
            return false;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private void addPlayerInventory(Inventory playerInventory) {
        // Player inventory (3x9 grid)
        // Starting at X:49 Y:165 as you specified
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 49 + col * 18, 165 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        // Player hotbar (1x9 grid below inventory)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 49 + col * 18, 223));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            result = stackInSlot.copy();

            // If clicking on equipment slot
            if (index < EQUIPMENT_SLOTS) {
                // Try to move to player inventory
                if (!this.moveItemStackTo(stackInSlot, EQUIPMENT_SLOTS, TOTAL_SLOTS, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // If clicking on player inventory
            else {
                // Try to move to appropriate equipment slot first
                boolean movedToEquipment = false;

                Item item = stackInSlot.getItem();

                // Try to place in appropriate armor slot
                if (item instanceof ArmorItem armorItem) {
                    int targetSlot = switch (armorItem.getEquipmentSlot()) {
                        case HEAD -> 0;
                        case CHEST -> 1;
                        case LEGS -> 2;
                        case FEET -> 3;
                        default -> -1;
                    };

                    if (targetSlot >= 0 && this.slots.get(targetSlot).mayPlace(stackInSlot) &&
                            !this.slots.get(targetSlot).hasItem()) {
                        this.slots.get(targetSlot).set(stackInSlot.split(1));
                        movedToEquipment = true;
                    }
                }
                // Try to place weapons in main hand
                else if (item instanceof SwordItem || item instanceof AxeItem ||
                        item instanceof BowItem || item instanceof CrossbowItem ||
                        item instanceof TridentItem || item instanceof PickaxeItem ||
                        item instanceof ShovelItem || item instanceof HoeItem) {
                    if (this.slots.get(4).mayPlace(stackInSlot) && !this.slots.get(4).hasItem()) {
                        this.slots.get(4).set(stackInSlot.split(1));
                        movedToEquipment = true;
                    }
                }
                // Try to place shields in off hand
                else if (item instanceof ShieldItem) {
                    if (this.slots.get(5).mayPlace(stackInSlot) && !this.slots.get(5).hasItem()) {
                        this.slots.get(5).set(stackInSlot.split(1));
                        movedToEquipment = true;
                    }
                }
                // Check for modded weapons by name
                else {
                    String itemName = item.toString().toLowerCase();
                    if (itemName.contains("sword") || itemName.contains("axe") ||
                            itemName.contains("bow") || itemName.contains("staff") ||
                            itemName.contains("wand") || itemName.contains("blade") ||
                            itemName.contains("dagger")) {
                        if (this.slots.get(4).mayPlace(stackInSlot) && !this.slots.get(4).hasItem()) {
                            this.slots.get(4).set(stackInSlot.split(1));
                            movedToEquipment = true;
                        }
                    }
                }

                // If couldn't move to equipment, move within player inventory
                if (!movedToEquipment) {
                    if (index < TOTAL_SLOTS - 9) {
                        // Move from inventory to hotbar
                        if (!this.moveItemStackTo(stackInSlot, TOTAL_SLOTS - 9, TOTAL_SLOTS, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else {
                        // Move from hotbar to inventory
                        if (!this.moveItemStackTo(stackInSlot, EQUIPMENT_SLOTS, TOTAL_SLOTS - 9, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return true; // Always valid since it's a UI-only screen
    }

    public EntitySnapshot getSnapshot() {
        return snapshot;
    }

    public Container getEquipmentContainer() {
        return equipmentContainer;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // Final save when menu is closed - but do it safely
        try {
            saveEquipmentToSnapshot();
        } catch (Exception e) {
            MagicRealms.LOGGER.error("Error saving equipment on menu close: {}", e.getMessage());
        }
    }
}