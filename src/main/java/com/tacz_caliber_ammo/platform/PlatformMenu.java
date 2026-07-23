package com.tacz_caliber_ammo.platform;

import com.tacz_caliber_ammo.menu.AmmoPouchMenu;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
//? if forge {
import net.minecraftforge.network.NetworkHooks;
//?}

public final class PlatformMenu {

    public static void openAmmoPouch(ServerPlayer player, ItemStack pouch, int pouchSlot) {
        SimpleMenuProvider provider = new SimpleMenuProvider(
                (id, inventory, menuPlayer) -> new AmmoPouchMenu(id, inventory, pouchSlot),
                pouch.getHoverName());
        //? if forge {
        NetworkHooks.openScreen(player, provider, buffer -> buffer.writeVarInt(pouchSlot));
        //?} else {
        /*player.openMenu(provider, buffer -> buffer.writeVarInt(pouchSlot));
        *///?}
    }

    private PlatformMenu() {
    }
}