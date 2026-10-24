package com.relay.client.hud;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.lang.reflect.Field;

/**
 * 读取容器界面的左上角与尺寸。原版字段是 protected，这里用反射读一次并缓存；
 * 读不到就退回 176x166 的居中估算，保证面板仍能画出来。
 */
final class ContainerGeometry {

    record Rect(int left, int top, int width, int height) {
    }

    private static boolean resolved;
    private static Field leftField;
    private static Field topField;
    private static Field widthField;
    private static Field heightField;

    private ContainerGeometry() {
    }

    static Rect of(Screen screen) {
        if (!(screen instanceof AbstractContainerScreen<?> container)) {
            return null;
        }
        resolve();
        if (leftField != null) {
            try {
                return new Rect(leftField.getInt(container), topField.getInt(container),
                        widthField.getInt(container), heightField.getInt(container));
            } catch (Exception ignored) {
            }
        }
        return new Rect((screen.width - 176) / 2, (screen.height - 166) / 2, 176, 166);
    }

    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            leftField = AbstractContainerScreen.class.getDeclaredField("leftPos");
            topField = AbstractContainerScreen.class.getDeclaredField("topPos");
            widthField = AbstractContainerScreen.class.getDeclaredField("imageWidth");
            heightField = AbstractContainerScreen.class.getDeclaredField("imageHeight");
            leftField.setAccessible(true);
            topField.setAccessible(true);
            widthField.setAccessible(true);
            heightField.setAccessible(true);
        } catch (Throwable t) {
            leftField = null;
        }
    }
}
