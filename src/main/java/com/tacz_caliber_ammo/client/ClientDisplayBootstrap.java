package com.tacz_caliber_ammo.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端信息显示自注册：订阅 Forge {@link ItemTooltipEvent}（仅客户端）并委派给 {@link TooltipHandler}。
 * 遵循 §2 自注册约定：@Mod.EventBusSubscriber 注解自动发现，主类 TaczCaliberAmmo 不改。PC-1 拥有。
 */
@Mod.EventBusSubscriber(modid = "tacz_caliber_ammo", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientDisplayBootstrap {

    private ClientDisplayBootstrap() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        TooltipHandler.appendTooltip(event.getItemStack(), event.getToolTip());
    }
}
