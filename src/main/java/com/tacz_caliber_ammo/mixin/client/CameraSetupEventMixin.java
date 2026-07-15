package com.tacz_caliber_ammo.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.tacz.guns.client.event.CameraSetupEvent;
import com.tacz_caliber_ammo.caliber.AmmoProfile;
import com.tacz_caliber_ammo.caliber.CaliberManager;
import com.tacz_caliber_ammo.nbt.LoadedAmmoSequence;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 后坐力归弹药：TacZ 的 {@code CameraSetupEvent.initialCameraRecoil(GunFireEvent)}（客户端、仅本地玩家开火时）
 * 里两处 {@code GunRecoil.genPitch/genYawSplineFunction(F)} 的幅值各 ×(1 + 后坐力%/100)。
 *
 * <p>弹种取本地玩家主手枪 {@link LoadedAmmoSequence#peekHead} 的队首（即将出膛那发）；无档/无弹种系数取 1。
 * 该方法本就只处理本地玩家自己的开火，故只影响自己的镜头后坐力。CR: 目标 TacZ 客户端类，{@code remap=false}。
 */
@Mixin(value = CameraSetupEvent.class, remap = false)
public class CameraSetupEventMixin {

    @ModifyArg(method = "initialCameraRecoil",
            at = @At(value = "INVOKE",
                    target = "Lcom/tacz/guns/resource/pojo/data/gun/GunRecoil;genPitchSplineFunction(F)Lorg/apache/commons/math3/analysis/polynomials/PolynomialSplineFunction;"))
    private static float taczCaliberAmmo$recoilPitch(float magnitude) {
        return magnitude * taczCaliberAmmo$recoilFactor();
    }

    @ModifyArg(method = "initialCameraRecoil",
            at = @At(value = "INVOKE",
                    target = "Lcom/tacz/guns/resource/pojo/data/gun/GunRecoil;genYawSplineFunction(F)Lorg/apache/commons/math3/analysis/polynomials/PolynomialSplineFunction;"))
    private static float taczCaliberAmmo$recoilYaw(float magnitude) {
        return magnitude * taczCaliberAmmo$recoilFactor();
    }

    /** 本地玩家主手枪当前出膛弹种的后坐力系数 (1 + 后坐力%/100)；无则 1。 */
    @Unique
    private static float taczCaliberAmmo$recoilFactor() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return 1.0f;
        }
        ItemStack gun = mc.player.getMainHandItem();
        ResourceLocation next = LoadedAmmoSequence.peekHead(gun);
        if (next == null) {
            return 1.0f;
        }
        AmmoProfile p = CaliberManager.getAmmoProfile(next);
        if (p == null) {
            return 1.0f;
        }
        return 1.0f + p.recoilModifier() / 100.0f;
    }
}
