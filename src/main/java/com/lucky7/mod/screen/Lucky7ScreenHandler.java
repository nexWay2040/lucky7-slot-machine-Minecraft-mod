package com.lucky7.mod.screen;

import com.lucky7.mod.blockentity.Lucky7BlockEntity;
import com.lucky7.mod.config.Lucky7Config;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;

public final class Lucky7ScreenHandler extends ScreenHandler {
    public static final int STAKE_SLOTS = Lucky7BlockEntity.STAKE_SLOTS;
    public static final int PRIZE_SLOTS = Lucky7BlockEntity.PRIZE_SLOTS;
    public static final int TOTAL_MACHINE_SLOTS = Lucky7BlockEntity.TOTAL_SLOTS;

    private final Lucky7BlockEntity machine;
    private final Inventory inventory;
    private final PropertyDelegate props;
    private final BlockPos pos;

    public Lucky7ScreenHandler(int syncId, PlayerInventory playerInventory, BlockPos pos) {
        this(syncId, playerInventory, resolve(playerInventory, pos), pos);
    }

    public Lucky7ScreenHandler(int syncId, PlayerInventory playerInventory, Lucky7BlockEntity machine, BlockPos pos) {
        super(Lucky7ScreenHandlers.MACHINE, syncId);
        this.machine = machine;
        this.pos = pos.toImmutable();
        this.inventory = machine == null ? new SimpleInventory(TOTAL_MACHINE_SLOTS) : machine;
        this.props = machine == null ? new ArrayPropertyDelegate(20) : machine.propertyDelegate();

        checkSize(inventory, TOTAL_MACHINE_SLOTS);
        inventory.onOpen(playerInventory.player);
        addProperties(props);

        // 1. 9 Stake Slots (3x3 grid on left: x=18..54, y=38..74)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int idx = row * 3 + col;
                addSlot(new Slot(inventory, idx, 18 + col * 18, 38 + row * 18) {
                    @Override
                    public boolean canInsert(ItemStack stack) {
                        return Lucky7Config.value(stack.getItem()) > 0;
                    }

                    @Override
                    public boolean canTakeItems(PlayerEntity player) {
                        return machine == null || !machine.isSpinning();
                    }

                    @Override
                    public int getMaxItemCount() {
                        return 64;
                    }
                });
            }
        }

        // 2. 27 Prize Showcase Slots (3 rows of 9 on right: x=178..322, y=38..74)
        // Row 0 = Jackpot, Row 1 = Big Win, Row 2 = Medium / Common
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int idx = STAKE_SLOTS + row * 9 + col;
                addSlot(new Slot(inventory, idx, 178 + col * 18, 38 + row * 18) {
                    @Override
                    public boolean canInsert(ItemStack stack) {
                        return false; // read-only showcase
                    }

                    @Override
                    public boolean canTakeItems(PlayerEntity player) {
                        return false; // prizes cannot be looted directly
                    }
                });
            }
        }

        // 3. Player Inventory (3 rows of 9: centered in 356px width at x=97)
        int playerX = 97;
        int playerY = 120;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, playerX + col * 18, playerY + row * 18));
            }
        }

        // 4. Player Hotbar (1 row of 9: x=97..241, y=178)
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, playerX + col * 18, playerY + 58));
        }
    }

    private static Lucky7BlockEntity resolve(PlayerInventory inventory, BlockPos pos) {
        var world = inventory.player.getWorld();
        if (world == null || pos == null) return null;
        var be = world.getBlockEntity(pos);
        if (be instanceof Lucky7BlockEntity m) return m;
        be = world.getBlockEntity(pos.down());
        if (be instanceof Lucky7BlockEntity m) return m;
        return null;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(slotIndex);
        if (!slot.hasStack()) return ItemStack.EMPTY;

        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();

        int playerStart = TOTAL_MACHINE_SLOTS;
        int playerEnd = slots.size();

        if (slotIndex < STAKE_SLOTS) {
            // From stake to player inventory
            if (!insertItem(stack, playerStart, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex >= STAKE_SLOTS && slotIndex < TOTAL_MACHINE_SLOTS) {
            // Prize showcase slots are not movable
            return ItemStack.EMPTY;
        } else {
            // From player inventory to stake slots
            if (Lucky7Config.value(stack.getItem()) > 0 && (machine == null || !machine.isSpinning())) {
                if (!insertItem(stack, 0, STAKE_SLOTS, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }

        return original;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return machine == null || machine.canPlayerUse(player);
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        inventory.onClose(player);
        if (machine != null) {
            machine.removeViewer(player);
        }
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (machine == null) return false;
        com.lucky7.mod.Lucky7Mod.LOGGER.info("ScreenHandler button {} clicked by player {}", id, player.getName().getString());
        if (id == 0) {
            return machine.startSpin(player);
        } else if (id == 1) {
            return machine.returnStake(player);
        }
        return false;
    }

    public Lucky7BlockEntity machine() { return machine; }
    public BlockPos machinePos() { return pos; }
    public int stakeValue() {
        int total = 0;
        for (int i = 0; i < STAKE_SLOTS; i++) {
            ItemStack stack = getSlot(i).getStack();
            if (!stack.isEmpty()) {
                total += Lucky7Config.value(stack.getItem()) * stack.getCount();
            }
        }
        return total > 0 ? total : props.get(0);
    }
    public boolean spinning() { return props.get(1) != 0; }
    public int spins() { return props.get(2); }
    public int wins() { return props.get(3); }
    public int jackpots() { return props.get(4); }
    public int bestWin() { return props.get(5); }
    public int lastWin() { return props.get(6); }
    public int lastTier() { return props.get(7); }
    public int jackpotPool() { return props.get(11); }
    public int totalWagered() { return props.get(13); }
    public int totalPaid() { return props.get(14); }
    public int stakeItems() { return props.get(15); }
}
