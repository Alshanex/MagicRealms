package net.alshanex.magic_realms.screens;

import net.alshanex.magic_realms.item.HumanInfoItem;
import net.alshanex.magic_realms.item.HumanTeamItem;
import net.alshanex.magic_realms.registry.MRMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class HumanTeamMenu extends AbstractContainerMenu {
    private final Container teamContainer;
    private final ItemStack sourceTeamItem;
    private final Player player;

    // Constantes para el sistema de slots
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;
    private static final int TE_INVENTORY_SLOT_COUNT = 4; // 4 slots para el equipo

    public HumanTeamMenu(int containerId, Inventory playerInventory, ItemStack teamItem) {
        super(MRMenus.HUMAN_TEAM_MENU.get(), containerId);
        this.sourceTeamItem = teamItem;
        this.player = playerInventory.player;

        // Crear el container para el equipo
        this.teamContainer = new SimpleContainer(4) {
            @Override
            public void setChanged() {
                super.setChanged();
                saveTeamToItem();
            }
        };

        loadTeamFromItem();

        addTeamSlots();

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    public HumanTeamMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, ItemStack.EMPTY);
    }

    private void addTeamSlots() {
        // Slot 1: X:80, Y:19
        this.addSlot(new HumanInfoSlot(teamContainer, 0, 80, 19));

        // Slot 2: X:60, Y:39
        this.addSlot(new HumanInfoSlot(teamContainer, 1, 60, 39));

        // Slot 3: X:100, Y:39
        this.addSlot(new HumanInfoSlot(teamContainer, 2, 100, 39));

        // Slot 4: X:80, Y:59
        this.addSlot(new HumanInfoSlot(teamContainer, 3, 80, 59));
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    private void loadTeamFromItem() {
        ItemStack[] teamMembers = HumanTeamItem.loadTeamFromItem(sourceTeamItem);
        for (int i = 0; i < 4; i++) {
            teamContainer.setItem(i, teamMembers[i]);
        }
    }

    private void saveTeamToItem() {
        if (sourceTeamItem.isEmpty()) return;

        ItemStack[] teamMembers = new ItemStack[4];
        for (int i = 0; i < 4; i++) {
            teamMembers[i] = teamContainer.getItem(i);
        }

        HumanTeamItem.saveTeamToItem(sourceTeamItem, teamMembers);
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int pIndex) {
        Slot sourceSlot = slots.get(pIndex);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        // Check if the slot clicked is one of the vanilla container slots
        if (pIndex < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            // This is a vanilla container slot so merge the stack into the team inventory
            if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX, TE_INVENTORY_FIRST_SLOT_INDEX
                    + TE_INVENTORY_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (pIndex < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
            // This is a team slot so merge the stack into the players inventory
            if (!moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            System.out.println("Invalid slotIndex:" + pIndex);
            return ItemStack.EMPTY;
        }

        // If stack size == 0 (the entire stack was moved) set slot contents to null
        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        sourceSlot.onTake(playerIn, sourceStack);
        return copyOfSourceStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        saveTeamToItem();
    }

    private static class HumanInfoSlot extends Slot {
        public HumanInfoSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof HumanInfoItem;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    public Container getTeamContainer() {
        return teamContainer;
    }
}
