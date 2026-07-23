package com.tacz_caliber_ammo.platform;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
//? if forge {
import net.minecraftforge.items.ItemHandlerHelper;
//?} else {
/*import net.neoforged.neoforge.items.ItemHandlerHelper;
*///?}

public final class PlatformItems {

    public static void giveToPlayer(Player player, ItemStack stack) {
        ItemHandlerHelper.giveItemToPlayer(player, stack);
    }

    private PlatformItems() {
    }
}