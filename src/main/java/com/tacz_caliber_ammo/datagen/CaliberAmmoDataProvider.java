package com.tacz_caliber_ammo.datagen;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import javax.imageio.ImageIO;

import com.google.common.hash.Hashing;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.tacz_caliber_ammo.platform.GenerationDialect;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import org.slf4j.Logger;

/**
 * datagen：读 {@code docs/tarkov_ammo_stats.csv}，按 {@code docs/eft-to-tacz-mapping.md} 的算法批量生成
 * 口径定义(calibers/*.json) 与 弹药索引(index/ammo/&lt;caliber&gt;/&lt;model&gt;.json)。
 * <p>贴图：有 TacZ 原型的口径复用 {@code tacz:<id>_display}；无原型的口径生成纯色占位
 * (display + uv/slot 单色 PNG，复用通用 TacZ 子弹几何体)。
 */
public class CaliberAmmoDataProvider implements DataProvider {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String NS = "tacz_caliber_ammo";

    // ==== 转换常量 (eft-to-tacz-mapping.md 第 3 节) ====
    private static final float K_DMG = 0.14f;
    private static final float K_DMG_SHOT = 0.09f;
    private static final double DMG_MIN = 3, DMG_MAX = 40;
    private static final int PEN0 = 8, PEN_SPAN = 64;
    private static final double AI_CAP = 0.90;
    private static final int P2 = 50, P3 = 68;
    private static final double HS_BASE = 1.30, HS_K = 0.60, HS_MIN = 1.30, HS_MAX = 2.00;

    /** 型号 id 规范化时要丢弃的口径限定词(仅出现在前导位置)。 */
    private static final Set<String> STOP = Set.of(
            "TT", "NATO", "PARA", "ACP", "HK", "FN", "GYURZA", "BLACKOUT", "MARLIN", "VOG");

    /** CSV 的中文口径名 -> 本项目口径 id (calibers.md)。未列入者跳过(榴弹/杂项)。 */
    private static final Map<String, String> CAL_ID = buildCalId();

    /** 口径 id -> TacZ 原型 display 基名 (复用 tacz:&lt;base&gt;_display)；不在表中 = 无原型 -> 纯色占位。 */
    private static final Map<String, String> PROTO = buildProto();

    /** 口径 id -> 自定义飞行弹(entity) geo 模型基名，放在 assets/&lt;ns&gt;/geo_models/ammo_entity/&lt;base&gt;.json。
     *  该口径带 slot 贴图的弹种会附加 display 的 entity 块：若存在
     *  textures/ammo_entity/&lt;id&gt;/&lt;model&gt;.png 则用本命名空间自定义 model+uv，
     *  否则回退 tacz 默认 entity(model 与 texture 均为 tacz:ammo_entity/&lt;base&gt;)。 */
    private static final Map<String, String> ENTITY_MODEL = Map.of("40x46", "40mm_grenade");

    /** protoBase -> 真实 geo 模型名（当 geo 文件名与 base 不同）；未列出者用 base 本身。
     *  例：40mm 的手持/几何体是 tacz:ammo/40mm_grenade（tacz 无 geo_models/ammo/40mm.json）。 */
    private static final Map<String, String> AMMO_GEO = Map.of("40mm", "40mm_grenade");

    /** 无 3D 手持几何体的 protoBase：tacz 本体这些弹的 display 只有 slot/shell、没有 model。
     *  对它们不要写 model/texture（否则指向不存在的 geo 会抛错、连带 entity 也不加载）；手持退化为 slot 平面图标，与原版一致。 */
    private static final java.util.List<String> NO_HELD_MODEL = java.util.List.of("22wmr", "45_70", "500mag", "792x57");

    private static Map<String, String> buildCalId() {
        Map<String, String> m = new HashMap<>();
        m.put("12/70", "12_70");
        m.put("9x18mm 马卡洛夫", "9x18");
        m.put("5.45x39mm", "5_45x39");
        m.put("5.56x45mm NATO", "5_56x45");
        m.put("20/70", "20_70");
        m.put("7.62x25mm 托卡列夫", "7_62x25");
        m.put("9x19mm 帕拉贝伦", "9x19");
        m.put("7.62x39mm", "7_62x39");
        m.put("7.62x54R", "7_62x54r");
        m.put("7.62x51mm NATO", "7_62x51");
        m.put("5.7x28mm FN", "5_7x28");
        m.put("9x39mm", "9x39");
        m.put("9x21mm Gyurza", "9x21");
        m.put("40x46mm", "40x46");
        m.put(".300 Blackout", "d300_blk");
        m.put(".45 ACP", "d45_acp");
        m.put("4.6x30mm HK", "4_6x30");
        m.put(".50 BMG", "d50_bmg");
        m.put("23x75R", "23x75_r");
        m.put(".357 马格南", "d357");
        m.put(".338 拉普阿马格南", "d338");
        m.put(".366 TKM", "d366_tkm");
        m.put(".50 AE", "d50_ae");
        m.put("12.7x55mm", "12_7x55");
        m.put("9.3x64mm", "9_3x64");
        m.put(".308 Marlin Express", "d308_marlin");
        m.put("6.8x51mm", "6_8x51");
        // 7 个有原生枪但 CSV 未覆盖的口径（启发数值, 见 CSV 补充行 + 复用 TacZ 原生 display）
        m.put(".22 WMR", "d22_wmr");
        m.put(".500 S&W Magnum", "d500_magnum");
        m.put("5.8x42mm", "5_8x42");
        m.put(".30-06 Springfield", "d30_06");
        m.put("7.92x57mm Mauser", "7_92x57_mauser");
        m.put(".45-70 Government", "d45_70");
        m.put("72.5mm Rocket", "72_5");
        // 3 个无原生枪的空缺口径（启发数值；无 TacZ 原型 -> 纯色占位贴图）
        m.put("12.7x108mm", "12_7x108");
        m.put("30x29mm", "30x29");
        m.put("40mm VOG-25", "40_vog_25");
        m.put("26x75mm", "26_75");
        return m;
    }

    private static Map<String, String> buildProto() {
        Map<String, String> m = new HashMap<>();
        m.put("5_56x45", "556x45");
        m.put("7_62x39", "762x39");
        m.put("9x19", "9mm");
        m.put("d45_acp", "45acp");
        m.put("d357", "357mag");
        m.put("d50_ae", "50ae");
        m.put("5_7x28", "57x28");
        m.put("7_62x51", "308");
        m.put("d338", "338");
        m.put("d50_bmg", "50bmg");
        m.put("12_70", "12g");
        m.put("40x46", "40mm");
        m.put("d22_wmr", "22wmr");
        m.put("d500_magnum", "500mag");
        m.put("5_8x42", "58x42");
        m.put("d30_06", "30_06");
        m.put("7_92x57_mauser", "792x57");
        m.put("d45_70", "45_70");
        m.put("72_5", "rpg_rocket");
        return m;
    }

    private final PackOutput output;

    public CaliberAmmoDataProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        Path root = output.getOutputFolder();
        Path projectRoot = findProjectRoot(root);
        Path csvPath = projectRoot.resolve("docs").resolve("tarkov_ammo_stats.csv");
        List<String> lines;
        try {
            lines = Files.readAllLines(csvPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("[tacz_caliber_ammo] datagen 读取 CSV 失败: " + csvPath, e);
        }
        if (lines.size() < 2) {
            return CompletableFuture.completedFuture(null);
        }
        String[] header = splitCsv(lines.get(0));
        int iCal = col(header, "口径"), iAmmo = col(header, "弹药"), iDmg = col(header, "伤害"),
                iPen = col(header, "穿甲"), iFrag = col(header, "破片%"), iProj = col(header, "弹丸数"),
                iRecoil = col(header, "后坐力%"), iAcc = col(header, "精度%"), iSpeed = col(header, "初速"),
                iBc = col(header, "弹道系数"), iCat = col(header, "分类");

        List<CompletableFuture<?>> futures = new ArrayList<>();
        Set<String> calibersSeen = new LinkedHashSet<>();
        Map<String, String> calNames = new HashMap<>();
        Map<String, String> firstModelByCal = new HashMap<>();
        int sort = 10, written = 0, skipped = 0;

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }
            String[] c = splitCsv(line);
            if (c.length <= iProj) {
                continue;
            }
            String calId = CAL_ID.get(c[iCal].trim());
            if (calId == null) {
                continue; // 未映射口径 -> 跳过
            }
            String model = toModelId(c[iAmmo]);
            if (model.isEmpty()) {
                skipped++; // 纯中文名(异形霰弹等) -> 跳过, 待手工补
                continue;
            }
            int d = parseInt(c[iDmg]), pen = parseInt(c[iPen]), proj = parseInt(c[iProj]);
            int recoil = parseInt(c[iRecoil]), accuracy = parseInt(c[iAcc]), speed = parseInt(c[iSpeed]);
            double ballisticCoefficient = parseDouble(c[iBc]); // 弹道系数（Tarkov 弹道系数列，越大越耐远距离）
            double fragRaw = parseDouble(c[iFrag]);
            double frag = fragRaw > 1.5 ? fragRaw / 100.0 : fragRaw; // CSV 破片% 为百分数

            float kd = proj > 1 ? K_DMG_SHOT : K_DMG;
            double base = clamp(round(d * kd, 0.5), DMG_MIN, DMG_MAX);
            double armorIgnore = round(clamp((pen - PEN0) / (double) PEN_SPAN, 0, AI_CAP), 0.05);
            int pierce = 1 + (pen >= P2 ? 1 : 0) + (pen >= P3 ? 1 : 0);
            double headShot = round(clamp(HS_BASE + HS_K * frag, HS_MIN, HS_MAX), 0.05);
            boolean customSlot = hasCustomSlot(projectRoot, calId, model);
            String display;
            if (customSlot) {
                display = NS + ":" + calId + "_" + model + "_display";
            } else if (PROTO.containsKey(calId)) {
                display = "tacz:" + PROTO.get(calId) + "_display";
            } else {
                display = NS + ":" + calId + "_display";
            }

            JsonObject json = new JsonObject();
            json.addProperty("name", "ammo." + NS + "." + calId + "." + model);
            json.addProperty("display", display);
            json.addProperty("stack_size", stackSizeFor(c[iCat]));
            json.addProperty("sort", sort++);
            json.addProperty("caliber", calId);
            json.addProperty("baseDamage", (float) base);
            json.addProperty("armorIgnore", (float) armorIgnore);
            json.addProperty("headShotMultiplier", (float) headShot);
            json.addProperty("pierce", pierce);
            json.addProperty("recoil", recoil);       // 后坐力%（带符号）
            json.addProperty("accuracy", accuracy);   // 精度%（带符号，可 >100）
            json.addProperty("speed", speed);         // 初速原始值 m/s（运行时 × 配置 bulletSpeedScale / 20）
            json.addProperty("pelletCount", proj);    // 弹丸数
            json.addProperty("ballisticCoefficient", (float) ballisticCoefficient); // 弹道系数（优势射程外倒数衰减速率 ∝ 1/(BC×x)）
            Path p = root.resolve("data/" + NS + "/index/ammo/" + calId + "/" + model + ".json");
            futures.add(DataProvider.saveStable(cache, json, p));
            // 配方（gun smith table）：参考 TacZ 原版弹药配方（铜+火药），高穿弹耗更多铜、AP 额外加铁芯
            JsonObject recipe = buildRecipe(calId, model, c[iCat], pen);
            futures.add(DataProvider.saveStable(cache, recipe,
                    root.resolve("data/" + NS + "/" + GenerationDialect.recipeDirectory()
                        + "/ammo/" + calId + "/" + model + ".json")));
            // 自定义 slot 贴图的弹种：生成 per-变种 display（复用原型 3D 几何/uv，slot 指向各自贴图）
            if (customSlot) {
                JsonObject vDisp = new JsonObject();
                if (PROTO.containsKey(calId)) {
                    String protoBase = PROTO.get(calId);
                    if (!NO_HELD_MODEL.contains(protoBase)) {
                        // 有 3D 手持几何体：model 用真实 geo 名（40mm 的 geo 是 40mm_grenade），texture 用 uv 基名。
                        vDisp.addProperty("model", "tacz:ammo/" + AMMO_GEO.getOrDefault(protoBase, protoBase));
                        vDisp.addProperty("texture", "tacz:ammo/uv/" + protoBase);
                    }
                    // NO_HELD_MODEL 的口径 tacz 本体也无 model，故省略；手持退化为 slot 平面图标（与原版一致）。
                } else {
                    vDisp.addProperty("model", "tacz:ammo/308");
                    vDisp.addProperty("texture", NS + ":ammo/uv/" + calId);
                }
                vDisp.addProperty("slot", NS + ":ammo/slot/" + calId + "/" + model);
                // 自定义飞行弹(entity)：该口径在 ENTITY_MODEL 中时附加 entity 块。
                // 存在 per-变种 uv -> 用本命名空间自定义 model+uv；否则回退 tacz 默认 entity。
                String entBase = ENTITY_MODEL.get(calId);
                if (entBase != null) {
                    JsonObject ent = new JsonObject();
                    Path entUv = projectRoot.resolve("src/main/resources/assets/" + NS
                            + "/textures/ammo_entity/" + calId + "/" + model + ".png");
                    if (Files.exists(entUv)) {
                        ent.addProperty("model", NS + ":ammo_entity/" + entBase);
                        ent.addProperty("texture", NS + ":ammo_entity/" + calId + "/" + model);
                    } else {
                        ent.addProperty("model", "tacz:ammo_entity/" + entBase);
                        ent.addProperty("texture", "tacz:ammo_entity/" + entBase);
                    }
                    vDisp.add("entity", ent);
                }
                futures.add(DataProvider.saveStable(cache, vDisp,
                        root.resolve("assets/" + NS + "/display/ammo/" + calId + "_" + model + "_display.json")));
            }
            calibersSeen.add(calId);
            calNames.putIfAbsent(calId, caliberDisplayName(c[iCal]));
            firstModelByCal.putIfAbsent(calId, model);
            written++;
        }

        // 万用弹（特殊口径 universal）：speed/pelletCount=0 表示不覆写以适配任意枪；
        // 爆头/护穿等取随意合理值。不入 calibersSeen -> 不注册 calibers/*.json、不参与枪的模糊口径匹配；
        // 贴图/display 走本 mod 命名空间纯色占位（universal 无 TacZ 原型）。
        {
            String uCal = "universal";
            String uModel = "round";
            JsonObject uj = new JsonObject();
            uj.addProperty("name", "ammo." + NS + "." + uCal + "." + uModel);
            uj.addProperty("display", NS + ":" + uCal + "_display");
            uj.addProperty("stack_size", 60);
            uj.addProperty("sort", sort++);
            uj.addProperty("caliber", uCal);
            uj.addProperty("baseDamage", 5.0f);
            uj.addProperty("armorIgnore", 0.1f);
            uj.addProperty("headShotMultiplier", 1.5f);
            uj.addProperty("pierce", 1);
            uj.addProperty("recoil", 0);
            uj.addProperty("accuracy", 0);
            uj.addProperty("speed", 0);
            uj.addProperty("pelletCount", 0);
            uj.addProperty("ballisticCoefficient", 0.2f); // 万用弹：中庸弹道系数
            futures.add(DataProvider.saveStable(cache, uj,
                    root.resolve("data/" + NS + "/index/ammo/" + uCal + "/" + uModel + ".json")));
            // 万用弹不生成配方（不可合成）：仅注册弹药本体, 获取途径由外部（战利品/指令/其它数据包）决定。
            try {
                byte[] uPng = solidPng(colorFor(uCal), 16);
                writePng(cache, root.resolve("assets/" + NS + "/textures/ammo/slot/" + uCal + ".png"), uPng);
                writePng(cache, root.resolve("assets/" + NS + "/textures/ammo/uv/" + uCal + ".png"), uPng);
            } catch (IOException e) {
                throw new RuntimeException("[tacz_caliber_ammo] 万用弹占位贴图写入失败", e);
            }
            JsonObject uDisp = new JsonObject();
            uDisp.addProperty("model", "tacz:ammo/308");
            uDisp.addProperty("texture", NS + ":ammo/uv/" + uCal);
            uDisp.addProperty("slot", NS + ":ammo/slot/" + uCal);
            futures.add(DataProvider.saveStable(cache, uDisp,
                    root.resolve("assets/" + NS + "/display/ammo/" + uCal + "_display.json")));
            written++;
        }

        // 口径定义
        for (String calId : calibersSeen) {
            JsonObject j = new JsonObject();
            j.addProperty("name", calNames.get(calId));
            futures.add(DataProvider.saveStable(cache, j,
                    root.resolve("data/" + NS + "/calibers/" + calId + ".json")));
        }

        // 新弹药工作台 advanced_ammo_workbench（本 mod 独立方块, 不覆写 TacZ）：每口径一个独立 tab 页。
        // TacZ 会为每个 block index 自动生成创造物品（GunSmithTableItem.fillItemCategory, BlockId=index 键）,
        // 故只需 datagen 出 index/blocks + data/blocks 两个文件即可获得可放置可开启的工作台。
        {
            String wbId = "advanced_ammo_workbench";
            JsonObject idx = new JsonObject();
            idx.addProperty("name", "block." + NS + "." + wbId + ".name");
            idx.addProperty("display", "tacz:ammo_workbench"); // 复用 TacZ 弹药工作台外观
            idx.addProperty("data", NS + ":" + wbId);
            idx.addProperty("tooltip", "block." + NS + "." + wbId + ".desc");
            idx.addProperty("id", "tacz:workbench_a");         // 物理方块 1x1x1
            idx.addProperty("sort", 100);
            futures.add(DataProvider.saveStable(cache, idx,
                    root.resolve("data/" + NS + "/index/blocks/" + wbId + ".json")));

            JsonObject blockData = new JsonObject();
            blockData.addProperty("filter", "tacz:default");   // ^.*$ 放行本 mod 全部配方
            JsonArray tabs = new JsonArray();
            List<String> sortedCals = new ArrayList<>(calibersSeen);
            sortedCals.sort(null);
            for (String calId : sortedCals) {
                addAmmoTab(tabs, NS + ":" + calId, calNames.get(calId),
                        NS + ":" + calId + "/" + firstModelByCal.get(calId));
            }
            blockData.add("tabs", tabs);
            futures.add(DataProvider.saveStable(cache, blockData,
                    root.resolve("data/" + NS + "/data/blocks/" + wbId + ".json")));
        }

        // 无原型口径 -> 纯色占位 (uv/slot 单色 PNG + display, 复用通用 TacZ 子弹几何体)
        int placeholders = 0;
        for (String calId : calibersSeen) {
            if (PROTO.containsKey(calId)) {
                continue;
            }
            int rgb = colorFor(calId);
            try {
                byte[] png = solidPng(rgb, 16);
                writePng(cache, root.resolve("assets/" + NS + "/textures/ammo/slot/" + calId + ".png"), png);
                writePng(cache, root.resolve("assets/" + NS + "/textures/ammo/uv/" + calId + ".png"), png);
            } catch (IOException e) {
                throw new RuntimeException("[tacz_caliber_ammo] 占位贴图写入失败: " + calId, e);
            }
            JsonObject disp = new JsonObject();
            disp.addProperty("model", "tacz:ammo/308"); // 复用通用 TacZ 子弹几何体
            disp.addProperty("texture", NS + ":ammo/uv/" + calId);
            disp.addProperty("slot", NS + ":ammo/slot/" + calId);
            futures.add(DataProvider.saveStable(cache, disp,
                    root.resolve("assets/" + NS + "/display/ammo/" + calId + "_display.json")));
            placeholders++;
        }

        LOGGER.info("[tacz_caliber_ammo] datagen: 写 {} 弹药 / {} 口径 (占位 {}), 跳过空 id {} 条",
                written, calibersSeen.size(), placeholders, skipped);
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    /** 该弹种是否有自定义 slot 贴图（src/main/resources 手放）。有则生成 per-变种 display 用各自图标。 */
    private static boolean hasCustomSlot(Path projectRoot, String calId, String model) {
        Path png = projectRoot.resolve("src/main/resources/assets/" + NS
                + "/textures/ammo/slot/" + calId + "/" + model + ".png");
        return Files.exists(png);
    }

    // ==== 型号 id 规范化 ====
    private static String toModelId(String name) {
        String s = name.replaceAll("[\\(（][^\\)）]*[\\)）]", " "); // 去括号限定词
        s = s.replaceAll("[^\\x00-\\x7F]", " "); // 去非 ASCII (中文/全角)
        String[] toks = s.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        boolean lead = true;
        for (String t : toks) {
            if (t.isEmpty()) {
                continue;
            }
            if (lead && (t.matches("^[.\\d][\\d.xX/\\-]*$") || STOP.contains(t.toUpperCase(Locale.ROOT)))) {
                continue; // 丢弃前导口径数字与限定词
            }
            lead = false;
            if (sb.length() > 0) {
                sb.append('_');
            }
            sb.append(t);
        }
        String m = sb.toString().toLowerCase(Locale.ROOT).replaceAll("[./\\- ]", "_").replaceAll("_+", "_");
        return m.replaceAll("^_+|_+$", "");
    }

    /** 口径展示名: 取 CSV 口径名去非 ASCII (保留 5.56x45mm NATO / .357 之类)。 */
    private static String caliberDisplayName(String csvCaliber) {
        String s = csvCaliber.replaceAll("[^\\x00-\\x7F]", "").trim();
        return s.isEmpty() ? csvCaliber.trim() : s;
    }

    // ==== 弹药配方（tacz:gun_smith_table_crafting）====

    /**
     * 参考 TacZ 原版弹药配方（铜锭 + 火药）按类别定基线；<b>穿甲越高铜越多</b>，穿甲 ≥ 40 额外加铁粒（AP 代价）。
     * 输出 {@code result.id = tacz_caliber_ammo:<口径>/<型号>}，即制枪台产出对应 NBT 弹药。
     */
    private static JsonObject buildRecipe(String calId, String model, String category, int pen) {
        int baseCu;
        int baseGp;
        int count;
        switch (category == null ? "" : category.trim()) {
            case "手枪":
                baseCu = 10; baseGp = 2; count = 45; break;
            case "PDW":
                baseCu = 12; baseGp = 2; count = 48; break;
            case "步枪":
                baseCu = 15; baseGp = 3; count = 40; break;
            case "霰弹枪":
                baseCu = 15; baseGp = 5; count = 18; break;
            case "榴弹":
                baseCu = 12; baseGp = 8; count = 6; break;
            default: // 信号弹等
                baseCu = 10; baseGp = 2; count = 24; break;
        }
        int copper = baseCu + (int) Math.round(pen / 6.0);                    // 高穿 -> 更多铜
        int ironNugget = pen >= 40 ? (int) Math.round((pen - 30) / 6.0) : 0;  // AP -> 加铁芯
        JsonArray materials = new JsonArray();
        materials.add(material(GenerationDialect.copperIngotTag(), Math.max(1, copper)));
        materials.add(material(GenerationDialect.gunpowderTag(), Math.max(1, baseGp)));
        if (ironNugget > 0) {
            materials.add(material(GenerationDialect.ironNuggetTag(), ironNugget));
        }
        JsonObject result = new JsonObject();
        result.addProperty("type", "ammo");
        // 每口径单开一页：group = 本命名空间口径 id，对应 advanced_ammo_workbench 里为该口径生成的 tab
        result.addProperty("group", NS + ":" + calId);
        result.addProperty("id", NS + ":" + calId + "/" + model);
        result.addProperty("count", count);
        JsonObject recipe = new JsonObject();
        recipe.add("materials", materials);
        recipe.add("result", result);
        recipe.addProperty("type", "tacz:gun_smith_table_crafting");
        return recipe;
    }

    /** Stack size by ammo category (CSV col1): rifle 40, shotgun 20, grenade/rocket 6, else (pistol/PDW/flare) 60. */
    private static int stackSizeFor(String category) {
        switch (category == null ? "" : category.trim()) {
            case "步枪":
                return 40;
            case "霰弹枪":
                return 20;
            case "榴弹":
                return 6;
            case "信号弹":
                return 1;
            default:
                return 60;
        }
    }

    private static JsonObject material(String tag, int count) {
        JsonObject item = new JsonObject();
        item.addProperty("tag", tag);
        JsonObject m = new JsonObject();
        m.add("item", item);
        m.addProperty("count", count);
        return m;
    }

    /**
     * advanced_ammo_workbench 的一个 tab（口径页）：{@code id} 需与该口径配方的 {@code result.group} 一致；
     * {@code name} 取口径展示名（无对应翻译键时按字面渲染）；图标用 {@code tacz:ammo} 物品 + {@code AmmoId} nbt（该口径首个弹种）。
     */
    private static void addAmmoTab(JsonArray tabs, String id, String name, String ammoId) {
        JsonObject t = new JsonObject();
        t.addProperty("id", id);
        t.addProperty("name", name);
        t.add("icon", GenerationDialect.ammoIcon(ammoId));
        tabs.add(t);
    }

    private static Path findProjectRoot(Path outputRoot) {
        Path current = outputRoot.toAbsolutePath();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("docs/tarkov_ammo_stats.csv"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("[tacz_caliber_ammo] cannot locate docs/tarkov_ammo_stats.csv from "
                + outputRoot.toAbsolutePath());
    }

    // ==== CSV / 数值工具 ====
    private static String[] splitCsv(String line) {
        String l = line.trim();
        if (l.startsWith("\"")) {
            l = l.substring(1);
        }
        if (l.endsWith("\"")) {
            l = l.substring(0, l.length() - 1);
        }
        return l.split("\",\"", -1);
    }

    private static int col(String[] header, String name) {
        for (int i = 0; i < header.length; i++) {
            if (header[i].trim().equals(name)) {
                return i;
            }
        }
        throw new IllegalStateException("[tacz_caliber_ammo] CSV 缺列: " + name);
    }

    private static int parseInt(String s) {
        try {
            return (int) Math.round(Double.parseDouble(s.trim()));
        } catch (Exception e) {
            return 0;
        }
    }

    private static double parseDouble(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private static double round(double x, double step) {
        return Math.round(x / step) * step;
    }

    private static double clamp(double x, double lo, double hi) {
        return Math.max(lo, Math.min(hi, x));
    }

    // ==== 占位贴图 ====
    private static int colorFor(String id) {
        int h = id.hashCode();
        float hue = (((h % 360) + 360) % 360) / 360f;
        return Color.HSBtoRGB(hue, 0.55f, 0.80f) & 0xFFFFFF;
    }

    private static byte[] solidPng(int rgb, int size) throws IOException {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(0xFF000000 | rgb, true));
        g.fillRect(0, 0, size, size);
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    private static void writePng(CachedOutput cache, Path path, byte[] data) throws IOException {
        cache.writeIfNeeded(path, data, Hashing.sha256().hashBytes(data));
    }

    @Override
    public String getName() {
        return "TacZ Caliber Ammo: EFT CSV -> calibers + ammo index + placeholder textures";
    }
}
