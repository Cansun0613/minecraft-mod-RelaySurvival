package com.relay.client.hud;

import com.relay.feature.hud.RelayHudPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * 打开背包时替代左上角 HUD：在背包左侧显示所有任务的详细信息。
 */
public final class InventoryTaskPanel {

    private static final int WIDTH = 176;
    private static final int PAD = 6;
    private static final int LINE = 9;

    private InventoryTaskPanel() {
    }

    public static void render(GuiGraphicsExtractor g, Screen screen) {
        if (!HudState.running || HudState.tasks.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        List<RelayHudPayload.TaskLine> tasks = HudState.tasks;

        ContainerGeometry.Rect rect = ContainerGeometry.of(screen);
        if (rect == null) {
            return;
        }
        int left = rect.left();
        int top = rect.top();

        int contentW = WIDTH - PAD * 2;
        // 与下面逐行绘制保持一致：标题(1 行) + 描述(n 行) + 进度(1 行) + 奖惩(1 行) + 4px 间隔
        int height = PAD * 2 + LINE + 2;
        for (RelayHudPayload.TaskLine task : tasks) {
            int descLines = wrap(font, task.description(), contentW).size();
            height += (3 + descLines) * LINE + 4;
        }
        height += 2;

        int x = left - WIDTH - 8;
        if (x < 4) {
            x = left + rect.width() + 8;
        }
        if (x + WIDTH > screen.width - 4) {
            x = Math.max(4, screen.width - WIDTH - 4);
        }
        // 以容器中心为准上下居中，再夹在屏幕内
        int y = top + (rect.height() - height) / 2;
        y = Math.max(4, Math.min(y, screen.height - height - 4));

        g.fill(x, y, x + WIDTH, y + height, 0xF0101010);
        g.fill(x, y, x + WIDTH, y + 1, 0xFF5A5A5A);
        g.fill(x, y + height - 1, x + WIDTH, y + height, 0xFF5A5A5A);

        int ty = y + PAD;
        g.text(font, "接力任务", x + PAD, ty, 0xFFFFD700);
        String count = tasks.size() + " 个";
        g.text(font, count, x + WIDTH - PAD - font.width(count), ty, 0xFF9A9A9A);
        ty += LINE + 2;

        for (RelayHudPayload.TaskLine task : tasks) {
            boolean done = "DONE".equals(task.state());
            boolean failed = "FAILED".equals(task.state());
            int titleColor = done || failed ? 0xFF6E6E6E : 0xFFEDEDED;
            int dimColor = 0xFF8A8A8A;

            g.text(font, "[" + task.tier() + "] " + task.title(), x + PAD, ty, titleColor);

            String stateText;
            int stateColor;
            if (done) {
                stateText = "已完成";
                stateColor = 0xFF7CE38B;
            } else if (failed) {
                stateText = "已超时";
                stateColor = 0xFFFF6B6B;
            } else {
                stateText = task.timed()
                        ? HudState.clock(task.secondsLeft()) + " / " + HudState.clock(task.timeLimit())
                        : "无限时";
                stateColor = 0xFF9A9A9A;
            }
            g.text(font, stateText, x + WIDTH - PAD - font.width(stateText), ty, stateColor);
            ty += LINE;

            for (FormattedCharSequence line : wrap(font, task.description(), contentW)) {
                g.text(font, line, x + PAD, ty, dimColor);
                ty += LINE;
            }

            String progress;
            if (done) {
                progress = "进度 " + task.progress() + " / " + task.target() + "（已完成）";
            } else if (failed) {
                progress = "进度 " + task.progress() + " / " + task.target() + "（已超时）";
            } else {
                progress = "进度 " + task.progress() + " / " + task.target()
                        + "（还差 " + Math.max(0, task.target() - task.progress()) + "）";
            }
            g.text(font, progress, x + PAD, ty, titleColor);
            ty += LINE;
            g.text(font, "奖励 +" + task.successSeconds() + " 秒 · 超时 -" + task.failSeconds() + " 秒",
                    x + PAD, ty, dimColor);
            ty += LINE + 4;
        }
    }

    private static List<FormattedCharSequence> wrap(Font font, String text, int width) {
        String value = text == null ? "" : text;
        return font.split(Component.literal(value), width);
    }
}
