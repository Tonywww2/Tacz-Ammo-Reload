# 弹药效果（声明式）

弹药可以在自己的 JSON 里声明"命中/触发时产生的效果"。本页讲**声明式原生效果**（改数据即可，无需脚本）。
需要任意自定义逻辑（药水、闪电、追踪等）见 `ammo-effect-scripting.md`（Lua 脚本，Phase 2）。

目前支持三类原生效果：**explosion（爆炸）**、**ignite（点燃）**、**knockback（额外击退）**。

## 写在哪里

效果写在弹药索引 JSON 的可选 `effects{}` 子块：

    data/<命名空间>/index/ammo/<口径id>/<型号id>.json

仅对**已配置口径**的弹药生效（即该 JSON 含 `caliber` 字段的弹药；未配置口径的原版弹药不受影响）。
效果按"枪 → 口径 → 弹种"绑定在弹药上：同一把枪换不同弹，效果随弹变化。

## 结构

```json
{
  "name": "ammo.<ns>.<cal>.<model>",
  "caliber": "5_56x45",
  "baseDamage": 8,
  "effects": {
    "explosion": {
      "enabled": true,
      "damage": 6.0,
      "radius": 3.0,
      "knockback": true,
      "destroyBlock": false,
      "delaySeconds": 0.0
    },
    "ignite": {
      "entity": true,
      "block": false,
      "entitySeconds": 5
    },
    "knockback": 1.5
  }
}
```

三个子块都是**可选**的：省略某一项就"不覆写、保留 TacZ 原生行为"。

### explosion（爆炸）

| 字段 | 类型 | 默认 | 说明 |
|---|---|---|---|
| enabled | bool | true | 是否爆炸；写 `false` 可显式关闭该弹爆炸 |
| damage | float | 3.0 | 爆炸伤害 |
| radius | float | 3.0 | 爆炸半径（受配置 `maxExplosionRadius` 上限裁剪） |
| knockback | bool | false | 爆炸是否产生击退 |
| destroyBlock | bool | false | 是否破坏方块（另受 TacZ `AmmoConfig.EXPLOSIVE_AMMO_DESTROYS_BLOCK` 门控） |
| delaySeconds | float | 0.0 | 引信延迟（秒），0 = 命中即炸 |

### ignite（点燃）

| 字段 | 类型 | 默认 | 说明 |
|---|---|---|---|
| entity | bool | false | 点燃命中的实体（另受 TacZ `AmmoConfig.IGNITE_ENTITY` 门控） |
| block | bool | false | 点燃命中的方块 |
| entitySeconds | int | 5 | 点燃实体的持续秒数 |

### knockback（额外击退）

`knockback`：一个 float，命中时的额外击退强度。省略 = 保留 TacZ 默认击退。

## 注意事项

- **省略即不覆写**：不写 `explosion` 就保留 TacZ 原生（通常无爆炸，除非枪/配件自带）。
- **总开关**：配置 `config/tacz_caliber_ammo-common.toml` 的 `[ammo_effects] enableAmmoEffects`（默认 true）；关掉则忽略所有弹药效果，等价原版 TacZ。
- **安全上限**：`[ammo_effects] maxExplosionRadius`（默认 8.0 格）裁剪爆炸半径，防滥用。
- **TacZ 全局门控**：ignite / explosion 仍受 TacZ 自身 `AmmoConfig` 的全局开关约束——TacZ 那边关了，这边即使写了也不生效。

## 示例

高爆弹（命中即炸、带击退）：

```json
"effects": { "explosion": { "enabled": true, "damage": 8, "radius": 4, "knockback": true } }
```

燃烧弹（点燃目标 8 秒）：

```json
"effects": { "ignite": { "entity": true, "entitySeconds": 8 } }
```

重击弹（额外击退，不改其它）：

```json
"effects": { "knockback": 2.0 }
```
