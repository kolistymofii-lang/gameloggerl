package com.example.basefinder;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;

public class MenuScreen extends Screen {

    public MenuScreen() {
        super(Text.literal("BaseFinder"));
    }

    @Override
    protected void init() {
        int w = 200, h = 20;
        int x = this.width / 2 - w / 2;
        int y = this.height / 2 - 40;

        addDrawableChild(ButtonWidget.builder(
            label("Контейнеры (ESP)", EspRenderer.enabled),
            b -> { EspRenderer.enabled = !EspRenderer.enabled; clearAndInit(); }
        ).dimensions(x, y, w, h).build());

        addDrawableChild(ButtonWidget.builder(
            label("Игроки до " + (int) EspRenderer.PLAYER_RANGE + " блоков", EspRenderer.playersEnabled),
            b -> { EspRenderer.playersEnabled = !EspRenderer.playersEnabled; clearAndInit(); }
        ).dimensions(x, y + 26, w, h).build());

        MinecraftClient mc = MinecraftClient.getInstance();
        boolean left = mc.options.getMainArm().getValue() == Arm.LEFT;
        addDrawableChild(ButtonWidget.builder(
            Text.literal("Основная рука: " + (left ? "ЛЕВАЯ" : "ПРАВАЯ")),
            b -> {
                mc.options.getMainArm().setValue(left ? Arm.RIGHT : Arm.LEFT);
                mc.options.write();
                clearAndInit();
            }
        ).dimensions(x, y + 52, w, h).build());

        addDrawableChild(ButtonWidget.builder(
            label("Y = /" + BaseFinderClient.homeCommand, BaseFinderClient.homeKeyEnabled),
            b -> { BaseFinderClient.homeKeyEnabled = !BaseFinderClient.homeKeyEnabled; clearAndInit(); }
        ).dimensions(x, y + 78, w, h).build());

        addDrawableChild(ButtonWidget.builder(
            label("Подсветка при ударе", HitEffect.enabled),
            b -> { HitEffect.enabled = !HitEffect.enabled; clearAndInit(); }
        ).dimensions(x, y + 104, w, h).build());

        addDrawableChild(ButtonWidget.builder(
            Text.literal("Закрыть"),
            b -> close()
        ).dimensions(x, y + 138, w, h).build());
    }

    private static Text label(String name, boolean on) {
        return Text.literal(name + ": " + (on ? "ВКЛ" : "ВЫКЛ"));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, this.title,
            this.width / 2, this.height / 2 - 62, 0xFFFFFF);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
