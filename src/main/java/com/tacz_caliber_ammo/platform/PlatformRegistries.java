package com.tacz_caliber_ammo.platform;

import java.util.function.Supplier;

import com.tacz_caliber_ammo.TaczCaliberAmmo;
import com.tacz_caliber_ammo.item.AmmoPouchItem;
import com.tacz_caliber_ammo.menu.AmmoPouchMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
//? if forge {
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
//?} else {
/*import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
*///?}

public final class PlatformRegistries {

    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, TaczCaliberAmmo.MODID);
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, TaczCaliberAmmo.MODID);

    public static final Supplier<Item> AMMO_POUCH =
            ITEMS.register("ammo_pouch", () -> new AmmoPouchItem(new Item.Properties().stacksTo(1)));
    //? if forge {
    public static final Supplier<MenuType<AmmoPouchMenu>> AMMO_POUCH_MENU =
            MENUS.register("ammo_pouch", () -> IForgeMenuType.create(AmmoPouchMenu::new));
    //?} else {
    /*public static final Supplier<MenuType<AmmoPouchMenu>> AMMO_POUCH_MENU =
            MENUS.register("ammo_pouch", () -> IMenuTypeExtension.create(AmmoPouchMenu::new));
    *///?}

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        MENUS.register(modBus);
    }

    private PlatformRegistries() {
    }
}