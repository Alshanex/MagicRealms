package net.alshanex.magic_realms.screens;

import io.redspace.ironsspellbooks.item.SpellBook;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.ContractData;
import net.alshanex.magic_realms.entity.RandomHumanEntity;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.util.EntitySnapshot;
import net.alshanex.magic_realms.util.humans.EntityClass;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class ContractHumanInfoMenu extends AbstractContainerMenu {
    private final Container equipmentContainer;
    private final EntitySnapshot snapshot;
    private final RandomHumanEntity entity;
    private final Player player;
    private java.util.Timer saveTimer;

    // Slot indices
    private static final int EQUIPMENT_SLOTS = 6; // head, chest, legs, boots, mainhand, offhand
    private static final int PLAYER_INVENTORY_SLOTS = 36;
    private static final int TOTAL_SLOTS = EQUIPMENT_SLOTS + PLAYER_INVENTORY_SLOTS;

    public ContractHumanInfoMenu(int containerId, Inventory playerInventory, EntitySnapshot snapshot, RandomHumanEntity entity) {
        super(net.alshanex.magic_realms.registry.MRMenus.CONTRACT_HUMAN_INFO_MENU.get(), containerId);
        this.snapshot = snapshot;
        this.entity = entity;
        this.player = playerInventory.player;

        // Crear el container
        this.equipmentContainer = new SimpleContainer(EQUIPMENT_SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
                // El guardado se hace manualmente
            }
        };

        // Cargar equipamiento actual de la entidad
        loadEquipmentFromEntity();

        // Add equipment slots with restrictions
        addEquipmentSlots();

        // Add player inventory slots
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    public ContractHumanInfoMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        super(net.alshanex.magic_realms.registry.MRMenus.CONTRACT_HUMAN_INFO_MENU.get(), containerId);

        // Deserializar snapshot
        this.snapshot = EntitySnapshot.deserialize(buf.readNbt());
        UUID entityUUID = buf.readUUID();
        this.player = playerInventory.player;

        MagicRealms.LOGGER.info("ContractHumanInfoMenu client constructor:");
        MagicRealms.LOGGER.info("  - Entity UUID: {}", entityUUID);
        MagicRealms.LOGGER.info("  - Player level: {}", playerInventory.player.level());
        MagicRealms.LOGGER.info("  - Is client side: {}", playerInventory.player.level().isClientSide);

        // Buscar la entidad en el mundo
        this.entity = findEntityByUUID(playerInventory.player.level(), entityUUID);

        MagicRealms.LOGGER.info("  - Found entity: {}", entity != null ? entity.getEntityName() : "null");

        // Crear el container
        this.equipmentContainer = new SimpleContainer(EQUIPMENT_SLOTS);

        if (entity != null) {
            loadEquipmentFromEntity();
        } else {
            MagicRealms.LOGGER.warn("Entity not found with UUID: {}", entityUUID);
            // En el cliente, la entidad podría no estar cargada aún
            // Esto es normal en algunos casos
        }

        // Add slots
        addEquipmentSlots();
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    private RandomHumanEntity findEntityByUUID(Level level, UUID entityUUID) {
        if (level instanceof ServerLevel serverLevel) {
            var entity = serverLevel.getEntity(entityUUID);
            if (entity instanceof RandomHumanEntity humanEntity) {
                return humanEntity;
            }
        }
        return null;
    }

    private void loadEquipmentFromEntity() {
        if (entity == null) return;

        try {
            // Cargar equipamiento directamente de la entidad
            equipmentContainer.setItem(0, entity.getItemBySlot(EquipmentSlot.HEAD).copy());
            equipmentContainer.setItem(1, entity.getItemBySlot(EquipmentSlot.CHEST).copy());
            equipmentContainer.setItem(2, entity.getItemBySlot(EquipmentSlot.LEGS).copy());
            equipmentContainer.setItem(3, entity.getItemBySlot(EquipmentSlot.FEET).copy());
            equipmentContainer.setItem(4, entity.getMainHandItem().copy());
            equipmentContainer.setItem(5, entity.getOffhandItem().copy());

            MagicRealms.LOGGER.debug("Loaded equipment from entity {} for contract menu", entity.getEntityName());
        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to load equipment from entity: {}", e.getMessage());
        }
    }

    private void saveEquipmentToEntity() {
        if (entity == null) return;

        // Verificar que el contrato sigue activo
        ContractData contractData = entity.getData(MRDataAttachments.CONTRACT_DATA);
        if (!contractData.isContractor(player.getUUID())) {
            MagicRealms.LOGGER.warn("Contract expired, cannot save equipment for entity {}", entity.getEntityName());
            return;
        }

        try {
            MagicRealms.LOGGER.debug("=== SAVING EQUIPMENT TO ENTITY {} ===", entity.getEntityName());

            // Aplicar equipamiento a la entidad
            ItemStack helmet = equipmentContainer.getItem(0);
            entity.setItemSlot(EquipmentSlot.HEAD, helmet.copy());
            MagicRealms.LOGGER.debug("Set helmet: {}", helmet.isEmpty() ? "EMPTY" : helmet.getDisplayName().getString());

            ItemStack chestplate = equipmentContainer.getItem(1);
            entity.setItemSlot(EquipmentSlot.CHEST, chestplate.copy());
            MagicRealms.LOGGER.debug("Set chestplate: {}", chestplate.isEmpty() ? "EMPTY" : chestplate.getDisplayName().getString());

            ItemStack leggings = equipmentContainer.getItem(2);
            entity.setItemSlot(EquipmentSlot.LEGS, leggings.copy());
            MagicRealms.LOGGER.debug("Set leggings: {}", leggings.isEmpty() ? "EMPTY" : leggings.getDisplayName().getString());

            ItemStack boots = equipmentContainer.getItem(3);
            entity.setItemSlot(EquipmentSlot.FEET, boots.copy());
            MagicRealms.LOGGER.debug("Set boots: {}", boots.isEmpty() ? "EMPTY" : boots.getDisplayName().getString());

            ItemStack mainHand = equipmentContainer.getItem(4);
            entity.setItemSlot(EquipmentSlot.MAINHAND, mainHand.copy());
            MagicRealms.LOGGER.debug("Set main hand: {}", mainHand.isEmpty() ? "EMPTY" : mainHand.getDisplayName().getString());

            ItemStack offHand = equipmentContainer.getItem(5);
            entity.setItemSlot(EquipmentSlot.OFFHAND, offHand.copy());
            MagicRealms.LOGGER.debug("Set off hand: {}", offHand.isEmpty() ? "EMPTY" : offHand.getDisplayName().getString());

            // Marcar como persistente el equipamiento (si es necesario en tu implementación)
            entity.setPersistenceRequired();

            MagicRealms.LOGGER.info("Successfully updated equipment for contracted entity {}", entity.getEntityName());

        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to save equipment to entity {}: {}", entity.getEntityName(), e.getMessage());
        }
    }

    private void saveEquipmentManually() {
        try {
            saveEquipmentToEntity();
            MagicRealms.LOGGER.debug("Manual equipment save completed for entity");
        } catch (Exception e) {
            MagicRealms.LOGGER.error("Error in manual equipment save: {}", e.getMessage());
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

    // Reutilizar las clases de slots del HumanInfoMenu original
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

            boolean isWeapon = item instanceof SwordItem ||
                    item instanceof AxeItem ||
                    item instanceof TridentItem;

            String itemName = item.toString().toLowerCase();
            boolean isModdedWeapon = itemName.contains("sword") ||
                    itemName.contains("axe") ||
                    itemName.contains("bow") ||
                    itemName.contains("staff") ||
                    itemName.contains("blade") ||
                    itemName.contains("dagger");

            if (snapshot != null && snapshot.entityClass == EntityClass.ROGUE && snapshot.isArcher) {
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

            if (snapshot != null &&
                    snapshot.entityClass == EntityClass.WARRIOR &&
                    snapshot.hasShield) {
                return stack.getItem() instanceof ShieldItem;
            }

            if (snapshot != null &&
                    snapshot.entityClass == EntityClass.MAGE) {
                return stack.getItem() instanceof SpellBook;
            }

            return false;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
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
                if (!this.moveItemStackTo(stackInSlot, EQUIPMENT_SLOTS, TOTAL_SLOTS, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Logic similar to original HumanInfoMenu
                boolean movedToEquipment = false;
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
                        movedToEquipment = true;
                    }
                } else if (item instanceof SwordItem || item instanceof AxeItem ||
                        item instanceof BowItem || item instanceof CrossbowItem ||
                        item instanceof TridentItem || item instanceof PickaxeItem ||
                        item instanceof ShovelItem || item instanceof HoeItem) {
                    if (this.slots.get(4).mayPlace(stackInSlot) && !this.slots.get(4).hasItem()) {
                        this.slots.get(4).set(stackInSlot.split(1));
                        movedToEquipment = true;
                    }
                } else if (item instanceof ShieldItem) {
                    if (this.slots.get(5).mayPlace(stackInSlot) && !this.slots.get(5).hasItem()) {
                        this.slots.get(5).set(stackInSlot.split(1));
                        movedToEquipment = true;
                    }
                }

                if (!movedToEquipment) {
                    if (index < TOTAL_SLOTS - 9) {
                        if (!this.moveItemStackTo(stackInSlot, TOTAL_SLOTS - 9, TOTAL_SLOTS, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else {
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

        saveEquipmentManually();
        return result;
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
                    saveEquipmentManually();
                }
            }, 100);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        // Verificación básica de distancia
        if (entity != null && !entity.isRemoved() && player.distanceToSqr(entity) > 64.0) {
            return false;
        }

        // Si la entidad es null en el cliente, permitir que continúe
        // (puede ocurrir debido a sincronización de red)
        if (entity == null && player.level().isClientSide) {
            return true;
        }

        if (entity == null) return false;

        // Verificar que el contrato sigue activo
        ContractData contractData = entity.getData(MRDataAttachments.CONTRACT_DATA);
        return contractData.isContractor(player.getUUID());
    }

    public EntitySnapshot getSnapshot() {
        return snapshot;
    }

    public Container getEquipmentContainer() {
        return equipmentContainer;
    }

    public RandomHumanEntity getEntity() {
        return entity;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);

        if (saveTimer != null) {
            saveTimer.cancel();
        }

        saveEquipmentManually();
    }
}
