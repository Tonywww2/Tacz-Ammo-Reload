package com.tacz_caliber_ammo.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 本 mod 通用配置（Forge {@link ForgeConfigSpec}, COMMON）。由 {@link ConfigBootstrap} 注册。
 */
public final class ModConfig {

    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.BooleanValue ENABLE_FUZZY;
    private static final ForgeConfigSpec.DoubleValue BULLET_SPEED_SCALE;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("caliber_resolution");
        ENABLE_FUZZY = b
                .comment("Tier 4 fuzzy caliber matching fallback (normalized gun/ammo id token match).",
                        "When disabled, guns whose caliber cannot be resolved by tiers 1-3 fall straight to 'none'.")
                .define("enableFuzzyCaliberMatch", true);
        b.pop();
        b.push("ballistics");
        BULLET_SPEED_SCALE = b
                .comment("Multiplier converting an ammo's raw muzzle velocity (m/s, from its 'speed' field)",
                        "into TacZ's internal bullet speed unit. Final bullet speed = ammoSpeed * scale / 20 (blocks/tick).",
                        "Default 0.33 is calibrated to TacZ gun defaults (aug 5.56=295, ak47 7.62x39=250).")
                .defineInRange("bulletSpeedScale", 0.33, 0.0, 100.0);
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

    /** 初速缩放：ammo.speed(m/s) × 本值 = TacZ 速度单位（再 /20 得 blocks/tick）。未加载时默认 0.33。 */
    public static double bulletSpeedScale() {
        try {
            return BULLET_SPEED_SCALE.get();
        } catch (IllegalStateException notLoaded) {
            return 0.33;
        }
    }
}
