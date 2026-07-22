package com.tacz_caliber_ammo.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 本 mod 通用配置（Forge {@link ForgeConfigSpec}, COMMON）。由 {@link ConfigBootstrap} 注册。
 */
public final class ModConfig {

    /** 子弹图标代号文字（{@link com.tacz_caliber_ammo.client.AmmoCodeDecorator}）的显示模式。 */
    public enum AmmoCodeDisplay {
        /** 跟随 lang：有 abbr 键且其 {@code .off} 不为真才显示（原行为）。 */
        DEFAULT,
        /** 始终显示：有 abbr 用 abbr、否则用弹药 id 末段，且忽略 {@code .off} 开关。 */
        ALWAYS,
        /** 始终不显示。 */
        NEVER
    }

    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.BooleanValue ENABLE_FUZZY;
    private static final ForgeConfigSpec.DoubleValue BULLET_SPEED_SCALE;
    private static final ForgeConfigSpec.DoubleValue BALLISTIC_COEFFICIENT_SCALE;
    private static final ForgeConfigSpec.DoubleValue RANGE_DECAY_RATE;
    private static final ForgeConfigSpec.BooleanValue ENABLE_SPEED_DECAY;
    private static final ForgeConfigSpec.DoubleValue SPEED_DECAY_SCALE;
    private static final ForgeConfigSpec.DoubleValue SPEED_DECAY_LIFE_REF;
    private static final ForgeConfigSpec.DoubleValue SPEED_DECAY_LIFE_MAX_MULT;
    private static final ForgeConfigSpec.BooleanValue ENABLE_AMMO_EFFECTS;
    private static final ForgeConfigSpec.DoubleValue MAX_EXPLOSION_RADIUS;
    private static final ForgeConfigSpec.IntValue MAX_EFFECT_SECONDS;
    private static final ForgeConfigSpec.EnumValue<AmmoCodeDisplay> AMMO_CODE_DISPLAY;
    private static final ForgeConfigSpec.BooleanValue ALWAYS_SHOW_FIRST_ROUND;

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
        BALLISTIC_COEFFICIENT_SCALE = b
                .comment("Ballistic-coefficient multiplier 'x': effective BC = ammo.ballisticCoefficient * x.",
                        "Within a gun's effective range damage is full; BEYOND it, damage decays by an inverse curve:",
                        "  damage = base / (1 + (y / (BC * x)) * excessBlocks)   where excessBlocks = hitDistance - effectiveRange.",
                        "RAISING x  -> larger effective BC -> SLOWER decay: every ammo keeps more damage at long range (more range-tanky).",
                        "LOWERING x -> faster decay: damage drops off sooner past the effective range.",
                        "Default 2000: a BC=0.3 bullet keeps ~86% damage 100 blocks past effective range, ~75% at 200 blocks.")
                .defineInRange("ballisticCoefficientScale", 2000.0, 0.0001, 1000000.0);
        RANGE_DECAY_RATE = b
                .comment("Beyond-effective-range decay-rate multiplier 'y' in the same curve:",
                        "  damage = base / (1 + (y / (BC * x)) * excessBlocks).",
                        "RAISING y  -> STEEPER decay: every ammo loses damage FASTER past the effective range.",
                        "LOWERING y -> flatter decay: more damage retained at range. y = 0 disables beyond-range decay entirely",
                        "             (full damage at any distance). Default 1.25.")
                .defineInRange("rangeDecayRate", 1.25, 0.0, 1000000.0);
        ENABLE_SPEED_DECAY = b
                .comment("Enable per-ammo in-flight SPEED decay (bullets slow down as they fly), driven by ballistic coefficient.",
                        "Implemented via TacZ bullet 'friction' (velocity *= 1-friction each tick), set so the speed-vs-distance",
                        "curve depends only on BC, not on muzzle velocity. Independent of the damage decay above.")
                .define("enableSpeedDecay", true);
        SPEED_DECAY_SCALE = b
                .comment("Speed-decay coefficient 'k': per-block speed-loss slope = k / BC, so speed% = 1 - (k/BC) * blocksFlown.",
                        "RAISING k  -> ALL ammo slows down FASTER over distance (shorter effective carry).",
                        "LOWERING k -> flatter decay, bullets keep speed longer. k = 0 disables slowdown.",
                        "Default 0.00055: BC=0.3 -> ~78% speed at 125 blocks / ~54% at 250; BC=0.6 -> ~88% / ~77%.")
                .defineInRange("speedDecayScale", 0.00055, 0.0, 1.0);
        SPEED_DECAY_LIFE_REF = b
                .comment("Bullet lifetime (life) extension while speed decay is on, so slow high-arc rounds (e.g. RPG)",
                        "don't despawn mid-air before landing. life *= clamp(refSpeed / muzzleSpeed, 1, maxMult):",
                        "the SLOWER the muzzle speed, the MORE life is extended (inversely proportional).",
                        "'speedDecayLifeRefSpeed' is the muzzle speed (blocks/tick) at/above which life is NOT extended;",
                        "below it life scales up. Default 40: rifle ~20 -> x2, RPG ~2.5 -> x16 (capped). 0 disables extension.")
                .defineInRange("speedDecayLifeRefSpeed", 40.0, 0.0, 100000.0);
        SPEED_DECAY_LIFE_MAX_MULT = b
                .comment("Upper cap on the life multiplier above (stops extremely slow rounds from living absurdly long).",
                        "Default 16.")
                .defineInRange("speedDecayLifeMaxMult", 16.0, 1.0, 1000.0);
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
        b.push("display");
        AMMO_CODE_DISPLAY = b
                .comment("Whether to render the ammo code/abbreviation text on bullet item icons (AmmoCodeDecorator).",
                        "DEFAULT: follow the lang files (show 'ammo.<ns>.<path>.abbr' unless its '.off' twin key is true);",
                        "ALWAYS: always show a code (abbr if present, else the ammo id's last path segment), ignoring '.off';",
                        "NEVER: never show ammo code text at all, regardless of lang.")
                .defineEnum("ammoCodeDisplay", AmmoCodeDisplay.DEFAULT);
        ALWAYS_SHOW_FIRST_ROUND = b
                .comment("Always show the loaded gun's first-round (chambered / next-to-fire) ammo stats in its tooltip,",
                        "without needing to hold Shift. When false (default), the stats are collapsed behind Shift",
                        "and only a hint line is shown.")
                .define("alwaysShowFirstRoundStats", false);
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

    /**
     * 弹道系数乘数 x：有效弹道系数 = ammo.ballisticCoefficient × x（优势射程外倒数衰减 rate = y/(BC×x)）。
     * 调高 x → 衰减更慢、所有弹药远距离更耐打；调低 x → 衰减更快。未加载时默认 2000。
     */
    public static double ballisticCoefficientScale() {
        try {
            return BALLISTIC_COEFFICIENT_SCALE.get();
        } catch (IllegalStateException notLoaded) {
            return 2000.0;
        }
    }

    /**
     * 优势射程外衰减速率乘数 y（倒数衰减 rate = y/(BC×x)）。调高 y → 衰减更快、远距离伤害更低；
     * y=0 关闭射程外衰减（任意距离满伤害）。未加载时默认 1.0。
     */
    public static double rangeDecayRate() {
        try {
            return RANGE_DECAY_RATE.get();
        } catch (IllegalStateException notLoaded) {
            return 1.0;
        }
    }

    /** 速度衰减总开关（子弹随飞行减速，基于弹道系数）。未加载时默认 true。 */
    public static boolean enableSpeedDecay() {
        try {
            return ENABLE_SPEED_DECAY.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    /**
     * 速度衰减系数 k：每格速度衰减斜率 = k/BC（速度% = 1 − (k/BC)·飞行格数）。
     * 调高 k → 所有弹药越飞越慢得更快（射程更短）；k=0 关闭减速。未加载时默认 0.00055。
     */
    public static double speedDecayScale() {
        try {
            return SPEED_DECAY_SCALE.get();
        } catch (IllegalStateException notLoaded) {
            return 0.00055;
        }
    }

    /** 速度衰减寿命延长参考初速：life *= clamp(本值/初速, 1, maxMult)，初速越慢 life 延长越多。未加载默认 40。 */
    public static double speedDecayLifeRefSpeed() {
        try {
            return SPEED_DECAY_LIFE_REF.get();
        } catch (IllegalStateException notLoaded) {
            return 40.0;
        }
    }

    /** 速度衰减寿命延长倍数上限。未加载默认 16。 */
    public static double speedDecayLifeMaxMult() {
        try {
            return SPEED_DECAY_LIFE_MAX_MULT.get();
        } catch (IllegalStateException notLoaded) {
            return 16.0;
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

    /** 子弹图标代号文字显示模式（{@link com.tacz_caliber_ammo.client.AmmoCodeDecorator}）。未加载时默认 {@link AmmoCodeDisplay#DEFAULT}（跟随 lang）。 */
    public static AmmoCodeDisplay ammoCodeDisplay() {
        try {
            return AMMO_CODE_DISPLAY.get();
        } catch (IllegalStateException notLoaded) {
            return AmmoCodeDisplay.DEFAULT;
        }
    }

    /** 是否始终显示枪械 tooltip 里首发子弹（膛内/下一发）的属性，无需按 Shift。未加载时默认 false。 */
    public static boolean alwaysShowFirstRoundStats() {
        try {
            return ALWAYS_SHOW_FIRST_ROUND.get();
        } catch (IllegalStateException notLoaded) {
            return false;
        }
    }
}
