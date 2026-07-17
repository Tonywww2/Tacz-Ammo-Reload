package com.tacz_caliber_ammo.caliber;

import net.minecraft.resources.ResourceLocation;

/**
 * 弹药包压弹图案的一项：每个装填循环装入 {@code perCycle} 发 {@code ammoId}。
 * 图案 = 有序 {@code List<PatternEntry>}（最多 5 项）；换弹时按序循环组成逐发序列（Stage 6 T6.6）。冻结契约 by PA-1。
 */
public record PatternEntry(ResourceLocation ammoId, int perCycle) {
}
