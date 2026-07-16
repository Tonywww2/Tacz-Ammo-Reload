package com.tacz_caliber_ammo.caliber;

import net.minecraft.resources.ResourceLocation;

/**
 * 弹药的"命中/触发效果"配置（来自弹药 JSON 的可选 {@code effects{}} 子块）。
 *
 * <p>分两类：
 * <ul>
 *   <li><b>声明式原生效果</b>（Phase 1）：{@link #explosion}/{@link #ignite}/{@link #knockback}——
 *       由 {@code EntityKineticBulletMixin} 在子弹构造末按 ammoId 覆写 TacZ 子弹实例字段，
 *       之后 TacZ 原生逻辑（onBulletTick/onHitEntity）照常应用。每一项为 {@code null} 表示"不覆写、保留 TacZ 默认"。</li>
 *   <li><b>Lua 脚本</b>（Phase 2）：{@link #script} 指向一段自定义效果脚本（{@code data/<ns>/ammo_effect_scripts/*.lua}），
 *       在命中/击杀/tick/开火等时机派发。</li>
 * </ul>
 */
public record AmmoEffects(ExplosionSpec explosion, IgniteSpec ignite, Float knockback, ResourceLocation script) {

    /** 空配置：无任何效果（不覆写子弹字段、无脚本）。 */
    public static final AmmoEffects EMPTY = new AmmoEffects(null, null, null, null);

    /** 是否含需要覆写子弹字段的声明式原生效果。 */
    public boolean hasNative() {
        return explosion != null || ignite != null || knockback != null;
    }

    /** 是否绑定了效果脚本。 */
    public boolean hasScript() {
        return script != null;
    }

    /**
     * 爆炸配置。{@code delaySeconds} 为引信延迟（秒，0=命中即炸）；{@code enabled=false} 可显式关闭
     * 该弹的爆炸（即便枪/配件带爆炸）。
     */
    public record ExplosionSpec(boolean enabled, float damage, float radius,
                                boolean knockback, boolean destroyBlock, float delaySeconds) {
    }

    /** 点燃配置。{@code entitySeconds} 为点燃实体的持续秒数。 */
    public record IgniteSpec(boolean igniteEntity, boolean igniteBlock, int entitySeconds) {
    }
}
