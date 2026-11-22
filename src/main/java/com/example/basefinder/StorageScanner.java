package com.example.basefinder;

import net.minecraft.block.entity.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.ChestBoatEntity;
import net.minecraft.entity.vehicle.HopperMinecartEntity;
import net.minecraft.entity.vehicle.StorageMinecartEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.*;

public class StorageScanner {

    public static volatile Map<ChunkPos, Integer> chunkCounts = new HashMap<>();
    public static volatile List<BlockPos> storages = new ArrayList<>();

    private static boolean isStorage(BlockEntity be) {
        return be instanceof ChestBlockEntity
            || be instanceof EnderChestBlockEntity
            || be instanceof BarrelBlockEntity
            || be instanceof ShulkerBoxBlockEntity
            || be instanceof HopperBlockEntity
            || be instanceof DispenserBlockEntity
            || be instanceof AbstractFurnaceBlockEntity
            || be instanceof BrewingStandBlockEntity
            || be instanceof ChiseledBookshelfBlockEntity
            || be instanceof DecoratedPotBlockEntity
            || be instanceof CrafterBlockEntity
            || be instanceof LecternBlockEntity;
    }

    public static void scan(MinecraftClient mc) {
        ClientWorld world = mc.world;
        if (world == null || mc.player == null) return;

        List<BlockPos> found = new ArrayList<>();
        Map<ChunkPos, Integer> counts = new HashMap<>();

        int r = mc.options.getClampedViewDistance();
        ChunkPos center = mc.player.getChunkPos();

        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                WorldChunk chunk = world.getChunkManager().getWorldChunk(center.x + x, center.z + z);
                if (chunk == null) continue;

                int n = 0;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (isStorage(be)) {
                        found.add(be.getPos());
                        n++;
                    }
                }
                if (n > 0) counts.put(chunk.getPos(), n);
            }
        }

        for (Entity e : world.getEntities()) {
            if (e instanceof StorageMinecartEntity || e instanceof ChestBoatEntity
                    || e instanceof HopperMinecartEntity) {
                found.add(e.getBlockPos());
                counts.merge(new ChunkPos(e.getBlockPos()), 1, Integer::sum);
            }
        }

        storages = found;
        chunkCounts = counts;
    }
}
