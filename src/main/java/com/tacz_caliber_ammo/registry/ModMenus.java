package com.tacz_caliber_ammo.registry;

import com.tacz_caliber_ammo.TaczCaliberAmmo;
import com.tacz_caliber_ammo.menu.AmmoPouchMenu;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Menu type registration (Forge DeferredRegister). Hooked to the mod bus by the main class, next to
 * ModItems. First menu in this mod: the ammo pouch GUI. Uses IForgeMenuType so the client factory can
 * read extra data (the pouch inventory-slot index) from the open-screen buffer.
 */
public final class ModMenus {

    public static final DeferredRegister<MenuType<?>> REGISTER =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, TaczCaliberAmmo.MODID);

    public static final RegistryObject<MenuType<AmmoPouchMenu>> AMMO_POUCH =
            REGISTER.register("ammo_pouch", () -> IForgeMenuType.create(AmmoPouchMenu::new));

    private ModMenus() {
    }
}
