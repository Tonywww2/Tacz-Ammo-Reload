package com.tacz_caliber_ammo.platform;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
//? if forge {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
//?} else {
/*import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
*///?}

/** Loader-neutral view over an entity item-handler capability. */
public final class PlatformInventory {

    public interface View {
        int slots();

        ItemStack stackInSlot(int slot);

        ItemStack extractItem(int slot, int amount, boolean simulate);
    }

    public static View find(LivingEntity entity) {
        //? if forge {
        IItemHandler handler = entity.getCapability(ForgeCapabilities.ITEM_HANDLER, null).resolve().orElse(null);
        //?} else {
        /*IItemHandler handler = entity.getCapability(Capabilities.ItemHandler.ENTITY);
        *///?}
        if (handler == null) {
            return null;
        }
        return new View() {
            @Override
            public int slots() {
                return handler.getSlots();
            }

            @Override
            public ItemStack stackInSlot(int slot) {
                return handler.getStackInSlot(slot);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return handler.extractItem(slot, amount, simulate);
            }
        };
    }

    private PlatformInventory() {
    }
}