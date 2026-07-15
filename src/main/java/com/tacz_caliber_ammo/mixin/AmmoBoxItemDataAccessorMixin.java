package com.tacz_caliber_ammo.mixin;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.nbt.AmmoBoxItemDataAccessor;
import com.tacz.guns.item.AmmoBoxItem;
import com.tacz_caliber_ammo.caliber.CaliberManager;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 弹药盒口径匹配。PB-3 / CR-2: 改注入具体类 AmmoBoxItem, 添加 isAmmoBoxOfGun 覆盖
 * (覆盖其继承自 AmmoBoxItemDataAccessor 的默认实现)。CR-1: 目标 TacZ 类, 置 remap=false。
 * 保留 TacZ 原前置判定: 全类型创造盒 -> true; 空弹药盒 -> false; 之后才做口径匹配。
 * 数据未就绪/未命中 -> 回退 TacZ 原逻辑(单一 ammoId 相等)。
 */
@Mixin(value = AmmoBoxItem.class, remap = false)
public abstract class AmmoBoxItemDataAccessorMixin {

    public boolean isAmmoBoxOfGun(ItemStack gun, ItemStack box) {
        if (gun.getItem() instanceof IGun iGun && box.getItem() instanceof IAmmoBox iBox) {
            if (((AmmoBoxItemDataAccessor) (Object) this).isAllTypeCreative(box)) {
                return true;
            }
            ResourceLocation ammoId = iBox.getAmmoId(box);
            if (ammoId.equals(DefaultAssets.EMPTY_AMMO_ID)) {
                return false;
            }
            ResourceLocation gunId = iGun.getGunId(gun);
            Set<ResourceLocation> gunCalibers = CaliberManager.getGunCalibers(gunId);
            ResourceLocation ammoCaliber = CaliberManager.getAmmoCaliber(ammoId);
            if (!gunCalibers.isEmpty() && ammoCaliber != null) {
                return gunCalibers.contains(ammoCaliber);
            }
            return TimelessAPI.getCommonGunIndex(gunId)
                    .map(index -> index.getGunData().getAmmoId().equals(ammoId)).orElse(false);
        }
        return false;
    }
}
