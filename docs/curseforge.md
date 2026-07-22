# TacZ: Caliber Ammo

为 **Timeless and Classics Zero (TacZ)** 打造的「口径」弹药系统 —— 让子弹的**弹种**、而不是枪，决定性能。
A caliber-based ammunition overhaul for **Timeless and Classics Zero (TacZ)** — where the **ammo variant**, not the gun, defines performance.

**Minecraft 1.20.1 · Forge 47+ · 需要 / Requires TacZ**

---

## 简体中文

### 这个模组做什么

原版 TacZ 里，伤害基本由**枪**决定，弹药之间差别有限。本模组把伤害归属改成「**口径 + 弹种**」：

- **枪**只负责认它能吃哪些口径；
- **伤害、穿甲、爆头倍率、后坐力、精度、初速、弹丸数**全部写在**弹药**上。

于是同一把枪、同一口径下的不同弹种（FMJ / 空尖 / 穿甲 / 曳光 / 霰弹……）各有性能；**混装弹匣还会逐发按装填顺序生效**。

### 你会注意到的变化

- **弹种决定手感**：穿甲弹破甲、空尖弹高伤软目标、霰弹每颗弹丸独立结算。
- **混装逐发**：一个弹匣混装多种弹药，开火时一发一发取用各自的数值。
- **信息提示**：悬停弹药看口径与全部数值，悬停枪看它接受的口径，优劣用颜色区分。
- **制枪台配方**：所有弹种都能在 TacZ 制枪台合成，穿甲越高的弹越贵。
- **退弹按钮**：在枪械改装界面把（混装）弹匣按弹种退回背包。
- **信号弹**：多种颜色的照明信号弹，升空点亮、持续燃烧，落地后仍在原地发光照明。

### 安装

1. 安装 **Forge 1.20.1**；
2. 把 **TacZ** 与本模组的 jar 一起放进 `mods` 文件夹；
3. 启动即可，开箱即用，无需配置。

内置 **37 个口径 / 207 个弹种**，覆盖 TacZ 全部原版枪。所有内容由数据包驱动，**不新增任何物品**（弹药仍是 TacZ 原生弹药物品，靠 NBT 区分弹种）。

### 想深入了解

配置项、给枪包 / 模组开发者的兼容适配、弹药效果脚本（声明式 / Lua / KubeJS）等**完整参考资料，请见本项目 GitHub 仓库的 README**：

> https://github.com/YOUR_ACCOUNT/YOUR_REPO#readme  （发布时替换为你的仓库地址）

---

## English

### What it does

In vanilla TacZ, damage is mostly decided by the **gun**, and ammo types barely differ. This mod moves damage ownership to a **caliber + variant** model:

- a **gun** only declares which caliber(s) it accepts;
- **damage, armor penetration, headshot multiplier, recoil, accuracy, muzzle velocity and pellet count** all live on the **ammo**.

Different variants of the same caliber (FMJ / JHP / AP / tracer / buckshot...) each perform differently, and a **mixed magazine is consumed round-by-round in load order**.

### What you'll notice

- **Variant defines feel**: AP rounds defeat armor, hollow-points shred soft targets, buckshot resolves each pellet independently.
- **Mixed mags, per-round**: load different variants into one magazine; each shot uses its own stats.
- **Tooltips**: hover ammo to see its caliber and full stats, hover a gun to see the calibers it accepts; pros and cons are color-coded.
- **Gun Smith Table recipes**: every variant is craftable; higher penetration costs more.
- **Unload button**: eject a (mixed) magazine back into your inventory, split by variant.
- **Signal flares**: multi-color illumination flares that ignite in mid-air, keep burning, and stay lit where they land.

### Install

1. Install **Forge 1.20.1**;
2. Drop **TacZ** and this mod's jar into your `mods` folder;
3. Launch — it works out of the box, no config needed.

Ships with **37 calibers / 207 ammo variants** covering every vanilla TacZ gun. Everything is datapack-driven and **adds no new items** (ammo stays as TacZ's native ammo item, distinguished by NBT).

### Dig deeper

Full reference — config options, gun-pack / mod compatibility, and ammo effect scripting (declarative / Lua / KubeJS) — lives in the **GitHub repository's README**:

> https://github.com/YOUR_ACCOUNT/YOUR_REPO#readme  (replace with your repo URL before publishing)
