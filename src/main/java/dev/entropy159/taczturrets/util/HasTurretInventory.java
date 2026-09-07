package dev.entropy159.taczturrets.util;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

public interface HasTurretInventory extends IItemHandler {
    ItemStackHandler getInventory();

    default void setStackInSlot(int slot, ItemStack stack) {
        getInventory().setStackInSlot(slot, stack);
    }

    @Override
    default int getSlots() {
        return getInventory().getSlots();
    }

    @Override
    @NotNull
    default ItemStack getStackInSlot(int slot) {
        return getInventory().getStackInSlot(slot);
    }

    @Override
    @NotNull
    default ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        return getInventory().insertItem(slot, stack, simulate);
    }

    @Override
    @NotNull
    default ItemStack extractItem(int slot, int amount, boolean simulate) {
        return getInventory().extractItem(slot, amount, simulate);
    }

    @Override
    default int getSlotLimit(int slot) {
        return getInventory().getSlotLimit(slot);
    }

    @Override
    default boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return getInventory().isItemValid(slot, stack);
    }
}
