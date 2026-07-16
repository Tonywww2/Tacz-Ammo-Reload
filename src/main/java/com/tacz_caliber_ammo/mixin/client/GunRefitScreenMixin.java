package com.tacz_caliber_ammo.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.tacz.guns.client.gui.GunRefitScreen;
import com.tacz.guns.client.gui.components.FlatColorButton;
import com.tacz_caliber_ammo.client.UnloadButtonAddon;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 在 GunRefitScreen 每次 init（{@code m_7856_}）末尾补加"退弹"按钮。
 *
 * <p>原先用 Forge {@code ScreenEvent.Init.Post} 加按钮，但改装页"显示图表"按钮的 {@code switchHideButton()}
 * 直接调 {@code m_7856_()}（先 {@code clearWidgets} 清掉本退弹按钮，再重加 TacZ 自己的按钮），
 * 且不触发 ScreenEvent，导致点击"显示图表"后退弹按钮消失；缩放窗口/重开走 {@code Screen.init(mc,w,h)}
 * 才触发事件、按钮恢复。改为 mixin {@code init@TAIL}，覆盖所有重建路径（打开/resize/switchHideButton）。
 *
 * <p>目标为 TacZ 类，类级 {@code remap=false}；{@code init} 是 MC 继承方法（{@code m_7856_}），注入单独 {@code remap=true}。
 */
@Mixin(value = GunRefitScreen.class, remap = false)
public abstract class GunRefitScreenMixin extends Screen {

    private GunRefitScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init()V", at = @At("TAIL"), remap = true)
    private void tacz_caliber_ammo$addUnloadButton(CallbackInfo ci) {
        FlatColorButton button = UnloadButtonAddon.createUnloadButton(this.height);
        if (button != null) {
            this.addRenderableWidget(button);
        }
    }
}
