# EFT -> TacZ 弹药数值转换算法

> 用途: 把 Escape from Tarkov (数据源 tarkov.dev) 的弹药数值, 转换为本项目 (TacZ Caliber Ammo)
> design 第 5 节定义的弹药字段, 供 datagen 批量生成弹药索引。
> 数据源: `docs/tarkov_ammo_stats.csv` (EFT 数值, 已按本项目口径归类) + `docs/calibers.md` (口径 id 表)。
> 落地: `CaliberAmmoDataProvider` 按本算法把 CSV 批量转成
> `data/tacz_caliber_ammo/index/ammo/<口径id>/<型号id>.json`。

## 1. 输入 / 输出

EFT 单条 (tarkov.dev `ItemPropertiesAmmo`):

| 字段 | 记号 | 含义 |
|---|---|---|
| damage | d | 单发基础血量伤害 |
| penetrationPower | pen | 破甲力 (约 0..70, 少数到 115) |
| fragmentationChance | frag | 碎裂概率 0..1 |
| armorDamage | - | 护甲耐久伤 (可选微调, 默认不用) |
| initialSpeed | - | 初速 m/s (归枪, 不进弹药) |
| projectileCount | proj | 弹丸数 (霰弹 > 1) |
| ammoType / caliber | - | 类型 / 口径 |

本项目弹药字段 (design 第 5 节):

| 字段 | 范围 | 来源 |
|---|---|---|
| caliber | 口径 id | 查表 (口径名 -> id) |
| baseDamage | 标量 | d |
| armorIgnore | 0..1 | pen |
| headShotMultiplier | >= 1 | 分类 / 口径 + frag |
| pierce | 整数 | pen |

最终伤害仍为 `final = baseDamage * (1 + gun.percentDamage) + gun.flatDamage`;
本算法只产弹药基线, 枪修正在其上叠加。

## 2. 转换公式

```
frag            = clamp( fragPercent / 100, 0, 1 )
baseDamage      = clamp( round0.5( d * K_DMG ),  DMG_MIN, DMG_MAX )   // 霰弹用 K_DMG_SHOT
armorIgnore     = clamp( (pen - PEN0) / PEN_SPAN, 0, AI_CAP )         // round 0.05
pierce          = 1 + (pen >= P2 ? 1 : 0) + (pen >= P3 ? 1 : 0)
headShotMin     = minimumByCategory(category, caliber)
headShotMult    = clamp( headShotMin + HS_K * frag, headShotMin, HS_MAX ) // round 0.05
```

设计意图:
- 伤害线性压缩到 MC 尺度 (步枪弹落 6..14, .50/12.7 更高但封顶)。
- 穿甲只由 pen 决定 (EFT 里 pen 就是破甲力); armorDamage 仅作可选微调。
- 穿透计数按 pen 阈值给 1/2/3。
- CSV 的 `破片%` 固定按 0..100 百分数读取，再归一化到 0..1。
- 爆头倍率先按武器类别给出最低值，再由 frag 提升：碎裂 = 软组织毁伤大 -> 爆头收益高。
- CSV 未单列“狙击步枪”，因此用口径集合识别；同一口径的弹药共享最低倍率。

## 3. 常量 (已标定, 可调)

| 常量 | 值 | 说明 |
|---|---|---|
| K_DMG | 0.16 | 伤害系数 (proj = 1) |
| K_DMG_SHOT | 0.10 | 霰弹单颗系数 (proj > 1, 防多颗求和爆表) |
| DMG_MIN / DMG_MAX | 3 / 40 | 伤害封顶 |
| PEN0 / PEN_SPAN | 8 / 64 | armorIgnore 线性区 |
| AI_CAP | 0.90 | armorIgnore 上限 |
| P2 / P3 | 50 / 68 | pierce 2/3 阈值 |
| HS_K / HS_MAX | 0.60 / 2.60 | 破片率加成斜率 / 全局上限 |

爆头最低倍率：手枪 1.20、PDW 1.35、步枪 1.50、霰弹枪 1.80、榴弹/信号弹 1.00；
无法识别的其他类别为 1.20。以下口径作为狙击步枪弹，优先使用 2.00：
`7_62x54r`、`d30_06`、`7_92x57_mauser`、`d338`、`9_3x64`、`d50_bmg`。

标定锚点: 5.56 M855 (d=54) -> base 8.5; 7.62x39 FMJ (d=63) -> base 10.0;
pen 53 -> armorIgnore 0.70; 步枪弹 frag 0.5 -> headshot 1.80。

## 4. 实测样例 (真实 EFT 数值)

| 口径 / 型号 | EFT d / pen / frag | -> base / armorIgnore / pierce / headshot |
|---|---|---|
| 5.56 M855 | 54 / 31 / 0.50 | 8.5 / 0.35 / 1 / 1.80 |
| 5.56 M995 | 42 / 53 / 0.42 | 6.5 / 0.70 / 2 / 1.75 |
| 5.56 HP | 79 / 7 / 0.70 | 12.5 / 0.00 / 1 / 1.90 |
| 5.56 SSA AP | 38 / 57 / 0.30 | 6.0 / 0.75 / 2 / 1.70 |
| 7.62x39 FMJ | 63 / 26 / 0.30 | 10.0 / 0.30 / 1 / 1.70 |
| 7.62x39 MAI AP | 53 / 58 / 0.05 | 8.5 / 0.80 / 2 / 1.55 |

## 5. 口径 id / 型号 id / 数据字段

- caliber id: 查 `calibers.md` (中文口径名 -> id)。例: `5.56x45mm NATO` -> `5_56x45`,
  `7.62x39mm` -> `7_62x39`, `.45 ACP` -> `d45_acp`。CSV 中无法映射的口径 (信号弹/榴弹等) 跳过。
- 型号 id: 弹药名去掉口径前缀与括号限定词、剥中文后, 按 calibers.md 命名规则规范化:
  小写, 去 mm, `.`/空格/`-`/`/` -> `_`, 合并连续 `_`。
  例: `M995` -> `m995`, `SSA AP` -> `ssa_ap`, `Mk 318 Mod 0 (SOST)` -> `mk_318_mod_0`。
- TacZ 字段: `name` = 本地化键 `ammo.tacz_caliber_ammo.<口径>.<型号>`; `stack_size` 默认 60;
  `sort` 递增; `display` 见第 6 节。

## 6. 边界与特殊情况

- speed 归枪: initialSpeed 不进弹药; 如需按口径中位速换算枪 speed (另开算法)。
- 霰弹 (proj > 1): baseDamage 用单颗 EFT 伤害 * K_DMG_SHOT (TacZ 霰弹枪自身发 proj 颗); 独头弹 proj = 1 走常规。
- 非子弹 (ammoType != bullet, 榴弹/火箭): 不套本公式, 跳过 (走 TacZ 爆炸/自定义)。
- 无 EFT 对应的型号: 按类型回退默认档 (FMJ/HP/AP 预设) 或手填。
- 贴图 (display), 见 datagen:
  - 有 TacZ 原型的口径 (calibers.md 中 `原TacZ弹药` 非空): 复用 `tacz:<taczId>_display`
    (如 `tacz:762x39_display`), 直接沿用 TacZ 的模型 / 材质。
  - 无原型的口径: 用纯色贴图占位 -> 复用一个通用 TacZ 子弹模型 (geo_models),
    但 `texture` (uv) 与 `slot` (GUI 图标) 指向本 mod 生成的单色 PNG
    (`assets/tacz_caliber_ammo/textures/ammo/{uv,slot}/<口径>.png`),
    `display` 指向 `assets/tacz_caliber_ammo/display/ammo/<口径>_display.json`。
    每个口径一种确定性色 (由口径 id 哈希取色), 便于区分。

TacZ 弹药 display schema (已核实):
```
{
  "model":   "<ns>:ammo/<id>",        // geo_models/ammo/<id>.json
  "texture": "<ns>:ammo/uv/<id>",     // textures/ammo/uv/<id>.png
  "slot":    "<ns>:ammo/slot/<id>",   // textures/ammo/slot/<id>.png (GUI 图标)
  "shell":   { "model": ..., "texture": ... },  // 可选
  "tracer_color": "#RRGGBB",          // 可选
  "transform": { "scale": { ... } }   // 可选
}
```

## 7. 落地 (datagen)

`CaliberAmmoDataProvider` 读 `docs/tarkov_ammo_stats.csv`, 逐行:
1. 口径名 -> 口径 id (查表, 未映射跳过); 弹药名 -> 型号 id (规范化)。
2. 套第 2 节公式 (第 3 节常量) 得 base / armorIgnore / pierce / headshot。
3. 写 `index/ammo/<口径>/<型号>.json` (TacZ 字段 + 我方字段) + `calibers/<口径>.json`。
4. display: 有原型复用 tacz; 无原型生成纯色占位 (display + uv/slot 单色 PNG)。

## Revision

- 2026-07-15 初稿: 4 条公式 + 常量, 用 EFT 真实数值标定; 驱动 datagen 批量转换 + 纯色占位贴图。
- 2026-07-31: 普通弹/霰弹伤害系数提高到 0.16/0.10；爆头倍率改为类别下限 + 破片率加成，并按口径识别狙击弹。
