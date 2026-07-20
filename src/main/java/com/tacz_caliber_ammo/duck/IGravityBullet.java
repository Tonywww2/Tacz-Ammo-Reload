package com.tacz_caliber_ammo.duck;

/**
 * 由 EntityKineticBulletMixin 实现（Mixin duck-typing 加到 TacZ EntityKineticBullet 上），
 * 把 TacZ 私有 {@code gravity} 字段暴露给普通 Java 代码（事件监听器）读写。
 *
 * <p>设计意图：mixin 只负责「暴露」——不在 mixin 里写任何弹种特例。哪些弹药、如何调整重力，
 * 全部由监听 {@link com.tacz_caliber_ammo.event.BulletCreatedEvent} 的普通 Java 代码决定
 * （见 {@code FlareEffect#onBulletCreated}）。
 *
 * <p>注意：必须放在 mixin 包（com.tacz_caliber_ammo.mixin）之外——mixin 包内的类会被 Mixin 接管、
 * 不能被运行时代码直接引用（同 {@link ISpeedDecayBullet}）。
 */
public interface IGravityBullet {

    /** 读取 TacZ 子弹当前每 tick 下坠重力（构造时由 {@code bulletData.gravity} 设定）。 */
    float taczCaliberAmmo$getGravity();

    /**
     * 覆写 TacZ 子弹重力。在服务端子弹构造末尾（spawn 之前）调用，
     * 随 {@code writeSpawnData} 同步到客户端，两端一致。
     */
    void taczCaliberAmmo$setGravity(float gravity);
}
