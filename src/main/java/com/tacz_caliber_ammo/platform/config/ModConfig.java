package com.tacz_caliber_ammo.platform.config;

//? if forge {
import net.minecraftforge.common.ForgeConfigSpec;
//?} else {
/*import net.neoforged.neoforge.common.ModConfigSpec;
*///?}

public final class ModConfig {

    public enum AmmoCodeDisplay { DEFAULT, ALWAYS, NEVER }

    //? if forge {
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
    //?} else {
    /*public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue ENABLE_FUZZY;
    private static final ModConfigSpec.DoubleValue BULLET_SPEED_SCALE;
    private static final ModConfigSpec.DoubleValue BALLISTIC_COEFFICIENT_SCALE;
    private static final ModConfigSpec.DoubleValue RANGE_DECAY_RATE;
    private static final ModConfigSpec.BooleanValue ENABLE_SPEED_DECAY;
    private static final ModConfigSpec.DoubleValue SPEED_DECAY_SCALE;
    private static final ModConfigSpec.DoubleValue SPEED_DECAY_LIFE_REF;
    private static final ModConfigSpec.DoubleValue SPEED_DECAY_LIFE_MAX_MULT;
    private static final ModConfigSpec.BooleanValue ENABLE_AMMO_EFFECTS;
    private static final ModConfigSpec.DoubleValue MAX_EXPLOSION_RADIUS;
    private static final ModConfigSpec.IntValue MAX_EFFECT_SECONDS;
    private static final ModConfigSpec.EnumValue<AmmoCodeDisplay> AMMO_CODE_DISPLAY;
    private static final ModConfigSpec.BooleanValue ALWAYS_SHOW_FIRST_ROUND;
    *///?}

    static {
        //? if forge {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        //?} else {
        /*ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        *///?}
        builder.push("caliber_resolution");
        ENABLE_FUZZY = builder.comment("Enable tier 4 fuzzy caliber matching fallback.")
                .define("enableFuzzyCaliberMatch", true);
        builder.pop();
        builder.push("ballistics");
        BULLET_SPEED_SCALE = builder.comment("Ammo speed conversion multiplier.")
                .defineInRange("bulletSpeedScale", 0.50, 0.0, 100.0);
        BALLISTIC_COEFFICIENT_SCALE = builder.comment("Ballistic coefficient multiplier x.")
                .defineInRange("ballisticCoefficientScale", 2000.0, 0.0001, 1000000.0);
        RANGE_DECAY_RATE = builder.comment("Beyond-effective-range decay multiplier y; zero disables it.")
                .defineInRange("rangeDecayRate", 1.25, 0.0, 1000000.0);
        ENABLE_SPEED_DECAY = builder.comment("Enable per-ammo in-flight speed decay.")
                .define("enableSpeedDecay", true);
        SPEED_DECAY_SCALE = builder.comment("Per-block speed-loss coefficient k.")
                .defineInRange("speedDecayScale", 0.00055, 0.0, 1.0);
        SPEED_DECAY_LIFE_REF = builder.comment("Reference speed for slow-projectile lifetime extension.")
                .defineInRange("speedDecayLifeRefSpeed", 40.0, 0.0, 100000.0);
        SPEED_DECAY_LIFE_MAX_MULT = builder.comment("Maximum slow-projectile lifetime multiplier.")
                .defineInRange("speedDecayLifeMaxMult", 16.0, 1.0, 1000.0);
        builder.pop();
        builder.push("ammo_effects");
        ENABLE_AMMO_EFFECTS = builder.comment("Enable declarative and Lua per-ammo effects.")
                .define("enableAmmoEffects", true);
        MAX_EXPLOSION_RADIUS = builder.comment("Maximum requested explosion radius in blocks.")
                .defineInRange("maxExplosionRadius", 8.0, 0.0, 64.0);
        MAX_EFFECT_SECONDS = builder.comment("Maximum scripted mob-effect duration in seconds.")
                .defineInRange("maxEffectSeconds", 60, 0, 100000);
        builder.pop();
        builder.push("display");
        AMMO_CODE_DISPLAY = builder.comment("Ammo icon abbreviation display mode.")
                .defineEnum("ammoCodeDisplay", AmmoCodeDisplay.DEFAULT);
        ALWAYS_SHOW_FIRST_ROUND = builder.comment("Show first-round gun stats without holding Shift.")
                .define("alwaysShowFirstRoundStats", false);
        builder.pop();
        SPEC = builder.build();
    }

    private ModConfig() {
    }

    public static boolean enableFuzzyCaliberMatch() { return get(ENABLE_FUZZY, true); }
    public static double bulletSpeedScale() { return get(BULLET_SPEED_SCALE, 0.33); }
    public static double ballisticCoefficientScale() { return get(BALLISTIC_COEFFICIENT_SCALE, 2000.0); }
    public static double rangeDecayRate() { return get(RANGE_DECAY_RATE, 1.0); }
    public static boolean enableSpeedDecay() { return get(ENABLE_SPEED_DECAY, true); }
    public static double speedDecayScale() { return get(SPEED_DECAY_SCALE, 0.00055); }
    public static double speedDecayLifeRefSpeed() { return get(SPEED_DECAY_LIFE_REF, 40.0); }
    public static double speedDecayLifeMaxMult() { return get(SPEED_DECAY_LIFE_MAX_MULT, 16.0); }
    public static boolean enableAmmoEffects() { return get(ENABLE_AMMO_EFFECTS, true); }
    public static float maxExplosionRadius() { return get(MAX_EXPLOSION_RADIUS, 8.0).floatValue(); }
    public static int maxEffectSeconds() { return get(MAX_EFFECT_SECONDS, 60); }
    public static AmmoCodeDisplay ammoCodeDisplay() { return get(AMMO_CODE_DISPLAY, AmmoCodeDisplay.DEFAULT); }
    public static boolean alwaysShowFirstRoundStats() { return get(ALWAYS_SHOW_FIRST_ROUND, false); }

    //? if forge {
    private static <T> T get(ForgeConfigSpec.ConfigValue<T> value, T fallback) {
        try { return value.get(); } catch (IllegalStateException notLoaded) { return fallback; }
    }
    //?} else {
    /*private static <T> T get(ModConfigSpec.ConfigValue<T> value, T fallback) {
        try { return value.get(); } catch (IllegalStateException notLoaded) { return fallback; }
    }
    *///?}
}