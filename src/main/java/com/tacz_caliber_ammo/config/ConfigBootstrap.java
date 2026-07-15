package com.tacz_caliber_ammo.config;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 注册本 mod 的 COMMON 配置（口径解析特性开关）。在 FMLConstructModEvent 里注册 —— 早于配置加载。
 * 遵循 §2 自注册约定：@Mod.EventBusSubscriber 自动发现, 主类 TaczCaliberAmmo 不改。
 */
@Mod.EventBusSubscriber(modid = "tacz_caliber_ammo", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ConfigBootstrap {

    private ConfigBootstrap() {
    }

    @SubscribeEvent
    public static void onConstruct(FMLConstructModEvent event) {
        ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, ModConfig.SPEC);
    }
}
