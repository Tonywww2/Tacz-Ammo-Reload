package com.tacz_caliber_ammo.caliber;

import net.minecraft.resources.ResourceLocation;

/**
 * 口径元数据（datapack: data/tacz_caliber_ammo/calibers/&lt;id&gt;.json）。冻结契约 by PA-1。
 * <p>说明文本不再由 JSON 字符串设置，改由口径 id 自动生成本地化键 {@link #tooltipKey()}（CR-3）。
 */
public record Caliber(ResourceLocation id, String name) {

    /** 口径说明的本地化键：由口径 id 自动生成，形如 {@code caliber.<namespace>.<path>.tooltip}。 */
    public String tooltipKey() {
        return "caliber." + id.getNamespace() + "." + id.getPath() + ".tooltip";
    }
}
