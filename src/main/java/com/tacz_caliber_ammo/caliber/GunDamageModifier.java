package com.tacz_caliber_ammo.caliber;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/** 枪的伤害修正（来自枪 JSON 的 calibers/flatDamage/percentDamage）。冻结契约 by PA-1。 */
public record GunDamageModifier(Set<ResourceLocation> calibers, float flatDamage, float percentDamage) {
}
