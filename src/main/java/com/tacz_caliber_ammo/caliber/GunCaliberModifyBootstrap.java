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
 * Tier 1 {@code modify_gun_caliber} 独立数据包的自注册。监听 {@code data/<ns>/modify_gun_caliber/*.json}，
 * 随服务端数据 reload 灌入 {@link CaliberManager#rebuildModify}。模板同 {@link CaliberDataBootstrap}。
 *
 * <p>文件格式：{@code { "priority": int, "guns": { "tacz:m4a1": ["tacz_caliber_ammo:5_56x45"], ... } }}。
 * 跨文件同枪按 priority 取最高（见 rebuildModify）。
 */
@Mod.EventBusSubscriber(modid = "tacz_caliber_ammo", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GunCaliberModifyBootstrap {

    private static final Gson GSON = new Gson();

    private GunCaliberModifyBootstrap() {
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new ModifyGunCaliberListener());
    }

    private static final class ModifyGunCaliberListener extends SimpleJsonResourceReloadListener {
        ModifyGunCaliberListener() {
            super(GSON, "modify_gun_caliber");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager,
                             ProfilerFiller profiler) {
            CaliberManager.rebuildModify(object);
        }
    }
}
