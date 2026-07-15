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
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;

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

    /** CSV 的中文口径名 -> 本项目口径 id (calibers.md)。未列入者跳过(信号弹/榴弹/杂项)。 */
    private static final Map<String, String> CAL_ID = buildCalId();

    /** 口径 id -> TacZ 原型 display 基名 (复用 tacz:&lt;base&gt;_display)；不在表中 = 无原型 -> 纯色占位。 */
    private static final Map<String, String> PROTO = buildProto();

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
        return m;
    }

    private final PackOutput output;

    public CaliberAmmoDataProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        Path root = output.getOutputFolder();
        // src/generated/resources -> src/generated -> src -> 项目根
        Path projectRoot = root.getParent().getParent().getParent();
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
                iRecoil = col(header, "后坐力%"), iAcc = col(header, "精度%"), iSpeed = col(header, "初速");

        List<CompletableFuture<?>> futures = new ArrayList<>();
        Set<String> calibersSeen = new LinkedHashSet<>();
        Map<String, String> calNames = new HashMap<>();
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
            double fragRaw = parseDouble(c[iFrag]);
            double frag = fragRaw > 1.5 ? fragRaw / 100.0 : fragRaw; // CSV 破片% 为百分数

            float kd = proj > 1 ? K_DMG_SHOT : K_DMG;
            double base = clamp(round(d * kd, 0.5), DMG_MIN, DMG_MAX);
            double armorIgnore = round(clamp((pen - PEN0) / (double) PEN_SPAN, 0, AI_CAP), 0.05);
            int pierce = 1 + (pen >= P2 ? 1 : 0) + (pen >= P3 ? 1 : 0);
            double headShot = round(clamp(HS_BASE + HS_K * frag, HS_MIN, HS_MAX), 0.05);
            String display = PROTO.containsKey(calId)
                    ? "tacz:" + PROTO.get(calId) + "_display"
                    : NS + ":" + calId + "_display";

            JsonObject json = new JsonObject();
            json.addProperty("name", "ammo." + NS + "." + calId + "." + model);
            json.addProperty("display", display);
            json.addProperty("stack_size", 60);
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
            Path p = root.resolve("data/" + NS + "/index/ammo/" + calId + "/" + model + ".json");
            futures.add(DataProvider.saveStable(cache, json, p));
            calibersSeen.add(calId);
            calNames.putIfAbsent(calId, caliberDisplayName(c[iCal]));
            written++;
        }

        // 口径定义
        for (String calId : calibersSeen) {
            JsonObject j = new JsonObject();
            j.addProperty("name", calNames.get(calId));
            futures.add(DataProvider.saveStable(cache, j,
                    root.resolve("data/" + NS + "/calibers/" + calId + ".json")));
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
        cache.writeIfNeeded(path, data, Hashing.sha1().hashBytes(data));
    }

    @Override
    public String getName() {
        return "TacZ Caliber Ammo: EFT CSV -> calibers + ammo index + placeholder textures";
    }
}
