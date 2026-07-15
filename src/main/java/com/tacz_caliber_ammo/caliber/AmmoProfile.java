package com.tacz_caliber_ammo.caliber;

import net.minecraft.resources.ResourceLocation;

/**
 * 弹药弹道档（来自弹药 JSON 的 caliber/伤害/弹道字段）。
 *
 * <p>字段：伤害三件套 baseDamage/armorIgnore/headShotMultiplier/pierce；
 * 弹道三件套 recoilModifier（后坐力%，带符号）、accuracyModifier（精度%，带符号，可 &gt;100）、
 * speed（初速原始值 m/s，0 = 不覆写、保留 TacZ 默认）、pelletCount（弹丸数，0 = 不覆写）。
 */
public record AmmoProfile(ResourceLocation caliber, float baseDamage, float armorIgnore,
                          float headShotMultiplier, int pierce,
                          float recoilModifier, float accuracyModifier, float speed, int pelletCount) {
}
