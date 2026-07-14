# TacZ 口径弹药系统重做 — 设计文档 (Stage 1)

> 目标: 把 TacZ 的 "枪 -> 单一弹药" 改为 "枪 -> 一个或多个口径, 口径 -> 多种弹种(HP/SP/FMJ/BP...)", 并把弹道/伤害数据从 "枪" 迁到 "弹药", 枪只保留伤害修正。
>
> In scope: 口径匹配层; 弹道所有权反转(弹药持有基础伤害/护甲穿透/爆头/穿透/距离衰减, 枪持有初速/重力及固定/百分比伤害修正); 新建一整套 TacZ 格式弹药并全量迁移原版枪械; JSON 数据包驱动; 未配置内容按规则自动派生。
>
> **Non-goals**: 不改后坐力/散布/射速/换弹时序等系统本身; 不做新的弹药渲染/模型体系(复用 TacZ 显示); 不支持非 TacZ 的枪; 不新增联机协议(沿用 TacZ 的 NBT 同步); 不改 TacZ 源码(仅 Mixin + 本 mod 数据包)。
>
> Constraints: Forge 1.20.1 / TacZ `1028108-8141310`(已经 CurseMaven 引入并解析); Mixin 已在 build.gradle.kts 注册; 保持与 TacZ 现有 gun pack 数据兼容。

## 1. 背景 / 问题

现状(已核实, javap 反编译自 `timeless-and-classics-zero-1028108-8141310.jar`):

- `GunData.ammoId: ResourceLocation` —— 一把枪只认一种弹药 id。
- `GunData.bulletData (BulletData)` —— 伤害 `damageAmount`、初速 `speed`、穿透 `pierce`、击退、点燃、曳光, 以及 `ExtraDamage` 的护甲无视 `armorIgnore`、爆头 `headShotMultiplier`、距离衰减 `damageAdjust` **全部定义在枪上**。
- 弹药物品 `AmmoIndexPOJO` 只有 `name/display/stackSize/tooltip/sort` —— 弹药是纯 "身份 + 显示", 不带任何弹道。
- 兼容判定 `IAmmo.isAmmoOfGun(gun, ammo)` 默认实现(字节码确认) =
  `getCommonGunIndex(gunId).map(idx -> idx.getGunData().getAmmoId().equals(ammoId)).orElse(false)` —— **精确 id 相等**。

痛点: 无法表达 "一个口径对应多种弹种, 一把枪吃一个/多个口径"; 也无法让 HP/SP/FMJ/BP 各有不同的伤害/穿透/爆头表现(因为弹道全绑在枪上)。

## 2. 方案 / 核心原理

两个改动叠加:

1. **口径层**: 在枪与弹药之间插入 "口径(caliber)"。枪声明可用口径集合; 弹药声明所属口径; 兼容 = 交集非空。
2. **弹道所有权反转**: 基础战斗数值(基础伤害、护甲穿透、爆头倍率、穿透、距离衰减)迁到 **弹药**; 枪只保留 **固定伤害** 与 **百分比伤害** 两类修正(初速/重力仍归枪, 移除枪上的护甲穿透/爆头/穿透)。**最终伤害 = (弹药基础) * (1 + 枪械百分比) + 枪械固定值**。

落地靠 4 个已核实的注入点(Mixin) + 一个数据包驱动的 `CaliberManager`。

**自动派生规则(关键, 兼顾 "全量迁移" 与 "未配置兼容")**: 默认把每把枪原始 `GunData.ammoId` 视为其唯一口径, 每个弹药物品原始 `ammoId` 视为其口径。于是 **未配置任何口径 JSON 时, 行为与原版完全等价**(单元素集合的交集 == 相等)。显式口径 JSON 再用于: 合并多个原始 ammoId 到一个口径 / 增加 HP/SP/FMJ/BP 变种 / 给枪多口径 / 覆盖弹道。

## 3. 架构与关键决策

```mermaid
flowchart TB
  subgraph Data["数据层 (本 mod JSON 数据包)"]
    C["calibers/ID.json\n口径元数据 + 默认弹道档"]
    AP["ammo_profiles/ammoId.json\n所属口径 + 基础伤害/护甲穿透/爆头/穿透pierce/距离衰减"]
    GM["gun_mappings/gunId.json\n可用口径列表 + 枪伤害修正(固定/百分比)"]
  end
  Data --> CM["CaliberManager (数据包重载监听)\n索引: gunId->口径 / ammoId->口径 / ammoId->弹道档 / gunId->修正"]
  CM --> M1["Mixin M1 isAmmoOfGun\n口径交集匹配"]
  CM --> M2["Mixin M2 换弹/背包搜索\n找口径匹配弹药, 写已装弹种到枪NBT"]
  CM --> M4["Mixin M4 EntityKineticBullet\n弹药基础值 + 枪修正 = 最终伤害"]
  NBT["枪NBT: 已装弹种 ammoId (新增)"] --> M3["Mixin M3 发射路径\n用已装弹种构造子弹"]
  M3 --> M4
```

关键决策:

- **为何 Mixin 而非纯数据/KubeJS**: 匹配与伤害计算是 TacZ 编译期逻辑, 无数据扩展点; `isAmmoOfGun` / `EntityKineticBullet` 必须字节码注入(用户已确认接受)。
- **为何在子弹构造处做伤害反转**: `EntityKineticBullet` 构造器已带 `ammoId + GunData + BulletData`(签名已核实), 在构造末尾按 `ammoId` 查弹药档覆盖 `damageAmount/armorIgnore/headShot` 字段, 下游 `getDamage/onHitEntity/createDamageSources` 无需再改。
- **为何新增 "已装弹种" NBT**: `IGun` 无 "当前已装填弹药 id" 的 getter(已核实), 多弹种时必须记录装的是哪个, 供发射与显示。
- **数据键用 TacZ 的 gunId/ammoId(ResourceLocation)**: 无法给 TacZ 的 POJO 加字段, 故用旁路映射按 id 关联。

伤害组合公式(已定):

- **最终伤害 = (弹药基础伤害) * (1 + 枪.percentDamage) + 枪.flatDamage**。基础伤害取弹药档距离-伤害曲线 `damageAdjust`(或标量 `baseDamage`)在当前距离的取值。
- 护甲穿透 `armorIgnore`、爆头 `headShotMultiplier`、穿透 `pierce`: **仅来自弹药**。
- **初速 `speed` / 重力 `gravity` 留在枪**; 射速/后坐/散布/换弹时序同样留在枪。
- 配件修饰(TacZ `modifyProperty`)在上式之后叠加(先后顺序实现期核实)。

## 4. 依赖

- **Hard**: TacZ `1028108-8141310`(`modImplementation` 经 CurseMaven, 已解析); Architectury Loom 内建 Mixin(已在 build.gradle.kts 注册 `tacz_ammo_reload.mixins.json`); Forge 1.20.1。
- **Soft / optional**: 无强制。TacZ 的配件修饰系统(`IGun.modifyProperty` / `resource.modifier.custom.*`)与本设计并行 —— 需保证配件对伤害的修正仍能与新公式叠加(风险项)。

## 5. 接口 / 契约(草案 —— 将在平行任务表冻结)

数据 schema(键 = TacZ ResourceLocation):

- caliber: `{ "name": string, "tooltip": string, "defaultProfile"?: AmmoProfile }`
- ammo_profile(按弹药物品 id): `{ "caliber": rl, "baseDamage": float | "damageAdjust": [[dist,dmg], ...], "armorIgnore": float, "headShotMultiplier": float, "pierce": int }`  (初速/重力不在弹药, 归枪)
- gun_mapping(按枪 id): `{ "calibers": [rl, ...], "flatDamage": float, "percentDamage": float }`
- 自动派生: 缺省开启; 未命中显式配置时 口径 = 原 `ammoId`, 弹道档 = 由枪原始 `bulletData` 派生。

Java 契约(命名待冻结):

- `CaliberManager`(SimpleJsonResourceReloadListener): `getGunCalibers(rl) -> Set<rl>`, `getAmmoCaliber(rl) -> rl`, `getAmmoProfile(rl) -> AmmoProfile`, `getGunModifier(rl) -> GunDamageModifier`; 未命中走自动派生。
- 已装弹种 NBT 访问器: `getLoadedAmmoId(stack) / setLoadedAmmoId(stack, rl)`(NBT 键如 `TacAmmoReload:LoadedAmmo`)。
- Mixin 目标(已核实): `com.tacz.guns.api.item.nbt.AmmoItemDataAccessor#isAmmoOfGun`; `com.tacz.guns.entity.EntityKineticBullet`(构造器 / `getDamage` / `onHitEntity`)。目标待核实: 换弹搜索 `com.tacz.guns.entity.shooter.LivingEntityAmmoCheck`(精确方法); 发射构造子弹处(类/方法)。

## 6. 风险与边界

| 风险 | 级别 | 缓解 |
|---|---|---|
| Mixin 随 TacZ 更新失效 | H | 锁版本; 集中 mixin; 每处 null 检查, 未命中回退原版逻辑 |
| 多弹种时换弹 "装哪一种" | L | 已定: 暂用背包扫描顺序取第一个口径匹配弹药(后续再加优先级/GUI) |
| 已装弹种 NBT 客户端/服务端同步 | M | 沿用 TacZ 的 NBT 同步; getCommonGunIndex 为服务端权威 |
| 与 TacZ 配件 modifyProperty 伤害修正叠加冲突 | M | 明确组合顺序: 弹药基础 -> 枪固定/百分比 -> 配件修饰 |
| 全量自动派生的正确性 | M | 派生 = 原 ammoId; 未配置即等价原版, 再逐口径显式覆盖 |
| 建整套 TacZ 格式弹药(各口径 HP/SP/FMJ/BP)的内容量 | M | 独立内容任务; 用模板 + 数据生成批量产出 |

**Boundaries —— 本项目不做**: 不改后坐/散布/射速/换弹时序算法; 不做弹药新渲染体系; 不动非 TacZ 的枪; 不改 TacZ 源码(仅 Mixin + 数据包)。

## 7. 核实状态

| 事项 | 状态 | 来源 / 确认方式 |
|---|---|---|
| GunData 有单一 ammoId + bulletData | verified | javap GunData |
| 弹道/护甲穿透/爆头全在枪(BulletData / ExtraDamage) | verified | javap BulletData, ExtraDamage |
| 弹药物品无弹道(AmmoIndexPOJO 仅显示) | verified | javap AmmoIndexPOJO |
| isAmmoOfGun = ammoId 精确相等 | verified | javap -c AmmoItemDataAccessor |
| EntityKineticBullet 构造带 ammoId+GunData+BulletData; onHitEntity/getDamage 为伤害点 | verified | javap EntityKineticBullet |
| IGun 无 "已装弹种 id" getter | verified | javap IGun |
| 换弹/背包搜索的精确方法, 是否复用 isAmmoOfGun | to-verify | 读 LivingEntityAmmoCheck / shooter 反编译 |
| 发射处 bullet.ammoId 来源(改传已装弹种) | to-verify | 定位 shoot 构造子弹的类/方法 |
| 客户端弹药计数/显示(ClientAmmoIndex)受影响面 | to-verify | 读 client.resource.index |

## 8. 已决策(原开放问题, 2026-07-14 确认)

- 初速 `speed` / 重力 `gravity` **归枪**; 穿透 `pierce` **归弹药**。
- 换弹选弹种: **暂用背包顺序**(扫描背包取第一个口径匹配的弹药; 暂不做优先级/记忆/GUI)。
- 弹药内容: **新建一整套 TacZ 格式弹药**(作为本 mod 内置 TacZ gun pack), 覆盖各口径的 HP/SP/FMJ/BP 等型号; 我方 caliber / ammo_profile 侧车数据按其 ammoId 关联。
- 伤害公式: **最终伤害 = (弹药基础) * (1 + 枪械百分比) + 枪械固定值**; 配件 `modifyProperty` 修饰在此之后叠加(先后顺序实现期核实)。

剩余 to-verify(见第 7 节): 换弹搜索精确方法 / 发射处 ammoId 来源 / 客户端显示影响面。

## 9. 关键骨架(最风险处, 伪代码)

```java
// M1: 口径交集匹配 (Mixin AmmoItemDataAccessor#isAmmoOfGun, @Inject cancellable 或 @Overwrite)
Set<ResourceLocation> gunCals = CaliberManager.getGunCalibers(gunId);
ResourceLocation ammoCal = CaliberManager.getAmmoCaliber(ammoId);
return ammoCal != null && gunCals.contains(ammoCal);

// M4: 弹药基础值 + 枪修正 (Mixin EntityKineticBullet 构造末尾)
AmmoProfile p = CaliberManager.getAmmoProfile(this.ammoId);   // 缺省: 由枪 bulletData 派生
if (p != null) {
    this.damageAmount = p.damageCurve();                       // 基础伤害曲线来自弹药
    this.armorIgnore  = p.armorIgnore();                       // 护甲穿透仅弹药
    this.headShot     = p.headShotMultiplier();                // 爆头仅弹药
}
    this.pierce       = p.pierce();                            // 穿透仅弹药 (初速/重力仍来自枪)
}
// 最终伤害 (在 getDamage(distance) 内组合):
//   float base = baseFromCurve(this.damageAmount, distance);          // 弹药基础
//   GunDamageModifier m = CaliberManager.getGunModifier(this.gunId);  // 枪: 百分比 + 固定
//   return base * (1 + m.percentDamage()) + m.flatDamage();
```

## Revision Log

- 2026-07-14 — 初稿(Stage 1)。基于 javap 核实 TacZ 8141310 的弹药/伤害模型; 5 项高杠杆决策(弹道所有权反转 / 接受 Mixin / 全量迁移 / JSON 数据包 / 自动派生兼容)已与用户确认。
- 2026-07-14 — 补充 4 项细节决策: 初速/重力归枪、穿透归弹药; 换弹暂用背包顺序; 新建整套 TacZ 格式弹药; 最终伤害 = 弹药基础 * (1 + 枪百分比) + 枪固定值。第 8 节开放问题全部结清。
