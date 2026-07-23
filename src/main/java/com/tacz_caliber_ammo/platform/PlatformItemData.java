package com.tacz_caliber_ammo.platform;

import java.util.function.Consumer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
//? if neoforge {
/*import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
*///?}

/** Cross-version access to an ItemStack's custom compound data. */
public final class PlatformItemData {

    public static CompoundTag copy(ItemStack stack) {
        //? if forge {
        CompoundTag tag = stack.getTag();
        return tag == null ? new CompoundTag() : tag.copy();
        //?} else {
        /*return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        *///?}
    }

    public static void update(ItemStack stack, Consumer<CompoundTag> updater) {
        //? if forge {
        updater.accept(stack.getOrCreateTag());
        //?} else {
        /*CustomData.update(DataComponents.CUSTOM_DATA, stack, updater);
        *///?}
    }

    private PlatformItemData() {
    }
}