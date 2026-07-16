package com.tacz_caliber_ammo.effect;

import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 弹药效果脚本的自注册：随服务端数据 reload 加载 {@code data/<ns>/ammo_effect_scripts/*.lua}。
 * 遵循 §2 自注册约定（@Mod.EventBusSubscriber 自动发现，主类不改），与 {@code CaliberDataBootstrap} 同法。
 */
@Mod.EventBusSubscriber(modid = "tacz_caliber_ammo", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EffectScriptBootstrap {

    private EffectScriptBootstrap() {
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(EffectScriptManager.listener());
    }
}
