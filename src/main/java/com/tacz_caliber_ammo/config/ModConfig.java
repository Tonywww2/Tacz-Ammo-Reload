package com.tacz_caliber_ammo.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 本 mod 通用配置（Forge {@link ForgeConfigSpec}, COMMON）。由 {@link ConfigBootstrap} 注册。
 */
public final class ModConfig {

    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.BooleanValue ENABLE_FUZZY;
    private static final ForgeConfigSpec.DoubleValue BULLET_SPEED_SCALE;
    private static final ForgeConfigSpec.BooleanValue ENABLE_AMMO_EFFECTS;
    private static final ForgeConfigSpec.DoubleValue MAX_EXPLOSION_RADIUS;
    private static final ForgeConfigSpec.IntValue MAX_EFFECT_SECONDS;

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
                        "0.33 is calibrated to TacZ gun defaults (aug 5.56=295, ak47 7.62x39=250).")
                .defineInRange("bulletSpeedScale", 0.50, 0.0, 100.0);
        b.pop();
        b.push("ammo_effects");
        ENABLE_AMMO_EFFECTS = b
                .comment("Master switch for per-ammo effects (declarative explosion/ignite/knockback and Lua effect scripts).",
                        "When disabled, ammo effect fields and effect scripts are ignored (vanilla TacZ behavior).")
                .define("enableAmmoEffects", true);
        MAX_EXPLOSION_RADIUS = b
                .comment("Safety cap on the explosion radius an ammo (or effect script) may request, in blocks.")
                .defineInRange("maxExplosionRadius", 8.0, 0.0, 64.0);
        MAX_EFFECT_SECONDS = b
                .comment("Safety cap on the duration (seconds) of a mob effect an effect script may apply.")
                .defineInRange("maxEffectSeconds", 60, 0, 100000);
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

    /** 弹药效果总开关（声明式原生效果 + Lua 效果脚本）。未加载时默认 true。 */
    public static boolean enableAmmoEffects() {
        try {
            return ENABLE_AMMO_EFFECTS.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    /** 爆炸半径安全上限（格）。未加载时默认 8.0。 */
    public static float maxExplosionRadius() {
        try {
            return (float) (double) MAX_EXPLOSION_RADIUS.get();
        } catch (IllegalStateException notLoaded) {
            return 8.0f;
        }
    }

    /** 效果脚本可施加的药水效果时长上限（秒）。未加载时默认 60。 */
    public static int maxEffectSeconds() {
        try {
            return MAX_EFFECT_SECONDS.get();
        } catch (IllegalStateException notLoaded) {
            return 60;
        }
    }
}
