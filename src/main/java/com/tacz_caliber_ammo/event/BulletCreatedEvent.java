package com.tacz_caliber_ammo.event;

import com.tacz.guns.entity.EntityKineticBullet;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.Event;

/**
 * 本 mod 自定义 Forge 事件：TacZ 子弹 {@link EntityKineticBullet} 在服务端「完整构造」末尾
 * （{@code gravity}/{@code friction} 已由 bulletData 设好、spawn 之前）触发，由
 * {@code EntityKineticBulletMixin} 在 {@code <init>} TAIL 发布到 {@code MinecraftForge.EVENT_BUS}。
 *
 * <p>目的：把「按弹种定制子弹弹道」的业务从 mixin 里剥离到普通 Java 事件监听器——mixin 只暴露
 * （通过 {@link com.tacz_caliber_ammo.duck.IGravityBullet} 提供 gravity 读写）并发布本事件，
 * 监听器（{@code AmmoEffectEvents#onBulletCreated} -> {@code FlareEffect#onBulletCreated}）再
 * 判断弹种、调整重力，mixin 内不含任何弹种特例。
 *
 * <p>仅服务端发布；对 gravity 的修改随子弹 spawn 数据同步到客户端。
 */
public class BulletCreatedEvent extends Event {

    private final EntityKineticBullet bullet;

    public BulletCreatedEvent(EntityKineticBullet bullet) {
        this.bullet = bullet;
    }

    /** 刚创建的 TacZ 子弹实体。可强转为 {@link com.tacz_caliber_ammo.duck.IGravityBullet} 读写重力。 */
    public EntityKineticBullet getBullet() {
        return bullet;
    }

    /** 弹种 id（{@code bullet.getAmmoId()} 便捷访问）。 */
    public ResourceLocation getAmmoId() {
        return bullet.getAmmoId();
    }
}
