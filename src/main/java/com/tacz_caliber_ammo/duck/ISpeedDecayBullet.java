package com.tacz_caliber_ammo.duck;

/**
 * 由 EntityKineticBulletMixin 实现（Mixin duck-typing 加到 TacZ EntityKineticBullet 上），
 * 供 AbstractGunItemMixin.doBulletSpread 在子弹初速度已设、spawn 之前回调，
 * 按弹道系数设置飞行 friction（速度衰减）。放在 spawn 之前设，故 friction 会随 spawn 数据同步到客户端。
 *
 * <p>注意：必须放在 mixin 包（com.tacz_caliber_ammo.mixin）之外——mixin 包内的类会被 Mixin 接管、
 * 不能被运行时代码直接引用，否则 TacZ 类加载时抛 IllegalClassLoadError。
 */
public interface ISpeedDecayBullet {

    /** 在子弹初速度已设、spawn 之前调用：按弹道系数设定飞行 friction，使子弹速度随距离衰减且两端一致。 */
    void taczCaliberAmmo$initSpeedDecay();
}
