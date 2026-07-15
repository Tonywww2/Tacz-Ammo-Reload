package com.tacz_caliber_ammo.mixin;

import java.util.LinkedList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.ExtraDamage;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz_caliber_ammo.caliber.AmmoProfile;
import com.tacz_caliber_ammo.caliber.CaliberManager;
import com.tacz_caliber_ammo.caliber.GunDamageModifier;
import com.tacz_caliber_ammo.util.DebugLog;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 伤害所有权反转（PB-3）。在子弹构造末尾按 ammoId 取弹药档，
 * 覆盖 damageAmount（单点标量 = ammoBase*(1+gun.percent)+gun.flat，无距离曲线）
 * 与 armorIgnore/headShot/pierce；未配置则保留 TacZ 派生值。
 * 配件/霞弹倒乘在 getDamage 内在本基础之后叠加（最后结算）。
 * CR-1: 目标 TacZ 类 EntityKineticBullet, 置 remap=false(其字段/构造非 MC 映射)。
 */
@Mixin(value = EntityKineticBullet.class, remap = false)
public class EntityKineticBulletMixin {

    @Shadow
    private LinkedList<ExtraDamage.DistanceDamagePair> damageAmount;
    @Shadow
    private float armorIgnore;
    @Shadow
    private float headShot;
    @Shadow
    private int pierce;
    @Shadow
    private ResourceLocation ammoId;
    @Shadow
    private ResourceLocation gunId;
    @Shadow
    private float damageModifier;
    @Shadow
    private float shotDamageMultiplier;

    @Inject(
        method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/resources/ResourceLocation;ZLcom/tacz/guns/resource/pojo/data/gun/GunData;Lcom/tacz/guns/resource/pojo/data/gun/BulletData;)V",
        at = @At("TAIL"))
    private void taczCaliberAmmo$overrideBallistics(EntityType<? extends Projectile> type, Level worldIn,
            LivingEntity throwerIn, ItemStack gunItem, ResourceLocation ammoIdArg, ResourceLocation gunIdArg,
            ResourceLocation gunDisplayId, boolean isTracerAmmo, GunData gunData, BulletData bulletData,
            CallbackInfo ci) {
        AmmoProfile profile = CaliberManager.getAmmoProfile(this.ammoId);
        if (profile == null) {
            DebugLog.log("bullet<init>: ammo={} gun={} -> NO PROFILE, keep TacZ damage", this.ammoId, this.gunId);
            return; // 未配置 -> 保留 TacZ 派生值
        }
        float dmg = profile.baseDamage();
        GunDamageModifier mod = CaliberManager.getGunModifier(this.gunId);
        if (mod != null) {
            dmg = dmg * (1.0f + mod.percentDamage()) + mod.flatDamage();
        }
        dmg = Math.max(dmg, 0.0f);
        LinkedList<ExtraDamage.DistanceDamagePair> flat = new LinkedList<>();
        flat.add(new ExtraDamage.DistanceDamagePair(0.0f, dmg));
        this.damageAmount = flat;
        this.armorIgnore = Mth.clamp(profile.armorIgnore(), 0.0f, 1.0f);
        this.headShot = Math.max(profile.headShotMultiplier(), 0.0f);
        this.pierce = Math.max(profile.pierce(), 1);
        DebugLog.log("bullet<init>: ammo={} gun={} base={} gunMod={} -> finalDmg={} armorIgnore={} headShot={} pierce={} (damageModifier={} shotMult={})",
                this.ammoId, this.gunId, profile.baseDamage(),
                mod == null ? "none" : ("flat=" + mod.flatDamage() + ",pct=" + mod.percentDamage()),
                dmg, this.armorIgnore, this.headShot, this.pierce, this.damageModifier, this.shotDamageMultiplier);
    }

    /**
     * 多头弹药（霰弹）伤害修正。TacZ 默认 {@code applyShotgunDamageSpread(bulletCount)} 把伤害按 1/bulletCount
     * 均摊到每颗弹丸（{@code damageModifier=1/bulletCount}）—— 因 TacZ 假设“枪”持有总伤害。本 mod 设计里“弹药”
     * 持有的是每颗弹丸的伤害，故对已配置弹药档的弹药跳过该均摊：每颗弹丸都打满 baseDamage，命中 N 颗即 N×baseDamage。
     * （原行为下总伤害恒为 baseDamage，多颗命中却“只造成一次伤害”的观感即源于此均摊。TacZ 已在 tacAttackEntity
     * 里 {@code f_19802_=0} 重置无敌帧，故每颗弹丸本就都能结算，只是被均摊缩小了。）
     * 未配置档的弹药保留 TacZ 均摊。该方法在构造之后、每颗弹丸创建时调用（ScriptAPI.shootOnce 循环内）。
     */
    @Inject(method = "applyShotgunDamageSpread", at = @At("HEAD"), cancellable = true)
    private void taczCaliberAmmo$perPelletFullDamage(int bulletCount, CallbackInfo ci) {
        if (bulletCount > 1 && CaliberManager.getAmmoProfile(this.ammoId) != null) {
            DebugLog.log("applyShotgunDamageSpread SKIP ammo={} bulletCount={} -> 每颗弹丸打满 baseDamage", this.ammoId,
                    bulletCount);
            ci.cancel();
        }
    }

    @Inject(method = "getDamage(Lnet/minecraft/world/phys/Vec3;)F", at = @At("RETURN"))
    private void taczCaliberAmmo$logDamage(Vec3 hitVec, CallbackInfoReturnable<Float> cir) {
        DebugLog.log("getDamage: ammo={} gun={} -> {} (damageAmount[0]={} damageModifier={} shotMult={})",
                this.ammoId, this.gunId, cir.getReturnValue(),
                this.damageAmount.isEmpty() ? "-" : this.damageAmount.get(0).getDamage(),
                this.damageModifier, this.shotDamageMultiplier);
    }
}
