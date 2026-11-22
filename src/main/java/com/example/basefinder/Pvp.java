package com.example.basefinder;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.atomic.AtomicBoolean;

public class Pvp implements ClientModInitializer {

    static final AtomicBoolean TOTEM = new AtomicBoolean(true);
    static final AtomicBoolean HEAL = new AtomicBoolean(true);
    static final AtomicBoolean SHIELD = new AtomicBoolean(false);
    static final AtomicBoolean CRYSTAL = new AtomicBoolean(false);

    private static final float HEAL_BELOW = 8f; // 4 сердца
    private static int t = 0;
    private static int eatTicks = 0;
    private static int eatPrev = -1;
    private static int shieldPrev = -1;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(Pvp::tick);
        reg("totem", "АвтоТотем", TOTEM);
        reg("heal", "АвтоХил", HEAL);
        reg("shield", "Снос щита", SHIELD);
        reg("crystal", "АвтоКристалл", CRYSTAL);
    }

    private static void reg(String cmd, String label, AtomicBoolean flag) {
        ClientCommandRegistrationCallback.EVENT.register((d, r) ->
            d.register(ClientCommandManager.literal(cmd).executes(ctx -> {
                flag.set(!flag.get());
                ctx.getSource().sendFeedback(
                    Text.literal(label + ": " + (flag.get() ? "вкл" : "выкл")));
                return 1;
            })));
    }

    private static void tick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null || mc.interactionManager == null) return;
        if (eatTicks > 0 && (mc.currentScreen != null || !HEAL.get())) finishEat(mc, p);
        if (mc.currentScreen != null) return;
        t++;

        if (HEAL.get() && doHeal(mc, p)) return;
        if (TOTEM.get() && t % 4 == 0) doTotem(mc, p);
        if (SHIELD.get()) doShield(mc, p);
        if (CRYSTAL.get()) doCrystal(mc, p);
    }

    // ---------- АвтоТотем ----------
    private static void doTotem(MinecraftClient mc, ClientPlayerEntity p) {
        if (p.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return;
        for (int i = 0; i < 36; i++) {
            if (p.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) {
                int slot = i < 9 ? i + 36 : i;
                mc.interactionManager.clickSlot(
                    p.playerScreenHandler.syncId, slot, 40, SlotActionType.SWAP, p);
                return;
            }
        }
    }

    // ---------- АвтоХил ----------
    private static boolean doHeal(MinecraftClient mc, ClientPlayerEntity p) {
        if (eatTicks > 0) {
            eatTicks--;
            mc.options.useKey.setPressed(true);
            if (eatTicks == 0) finishEat(mc, p);
            return true;
        }
        if (p.getHealth() > HEAL_BELOW) return false;
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack s = p.getInventory().getStack(i);
            if (s.isOf(Items.ENCHANTED_GOLDEN_APPLE) || s.isOf(Items.GOLDEN_APPLE)) {
                slot = i;
                break;
            }
        }
        if (slot < 0) return false;
        eatPrev = p.getInventory().getSelectedSlot();
        p.getInventory().setSelectedSlot(slot);
        eatTicks = 36;
        mc.options.useKey.setPressed(true);
        return true;
    }

    private static void finishEat(MinecraftClient mc, ClientPlayerEntity p) {
        mc.options.useKey.setPressed(false);
        eatTicks = 0;
        if (eatPrev >= 0) {
            p.getInventory().setSelectedSlot(eatPrev);
            eatPrev = -1;
        }
    }

    // ---------- Снос щита ----------
    private static void doShield(MinecraftClient mc, ClientPlayerEntity p) {
        PlayerEntity target = null;
        double best = 16.0; // 4 блока
        for (PlayerEntity e : mc.world.getPlayers()) {
            if (e == p || !e.isAlive() || !e.isBlocking()) continue;
            double d = e.squaredDistanceTo(p);
            if (d <= best) { best = d; target = e; }
        }
        if (target == null) {
            if (shieldPrev >= 0) {
                p.getInventory().setSelectedSlot(shieldPrev);
                shieldPrev = -1;
            }
            return;
        }
        int axe = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack s = p.getInventory().getStack(i);
            if (!s.isEmpty()
                && Registries.ITEM.getId(s.getItem()).getPath().endsWith("_axe")) {
                axe = i;
                break;
            }
        }
        if (axe < 0) return;
        if (shieldPrev < 0) shieldPrev = p.getInventory().getSelectedSlot();
        p.getInventory().setSelectedSlot(axe);
        if (p.getAttackCooldownProgress(0.5f) >= 1.0f) {
            mc.interactionManager.attackEntity(p, target);
            p.swingHand(Hand.MAIN_HAND);
        }
    }

    // ---------- АвтоКристалл ----------
    private static void doCrystal(MinecraftClient mc, ClientPlayerEntity p) {
        PlayerEntity target = null;
        double best = 100.0; // 10 блоков
        for (PlayerEntity e : mc.world.getPlayers()) {
            if (e == p || !e.isAlive() || e.isSpectator()) continue;
            double d = e.squaredDistanceTo(p);
            if (d <= best) { best = d; target = e; }
        }
        if (target == null) return;

        // 1) ломаем кристалл рядом с целью
        Box box = p.getBoundingBox().expand(4.5);
        for (EndCrystalEntity c : mc.world.getEntitiesByClass(EndCrystalEntity.class, box, x -> true)) {
            if (c.squaredDistanceTo(target) > 36.0) continue;
            if (c.squaredDistanceTo(p) < 9.0) continue;
            if (t % 2 == 0) {
                mc.interactionManager.attackEntity(p, c);
                p.swingHand(Hand.MAIN_HAND);
            }
            return;
        }

        // 2) ставим новый
        if (t % 4 != 0 || p.getHealth() <= 10f) return;
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            if (p.getInventory().getStack(i).isOf(Items.END_CRYSTAL)) { slot = i; break; }
        }
        if (slot < 0) return;

        BlockPos me = p.getBlockPos();
        BlockPos bestPos = null;
        double bestD = 1e9;
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -4; dz <= 4; dz++) {
                    BlockPos b = me.add(dx, dy, dz);
                    var st = mc.world.getBlockState(b);
                    if (!(st.isOf(Blocks.OBSIDIAN) || st.isOf(Blocks.BEDROCK))) continue;
                    if (!mc.world.getBlockState(b.up()).isAir()
                        || !mc.world.getBlockState(b.up(2)).isAir()) continue;
                    double cx = b.getX() + 0.5, cy = b.getY() + 1.0, cz = b.getZ() + 0.5;
                    double dt = target.squaredDistanceTo(cx, cy, cz);
                    if (dt > 25.0) continue;
                    double dm = p.squaredDistanceTo(cx, cy, cz);
                    if (dm < 9.0 || dm > 20.25) continue;
                    if (!mc.world.getOtherEntities(null, new Box(b.up())).isEmpty()) continue;
                    if (dt < bestD) { bestD = dt; bestPos = b; }
                }
            }
        }
        if (bestPos == null) return;

        p.getInventory().setSelectedSlot(slot);
        BlockHitResult hit = new BlockHitResult(
            new Vec3d(bestPos.getX() + 0.5, bestPos.getY() + 1.0, bestPos.getZ() + 0.5),
            Direction.UP, bestPos, false);
        mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, hit);
        p.swingHand(Hand.MAIN_HAND);
    }
    }
