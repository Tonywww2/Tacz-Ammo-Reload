package com.tacz_caliber_ammo.registry;

import com.tacz_caliber_ammo.item.AmmoPouchItem;
import com.tacz_caliber_ammo.platform.PlatformRegistries;

import java.util.function.Supplier;

import net.minecraft.world.item.Item;

/**
 * 平台中立物品引用。实际 DeferredRegister 位于所选 platform 源集。
 */
public final class ModItems {

    /** 弹药包（多弹种存储 + 压弹图案，见 {@link AmmoPouchItem}）。 */
    public static final Supplier<Item> AMMO_POUCH = PlatformRegistries.AMMO_POUCH;

    private ModItems() {
    }
}
