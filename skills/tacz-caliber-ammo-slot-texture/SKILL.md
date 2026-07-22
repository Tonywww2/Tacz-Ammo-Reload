---
name: tacz-caliber-ammo-slot-texture
description: >-
  Apply custom per-variant ammo slot (inventory icon) textures to TacZ Caliber Ammo
  (tacz_caliber_ammo) bullets and disable the programmatic ammo-code overlay for those
  bullets via lang. USE WHEN adding or replacing 子弹贴图/弹药图标 for a caliber's variants in
  this project, or when a texture already bakes in the code label and you want to turn off
  AmmoCodeDecorator's extra rendering (保留 abbr 同时关闭代号叠加). Covers: naming textures to
  match datagen model ids, the CaliberAmmoDataProvider hasCustomSlot auto per-variant display
  mechanism, adding "<key>.abbr.off": "true" to en_us/zh_cn, running runData, and restarting
  runClient. DO NOT USE for gun/attachment models, 3D bullet geometry, or non-tacz_caliber_ammo
  projects.
---

# TacZ Caliber Ammo — 弹药 slot 贴图 + 关闭代号叠加

把一套自定义子弹图标应用到某口径的各弹种，并（可选）关闭 `AmmoCodeDecorator` 在图标上叠加的代号缩写。

## 速查 (TL;DR)

1. 贴图命名成 datagen 的**弹种 model id**（不是 abbr、不是显示名）：如 `fmj.png` / `mk_255_mod_0.png`。
2. 放到 `src/main/resources/assets/tacz_caliber_ammo/textures/ammo/slot/<calId>/<model>.png`（32x32 PNG）。
3. 跑 `runData` —— datagen 的 `hasCustomSlot` 会自动为有贴图的弹种生成 per-变种 display。
4. （可选）贴图已自带代号 → 在 en_us/zh_cn 给该弹种补一行 `.abbr.off` = `"true"`，关掉程序化代号叠加。
5. **重启 runClient**（display 属服务端 data，F3+T 只重载客户端资源、不重载 data）。

## 背景（机制，先读）

- TacZ 弹药是**单个** `tacz:ammo` item + `AmmoItem.initializeClient` 的 BEWLR（`AmmoItemRenderer`）按 NBT 里的 ammoId 渲染。**没有 per-子弹的 item model 文件**，所以不能靠 item model 字段区分弹种。
- 物品栏（GUI）图标 = 该弹种 display JSON 的 `slot` 贴图（`AmmoItemRenderer` 在 `ItemDisplayContext.GUI` 分支用 `getSlotTextureLocation()` + `SlotModel`）。
- display 由 vanilla 资源系统加载：`ClientAssetsManager` 用 `DisplayManager(..., "display/ammo", ...)` 扫描所有 mod 的 `assets/<ns>/display/ammo/**`。
- 弹种 ammoId = `tacz_caliber_ammo:<calId>/<model>`，index 在 `src/generated/resources/data/tacz_caliber_ammo/index/ammo/<calId>/<model>.json`（datagen `CaliberAmmoDataProvider` 生成，来源 `docs/tarkov_ammo_stats.csv`）。
- **datagen 已内置自动机制（`CaliberAmmoDataProvider.hasCustomSlot`）**：若存在 `src/main/resources/assets/tacz_caliber_ammo/textures/ammo/slot/<calId>/<model>.png`，则该弹种自动生成 per-变种 display（`slot` 指向该贴图，`model`/`texture` 复用原型口径的 3D 几何/uv），并让其 `index.display` 指向 `tacz_caliber_ammo:<calId>_<model>_display`。**所以应用贴图 = 放贴图 + runData**，无需手改 datagen。
- 代号叠加来自 `AmmoCodeDecorator`（客户端 `IItemDecorator`），代号取 lang 键 `ammo.tacz_caliber_ammo.<calId>.<model>.abbr`。并列键 `<abbr 键>.off` = `true`/`1`/`yes`/`on` 时：**保留 abbr 但不渲染**（用于贴图已自带代号、避免重复）。

## 步骤

### 1) 对齐弹种 model id（关键）
贴图文件名必须精确等于 datagen 的弹种 model id，不是 abbr、不是显示名。列出该口径弹种：
`Get-ChildItem "src\generated\resources\data\tacz_caliber_ammo\index\ammo\<calId>" -File | % BaseName`
留意命名差异，用 lang 的 `.abbr` 值辅助核对（例：`mk255`->`mk_255_mod_0`；`sost`->`mk_318_mod_0`（SOST=Mk318）；`wg`->`warmageddon`）。

### 2) 放贴图
- 32x32 PNG（与 TacZ 原版 slot 贴图一致；非正方形会在 quad 上拉伸）。
- 目标：`src/main/resources/assets/tacz_caliber_ammo/textures/ammo/slot/<calId>/<model>.png`（手写资源区，不被 runData 覆盖）。见片段 A。

### 3) （可选）lang 关闭代号叠加
给每个已配贴图的弹种，在 `en_us.json` 与 `zh_cn.json` 各加一行 `"ammo.tacz_caliber_ammo.<calId>.<model>.abbr.off": "true"`，**保留原 abbr 行**。见片段 B。

### 4) 重新生成 + 验证
`.\gradlew.bat :1.20.1-forge:runData --no-daemon` —— 见片段 C 验证：`index.display` 指向 per-变种、生成了对应 display JSON 且 `slot` 正确、lang off 数量正确且 JSON 有效。

### 5) 看效果
**重启 runClient**。`index.display` 属服务端 data，F3+T 只重载客户端资源、不会重载 data；需重进世界/重启才生效。创造栏该口径弹种应显示各自贴图、顶部无程序化代号。

## 注意
- 贴图名匹配 **model id**；源文件名含空格用 `-LiteralPath`。
- `slot` 贴图用子目录 `slot/<calId>/<model>.png`（直接 RL 引用，支持子目录）；datagen 生成的 display 文件用扁平名 `<calId>_<model>_display.json`。
- 不要编辑 `src/generated/resources`（runData 覆盖）。贴图放 `src/main/resources`；机制在 java、文案在 lang 源文件。
- 只 F3+T 没换图标 = data 未重载 → 重启 runClient。

## 命令片段（把 `<calId>` 与映射改成实际值）

### A. 复制并重命名贴图
```powershell
$cal='5_56x45'; $src="assets\556x45"
$dst="src\main\resources\assets\tacz_caliber_ammo\textures\ammo\slot\$cal"
New-Item -ItemType Directory -Force -Path $dst | Out-Null
$map=[ordered]@{ "556x45_fmj.png"="fmj.png"; "556x45_mk255.png"="mk_255_mod_0.png"; "556x45_sost.png"="mk_318_mod_0.png"; "556x45_wg.png"="warmageddon.png" }  # 源名 -> <model>.png
foreach($k in $map.Keys){ $s=Join-Path $src $k; $d=Join-Path $dst $map[$k]
  if(Test-Path -LiteralPath $s){ Copy-Item -LiteralPath $s -Destination $d -Force } else { Write-Output "MISSING: $k" } }
```

### B. lang 插入 .abbr.off（保留缩进/换行；UTF8 无 BOM）
```powershell
$cal='5_56x45'; $models=@('fmj','hp','mk_255_mod_0','mk_318_mod_0','warmageddon')
$enc=New-Object System.Text.UTF8Encoding($false)
foreach($lf in 'en_us.json','zh_cn.json'){
  $p=(Resolve-Path "src\main\resources\assets\tacz_caliber_ammo\lang\$lf").Path
  $raw=[System.IO.File]::ReadAllText($p,[System.Text.Encoding]::UTF8)
  foreach($m in $models){
    $key="ammo.tacz_caliber_ammo.$cal.$m.abbr"; $ekey=[regex]::Escape($key)
    $pattern='(?m)^([ \t]*)("'+$ekey+'"\s*:\s*"[^"]*",)(\r?\n)'
    if([regex]::IsMatch($raw,$pattern)){
      $raw=[regex]::Replace($raw,$pattern,{ param($x)
        $i=$x.Groups[1].Value;$o=$x.Groups[2].Value;$n=$x.Groups[3].Value
        "$i$o$n$i`"${key}.off`": `"true`",$n" },1)
    } else { Write-Output "[$lf] NOMATCH $m" }
  }
  [System.IO.File]::WriteAllText($p,$raw,$enc)
}
```

### C. 验证
```powershell
$gen="src\generated\resources"; $cal='5_56x45'
Get-ChildItem "$gen\data\tacz_caliber_ammo\index\ammo\$cal" -File | ForEach-Object {
  $d=(Get-Content $_.FullName -Raw|Select-String '"display"\s*:\s*"([^"]+)"').Matches.Groups[1].Value
  "{0,-18} -> {1}" -f $_.Name,$d }
Get-ChildItem "$gen\assets\tacz_caliber_ammo\display\ammo" -File | Where-Object { $_.Name -match "^${cal}_" } | % Name
foreach($lf in 'en_us.json','zh_cn.json'){ $p="src\main\resources\assets\tacz_caliber_ammo\lang\$lf"
  $off=(Get-Content $p|Select-String "$cal\.[a-z0-9_]+\.abbr\.off""").Count
  try{Get-Content $p -Raw -Encoding UTF8|ConvertFrom-Json|Out-Null;$j='OK'}catch{$j='INVALID'}
  "$lf off=$off JSON=$j" }
```

## 相关文件
- `src/main/java/com/tacz_caliber_ammo/datagen/CaliberAmmoDataProvider.java` — `hasCustomSlot` + per-变种 display 生成。
- `src/main/java/com/tacz_caliber_ammo/client/AmmoCodeDecorator.java` — 代号叠加 + `.abbr.off` 开关。
- `src/main/resources/assets/tacz_caliber_ammo/lang/{en_us,zh_cn}.json` — 弹种 name/abbr/off。
- `src/main/resources/assets/tacz_caliber_ammo/textures/ammo/slot/<calId>/<model>.png` — 自定义 slot 贴图。
