package com.tacz_caliber_ammo;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

/**
 * TacZ Caliber Ammo 的平台中立入口常量。
 * 实际 loader {@code @Mod} 入口位于所选 platform 源集。
 */
public final class TaczCaliberAmmo {

    public static final String MODID = "tacz_caliber_ammo";
    public static final Logger LOGGER = LogUtils.getLogger();

    /** 构造本 mod 命名空间下的 ResourceLocation。 */
    public static ResourceLocation prefix(String path) {
        return ResourceLocation.tryParse(MODID + ":" + path);
    }

    private TaczCaliberAmmo() {
    }
}
