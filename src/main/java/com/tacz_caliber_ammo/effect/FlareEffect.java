package com.tacz_caliber_ammo.effect;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.joml.Vector3f;

import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz_caliber_ammo.duck.IGravityBullet;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * 信号弹（26x75mm 照明弹）照明效果 —— 纯 Java 实现，不依赖 Lua 脚本或数据绑定。
 *
 * <p>发光与子弹实体解耦：EntityKineticBulletMixin.onBulletTick 每 tick 调 tick()，把点亮后的信号弹登记为
 * 一个「发光点」并让其跟随子弹飞行；实际照明帧由 serverTick() 统一发出（AmmoEffectEvents 监听 ServerTickEvent）。
 * 子弹消失后（空中寿命到期或落地命中），发光点在最后位置继续驻留发光 LINGER_TICKS tick，模拟真实信号弹
 * 升空/落地后的持续燃烧照明（而非烟花般一次爆闪）。仅对本项目 26_75 口径生效。
 */
public final class FlareEffect {

    private static final String NAMESPACE = "tacz_caliber_ammo";
    /** 只处理本项目信号弹口径（ammoId path 形如 {@code 26_75/green}）。 */
    private static final String CALIBER_PREFIX = "26_75/";
    /** 飞行多少 tick 后点亮（20 tick = 1 秒）。 */
    private static final int IGNITE_TICK = 30;
    /** 信号弹重力系数。 */
    private static final float GRAVITY_FACTOR = 0.15f;
    /** 子弹消失后（空中寿命到期 / 落地命中），在最后位置继续驻留发光的 tick 数（600 = 30 秒）。 */
    private static final int LINGER_TICKS = 600;

    /** 弹种 model -> 照明颜色 RGB(0~1)。 */
    private static final Map<String, float[]> COLORS = Map.of(
            "green", new float[] {0.15f, 1.0f, 0.2f},
            "red", new float[] {1.0f, 0.12f, 0.12f},
            "white", new float[] {1.0f, 1.0f, 1.0f},
            "yellow", new float[] {1.0f, 0.85f, 0.1f},
            "blue", new float[] {0.2f, 0.45f, 1.0f},
            "acid_green", new float[] {0.6f, 1.0f, 0.05f});
    private static final float[] DEFAULT_COLOR = {1.0f, 1.0f, 1.0f};
    /** 新年弹：多彩循环色板。 */
    private static final float[][] FESTIVE = {
            {1.0f, 0.15f, 0.15f}, {1.0f, 0.85f, 0.1f}, {0.15f, 1.0f, 0.2f},
            {0.2f, 0.45f, 1.0f}, {1.0f, 1.0f, 1.0f}, {1.0f, 0.3f, 0.75f}};

    /**
     * 活跃信号弹发光点：entityId -> 点。子弹活着时每 tick 由 {@code tick()} 刷新位置并把 ttl 重置为满，
     * 故发光跟随子弹飞行；子弹消失后不再刷新，ttl 在 {@code serverTick()} 里递减到 0 前，在最后位置持续发光。
     * 仅服务端主线程访问。
     */
    private static final Map<Integer, FlarePoint> POINTS = new HashMap<>();

    /** 一个信号弹发光点（可变）：位置、弹种色、剩余发光 ttl、已发光 age（驱动 new_year 循环与粒子节奏）。 */
    private static final class FlarePoint {
        final ServerLevel level;
        double x;
        double y;
        double z;
        final String model;
        int ttl;
        int age;

        FlarePoint(ServerLevel level, double x, double y, double z, String model) {
            this.level = level;
            this.x = x;
            this.y = y;
            this.z = z;
            this.model = model;
            this.ttl = LINGER_TICKS;
            this.age = 0;
        }
    }

    private FlareEffect() {
    }

    /** 是否本项目信号弹（26x75mm 照明弹）弹种。供飞行照明与弹道定制复用。 */
    public static boolean isFlare(ResourceLocation ammoId) {
        return ammoId != null && NAMESPACE.equals(ammoId.getNamespace())
                && ammoId.getPath().startsWith(CALIBER_PREFIX);
    }

    /**
     * 子弹创建事件回调（服务端）：信号弹降低重力，升空更高、滞空更久（照明弹特性）。
     * 由 AmmoEffectEvents 监听 BulletCreatedEvent 转发；通过 IGravityBullet（mixin 暴露）
     * 读写 TacZ 私有 gravity，本类不碰 mixin 内部、mixin 也不含弹种逻辑。
     */
    public static void onBulletCreated(EntityKineticBullet bullet) {
        if (!isFlare(bullet.getAmmoId())) {
            return;
        }
        IGravityBullet g = (IGravityBullet) bullet;
        g.taczCaliberAmmo$setGravity(g.taczCaliberAmmo$getGravity() * GRAVITY_FACTOR);
    }

    /** 弹种 model + 已发光 age -> 照明颜色（new_year 循环，其余固定色）。 */
    private static float[] colorFor(String model, int age) {
        return "new_year".equals(model) ? FESTIVE[(age / 3) % FESTIVE.length]
                : COLORS.getOrDefault(model, DEFAULT_COLOR);
    }

    /**
     * 子弹每 tick（服务端）调用：点亮后把该子弹登记/刷新为一个发光点（位置跟随子弹、ttl 重置为满），
     * 首次登记时播放点亮爆发。实际发光帧由 serverTick 统一发出，故子弹消失后发光点仍在最后位置
     * 驻留至 ttl 耗尽——空中寿命到期与落地命中都能继续发光。
     */
    public static void tick(EntityKineticBullet bullet) {
        ResourceLocation ammoId = bullet.getAmmoId();
        if (!isFlare(ammoId)) {
            return;
        }
        if (!(bullet.level() instanceof ServerLevel level)) {
            return;
        }
        if (bullet.tickCount < IGNITE_TICK) {
            return; // 未到点亮
        }
        String model = ammoId.getPath().substring(CALIBER_PREFIX.length());
        Vec3 p = bullet.position();
        FlarePoint fp = POINTS.get(bullet.getId());
        if (fp == null) {
            fp = new FlarePoint(level, p.x, p.y, p.z, model);
            POINTS.put(bullet.getId(), fp);
            emitIgnite(fp); // 点亮瞬间：彩色爆发 + 白闪 + 音效
        } else {
            fp.x = p.x;
            fp.y = p.y;
            fp.z = p.z; // 发光点跟随子弹飞行
        }
        fp.ttl = LINGER_TICKS; // 子弹活着 -> ttl 保持满，不过期
    }

    /**
     * 每服务端 tick（ServerTickEvent.END）调用：所有登记的信号弹发光点各发一帧照明，age 递增、
     * ttl 递减，ttl 耗尽即移除。子弹活着时 tick 每帧把 ttl 重置为满，发光跟随子弹；子弹消失后不再
     * 重置，发光点在最后位置驻留 LINGER_TICKS 后熄灭。
     */
    public static void serverTick() {
        if (POINTS.isEmpty()) {
            return;
        }
        Iterator<FlarePoint> it = POINTS.values().iterator();
        while (it.hasNext()) {
            FlarePoint fp = it.next();
            emitBurn(fp);
            fp.age++;
            if (--fp.ttl <= 0) {
                it.remove();
            }
        }
    }

    /** 点亮瞬间：彩色爆发 + 白闪 + 音效。 */
    private static void emitIgnite(FlarePoint fp) {
        float[] c = colorFor(fp.model, 0);
        DustParticleOptions core = new DustParticleOptions(new Vector3f(c[0], c[1], c[2]), 1.6f);
        force(fp, core, fp.x, fp.y, fp.z, 45, 0.6, 0.6, 0.6, 0.05);
        force(fp, ParticleTypes.FIREWORK, fp.x, fp.y, fp.z, 20, 0.4, 0.4, 0.4, 0.1);
        fp.level.playSound(null, fp.x, fp.y, fp.z, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.AMBIENT, 2.5f, 1.1f);
    }

    /** 持续照明一帧：彩色核心 + 光晕（向上飘）+ 火花 + 拖尾烟。 */
    private static void emitBurn(FlarePoint fp) {
        float[] c = colorFor(fp.model, fp.age);
        DustParticleOptions core = new DustParticleOptions(new Vector3f(c[0], c[1], c[2]), 1.6f);
        DustParticleOptions halo = new DustParticleOptions(new Vector3f(c[0], c[1], c[2]), 3.0f);
        forceUp(fp, core, 6);
        forceUp(fp, halo, 2);
        if ((fp.age & 1) == 0) {
            force(fp, ParticleTypes.SMALL_FLAME, fp.x, fp.y, fp.z, 2, 0.2, 0.2, 0.2, 0.03);
            force(fp, ParticleTypes.FIREWORK, fp.x, fp.y, fp.z, 4, 0.4, 0.4, 0.4, 0.1);
        }
        if (fp.age % 5 == 0) {
            force(fp, ParticleTypes.LARGE_SMOKE, fp.x, fp.y, fp.z, 2, 0.25, 0.25, 0.25, 0.03);
        }
    }

    /**
     * 强制发送粒子（overrideLimiter=true）：遍历该维度所有玩家，逐个用带 force 的 sendParticles 发送，
     * 使粒子在远距离（force 下最远 512 格）也强制生成——1.20.1 的 ServerLevel 无「广播版」force 重载，
     * 只有 sendParticles(ServerPlayer, type, force, ...)，故手动遍历 players()。信号弹是远程照明信号，需强制可见。
     */
    private static void force(FlarePoint fp, ParticleOptions type, double x, double y, double z,
            int count, double dx, double dy, double dz, double speed) {
        for (ServerPlayer p : fp.level.players()) {
            fp.level.sendParticles(p, type, true, x, y, z, count, dx, dy, dz, speed);
        }
    }

    /**
     * 向上飘的有色粒子：逐个用 count=0 定向模式发送（此时 xd/yd/zd 当作速度向量、speed 为倍率），
     * 垂直向上速度（0.6~1.2）明显大于水平微动（±0.06），模拟照明烟光向上升腾。
     * 位置在中心附近随机散开（水平 ±0.3、垂直 ±0.2）。仍走 force（overrideLimiter=true）保证远距离可见。
     */
    private static void forceUp(FlarePoint fp, ParticleOptions type, int count) {
        for (int i = 0; i < count; i++) {
            double px = fp.x + (Math.random() - 0.5) * 0.6;
            double py = fp.y + (Math.random() - 0.5) * 0.4;
            double pz = fp.z + (Math.random() - 0.5) * 0.6;
            double vx = (Math.random() - 0.5) * 0.12;
            double vy = 0.6 + Math.random() * 0.6;
            double vz = (Math.random() - 0.5) * 0.12;
            for (ServerPlayer p : fp.level.players()) {
                fp.level.sendParticles(p, type, true, px, py, pz, 0, vx, vy, vz, 1.0);
            }
        }
    }
}
