package com.tacz_caliber_ammo.registry;

import com.tacz_caliber_ammo.menu.AmmoPouchMenu;
import com.tacz_caliber_ammo.platform.PlatformRegistries;

import java.util.function.Supplier;

import net.minecraft.world.inventory.MenuType;

/**
 * 平台中立菜单引用。实际 menu factory 与 DeferredRegister 位于所选 platform 源集。
 */
public final class ModMenus {

    public static final Supplier<MenuType<AmmoPouchMenu>> AMMO_POUCH = PlatformRegistries.AMMO_POUCH_MENU;

    private ModMenus() {
    }
}
