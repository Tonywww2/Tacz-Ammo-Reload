package com.tacz_caliber_ammo.datagen;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;

/**
 * datagen 入口：GatherDataEvent(MOD 总线)注册各 DataProvider。
 * 由 build.gradle.kts 的 {@code data} runConfig（任务 {@code runData}）触发，输出到 {@code src/generated/resources}。
 */
public final class ModDataGen {

    private ModDataGen() {
    }

    public static void gather(DataGenerator generator, PackOutput output, boolean includeServer) {
        generator.addProvider(includeServer, new CaliberAmmoDataProvider(output));
        generator.addProvider(includeServer, new GunCaliberModifyProvider(output));
    }
}
