package com.tacz_caliber_ammo.registry;

import com.tacz_caliber_ammo.TaczCaliberAmmo;
import com.tacz_caliber_ammo.item.AmmoPouchItem;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 本 mod 物品注册（Forge {@link DeferredRegister}）。由主类构造器 {@code ModItems.REGISTER.register(modBus)} 挂到 mod 总线。
 * 本 mod 首个 Java 注册物品：弹药包（弹药本身走 TacZ gun pack 数据，不在此注册）。
 */
public final class ModItems {

    public static final DeferredRegister<Item> REGISTER =
            DeferredRegister.create(ForgeRegistries.ITEMS, TaczCaliberAmmo.MODID);

    /** 弹药包（多弹种存储 + 压弹图案，见 {@link AmmoPouchItem}）。 */
    public static final RegistryObject<Item> AMMO_POUCH =
            REGISTER.register("ammo_pouch", () -> new AmmoPouchItem(new Item.Properties().stacksTo(1)));

    private ModItems() {
    }
}
