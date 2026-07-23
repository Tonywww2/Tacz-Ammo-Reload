package com.tacz_caliber_ammo.util;

import com.mojang.logging.LogUtils;
import com.tacz_caliber_ammo.platform.PlatformEnvironment;

import org.slf4j.Logger;

/**
 * 开发环境专用调试日志（生产自动静默）。用于让用户在 runClient 里帮忙验证口径匹配/换弹序列/伤害应用等逻辑。
 * 门控：仅当 {@code !FMLEnvironment.production}（dev runClient/runServer）输出，前缀 {@code [tacz_caliber_ammo/DBG]}。
 */
public final class DebugLog {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 仅开发环境为 true。 */
    public static final boolean ENABLED = !PlatformEnvironment.isProduction();

    private DebugLog() {
    }

    public static void log(String fmt, Object... args) {
        if (ENABLED) {
            LOGGER.info("[tacz_caliber_ammo/DBG] " + fmt, args);
        }
    }
}
