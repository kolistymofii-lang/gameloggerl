package com.example.basefinder;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class HitEffect {

    public static boolean enabled = true;
    public static final long DURATION_MS = 2500;

    public static final Map<Integer, Long> hits = new HashMap<>();

    public static void init() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (enabled && world.isClient() && entity instanceof PlayerEntity) {
                hits.put(entity.getId(), System.currentTimeMillis());
                entity.setGlowing(true);
            }
            return ActionResult.PASS;
        });
    }

    public static void tick(MinecraftClient mc) {
        if (mc.world == null) { hits.clear(); return; }
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Integer, Long>> it = hits.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            if (!enabled || now - e.getValue() > DURATION_MS) {
                Entity ent = mc.world.getEntityById(e.getKey());
                if (ent != null) ent.setGlowing(false);
                it.remove();
            }
        }
    }
}
