package mathax.client.systems.modules.movement;

import mathax.client.MatHax;
import mathax.client.eventbus.EventHandler;
import mathax.client.events.mathax.KeyEvent;
import mathax.client.events.world.TickEvent;
import mathax.client.gui.WidgetScreen;
import mathax.client.mixin.CreativeInventoryScreenAccessor;
import mathax.client.mixin.KeyBindingAccessor;
import mathax.client.settings.*;
import mathax.client.systems.modules.Categories;
import mathax.client.systems.modules.Module;
import mathax.client.utils.Utils;
import mathax.client.utils.misc.input.Input;
import mathax.client.utils.misc.input.KeyAction;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Items;

import static org.lwjgl.glfw.GLFW.*;

public class GUIMove extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

    private final Setting<Screens> screens = sgGeneral.add(new EnumSetting.Builder<Screens>()
        .name("guis")
        .description("Which GUIs to move in.")
        .defaultValue(Screens.Inventory)
        .build()
    );

    private final Setting<Boolean> jump = sgGeneral.add(new BoolSetting.Builder()
        .name("jump")
        .description("Allows you to jump while in GUIs.")
        .defaultValue(true)
        .onChanged(aBoolean -> {
            if (isActive() && !aBoolean) set(mc.options.keyJump, false);
        })
        .build()
    );

    private final Setting<Boolean> sneak = sgGeneral.add(new BoolSetting.Builder()
        .name("sneak")
        .description("Allows you to sneak while in GUIs.")
        .defaultValue(true)
        .onChanged(aBoolean -> {
            if (isActive() && !aBoolean) set(mc.options.keySneak, false);
        })
        .build()
    );

    private final Setting<Boolean> sprint = sgGeneral.add(new BoolSetting.Builder()
        .name("sprint")
        .description("Allows you to sprint while in GUIs.")
        .defaultValue(true)
        .onChanged(aBoolean -> {
            if (isActive() && !aBoolean) set(mc.options.keySprint, false);
        })
        .build()
    );

    private final Setting<Boolean> arrowsRotate = sgGeneral.add(new BoolSetting.Builder()
        .name("arrows-rotate")
        .description("Allows you to use your arrow keys to rotate while in GUIs.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> rotateSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("rotate-speed")
        .description("Rotation speed while in GUIs.")
        .defaultValue(4)
        .min(0)
        .sliderRange(0, 10)
        .build()
    );

    public GUIMove() {
        super(Categories.Movement, Items.DIAMOND_BOOTS, "gui-move", "Allows you to perform various actions while in GUIs.");
    }

    @Override
    public void onDeactivate() {
        set(mc.options.keyForward, false);
        set(mc.options.keyBack, false);
        set(mc.options.keyLeft, false);
        set(mc.options.keyRight, false);

        if (jump.get()) set(mc.options.keyJump, false);
        if (sneak.get()) set(mc.options.keySneak, false);
        if (sprint.get()) set(mc.options.keySprint, false);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (skip()) return;
        if (screens.get() == Screens.GUI && !(mc.currentScreen instanceof WidgetScreen)) return;
        if (screens.get() == Screens.Inventory && mc.currentScreen instanceof WidgetScreen) return;

        set(mc.options.keyForward, Input.isPressed(mc.options.keyForward));
        set(mc.options.keyBack, Input.isPressed(mc.options.keyBack));
        set(mc.options.keyLeft, Input.isPressed(mc.options.keyLeft));
        set(mc.options.keyRight, Input.isPressed(mc.options.keyRight));

        if (jump.get()) set(mc.options.keyJump, Input.isPressed(mc.options.keyJump));
        if (sneak.get()) set(mc.options.keySneak, Input.isPressed(mc.options.keySneak));
        if (sprint.get()) set(mc.options.keySprint, Input.isPressed(mc.options.keySprint));

        if (arrowsRotate.get()) {
            float yaw = mc.player.getYaw();
            float pitch = mc.player.getPitch();

            for (int i = 0; i < (rotateSpeed.get() * 2); i++) {
                if (Input.isKeyPressed(GLFW_KEY_LEFT)) yaw -= 0.5;
                if (Input.isKeyPressed(GLFW_KEY_RIGHT)) yaw += 0.5;
                if (Input.isKeyPressed(GLFW_KEY_UP)) pitch -= 0.5;
                if (Input.isKeyPressed(GLFW_KEY_DOWN)) pitch += 0.5;
            }

            pitch = Utils.clamp(pitch, -90, 90);

            mc.player.setYaw(yaw);
            mc.player.setPitch(pitch);
        }
    }

    private void set(KeyBinding bind, boolean pressed) {
        boolean wasPressed = bind.isPressed();
        bind.setPressed(pressed);

        InputUtil.Key key = ((KeyBindingAccessor) bind).getKey();
        if (wasPressed != pressed && key.getCategory() == InputUtil.Type.KEYSYM) MatHax.EVENT_BUS.post(KeyEvent.get(key.getCode(), 0, pressed ? KeyAction.Press : KeyAction.Release));
    }

    public boolean skip() {
        return mc.currentScreen == null || (mc.currentScreen instanceof CreativeInventoryScreen && CreativeInventoryScreenAccessor.getSelectedTab() == ItemGroup.SEARCH.getIndex()) || mc.currentScreen instanceof ChatScreen || mc.currentScreen instanceof SignEditScreen || mc.currentScreen instanceof AnvilScreen || mc.currentScreen instanceof AbstractCommandBlockScreen || mc.currentScreen instanceof StructureBlockScreen;
    }

    public enum Screens {
        GUI("GUI"),
        Inventory("Inventory"),
        Both("Both");

        private final String title;

        Screens(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }
}
