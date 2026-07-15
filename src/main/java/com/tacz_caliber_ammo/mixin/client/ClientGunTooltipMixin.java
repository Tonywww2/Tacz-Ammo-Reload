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
 *   <li>Task 1: 从 tooltip 去掉 damage / armorIgnore / headShotMultiplier 三行(伤害归弹药, 枪不再显示),
 *       并回收其纵向空隙 —— {@code @Inject(HEAD, cancellable)} 重画 {@code renderText}(照原布局但跳过这三行且压缩
 *       yOffset) + {@code @Inject(RETURN)} 缩小 {@code getHeight}(BASE_INFO -10, EXTRA_DAMAGE_INFO -20)。
 *       原 getHeight/renderText 用固定行高, 单纯把组件置空只会留空行, 故必须同时改高度与布局。</li>
 *   <li>Task 2: AMMO_INFO 区改为"口径行 + 两列表格"。第 1 行=枪口径(移自 TooltipHandler, {@link TooltipHandler#gunCaliberLine});
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
    private MutableComponent levelInfo;
    @Shadow
    private MutableComponent gunType;
    @Shadow
    private MutableComponent weight;
    @Shadow
    private MutableComponent tips;
    @Shadow
    private MutableComponent packInfo;
    @Shadow
    private MutableComponent ammoCountText;
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

    // ---- Task 1a: 重画 renderText, 跳过 damage/armorIgnore/headShotMultiplier 三行并压缩纵向空隙 ----
    @Inject(method = "renderText", at = @At("HEAD"), cancellable = true, remap = true)
    private void taczCaliberAmmo$renderTextTrimmed(Font font, int pX, int pY, Matrix4f matrix4f,
            MultiBufferSource.BufferSource bufferSource, CallbackInfo ci) {
        int yOffset = pY;
        if (this.shouldShow(GunTooltipPart.DESCRIPTION) && this.desc != null) {
            yOffset += 2;
            for (FormattedCharSequence sequence : this.desc) {
                font.drawInBatch(sequence, (float) pX, (float) yOffset, 0xAAAAAA, false, matrix4f, bufferSource,
                        Font.DisplayMode.NORMAL, 0, 0xF000F0);
                yOffset += 10;
            }
        }
        if (this.shouldShow(GunTooltipPart.AMMO_INFO)) {
            // AMMO_INFO 改为: 口径行(上) + 两列表格(A=容量: 列名/当前上限; B=装填: 列名/弹匣弹药明细)。子弹与容量文字白色。
            Component caliber = this.taczCaliberAmmo$caliberLine();
            List<String> bullets = this.taczCaliberAmmo$loadedAmmoLines();
            if (caliber != null) {
                font.drawInBatch(caliber, (float) pX, (float) yOffset, 0xFFFFFF, false, matrix4f, bufferSource,
                        Font.DisplayMode.NORMAL, 0, 0xF000F0);
                yOffset += 10;
            }
            int colBx = pX + this.taczCaliberAmmo$colAWidth(font) + TACZ_CALIBER_AMMO$COLUMN_GAP;
            font.drawInBatch(Component.translatable(TACZ_CALIBER_AMMO$COL_A_KEY), (float) pX, (float) yOffset, 0xAAAAAA,
                    false, matrix4f, bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);
            font.drawInBatch(Component.translatable(TACZ_CALIBER_AMMO$COL_B_KEY), (float) colBx, (float) yOffset,
                    0xAAAAAA, false, matrix4f, bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);
            if (this.ammoCountText != null) {
                font.drawInBatch(this.ammoCountText, (float) pX, (float) (yOffset + 10), 0xFFFFFF, false, matrix4f,
                        bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);
            }
            for (int i = 0; i < bullets.size(); i++) {
                font.drawInBatch(Component.literal(bullets.get(i)), (float) colBx, (float) (yOffset + 10 + i * 10),
                        0xFFFFFF, false, matrix4f, bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);
            }
            yOffset += (1 + Math.max(1, bullets.size())) * 10;
        }
        if (this.shouldShow(GunTooltipPart.BASE_INFO)) {
            font.drawInBatch(this.levelInfo, (float) pX, (float) (yOffset += 4), 0x777777, false, matrix4f,
                    bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);
            yOffset += 10;
            if (this.gunType != null) {
                font.drawInBatch(this.gunType, (float) pX, (float) yOffset, 0x777777, false, matrix4f, bufferSource,
                        Font.DisplayMode.NORMAL, 0, 0xF000F0);
                yOffset += 10;
            }
            // damage 行已移除(伤害归弹药)
        }
        if (this.shouldShow(GunTooltipPart.EXTRA_DAMAGE_INFO)) {
            // armorIgnore / headShotMultiplier 已移除(归弹药), 仅保留 weight(移动速度)
            font.drawInBatch(this.weight, (float) pX, (float) (yOffset += 4), 0xFFFFFF, false, matrix4f, bufferSource,
                    Font.DisplayMode.NORMAL, 0, 0xF000F0);
            yOffset += 10;
        }
        if (this.shouldShow(GunTooltipPart.UPGRADES_TIP)) {
            font.drawInBatch(this.tips, (float) pX, (float) (yOffset += 4), 0xFFFFFF, false, matrix4f, bufferSource,
                    Font.DisplayMode.NORMAL, 0, 0xF000F0);
            yOffset += 10;
        }
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
            h -= 10;
        }
        if (this.shouldShow(GunTooltipPart.EXTRA_DAMAGE_INFO)) {
            h -= 20;
        }
        if (this.shouldShow(GunTooltipPart.AMMO_INFO)) {
            int rows = 1 + Math.max(1, this.taczCaliberAmmo$loadedAmmoLines().size());
            if (this.taczCaliberAmmo$caliberLine() != null) {
                rows += 1;
            }
            h += rows * 10 - 24;
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
        if (!this.shouldShow(GunTooltipPart.AMMO_INFO)) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        int colB = font.width(Component.translatable(TACZ_CALIBER_AMMO$COL_B_KEY));
        for (String line : this.taczCaliberAmmo$loadedAmmoLines()) {
            colB = Math.max(colB, font.width(line));
        }
        int tableWidth = this.taczCaliberAmmo$colAWidth(font) + TACZ_CALIBER_AMMO$COLUMN_GAP + colB;
        this.maxWidth = Math.max(this.maxWidth, tableWidth);
        Component caliber = this.taczCaliberAmmo$caliberLine();
        if (caliber != null) {
            this.maxWidth = Math.max(this.maxWidth, font.width(caliber));
        }
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

    /** 枪口径展示行(移自 TooltipHandler); 非枪或 gunId 缺失返回 null。 */
    @Unique
    private Component taczCaliberAmmo$caliberLine() {
        if (this.gun != null && this.gun.getItem() instanceof IGun ig) {
            return TooltipHandler.gunCaliberLine(ig.getGunId(this.gun));
        }
        return null;
    }

    /** 弹匣内弹药文本行 "{数量}x {abbr}"(按装填顺序, 最多 5 行); 段数超过 5 时末尾补一行 "..."; 空弹匣 -> 空表。 */
    @Unique
    private List<String> taczCaliberAmmo$loadedAmmoLines() {
        List<Round> seq = LoadedAmmoSequence.getSequence(this.gun);
        List<String> lines = new ArrayList<>();
        for (Round r : seq) {
            if (lines.size() >= TACZ_CALIBER_AMMO$MAX_LINES) {
                break;
            }
            lines.add(r.count() + "x " + taczCaliberAmmo$abbrOf(r.ammoId()));
        }
        if (seq.size() > TACZ_CALIBER_AMMO$MAX_LINES) {
            lines.add("...");
        }
        return lines;
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
