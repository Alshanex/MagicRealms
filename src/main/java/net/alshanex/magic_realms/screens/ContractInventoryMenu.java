package net.alshanex.magic_realms.screens;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.ContractData;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.util.humans.appearance.EntitySnapshot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class ContractInventoryMenu extends AbstractContainerMenu {
    private final Container equipmentContainer;
    private final EntitySnapshot snapshot;
    private final AbstractMercenaryEntity entity;
    private final Player player;
    private final EntityType<? extends AbstractMercenaryEntity> entityType;
    private java.util.Timer saveTimer;

    // Tab management - keep all tabs for UI consistency
    public enum Tab {
        IRON_SPELLS, APOTHIC, INVENTORY
    }

    private Tab currentTab = Tab.INVENTORY; // Always starts on inventory

    // All slots - equipment + player + entity inventory (84 total)
    private static final int EQUIPMENT_SLOTS = 6;
    private static final int PLAYER_INVENTORY_SLOTS = 36;
    private static final int ENTITY_INVENTORY_SLOTS = 42;
    private static final int EQUIPMENT_SLOT_START = 0;
    private static final int PLAYER_INVENTORY_START = EQUIPMENT_SLOTS;
    private static final int ENTITY_INVENTORY_START = EQUIPMENT_SLOTS + PLAYER_INVENTORY_SLOTS;

    // Constructor for when you have the real entity
    public ContractInventoryMenu(int containerId, Inventory playerInventory, EntitySnapshot snapshot, AbstractMercenaryEntity entity) {
        this(containerId, playerInventory, snapshot, entity,
                entity != null ? (EntityType<? extends AbstractMercenaryEntity>) entity.getType() : snapshot.entityType);
    }

    // Full constructor with explicit entity type
    public ContractInventoryMenu(int containerId, Inventory playerInventory, EntitySnapshot snapshot,
                                 AbstractMercenaryEntity entity, EntityType<? extends AbstractMercenaryEntity> entityType) {
        super(net.alshanex.magic_realms.registry.MRMenus.CONTRACT_INVENTORY_MENU.get(), containerId);
        this.snapshot = snapshot;
        this.entity = entity;
        this.player = playerInventory.player;
        this.entityType = entityType;

        this.equipmentContainer = new SimpleContainer(EQUIPMENT_SLOTS);

        loadEquipmentFromEntity();

        // Add all slots - always 84 total
        addEquipmentSlots();
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        addEntityInventorySlots();

        MagicRealms.LOGGER.debug("Created inventory menu with {} total slots", this.slots.size());
    }

    // Constructor for client-side (from snapshot only)
    public ContractInventoryMenu(int containerId, Inventory playerInventory, EntitySnapshot snapshot) {
        this(containerId, playerInventory, snapshot, null, snapshot.entityType);
    }

    // Network constructor
    public ContractInventoryMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        super(net.alshanex.magic_realms.registry.MRMenus.CONTRACT_INVENTORY_MENU.get(), containerId);

        this.snapshot = EntitySnapshot.deserialize(buf.readNbt());
        UUID entityUUID = buf.readUUID();
        this.player = playerInventory.player;
        this.entityType = snapshot != null ? snapshot.entityType : null;

        this.entity = findEntityByUUID(playerInventory.player.level(), entityUUID);
        this.equipmentContainer = new SimpleContainer(EQUIPMENT_SLOTS);

        loadEquipmentFromEntity();

        // Add all slots - always 84 total
        addEquipmentSlots();
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        addEntityInventorySlots();

        MagicRealms.LOGGER.debug("Network created inventory menu with {} total slots", this.slots.size());
    }

    public Tab getCurrentTab() {
        return currentTab;
    }

    public void switchToTabServerSide(Tab newTab) {
        // This method is called when switching away from inventory to attributes menu
        // The actual menu switching is handled by the packet handler
        MagicRealms.LOGGER.debug("Inventory menu received tab switch request to: {}", newTab);
    }

    private AbstractMercenaryEntity findEntityByUUID(Level level, UUID entityUUID) {
        if (level instanceof ServerLevel serverLevel) {
            var entity = serverLevel.getEntity(entityUUID);
            if (entity instanceof AbstractMercenaryEntity humanEntity) {
                return humanEntity;
            }
        }
        return null;
    }

    private void loadEquipmentFromEntity() {
        if (entity == null) return;

        try {
            equipmentContainer.setItem(0, entity.getItemBySlot(EquipmentSlot.HEAD).copy());
            equipmentContainer.setItem(1, entity.getItemBySlot(EquipmentSlot.CHEST).copy());
            equipmentContainer.setItem(2, entity.getItemBySlot(EquipmentSlot.LEGS).copy());
            equipmentContainer.setItem(3, entity.getItemBySlot(EquipmentSlot.FEET).copy());
            equipmentContainer.setItem(4, entity.getMainHandItem().copy());
            equipmentContainer.setItem(5, entity.getOffhandItem().copy());
        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to load equipment from entity: {}", e.getMessage());
        }
    }

    private void saveEquipmentToEntity() {
        if (entity == null) return;

        try {
            entity.setItemSlot(EquipmentSlot.HEAD, equipmentContainer.getItem(0).copy());
            entity.setItemSlot(EquipmentSlot.CHEST, equipmentContainer.getItem(1).copy());
            entity.setItemSlot(EquipmentSlot.LEGS, equipmentContainer.getItem(2).copy());
            entity.setItemSlot(EquipmentSlot.FEET, equipmentContainer.getItem(3).copy());
            entity.setItemSlot(EquipmentSlot.MAINHAND, equipmentContainer.getItem(4).copy());
            entity.setItemSlot(EquipmentSlot.OFFHAND, equipmentContainer.getItem(5).copy());

            entity.setPersistenceRequired();
            entity.refreshSpellsAfterEquipmentChange();
        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to save equipment to entity: {}", e.getMessage());
        }
    }

    private void addEquipmentSlots() {
        this.addSlot(new RestrictedSlot(equipmentContainer, 0, 73, 42, EquipmentSlot.HEAD));
        this.addSlot(new RestrictedSlot(equipmentContainer, 1, 73, 60, EquipmentSlot.CHEST));
        this.addSlot(new RestrictedSlot(equipmentContainer, 2, 73, 78, EquipmentSlot.LEGS));
        this.addSlot(new RestrictedSlot(equipmentContainer, 3, 73, 96, EquipmentSlot.FEET));
        this.addSlot(new MainHandSlot(equipmentContainer, 4, 91, 78, snapshot));
        this.addSlot(new OffHandSlot(equipmentContainer, 5, 91, 96, snapshot));
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 49 + col * 18, 165 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 49 + col * 18, 223));
        }
    }

    private void addEntityInventorySlots() {
        Container entityInventory = (entity != null) ? entity.getInventory() : new SimpleContainer(ENTITY_INVENTORY_SLOTS);

        int startX = 133;
        int startY = 26;
        int slotSize = 18;

        for (int row = 0; row < 7; row++) {
            for (int col = 0; col < 6; col++) {
                int slotIndex = row * 6 + col;
                if (slotIndex < ENTITY_INVENTORY_SLOTS) {
                    int x = startX + (col * slotSize);
                    int y = startY + (row * slotSize);
                    this.addSlot(new Slot(entityInventory, slotIndex, x, y));
                }
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            result = stackInSlot.copy();

            if (index < EQUIPMENT_SLOTS) {
                // Equipment slot - move to entity inventory first, then player inventory
                if (!this.moveItemStackTo(stackInSlot, ENTITY_INVENTORY_START,
                        ENTITY_INVENTORY_START + ENTITY_INVENTORY_SLOTS, false)) {
                    if (!this.moveItemStackTo(stackInSlot, PLAYER_INVENTORY_START,
                            PLAYER_INVENTORY_START + PLAYER_INVENTORY_SLOTS, true)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else if (index >= PLAYER_INVENTORY_START && index < PLAYER_INVENTORY_START + PLAYER_INVENTORY_SLOTS) {
                // Player inventory slot - try equipment first, then entity inventory
                if (!tryMoveToEquipmentSlot(stackInSlot)) {
                    if (!this.moveItemStackTo(stackInSlot, ENTITY_INVENTORY_START,
                            ENTITY_INVENTORY_START + ENTITY_INVENTORY_SLOTS, false)) {
                        // Move within player inventory
                        if (index < PLAYER_INVENTORY_START + 27) {
                            if (!this.moveItemStackTo(stackInSlot, PLAYER_INVENTORY_START + 27,
                                    PLAYER_INVENTORY_START + PLAYER_INVENTORY_SLOTS, false)) {
                                return ItemStack.EMPTY;
                            }
                        } else {
                            if (!this.moveItemStackTo(stackInSlot, PLAYER_INVENTORY_START,
                                    PLAYER_INVENTORY_START + 27, false)) {
                                return ItemStack.EMPTY;
                            }
                        }
                    }
                }
            } else if (index >= ENTITY_INVENTORY_START && index < ENTITY_INVENTORY_START + ENTITY_INVENTORY_SLOTS) {
                // Entity inventory slot - move to player inventory
                if (!this.moveItemStackTo(stackInSlot, PLAYER_INVENTORY_START,
                        PLAYER_INVENTORY_START + PLAYER_INVENTORY_SLOTS, true)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        saveEquipmentToEntity();
        return result;
    }

    private boolean tryMoveToEquipmentSlot(ItemStack stackInSlot) {
        Item item = stackInSlot.getItem();

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
                return true;
            }
        } else if (item instanceof SwordItem || item instanceof AxeItem ||
                item instanceof BowItem || item instanceof CrossbowItem ||
                item instanceof TridentItem) {
            if (this.slots.get(4).mayPlace(stackInSlot) && !this.slots.get(4).hasItem()) {
                this.slots.get(4).set(stackInSlot.split(1));
                return true;
            }
        } else if (item instanceof ShieldItem) {
            if (this.slots.get(5).mayPlace(stackInSlot) && !this.slots.get(5).hasItem()) {
                this.slots.get(5).set(stackInSlot.split(1));
                return true;
            }
        }
        return false;
    }

    @Override
    public void clicked(int slotId, int button, net.minecraft.world.inventory.ClickType clickType, Player player) {
        super.clicked(slotId, button, clickType, player);

        if (slotId >= 0 && slotId < EQUIPMENT_SLOTS) {
            if (saveTimer != null) {
                saveTimer.cancel();
            }

            saveTimer = new java.util.Timer();
            saveTimer.schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    saveEquipmentToEntity();
                }
            }, 100);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (entity != null && !entity.isRemoved() && player.distanceToSqr(entity) > 64.0) {
            return false;
        }

        if (entity == null && player.level().isClientSide) {
            return true;
        }

        if (entity == null) return false;

        if(entity.getSummoner() == null) return false;

        return entity.isInMenuState();
    }

    public EntitySnapshot getSnapshot() {
        return snapshot;
    }

    public Container getEquipmentContainer() {
        return equipmentContainer;
    }

    public AbstractMercenaryEntity getEntity() {
        return entity;
    }

    public EntityType<? extends AbstractMercenaryEntity> getEntityType() {
        return entityType;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);

        if (saveTimer != null) {
            saveTimer.cancel();
        }

        if (entity != null && !player.level().isClientSide) {
            if (entity.isInMenuState()) {
                entity.setMenuState(false);
            }
        }

        saveEquipmentToEntity();
    }

    // Slot classes (same as in ContractHumanInfoMenu)
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
            if (item instanceof ArmorItem armorItem) {
                return armorItem.getEquipmentSlot() == equipmentSlot;
            }
            return false;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

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
            boolean isWeapon = item instanceof SwordItem || item instanceof AxeItem || item instanceof TridentItem;
            String itemName = item.toString().toLowerCase();
            boolean isModdedWeapon = itemName.contains("sword") || itemName.contains("axe") ||
                    itemName.contains("bow") || itemName.contains("staff") ||
                    itemName.contains("blade") || itemName.contains("dagger");

            if (snapshot != null && snapshot.entityClass.name().equals("ROGUE") && snapshot.isArcher) {
                return item instanceof BowItem || itemName.contains("bow");
            }

            return isWeapon || isModdedWeapon;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private static class OffHandSlot extends Slot {
        private final EntitySnapshot snapshot;

        public OffHandSlot(Container container, int slot, int x, int y, EntitySnapshot snapshot) {
            super(container, slot, x, y);
            this.snapshot = snapshot;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (stack.isEmpty()) return true;

            if (snapshot != null) {
                if (snapshot.entityClass.name().equals("WARRIOR") && snapshot.hasShield) {
                    return stack.getItem() instanceof ShieldItem;
                } else if (snapshot.entityClass.name().equals("MAGE")) {
                    try {
                        return stack.getItem().getClass().getSimpleName().equals("SpellBook");
                    } catch (Exception e) {
                        return false;
                    }
                }
            }

            return false;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
