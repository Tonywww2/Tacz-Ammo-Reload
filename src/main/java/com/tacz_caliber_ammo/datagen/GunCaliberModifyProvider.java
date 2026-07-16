package com.tacz_caliber_ammo.datagen;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/**
 * datagen：生成 {@code modify_gun_caliber} 独立数据包 demo（不覆写 TacZ 枪 index）。
 * <ul>
 *   <li>{@code tacz_762x39_guns.json}：7.62x39 的原版枪（rpk/sks_tactical/type_81）声明口径
 *       {@code tacz_caliber_ammo:7_62x39}（数组形式，仅覆盖口径）。</li>
 *   <li>{@code tacz_gun_damage.json}：ak47 的枪伤害修正（对象形式）——{@code flatDamage=0.5, percentDamage=0.05}，
 *       即 ak47 打出的（已配口径档的）弹药最终伤害 = 弹药基础伤害 × 1.05 + 0.5。</li>
 * </ul>
 */
public class GunCaliberModifyProvider implements DataProvider {

    private static final String NS = "tacz_caliber_ammo";
    private static final String[] GUNS_762X39 = {"rpk", "sks_tactical", "type_81"};

    private final PackOutput output;

    public GunCaliberModifyProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        // demo 1: 仅覆盖口径（数组形式）
        JsonObject root = new JsonObject();
        root.addProperty("priority", 0);
        JsonObject guns = new JsonObject();
        for (String gun : GUNS_762X39) {
            JsonArray calibers = new JsonArray();
            calibers.add(NS + ":7_62x39");
            guns.add("tacz:" + gun, calibers);
        }
        root.add("guns", guns);
        Path path = output.getOutputFolder().resolve("data/" + NS + "/modify_gun_caliber/tacz_762x39_guns.json");

        // demo 2: 枪伤害修正（对象形式）—— ak47 +0.5 固定伤害, +5% 伤害
        JsonObject dmgRoot = new JsonObject();
        dmgRoot.addProperty("priority", 0);
        JsonObject dmgGuns = new JsonObject();
        JsonObject ak47 = new JsonObject();
        ak47.addProperty("flatDamage", 0.5);
        ak47.addProperty("percentDamage", 0.05);
        dmgGuns.add("tacz:ak47", ak47);
        dmgRoot.add("guns", dmgGuns);
        Path dmgPath = output.getOutputFolder().resolve("data/" + NS + "/modify_gun_caliber/tacz_gun_damage.json");

        return CompletableFuture.allOf(
                DataProvider.saveStable(cache, root, path),
                DataProvider.saveStable(cache, dmgRoot, dmgPath));
    }

    @Override
    public String getName() {
        return "TacZ Caliber Ammo: modify_gun_caliber demo (7.62x39)";
    }
}
