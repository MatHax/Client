package mathax.client.mixin;

import mathax.client.MatHax;
import mathax.client.events.mathax.CharTypedEvent;
import mathax.client.events.mathax.KeyEvent;
import mathax.client.utils.Utils;
import mathax.client.utils.misc.input.Input;
import mathax.client.utils.misc.input.KeyAction;
import mathax.client.gui.GuiKeyEvents;
import mathax.client.gui.WidgetScreen;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public abstract class KeyboardMixin {
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    public void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo info) {
        if (key != GLFW.GLFW_KEY_UNKNOWN) {
            if (client.currentScreen instanceof WidgetScreen && action == GLFW.GLFW_REPEAT) ((WidgetScreen) client.currentScreen).keyRepeated(key, modifiers);

            if (GuiKeyEvents.cankeyUses) {
                Input.setKeyState(key, action != GLFW.GLFW_RELEASE);
                if (MatHax.EVENT_BUS.post(KeyEvent.get(key, modifiers, KeyAction.get(action))).isCancelled()) info.cancel();
            }
        }
    }

    @Inject(method = "onChar", at = @At("HEAD"), cancellable = true)
    private void onChar(long window, int i, int j, CallbackInfo info) {
        if (Utils.canUpdate() && !client.isPaused() && (client.currentScreen == null || client.currentScreen instanceof WidgetScreen)) {
            if (MatHax.EVENT_BUS.post(CharTypedEvent.get((char) i)).isCancelled()) info.cancel();
        }
    }
}
