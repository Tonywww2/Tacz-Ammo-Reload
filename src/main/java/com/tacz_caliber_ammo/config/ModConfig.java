package com.tacz_caliber_ammo.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 本 mod 通用配置（Forge {@link ForgeConfigSpec}, COMMON）。由 {@link ConfigBootstrap} 注册。
 */
public final class ModConfig {

    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.BooleanValue ENABLE_FUZZY;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("caliber_resolution");
        ENABLE_FUZZY = b
                .comment("Tier 4 fuzzy caliber matching fallback (normalized gun/ammo id token match).",
                        "When disabled, guns whose caliber cannot be resolved by tiers 1-3 fall straight to 'none'.")
                .define("enableFuzzyCaliberMatch", true);
        b.pop();
        SPEC = b.build();
    }

    private ModConfig() {
    }

    /** Tier 4 模糊匹配开关（配置文件可关）。配置未加载（如 datagen/早期）时默认 true。 */
    public static boolean enableFuzzyCaliberMatch() {
        try {
            return ENABLE_FUZZY.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }
}
