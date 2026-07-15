package com.tacz_caliber_ammo.caliber;

import net.minecraft.resources.ResourceLocation;

/** 弹药弹道档（来自弹药 JSON 的 caliber/伤害字段）。冻结契约 by PA-1。 */
public record AmmoProfile(ResourceLocation caliber, float baseDamage, float armorIgnore,
                          float headShotMultiplier, int pierce) {
}
