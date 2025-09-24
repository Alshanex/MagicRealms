package net.alshanex.magic_realms.screens;

import com.mojang.datafixers.util.Pair;
import io.redspace.ironsspellbooks.item.SpellBook;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.ContractData;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.util.humans.appearance.EntitySnapshot;
import net.alshanex.magic_realms.util.humans.EntityClass;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class ContractHumanInfoMenu extends AbstractContainerMenu {
    private final Container equipmentContainer;
    private final EntitySnapshot snapshot;
    private final AbstractMercenaryEntity entity;
    private final Player player;
    private final EntityType<? extends AbstractMercenaryEntity> entityType;
    private java.util.Timer saveTimer;

    // Tab management - REMOVED INVENTORY TAB
    public enum Tab {
        IRON_SPELLS
    }

    private Tab currentTab = Tab.IRON_SPELLS;

    // Slot indices - ONLY equipment + player inventory (42 total)
    private static final int EQUIPMENT_SLOTS = 6;
    private static final int PLAYER_INVENTORY_SLOTS = 36;
    private static final int EQUIPMENT_SLOT_START = 0;
    private static final int PLAYER_INVENTORY_START = EQUIPMENT_SLOTS;

    // Constructor for when you have the real entity (server-side usually)
    public ContractHumanInfoMenu(int containerId, Inventory playerInventory, EntitySnapshot snapshot, AbstractMercenaryEntity entity) {
        this(containerId, playerInventory, snapshot, entity,
                entity != null ? (EntityType<? extends AbstractMercenaryEntity>) entity.getType() : snapshot.entityType);
    }

    // Full constructor with explicit entity type
    public ContractHumanInfoMenu(int containerId, Inventory playerInventory, EntitySnapshot snapshot,
                                 AbstractMercenaryEntity entity, EntityType<? extends AbstractMercenaryEntity> entityType) {
        super(net.alshanex.magic_realms.registry.MRMenus.CONTRACT_HUMAN_INFO_MENU.get(), containerId);
        this.snapshot = snapshot;
        this.entity = entity;
        this.player = playerInventory.player;
        this.entityType = entityType;

        // Create the equipment container
        this.equipmentContainer = new SimpleContainer(EQUIPMENT_SLOTS);

        // Load equipment from entity
        loadEquipmentFromEntity();

        // Add equipment slots with restrictions
        addEquipmentSlots();

        // Add player inventory slots
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    // Constructor for client-side (from snapshot only)
    public ContractHumanInfoMenu(int containerId, Inventory playerInventory, EntitySnapshot snapshot) {
        this(containerId, playerInventory, snapshot, null, snapshot.entityType);
    }

    // Network constructor
    public ContractHumanInfoMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        super(net.alshanex.magic_realms.registry.MRMenus.CONTRACT_HUMAN_INFO_MENU.get(), containerId);

        // Deserialize snapshot
        this.snapshot = EntitySnapshot.deserialize(buf.readNbt());
        UUID entityUUID = buf.readUUID();
        this.player = playerInventory.player;
        this.entityType = snapshot != null ? snapshot.entityType : null;

        // Find the entity in the world
        this.entity = findEntityByUUID(playerInventory.player.level(), entityUUID);

        // Create the container
        this.equipmentContainer = new SimpleContainer(EQUIPMENT_SLOTS);

        if (entity != null) {
            loadEquipmentFromEntity();
        } else if (snapshot != null && snapshot.equipment != null) {
            loadEquipmentFromSnapshot();
        }

        // Add slots
        addEquipmentSlots();
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    // Simplified tab management - no inventory tab logic
    public Tab getCurrentTab() {
        return currentTab;
    }

    public void switchToTabServerSide(Tab newTab) {
        if (this.currentTab == newTab) return;
        this.currentTab = newTab;
        MagicRealms.LOGGER.debug("Server-side switched to tab: {}", newTab);
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

    private void loadEquipmentFromSnapshot() {
        if (snapshot == null || snapshot.equipment == null) return;

        try {
            var registryAccess = player.level().registryAccess();

            if (snapshot.equipment.contains("head")) {
                equipmentContainer.setItem(0, ItemStack.parseOptional(registryAccess, snapshot.equipment.getCompound("head")));
            }
            if (snapshot.equipment.contains("chest")) {
                equipmentContainer.setItem(1, ItemStack.parseOptional(registryAccess, snapshot.equipment.getCompound("chest")));
            }
            if (snapshot.equipment.contains("legs")) {
                equipmentContainer.setItem(2, ItemStack.parseOptional(registryAccess, snapshot.equipment.getCompound("legs")));
            }
            if (snapshot.equipment.contains("boots")) {
                equipmentContainer.setItem(3, ItemStack.parseOptional(registryAccess, snapshot.equipment.getCompound("boots")));
            }
            if (snapshot.equipment.contains("main_hand")) {
                equipmentContainer.setItem(4, ItemStack.parseOptional(registryAccess, snapshot.equipment.getCompound("main_hand")));
            }
            if (snapshot.equipment.contains("off_hand")) {
                equipmentContainer.setItem(5, ItemStack.parseOptional(registryAccess, snapshot.equipment.getCompound("off_hand")));
            }
        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to load equipment from snapshot: {}", e.getMessage());
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

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            result = stackInSlot.copy();

            if (index < EQUIPMENT_SLOTS) {
                // Equipment slot - move to player inventory
                if (!this.moveItemStackTo(stackInSlot, PLAYER_INVENTORY_START,
                        PLAYER_INVENTORY_START + PLAYER_INVENTORY_SLOTS, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= PLAYER_INVENTORY_START && index < PLAYER_INVENTORY_START + PLAYER_INVENTORY_SLOTS) {
                // Player inventory slot - try to move to equipment first
                if (!tryMoveToEquipmentSlot(stackInSlot)) {
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

    // Slot classes
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

        @Override
        public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
            ResourceLocation atlas = InventoryMenu.BLOCK_ATLAS;
            ResourceLocation sprite = switch (equipmentSlot) {
                case HEAD -> InventoryMenu.EMPTY_ARMOR_SLOT_HELMET;
                case CHEST -> InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE;
                case LEGS -> InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS;
                case FEET -> InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS;
                default -> null;
            };

            return sprite != null ? Pair.of(atlas, sprite) : null;
        }
    }

    private static class MainHandSlot extends Slot {
        private final EntitySnapshot snapshot;

        private static final ResourceLocation EMPTY_SLOT_SWORD =
                ResourceLocation.withDefaultNamespace("item/empty_slot_sword");

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

        @Override
        public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
            return Pair.of(InventoryMenu.BLOCK_ATLAS, EMPTY_SLOT_SWORD);
        }
    }

    private static class OffHandSlot extends Slot {
        private final EntitySnapshot snapshot;

        private static final ResourceLocation EMPTY_SLOT_SPELLBOOK =
                ResourceLocation.fromNamespaceAndPath("curios", "slot/spellbook_slot");

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
                        return stack.getItem() instanceof SpellBook;
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

        @Override
        public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
            if(snapshot.entityClass.name().equals("MAGE")){
                return Pair.of(InventoryMenu.BLOCK_ATLAS, EMPTY_SLOT_SPELLBOOK);
            }
            return Pair.of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
        }
    }
}