# TacZ Caliber Ammo

> 为 **Timeless and Classics Zero (TacZ)** 打造的「口径 (Caliber)」弹药系统。
> Minecraft 1.20.1 · Forge 47+ · Java 17 · 依赖 TacZ

`TacZ Caliber Ammo` 把 TacZ 的伤害归属从「枪决定一切」改成「**口径 + 弹种决定一切**」：
枪只负责认它能吃哪种口径，真正的伤害、护甲穿透、爆头倍率、后坐力、精度、初速、弹丸数都写在**弹药**上。
于是同一把枪、同一口径下的不同弹种（FMJ / JHP / AP / 曳光 / 霰弹……）会有各自的性能，
混装弹匣时还能**逐发按装填顺序**生效。

---

## 目录

- [核心理念](#核心理念)
- [环境与依赖](#环境与依赖)
- [给玩家](#给玩家)
- [给枪包开发者](#给枪包开发者如何兼容本项目)
- [给模组开发者](#给模组开发者如何兼容本项目)
- [构建与数据生成](#构建与数据生成)
- [许可](#许可)

---

## 核心理念

原版 TacZ 的伤害基本由枪的数据决定，弹药差异有限。本项目引入一层「口径」抽象：

```
枪 (Gun) ──认领──▶ 口径 (Caliber) ──包含──▶ 多个弹种 (Ammo variant)
                                              每个弹种自带：
                                              伤害 / 护甲穿透 / 爆头倍率 / 穿透数
                                              后坐力% / 精度% / 初速 / 弹丸数
```

- **枪** 只声明「我接受哪个/哪些口径」。
- **弹种** 是同口径下的具体子弹，携带全部数值；换不同弹种就是换性能。
- **混装弹匣**：一梭子里可以有多种弹种，开火时按装填顺序**逐发**取用各自的数值（连发、霰弹每一粒都独立）。

内容层完全通过**数据包**驱动（弹药 / 口径 / 配方 JSON），逻辑层通过 **Mixin** 挂接 TacZ，
本 mod **不新增任何物品**——弹药仍然是 TacZ 原生的弹药物品（靠 NBT 的 `ammoId` 区分弹种）。

当前内置内容规模：**37 个口径 / 207 个弹种 / 207 个制枪台配方**，覆盖 TacZ 全部 54 把原版枪。

---

## 环境与依赖

| 项目 | 版本 |
| --- | --- |
| Minecraft | 1.20.1 |
| 加载器 | Forge `[47,)` |
| Java | 17 |
| 前置 | Timeless and Classics Zero (TacZ) |

> 工程使用 **Stonecutter + Architectury Loom** 组织，主类保留了多加载器占位；当前仅激活 `1.20.1-forge` 节点。

---

## 给玩家

### 安装

1. 安装 Forge 1.20.1。
2. 把 **TacZ** 与本 mod 的 jar 一起放进 `mods/`。
3. 启动即可，无需额外配置。

### 你会注意到的变化

- **弹种决定性能**：同口径不同弹种，伤害 / 穿甲 / 爆头 / 后坐力 / 精度 / 初速 / 弹丸数各不相同。
- **混装逐发生效**：弹匣里混装多种弹种时，按装填顺序一发一发地取用对应数值。
- **物品提示 (Tooltip)**：悬停弹药会显示它的口径与各项数值；悬停枪会显示它接受的口径。
  后坐力 / 精度等带符号数值会用颜色区分优劣。
- **制枪台配方**：所有弹种都能在 TacZ 的**制枪台 (Gun Smith Table)** 合成。
  穿甲越高的弹（如 AP 弹）需要越多的铜锭，且会额外消耗铁粒，作为高性能的代价。
- **退弹**：在枪械改装界面新增「退弹」按钮，可把弹匣里的（混装）弹药按弹种退回背包。
- **创造模式换弹**：优化为「背包里有对应弹药时优先消耗背包弹药」，没有时才走创造无限装填。

### 配置

配置文件：`config/tacz_caliber_ammo-common.toml`

| 配置项 | 默认 | 说明 |
| --- | --- | --- |
| `caliber_resolution.enableFuzzyCaliberMatch` | `true` | 是否启用「模糊匹配」兜底（按枪/弹 id 归一化 token 猜口径）。关掉后无法被前三级解析的枪直接归为 `none`。 |
| `ballistics.bulletSpeedScale` | `0.33` | 把弹药初速 (m/s) 换算成 TacZ 内部速度单位的系数。最终速度 = `初速 × 本值 ÷ 20` (格/tick)。 |

---

## 给枪包开发者（如何兼容本项目）

好消息：**你可以零改动兼容**。如果你的枪使用 TacZ 原生弹药，本 mod 会自动为它找到口径；
如果你想让自己的枪/弹药获得完整的口径 + 弹道体验，则按下文主动适配。

### 口径解析：四级优先链

给一把枪找口径时，本 mod 按优先级从高到低依次尝试，命中即止，全部落空则归为 `tacz_caliber_ammo:none`：

| 级别 | 来源 | 说明 |
| --- | --- | --- |
| Tier 1 | `modify_gun_caliber` 独立数据包 | 最高优先，直接指定「枪 → 口径」 |
| Tier 2 | 枪 index JSON 的 `calibers` 字段 | 在枪自己的数据里声明 |
| Tier 3 | 弹药的 `caliber` 字段 / 内置原生映射表 | 由枪的默认弹药推出口径（含 19 条 TacZ 原生弹映射） |
| Tier 4 | 模糊匹配（可在配置关闭） | 按 id token 归一化猜测，保守命中 |

### 方式 A：给弹药加字段（推荐）

TacZ 用 Gson 解析弹药 index JSON 且**忽略未知字段**，因此你只需在自己的弹药 index JSON
（`data/<你的命名空间>/index/ammo/<口径>/<型号>.json`）里加上本 mod 认识的字段：

```json
{
  "caliber": "7_62x25",
  "baseDamage": 4.5,
  "armorIgnore": 0.45,
  "headShotMultiplier": 1.45,
  "pierce": 1,
  "recoil": 0,
  "accuracy": 0,
  "speed": 940,
  "pelletCount": 1,

  "display": "tacz_caliber_ammo:7_62x25_display",
  "name": "ammo.tacz_caliber_ammo.7_62x25.m995",
  "sort": 10,
  "stack_size": 60
}
```

上半部分是本 mod 扩展字段，下半部分是 TacZ 原有字段（照常填写）。扩展字段说明：

| 字段 | 类型 | 默认 | 含义 |
| --- | --- | --- | --- |
| `caliber` | string | 无则不识别为口径弹 | 口径 id；不带命名空间时默认 `tacz_caliber_ammo` |
| `baseDamage` | float | `0` | 基础伤害 |
| `armorIgnore` | float (0–1) | `0` | 护甲穿透比例 |
| `headShotMultiplier` | float | `1` | 爆头伤害倍率 |
| `pierce` | int | `1` | 可穿透的实体数 |
| `recoil` | float | `0` | 后坐力修正 %（带符号；正数 = 后坐力更大）。映射：`后坐力 × (1 + recoil/100)` |
| `accuracy` | float | `0` | 精度修正 %（带符号，可 >100；正数 = 更准）。映射：`散布 ÷ (1 + accuracy/100)` |
| `speed` | float | `0` | 初速原始值 m/s；`0` = 沿用枪默认 |
| `pelletCount` | int | `0` | 弹丸数；`0` = 沿用枪默认，`>1` = 霰弹多弹丸 |

### 方式 B：口径定义文件

为口径提供展示名（用于 Tooltip），放在 `data/<你的命名空间>/calibers/<口径 id>.json`：

```json
{ "name": ".30-06 Springfield" }
```

### 方式 C：在枪数据里声明口径 / 微调伤害

在你的枪 index JSON（`data/<ns>/index/gun/<枪>.json`）里加入（Tier 2）：

```json
{
  "calibers": ["tacz_caliber_ammo:5_56x45"],
  "flatDamage": 0,
  "percentDamage": 0
}
```

- `calibers`：这把枪接受的口径列表。
- `flatDamage` / `percentDamage`：在弹药伤害之上叠加的固定 / 百分比修正（可选）。

### 方式 D：modify_gun_caliber 数据包（Tier 1，最高优先）

无需改动别人的枪包即可覆盖「枪 → 口径」映射，放在 `data/<你的命名空间>/modify_gun_caliber/任意名.json`：

```json
{
  "priority": 0,
  "guns": {
    "tacz:ak47": ["tacz_caliber_ammo:7_62x39"],
    "tacz:m4a1": ["tacz_caliber_ammo:5_56x45"]
  }
}
```

- 值可以是数组或单个字符串。
- 跨多个文件对同一把枪冲突时，`priority` 高者胜；同级则后加载者覆盖并给出警告。

### 方式 E：制枪台配方

让你的弹种可在 TacZ 制枪台合成，放在 `data/<ns>/recipes/ammo/<口径>/<型号>.json`：

```json
{
  "type": "tacz:gun_smith_table_crafting",
  "materials": [
    { "count": 16, "item": { "tag": "forge:ingots/copper" } },
    { "count": 2,  "item": { "tag": "forge:gunpowder" } }
  ],
  "result": {
    "type": "ammo",
    "count": 45,
    "group": "pd_cartridges",
    "id": "tacz_caliber_ammo:7_62x25/m995"
  }
}
```

- `result.id` 指向要产出的弹种（`<命名空间>:<口径>/<型号>`）。
- `result.group` 决定在制枪台的分组页签（本项目约定：手枪/PDW → `pd_cartridges`，步枪 → `ifp_rifle_cartridges`，霰弹 → `shotgun_shells`，榴弹 → `explosives`）。
- 本项目内置配方的经验公式：`铜 = 基础 + round(穿甲/6)`；`穿甲 ≥ 40` 额外加 `round((穿甲-30)/6)` 个铁粒——**穿甲越高越贵**。

---

## 给模组开发者（如何兼容本项目）

### 只读 API：`CaliberManager`

`com.tacz_caliber_ammo.caliber.CaliberManager` 是查询口径数据的静态门面，全部为线程安全的只读方法：

```java
// 一把枪接受的口径集合（走上文四级解析链；无则返回 { tacz_caliber_ammo:none }）
Set<ResourceLocation> getGunCalibers(ResourceLocation gunId);

// 一种弹药所属的口径（弹药 caliber 字段 → 原生映射 → none）
ResourceLocation getAmmoCaliber(ResourceLocation ammoId);

// 弹药的完整弹道档；未配置返回 null（调用方应回退到枪的 bulletData）
@Nullable AmmoProfile getAmmoProfile(ResourceLocation ammoId);

// 枪的伤害修正（calibers/flatDamage/percentDamage）；未配置返回 null
@Nullable GunDamageModifier getGunModifier(ResourceLocation gunId);

// 口径定义（展示名）；未定义时回退为「以 id 路径为名」
Caliber getCaliber(ResourceLocation caliberId);

// 某口径下的全部弹种 id（按 id 排序，稳定轮询）
List<ResourceLocation> getAmmosForCaliber(ResourceLocation caliberId);
```

数据模型（均为不可变 `record`）：

```java
record AmmoProfile(ResourceLocation caliber, float baseDamage, float armorIgnore,
                   float headShotMultiplier, int pierce,
                   float recoilModifier, float accuracyModifier, float speed, int pelletCount);

record GunDamageModifier(Set<ResourceLocation> calibers, float flatDamage, float percentDamage);

record Caliber(ResourceLocation id, String name);
```

### 数据加载时机

- 弹药 / 枪的字段：由 `CommonDataManagerMixin` 在 **TacZ 数据 reload 的末尾**灌入 `CaliberManager`。
- 口径定义、`modify_gun_caliber`：各自通过 Forge `AddReloadListenerEvent` 挂 `SimpleJsonResourceReloadListener`，随**服务端数据 reload**刷新。

因此请在**世界加载 / 数据包 reload 之后**再查询这些 API；过早（如 mod 构造期）查询会得到空数据。

### 通过数据包集成（免代码）

上文「给枪包开发者」的全部方式（弹药字段 / 口径定义 / `modify_gun_caliber` / 配方）对模组同样适用——
把这些 JSON 放进你 mod 的 `resources/data/...` 即可，无需编译期依赖本项目。

### Mixin 冲突提示

本 mod 通过 Mixin 挂接以下 TacZ / MC 类。如果你的 mod 也修改它们，请留意兼容：

| 目标 | 用途 |
| --- | --- |
| `AbstractGunItem` | 伤害计算接入口径数据 |
| `AmmoItem` / `AmmoBoxItem`（DataAccessor） | 覆盖「弹药是否匹配该枪」的判定，改为按口径匹配 |
| `CommonDataManager` | 在数据 reload 末尾抽取弹药/枪字段灌入 `CaliberManager` |
| `EntityKineticBullet` | 按弹种应用伤害 / 护甲穿透 / 爆头，及霰弹多弹丸伤害 |
| `ModernKineticGunScriptAPI` | 逐发发射的初速 / 精度 / 弹丸数，及混装换弹逻辑 |
| `CameraSetupEvent`（客户端） | 按弹种缩放开火后坐力 |
| `ClientGunTooltip`（客户端） | 弹匣 / 弹药 Tooltip 展示 |

> 针对 TacZ 类的 Mixin 使用 `remap = false`；从 Minecraft 继承的方法按需逐注解 `remap = true`。

---

## 构建与数据生成

```powershell
# 编译
./gradlew :1.20.1-forge:build

# 生成内置数据包（口径 / 弹药 index / 配方，源数据为 docs/tarkov_ammo_stats.csv）
./gradlew :1.20.1-forge:runData

# 本地起服 / 起客户端冒烟测试
./gradlew :1.20.1-forge:runServer
./gradlew :1.20.1-forge:runClient
```

内置内容由 `datagen/CaliberAmmoDataProvider` 从 `docs/tarkov_ammo_stats.csv` 全量生成，产物位于 `src/generated/resources`。

---

## 许可

All Rights Reserved · 作者：Tonywww
