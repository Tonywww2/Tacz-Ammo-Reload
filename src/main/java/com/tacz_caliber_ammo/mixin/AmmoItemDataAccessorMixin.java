package com.tacz_caliber_ammo.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.item.AmmoItem;
import com.tacz_caliber_ammo.compat.runtime.CompatibilityRuntime;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 口径交集匹配。PB-3 / CR-2: Mixin 不支持接口注入(Injector in interface is unsupported),
 * 故改为对具体类 AmmoItem 添加 isAmmoOfGun 覆盖(AmmoItem 原继承 AmmoItemDataAccessor 默认实现;
 * 类方法优先于接口默认方法, 虚分派命中本覆盖)。CR-1: 目标 TacZ 类, 置 remap=false。
 * 数据未就绪/未命中 -> 回退 TacZ 原逻辑(TimelessAPI, 与原默认实现一致)。
 */
@Mixin(value = AmmoItem.class, remap = false)
public abstract class AmmoItemDataAccessorMixin {

    public boolean isAmmoOfGun(ItemStack gun, ItemStack ammo) {
        if (gun.getItem() instanceof IGun iGun && ammo.getItem() instanceof IAmmo iAmmo) {
            ResourceLocation ammoId = iAmmo.getAmmoId(ammo);
            return CompatibilityRuntime.isAmmoAllowed(gun, ammoId);
        }
        return false;
    }
}
