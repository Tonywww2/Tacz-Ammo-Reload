package com.tacz_caliber_ammo.client;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.gui.GunRefitScreen;
import com.tacz_caliber_ammo.TaczCaliberAmmo;
import com.tacz_caliber_ammo.network.CMsgUnloadAmmo;
import com.tacz_caliber_ammo.network.ModNetwork;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端：在 TacZ {@link GunRefitScreen} 上加"退弹"按钮（区别于 TacZ 原"卸配件"按钮）。
 * {@code ScreenEvent.Init.Post} 加 widget；onPress 发 {@link CMsgUnloadAmmo}(selected 槽位)。
 * 空弹匣时按钮禁用。{@code Dist.CLIENT} 保证专用服务器不加载此类（其 import 含客户端类）。
 */
@Mod.EventBusSubscriber(modid = TaczCaliberAmmo.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class UnloadButtonAddon {

    private UnloadButtonAddon() {
    }

    @SubscribeEvent
    public static void onScreenInitPost(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof GunRefitScreen)) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack gun = player.getMainHandItem();
        IGun iGun = IGun.getIGunOrNull(gun);
        if (iGun == null) {
            return;
        }
        int slot = player.getInventory().selected;
        boolean hasAmmo = iGun.getCurrentAmmoCount(gun) > 0;

        Screen screen = event.getScreen();
        int x = 6;
        int y = screen.height / 2 - 10;
        Button button = Button.builder(
                        Component.translatable("gui.tacz_caliber_ammo.unload"),
                        b -> ModNetwork.CHANNEL.sendToServer(new CMsgUnloadAmmo(slot)))
                .bounds(x, y, 72, 20)
                .build();
        button.active = hasAmmo;
        event.addListener(button);
    }
}
