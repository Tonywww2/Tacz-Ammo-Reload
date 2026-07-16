package com.tacz_caliber_ammo.mixin.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import org.joml.Matrix4f;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.tooltip.ClientGunTooltip;
import com.tacz.guns.item.GunTooltipPart;
import com.tacz_caliber_ammo.caliber.Round;
import com.tacz_caliber_ammo.client.TooltipHandler;
import com.tacz_caliber_ammo.nbt.LoadedAmmoSequence;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * TacZ 枪械 tooltip 两处改造:
 * <ul>
 *   <li>Task 1: 重画 {@code renderText} + 调 {@code getHeight} 精简枪 tooltip: 去 damage/armorIgnore/headShotMultiplier
 *       (伤害归弹药)、去经验等级(levelInfo)与"按 Z 改造"提示(UPGRADES_TIP); 保留 类型(gunType)/移动速度(weight)。
 *       配色(CR-11): 标签(口径/列名/类型标签)浅灰 0xAAAAAA; 值橙 GOLD(口径名/子弹 abbr/容量上限)、白 WHITE(子弹数量/容量当前数);
 *       desc/类型名(青)/移速(红)/pack(蓝) 保留 TacZ 原样式。高度: BASE_INFO -20 / EXTRA_DAMAGE_INFO -20 / UPGRADES_TIP -14。</li>
 *   <li>Task 2: AMMO_INFO 区 = 口径表格 + 弹匣两列表格。口径表格: 列1 首行标签"口径："(其余行空) + 列2/列3 每行两个口径名(移自 TooltipHandler, {@link TooltipHandler#gunCaliberNames}), 以此类推; 无口径显示"未配置口径"单行。
 *       其下为表格: A 列"容量"(列名 + 当前/上限弹量 ammoCountText), B 列"装填"(列名 + 弹匣内弹药明细 "{数量}x {abbr}",
 *       最多 5 行, 超 5 补 "...")。abbr 取本地化键 {@code ammo.<ns>.<路径(/换.)>.abbr}(无则回退路径段大写)。子弹与容量文字为白色。
 *       做法: renderText 画口径行+表格; renderImage 整体 cancel(不画图标); getHeight 把 AMMO_INFO 定高 24 改为 总行数*10;
 *       getText 末尾按口径行/表格两列总宽撑 maxWidth。列名用 I18n 键 {@code tooltip.tacz_caliber_ammo.mag.capacity/loaded}。</li>
 * </ul>
 *
 * <p>CR: 目标 TacZ 客户端类, 类级 {@code remap=false}; renderText/getHeight/renderImage 为 MC 继承方法,
 * 相关注入各自单独 {@code remap=true}。仅客户端 mixin(登记于 mixins.json 的 client 段)。
 */
@Mixin(value = ClientGunTooltip.class, remap = false)
public class ClientGunTooltipMixin {

    @Shadow
    private List<FormattedCharSequence> desc;
    @Shadow
    private MutableComponent gunType;
    @Shadow
    private MutableComponent weight;
    @Shadow
    private MutableComponent packInfo;
    @Shadow
    private MutableComponent ammoCountText;
    @Shadow
    private MutableComponent levelInfo;
    @Shadow
    private MutableComponent tips;
    @Shadow
    private ItemStack gun;
    @Shadow
    private int maxWidth;

    @Shadow
    private boolean shouldShow(GunTooltipPart part) {
        throw new AssertionError();
    }

    /** 弹匣弹药明细最多展示的行数(不含 "..." 与列名/口径)。 */
    @Unique
    private static final int TACZ_CALIBER_AMMO$MAX_LINES = 5;

    /** 表格 A/B 两列之间的水平像素间隔。 */
    @Unique
    private static final int TACZ_CALIBER_AMMO$COLUMN_GAP = 8;

    /** 列名本地化键: A 列(容量) / B 列(装填)。 */
    @Unique
    private static final String TACZ_CALIBER_AMMO$COL_A_KEY = "tooltip.tacz_caliber_ammo.mag.capacity";
    @Unique
    private static final String TACZ_CALIBER_AMMO$COL_B_KEY = "tooltip.tacz_caliber_ammo.mag.loaded";

    /** 口径表格标签键（列１ 首行“口径：”）。 */
    @Unique
    private static final String TACZ_CALIBER_AMMO$CALIBER_LABEL_KEY = "tooltip.tacz_caliber_ammo.caliber_label";

    /** 标签浅灰 0xAAAAAA(口径/列名/类型标签)。数值白/橙由各行组件自带 WHITE/GOLD 样式决定。 */
    @Unique
    private static final int TACZ_CALIBER_AMMO$LABEL = 0xAAAAAA;

    // ---- Task 1a: 重画 renderText, 跳过 damage/armorIgnore/headShotMultiplier 三行并压缩纵向空隙 ----
    @Inject(method = "renderText", at = @At("HEAD"), cancellable = true, remap = true)
    private void taczCaliberAmmo$renderTextTrimmed(Font font, int pX, int pY, Matrix4f matrix4f,
            MultiBufferSource.BufferSource bufferSource, CallbackInfo ci) {
        int yOffset = pY;
        if (this.shouldShow(GunTooltipPart.DESCRIPTION) && this.desc != null) {
            yOffset += 2;
            for (FormattedCharSequence sequence : this.desc) {
                // 描述, TacZ 原为 0xAAAAAA
                font.drawInBatch(sequence, (float) pX, (float) yOffset, 0x555555 , false, matrix4f,
                        bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);
                yOffset += 10;
            }
        }
        if (this.shouldShow(GunTooltipPart.AMMO_INFO)) {
            // 口径表格(上, 列１标签+列２/列３每行两口径) + 两列表格(A=容量, B=装填)。标签浅灰; 口径/容量上限/abbr=橙; 容量当前/数量=白。
            List<Component> bullets = this.taczCaliberAmmo$loadedAmmoLines();
            Component barrel = this.taczCaliberAmmo$barrelLine();
            yOffset = this.taczCaliberAmmo$renderCaliberTable(font, pX, yOffset, matrix4f, bufferSource);
            // 容量/装填 列名
            int colBx = pX + this.taczCaliberAmmo$colAWidth(font) + TACZ_CALIBER_AMMO$COLUMN_GAP;
            font.drawInBatch(Component.translatable(TACZ_CALIBER_AMMO$COL_A_KEY), (float) pX, (float) yOffset,
                    TACZ_CALIBER_AMMO$LABEL, false, matrix4f, bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);
            font.drawInBatch(Component.translatable(TACZ_CALIBER_AMMO$COL_B_KEY), (float) colBx, (float) yOffset,
                    TACZ_CALIBER_AMMO$LABEL, false, matrix4f, bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);
            yOffset += 10;
            // A 列: 容量当前/上限(仅第一数据行)
            if (this.ammoCountText != null) {
                font.drawInBatch(this.taczCaliberAmmo$capacityText(), (float) pX, (float) yOffset,
                        TACZ_CALIBER_AMMO$LABEL, false, matrix4f, bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);
            }
            // B 列: 膛内型号(第一行, 若有) + 弹匣逐发明细
            int bRow = 0;
            if (barrel != null) {
                font.drawInBatch(barrel, (float) colBx, (float) (yOffset + bRow * 10), TACZ_CALIBER_AMMO$LABEL,
                        false, matrix4f, bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);
                bRow++;
            }
            for (int i = 0; i < bullets.size(); i++) {
                font.drawInBatch(bullets.get(i), (float) colBx, (float) (yOffset + bRow * 10),
                        TACZ_CALIBER_AMMO$LABEL, false, matrix4f, bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);
                bRow++;
            }
            yOffset += Math.max(1, bRow) * 10;
        }
        if (this.shouldShow(GunTooltipPart.BASE_INFO)) {
            // 经验等级/damage 已移除; 仅保留 gunType(类型): 标签"类型："浅灰(param), 类型名保留原样式(青)
            if (this.gunType != null) {
                font.drawInBatch(this.gunType, (float) pX, (float) (yOffset += 4), TACZ_CALIBER_AMMO$LABEL, false,
                        matrix4f, bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);
                yOffset += 10;
            }
        }
        // 枪伤害修正(固定/百分比): 枪种之下, 移动速度之上; 仅当该枪配置了非 0 修正时显示
        List<Component> gunDmg = this.taczCaliberAmmo$gunDamageLines();
        if (!gunDmg.isEmpty()) {
            yOffset += 4;
            for (Component line : gunDmg) {
                font.drawInBatch(line, (float) pX, (float) yOffset, TACZ_CALIBER_AMMO$LABEL, false,
                        matrix4f, bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);
                yOffset += 10;
            }
        }
        if (this.shouldShow(GunTooltipPart.EXTRA_DAMAGE_INFO)) {
            // armorIgnore / headShotMultiplier 已移除(归弹药), 仅保留 weight(移动速度): 保留原本逻辑(TacZ 红色样式)
            font.drawInBatch(this.weight, (float) pX, (float) (yOffset += 4), 0xFFFFFF, false, matrix4f, bufferSource,
                    Font.DisplayMode.NORMAL, 0, 0xF000F0);
            yOffset += 10;
        }
        // UPGRADES_TIP(按 Z 改造提示)已移除
        if (this.shouldShow(GunTooltipPart.PACK_INFO) && this.packInfo != null) {
            font.drawInBatch(this.packInfo, (float) pX, (float) (yOffset += 4), 0xFFFFFF, false, matrix4f, bufferSource,
                    Font.DisplayMode.NORMAL, 0, 0xF000F0);
        }
        ci.cancel();
    }

    // ---- 调整 tooltip 高度: BASE_INFO 去 damage -10; EXTRA_DAMAGE_INFO 去两行 -20; AMMO_INFO 定高 24 改为 行数*10 ----
    @Inject(method = "getHeight", at = @At("RETURN"), cancellable = true, remap = true)
    private void taczCaliberAmmo$adjustHeight(CallbackInfoReturnable<Integer> cir) {
        int h = cir.getReturnValueI();
        if (this.shouldShow(GunTooltipPart.BASE_INFO)) {
            h -= 20; // 去 damage + 去经验等级 -> 仅剩 gunType(14)
        }
        if (this.shouldShow(GunTooltipPart.EXTRA_DAMAGE_INFO)) {
            h -= 20;
        }
        if (this.shouldShow(GunTooltipPart.UPGRADES_TIP)) {
            h -= 14; // 去"按 Z 改造"提示
        }
        if (this.shouldShow(GunTooltipPart.AMMO_INFO)) {
            int bRows = (this.taczCaliberAmmo$barrelLine() != null ? 1 : 0)
                    + this.taczCaliberAmmo$loadedAmmoLines().size();
            int rows = 1 + Math.max(1, bRows);
            rows += this.taczCaliberAmmo$caliberRows();
            h += rows * 10 - 24;
        }
        // 枪伤害修正行(枪种之下, 移动速度之上): 每行 10 + 块首 4 间距
        int dmgRows = this.taczCaliberAmmo$gunDamageLines().size();
        if (dmgRows > 0) {
            h += dmgRows * 10 + 4;
        }
        cir.setReturnValue(h);
    }

    // ---- Task 2: 不再画弹药图标(弹匣弹药改由 renderText 纯文本列出), 整体 cancel renderImage ----
    @Inject(method = "renderImage", at = @At("HEAD"), cancellable = true, remap = true)
    private void taczCaliberAmmo$suppressAmmoIcon(Font pFont, int pX, int pY, GuiGraphics guiGraphics, CallbackInfo ci) {
        ci.cancel();
    }

    // ---- 撑宽 tooltip 以容纳口径行与弹匣表格 (getText 是 TacZ 自有名, remap 随类=false) ----
    @Inject(method = "getText", at = @At("TAIL"))
    private void taczCaliberAmmo$widenForAmmoTable(CallbackInfo ci) {
        Font font = Minecraft.getInstance().font;
        // 枪伤害修正行也要参与撑宽(与 AMMO_INFO 是否显示无关)
        for (Component line : this.taczCaliberAmmo$gunDamageLines()) {
            this.maxWidth = Math.max(this.maxWidth, font.width(line));
        }
        if (!this.shouldShow(GunTooltipPart.AMMO_INFO)) {
            return;
        }
        int colB = font.width(Component.translatable(TACZ_CALIBER_AMMO$COL_B_KEY));
        Component barrel = this.taczCaliberAmmo$barrelLine();
        if (barrel != null) {
            colB = Math.max(colB, font.width(barrel));
        }
        for (Component line : this.taczCaliberAmmo$loadedAmmoLines()) {
            colB = Math.max(colB, font.width(line));
        }
        int tableWidth = this.taczCaliberAmmo$colAWidth(font) + TACZ_CALIBER_AMMO$COLUMN_GAP + colB;
        this.maxWidth = Math.max(this.maxWidth, tableWidth);
        this.taczCaliberAmmo$widenForCaliberTable(font);
    }

    /** A 列(容量)宽度 = max(列名, 当前/上限弹量文字)。 */
    @Unique
    private int taczCaliberAmmo$colAWidth(Font font) {
        int w = font.width(Component.translatable(TACZ_CALIBER_AMMO$COL_A_KEY));
        if (this.ammoCountText != null) {
            w = Math.max(w, font.width(this.ammoCountText));
        }
        return w;
    }

    /** 枪口径名列表(移自 TooltipHandler, 过滤 none/universal); 非枪返回空表。 */
    @Unique
    private List<String> taczCaliberAmmo$caliberNames() {
        if (this.gun != null && this.gun.getItem() instanceof IGun ig) {
            return TooltipHandler.gunCaliberNames(ig.getGunId(this.gun));
        }
        return List.of();
    }

    /** 口径区行数: 非枪 0; 无口径 1(未配置行); 有 n 口径 -> ceil(n/2)(每行 2 个)。 */
    @Unique
    private int taczCaliberAmmo$caliberRows() {
        if (this.gun == null || !(this.gun.getItem() instanceof IGun)) {
            return 0;
        }
        int n = this.taczCaliberAmmo$caliberNames().size();
        return n == 0 ? 1 : (n + 1) / 2;
    }

    /** 口径列宽 = max 各口径名宽度(列2/列3 等宽)。 */
    @Unique
    private int taczCaliberAmmo$caliberColWidth(Font font, List<String> cals) {
        int w = 0;
        for (String name : cals) {
            w = Math.max(w, font.width(Component.literal(name)));
        }
        return w;
    }

    /**
     * 画口径区(返回新 yOffset): 非枪不画; 无口径画单行"未配置口径"; 有口径画 3 列表格——
     * 列1 首行标签"口径："(浅灰, 其余行空), 列2/列3 每行两个口径名(橙), 以此类推。
     */
    @Unique
    private int taczCaliberAmmo$renderCaliberTable(Font font, int pX, int yOffset, Matrix4f matrix4f,
            MultiBufferSource.BufferSource bufferSource) {
        if (this.gun == null || !(this.gun.getItem() instanceof IGun)) {
            return yOffset;
        }
        List<String> cals = this.taczCaliberAmmo$caliberNames();
        if (cals.isEmpty()) {
            font.drawInBatch(Component.translatable("tooltip.tacz_caliber_ammo.no_caliber").withStyle(ChatFormatting.GRAY),
                    (float) pX, (float) yOffset, TACZ_CALIBER_AMMO$LABEL, false, matrix4f, bufferSource,
                    Font.DisplayMode.NORMAL, 0, 0xF000F0);
            return yOffset + 10;
        }
        Component label = Component.translatable(TACZ_CALIBER_AMMO$CALIBER_LABEL_KEY);
        int calColW = this.taczCaliberAmmo$caliberColWidth(font, cals);
        int col2x = pX + font.width(label) + TACZ_CALIBER_AMMO$COLUMN_GAP;
        int col3x = col2x + calColW + TACZ_CALIBER_AMMO$COLUMN_GAP;
        int rows = (cals.size() + 1) / 2;
        for (int row = 0; row < rows; row++) {
            float yy = (float) (yOffset + row * 10);
            if (row == 0) {
                font.drawInBatch(label, (float) pX, yy, TACZ_CALIBER_AMMO$LABEL, false, matrix4f, bufferSource,
                        Font.DisplayMode.NORMAL, 0, 0xF000F0);
            }
            font.drawInBatch(Component.literal(cals.get(row * 2)).withStyle(ChatFormatting.GOLD),
                    (float) col2x, yy, TACZ_CALIBER_AMMO$LABEL, false, matrix4f, bufferSource,
                    Font.DisplayMode.NORMAL, 0, 0xF000F0);
            int i3 = row * 2 + 1;
            if (i3 < cals.size()) {
                font.drawInBatch(Component.literal(cals.get(i3)).withStyle(ChatFormatting.GOLD),
                        (float) col3x, yy, TACZ_CALIBER_AMMO$LABEL, false, matrix4f, bufferSource,
                        Font.DisplayMode.NORMAL, 0, 0xF000F0);
            }
        }
        return yOffset + rows * 10;
    }

    /** getText 撑宽: 口径表格总宽(标签 + gap + 口径列 [+ gap + 口径列 若>=2]); 无口径按未配置行宽。 */
    @Unique
    private void taczCaliberAmmo$widenForCaliberTable(Font font) {
        if (this.gun == null || !(this.gun.getItem() instanceof IGun)) {
            return;
        }
        List<String> cals = this.taczCaliberAmmo$caliberNames();
        if (cals.isEmpty()) {
            this.maxWidth = Math.max(this.maxWidth,
                    font.width(Component.translatable("tooltip.tacz_caliber_ammo.no_caliber")));
            return;
        }
        int calColW = this.taczCaliberAmmo$caliberColWidth(font, cals);
        int w = font.width(Component.translatable(TACZ_CALIBER_AMMO$CALIBER_LABEL_KEY))
                + TACZ_CALIBER_AMMO$COLUMN_GAP + calColW;
        if (cals.size() >= 2) {
            w += TACZ_CALIBER_AMMO$COLUMN_GAP + calColW;
        }
        this.maxWidth = Math.max(this.maxWidth, w);
    }

    /** 枪伤害修正行(固定/百分比, 移自 TooltipHandler); 非枪或无修正返回空表。 */
    @Unique
    private List<Component> taczCaliberAmmo$gunDamageLines() {
        if (this.gun != null && this.gun.getItem() instanceof IGun ig) {
            return TooltipHandler.gunDamageModifierLines(ig.getGunId(this.gun));
        }
        return List.of();
    }

    /** 膛内子弹行(枪膛内有弹且记录了弹种时): "膛内 1x <abbr>"; 否则 null。置于口径表格与容量/装填表格之间。 */
    @Unique
    private Component taczCaliberAmmo$barrelLine() {
        if (this.gun == null || !(this.gun.getItem() instanceof IGun ig)) {
            return null;
        }
        if (!ig.hasBulletInBarrel(this.gun)) {
            return null;
        }
        ResourceLocation barrelAmmo = LoadedAmmoSequence.peekBarrelAmmo(this.gun);
        if (barrelAmmo == null) {
            return null;
        }
        return Component.translatable("tooltip.tacz_caliber_ammo.barrel")
                .append(Component.literal(" "))
                .append(Component.literal(taczCaliberAmmo$abbrOf(barrelAmmo)).withStyle(ChatFormatting.YELLOW));
    }

    /** 弹匣内弹药行: 数量(白) + abbr(橙), 按装填顺序, 最多 5 行; 超 5 补 "..."(白); 空弹匣 -> 空表。 */
    @Unique
    private List<Component> taczCaliberAmmo$loadedAmmoLines() {
        List<Round> seq = LoadedAmmoSequence.getSequence(this.gun);
        List<Component> lines = new ArrayList<>();
        for (Round r : seq) {
            if (lines.size() >= TACZ_CALIBER_AMMO$MAX_LINES) {
                break;
            }
            lines.add(Component.literal(r.count() + "x ").withStyle(ChatFormatting.WHITE)
                    .append(Component.literal(taczCaliberAmmo$abbrOf(r.ammoId())).withStyle(ChatFormatting.GOLD)));
        }
        if (seq.size() > TACZ_CALIBER_AMMO$MAX_LINES) {
            lines.add(Component.literal("...").withStyle(ChatFormatting.WHITE));
        }
        return lines;
    }

    /** 容量格: 当前数量(白) + "/上限"(橙); 非 "x/y" 格式(百分比/库存模式)整体白色。 */
    @Unique
    private Component taczCaliberAmmo$capacityText() {
        String s = this.ammoCountText.getString();
        int slash = s.indexOf('/');
        if (slash >= 0) {
            return Component.literal(s.substring(0, slash)).withStyle(ChatFormatting.WHITE)
                    .append(Component.literal(s.substring(slash)).withStyle(ChatFormatting.GOLD));
        }
        return Component.literal(s).withStyle(ChatFormatting.WHITE);
    }

    /** 弹药缩写: 读 {@code ammo.<ns>.<路径(/换.)>.abbr}(下划线换空格); 无该键回退路径末段大写。 */
    @Unique
    private static String taczCaliberAmmo$abbrOf(ResourceLocation ammoId) {
        String key = "ammo." + ammoId.getNamespace() + "." + ammoId.getPath().replace('/', '.') + ".abbr";
        if (I18n.exists(key)) {
            String abbr = I18n.get(key).replace('_', ' ').trim();
            if (!abbr.isEmpty()) {
                return abbr;
            }
        }
        String path = ammoId.getPath();
        int slash = path.lastIndexOf('/');
        String seg = slash >= 0 ? path.substring(slash + 1) : path;
        return seg.replace('_', ' ').toUpperCase(Locale.ROOT);
    }
}
