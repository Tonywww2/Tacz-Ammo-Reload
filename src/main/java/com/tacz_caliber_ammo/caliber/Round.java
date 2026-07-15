package com.tacz_caliber_ammo.caliber;

import net.minecraft.resources.ResourceLocation;

/** LoadedSeq 的一段 RLE：count 发同一 ammoId。冻结契约 by PA-1。 */
public record Round(ResourceLocation ammoId, int count) {
}
