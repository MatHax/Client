package mathax.client.mixin;

import net.minecraft.client.gui.screen.SplashOverlay;
import org.spongepowered.asm.mixin.Mixin;

// TODO: Fix.

@Mixin(SplashOverlay.class)
public abstract class SplashOverlayMixin {/*
    @Mutable
    @Shadow
    @Final
    static Identifier LOGO;

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    @Final
    private boolean reloading;

    @Shadow
    @Final
    private ResourceReload reload;

    @Shadow
    private float progress;

    @Shadow
    private long reloadCompleteTime;

    @Shadow
    private long reloadStartTime;

    @Shadow
    @Final
    private Consumer<Optional<Throwable>> exceptionHandler;

    private static final Identifier MATHAX_LOGO = new Identifier("mathax", "textures/icons/icon.png");

    private static final int BACKGROUND_COLOR = ColorHelper.Argb.getArgb(255, 30, 30, 45);

    @Inject(method = "init(Lnet/minecraft/client/MinecraftClient;)V", at = @At("HEAD"), cancellable = true)
    private static void init(MinecraftClient client, CallbackInfo info) {
        LOGO = new Identifier("mathax", "textures/blank.png");
        info.cancel();
    }

    @Inject(at = @At("TAIL"), method = "render")
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo info) {
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        long time = Util.getMeasuringTimeMs();

        if (reloading && reloadStartTime == -1L) reloadStartTime = time;

        float completeTime = reloadCompleteTime > -1L ? (float)(time - reloadCompleteTime) / 1000.0F : -1.0F;
        float startTime = reloadStartTime > -1L ? (float)(time - reloadStartTime) / 500.0F : -1.0F;

        int color;
        float floatTime;
        if (completeTime >= 1.0F) {
            if (client.currentScreen != null) client.currentScreen.render(matrices, 0, 0, delta);

            floatTime = 1.0F - MathHelper.clamp(completeTime - 1.0F, 0.0F, 1.0F);
            color = MathHelper.ceil((1.0F - MathHelper.clamp(completeTime - 1.0F, 0.0F, 1.0F)) * 255.0F);
            DrawableHelper.fill(matrices, 0, 0, width, height, withAlpha(BACKGROUND_COLOR, color));
        } else if (reloading) {
            if (client.currentScreen != null && startTime < 1.0F) client.currentScreen.render(matrices, mouseX, mouseY, delta);

            floatTime = MathHelper.clamp(startTime, 0.0F, 1.0F);
            color = MathHelper.ceil(MathHelper.clamp(startTime, 0.15D, 1.0D) * 255.0D);
            DrawableHelper.fill(matrices, 0, 0, width, height, withAlpha(BACKGROUND_COLOR, color));
        } else {
            floatTime = 1.0F;
            color = BACKGROUND_COLOR;
            GlStateManager._clearColor((float) (color >> 16 & 255) / 255.0F, (float) (color >> 8 & 255) / 255.0F, (float) (color & 255) / 255.0F, 1.0F);
            GlStateManager._clear(16384, MinecraftClient.IS_SYSTEM_MAC);
        }

        color = (int) ((double) client.getWindow().getScaledWidth() * 0.5D);
        double res = Math.min((double) client.getWindow().getScaledWidth() * 0.75D, client.getWindow().getScaledHeight()) * 0.25D;
        int resCalculated = (int) ((res * 4.0D) * 0.5D);

        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.setShaderTexture(0, MATHAX_LOGO);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, floatTime);
        DrawableHelper.drawTexture(matrices, color - (resCalculated / 2), (int) (res * 0.5D), resCalculated, resCalculated, 0, 0, 512, 512, 512, 512);

        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();

        int r = (int) ((double) client.getWindow().getScaledHeight() * 0.8325D);

        progress = MathHelper.clamp(progress * 0.95F + reload.getProgress() * 0.050000012F, 0.0F, 1.0F);

        if (completeTime < 1.0F) renderProgressBar(matrices, width / 2 - resCalculated, r - 5, width / 2 + resCalculated, r + 5, 1.0F - MathHelper.clamp(completeTime, 0.0F, 1.0F), null);

        if (completeTime >= 2.0F) client.setOverlay(null);

        if (reloadCompleteTime == -1L && reload.isComplete() && (!reloading || startTime >= 2.0F)) {
            try {
                reload.throwException();
                exceptionHandler.accept(Optional.empty());
            } catch (Throwable throwable) {
                exceptionHandler.accept(Optional.of(throwable));
            }

            reloadCompleteTime = Util.getMeasuringTimeMs();
            if (client.currentScreen != null) client.currentScreen.init(client, width, height);
        }
    }

    @Inject(method = "renderProgressBar", at = @At("TAIL"))
    private void renderProgressBar(MatrixStack matrices, int x1, int y1, int x2, int y2, float opacity, CallbackInfo info) {
        int progress = MathHelper.ceil((float) (x2 - x1 - 2) * this.progress);
        int color = ColorHelper.Argb.getArgb(Math.round(opacity * 255.0F), 230, 75, 100);

        DrawableHelper.fill(matrices, x1 + 2, y1 + 2, x1 + progress, y2 - 2, color);
        DrawableHelper.fill(matrices, x1 + 1, y1, x2 - 1, y1 + 1, color);
        DrawableHelper.fill(matrices, x1 + 1, y2, x2 - 1, y2 - 1, color);
        DrawableHelper.fill(matrices, x1, y1, x1 + 1, y2, color);
        DrawableHelper.fill(matrices, x2, y1, x2 - 1, y2, color);
    }

    private static int withAlpha(int color, int alpha) {
        return color | alpha << 24;
    }*/
}
