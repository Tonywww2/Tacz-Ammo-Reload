# Gate B 验证记录

> 日期: 2026-07-15 · 执行: gate-runner(集成/门禁) · 阶段: Stage B 三 lane(PB-1/PB-2/PB-3)集成后
> 环境: `1.20.1-forge`(Forge 47.4.0, Java 17) + TacZ `timeless-and-classics-zero` 8141310

## 1. 结论

- **集成 + 数据层 + 核心判定逻辑: 通过(headless 一手证据)。**
- **游戏内开火/换弹 UI/伤害数值观测: 待手动 runClient**(无头服务器无玩家实体, GUI 无法自动化; 见 §5)。

## 2. 集成构建

- `:1.20.1-forge:build --rerun-tasks` -> `BUILD SUCCESSFUL`, 产出 `tacz_caliber_ammo-forge-0.1.0+1.20.1.jar`。
- 三 lane(PB-1 数据 / PB-2 序列 / PB-3 匹配+伤害)合并编译零 error; CR-1(`remap=false`)已被全部目标 TacZ 的 mixin 采用(EntityKineticBullet 警告清零)。
- 删除临时自检类后 `compileJava --rerun-tasks` 仍 `BUILD SUCCESSFUL`。

## 3. 运行期(runServer)

- 服务器到 `Done`(多次: 7.897s / 2.940s), 优雅 `stop`, 全程无崩溃。
- mixin 全部 6 个 PREPARE; 启动期应用 5 个到 TacZ 目标类, **零 mixin 错误**:
  - `AmmoItemDataAccessorMixin` -> `com.tacz.guns.item.AmmoItem`
  - `AmmoBoxItemDataAccessorMixin` -> `com.tacz.guns.item.AmmoBoxItem`
  - `EntityKineticBulletMixin` -> `com.tacz.guns.entity.EntityKineticBullet`
  - `CommonDataManagerMixin` -> `com.tacz.guns.resource.manager.CommonDataManager`
  - `AbstractGunItemMixin` -> `com.tacz.guns.api.item.gun.AbstractGunItem`(空骨架)
  - `ModernKineticGunScriptAPIMixin`(PB-2): 懒加载, 首次用枪时才应用(编译期 AP 校验注入点通过, 已 PREPARE)。

## 4. 数据层(PB-1)端到端

用最小种子(见 §6)证明三条加载链真实生效, 计数从 0 变正:

| 加载链 | 空内容 | 加种子后(reload) | 冷启动 |
|---|---|---|---|
| 口径定义 `calibers/*.json`(CaliberDataBootstrap) | 0 | 2 | 2 |
| 弹药 `caliber` 字段(CommonDataManagerMixin AMMO_INDEX) | 0 | 2 (of 24) | 2 |
| 枪 `calibers/flatDamage/percentDamage`(GUN_INDEX) | 0 | 1 (of 54) | 1 |

-> `CommonDataManagerMixin.apply@TAIL` 按 `DataType` 正确分发; `parseAmmo/parseGun` 正确识别我方字段并忽略其余 22 弹/53 枪。原先的 "0" 是"无内容"而非"管线坏"。

## 5. 核心判定逻辑自检(临时自检类, 22/0)

在 `ServerStartedEvent` 上对**真实 reload 后的数据**跑断言, 全绿(不需玩家/开火):

- 弹药->口径: 762x39/556x45 命中, 9mm(未配置)-> `none`。
- **同口径接受 / 异口径拒绝**: ak47 口径集 ∩ 762x39 非空(接受); ∩ 556x45 空(拒绝)。
- 弹药档值: 762x39 base=9.0 / armorIgnore=0.1 / headShot=1.5 / pierce=1。
- 枪修正值: ak47 flat=2.0 / percent=0.1。
- `LoadedAmmoSequence` RLE: 往返 [(762x39,3),(556x45,2)]; `peekHead`=762x39; 顺序出队 3x762x39 后转 556x45; `reconcile` 计数不一致时补齐到 5(逐发按序 + 边界重建)。

自检类已删除(临时门禁产物)。

## 6. 种子测试数据(保留, 供手动 runClient)

均在 gitignore 的 `run/` 下, 非仓库内容:
- `run/tacz/tacz_default_gun/data/tacz/index/ammo/762x39.json` 加 `caliber/baseDamage/armorIgnore/headShotMultiplier/pierce`
- 同上 `ammo/556x45.json`
- `.../index/guns/ak47.json` 加 `calibers/flatDamage/percentDamage`
- `run/world/datapacks/caliber_test/`(口径定义 762x39 / 556x45)

移除方法: 删掉上述新增字段与 `caliber_test` 目录即可(或让 TacZ 重生成默认包)。

## 7. 待手动验证(runClient, 交互)

无头服务器无法给玩家发枪/开火/读伤害数值。请在 runClient 内:
1. `/give` 一把 AK47 + 若干 `tacz:762x39`(必要时用命令混装两种同口径弹以测逐发)。
2. 换弹 -> 检查逐发按序消耗、`shootOnce` 每发用序列头弹种。
3. 对同一目标分别用不同口径弹种开火 -> 伤害按 `base*(1+percent)+flat` 不同、与距离无关。
4. 给 AK47 装 556x45(异口径)-> 应被拒。
5. 删除种子数据/数据包 -> 回退 TacZ 原版行为。
6. 全程无崩溃。
