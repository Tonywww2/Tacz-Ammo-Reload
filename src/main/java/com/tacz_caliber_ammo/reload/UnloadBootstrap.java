package com.tacz_caliber_ammo.reload;

import com.tacz_caliber_ammo.TaczCaliberAmmo;
import com.tacz_caliber_ammo.network.ModNetwork;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * 退弹子系统自注册（MOD 总线）：在 {@link FMLCommonSetupEvent} 注册网络包。
 * 依 §2 自注册约定，主类 {@code TaczCaliberAmmo} 不改。
 */
@Mod.EventBusSubscriber(modid = TaczCaliberAmmo.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class UnloadBootstrap {

    private UnloadBootstrap() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }
}
