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
    private java.util.Timer saveTimer;

    // Slot indices
    private static final int EQUIPMENT_SLOTS = 6; // head, chest, legs, boots, mainhand, offhand
    private static final int PLAYER_INVENTORY_SLOTS = 36;
    private static final int TOTAL_SLOTS = EQUIPMENT_SLOTS + PLAYER_INVENTORY_SLOTS;

    public HumanInfoMenu(int containerId, Inventory playerInventory, EntitySnapshot snapshot, ItemStack sourceItem) {
        super(net.alshanex.magic_realms.registry.MRMenus.HUMAN_INFO_MENU.get(), containerId);
        this.snapshot = snapshot;
        this.sourceItem = sourceItem;
        this.player = playerInventory.player;

        // Crear el container SIN el auto-save en setChanged()
        this.equipmentContainer = new SimpleContainer(EQUIPMENT_SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
                // NO llamar saveEquipmentToSnapshot() aquí - se hace manualmente cuando es necesario
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

    private void saveEquipmentManually() {
        try {
            saveEquipmentToSnapshot();
            MagicRealms.LOGGER.debug("Manual equipment save completed");
        } catch (Exception e) {
            MagicRealms.LOGGER.error("Error in manual equipment save: {}", e.getMessage());
        }
    }

    private void saveEquipmentToSnapshot() {
        if (snapshot == null || snapshot.equipment == null) return;

        try {
            // Get the registry access from the player's level
            var registryAccess = player.level().registryAccess();

            MagicRealms.LOGGER.debug("=== SAVING EQUIPMENT START ===");
            MagicRealms.LOGGER.debug("Current equipment keys before save: {}", snapshot.equipment.getAllKeys());
            MagicRealms.LOGGER.debug("Equipment container size: {}", equipmentContainer.getContainerSize());

            // Log current container contents
            for (int i = 0; i < equipmentContainer.getContainerSize(); i++) {
                ItemStack item = equipmentContainer.getItem(i);
                MagicRealms.LOGGER.debug("Container slot {}: {}", i,
                        item.isEmpty() ? "EMPTY" : item.getDisplayName().getString());
            }

            // Clear existing equipment
            snapshot.equipment.remove("head");
            snapshot.equipment.remove("chest");
            snapshot.equipment.remove("legs");
            snapshot.equipment.remove("boots");
            snapshot.equipment.remove("main_hand");
            snapshot.equipment.remove("off_hand");

            // Save current equipment
            ItemStack helmet = equipmentContainer.getItem(0);
            MagicRealms.LOGGER.debug("Processing helmet slot 0: {}",
                    helmet.isEmpty() ? "EMPTY" : helmet.getDisplayName().getString());
            if (!helmet.isEmpty()) {
                snapshot.equipment.put("head", helmet.save(registryAccess));
                MagicRealms.LOGGER.debug("Saved helmet to 'head' key");
            }

            ItemStack chest = equipmentContainer.getItem(1);
            MagicRealms.LOGGER.debug("Processing chestplate slot 1: {}",
                    chest.isEmpty() ? "EMPTY" : chest.getDisplayName().getString());
            if (!chest.isEmpty()) {
                snapshot.equipment.put("chest", chest.save(registryAccess));
                MagicRealms.LOGGER.debug("Saved chestplate to 'chest' key");
            }

            ItemStack legs = equipmentContainer.getItem(2);
            MagicRealms.LOGGER.debug("Processing leggings slot 2: {}",
                    legs.isEmpty() ? "EMPTY" : legs.getDisplayName().getString());
            if (!legs.isEmpty()) {
                snapshot.equipment.put("legs", legs.save(registryAccess));
                MagicRealms.LOGGER.debug("Saved leggings to 'legs' key");
            }

            ItemStack boots = equipmentContainer.getItem(3);
            MagicRealms.LOGGER.debug("Processing boots slot 3: {}",
                    boots.isEmpty() ? "EMPTY" : boots.getDisplayName().getString());
            if (!boots.isEmpty()) {
                snapshot.equipment.put("boots", boots.save(registryAccess));
                MagicRealms.LOGGER.debug("Saved boots to 'boots' key");
            }

            ItemStack mainHand = equipmentContainer.getItem(4);
            MagicRealms.LOGGER.debug("Processing main hand slot 4: {}",
                    mainHand.isEmpty() ? "EMPTY" : mainHand.getDisplayName().getString());
            if (!mainHand.isEmpty()) {
                snapshot.equipment.put("main_hand", mainHand.save(registryAccess));
                MagicRealms.LOGGER.debug("Saved main hand to 'main_hand' key");
            }

            ItemStack offHand = equipmentContainer.getItem(5);
            MagicRealms.LOGGER.debug("Processing off hand slot 5: {}",
                    offHand.isEmpty() ? "EMPTY" : offHand.getDisplayName().getString());
            if (!offHand.isEmpty()) {
                snapshot.equipment.put("off_hand", offHand.save(registryAccess));
                MagicRealms.LOGGER.debug("Saved off hand to 'off_hand' key");
            }

            MagicRealms.LOGGER.debug("Equipment keys after save: {}", snapshot.equipment.getAllKeys());
            MagicRealms.LOGGER.debug("Final equipment NBT: {}", snapshot.equipment.toString());

            // Update the source item with the new snapshot data
            if (!sourceItem.isEmpty()) {
                CompoundTag updatedTag = snapshot.serialize();
                sourceItem.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                        net.minecraft.world.item.component.CustomData.of(updatedTag));
                MagicRealms.LOGGER.debug("Updated source item with new equipment data");
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

                MagicRealms.LOGGER.debug("Loading equipment from snapshot. Available keys: {}",
                        snapshot.equipment.getAllKeys().toString());

                // Debug: Log the entire equipment tag structure
                MagicRealms.LOGGER.debug("Full equipment NBT: {}", snapshot.equipment.toString());

                // Load helmet (slot 0)
                MagicRealms.LOGGER.debug("Checking for 'head' key: {}", snapshot.equipment.contains("head"));
                if (snapshot.equipment.contains("head")) {
                    try {
                        CompoundTag helmetTag = snapshot.equipment.getCompound("head");
                        MagicRealms.LOGGER.debug("Loading helmet from tag: {}", helmetTag.toString());

                        ItemStack helmet = ItemStack.parseOptional(registryAccess, helmetTag);
                        if (!helmet.isEmpty()) {
                            equipmentContainer.setItem(0, helmet);
                            MagicRealms.LOGGER.debug("Loaded helmet: {}", helmet.getDisplayName().getString());
                        } else {
                            MagicRealms.LOGGER.warn("Helmet tag resulted in empty ItemStack");
                        }
                    } catch (Exception e) {
                        MagicRealms.LOGGER.error("Failed to load helmet: {}", e.getMessage(), e);
                    }
                } else {
                    MagicRealms.LOGGER.debug("No helmet data found in snapshot");
                }

                // Load chestplate (slot 1)
                MagicRealms.LOGGER.debug("Checking for 'chest' key: {}", snapshot.equipment.contains("chest"));
                if (snapshot.equipment.contains("chest")) {
                    try {
                        CompoundTag chestTag = snapshot.equipment.getCompound("chest");
                        MagicRealms.LOGGER.debug("Loading chestplate from tag: {}", chestTag.toString());

                        ItemStack chest = ItemStack.parseOptional(registryAccess, chestTag);
                        if (!chest.isEmpty()) {
                            equipmentContainer.setItem(1, chest);
                            MagicRealms.LOGGER.debug("Loaded chestplate: {}", chest.getDisplayName().getString());
                        } else {
                            MagicRealms.LOGGER.warn("Chestplate tag resulted in empty ItemStack");
                        }
                    } catch (Exception e) {
                        MagicRealms.LOGGER.error("Failed to load chestplate: {}", e.getMessage(), e);
                    }
                } else {
                    MagicRealms.LOGGER.debug("No chestplate data found in snapshot - trying direct access");
                    // Try direct access to debug
                    try {
                        CompoundTag directChest = snapshot.equipment.getCompound("chest");
                        MagicRealms.LOGGER.debug("Direct chest access result: {}", directChest.toString());
                    } catch (Exception e) {
                        MagicRealms.LOGGER.debug("Direct chest access failed: {}", e.getMessage());
                    }
                }

                // Load leggings (slot 2)
                MagicRealms.LOGGER.debug("Checking for 'legs' key: {}", snapshot.equipment.contains("legs"));
                if (snapshot.equipment.contains("legs")) {
                    try {
                        CompoundTag legsTag = snapshot.equipment.getCompound("legs");
                        MagicRealms.LOGGER.debug("Loading leggings from tag: {}", legsTag.toString());

                        ItemStack legs = ItemStack.parseOptional(registryAccess, legsTag);
                        if (!legs.isEmpty()) {
                            equipmentContainer.setItem(2, legs);
                            MagicRealms.LOGGER.debug("Loaded leggings: {}", legs.getDisplayName().getString());
                        } else {
                            MagicRealms.LOGGER.warn("Leggings tag resulted in empty ItemStack");
                        }
                    } catch (Exception e) {
                        MagicRealms.LOGGER.error("Failed to load leggings: {}", e.getMessage(), e);
                    }
                } else {
                    MagicRealms.LOGGER.debug("No leggings data found in snapshot - trying direct access");
                    try {
                        CompoundTag directLegs = snapshot.equipment.getCompound("legs");
                        MagicRealms.LOGGER.debug("Direct legs access result: {}", directLegs.toString());
                    } catch (Exception e) {
                        MagicRealms.LOGGER.debug("Direct legs access failed: {}", e.getMessage());
                    }
                }

                // Load boots (slot 3)
                MagicRealms.LOGGER.debug("Checking for 'boots' key: {}", snapshot.equipment.contains("boots"));
                if (snapshot.equipment.contains("boots")) {
                    try {
                        CompoundTag bootsTag = snapshot.equipment.getCompound("boots");
                        MagicRealms.LOGGER.debug("Loading boots from tag: {}", bootsTag.toString());

                        ItemStack boots = ItemStack.parseOptional(registryAccess, bootsTag);
                        if (!boots.isEmpty()) {
                            equipmentContainer.setItem(3, boots);
                            MagicRealms.LOGGER.debug("Loaded boots: {}", boots.getDisplayName().getString());
                        } else {
                            MagicRealms.LOGGER.warn("Boots tag resulted in empty ItemStack");
                        }
                    } catch (Exception e) {
                        MagicRealms.LOGGER.error("Failed to load boots: {}", e.getMessage(), e);
                    }
                } else {
                    MagicRealms.LOGGER.debug("No boots data found in snapshot - trying direct access");
                    try {
                        CompoundTag directBoots = snapshot.equipment.getCompound("boots");
                        MagicRealms.LOGGER.debug("Direct boots access result: {}", directBoots.toString());
                    } catch (Exception e) {
                        MagicRealms.LOGGER.debug("Direct boots access failed: {}", e.getMessage());
                    }
                }

                // Load main hand (slot 4)
                MagicRealms.LOGGER.debug("Checking for 'main_hand' key: {}", snapshot.equipment.contains("main_hand"));
                if (snapshot.equipment.contains("main_hand")) {
                    try {
                        CompoundTag mainHandTag = snapshot.equipment.getCompound("main_hand");
                        MagicRealms.LOGGER.debug("Loading main hand from tag: {}", mainHandTag.toString());

                        ItemStack mainHand = ItemStack.parseOptional(registryAccess, mainHandTag);
                        if (!mainHand.isEmpty()) {
                            equipmentContainer.setItem(4, mainHand);
                            MagicRealms.LOGGER.debug("Loaded main hand: {}", mainHand.getDisplayName().getString());
                        } else {
                            MagicRealms.LOGGER.warn("Main hand tag resulted in empty ItemStack");
                        }
                    } catch (Exception e) {
                        MagicRealms.LOGGER.error("Failed to load main hand: {}", e.getMessage(), e);
                    }
                } else {
                    MagicRealms.LOGGER.debug("No main hand data found in snapshot - trying direct access");
                    try {
                        CompoundTag directMainHand = snapshot.equipment.getCompound("main_hand");
                        MagicRealms.LOGGER.debug("Direct main_hand access result: {}", directMainHand.toString());
                    } catch (Exception e) {
                        MagicRealms.LOGGER.debug("Direct main_hand access failed: {}", e.getMessage());
                    }
                }

                // Load off hand (slot 5)
                MagicRealms.LOGGER.debug("Checking for 'off_hand' key: {}", snapshot.equipment.contains("off_hand"));
                if (snapshot.equipment.contains("off_hand")) {
                    try {
                        CompoundTag offHandTag = snapshot.equipment.getCompound("off_hand");
                        MagicRealms.LOGGER.debug("Loading off hand from tag: {}", offHandTag.toString());

                        ItemStack offHand = ItemStack.parseOptional(registryAccess, offHandTag);
                        if (!offHand.isEmpty()) {
                            equipmentContainer.setItem(5, offHand);
                            MagicRealms.LOGGER.debug("Loaded off hand: {}", offHand.getDisplayName().getString());
                        } else {
                            MagicRealms.LOGGER.warn("Off hand tag resulted in empty ItemStack");
                        }
                    } catch (Exception e) {
                        MagicRealms.LOGGER.error("Failed to load off hand: {}", e.getMessage(), e);
                    }
                } else {
                    MagicRealms.LOGGER.debug("No off hand data found in snapshot");
                }

            } catch (Exception e) {
                MagicRealms.LOGGER.error("Failed to load equipment from snapshot: {}", e.getMessage(), e);
            }
        } else {
            MagicRealms.LOGGER.warn("Snapshot or equipment data is null");
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

        // Guardar equipamiento después de mover items
        saveEquipmentManually();

        return result;
    }

    @Override
    public void clicked(int slotId, int button, net.minecraft.world.inventory.ClickType clickType, Player player) {
        super.clicked(slotId, button, clickType, player);

        // Si se hizo click en un slot de equipamiento (slots 0-5), guardar
        if (slotId >= 0 && slotId < EQUIPMENT_SLOTS) {
            // Usar un timer para evitar múltiples saves seguidos
            if (saveTimer != null) {
                saveTimer.cancel();
            }

            saveTimer = new java.util.Timer();
            saveTimer.schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    saveEquipmentManually();
                }
            }, 100); // Esperar 100ms antes de guardar
        }
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

        // Cancelar timer si existe
        if (saveTimer != null) {
            saveTimer.cancel();
        }

        // Guardar final cuando se cierra el menú
        saveEquipmentManually();
    }
}