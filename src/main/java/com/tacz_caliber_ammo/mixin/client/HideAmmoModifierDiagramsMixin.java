package com.tacz_caliber_ammo.mixin.client;

import java.util.Collections;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.tacz.guns.api.modifier.IAttachmentModifier;
import com.tacz.guns.resource.modifier.custom.AmmoSpeedModifier;
import com.tacz.guns.resource.modifier.custom.ArmorIgnoreModifier;
import com.tacz.guns.resource.modifier.custom.DamageModifier;
import com.tacz.guns.resource.modifier.custom.HeadShotModifier;
import com.tacz.guns.resource.modifier.custom.KnockbackModifier;
import com.tacz.guns.resource.modifier.custom.PierceModifier;

/**
 * 从枪械属性图表(改装页"显示图表")隐藏已移到弹药上的属性行:
 * 伤害 / 弹速 / 护甲穿透 / 爆头倍率 / 穿透 / 击退。这些现由弹药(AmmoProfile/AmmoEffects)承载。
 *
 * 稳妥做法: 直接对这 6 个 Modifier 具体类自己 override 的
 * getPropertyDiagramsData(返回空表) 与 getDiagramsDataSize(返回 0) 注入 HEAD 提前返回。
 * GunPropertyDiagrams.draw 里 getModifiers().forEach 遍历到空表 -> 不画行;
 * getHidePropertyButtonYOffset 累加 getDiagramsDataSize 得 0 -> 背景框高度自动正确。
 * 全部目标是稳定的 named 方法(在各类字节码内), Mixin 必匹配, 不依赖任何 lambda 名
 * (早先 @Redirect GunPropertyDiagrams.getModifiers 要命中 draw 里 ifPresent 的 lambda,
 * 而 lambda 合成名 lambda$draw$N 在 Loom 重映射 TacZ 后不可靠, 静默失效)。
 *
 * ExplosionModifier / IgniteModifier 未 override 这两法(用接口 default = emptyList/0),
 * 本就不画行, 无需处理。这两法只被属性图表 GUI 调用, 拦截不影响伤害/弹道计算(走 eval/getCache)。
 * 目标为 TacZ 类, 类级 remap=false; 仅客户端。
 */
@Mixin(value = {
        DamageModifier.class,
        AmmoSpeedModifier.class,
        ArmorIgnoreModifier.class,
        HeadShotModifier.class,
        PierceModifier.class,
        KnockbackModifier.class
}, remap = false)
public class HideAmmoModifierDiagramsMixin {

    @Inject(method = "getPropertyDiagramsData", at = @At("HEAD"), cancellable = true)
    private void tacz_caliber_ammo$hideDiagramRows(
            CallbackInfoReturnable<List<IAttachmentModifier.DiagramsData>> cir) {
        cir.setReturnValue(Collections.emptyList());
    }

    @Inject(method = "getDiagramsDataSize", at = @At("HEAD"), cancellable = true)
    private void tacz_caliber_ammo$hideDiagramSize(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(0);
    }
}