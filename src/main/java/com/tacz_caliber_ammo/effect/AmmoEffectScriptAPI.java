package com.tacz_caliber_ammo.effect;

import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz_caliber_ammo.platform.PlatformEffects;
import com.tacz_caliber_ammo.platform.PlatformEntity;
import com.tacz_caliber_ammo.platform.config.ModConfig;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 传给弹药效果 Lua 脚本的 api 对象（每次派发新建，经 {@code CoerceJavaToLua.coerce} 传入，脚本用 {@code api:xxx()} 调用）。
 *
 * <p>全部效果**服务端结算**（本对象仅在服务端派发时构建）。上下文按钩子不同而部分为空：
 * on_hit_entity/on_kill 有 target；on_hit_block 有命中坐标但无 target；on_fire/on_bullet_tick 无 target。
 * 所有助手都做空值/合法性保护，脚本写错不抛异常到游戏主循环（另见 {@link AmmoEffectDispatcher} 的 try/catch）。
 */
public final class AmmoEffectScriptAPI {

    private final EntityKineticBullet bullet;
    private final ServerLevel level;
    private final LivingEntity shooter;
    private final LivingEntity target;
    private final Vec3 pos;
    private final String hook;
    private final ResourceLocation ammoId;

    public AmmoEffectScriptAPI(EntityKineticBullet bullet, LivingEntity target, Vec3 pos, String hook) {
        this.bullet = bullet;
        this.target = target;
        this.hook = hook;
        this.pos = (pos != null) ? pos : bullet.position();
        this.level = (bullet.level() instanceof ServerLevel sl) ? sl : null;
        this.shooter = (bullet.getOwner() instanceof LivingEntity le) ? le : null;
        this.ammoId = bullet.getAmmoId();
    }

    // ==== 上下文查询（供脚本读取） ====

    /** 当前弹种 id（如 {@code tacz_caliber_ammo:5_56x45/m855}）。 */
    public String getAmmoId() {
        return ammoId == null ? "" : ammoId.toString();
    }

    /** 当前钩子名（on_hit_entity / on_hit_block / on_bullet_tick / on_fire / on_kill）。 */
    public String getHook() {
        return hook;
    }

    /** 是否存在命中/目标实体（on_hit_entity / on_kill 为 true）。 */
    public boolean hasTarget() {
        return target != null;
    }

    /** 是否存在射手。 */
    public boolean hasShooter() {
        return shooter != null;
    }

    public double getX() {
        return pos.x;
    }

    public double getY() {
        return pos.y;
    }

    public double getZ() {
        return pos.z;
    }

    /** 目标当前生命值（无目标返回 0）。 */
    public double getTargetHealth() {
        return target == null ? 0.0 : target.getHealth();
    }

    /** 子弹已飞行的游戏刻（tick，20 tick = 1 秒）；用于 on_bullet_tick 里按飞行时长触发效果。 */
    public int getAge() {
        return bullet.tickCount;
    }

    // ==== 效果助手（服务端） ====

    /** 点燃目标 {@code seconds} 秒（无目标则无效）。 */
    public void ignite(int seconds) {
        if (target != null && seconds > 0) {
            PlatformEntity.ignite(target, seconds);
        }
    }

    /** 给目标施加药水效果（{@code effectId} 如 {@code minecraft:poison}）。时长受 config maxEffectSeconds 上限。 */
    public void addEffect(String effectId, int durationTicks, int amplifier) {
        if (target == null) {
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(effectId);
        if (id == null) {
            return;
        }
        int cap = ModConfig.maxEffectSeconds() * 20;
        int dur = Mth.clamp(durationTicks, 0, cap);
        MobEffectInstance instance = PlatformEffects.createInstance(id, dur, Math.max(amplifier, 0));
        if (instance != null) {
            target.addEffect(instance);
        }
    }

    /** 对目标追加伤害（在弹药本体伤害之外，无目标则无效）。 */
    public void damageTarget(double amount) {
        if (target != null && level != null && amount > 0) {
            target.hurt(level.damageSources().generic(), (float) amount);
        }
    }

    /** 治疗射手（无射手则无效）。 */
    public void healShooter(double amount) {
        if (shooter != null && amount > 0) {
            shooter.heal((float) amount);
        }
    }

    /** 把目标从子弹处击退（无目标则无效）。 */
    public void knockback(double strength) {
        if (target != null && strength > 0) {
            target.knockback((float) strength, pos.x - target.getX(), pos.z - target.getZ());
            target.hurtMarked = true;
        }
    }

    /** 把目标朝射手（无射手则朝子弹）方向拉拽（无目标则无效）。 */
    public void pull(double strength) {
        if (target == null || strength <= 0) {
            return;
        }
        Vec3 toward = (shooter != null ? shooter.position() : pos).subtract(target.position());
        if (toward.lengthSqr() < 1.0e-4) {
            return;
        }
        Vec3 dir = toward.normalize();
        target.push(dir.x * strength, dir.y * strength * 0.5, dir.z * strength);
        target.hurtMarked = true;
    }

    /** 在命中处引发爆炸（半径受 config maxExplosionRadius 上限；伤害由半径决定，同原版）。 */
    public void explode(double radius, boolean fire, boolean destroyBlock) {
        if (level == null) {
            return;
        }
        float r = Mth.clamp((float) radius, 0.0f, ModConfig.maxExplosionRadius());
        level.explode(bullet, pos.x, pos.y, pos.z, r, fire,
                destroyBlock ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
    }

    /** 在命中处降下闪电。 */
    public void strikeLightning() {
        if (level == null) {
            return;
        }
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(pos.x, pos.y, pos.z);
            level.addFreshEntity(bolt);
        }
    }

    /** 在命中处生成粒子（{@code particleId} 需是简单粒子，如 {@code minecraft:flame}）。 */
    public void particle(String particleId, int count, double spread) {
        if (level == null) {
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(particleId);
        if (id == null) {
            return;
        }
        ParticleType<?> type = PlatformEffects.particle(id);
        if (type instanceof SimpleParticleType simple) {
            level.sendParticles(simple, pos.x, pos.y, pos.z, Math.max(count, 0), spread, spread, spread, 0.0);
        }
    }

    /** 在命中处播放音效（{@code soundId} 如 {@code minecraft:entity.generic.explode}）。 */
    public void sound(String soundId, double volume, double pitch) {
        if (level == null) {
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(soundId);
        if (id == null) {
            return;
        }
        SoundEvent se = PlatformEffects.sound(id);
        if (se != null) {
            level.playSound(null, pos.x, pos.y, pos.z, se, SoundSource.PLAYERS, (float) volume, (float) pitch);
        }
    }

    /** 调试日志（便于脚本作者排错）。 */
    public void log(String message) {
        AmmoEffectDispatcher.LOGGER.info("[ammo-effect-script] {} ({}): {}", getAmmoId(), hook, message);
    }
}
