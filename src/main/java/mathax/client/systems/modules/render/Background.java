package mathax.client.systems.modules.render;

import mathax.client.MatHax;
import mathax.client.eventbus.listeners.ConsumerListener;
import mathax.client.events.game.WindowResizedEvent;
import mathax.client.events.render.RenderAfterWorldEvent;
import mathax.client.gui.WidgetScreen;
import mathax.client.renderer.Framebuffer;
import mathax.client.renderer.GL;
import mathax.client.renderer.PostProcessRenderer;
import mathax.client.renderer.Shader;
import mathax.client.settings.*;
import mathax.client.systems.modules.Categories;
import mathax.client.systems.modules.Module;
import mathax.client.utils.Utils;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Items;

public class Background extends Module {
    private Shader shader;
    private Framebuffer fbo1, fbo2;

    private boolean enabled;
    private long fadeEndAt;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgBlur = settings.createGroup("Blur");
    private final SettingGroup sgScreens = settings.createGroup("Screens");

    // General

    private final Setting<BackgroundMode> backgroundMode = sgGeneral.add(new EnumSetting.Builder<BackgroundMode>()
        .name("mode")
        .description("Which mode the blur should use.")
        .defaultValue(BackgroundMode.Blur)
        .build()
    );

    private final Setting<Integer> fadeTime = sgGeneral.add(new IntSetting.Builder()
        .name("fade-time")
        .description("How long the will fade last in milliseconds.")
        .defaultValue(500)
        .min(0)
        .sliderMax(1000)
        .build()
    );

    // Blur

    private final Setting<BlurMode> blurMode = sgBlur.add(new EnumSetting.Builder<BlurMode>()
        .name("mode")
        .description("Which mode the blur should use.")
        .defaultValue(BlurMode.Fancy)
        .build()
    );

    private final Setting<Integer> blurRadius = sgBlur.add(new IntSetting.Builder()
        .name("radius")
        .description("How large the blur should be.")
        .defaultValue(5)
        .min(1)
        .sliderRange(1, 32)
        .build()
    );

    // Screens

    private final Setting<Boolean> mathax = sgScreens.add(new BoolSetting.Builder()
        .name("matHax")
        .description("Applies blur to MatHax screens.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> inventories = sgScreens.add(new BoolSetting.Builder()
        .name("inventories")
        .description("Applies blur to inventory screens.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> chat = sgScreens.add(new BoolSetting.Builder()
        .name("chat")
        .description("Applies blur when in chat.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> other = sgScreens.add(new BoolSetting.Builder()
        .name("other")
        .description("Applies blur to all other screen types.")
        .defaultValue(true)
        .build()
    );

    public Background() {
        super(Categories.Render, Items.TINTED_GLASS, "background", "Custom background when in GUI screens");

        // The listeners need to run even when the module is not enabled
        MatHax.EVENT_BUS.subscribe(new ConsumerListener<>(WindowResizedEvent.class, event -> {
            if (fbo1 != null) {
                fbo1.resize();
                fbo2.resize();
            }
        }));

        MatHax.EVENT_BUS.subscribe(new ConsumerListener<>(RenderAfterWorldEvent.class, event -> onRenderAfterWorld()));
    }

    private void onRenderAfterWorld() {
        // Enable / disable with fading
        boolean shouldRender = shouldRender();
        long time = Utils.getCurrentTimeMillis();

        if (enabled) {
            if (!shouldRender) {
                if (fadeEndAt == -1) fadeEndAt = Utils.getCurrentTimeMillis() + fadeTime.get();

                if (time >= fadeEndAt) {
                    enabled = false;
                    fadeEndAt = -1;
                }
            }
        } else {
            if (shouldRender) {
                enabled = true;
                fadeEndAt = Utils.getCurrentTimeMillis() + fadeTime.get();
            }
        }

        if (!enabled) return;

        switch (backgroundMode.get()) {
            case Blur:
            // Initialize shader and framebuffer if running for the first time
            if (shader == null) {
                shader = new Shader("background/blur.vert", "background/blur.frag");
                fbo1 = new Framebuffer();
                fbo2 = new Framebuffer();
            }

            // Prepare stuff for rendering
            int sourceTexture = mc.getFramebuffer().getColorAttachment();

            shader.bind();
            shader.set("u_Size", mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());
            shader.set("u_Texture", 0);

            // Update progress
            double progress = 1;

            if (time < fadeEndAt) {
                if (shouldRender) progress = 1 - (fadeEndAt - time) / fadeTime.get().doubleValue();
                else progress = (fadeEndAt - time) / fadeTime.get().doubleValue();
            } else {
                fadeEndAt = -1;
            }

            // Render the blur
            shader.set("u_Radius", Math.floor(blurRadius.get() * progress));

            PostProcessRenderer.beginRender();

            fbo1.bind();
            GL.bindTexture(sourceTexture);
            shader.set("u_Direction", 1.0, 0.0);
            PostProcessRenderer.render();

            if (blurMode.get() == BlurMode.Fancy) fbo2.bind();
            else fbo2.unbind();
            GL.bindTexture(fbo1.texture);
            shader.set("u_Direction", 0.0, 1.0);
            PostProcessRenderer.render();

            if (blurMode.get() == BlurMode.Fancy) {
                fbo1.bind();
                GL.bindTexture(fbo2.texture);
                shader.set("u_Direction", 1.0, 0.0);
                PostProcessRenderer.render();

                fbo2.unbind();
                GL.bindTexture(fbo1.texture);
                shader.set("u_Direction", 0.0, 1.0);
                PostProcessRenderer.render();
            }

            PostProcessRenderer.endRender();
        }
    }

    private boolean shouldRender() {
        if (!isActive()) return false;
        Screen screen = mc.currentScreen;

        if (screen instanceof WidgetScreen) return mathax.get();
        if (screen instanceof HandledScreen) return inventories.get();
        if (screen instanceof ChatScreen) return chat.get();
        if (screen != null) return other.get();

        return false;
    }

    public enum BackgroundMode {
        Blur
    }

    public enum BlurMode {
        Fancy,
        Fast
    }
}
