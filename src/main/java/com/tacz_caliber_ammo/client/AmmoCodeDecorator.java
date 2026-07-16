package com.tacz_caliber_ammo.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.item.IAmmo;
import com.tacz_caliber_ammo.config.ModConfig;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.IItemDecorator;
import net.minecraftforge.client.event.RegisterItemDecorationsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 在子弹物品图标顶边用文字渲染其"代号缩写"——由弹药 id 映射到本地化缩写键
 * {@code ammo.<命名空间>.<路径(/换为.)>.abbr} 取得（下划线渲染为空格；无该键则不渲染）。
 * 例 {@code tacz_caliber_ammo:5_56x45/m855} -> 键 {@code ammo.tacz_caliber_ammo.5_56x45.m855.abbr} -> {@code M855}。
 * 另可用并列开关键 {@code <abbr 键>.off}=true（true/1/yes/on）在保留 abbr 的同时关闭特定弹药的代号渲染。
 *
 * <p>机制：Forge {@link IItemDecorator} + {@link RegisterItemDecorationsEvent}（MOD 总线，仅客户端）。
 * 遵循 §2 自注册约定（@Mod.EventBusSubscriber 自动发现，主类 {@code TaczCaliberAmmo} 不改）。
 * 仅作用于 TacZ 弹药物品（{@link IAmmo}；弹药盒 IAmmoBox 不含，符合"子弹"语义）。
 */
@Mod.EventBusSubscriber(modid = "tacz_caliber_ammo", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class AmmoCodeDecorator implements IItemDecorator {

    private static final AmmoCodeDecorator INSTANCE = new AmmoCodeDecorator();

    /** 顶边文字颜色（白色，配合阴影在各色图标上均可读）。 */
    private static final int COLOR = 0xFFFFFF;

    /** 物品图标边长（GUI 像素）。 */
    private static final float ICON = 16.0f;

    /** 固定字号缩放（相对原版字体；0.5 = 半字号）。所有代号统一此大小，不随长度自适应。 */
    private static final float FONT_SCALE = 0.5f;

    private AmmoCodeDecorator() {
    }

    /** 给所有 TacZ 弹药物品（IAmmo）挂上本装饰器。 */
    @SubscribeEvent
    public static void onRegisterItemDecorations(RegisterItemDecorationsEvent event) {
        for (Item item : ForgeRegistries.ITEMS) {
            if (item instanceof IAmmo) {
                event.register(item, INSTANCE);
            }
        }
    }

    @Override
    public boolean render(GuiGraphics guiGraphics, Font font, ItemStack stack, int xOffset, int yOffset) {
        String code = codeOf(stack);
        if (code == null) {
            return false;
        }
        int width = font.width(code);
        if (width <= 0) {
            return false;
        }
        // 固定字号（不随代号长度自适应）；水平居中，纵向贴顶边（y=0）。
        float scale = FONT_SCALE;
        int localX = Math.round((ICON / scale - width) / 2.0f);

        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        // z=200 使文字浮于物品模型之上（与原版数量文字一致）。
        pose.translate(xOffset, yOffset, 200.0);
        pose.scale(scale, scale, 1.0f);
        guiGraphics.drawString(font, code, localX, 0, COLOR, true);
        pose.popPose();
        return true;
    }

    /**
     * 取弹药代号缩写。先看配置 {@link ModConfig#ammoCodeDisplay()}：
     * <ul>
     *   <li>{@code NEVER} —— 直接返回 null（始终不渲染）；</li>
     *   <li>{@code ALWAYS} —— 有 abbr 键用其值、否则用弹药 id 末段作兜底，且忽略 {@code .off} 开关（始终渲染）；</li>
     *   <li>{@code DEFAULT} —— 跟随 lang：读缩写键 {@code ammo.<命名空间>.<路径(/换为.)>.abbr}，
     *       无该键返回 null，并列键 {@code <abbr 键>.off} 为真时也返回 null。</li>
     * </ul>
     * 下划线一律替换为空格；空串返回 null（不渲染）。
     */
    private static String codeOf(ItemStack stack) {
        ModConfig.AmmoCodeDisplay mode = ModConfig.ammoCodeDisplay();
        if (mode == ModConfig.AmmoCodeDisplay.NEVER) {
            return null; // 配置：始终关闭，无视 lang
        }
        if (!(stack.getItem() instanceof IAmmo ammo)) {
            return null;
        }
        ResourceLocation ammoId = ammo.getAmmoId(stack);
        if (ammoId == null) {
            return null;
        }
        String key = "ammo." + ammoId.getNamespace() + "." + ammoId.getPath().replace('/', '.') + ".abbr";
        boolean hasAbbr = I18n.exists(key);
        if (mode == ModConfig.AmmoCodeDisplay.ALWAYS) {
            // 配置：始终显示 —— 有 abbr 用 abbr（忽略 .off），否则用弹药 id 末段兜底。
            String path = ammoId.getPath();
            String raw = hasAbbr ? I18n.get(key) : path.substring(path.lastIndexOf('/') + 1);
            String code = raw.replace('_', ' ').trim();
            return code.isEmpty() ? null : code;
        }
        // 配置：DEFAULT —— 跟随 lang（需 abbr 存在，且 .off 不为真）。
        if (!hasAbbr) {
            return null;
        }
        // 关闭开关：并列键 <abbr 键>.off 为真时，保留 abbr 但不渲染该弹药的代号。
        String offKey = key + ".off";
        if (I18n.exists(offKey) && isTrue(I18n.get(offKey))) {
            return null;
        }
        String abbr = I18n.get(key).replace('_', ' ').trim();
        return abbr.isEmpty() ? null : abbr;
    }

    /** 关闭开关取值：忽略大小写的 true/1/yes/on 视为“关闭渲染”。 */
    private static boolean isTrue(String value) {
        String v = value.trim();
        return v.equalsIgnoreCase("true") || v.equals("1")
                || v.equalsIgnoreCase("yes") || v.equalsIgnoreCase("on");
    }
}
