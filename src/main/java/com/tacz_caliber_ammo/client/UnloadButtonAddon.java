package com.tacz_caliber_ammo.client;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.gui.GunRefitScreen;
import com.tacz.guns.client.gui.components.FlatColorButton;
import com.tacz_caliber_ammo.network.CMsgUnloadAmmo;
import com.tacz_caliber_ammo.network.ModNetwork;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 客户端：构造 TacZ {@link GunRefitScreen} 的"退弹"按钮（区别于 TacZ 原"卸配件"按钮）。
 * 由 {@code GunRefitScreenMixin} 在每次 {@code init} 末尾调用并 {@code addRenderableWidget}——
 * 覆盖"显示图表"{@code switchHideButton()} 的重建路径（原 {@code ScreenEvent.Init.Post} 会被其 clearWidgets 清掉）。
 * 按钮用 TacZ {@link FlatColorButton}（扁平色块），与改装界面其它按钮样式一致；onPress 发 {@link CMsgUnloadAmmo}(selected 槽位)。
 * 空弹匣时按钮禁用（{@code active=false}）；主手非枪返回 {@code null}（不加按钮）。
 */
public final class UnloadButtonAddon {

    private UnloadButtonAddon() {
    }

    /** 为当前主手枪构造退弹按钮；主手非枪 / 无玩家返回 null。screenHeight 用于纵向定位。 */
    public static FlatColorButton createUnloadButton(int screenHeight) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        ItemStack gun = player.getMainHandItem();
        IGun iGun = IGun.getIGunOrNull(gun);
        if (iGun == null) {
            return null;
        }
        int slot = player.getInventory().selected;
        boolean hasAmmo = iGun.getCurrentAmmoCount(gun) > 0;
        int x = 6;
        int y = screenHeight / 2 - 8;
        FlatColorButton button = new FlatColorButton(x, y, 72, 16,
                Component.translatable("gui.tacz_caliber_ammo.unload"),
                b -> ModNetwork.CHANNEL.sendToServer(new CMsgUnloadAmmo(slot)));
        button.active = hasAmmo;
        return button;
    }
}
