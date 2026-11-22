package com.example.basefinder;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public class EspRenderer {
    public static boolean enabled = true;
    public static boolean playersEnabled = true;
    public static final double PLAYER_RANGE = 200.0;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(EspRenderer::tick);
    }

    private static void tick(MinecraftClient mc) {
        if (mc.world == null || mc.player == null) return;
        for (var p : mc.world.getPlayers()) {
            if (p == mc.player) continue;
            boolean on = playersEnabled
                && p.squaredDistanceTo(mc.player) <= PLAYER_RANGE * PLAYER_RANGE;
            if (on) p.setGlowing(true);
            else if (!HitEffect.hits.containsKey(p.getId())) p.setGlowing(false);
        }
    }
}
