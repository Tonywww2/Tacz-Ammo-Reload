package com.tacz_caliber_ammo.effect;

import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.api.event.common.EntityKillByGunEvent;
import com.tacz.guns.api.event.server.AmmoHitBlockEvent;
import com.tacz.guns.entity.EntityKineticBullet;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 弹药效果脚本的事件钩子（Forge 总线，服务端）：命中实体 / 击杀 / 命中方块。
 *
 * <p>三者都从 TacZ 事件拿到子弹实体，用 {@link EntityKineticBullet#getAmmoId()} 精确取弹种（非持有态），
 * 再派发到该弹绑定的 Lua 脚本。逐 tick 与开火钩子在 {@code EntityKineticBulletMixin} 里挂。
 * 遵循 §2 自注册约定（@Mod.EventBusSubscriber 自动发现，主类不改）。
 */
@Mod.EventBusSubscriber(modid = "tacz_caliber_ammo", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AmmoEffectEvents {

    private AmmoEffectEvents() {
    }

    @SubscribeEvent
    public static void onEntityHurt(EntityHurtByGunEvent.Post event) {
        if (!(event.getBullet() instanceof EntityKineticBullet bullet) || bullet.level().isClientSide()) {
            return;
        }
        LivingEntity target = event.getHurtEntity() instanceof LivingEntity le ? le : null;
        AmmoEffectDispatcher.dispatch(bullet.getAmmoId(), "on_hit_entity",
                () -> new AmmoEffectScriptAPI(bullet, target, bullet.position(), "on_hit_entity"));
    }

    @SubscribeEvent
    public static void onEntityKill(EntityKillByGunEvent event) {
        if (!(event.getBullet() instanceof EntityKineticBullet bullet) || bullet.level().isClientSide()) {
            return;
        }
        LivingEntity target = event.getKilledEntity();
        AmmoEffectDispatcher.dispatch(bullet.getAmmoId(), "on_kill",
                () -> new AmmoEffectScriptAPI(bullet, target, bullet.position(), "on_kill"));
    }

    @SubscribeEvent
    public static void onHitBlock(AmmoHitBlockEvent event) {
        EntityKineticBullet bullet = event.getAmmo();
        if (bullet == null || bullet.level().isClientSide()) {
            return;
        }
        Vec3 pos = event.getHitResult().getLocation();
        AmmoEffectDispatcher.dispatch(bullet.getAmmoId(), "on_hit_block",
                () -> new AmmoEffectScriptAPI(bullet, null, pos, "on_hit_block"));
    }
}
