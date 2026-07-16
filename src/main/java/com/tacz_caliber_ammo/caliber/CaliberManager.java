package com.tacz_caliber_ammo.caliber;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.tacz.guns.api.TimelessAPI;
import com.tacz_caliber_ammo.TaczCaliberAmmo;
import com.tacz_caliber_ammo.config.ModConfig;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

/**
 * 口径/弹药/枪 数据的静态门面（PB-1 实现）。
 * 数据由 {@code CommonDataManagerMixin} 在 TacZ 数据 reload 的 apply 末尾灌入（见 rebuildAmmo/rebuildGun）。
 * 读 API（getGunCalibers/getAmmoCaliber/getAmmoProfile/getGunModifier）为冻结契约，其余 lane 只读。
 */
public final class CaliberManager {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 默认口径：未在弹药 JSON 配置 caliber 的弹药（含 TacZ 原版）归入此口径。 */
    public static final ResourceLocation NONE = TaczCaliberAmmo.prefix("none");

    /**
     * 特殊口径 万用（universal）: 配置为此口径的弹药可装入任意枪械（isAmmoOfGun 恒真）。
     * 纯弹药侧属性: 不写入任何枪的口径集、不在枪 tooltip 显示（getGunCalibers 不产出该口径、
     * TooltipHandler.gunCaliberLine 过滤之）, 仅在被配置为该口径的弹药 tooltip 上显示。
     */
    public static final ResourceLocation UNIVERSAL = TaczCaliberAmmo.prefix("universal");

    /** ammoId -> 弹道档（仅记录带 caliber 字段的弹药）。 */
    private static final Map<ResourceLocation, AmmoProfile> AMMO = new ConcurrentHashMap<>();
    /**
     * 原生 TacZ 弹药 id -> 极低弹道档：baseDamage = 同口径内容弹药（{@link #AMMO}）的最低 baseDamage，
     * 其余属性全 0；由 {@link #rebuildNativeProfiles} 在 AMMO 重建后派生（本体子弹默认走此弱档，除非另有内容档）。
     */
    private static final Map<ResourceLocation, AmmoProfile> NATIVE_PROFILE = new ConcurrentHashMap<>();
    /** gunId -> 伤害修正（仅记录带 calibers/flatDamage/percentDamage 字段的枪）。 */
    private static final Map<ResourceLocation, GunDamageModifier> GUN = new ConcurrentHashMap<>();
    /** caliberId -> 口径定义（name/tooltip，来自 data/&lt;ns&gt;/calibers/*.json，由 CaliberDataBootstrap 灌入）。 */
    private static final Map<ResourceLocation, Caliber> CALIBERS = new ConcurrentHashMap<>();

    /** Tier 1：gunId -> 口径集，来自 modify_gun_caliber 独立数据包（GunCaliberModifyBootstrap 灌入）。 */
    private static final Map<ResourceLocation, Set<ResourceLocation>> MODIFY = new ConcurrentHashMap<>();

    /**
     * gunId -> 枪伤害修正（flatDamage/percentDamage），来自 modify_gun_caliber 独立数据包的<b>对象形式</b>条目。
     * 独立注入, 不覆写 TacZ 枪 index; 由 {@link #getGunModifier} 优先于 {@link #GUN}（枪 index 字段）读取。
     */
    private static final Map<ResourceLocation, GunDamageModifier> MODIFY_DMG = new ConcurrentHashMap<>();

    /** Tier 3 内置表：原生 TacZ 弹药 id -> 口径 id（源自 docs/calibers.md 原TacZ弹药列, 24 条）。 */
    private static final Map<ResourceLocation, ResourceLocation> NATIVE_MAP = buildNativeMap();

    private static Map<ResourceLocation, ResourceLocation> buildNativeMap() {
        Map<ResourceLocation, ResourceLocation> m = new HashMap<>();
        putNative(m, "22wmr", "d22_wmr");
        putNative(m, "9mm", "9x19");
        putNative(m, "357mag", "d357");
        putNative(m, "45acp", "d45_acp");
        putNative(m, "50ae", "d50_ae");
        putNative(m, "500mag", "d500_magnum");
        putNative(m, "57x28", "5_7x28");
        putNative(m, "556x45", "5_56x45");
        putNative(m, "58x42", "5_8x42");
        putNative(m, "762x39", "7_62x39");
        putNative(m, "308", "7_62x51");
        putNative(m, "338", "d338");
        putNative(m, "50bmg", "d50_bmg");
        putNative(m, "30_06", "d30_06");
        putNative(m, "45_70", "d45_70");
        putNative(m, "792x57", "7_92x57_mauser");
        putNative(m, "12g", "12_70");
        putNative(m, "40mm", "40x46");
        putNative(m, "rpg_rocket", "72_5");
        // 补齐 TacZ 本体另 5 种弹药（对应口径本项目均已定义并生成内容弹）
        putNative(m, "46x30", "4_6x30");
        putNative(m, "545x39", "5_45x39");
        putNative(m, "68x51fury", "6_8x51");
        putNative(m, "762x25", "7_62x25");
        putNative(m, "762x54", "7_62x54r");
        // 本体新增口径(无原生 TacZ 弹药)的基准弹 -> 指向本 mod 自造弹药, 作 getAmmosForCaliber 的代码级兜底基准
        m.put(TaczCaliberAmmo.prefix("12_7x108/b_32"), TaczCaliberAmmo.prefix("12_7x108"));
        m.put(TaczCaliberAmmo.prefix("30x29/vog_30"), TaczCaliberAmmo.prefix("30x29"));
        m.put(TaczCaliberAmmo.prefix("40_vog_25/vog_25"), TaczCaliberAmmo.prefix("40_vog_25"));
        return Collections.unmodifiableMap(m);
    }

    private static void putNative(Map<ResourceLocation, ResourceLocation> m, String nativeAmmo, String caliber) {
        m.put(new ResourceLocation("tacz", nativeAmmo), TaczCaliberAmmo.prefix(caliber));
    }

    private CaliberManager() {
    }

    /**
     * 枪可用口径集合 —— 4 级优先链（高优先命中即返回，都不中则 {@link #NONE}）:
     * <ol>
     *   <li>Tier 1 {@code modify_gun_caliber} 独立数据包（{@link #MODIFY}）；</li>
     *   <li>Tier 2 枪 index/gun 的显式 {@code calibers}（{@link #GUN}）；</li>
     *   <li>Tier 3 原生子弹转口径（{@link #getAmmoCaliber} = 内容弹药 + {@link #NATIVE_MAP}）；</li>
     *   <li>Tier 4 模糊匹配（{@link #fuzzyCaliber}，可由 {@link ModConfig#enableFuzzyCaliberMatch()} 关闭）。</li>
     * </ol>
     */
    public static Set<ResourceLocation> getGunCalibers(ResourceLocation gunId) {
        // Tier 1: modify_gun_caliber 独立数据包
        Set<ResourceLocation> mod = MODIFY.get(gunId);
        if (mod != null && !mod.isEmpty()) {
            return mod;
        }
        // Tier 2: 枪 index/gun 显式 calibers
        GunDamageModifier m = GUN.get(gunId);
        if (m != null && m.calibers() != null && !m.calibers().isEmpty()) {
            return m.calibers();
        }
        ResourceLocation ammoId = TimelessAPI.getCommonGunIndex(gunId)
                .map(idx -> idx.getGunData().getAmmoId())
                .orElse(null);
        if (ammoId != null) {
            // Tier 3: 原生子弹转口径（内容弹药 caliber 或 NATIVE_MAP）；万用口径为纯弹药侧, 不作枪口径
            ResourceLocation c3 = getAmmoCaliber(ammoId);
            if (!NONE.equals(c3) && !UNIVERSAL.equals(c3)) {
                return Collections.singleton(c3);
            }
            // Tier 4: 模糊匹配（配置可关）
            if (ModConfig.enableFuzzyCaliberMatch()) {
                ResourceLocation c4 = fuzzyCaliber(ammoId, gunId);
                if (c4 != null) {
                    return Collections.singleton(c4);
                }
            }
        }
        // 兜底：特殊口径 none
        return Collections.singleton(NONE);
    }

    /**
     * 弹药所属口径。优先内容弹药的 {@code caliber} 字段（{@link #AMMO}），
     * 否则查内置原生映射（{@link #NATIVE_MAP}，让原版弹药也归入其口径），再否则 {@link #NONE}。
     */
    public static ResourceLocation getAmmoCaliber(ResourceLocation ammoId) {
        AmmoProfile p = AMMO.get(ammoId);
        if (p != null) {
            return p.caliber();
        }
        ResourceLocation nativeCaliber = NATIVE_MAP.get(ammoId);
        return (nativeCaliber != null) ? nativeCaliber : NONE;
    }

    /** 弹药弹道档；内容弹药优先（{@link #AMMO}），其次原生 TacZ 弹药的极低档（{@link #NATIVE_PROFILE}）；均无返回 null（调用方回退到由枪 bulletData 派生）。 */
    public static AmmoProfile getAmmoProfile(ResourceLocation ammoId) {
        AmmoProfile p = AMMO.get(ammoId);
        return (p != null) ? p : NATIVE_PROFILE.get(ammoId);
    }

    /**
     * 枪伤害修正；优先 {@code modify_gun_caliber} 独立数据包的对象形式条目（{@link #MODIFY_DMG}），
     * 其次枪 index 的 {@code flatDamage/percentDamage} 字段（{@link #GUN}）；均无返回 null（调用方回退 flat0/percent0）。
     */
    public static GunDamageModifier getGunModifier(ResourceLocation gunId) {
        GunDamageModifier md = MODIFY_DMG.get(gunId);
        return (md != null) ? md : GUN.get(gunId);
    }

    /** 口径定义（name + 由 id 派生的 tooltip 键）；未定义时回退为 id 自身（name=路径）。 */
    public static Caliber getCaliber(ResourceLocation caliberId) {
        Caliber c = CALIBERS.get(caliberId);
        return (c != null) ? c : new Caliber(caliberId, caliberId.getPath());
    }

    /** 某口径下的全部弹药 id（内容弹药 AMMO + 内置原生映射 NATIVE_MAP），按 id 排序供稳定轮询；无/none 返回空表。 */
    public static List<ResourceLocation> getAmmosForCaliber(ResourceLocation caliberId) {
        if (caliberId == null || NONE.equals(caliberId)) {
            return Collections.emptyList();
        }
        List<ResourceLocation> out = new ArrayList<>();
        for (Map.Entry<ResourceLocation, AmmoProfile> e : AMMO.entrySet()) {
            if (caliberId.equals(e.getValue().caliber())) {
                out.add(e.getKey());
            }
        }
        for (Map.Entry<ResourceLocation, ResourceLocation> e : NATIVE_MAP.entrySet()) {
            if (caliberId.equals(e.getValue()) && !out.contains(e.getKey())) {
                out.add(e.getKey());
            }
        }
        out.sort(Comparator.comparing(ResourceLocation::toString));
        return out;
    }

    // ==== 数据加载：由 CommonDataManagerMixin 在 apply(TAIL) 按 DataType 调用 ====

    public static void rebuildAmmo(Map<ResourceLocation, JsonElement> data) {
        AMMO.clear();
        for (Map.Entry<ResourceLocation, JsonElement> e : data.entrySet()) {
            AmmoProfile p = parseAmmo(e.getValue());
            if (p != null) {
                AMMO.put(e.getKey(), p);
            }
        }
        rebuildNativeProfiles();
        LOGGER.info("[tacz_caliber_ammo] loaded {} ammo profile(s) with caliber (of {} ammo)", AMMO.size(), data.size());
    }

    /**
     * 为原生 TacZ 弹药（{@link #NATIVE_MAP} 中的 {@code tacz:*} 条目）派生"极低"弹道档：
     * baseDamage = 同口径内容弹药（{@link #AMMO}）的最低 baseDamage，headShotMultiplier = 1.2（爆头 120%），
     * 其余属性全 0（armorIgnore/pierce/recoil/accuracy/speed/pelletCount 均 0，effects 空）。
     * 已在 AMMO 中的 id（如本 mod 自造基准弹）不覆盖；某口径无内容弹药可参照则跳过（保留 TacZ 派生伤害）。
     * 依赖 AMMO 已重建，故在 {@link #rebuildAmmo} 末尾调用。
     */
    private static void rebuildNativeProfiles() {
        NATIVE_PROFILE.clear();
        Map<ResourceLocation, Float> minByCaliber = new HashMap<>();
        for (AmmoProfile p : AMMO.values()) {
            ResourceLocation cal = p.caliber();
            Float cur = minByCaliber.get(cal);
            if (cur == null || p.baseDamage() < cur) {
                minByCaliber.put(cal, p.baseDamage());
            }
        }
        for (Map.Entry<ResourceLocation, ResourceLocation> e : NATIVE_MAP.entrySet()) {
            ResourceLocation ammoId = e.getKey();
            if (AMMO.containsKey(ammoId)) {
                continue; // 已有内容弹药档（本 mod 自造基准弹）不覆盖
            }
            Float min = minByCaliber.get(e.getValue());
            if (min == null) {
                continue; // 该口径无内容弹药可参照 -> 保留 TacZ 派生伤害
            }
            // 本体子弹：同口径最低伤害；爆头 120%（headShotMultiplier=1.2f）；其余属性全 0
            NATIVE_PROFILE.put(ammoId, new AmmoProfile(e.getValue(), min,
                    0f, 1.2f, 0, 0f, 0f, 0f, 0, AmmoEffects.EMPTY));
        }
        LOGGER.info("[tacz_caliber_ammo] built {} native-ammo low profile(s) (同口径最低伤害 + 爆头120% + 其余全 0)", NATIVE_PROFILE.size());
    }

    public static void rebuildGun(Map<ResourceLocation, JsonElement> data) {
        GUN.clear();
        for (Map.Entry<ResourceLocation, JsonElement> e : data.entrySet()) {
            GunDamageModifier m = parseGun(e.getValue());
            if (m != null) {
                GUN.put(e.getKey(), m);
            }
        }
        LOGGER.info("[tacz_caliber_ammo] loaded {} gun modifier(s) (of {} gun)", GUN.size(), data.size());
    }

    /** 口径定义加载：由 {@code CaliberDataBootstrap} 的 reload 监听在 data/&lt;ns&gt;/calibers/*.json 上调用。 */
    public static void rebuildCalibers(Map<ResourceLocation, JsonElement> data) {
        CALIBERS.clear();
        for (Map.Entry<ResourceLocation, JsonElement> e : data.entrySet()) {
            JsonElement el = e.getValue();
            if (el == null || !el.isJsonObject()) {
                continue;
            }
            JsonObject o = el.getAsJsonObject();
            String name = o.has("name") ? o.get("name").getAsString() : e.getKey().getPath();
            CALIBERS.put(e.getKey(), new Caliber(e.getKey(), name));
        }
        LOGGER.info("[tacz_caliber_ammo] loaded {} caliber definition(s)", CALIBERS.size());
    }

    /**
     * Tier 1 {@code modify_gun_caliber} 数据加载：由 {@link GunCaliberModifyBootstrap} 的 reload 监听调用。
     * 每文件 {@code { "priority": int(默认0), "guns": { gunId: 值 } }}，其中每个 gun 的值可为：
     * <ul>
     *   <li>字符串 / 数组：仅覆盖口径（注入 {@link #MODIFY}，供 getGunCalibers Tier 1）；</li>
     *   <li>对象 {@code { "calibers": [...]?, "flatDamage": float?, "percentDamage": float? }}：
     *       calibers 可选（注入 {@link #MODIFY}）；flatDamage/percentDamage 可选（注入 {@link #MODIFY_DMG}，
     *       供 {@link #getGunModifier}）——即可不改口径而单独给某枪配伤害修正。</li>
     * </ul>
     * 跨文件同一 gun 取 priority 最高者，平级则后加载者覆盖（并 warn）。
     */
    public static void rebuildModify(Map<ResourceLocation, JsonElement> data) {
        MODIFY.clear();
        MODIFY_DMG.clear();
        Map<ResourceLocation, Integer> wonPriority = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> e : data.entrySet()) {
            JsonElement el = e.getValue();
            if (el == null || !el.isJsonObject()) {
                continue;
            }
            JsonObject o = el.getAsJsonObject();
            int priority = o.has("priority") ? o.get("priority").getAsInt() : 0;
            if (!o.has("guns") || !o.get("guns").isJsonObject()) {
                continue;
            }
            for (Map.Entry<String, JsonElement> g : o.getAsJsonObject("guns").entrySet()) {
                ResourceLocation gunId = new ResourceLocation(g.getKey());
                Integer prev = wonPriority.get(gunId);
                if (prev != null && prev > priority) {
                    continue;
                }
                if (prev != null && prev == priority) {
                    LOGGER.warn("[tacz_caliber_ammo] modify_gun_caliber conflict on {} at priority {}, using later-loaded",
                            gunId, priority);
                }
                Set<ResourceLocation> calibers = new HashSet<>();
                Float flat = null;
                Float percent = null;
                JsonElement cv = g.getValue();
                if (cv.isJsonArray()) {
                    for (JsonElement c : cv.getAsJsonArray()) {
                        calibers.add(toCaliberId(c.getAsString()));
                    }
                } else if (cv.isJsonPrimitive()) {
                    calibers.add(toCaliberId(cv.getAsString()));
                } else if (cv.isJsonObject()) {
                    JsonObject go = cv.getAsJsonObject();
                    JsonElement ce = go.get("calibers");
                    if (ce != null && ce.isJsonArray()) {
                        for (JsonElement c : ce.getAsJsonArray()) {
                            calibers.add(toCaliberId(c.getAsString()));
                        }
                    } else if (ce != null && ce.isJsonPrimitive()) {
                        calibers.add(toCaliberId(ce.getAsString()));
                    }
                    if (go.has("flatDamage")) {
                        flat = go.get("flatDamage").getAsFloat();
                    }
                    if (go.has("percentDamage")) {
                        percent = go.get("percentDamage").getAsFloat();
                    }
                }
                boolean hasDmg = flat != null || percent != null;
                if (!calibers.isEmpty() || hasDmg) {
                    if (!calibers.isEmpty()) {
                        MODIFY.put(gunId, Collections.unmodifiableSet(calibers));
                    }
                    if (hasDmg) {
                        MODIFY_DMG.put(gunId, new GunDamageModifier(Collections.emptySet(),
                                flat == null ? 0f : flat, percent == null ? 0f : percent));
                    }
                    wonPriority.put(gunId, priority);
                }
            }
        }
        LOGGER.info("[tacz_caliber_ammo] loaded modify_gun_caliber for {} gun(s), {} with damage modifier",
                MODIFY.size(), MODIFY_DMG.size());
    }

    /**
     * Tier 4 模糊匹配：把 native ammoId / gunId 的 path 归一化（小写去非字母数字）后, 与已知口径 id
     * 的归一化 token 作 containment, 取最长命中的口径; 无强命中返回 null（保守, 避免误配相近口径）。
     */
    private static ResourceLocation fuzzyCaliber(ResourceLocation ammoId, ResourceLocation gunId) {
        String hay = normalizeToken(ammoId.getPath()) + "|" + normalizeToken(gunId.getPath());
        ResourceLocation best = null;
        int bestLen = 0;
        Set<ResourceLocation> known = new HashSet<>(CALIBERS.keySet());
        known.addAll(NATIVE_MAP.values());
        for (ResourceLocation caliberId : known) {
            String token = normalizeToken(caliberId.getPath());
            if (token.length() >= 3 && token.length() > bestLen && hay.contains(token)) {
                best = caliberId;
                bestLen = token.length();
            }
        }
        return best;
    }

    private static String normalizeToken(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static AmmoProfile parseAmmo(JsonElement el) {
        if (el == null || !el.isJsonObject()) {
            return null;
        }
        JsonObject o = el.getAsJsonObject();
        if (!o.has("caliber")) {
            return null; // 未配置口径 -> none（不记录，getAmmoCaliber 默认返回 NONE，profile 为 null 由枪派生）
        }
        ResourceLocation caliber = toCaliberId(o.get("caliber").getAsString());
        float baseDamage = getFloat(o, "baseDamage", 0f);
        float armorIgnore = getFloat(o, "armorIgnore", 0f);
        float headShot = getFloat(o, "headShotMultiplier", 1f);
        int pierce = o.has("pierce") ? o.get("pierce").getAsInt() : 1;
        float recoilModifier = getFloat(o, "recoil", 0f);      // 后坐力%（带符号）
        float accuracyModifier = getFloat(o, "accuracy", 0f);  // 精度%（带符号，可 >100）
        float speed = getFloat(o, "speed", 0f);                // 初速原始值 m/s（0 = 不覆写）
        int pelletCount = o.has("pelletCount") ? o.get("pelletCount").getAsInt() : 0; // 弹丸数（0 = 不覆写）
        AmmoEffects effects = parseEffects(o.has("effects") && o.get("effects").isJsonObject()
                ? o.getAsJsonObject("effects") : null);
        return new AmmoProfile(caliber, baseDamage, armorIgnore, headShot, pierce,
                recoilModifier, accuracyModifier, speed, pelletCount, effects);
    }

    /** 解析弹药 JSON 的可选 {@code effects{}} 子块 -&gt; {@link AmmoEffects}；无则 {@link AmmoEffects#EMPTY}。 */
    private static AmmoEffects parseEffects(JsonObject e) {
        if (e == null) {
            return AmmoEffects.EMPTY;
        }
        AmmoEffects.ExplosionSpec explosion = null;
        if (e.has("explosion") && e.get("explosion").isJsonObject()) {
            JsonObject ex = e.getAsJsonObject("explosion");
            boolean enabled = !ex.has("enabled") || ex.get("enabled").getAsBoolean();
            explosion = new AmmoEffects.ExplosionSpec(enabled,
                    getFloat(ex, "damage", 3.0f), getFloat(ex, "radius", 3.0f),
                    ex.has("knockback") && ex.get("knockback").getAsBoolean(),
                    ex.has("destroyBlock") && ex.get("destroyBlock").getAsBoolean(),
                    getFloat(ex, "delaySeconds", 0f));
        }
        AmmoEffects.IgniteSpec ignite = null;
        if (e.has("ignite") && e.get("ignite").isJsonObject()) {
            JsonObject ig = e.getAsJsonObject("ignite");
            ignite = new AmmoEffects.IgniteSpec(
                    ig.has("entity") && ig.get("entity").getAsBoolean(),
                    ig.has("block") && ig.get("block").getAsBoolean(),
                    ig.has("entitySeconds") ? ig.get("entitySeconds").getAsInt() : 5);
        }
        Float knockback = e.has("knockback") ? e.get("knockback").getAsFloat() : null;
        ResourceLocation script = null;
        if (e.has("script")) {
            String s = e.get("script").getAsString();
            script = ResourceLocation.tryParse(s.indexOf(':') >= 0 ? s : "tacz_caliber_ammo:" + s);
        }
        return new AmmoEffects(explosion, ignite, knockback, script);
    }

    private static GunDamageModifier parseGun(JsonElement el) {
        if (el == null || !el.isJsonObject()) {
            return null;
        }
        JsonObject o = el.getAsJsonObject();
        boolean hasCalibers = o.has("calibers");
        boolean hasFlat = o.has("flatDamage");
        boolean hasPercent = o.has("percentDamage");
        if (!hasCalibers && !hasFlat && !hasPercent) {
            return null;
        }
        Set<ResourceLocation> calibers = new HashSet<>();
        if (hasCalibers && o.get("calibers").isJsonArray()) {
            JsonArray arr = o.getAsJsonArray("calibers");
            for (JsonElement c : arr) {
                calibers.add(toCaliberId(c.getAsString()));
            }
        }
        float flat = getFloat(o, "flatDamage", 0f);
        float percent = getFloat(o, "percentDamage", 0f);
        return new GunDamageModifier(Collections.unmodifiableSet(calibers), flat, percent);
    }

    private static float getFloat(JsonObject o, String key, float def) {
        return o.has(key) ? o.get(key).getAsFloat() : def;
    }

    /** 口径字符串 -> ResourceLocation：无命名空间时默认 tacz_caliber_ammo。 */
    private static ResourceLocation toCaliberId(String s) {
        if (s.indexOf(':') >= 0) {
            return new ResourceLocation(s);
        }
        return new ResourceLocation("tacz_caliber_ammo", s);
    }
}
