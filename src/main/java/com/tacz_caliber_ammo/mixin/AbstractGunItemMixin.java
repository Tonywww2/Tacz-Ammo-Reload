package com.tacz_caliber_ammo.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.tacz.guns.api.item.gun.AbstractGunItem;

/**
 * 换弹写序列注入点（findAndExtractInventoryAmmo：记录提取到的弹种到 LoadedSeq）。
 * 骨架 by PA-1；PB-2 填充。
 */
@Mixin(AbstractGunItem.class)
public class AbstractGunItemMixin {
}
