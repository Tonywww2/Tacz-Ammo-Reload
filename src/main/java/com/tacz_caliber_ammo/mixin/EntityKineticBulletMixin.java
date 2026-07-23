package com.tacz_caliber_ammo.mixin;

import java.util.LinkedList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.tacz.guns.api.GunProperties;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.ExtraDamage;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz_caliber_ammo.caliber.AmmoEffects;
import com.tacz_caliber_ammo.caliber.AmmoProfile;
import com.tacz_caliber_ammo.caliber.CaliberManager;
import com.tacz_caliber_ammo.caliber.GunDamageModifier;
import com.tacz_caliber_ammo.platform.config.ModConfig;
import com.tacz_caliber_ammo.duck.IGravityBullet;
import com.tacz_caliber_ammo.duck.ISpeedDecayBullet;
import com.tacz_caliber_ammo.effect.AmmoEffectDispatcher;
import com.tacz_caliber_ammo.effect.AmmoEffectScriptAPI;
import com.tacz_caliber_ammo.effect.FlareEffect;
import com.tacz_caliber_ammo.platform.EventBridge;
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
public class EntityKineticBulletMixin implements ISpeedDecayBullet, IGravityBullet {

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
    @Shadow
    private Vec3 startPos;
    @Shadow
    private float distanceAmount;
    @Shadow
    private float friction;
    @Shadow
    private int life;
    /** TacZ 私有：子弹每 tick 的下坠重力（构造时由 bulletData.gravity 设定）。经 IGravityBullet 暴露给事件监听器。 */
    @Shadow
    private float gravity;

    /** TacZ 私有：对给定属性 id 应用配件修正（用于叠加 DAMAGE 配件加成）。@Shadow 存根。 */
    @Shadow
    private <T> T modifyProperty(String id, Class<T> type, T original) {
        throw new AssertionError();
    }
    // ==== TacZ 子弹的效果实例字段（构造末由 gun cacheProperty 读入；本 mixin 按弹药 effects 覆写） ====
    @Shadow
    private boolean explosion;
    @Shadow
    private float explosionDamage;
    @Shadow
    private float explosionRadius;
    @Shadow
    private boolean explosionKnockback;
    @Shadow
    private boolean explosionDestroyBlock;
    @Shadow
    private int explosionDelayCount;
    @Shadow
    private boolean igniteEntity;
    @Shadow
    private boolean igniteBlock;
    @Shadow
    private int igniteEntityTime;
    @Shadow
    private float knockback;

    /** dev-only 速度衰减测试：本发子弹初速(blocks/tick)与下一次记录的距离阈值(格)。见 taczCaliberAmmo$logSpeedDecayTick。 */
    @Unique
    private double taczCaliberAmmo$muzzleSpeed = 0.0;
    @Unique
    private double taczCaliberAmmo$nextSpeedLogAt = 0.0;

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
        AmmoEffects fx = profile.effects();
        if (fx.hasNative() && ModConfig.enableAmmoEffects()) {
            taczCaliberAmmo$applyNativeEffects(fx);
        }
        DebugLog.log("bullet<init>: ammo={} gun={} base={} gunMod={} -> finalDmg={} armorIgnore={} headShot={} pierce={} (damageModifier={} shotMult={})",
                this.ammoId, this.gunId, profile.baseDamage(),
                mod == null ? "none" : ("flat=" + mod.flatDamage() + ",pct=" + mod.percentDamage()),
                dmg, this.armorIgnore, this.headShot, this.pierce, this.damageModifier, this.shotDamageMultiplier);
        // on_fire：本发子弹生成（服务端）时派发脚本钩子（读子弹自身 getAmmoId()，精确、非持有态）。
        if (!worldIn.isClientSide()) {
            EntityKineticBullet self = (EntityKineticBullet) (Object) this;
            AmmoEffectDispatcher.dispatch(self.getAmmoId(), "on_fire",
                    () -> new AmmoEffectScriptAPI(self, null, self.position(), "on_fire"));
        }
    }

    // 发布子弹创建事件(通用, 无弹种特例): 在 TacZ 完整构造末尾(gravity/friction 已设, spawn 之前),
    // 把子弹经 BulletCreatedEvent 交给普通 Java 事件监听器定制弹道 -- mixin 只暴露(见 IGravityBullet),
    // 不判断任何弹种; 仅服务端发布, gravity 改动随 spawn 数据同步到客户端。
    @Inject(
        method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/resources/ResourceLocation;ZLcom/tacz/guns/resource/pojo/data/gun/GunData;Lcom/tacz/guns/resource/pojo/data/gun/BulletData;)V",
        at = @At("TAIL"))
    private void taczCaliberAmmo$postBulletCreated(EntityType<? extends Projectile> type, Level worldIn,
            LivingEntity throwerIn, ItemStack gunItem, ResourceLocation ammoIdArg, ResourceLocation gunIdArg,
            ResourceLocation gunDisplayId, boolean isTracerAmmo, GunData gunData, BulletData bulletData,
            CallbackInfo ci) {
        if (worldIn.isClientSide()) {
            return;
        }
        EventBridge.postBulletCreated((EntityKineticBullet) (Object) this);
    }

    /** IGravityBullet：暴露 TacZ 私有 gravity 供事件监听器读写（mixin 只暴露、不含弹种逻辑）。 */
    @Override
    public float taczCaliberAmmo$getGravity() {
        return this.gravity;
    }

    @Override
    public void taczCaliberAmmo$setGravity(float value) {
        this.gravity = value;
    }

    /** on_bullet_tick：子弹每 tick（服务端）派发脚本钩子（仅当脚本定义了该函数才真正执行，见 AmmoEffectDispatcher）。 */
    @Inject(method = "onBulletTick", at = @At("HEAD"))
    private void taczCaliberAmmo$onBulletTick(CallbackInfo ci) {
        EntityKineticBullet self = (EntityKineticBullet) (Object) this;
        if (self.level().isClientSide()) {
            return;
        }
        taczCaliberAmmo$logSpeedDecayTick(self);
        FlareEffect.tick(self);
        AmmoEffectDispatcher.dispatch(self.getAmmoId(), "on_bullet_tick",
                () -> new AmmoEffectScriptAPI(self, null, self.position(), "on_bullet_tick"));
    }

    /**
     * dev-only 速度衰减测试日志：每飞约 25 格记录一次子弹的已飞直线距离、当前速度、速度百分比(当前/初速)与 friction，
     * 便于在 runClient/runServer 里对照标定曲线（如 BC=0.3 应约 125 格 78%、250 格 54%）。
     * 仅开发环境（{@link DebugLog#ENABLED}）、且 shoot 已记录初速（本 mod 弹药 + 速度衰减开启）时输出。
     */
    @Unique
    private void taczCaliberAmmo$logSpeedDecayTick(EntityKineticBullet self) {
        if (!DebugLog.ENABLED || this.taczCaliberAmmo$muzzleSpeed <= 0.0) {
            return;
        }
        double dist = self.position().distanceTo(this.startPos);
        if (dist < this.taczCaliberAmmo$nextSpeedLogAt) {
            return;
        }
        double speed = self.getDeltaMovement().length();
        double pct = speed / this.taczCaliberAmmo$muzzleSpeed * 100.0;
        DebugLog.log("speedDecay: ammo={} dist={}blk speed={}/{} ({}%) friction={}",
                this.ammoId, String.format("%.1f", dist), String.format("%.3f", speed),
                String.format("%.3f", this.taczCaliberAmmo$muzzleSpeed), String.format("%.1f", pct),
                String.format("%.5f", this.friction));
        this.taczCaliberAmmo$nextSpeedLogAt = dist + 25.0; // 下一记录点：再飞 ~25 格
    }

    /**
     * 按弹药 {@link AmmoEffects} 覆写 TacZ 子弹的爆炸/点燃/击退实例字段（{@code null} 项保留 TacZ 默认）。
     * 只覆写子弹本地字段——爆炸由 TacZ {@code onBulletTick} 触发、点燃/击退由 {@code onHitEntity} 应用，
     * 均照常执行，无需重写。爆炸半径受 {@link ModConfig#maxExplosionRadius()} 上限；ignite/explosion 仍受
     * TacZ 自身 {@code AmmoConfig} 全局开关门控。
     */
    @Unique
    private void taczCaliberAmmo$applyNativeEffects(AmmoEffects fx) {
        AmmoEffects.ExplosionSpec ex = fx.explosion();
        if (ex != null) {
            this.explosion = ex.enabled();
            if (ex.enabled()) {
                this.explosionDamage = Math.max(ex.damage(), 0.0f);
                this.explosionRadius = Mth.clamp(ex.radius(), 0.0f, ModConfig.maxExplosionRadius());
                this.explosionKnockback = ex.knockback();
                this.explosionDestroyBlock = ex.destroyBlock();
                this.explosionDelayCount = Math.max((int) (ex.delaySeconds() * 20.0f), 1);
            }
        }
        AmmoEffects.IgniteSpec ig = fx.ignite();
        if (ig != null) {
            this.igniteEntity = ig.igniteEntity();
            this.igniteBlock = ig.igniteBlock();
            this.igniteEntityTime = Math.max(ig.entitySeconds(), 0);
        }
        if (fx.knockback() != null) {
            this.knockback = Math.max(fx.knockback(), 0.0f);
        }
        DebugLog.log("bullet<init> effects: ammo={} explosion={} igniteEntity={} igniteBlock={} knockback={}",
                this.ammoId, this.explosion, this.igniteEntity, this.igniteBlock, this.knockback);
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

    /**
     * 优势射程外距离衰减（本 mod 弹药）。TacZ {@code getDamage} 对本 mod 单段曲线的结果是"优势射程内满伤害、
     * 射程外直接 0"；此处对已配置弹药档的弹药接管：射程内满伤害，射程外按弹道系数(BC)倒数衰减——
     * {@code damage = base / (1 + (y / (BC*x)) * excess)}，excess = 命中距离 − 优势射程(distanceAmount)。
     * x = {@link ModConfig#ballisticCoefficientScale()}（BC 乘数），y = {@link ModConfig#rangeDecayRate()}（射程外速率）。
     * 极端值：BC 先钳制到 [0.01, 1.0]（BC≤0/过小 → 0.01，衰减极快但不瞬间归零；BC≥1 → 1.0，衰减最慢），
     * 既避免除零又统一语义。y=0 或射程内 → 满伤害。仍过配件 DAMAGE 修正与 {@code shotDamageMultiplier}。
     * 未配置档弹药不接管（{@code return}，走 TacZ 原 getDamage）。
     */
    @Inject(method = "getDamage(Lnet/minecraft/world/phys/Vec3;)F", at = @At("HEAD"), cancellable = true)
    private void taczCaliberAmmo$rangedDamage(Vec3 hitVec, CallbackInfoReturnable<Float> cir) {
        AmmoProfile profile = CaliberManager.getAmmoProfile(this.ammoId);
        if (profile == null || this.damageAmount == null || this.damageAmount.isEmpty()) {
            return; // 未配置弹药档 -> 保留 TacZ 原 getDamage
        }
        double base = this.damageAmount.get(0).getDamage(); // 已含本 mod gunMod 的每发伤害
        double excess = hitVec.distanceTo(this.startPos) - this.distanceAmount; // 超出优势射程的距离（格）
        double y = ModConfig.rangeDecayRate();
        double factor;
        if (excess <= 0.0 || y <= 0.0) {
            factor = 1.0; // 优势射程内、或关闭射程外衰减(y=0) -> 满伤害
        } else {
            float bc = Mth.clamp(profile.ballisticCoefficient(), 0.01f, 1.0f); // 极端值钳制到 [0.01, 1.0]
            double rate = y / (bc * ModConfig.ballisticCoefficientScale()); // y / (BC*x)
            factor = 1.0 / (1.0 + rate * excess); // 倒数衰减
        }
        float damaged = (float) (base * factor) * this.damageModifier;
        float modified = this.modifyProperty(GunProperties.DAMAGE.name(), Float.class, damaged);
        float result = Math.max(modified * this.shotDamageMultiplier, 0.0f);
        DebugLog.log("getDamage(ranged): ammo={} gun={} excess={} bc={} factor={} base={} -> {}",
                this.ammoId, this.gunId, String.format("%.1f", excess), profile.ballisticCoefficient(),
                String.format("%.3f", factor), base, result);
        cir.setReturnValue(result);
    }

    /**
     * 速度衰减初始化（本 mod 弹药）。由 {@code AbstractGunItemMixin.doBulletSpread}（子弹初速度已设、spawn 之前）
     * 经 {@link ISpeedDecayBullet} 回调。按弹道系数(BC)设 TacZ bullet friction：每 tick {@code v*=(1-friction)}，
     * 累积得速度随距离近似线性衰减 {@code 速度% = 1 - (k/BC)*已飞格数}（令 friction=slope*v0 使斜率 k/BC 与初速无关）。
     * k = {@link ModConfig#speedDecayScale()}；BC 钳制 [0.01,1.0]；friction 再钳到 [0,0.99] 防瞬停。
     * 在 spawn 前设定 → friction 随 spawn 数据同步到客户端（两端一致）。未配置档/关闭 → 保留 TacZ 原 friction。
     */
    @Override
    public void taczCaliberAmmo$initSpeedDecay() {
        if (!ModConfig.enableSpeedDecay()) {
            return;
        }
        AmmoProfile profile = CaliberManager.getAmmoProfile(this.ammoId);
        if (profile == null) {
            return; // 未配置弹药档 -> 保留 TacZ 原 friction
        }
        double v0 = ((EntityKineticBullet) (Object) this).getDeltaMovement().length();
        if (v0 <= 1.0e-6) {
            return; // 无初速（异常）跳过
        }
        float bc = Mth.clamp(profile.ballisticCoefficient(), 0.01f, 1.0f); // 极端值钳制到 [0.01, 1.0]
        double slope = ModConfig.speedDecayScale() / bc;   // 每格速度衰减比例 = k / BC
        double f = slope * v0;                              // friction = slope * v0 -> 速度% = 1 - slope*距离（与初速无关）
        this.friction = (float) Mth.clamp(f, 0.0, 0.99);
        this.taczCaliberAmmo$muzzleSpeed = v0;      // dev 测试：记录初速，供 onBulletTick 算速度%
        this.taczCaliberAmmo$nextSpeedLogAt = 0.0;  // 从 0 格起按 ~25 格间隔记录飞行速度
        // 寿命按初速反比延长：初速越慢（如火箭）飞行时间越长 -> life 放大越多，避免高抛物线弹落地前被 life 剔除。
        // life *= clamp(refSpeed / v0, 1, maxMult)：v0>=refSpeed 不延长，越慢延长越多、封顶 maxMult。
        double lifeMult = Mth.clamp(ModConfig.speedDecayLifeRefSpeed() / v0, 1.0, ModConfig.speedDecayLifeMaxMult());
        int oldLife = this.life;
        this.life = (int) Math.min((double) this.life * lifeMult, Integer.MAX_VALUE);
        DebugLog.log("initSpeedDecay: ammo={} gun={} v0={} bc={} slope={} -> friction={} life={}*{}={}",
                this.ammoId, this.gunId, String.format("%.3f", v0), profile.ballisticCoefficient(),
                String.format("%.5f", slope), this.friction, oldLife, String.format("%.2f", lifeMult), this.life);
    }

    @Inject(method = "getDamage(Lnet/minecraft/world/phys/Vec3;)F", at = @At("RETURN"))
    private void taczCaliberAmmo$logDamage(Vec3 hitVec, CallbackInfoReturnable<Float> cir) {
        DebugLog.log("getDamage: ammo={} gun={} -> {} (damageAmount[0]={} damageModifier={} shotMult={})",
                this.ammoId, this.gunId, cir.getReturnValue(),
                this.damageAmount.isEmpty() ? "-" : this.damageAmount.get(0).getDamage(),
                this.damageModifier, this.shotDamageMultiplier);
    }
}
