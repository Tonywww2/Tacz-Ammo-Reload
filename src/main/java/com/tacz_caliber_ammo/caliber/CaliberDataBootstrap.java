package com.tacz_caliber_ammo.caliber;

import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 口径定义（{@code data/&lt;ns&gt;/calibers/*.json} → {@link Caliber} 的 name）的自注册。
 * 通过 Forge {@link AddReloadListenerEvent} 挂一个 {@link SimpleJsonResourceReloadListener}，
 * 随服务端数据 reload 灌入 {@link CaliberManager#rebuildCalibers}。弹药/枪的字段加载在 CommonDataManagerMixin，
 * 二者互不相干（本类只负责口径的友好名；说明文本由 {@link Caliber#tooltipKey()} 按 id 自动生成本地化）。
 */
@Mod.EventBusSubscriber(modid = "tacz_caliber_ammo", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CaliberDataBootstrap {

    private static final Gson GSON = new Gson();

    private CaliberDataBootstrap() {
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new CaliberDefinitionListener());
    }

    private static final class CaliberDefinitionListener extends SimpleJsonResourceReloadListener {
        CaliberDefinitionListener() {
            super(GSON, "calibers");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager,
                             ProfilerFiller profiler) {
            CaliberManager.rebuildCalibers(object);
        }
    }
}
