package mathax.client.mixin;

import mathax.client.MatHax;
import mathax.client.events.entity.player.InteractEvent;
import mathax.client.events.entity.player.ItemUseCrosshairTargetEvent;
import mathax.client.events.game.GameLeftEvent;
import mathax.client.events.game.OpenScreenEvent;
import mathax.client.events.game.ResourcePacksReloadedEvent;
import mathax.client.events.game.WindowResizedEvent;
import mathax.client.events.world.TickEvent;
import mathax.client.mixininterface.IMinecraftClient;
import mathax.client.systems.modules.Modules;
import mathax.client.systems.modules.render.UnfocusedCPU;
import mathax.client.utils.Utils;
import mathax.client.gui.WidgetScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.util.Window;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Mixin(value = MinecraftClient.class, priority = 1001)
public abstract class MinecraftClientMixin implements IMinecraftClient {
    @Unique
    private boolean doItemUseCalled;

    @Unique
    private boolean rightClick;

    @Unique
    private long lastTime;

    @Unique
    private boolean firstFrame;

    @Shadow public ClientWorld world;

    @Shadow
    @Final
    public Mouse mouse;

    @Shadow
    @Final
    private Window window;

    @Shadow
    public Screen currentScreen;

    @Shadow
    protected abstract void doItemUse();

    @Shadow
    public abstract Profiler getProfiler();

    @Shadow
    public abstract boolean isWindowFocused();

    @Shadow
    @Nullable
    public ClientPlayerInteractionManager interactionManager;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo info) {
        MatHax.INSTANCE.onInitializeClient();
        firstFrame = true;
    }

    @Inject(method = "updateWindowTitle()V", at = @At("HEAD"), cancellable = true)
    private void updateTitle(final CallbackInfo info){
        info.cancel();
    }

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    public void setScreen(Screen screen, CallbackInfo info) {
        OpenScreenEvent.getOpenedScreen event = new OpenScreenEvent.getOpenedScreen(screen);
        MatHax.EVENT_BUS.post(event);

        if (event.isCancelled()) info.cancel();
    }

    @Inject(at = @At("HEAD"), method = "tick")
    private void onPreTick(CallbackInfo info) {
        doItemUseCalled = false;

        getProfiler().push("mathax_pre_update");
        MatHax.EVENT_BUS.post(TickEvent.Pre.get());
        getProfiler().pop();

        if (rightClick && !doItemUseCalled && interactionManager != null) doItemUse();
        rightClick = false;
    }

    @Inject(at = @At("TAIL"), method = "tick")
    private void onTick(CallbackInfo info) {
        getProfiler().push("mathax_post_update");
        MatHax.EVENT_BUS.post(TickEvent.Post.get());
        getProfiler().pop();
    }

    @Inject(method = "doItemUse", at = @At("HEAD"))
    private void onDoItemUse(CallbackInfo info) {
        doItemUseCalled = true;
    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At("HEAD"))
    private void onDisconnect(Screen screen, CallbackInfo info) {
        if (world != null) MatHax.EVENT_BUS.post(GameLeftEvent.get());
    }

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void onSetScreen(Screen screen, CallbackInfo info) {
        if (screen instanceof WidgetScreen) screen.mouseMoved(mouse.getX() * window.getScaleFactor(), mouse.getY() * window.getScaleFactor());

        OpenScreenEvent event = OpenScreenEvent.get(screen);
        MatHax.EVENT_BUS.post(event);

        if (event.isCancelled()) info.cancel();
    }

    @Redirect(method = "doItemUse", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;crosshairTarget:Lnet/minecraft/util/hit/HitResult;", ordinal = 1))
    private HitResult doItemUseMinecraftClientCrosshairTargetProxy(MinecraftClient client) {
        return MatHax.EVENT_BUS.post(ItemUseCrosshairTargetEvent.get(client.crosshairTarget)).target;
    }

    @ModifyVariable(method = "reloadResources(Z)Ljava/util/concurrent/CompletableFuture;", at = @At("STORE"), ordinal = 0)
    private CompletableFuture<Void> onReloadResourcesNewCompletableFuture(CompletableFuture<Void> completableFuture) {
        completableFuture.thenRun(() -> MatHax.EVENT_BUS.post(ResourcePacksReloadedEvent.get()));
        return completableFuture;
    }

    @Inject(method = "onResolutionChanged", at = @At("TAIL"))
    private void onResolutionChanged(CallbackInfo info) {
        MatHax.EVENT_BUS.post(WindowResizedEvent.get());
    }

    @Inject(method = "getFramerateLimit", at = @At("HEAD"), cancellable = true)
    private void onGetFramerateLimit(CallbackInfoReturnable<Integer> infor) {
        if (Modules.get().isActive(UnfocusedCPU.class) && !isWindowFocused()) infor.setReturnValue(1);
    }

    @Redirect(method = "handleBlockBreaking", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"))
    public boolean breakBlockCheck(ClientPlayerEntity clientPlayerEntity) {
        return MatHax.EVENT_BUS.post(InteractEvent.get(clientPlayerEntity.isUsingItem())).usingItem;
    }

    @Redirect(method = "doItemUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;isBreakingBlock()Z"))
    public boolean useItemBreakCheck(ClientPlayerInteractionManager clientPlayerInteractionManager) {
        return MatHax.EVENT_BUS.post(InteractEvent.get(clientPlayerInteractionManager.isBreakingBlock())).usingItem;
    }

    // Time delta

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(CallbackInfo info) {
        long time = Utils.getCurrentTimeMillis();

        if (firstFrame) {
            lastTime = time;
            firstFrame = false;
        }

        Utils.frameTime = (time - lastTime) / 1000.0;
        lastTime = time;
    }

    // Interface

    @Override
    public void rightClick() {
        rightClick = true;
    }

    // Music

    private static final Set<Screen> musicPassingScreens = new HashSet<>();
    @Redirect(method = "setScreen(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;currentScreen:Lnet/minecraft/client/gui/screen/Screen;", opcode = Opcodes.PUTFIELD))
    public void preInitScreen(MinecraftClient client, Screen screen) {
        if (screen != null) {
            musicPassingScreens.add(screen);
            screen.init((MinecraftClient)(Object)this, window.getScaledWidth(), window.getScaledHeight());
        }

        client.currentScreen = screen;
    }

    @Redirect(method = "setScreen(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;init(Lnet/minecraft/client/MinecraftClient;II)V"))
    private void disableInit(Screen screen, MinecraftClient client, int width, int height) {
        if (musicPassingScreens.remove(screen)) screen.init(client, width, height);
    }
}
