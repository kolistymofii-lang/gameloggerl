package com.example.basefinder;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

import java.util.HashSet;
import java.util.Set;

public class EspRenderer {

    public static boolean enabled = true;
    public static boolean playersEnabled = true;
    public static final double PLAYER_RANGE = 200.0;

    private static final Set<Integer> glowed = new HashSet<>();

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(EspRenderer::tick);
    }

    private static void tick(MinecraftClient mc) {
        if (mc.world == null || mc.player == null) {
            glowed.clear();
            return;
        }
        double maxSq = PLAYER_RANGE * PLAYER_RANGE;
        for (var p : mc.world.getPlayers()) {
            if (p == mc.player) continue;
            boolean inRange = p.squaredDistanceTo(mc.player)
