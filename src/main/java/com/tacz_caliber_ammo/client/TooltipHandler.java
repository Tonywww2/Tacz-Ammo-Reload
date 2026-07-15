package com.tacz_caliber_ammo.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz_caliber_ammo.caliber.AmmoProfile;
import com.tacz_caliber_ammo.caliber.CaliberManager;

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
            appendAmmo(ammo.getAmmoId(stack), lines);
        } else if (stack.getItem() instanceof IAmmoBox box) {
            appendAmmo(box.getAmmoId(stack), lines);
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
            if (c != null && !CaliberManager.NONE.equals(c)) {
                names.add(CaliberManager.getCaliber(c).name());
            }
        }
        if (names.isEmpty()) {
            return noCaliberLine();
        }
        return Component.translatable("tooltip.tacz_caliber_ammo.caliber", String.join(", ", names))
                .withStyle(ChatFormatting.GRAY);
    }

    /** 弹药/弹药盒：显示口径 + 弹道档（若已配置）；未配置显示"未配置口径"。 */
    private static void appendAmmo(ResourceLocation ammoId, List<Component> lines) {
        if (ammoId == null) {
            return;
        }
        ResourceLocation caliber = CaliberManager.getAmmoCaliber(ammoId);
        if (caliber == null || CaliberManager.NONE.equals(caliber)) {
            lines.add(noCaliberLine());
            return;
        }
        lines.add(Component.translatable("tooltip.tacz_caliber_ammo.caliber", CaliberManager.getCaliber(caliber).name())
                .withStyle(ChatFormatting.GRAY));
        AmmoProfile p = CaliberManager.getAmmoProfile(ammoId);
        if (p != null) {
            lines.add(Component.translatable("tooltip.tacz_caliber_ammo.damage", fmt(p.baseDamage()))
                    .withStyle(ChatFormatting.DARK_GRAY));
            lines.add(Component.translatable("tooltip.tacz_caliber_ammo.armor_ignore", percent(p.armorIgnore()))
                    .withStyle(ChatFormatting.DARK_GRAY));
            lines.add(Component.translatable("tooltip.tacz_caliber_ammo.headshot", fmt(p.headShotMultiplier()))
                    .withStyle(ChatFormatting.DARK_GRAY));
            lines.add(Component.translatable("tooltip.tacz_caliber_ammo.pierce", p.pierce())
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static Component noCaliberLine() {
        return Component.translatable("tooltip.tacz_caliber_ammo.no_caliber")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
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
}
