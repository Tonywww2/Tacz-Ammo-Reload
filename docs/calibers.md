# TacZ 口径总表 (Caliber Master Table)

> 命名空间前缀省略：口径 id 实际为 `tacz_caliber_ammo:<id>`。
> 口径 id 规则：小写、去 `mm`、`.`/空格/`-`/`/` -> `_`、开头 `.` -> `d`、剥离中文/国别限定词。
> `原TacZ弹药` / `使用枪械` 为空(—)者：`Ammo.csv` 里有该口径但 TacZ 无对应原版枪械(纯新增内容)。
> 型号来源：`Ammo.csv`(用户提供) + 6 个 CSV 未覆盖口径的 Wikipedia 查证(.22 WMR / .30-06 / .45-70 / .500 S&W / 5.8x42 / 7.92x57)。
> 使用枪械均为 `tacz:` 命名空间；共 54 把原版枪、19 个 TacZ 原版弹药、37 个口径。

| 类别 | 口径 | 口径id | 原TacZ弹药 | 子弹型号 | 使用枪械(tacz:) |
|---|---|---|---|---|---|
| 手枪 | .22 WMR | d22_wmr | tacz:22wmr | JHP、FMJ、V-Max、Snake Shot | taurus943 |
| 手枪 | 7.62x25mm 托卡列夫 | 7_62x25 | — | M856A1、M855A1、M995、LRNPC、LRN、FMJ43、AKBS、P gl、PT、Pst | — |
| 手枪 | 9x18mm 马卡洛夫 | 9x18 | — | SP8、SP7、PSV、P、PSO、PS PPO、PRS、PPe、PPT、Pst、RG028、BZhT、PstM(PMM)、PBM | — |
| 手枪 | 9x19mm 帕拉贝伦 | 9x19 | tacz:9mm | RIP、QuakeMaker、PSO gzh、Luger CCI、绿色曳光弹、FMJ M882、Pst、AP 6.3、7N31 | b93r、cz75、glock_17、hk_mp5a5、m9a4、uzi |
| 手枪 | 9x21mm Gyurza | 9x21 | — | 7N42凿刀、7U4、SP12、SP11、SP10、SP13 | — |
| 手枪 | .357 马格南 | d357 | tacz:357mag | Soft Point、Hollow Point、JHP、FMJ | deagle_golden、rhino357 |
| 手枪 | .45 ACP | d45_acp | tacz:45acp | RIP、Hydra-Shok、Lasermatch FMJ、FMJ、AP | hk_mk23、m1911、p320、ump45、vector45 |
| 手枪 | .50 AE | d50_ae | tacz:50ae | Hawk JSP、实心铜弹、JHP、FMJ | deagle、timeless50 |
| 手枪 | .500 S&W Magnum | d500_magnum | tacz:500mag | SP(软尖)、JHP、hardcast(硬铸铅)、XTP | taurus500 |
| PDW | 4.6x30mm HK | 4_6x30 | — | Action SX、Subsonic SX、JSP SX、FMJ SX、AP SX | — |
| PDW | 5.7x28mm FN | 5_7x28 | tacz:57x28 | R37.F、R37.X、SS198LF、SS197SR、SB193、L191、SS190 | p90 |
| 步枪 | 5.45x39mm | 5_45x39 | — | HP、PRS、SP、US、T、FMJ、PS、PP、BP、BT、7N40、BS、7N39针刺 | — |
| 步枪 | 5.56x45mm NATO | 5_56x45 | tacz:556x45 | Warmage、HP、Mk 255 Mod 0、Mk 318 Mod 0 (SOST)、M856、FMJ、M855、M856A1、M855A1、M995、SSA AP | aug、g36k、hk416d、m16a1、m16a4、m249、m4a1、scar_l、spr15hb |
| 步枪 | 5.8x42mm | 5_8x42 | tacz:58x42 | DBP10(通用)、DBP87(标准)、DBP88(重弹)、DBU141(狙击)、DBP191(新一代) | qbz_191、qbz_95 |
| 步枪 | 6.8x51mm (.277 FURY) | 6_8x51 | — | SIG FMJ、SIG Hybrid | — |
| 步枪 | .300 Blackout | d300_blk | — | Whisper、V-Max、BPZ FMJ、M62 曳光弹、CBJ、AP | — |
| 步枪 | .30-06 斯普林菲尔德 | d30_06 | tacz:30_06 | M2 Ball(FMJ)、M2 AP(穿甲)、M1 Tracer(曳光)、M72 Match、SP(软尖) | lonetrail、m700 |
| 步枪 | .308 Marlin Express | d308_marlin | — | ME LOKT、ME | — |
| 步枪 | 7.62x39mm | 7_62x39 | tacz:762x39 | HP、SP、FMJ、US、T45M1、PS、PP gzh、BP、MAI AP | ak47、rpk、sks_tactical、type_81 |
| 步枪 | 7.62x51mm NATO | 7_62x51 | tacz:308 | M80A1、Ultra Nosler、TCW SP、BPZ FMJ、M80、M62 曳光弹、M61、M993 | fn_evolys、fn_fal、hk_g3、minigun、mk14、scar_h |
| 步枪 | 7.62x54R | 7_62x54r | — | HP BT、SP BT、FMJ、T-46M、LPS Gzh、7N1 狙击弹、7BT1、SNB、7N37 | — |
| 步枪 | 7.92x57mm 毛瑟 | 7_92x57_mauser | tacz:792x57 | sS(重尖球弹)、S.m.K.(钢芯穿甲)、S.m.K. L'spur(穿甲曳光)、S.m.E.(软钢芯)、P.m.K.(磷芯穿甲燃烧) | kar98 |
| 步枪 | .338 拉普阿马格南 | d338 | tacz:338 | TAC-X、UCW、FMJ、AP | ai_awp |
| 步枪 | .366 TKM | d366_tkm | — | Geksa、FMJ、EKO、AP | — |
| 步枪 | .45-70 Government | d45_70 | tacz:45_70 | 405gr 铅弹(SP)、300gr JHP、hardcast(硬铸铅)、Forager(木弹头霰粒) | springfield1873 |
| 步枪 | 9x39mm | 9x39 | — | FMJ、SP-5、SPP、SP-6、PAB-9、BP | — |
| 步枪 | 9.3x64mm | 9_3x64 | — | SP、FMJ、7N33 | — |
| 步枪 | 12.7x55mm | 12_7x55 | — | PS12A、PS12、PS12B | — |
| 步枪 | .50 BMG | d50_bmg | tacz:50bmg | M903 SLAP、HP、M21、M33 | m107、m95 |
| 霰弹枪 | 12/70 | 12_70 | tacz:12g | 5.25mm鹿弹、Magnum 8.5mm鹿弹、Express 6.5mm鹿弹、7mm鹿弹、食人鱼、箭形弹、RIP、SuperFormance 空尖独头弹、Grizzly 40 独头弹、Copper Sabot Premier 空尖独头弹、铅头弹、Dual Sabot 独头弹、Poleva-3 独头弹、FTX Custom Lite 独头弹、Poleva-6U 独头弹、.50 BMG 简易独头弹、AP-20 穿甲独头弹 | aa12、db_long、db_short、m1014、m870、spas_12 |
| 霰弹枪 | 20/70 | 20_70 | — | 箭形弹、危险猎物独头弹 (DGS)、TSS 穿甲独头弹、7.5mm鹿弹、5.6mm鹿弹、6.2mm鹿弹、7.3mm鹿弹、Devastator 独头弹、Poleva-3 独头弹、Star 独头弹、Poleva-6U 独头弹 | — |
| 霰弹枪 | 23x75mm R | 23x75_r | — | 红星闪光弹、破片-25霰弹、破片-10霰弹、破障独头弹 | — |
| 重机枪 | 12.7x108mm | 12_7x108 | — | B-32、BZT-44M | — |
| 榴弹 | 30x29mm | 30x29 | — | VOG-30 | — |
| 榴弹 | 40x46mm | 40x46 | tacz:40mm | M386(HE)、M576(MP-APERS)、M381(HE)、M406(HE)、M441(HE)、M433 (HEDP) | m320 |
| 榴弹 | 40mm VOG-25 | 40_vog_25 | — | VOG-25 榴弹 | — |
| 其他 | 72.5mm 火箭弹 | 72_5 | tacz:rpg_rocket | ShG-2 反人员火箭弹 | rpg7 |

## 备注

- **有原版枪械的口径 = 19 个**(需迁移)：9x19、.357、.45 ACP、.50 AE、5.7x28、5.56x45、7.62x39、7.62x51、.338、.50 BMG、12/70、40x46、72.5mm rocket、.22 WMR、.30-06、.45-70、.500 S&W、5.8x42、7.92x57。
- **无原版枪械的口径 = 18 个**(纯新增弹药内容，暂无 TacZ 枪使用)。
- `tacz:308` 归入 **7.62x51mm NATO**(其枪均为 7.62 战斗步枪)；`.308 Marlin Express` 为另一独立口径(无枪)。
- `tacz:40mm` 归入 **40x46mm**(m320 为北约 40x46 榴弹发射器)；`40mm VOG-25` 为另一独立口径(无枪)。
- 口径 id 为建议值，可再调整(尤其带 `d` 前缀者与去限定词者)。
- **模式**：本项目**弃用 枪->单一弹药，改为 枪->口径**；枪不再引用弹药 id，而是声明口径；弹药经 `caliber` 字段(应与其所在 `口径id/` 子目录一致)归入口径。
- **弹药命名规范(已核实 TacZ 递归扫描)**：弹药按 `口径id/型号id` 子目录组织，注册 id = `tacz_caliber_ammo:口径id/型号id`，文件放 `data/tacz_caliber_ammo/index/ammo/口径id/型号id.json`。例：`.../index/ammo/5_56x45/m855.json` -> `tacz_caliber_ammo:5_56x45/m855`。型号id 用与口径同一套规则(小写/去mm/`.`&空格&`-`->`_`/剥中文；如 `M855`->`m855`、`S.m.K.`->`smk`、`sS`->`ss`)。引用处(枪口径映射等)用完整含斜杠 id。
