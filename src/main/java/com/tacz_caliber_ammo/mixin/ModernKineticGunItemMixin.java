package com.tacz_caliber_ammo.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.tacz.guns.entity.shooter.ShooterDataHolder;
import com.tacz.guns.item.ModernKineticGunItem;
import com.tacz_caliber_ammo.duck.ISpeedDecayBullet;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;

/**
 * 速度衰减挂钩点。ModernKineticGunItem（TacZ 现代枪的实际物品类，如 m700/db_long）<b>覆写了</b>
 * AbstractGunItem.doBulletSpread（用 lua calcSpread 算散布后 shootFromRotation/m_37251_ 设初速度），
 * ScriptAPI.shootOnce 虚分派调的正是这个覆写版——故 hook 父类 AbstractGunItem.doBulletSpread 不触发。
 * 在其 TAIL（初速度已设、spawn 之前）对本 mod 弹药经 {@link ISpeedDecayBullet} 回调设飞行 friction，
 * 此点在 spawn 前故 friction 随 spawn 数据同步到客户端（两端一致）。CR: TacZ 方法, remap=false。
 */
@Mixin(ModernKineticGunItem.class)
public class ModernKineticGunItemMixin {

    @Inject(method = "doBulletSpread", at = @At("TAIL"), remap = false)
    private void tacz_caliber_ammo$initBulletSpeedDecay(ShooterDataHolder dataHolder, ItemStack gunItem,
            LivingEntity shooter, Projectile projectile, int bulletCnt, float processedSpeed, float inaccuracy,
            float pitch, float yaw, CallbackInfo ci) {
        if (projectile instanceof ISpeedDecayBullet bullet) {
            bullet.taczCaliberAmmo$initSpeedDecay();
        }
    }
}
