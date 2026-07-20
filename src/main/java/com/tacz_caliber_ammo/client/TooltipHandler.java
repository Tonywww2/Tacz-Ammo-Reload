package com.tacz_caliber_ammo.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz_caliber_ammo.caliber.AmmoEffects;
import com.tacz_caliber_ammo.caliber.AmmoProfile;
import com.tacz_caliber_ammo.caliber.CaliberManager;
import com.tacz_caliber_ammo.caliber.GunDamageModifier;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 弹药/枪 tooltip 追加（订阅 Forge ItemTooltipEvent，见 {@link ClientDisplayBootstrap}）。PC-1 实现。
 * 悬停枪 -> 口径; 悬停弹药/弹药盒 -> 口径 + baseDamage/armorIgnore/headShot/pierce;
 * 口径为 none（含 TacZ 原版未配置）-> "未配置口径"标记。数据由 {@link CaliberManager}（PB-1）只读提供。
 */
public final class TooltipHandler {

    private TooltipHandler() {
    }

    /** 由 ItemTooltipEvent 调用：按物品类型追加口径/弹道信息。枪的口径已移到弹匣表格上方(ClientGunTooltipMixin)，此处只处理弹药/弹药盒。 */
    public static void appendTooltip(ItemStack stack, List<Component> lines) {
        if (stack.isEmpty()) {
            return;
        }
        if (stack.getItem() instanceof IAmmo ammo) {
            appendAmmo(ammo.getAmmoId(stack), lines, stack.getMaxStackSize());
        } else if (stack.getItem() instanceof IAmmoBox box) {
            appendAmmo(box.getAmmoId(stack), lines, 0);
        }
    }

    /**
     * 枪的口径展示行（过滤 none；无配置口径返回"未配置口径"行）；供弹匣表格上方渲染复用（{@code ClientGunTooltipMixin}）。
     * gunId==null 返回 null。
     */
    public static Component gunCaliberLine(ResourceLocation gunId) {
        if (gunId == null) {
            return null;
        }
        List<String> names = new ArrayList<>();
        for (ResourceLocation c : CaliberManager.getGunCalibers(gunId)) {
            if (c != null && !CaliberManager.NONE.equals(c) && !CaliberManager.UNIVERSAL.equals(c)) {
                names.add(CaliberManager.getCaliber(c).name());
            }
        }
        if (names.isEmpty()) {
            return noCaliberLine();
        }
        return Component.translatable("tooltip.tacz_caliber_ammo.caliber",
                Component.literal(String.join(", ", names)).withStyle(ChatFormatting.GOLD))
                .withStyle(ChatFormatting.GRAY);
    }

    /** 枪的口径名列表（过滤 none/universal，有序）；空 = 无配置口径。供 tooltip 表格排列（{@code ClientGunTooltipMixin}）。 */
    public static List<String> gunCaliberNames(ResourceLocation gunId) {
        List<String> names = new ArrayList<>();
        if (gunId == null) {
            return names;
        }
        for (ResourceLocation c : CaliberManager.getGunCalibers(gunId)) {
            if (c != null && !CaliberManager.NONE.equals(c) && !CaliberManager.UNIVERSAL.equals(c)) {
                names.add(CaliberManager.getCaliber(c).name());
            }
        }
        return names;
    }

    /**
     * 枪的伤害修正展示行（固定伤害 + 百分比伤害，均来自 {@link CaliberManager#getGunModifier}）。
     * 仅列出非 0 项：固定伤害显示带符号绝对值（+0.5），百分比显示带符号百分数（+5%）；正=绿(增伤)、负=红。
     * 供枪 tooltip 在"枪种之下、移动速度之上"渲染（{@code ClientGunTooltipMixin}）。gunId==null 或无修正返回空表。
     */
    public static List<Component> gunDamageModifierLines(ResourceLocation gunId) {
        List<Component> out = new ArrayList<>();
        if (gunId == null) {
            return out;
        }
        GunDamageModifier mod = CaliberManager.getGunModifier(gunId);
        if (mod == null) {
            return out;
        }
        if (mod.flatDamage() != 0f) {
            out.add(signedFlatLine("tooltip.tacz_caliber_ammo.gun_flat_damage", mod.flatDamage()));
        }
        if (mod.percentDamage() != 0f) {
            out.add(signedValueLine("tooltip.tacz_caliber_ammo.gun_percent_damage", mod.percentDamage() * 100f, false));
        }
        return out;
    }

    /**
     * 弹药/弹药盒：显示口径 + 弹道档（若已配置）；未配置显示"未配置口径"。标签深灰、具体数值白色；
     * 插到 index 1（物品名下方第一行，与枪口径同在首行位置）。
     */
    private static void appendAmmo(ResourceLocation ammoId, List<Component> lines, int stackSize) {
        if (ammoId == null) {
            return;
        }
        List<Component> out = new ArrayList<>();
        ResourceLocation caliber = CaliberManager.getAmmoCaliber(ammoId);
        if (caliber == null || CaliberManager.NONE.equals(caliber)) {
            out.add(noCaliberLine());
        } else {
            // 万用口径用可本地化键显示（万用 / Universal）；其余口径沿用 calibers/*.json 的 name 字面量
            Component caliberName = CaliberManager.UNIVERSAL.equals(caliber)
                    ? Component.translatable("caliber.tacz_caliber_ammo.universal").withStyle(ChatFormatting.GOLD)
                    : Component.literal(CaliberManager.getCaliber(caliber).name()).withStyle(ChatFormatting.GOLD);
            out.add(Component.translatable("tooltip.tacz_caliber_ammo.caliber", caliberName)
                    .withStyle(ChatFormatting.GRAY));
            AmmoProfile p = CaliberManager.getAmmoProfile(ammoId);
            if (p != null) {
                out.add(valueLine("tooltip.tacz_caliber_ammo.damage", fmt(p.baseDamage())));
                out.add(valueLine("tooltip.tacz_caliber_ammo.armor_ignore", percent(p.armorIgnore())));
                out.add(valueLine("tooltip.tacz_caliber_ammo.headshot", fmt(p.headShotMultiplier())));
                out.add(valueLine("tooltip.tacz_caliber_ammo.pierce", Integer.toString(p.pierce())));
                out.add(valueLine("tooltip.tacz_caliber_ammo.ballistic_coefficient", fmt(p.ballisticCoefficient())));
                if (p.speed() > 0f) {
                    out.add(valueLine("tooltip.tacz_caliber_ammo.speed", fmt(p.speed()) + " m/s"));
                }
                if (p.pelletCount() > 1) {
                    out.add(valueLine("tooltip.tacz_caliber_ammo.pellet_count", Integer.toString(p.pelletCount())));
                }
                out.add(signedValueLine("tooltip.tacz_caliber_ammo.recoil", p.recoilModifier(), true));
                out.add(signedValueLine("tooltip.tacz_caliber_ammo.accuracy", p.accuracyModifier(), false));
                appendEffects(p.effects(), out);
            }
        }
        if (stackSize > 0) {
            out.add(valueLine("tooltip.tacz_caliber_ammo.stack_size", Integer.toString(stackSize)));
        }
        lines.addAll(Math.min(1, lines.size()), out);
    }

    /** 弹药特殊效果指示（命中爆炸/点燃/额外击退/自定义脚本）：每项一行金色，便于识别特种弹。 */
    private static void appendEffects(AmmoEffects fx, List<Component> out) {
        if (fx == null) {
            return;
        }
        if (fx.explosion() != null && fx.explosion().enabled()) {
            out.add(Component.translatable("tooltip.tacz_caliber_ammo.effect.explosion").withStyle(ChatFormatting.GOLD));
        }
        if (fx.ignite() != null && (fx.ignite().igniteEntity() || fx.ignite().igniteBlock())) {
            out.add(Component.translatable("tooltip.tacz_caliber_ammo.effect.ignite").withStyle(ChatFormatting.GOLD));
        }
        if (fx.knockback() != null) {
            out.add(Component.translatable("tooltip.tacz_caliber_ammo.effect.knockback").withStyle(ChatFormatting.GOLD));
        }
        if (fx.hasScript()) {
            out.add(Component.translatable("tooltip.tacz_caliber_ammo.effect.script").withStyle(ChatFormatting.GOLD));
        }
    }

    /** "标签：值" 一行：标签浅灰、值橙色。 */
    private static Component valueLine(String key, String value) {
        return Component.translatable(key, Component.literal(value).withStyle(ChatFormatting.GOLD))
                .withStyle(ChatFormatting.GRAY);
    }

    /**
     * 同 valueLine，但数值按"好坏"着色：好绿、坏红、0 白；数字仍显示原始带符号值。
     * {@code higherIsWorse=true} 时正数视为"坏"（如后坐力：正=更多后坐力=坏，正红负绿）；false 时正=好（如精度）。
     */
    private static Component signedValueLine(String key, float v, boolean higherIsWorse) {
        float goodness = higherIsWorse ? -v : v;
        ChatFormatting color = goodness > 0 ? ChatFormatting.GREEN : (goodness < 0 ? ChatFormatting.RED : ChatFormatting.WHITE);
        return Component.translatable(key, Component.literal(signedPercent(v)).withStyle(color))
                .withStyle(ChatFormatting.GRAY);
    }

    /** 同 signedValueLine，但数值是带符号的绝对值（+0.5 / -1，无 %）；正绿负红 0 白。供枪固定伤害修正显示。 */
    private static Component signedFlatLine(String key, float v) {
        ChatFormatting color = v > 0 ? ChatFormatting.GREEN : (v < 0 ? ChatFormatting.RED : ChatFormatting.WHITE);
        String txt = (v > 0 ? "+" : "") + fmt(v);
        return Component.translatable(key, Component.literal(txt).withStyle(color))
                .withStyle(ChatFormatting.GRAY);
    }

    private static Component noCaliberLine() {
        return Component.translatable("tooltip.tacz_caliber_ammo.no_caliber")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
    }

    /** 去尾零的浮点格式（5.0 -> "5", 7.5 -> "7.5"）。 */
    private static String fmt(float v) {
        if (v == Math.rint(v)) {
            return Integer.toString((int) v);
        }
        String s = String.format(Locale.ROOT, "%.2f", v);
        while (s.endsWith("0")) {
            s = s.substring(0, s.length() - 1);
        }
        if (s.endsWith(".")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    /** 0-1 -> 百分比（0.5 -> "50%"）。 */
    private static String percent(float v) {
        return fmt(v * 100f) + "%";
    }

    /** 带符号百分比（+8%、-15%、+0%）；供后坐力/精度修正显示。0 也显示 "+0%"。 */
    private static String signedPercent(float v) {
        return (v >= 0 ? "+" : "") + fmt(v) + "%";
    }
}
