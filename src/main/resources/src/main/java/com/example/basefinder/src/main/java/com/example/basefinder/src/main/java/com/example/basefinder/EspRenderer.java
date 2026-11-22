package com.example.basefinder;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.OptionalDouble;

public class EspRenderer {

    public static boolean enabled = true;
    public static boolean playersEnabled = true;
    public static final double PLAYER_RANGE = 200.0;

    private static final RenderPipeline LINES_NO_DEPTH = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation(Identifier.of("basefinder", "pipeline/esp_lines"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build()
    );

    private static final RenderLayer ESP_LAYER = RenderLayer.of(
        "basefinder_esp_lines",
        1536,
        LINES_NO_DEPTH,
        RenderLayer.MultiPhaseParameters.builder()
            .lineWidth(new RenderPhase.LineWidth(OptionalDouble.of(2.0)))
            .build(false)
    );

    public static void init() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(EspRenderer::render);
    }

    private static void render(WorldRenderContext ctx) {
        if (!enabled && !playersEnabled && !HitEffect.enabled) return;

        MatrixStack matrices = ctx.matrixStack();
        Vec3d cam = ctx.camera().getPos();
        VertexConsumerProvider consumers = ctx.consumers();
        if (matrices == null || consumers == null) return;

        VertexConsumer lines = consumers.getBuffer(ESP_LAYER);

        matrices.push();
        matrices.translate(-cam.x, -cam.y, -cam.z);

        if (enabled) {
            for (BlockPos pos : StorageScanner.storages) {
                VertexRendering.drawBox(matrices, lines, new Box(pos), 1.0f, 0.8f, 0.0f, 1.0f);
            }
        }

        if (playersEnabled) {
            var mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc.world != null && mc.player != null) {
                double maxSq = PLAYER_RANGE * PLAYER_RANGE;
                for (var p : mc.world.getPlayers()) {
                    if (p == mc.player) continue;
                    if (p.squaredDistanceTo(mc.player) > maxSq) continue;
                    VertexRendering.drawBox(matrices, lines, p.getBoundingBox(), 1.0f, 0.1f, 0.1f, 1.0f);
                }
            }
        }

        if (HitEffect.enabled && !HitEffect.hits.isEmpty()) {
            var mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc.world != null) {
                long now = System.currentTimeMillis();
                for (var e : HitEffect.hits.entrySet()) {
                    var ent = mc.world.getEntityById(e.getKey());
                    if (ent == null) continue;
                    float t = (now - e.getValue()) / (float) HitEffect.DURATION_MS;
                    if (t >= 1f) continue;
                    float fade = 1f - t;
                    float hue = ((now / 8) % 360) / 360f;
                    int rgb = net.minecraft.util.math.MathHelper.hsvToRgb(hue, 1f, 1f);
                    float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;
                    float pulse = (float) (0.5 + 0.5 * Math.sin(now / 90.0));
                    Box box = ent.getBoundingBox();
                    VertexRendering.drawBox(matrices, lines, box.expand(0.03 + 0.10 * pulse), r, g, b, fade);
                    VertexRendering.drawBox(matrices, lines, box.expand(0.12 + 0.18 * pulse), r, g, b, fade * 0.5f);
                }
            }
        }
        matrices.pop();
    }
}
