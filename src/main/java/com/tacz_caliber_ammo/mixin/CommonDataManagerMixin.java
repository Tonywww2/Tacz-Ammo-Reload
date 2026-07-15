package com.tacz_caliber_ammo.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.gson.JsonElement;
import com.tacz.guns.resource.manager.CommonDataManager;
import com.tacz.guns.resource.network.DataType;
import com.tacz_caliber_ammo.caliber.CaliberManager;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * 在 TacZ 数据 reload 的 {@code CommonDataManager.apply} 末尾, 按 {@code this.type} 捕获
 * 弹药/枪 JSON（含我方新增字段, 含 id）灌入 {@link CaliberManager}。
 *
 * <p>PB-1 拥有。替代原先的两个 serializer mixin —— 因为 {@code CommonAmmoIndexSerializer.deserialize}
 * 只拿到 JSON 而拿不到 id, 无法按 id 建索引; 而此处 {@code pObject} 同时含 id 与原始 JSON。见 §7 CR-1。
 */
@Mixin(value = CommonDataManager.class, remap = false)
public abstract class CommonDataManagerMixin {

    @Shadow
    @Final
    private DataType type;

    // apply 由 TacZ 类声明其覆写、type 为 TacZ 字段，均不参与本 mod 的 MC 混淆映射；
    // 依 §2/CR-1 约定，本 mixin 整体 remap=false，开发环境(Mojmap)下按字面名匹配。
    @Inject(method = "apply", at = @At("TAIL"))
    private void tacz_caliber_ammo$onApply(Map<ResourceLocation, JsonElement> pObject,
                                           ResourceManager resourceManager, ProfilerFiller profiler,
                                           CallbackInfo ci) {
        if (this.type == DataType.AMMO_INDEX) {
            CaliberManager.rebuildAmmo(pObject);
        } else if (this.type == DataType.GUN_INDEX) {
            CaliberManager.rebuildGun(pObject);
        }
    }
}
