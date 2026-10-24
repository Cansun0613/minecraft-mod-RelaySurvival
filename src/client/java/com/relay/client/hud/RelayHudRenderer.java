package com.relay.client.hud;

import com.relay.feature.hud.RelayHudPayload;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.List;
import java.util.UUID;

/**
 * 左上角接力面板：皮肤头像 + 名字 + 倒计时 + 进度条，下面是任务卡。
 * 整体按 HudState.scale 缩放（默认 0.75）。
 * 完成/失败的任务保留：整卡变灰，顶部居中盖对勾/叉。
 * 鼠标悬停任务卡时，右侧展开详情（条件、进度、限时、奖惩）。
 */
public final class RelayHudRenderer implements HudElement {

    private static final int PANEL_BG = 0xD0121212;
    private static final int PANEL_EDGE = 0xFF3C3C3C;
    private static final int BAR_BG = 0xFF242424;
    private static final int BAR_FG = 0xFF9BE564;
    private static final int TEXT = 0xFFEDEDED;
    private static final int TEXT_DIM = 0xFF9A9A9A;
    private static final int TEXT_WARN = 0xFFFF6B6B;
    private static final int TEXT_DONE = 0xFF6E6E6E;
    private static final int CARD_BG = 0xB8141414;
    private static final int CHECK = 0xFF7CE38B;
    private static final int CROSS = 0xFFFF6B6B;
    private static final int TOOLTIP_BG = 0xF0101010;
    private static final int TOOLTIP_EDGE = 0xFF5A5A5A;

    private static final int PANEL_W = 182;
    private static final int PANEL_H = 48;
    private static final int MARGIN = 6;
    private static final int CARD_H = 36;
    private static final int CARD_GAP = 4;
    private static final int CARD_TOP = PANEL_H + 6;

    private static boolean disabled;

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker delta) {
        if (disabled || !HudState.running) {
            return;
        }
        try {
            render(g);
        } catch (Throwable t) {
            disabled = true;
            com.relay.RelayMod.LOGGER.error("HUD 渲染异常，已自动停用 HUD（游戏继续运行）", t);
        }
    }

    private void render(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        // 打开背包时由 InventoryTaskPanel 接管，不再画左上角 HUD
        if (HudState.containerOpen) {
            return;
        }
        Font font = mc.font;
        float scale = HudState.scale();

        g.pose().pushMatrix();
        g.pose().translate((float) MARGIN, (float) MARGIN);
        g.pose().scale(scale, scale);

        g.fill(0, 0, PANEL_W, PANEL_H, PANEL_BG);
        g.fill(0, 0, PANEL_W, 1, PANEL_EDGE);
        g.fill(0, PANEL_H - 1, PANEL_W, PANEL_H, PANEL_EDGE);

        PlayerSkin skin = skinFor(HudState.playerId);
        if (skin != null) {
            g.fill(4, 5, 38, 39, 0xFF2B2B2B);
            PlayerFaceExtractor.extractRenderState(g, skin, 5, 6, 32);
        } else {
            // 拿不到皮肤时的兜底：名字生成的色块
            int avatar = 0xFF000000 | (HudState.playerName.hashCode() & 0x7F7F7F);
            g.fill(5, 6, 37, 38, 0xFF2B2B2B);
            g.fill(8, 9, 34, 35, avatar);
            g.fill(14, 16, 18, 20, 0xFF101010);
            g.fill(24, 16, 28, 20, 0xFF101010);
            g.fill(15, 27, 27, 29, 0xFF101010);
        }

        g.text(font, HudState.playerName, 44, 7, TEXT);

        int color = HudState.remaining <= 10 ? TEXT_WARN : TEXT;
        g.pose().pushMatrix();
        g.pose().translate(43.0f, 18.0f);
        g.pose().scale(2.0f, 2.0f);
        g.text(font, HudState.clock(HudState.remaining), 0, 0, color);
        g.pose().popMatrix();

        String turnText = HudState.total > 0
                ? "第 " + (HudState.index + 1) + "/" + HudState.total + " 棒"
                : "第 " + (HudState.index + 1) + " 棒";
        g.text(font, turnText, PANEL_W - 6 - font.width(turnText), 8, TEXT_DIM);

        int barY = PANEL_H - 7;
        int barW = PANEL_W - 12;
        g.fill(6, barY, 6 + barW, barY + 3, BAR_BG);
        float ratio = HudState.turnSeconds <= 0 ? 0f
                : Math.max(0f, Math.min(1f, HudState.remaining / (float) HudState.turnSeconds));
        g.fill(6, barY, 6 + (int) (barW * ratio), barY + 3, BAR_FG);

        drawTasks(g, font, 0, CARD_TOP, PANEL_W, visibleCards(g, scale));

        g.pose().popMatrix();

        drawTaskCountdown(g, font, scale);
        drawHoverTooltip(g, font, mc, scale);
    }

    /** 按屏幕高度算最多能放几张任务卡（自动延长，不会超出屏幕）。 */
    private int visibleCards(GuiGraphicsExtractor g, float scale) {
        double available = g.guiHeight() - (MARGIN + CARD_TOP * scale) - 4;
        double step = (CARD_H + CARD_GAP) * scale;
        return Math.max(1, (int) (available / step));
    }

    private void drawTasks(GuiGraphicsExtractor g, Font font, int x, int y, int w, int maxCards) {
        List<RelayHudPayload.TaskLine> tasks = HudState.tasks;
        int limit = Math.min(tasks.size(), maxCards);
        for (int i = 0; i < limit; i++) {
            RelayHudPayload.TaskLine task = tasks.get(i);
            boolean done = "DONE".equals(task.state());
            boolean failed = "FAILED".equals(task.state());
            boolean finished = done || failed;

            int top = y + i * (CARD_H + CARD_GAP);
            g.fill(x, top, x + w, top + CARD_H, CARD_BG);
            g.fill(x, top, x + 1, top + CARD_H, finished ? 0xFF4A4A4A : tierColor(task.tier()));

            int tierColor = finished ? TEXT_DONE : tierColor(task.tier());
            int categoryColor = finished ? TEXT_DONE : 0xFF7CE38B;
            int titleColor = finished ? TEXT_DONE : TEXT;
            int progressColor = finished ? TEXT_DONE : TEXT_DIM;

            g.text(font, task.tier(), x + 5, top + 3, tierColor);
            g.text(font, task.category(), x + 16, top + 3, categoryColor);

            String timeText;
            int timeColor;
            if (done) {
                timeText = "完成";
                timeColor = CHECK;
            } else if (failed) {
                timeText = "超时";
                timeColor = CROSS;
            } else if (!task.timed()) {
                timeText = "无限";
                timeColor = TEXT_DIM;
            } else {
                timeText = HudState.clock(task.secondsLeft());
                timeColor = task.secondsLeft() <= 10 ? TEXT_WARN : TEXT;
            }
            g.text(font, timeText, x + w - 5 - font.width(timeText), top + 3, timeColor);

            g.text(font, task.title(), x + 5, top + 13, titleColor);

            String progress = task.progress() + " / " + task.target();
            g.text(font, progress, x + w - 5 - font.width(progress), top + 23, progressColor);

            int barY = top + CARD_H - 5;
            g.fill(x + 5, barY, x + w - 5, barY + 2, BAR_BG);
            float ratio = task.target() <= 0 ? 0f : Math.min(1f, task.progress() / (float) task.target());
            g.fill(x + 5, barY, x + 5 + (int) ((w - 10) * ratio), barY + 2, finished ? 0xFF5A5A5A : 0xFF5FC9F8);

            if (finished) {
                // 变灰 + 顶部居中的对勾/叉（最后画，保证在最上层）
                g.fill(x, top, x + w, top + CARD_H, 0x66000000);
                int cx = x + w / 2;
                int cy = top + 2;
                g.fill(cx - 9, cy - 1, cx + 9, cy + 13, 0xCC000000);
                if (done) {
                    drawLine(g, cx - 5, cy + 5, cx - 2, cy + 9, CHECK);
                    drawLine(g, cx - 2, cy + 9, cx + 6, cy + 1, CHECK);
                } else {
                    drawLine(g, cx - 4, cy + 3, cx + 4, cy + 11, CROSS);
                    drawLine(g, cx + 4, cy + 3, cx - 4, cy + 11, CROSS);
                }
            }
        }
    }

    private void drawHoverTooltip(GuiGraphicsExtractor g, Font font, Minecraft mc, float scale) {
        List<RelayHudPayload.TaskLine> tasks = HudState.tasks;
        if (tasks.isEmpty() || mc.getWindow() == null) {
            return;
        }
        double mouseX = mc.mouseHandler.getScaledXPos(mc.getWindow());
        double mouseY = mc.mouseHandler.getScaledYPos(mc.getWindow());

        int hovered = -1;
        int limit = Math.min(tasks.size(), visibleCards(g, scale));
        for (int i = 0; i < limit; i++) {
            double left = MARGIN;
            double top = MARGIN + (CARD_TOP + i * (CARD_H + CARD_GAP)) * scale;
            double right = left + PANEL_W * scale;
            double bottom = top + CARD_H * scale;
            if (mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= bottom) {
                hovered = i;
                break;
            }
        }
        if (hovered < 0) {
            return;
        }

        RelayHudPayload.TaskLine task = tasks.get(hovered);
        int textW = 188;
        List<FormattedCharSequence> desc = font.split(Component.literal(task.description()), textW);
        int lines = 0;
        for (FormattedCharSequence ignored : desc) {
            lines++;
        }

        int pad = 6;
        int lineH = 10;
        int height = pad * 2 + lineH * (4 + lines);
        int x = MARGIN + PANEL_W + 8;
        int y = MARGIN + (CARD_TOP + hovered * (CARD_H + CARD_GAP));
        int screenW = g.guiWidth();
        if (x + textW + pad * 2 > screenW) {
            x = Math.max(4, screenW - textW - pad * 2 - 4);
        }
        int screenH = g.guiHeight();
        if (y + height > screenH) {
            y = Math.max(4, screenH - height - 4);
        }

        g.fill(x, y, x + textW + pad * 2, y + height, TOOLTIP_BG);
        g.fill(x, y, x + textW + pad * 2, y + 1, TOOLTIP_EDGE);
        g.fill(x, y + height - 1, x + textW + pad * 2, y + height, TOOLTIP_EDGE);

        int ty = y + pad;
        g.text(font, task.title(), x + pad, ty, TEXT);
        g.text(font, "[" + task.tier() + "]", x + pad + textW - font.width("[" + task.tier() + "]"), ty, tierColor(task.tier()));
        ty += lineH;

        for (FormattedCharSequence line : desc) {
            g.text(font, line, x + pad, ty, TEXT_DIM);
            ty += lineH;
        }

        String progressText;
        if ("DONE".equals(task.state())) {
            progressText = "进度：" + task.progress() + " / " + task.target() + "（已完成）";
        } else if ("FAILED".equals(task.state())) {
            progressText = "进度：" + task.progress() + " / " + task.target() + "（已超时）";
        } else {
            int remain = Math.max(0, task.target() - task.progress());
            progressText = "进度：" + task.progress() + " / " + task.target() + "（还差 " + remain + "）";
        }
        g.text(font, progressText, x + pad, ty, TEXT);
        ty += lineH;

        String timeText = task.timed() ? "限时 " + HudState.clock(task.timeLimit()) : "限时 无限";
        if (task.timed() && "ACTIVE".equals(task.state())) {
            timeText += " · 剩余 " + HudState.clock(task.secondsLeft());
        }
        g.text(font, timeText, x + pad, ty, TEXT_DIM);
        ty += lineH;

        g.text(font, "奖励 +" + task.successSeconds() + " 秒 · 超时 -" + task.failSeconds() + " 秒",
                x + pad, ty, TEXT_DIM);
    }

    private PlayerSkin skinFor(String playerId) {
        if (playerId == null || playerId.isEmpty()) {
            return null;
        }
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return null;
        }
        try {
            var info = connection.getPlayerInfo(UUID.fromString(playerId));
            return info == null ? null : info.getSkin();
        } catch (Exception e) {
            return null;
        }
    }

    private void drawLine(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int color) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (steps == 0) {
            g.fill(x1, y1, x1 + 2, y1 + 2, color);
            return;
        }
        for (int i = 0; i <= steps; i++) {
            int px = x1 + (x2 - x1) * i / steps;
            int py = y1 + (y2 - y1) * i / steps;
            g.fill(px, py, px + 2, py + 2, color);
        }
    }

    private int tierColor(String tier) {
        if (tier == null) {
            return 0xFF9E9E9E;
        }
        return switch (tier) {
            case "S" -> 0xFFFFD700;
            case "A" -> 0xFFFF6B6B;
            case "B" -> 0xFFC792EA;
            case "C" -> 0xFF82AAFF;
            default -> 0xFF9E9E9E;
        };
    }

    private void drawTaskCountdown(GuiGraphicsExtractor g, Font font, float scale) {
        int urgent = Integer.MAX_VALUE;
        for (RelayHudPayload.TaskLine task : HudState.tasks) {
            if ("ACTIVE".equals(task.state()) && task.timed()) {
                urgent = Math.min(urgent, task.secondsLeft());
            }
        }
        if (urgent > 10) {
            return;
        }
        int seconds = Math.max(0, urgent);
        String text = String.format("00:%02d", seconds);
        int textW = font.width(text) * 2;
        int screenW = g.guiWidth();
        float x = (screenW - textW * scale) / 2f;
        float y = 10f;

        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(scale, scale);
        g.fill(-8, -4, textW + 8, 22, 0xD9000000);
        g.pose().pushMatrix();
        g.pose().scale(2.0f, 2.0f);
        g.text(font, text, 0, 0, seconds == 0 ? TEXT_WARN : TEXT);
        g.pose().popMatrix();
        g.pose().popMatrix();
    }
}
