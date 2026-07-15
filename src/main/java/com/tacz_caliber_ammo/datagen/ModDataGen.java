package com.tacz_caliber_ammo.datagen;

import com.tacz_caliber_ammo.TaczCaliberAmmo;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * datagen 入口：GatherDataEvent(MOD 总线)注册各 DataProvider。
 * 由 build.gradle.kts 的 {@code data} runConfig（任务 {@code runData}）触发，输出到 {@code src/generated/resources}。
 */
@Mod.EventBusSubscriber(modid = TaczCaliberAmmo.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModDataGen {

    private ModDataGen() {
    }

    @SubscribeEvent
    public static void onGatherData(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        PackOutput output = gen.getPackOutput();
        gen.addProvider(event.includeServer(), new CaliberAmmoDataProvider(output));
        gen.addProvider(event.includeServer(), new GunCaliberModifyProvider(output));
    }
}
