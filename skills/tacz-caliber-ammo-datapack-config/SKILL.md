---
name: tacz-caliber-ammo-datapack-config
description: >-
  Configure this project's (tacz_caliber_ammo, a TacZ extension) features via datapack JSON or
  datagen: (1) override/assign a gun's calibers, (2) give a gun flat/percent damage modifiers,
  (3) set an ammo's ballistic profile (caliber, baseDamage, armorIgnore, headShotMultiplier,
  pierce, recoil, accuracy, speed, pelletCount) and hit effects (explosion/ignite/knockback),
  (4) use the special calibers none / universal (wan-yong 万用, an ammo that fits every gun).
  USE WHEN 为枪械配置或修改口径, 调整弹药伤害/穿甲/弹道数据, 给某枪加固定或百分比伤害修正, 注册万用弹,
  or adding modify_gun_caliber files. Covers the data schema read by CaliberManager
  (parseAmmo/parseGun/rebuildModify), the 4-tier gun-to-caliber resolution, the damage formula,
  and the two datagen providers (GunCaliberModifyProvider, CaliberAmmoDataProvider). DO NOT USE
  for ammo slot/icon textures (use the tacz-caliber-ammo-slot-texture skill), gun/attachment 3D
  models, or non-tacz_caliber_ammo projects.
---

# TacZ Caliber Ammo -- 数据包/datagen 配置口径·弹药·枪械

本项目在 TacZ 之上加了一层"口径系统"。三类可配置数据都由 `CaliberManager`（静态门面）持有，
在 TacZ 服务端数据 reload 末尾灌入。本 skill 讲怎么用 datapack JSON 或项目内 datagen 去配置它们。

## 速查 (TL;DR)

| 你想做 | 去哪 |
|---|---|
| 给某枪指定/改口径 | `data/[ns]/modify_gun_caliber/*.json`（Tier 1，最高优先、不覆写枪定义） |
| 给某枪加伤害修正 | 同上，把该枪的值写成对象 `{ "flatDamage": .., "percentDamage": .. }` |
| 改一发弹的伤害/穿甲/弹道/效果 | 弹药 index JSON 加扩展字段（**必须有 `caliber`**）；批量走 `docs/tarkov_ammo_stats.csv` + `runData` |
| 让某弹装进任意枪 | 该弹 `caliber` 设为 `tacz_caliber_ammo:universal`（万用弹） |
| 改完不生效？ | 属服务端 data：重进世界 / 重启 runServer/runClient（F3+T 只重载客户端资源、不重载 data） |

## 机制总览（先读）

- 数据来源与入口：
  - 弹药/枪 index JSON（含本项目扩展字段） -> `CommonDataManagerMixin` 在 `CommonDataManager.apply` 的 TAIL 捕获 -> `CaliberManager.rebuildAmmo` / `rebuildGun`。
  - `data/<ns>/modify_gun_caliber/*.json` -> `GunCaliberModifyBootstrap`（`SimpleJsonResourceReloadListener`，扫所有命名空间的该文件夹）-> `rebuildModify`。
  - `data/<ns>/calibers/*.json` -> `CaliberDataBootstrap` -> `rebuildCalibers`（口径友好名，仅 `{ "name": string }`）。
- 三种可配置对象：**枪口径覆盖**、**枪伤害修正**、**弹药弹道档 + 效果**。

## 枪 -> 口径 4 级解析（高优先先命中，都不中则特殊口径 none）

1. Tier 1 `modify_gun_caliber` 独立数据包（不覆写 TacZ 枪 index）。**推荐手动配置走这层**。
2. Tier 2 枪 index 的显式 `calibers` 字段。
3. Tier 3 原生子弹转口径（内置 NATIVE_MAP + 内容弹药自身的 `caliber` 字段）。
4. Tier 4 模糊匹配（`ModConfig.enableFuzzyCaliberMatch`，可关）。

弹药能否装枪：`弹药口径 ∈ 枪口径集`，或`弹药口径 = universal`（万用，装任意枪）。

## 1) 改枪口径 -- modify_gun_caliber（Tier 1，最常用）

文件：`data/<ns>/modify_gun_caliber/<任意名>.json`（命名空间任意，习惯用 `tacz_caliber_ammo`）。

```json
{
  "priority": 0,
  "guns": {
    "tacz:m700": ["tacz_caliber_ammo:7_62x51", "tacz_caliber_ammo:d30_06"],
    "tacz:ak47": "tacz_caliber_ammo:7_62x39"
  }
}
```

- 每个 gun 的值：字符串或数组 = 仅覆盖口径。口径 id 无命名空间时默认补 `tacz_caliber_ammo`（不是 tacz）。
- 跨文件同一 gun：`priority` 高者胜；平级则后加载者胜（会 warn）。
- 优先级最高、且不写入 TacZ 枪数据，适合"手动为某枪指定/改口径"。

## 2) 给枪加伤害修正 -- modify_gun_caliber 对象形式

```json
{
  "priority": 0,
  "guns": {
    "tacz:ak47": { "flatDamage": 0.5, "percentDamage": 0.05 },
    "tacz:m700": { "calibers": ["tacz_caliber_ammo:7_62x51"], "flatDamage": 2.0 }
  }
}
```

- 对象可同时含 `calibers`（可选，注入口径）+ `flatDamage`/`percentDamage`（可选，注入伤害修正），也可只配伤害不改口径。
- 伤害公式（仅对已配弹药档的弹药生效）：`最终伤害 = 弹药 baseDamage * (1 + percentDamage) + flatDamage`，结果 clamp 到 >=0。
- 优先级：modify_gun_caliber 对象形式（MODIFY_DMG）> 枪 index 的 flatDamage/percentDamage 字段（`getGunModifier`）。

## 3) 改弹药数据 -- 弹药 index 扩展字段（parseAmmo 读取）

在弹药 index JSON 里加下列本项目字段。**必须有 `caliber` 才会生成弹药档**；否则该弹回退 TacZ 派生伤害（本项目不接管）。

| 字段 | 类型 | 默认 | 说明 |
|---|---|---|---|
| caliber | string | 必需 | 口径 id，无 ns 默认 tacz_caliber_ammo |
| baseDamage | float | 0 | 单点伤害标量（无距离衰减曲线） |
| armorIgnore | float | 0 | 护甲穿透 0-1（运行时 clamp[0,1]） |
| headShotMultiplier | float | 1 | 爆头倍率（运行时 max(x,0)；0=爆头 0 伤） |
| pierce | int | 1 | 穿透目标数（运行时 max(x,1)） |
| recoil | float(%) | 0 | 后坐力%，带符号 |
| accuracy | float(%) | 0 | 精度%，带符号，可 >100 |
| speed | float | 0 | 初速 m/s 原始值，0 = 不覆写（留 TacZ 默认） |
| pelletCount | int | 0 | 弹丸数，0 = 不覆写；有弹药档且 >1 时每颗弹丸打满 baseDamage |
| effects | object | 无 | 命中/触发效果，见下 |

`effects` 子块（全部可选，缺省保留 TacZ 默认）：

```json
"effects": {
  "explosion": { "enabled": true, "damage": 3.0, "radius": 3.0, "knockback": false, "destroyBlock": false, "delaySeconds": 0 },
  "ignite": { "entity": true, "block": false, "entitySeconds": 5 },
  "knockback": 1.0
}
```

datagen 批量生成：`CaliberAmmoDataProvider` 从 `docs/tarkov_ammo_stats.csv` 生成到
`src/generated/resources/data/tacz_caliber_ammo/index/ammo/<calId>/<model>.json` + 配方 + 无原型口径的占位贴图。
加弹种 = 加一行 CSV + `runData`（名称/abbr 的 lang 键要手动补到 en_us/zh_cn）。

## 4) 改枪属性 -- 枪 index 扩展字段（Tier 2，parseGun）

TacZ 枪 index JSON 可加 `calibers` / `flatDamage` / `percentDamage`。但改 TacZ 原枪 index 是**整文件覆写**
（会丢失 TacZ 自带字段，风险高）。一般改用 Tier 1 的 modify_gun_caliber（独立注入、不覆写）。

## 特殊口径

- `tacz_caliber_ammo:none`：未配置口径的兜底。
- `tacz_caliber_ammo:universal`（万用）：把某弹药的 `caliber` 设成它 -> 该弹可装入**任意**枪械（`isAmmoOfGun` 恒真）；
  不写入/不显示于枪信息，只在弹药 tooltip 显示"万用"。它是纯代码级特殊口径：不入口径注册表、不参与模糊匹配。
  datagen 已内置一发 `universal/round`（baseDamage=7）作示例。

## datagen（改代码）vs 生数据包（运行时挂载）

- **datagen**：改 `GunCaliberModifyProvider`（枪口径/枪伤害）或 `CaliberAmmoDataProvider`（弹药/CSV），跑
  `.\gradlew.bat :1.20.1-forge:runData --no-daemon` 生成到 `src/generated/resources`。
- **生数据包**：把相同结构的 JSON 放进世界/服务器 datapack 的 `data/<ns>/modify_gun_caliber/`（或对应 index 路径），
  `/reload` 或重进世界即生效。两条路 schema 完全相同。

## 验证

- 编译：`.\gradlew.bat :1.20.1-forge:compileJava --no-daemon`。
- 生成：`runData` 后查 `src/generated/resources/...`。
- 生成的 JSON 由 `DataProvider.saveStable` 产出：2 空格缩进、键按字母序、LF 行尾、无末尾换行、无 BOM。
  手写生成物要与此一致，否则下次 runData 会产生 diff（churn）。
- 运行期：枪口径/弹药档属服务端 data，改后需重进世界或重启 runServer/runClient（F3+T 只重载客户端资源，不重载 data）。
- 日志确认：`[tacz_caliber_ammo] loaded N ammo profile(s)` / `loaded modify_gun_caliber for N gun(s)`。

## 常见坑

- 口径 id 无命名空间默认 `tacz_caliber_ammo`（不是 tacz）。
- 弹药无 `caliber` 字段 -> 不生成弹药档，伤害回退 TacZ 派生（非本项目控制）。
- `speed` / `pelletCount` = 0 表示"不覆写"，不是"设为 0"。
- 改 TacZ 原枪/原弹 index 是整文件覆写（会丢 TacZ 字段）；优先用 modify_gun_caliber。
- gun id / ammo id 用 `tacz:<path>`；本项目内容弹 id 是 `tacz_caliber_ammo:<calId>/<model>`。
