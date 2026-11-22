package com.example.basefinder;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class Aimbot implements ClientModInitializer {

    public static boolean enabled = false;
    public static final double RANGE = 8.0;
    // Чем меньше SMOOTH и MAX_TURN, тем плавнее и медленнее наводка
    private static final float SMOOTH = 0.15f;
    private static final float MAX_TURN = 6f;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(Aimbot::tick);
        ClientCommandRegistrationCallback.EVENT.register((d, r) ->
            d.register(ClientCommandManager.literal("aim").executes(ctx -> {
                enabled = !enabled;
                ctx.getSource().sendFeedback(
                    Text.literal("Автонаводка: " + (enabled ? "вкл" : "выкл")));
                return 1;
            })));
    }

    private static void tick(MinecraftClient mc) {
        if (!enabled || mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;

        PlayerEntity best = null;
        double bestSq = RANGE * RANGE;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || !p.isAlive() || p.isSpectator()) continue;
            double d = p.squaredDistanceTo(mc.player);
            if (d <= bestSq) { bestSq = d; best = p; }
        }
        if (best == null) return;

        Vec3d eye = mc.player.getEyePos();
        Vec3d t = best.getBoundingBox().getCenter();
        double dx = t.x - eye.x, dy = t.y - eye.y, dz = t.z - eye.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);

        float wantYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float wantPitch = (float) -Math.toDegrees(Math.atan2(dy, horiz));

        float errYaw = MathHelper.wrapDegrees(wantYaw - mc.player.getYaw());
        float errPitch = wantPitch - mc.player.getPitch();

        float dYaw = MathHelper.clamp(errYaw * SMOOTH, -MAX_TURN, MAX_TURN);
        float dPitch = MathHelper.clamp(errPitch * SMOOTH, -MAX_TURN, MAX_TURN);

        mc.player.setYaw(mc.player.getYaw() + dYaw);
        mc.player.setPitch(MathHelper.clamp(mc.player.getPitch() + dPitch, -90f, 90f));

        boolean aimed = Math.abs(errYaw) < 12f && Math.abs(errPitch) < 12f;
        if (aimed && bestSq <= 9.0 && mc.interactionManager != null
                && mc.player.getAttackCooldownProgress(0.5f) >= 1.0f) {
            mc.interactionManager.attackEntity(mc.player, best);
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }
}
