# 弹药效果脚本（Lua）

整合包作者可以用 Lua 脚本给弹药实现**任意自定义命中效果**——和 TacZ 本体写枪械脚本一样的体验。
纯数据的爆炸/点燃/击退见 `ammo-effects.md`；本页讲脚本。

## 绑定脚本

在弹药 JSON 的 `effects` 子块里写 `script`：

```json
"effects": { "script": "tacz_caliber_ammo:incendiary" }
```

- 只写名字（无命名空间）时默认命名空间 `tacz_caliber_ammo`（即 `"incendiary"` = `"tacz_caliber_ammo:incendiary"`）。
- 脚本文件放在：`data/<命名空间>/ammo_effect_scripts/<名字>.lua`。
- 随数据包 reload 生效（`/reload` 或重进世界）。

## 脚本结构

每个 `.lua` 返回一张表，表里定义"钩子函数"；到对应时机就会被调用，参数是 `api`：

```lua
local M = {}

function M.on_hit_entity(api)
    api:ignite(5)
end

return M
```

只需定义你要用的钩子（未定义的不会被调用，也不产生开销）。

## 钩子

| 钩子 | 何时触发 | 有目标 `hasTarget` | 备注 |
|---|---|---|---|
| `on_fire` | 每发子弹生成时 | 否 | 开火瞬间；霰弹按每颗弹丸各调一次 |
| `on_bullet_tick` | 子弹飞行每 tick | 否 | **高频！请自行节流** |
| `on_hit_entity` | 命中实体 | 是 | |
| `on_hit_block` | 命中方块 | 否 | 有命中坐标 |
| `on_kill` | 用该弹击杀实体 | 是 | 目标 = 被击杀者 |

全部在**服务端**调用。弹种 id 由子弹自身精确取得（`api:getAmmoId()`）。

## api 参考

**上下文查询**

| 方法 | 返回 | 说明 |
|---|---|---|
| `api:getAmmoId()` | string | 当前弹种 id，如 `tacz_caliber_ammo:5_56x45/m855` |
| `api:getHook()` | string | 当前钩子名 |
| `api:hasTarget()` | bool | 是否有命中/目标实体 |
| `api:hasShooter()` | bool | 是否有射手 |
| `api:getX()` / `getY()` / `getZ()` | number | 命中/子弹坐标 |
| `api:getTargetHealth()` | number | 目标当前生命值（无目标为 0） |
| `api:getAge()` | number | 子弹已飞行的游戏刻（tick，20 = 1 秒）；配合 `on_bullet_tick` 做飞行 N tick 后触发 |

**效果助手**（都有空值保护：无目标/无射手时对应助手自动无效）

| 方法 | 说明 |
|---|---|
| `api:ignite(seconds)` | 点燃目标 |
| `api:addEffect(effectId, durationTicks, amplifier)` | 给目标施加药水效果（时长受 `maxEffectSeconds` 上限） |
| `api:damageTarget(amount)` | 对目标追加伤害 |
| `api:healShooter(amount)` | 治疗射手 |
| `api:knockback(strength)` | 把目标从子弹处击退 |
| `api:pull(strength)` | 把目标朝射手方向拉拽 |
| `api:explode(radius, fire, destroyBlock)` | 命中处爆炸（**范围效果**，会波及附近实体含射手；半径受 `maxExplosionRadius` 上限；伤害随半径） |
| `api:strikeLightning()` | 命中处降下闪电（**范围效果**，会点燃/波及附近实体含射手） |
| `api:particle(particleId, count, spread)` | 生成简单粒子（如 `minecraft:flame`） |
| `api:sound(soundId, volume, pitch)` | 播放音效（如 `minecraft:entity.generic.explode`） |
| `api:log(message)` | 调试日志 |

## 沙箱与安全

- **沙箱**：脚本运行在受限 luaj 环境（base/package/table/string/math/bit32），**没有 `io`/`os`**——不能读写文件或执行系统命令。
- **服务端**：所有效果均服务端结算。
- **范围效果会波及附近实体（含射手/玩家）**：`api:explode` 与 `api:strikeLightning` 作用于命中点周围——目标离射手很近时，**射手/玩家自己也可能被点燃或波及**（原版行为，非 bug）。范围类效果请谨慎使用，或用 `api:getX/Y/Z` 判断距离后再触发。相比之下 `ignite`/`addEffect`/`damageTarget`/`knockback`/`pull` 只作用于命中目标、`healShooter` 只作用于射手，均安全。
- **出错兜底**：脚本任何异常（含语法错误、空值、乃至无限递归的栈溢出）都会被捕获并记入日志（搜索 `effect script ... failed`），**绝不会崩游戏**。
- **总开关/上限**：`config/tacz_caliber_ammo-common.toml` 的 `[ammo_effects]` 段：`enableAmmoEffects`、`maxExplosionRadius`、`maxEffectSeconds`。
- **`on_bullet_tick` 每 tick、每颗子弹都调用**——如果做重活，请在脚本里自行节流（例如只在满足某条件时才执行）。

## 自带示例脚本

本 mod 内置以下示例（默认不绑定任何弹药，写进某弹药的 `effects.script` 即可启用）：

| 脚本 id | 效果 |
|---|---|
| `tacz_caliber_ammo:incendiary` | 命中点燃 + 发光 |
| `tacz_caliber_ammo:poison` | 命中中毒 |
| `tacz_caliber_ammo:explosive` | 命中实体/方块时小爆炸 |
| `tacz_caliber_ammo:vampire` | 击杀回血 + 音效 |
| `tacz_caliber_ammo:tracer_spark` | 飞行途中火花粒子 |

绑定示例（写进某弹药的 `data/<ns>/index/ammo/<口径>/<型号>.json`）：

```json
"effects": { "script": "tacz_caliber_ammo:incendiary" }
```

一个弹药可**同时**用声明式效果与脚本：

```json
"effects": {
  "explosion": { "enabled": true, "radius": 3 },
  "script": "mypack:my_effect"
}
```
