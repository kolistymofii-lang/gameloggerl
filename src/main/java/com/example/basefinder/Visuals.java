package com.example.basefinder;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class Visuals implements ClientModInitializer {

    private static final List<ItemEntity> fakes = new ArrayList<>();
    private int tick = 0;

    @Override
    public void onInitializeClient() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
            client.execute(() -> {
                if (client.player != null) {
                    client.player.sendMessage(
                        Text.literal("BaseFinder v2 загружен: /menu /aim /bases"), false);
                }
            }));
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(MinecraftClient mc) {
        if (mc.world == null || mc.player == null) {
            fakes.clear();
            return;
        }
        Scoreboard sb = mc.world.getScoreboard();
        Team purple = team(sb, "bf_purple", Formatting.DARK_PURPLE);
        Team red = team(sb, "bf_red", Formatting.RED);

        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) continue;
            boolean hit = HitEffect.hits.containsKey(p.getId());
            put(sb, p.getNameForScoreboard(), hit ? red : purple);
        }

        if (++tick % 20 != 0) return;
        for (ItemEntity f : fakes) f.discard();
        fakes.clear();
        if (!EspRenderer.enabled) return;

        double mx = mc.player.getX(), my = mc.player.getY(), mz = mc.player.getZ();
        List<BlockPos> near = new ArrayList<>();
        for (BlockPos b : StorageScanner.storages) {
            double dx = b.getX() + 0.5 - mx, dy = b.getY() - my, dz = b.getZ() + 0.5 - mz;
            if (dx * dx + dy * dy + dz * dz <= 64.0 * 64.0) near.add(b);
        }
        int n = 0;
        for (BlockPos b : near) {
            if (n++ >= 60) break;
            ItemEntity e = new ItemEntity(mc.world,
                b.getX() + 0.5, b.getY() + 0.5, b.getZ() + 0.5,
                new ItemStack(Items.CHEST));
            e.setNoGravity(true);
            e.setVelocity(Vec3d.ZERO);
            e.setGlowing(true);
            mc.world.addEntity(e);
            put(sb, e.getNameForScoreboard(), red);
            fakes.add(e);
        }
    }

    private static Team team(Scoreboard sb, String name, Formatting color) {
        Team t = sb.getTeam(name);
        if (t == null) {
            t = sb.addTeam(name);
            t.setColor(color);
        }
        return t;
    }

    private static void put(Scoreboard sb, String holder, Team team) {
        Team cur = sb.getScoreHolderTeam(holder);
        if (cur == team) return;
        if (cur != null) sb.removeScoreHolderFromTeam(holder, cur);
        sb.addScoreHolderToTeam(holder, team);
    }
}
