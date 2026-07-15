package com.tacz_caliber_ammo.datagen;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/**
 * datagen：生成 {@code modify_gun_caliber} 独立数据包 demo（Tier 1 最高优先的 枪->口径 覆盖）。
 * demo：7.62x39 的 4 把原版枪（ak47/rpk/sks_tactical/type_81）声明口径 {@code tacz_caliber_ammo:7_62x39}。
 * 输出 {@code data/tacz_caliber_ammo/modify_gun_caliber/tacz_762x39_guns.json}。
 */
public class GunCaliberModifyProvider implements DataProvider {

    private static final String NS = "tacz_caliber_ammo";
    private static final String[] GUNS_762X39 = {"ak47", "rpk", "sks_tactical", "type_81"};

    private final PackOutput output;

    public GunCaliberModifyProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
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
        return DataProvider.saveStable(cache, root, path);
    }

    @Override
    public String getName() {
        return "TacZ Caliber Ammo: modify_gun_caliber demo (7.62x39)";
    }
}
