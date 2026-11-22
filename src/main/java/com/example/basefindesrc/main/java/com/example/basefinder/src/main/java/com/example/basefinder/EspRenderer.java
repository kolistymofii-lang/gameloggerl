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
        WorldRenderEvents.AFTER_TRANSLUCENT.register(EspRenderer::render
