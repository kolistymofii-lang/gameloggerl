package com.example.basefinder;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;

import java.util.Map;

public class BaseFinderClient implements ClientModInitializer {

    private int tick = 0;
    private boolean menuKeyWasDown = false;
    private static boolean openMenuNext = false;
    private boolean homeKeyWasDown = false;
    private static final int HOME_KEY = org.lwjgl.glfw.GLFW.GLFW_KEY_Y;
    public static String homeCommand = "home";
    public static boolean homeKeyEnabled = true;
    private static final int MENU_KEY = org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;

    @Override
    public void onInitializeClient() {
        EspRenderer.init();
        HitEffect.init();

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.world == null) return;
            HitEffect.tick(mc);
            if (openMenuNext) { openMenuNext = false; mc.setScreen(new MenuScreen()); }

            boolean down = org.lwjgl.glfw.GLFW.glfwGetKey(mc.getWindow().getHandle(), MENU_KEY)
                    == org.lwjgl.glfw.GLFW.GLFW_PRESS;
            if (down && !menuKeyWasDown && mc.currentScreen == null) {
                mc.setScreen(new MenuScreen());
            }
            menuKeyWasDown = down;

            boolean yDown = org.lwjgl.glfw.GLFW.glfwGetKey(mc.getWindow().getHandle(), HOME_KEY)
                    == org.lwjgl.glfw.GLFW.GLFW_PRESS;
            if (yDown && !homeKeyWasDown && homeKeyEnabled
                    && mc.currentScreen == null && mc.player != null) {
                mc.player.networkHandler.sendChatCommand(homeCommand);
            }
            homeKeyWasDown = yDown;

            if (++tick % 20 == 0) StorageScanner.scan(mc);
        });

        ClientCommandRegistrationCallback.EVENT.register((d, r) ->
            d.register(ClientCommandManager.literal("menu").executes(ctx -> {
                openMenuNext = true;
                return 1;
            })));

        ClientCommandRegistrationCallback.EVENT.register((d, r) ->
            d.register(ClientCommandManager.literal("esp").executes(ctx -> {
                EspRenderer.enabled = !EspRenderer.enabled;
                ctx.getSource().sendFeedback(Text.literal("ESP: " + (EspRenderer.enabled ? "вкл" : "выкл")));
                return 1;
            })));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("bases")
                .executes(ctx -> report(ctx.getSource(), 5))
                .then(ClientCommandManager.argument("min", IntegerArgumentType.integer(1, 500))
                    .executes(ctx -> report(ctx.getSource(),
                            IntegerArgumentType.getInteger(ctx, "min")))))
        );
    }

    private int report(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource src, int min) {
        int shown = 0;
        src.sendFeedback(Text.literal("Всего контейнеров: " + StorageScanner.storages.size()));
        for (Map.Entry<ChunkPos, Integer> e : StorageScanner.chunkCounts.entrySet()) {
            if (e.getValue() < min) continue;
            ChunkPos p = e.getKey();
            src.sendFeedback(Text.literal("Чанк " + p.x + ", " + p.z
                    + "  (блок " + p.getStartX() + ", " + p.getStartZ() + ")  — "
                    + e.getValue() + " шт."));
            if (++shown >= 15) break;
        }
        if (shown == 0) src.sendFeedback(Text.literal("Подозрительных чанков нет (мин. " + min + ")."));
        return 1;
    }
}
