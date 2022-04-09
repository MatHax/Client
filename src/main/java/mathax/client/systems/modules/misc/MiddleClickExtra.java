package mathax.client.systems.modules.misc;

import mathax.client.eventbus.EventHandler;
import mathax.client.events.entity.player.FinishUsingItemEvent;
import mathax.client.events.entity.player.StoppedUsingItemEvent;
import mathax.client.events.mathax.MouseButtonEvent;
import mathax.client.events.world.TickEvent;
import mathax.client.settings.BoolSetting;
import mathax.client.settings.EnumSetting;
import mathax.client.settings.Setting;
import mathax.client.settings.SettingGroup;
import mathax.client.systems.modules.Categories;
import mathax.client.systems.modules.Module;
import mathax.client.utils.misc.input.KeyAction;
import mathax.client.utils.player.FindItemResult;
import mathax.client.utils.player.InvUtils;
import net.minecraft.item.BowItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_MIDDLE;

public class MiddleClickExtra extends Module {
    private boolean isUsing;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("Which item to use when you middle click.")
        .defaultValue(Mode.Pearl)
        .build()
    );

    private final Setting<Boolean> notify = sgGeneral.add(new BoolSetting.Builder()
        .name("notify")
        .description("Notifies you when you do not have the specified item in your hotbar.")
        .defaultValue(true)
        .build()
    );

    public MiddleClickExtra() {
        super(Categories.Misc, Items.STONE_BUTTON, "middle-click-extra", "Lets you use items when you middle click.");
    }

    @Override
    public void onDeactivate() {
        stopIfUsing();
    }

    @EventHandler
    private void onMouseButton(MouseButtonEvent event) {
        if (event.action != KeyAction.Press || event.button != GLFW_MOUSE_BUTTON_MIDDLE) return;

        FindItemResult result = InvUtils.findInHotbar(mode.get().item);

        if (!result.found()) {
            if (notify.get()) warning("Unable to find specified item.");
            return;
        }

        InvUtils.swap(result.slot(), true);

        switch (mode.get().type) {
            case Immediate -> {
                mc.interactionManager.interactItem(mc.player, mc.world, Hand.MAIN_HAND);
                InvUtils.swapBack();
            }
            case Longer_Single_Click -> mc.interactionManager.interactItem(mc.player, mc.world, Hand.MAIN_HAND);
            case Longer -> {
                mc.options.keyUse.setPressed(true);
                isUsing = true;
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (isUsing) {
            boolean pressed = true;
            if (mc.player.getMainHandStack().getItem() instanceof BowItem) pressed = BowItem.getPullProgress(mc.player.getItemUseTime()) < 1;
            mc.options.keyUse.setPressed(pressed);
        }
    }

    @EventHandler
    private void onFinishUsingItem(FinishUsingItemEvent event) {
        stopIfUsing();
    }

    @EventHandler
    private void onStoppedUsingItem(StoppedUsingItemEvent event) {
        stopIfUsing();
    }

    private void stopIfUsing() {
        if (isUsing) {
            mc.options.keyUse.setPressed(false);
            InvUtils.swapBack();
            isUsing = false;
        }
    }

    private enum Type {
        Immediate("Immediate"),
        Longer_Single_Click("Longer Single Click"),
        Longer("Longer");

        private final String title;

        Type(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    public enum Mode {
        Pearl("Pearl", Items.ENDER_PEARL, Type.Immediate),
        Rocket("Rocket", Items.FIREWORK_ROCKET, Type.Immediate),
        Rod("Rod", Items.FISHING_ROD, Type.Longer_Single_Click),
        Bow("Bow", Items.BOW, Type.Longer),
        Gap("Gap", Items.GOLDEN_APPLE, Type.Longer),
        EGap("EGap", Items.ENCHANTED_GOLDEN_APPLE, Type.Longer),
        Chorus("Chorus", Items.CHORUS_FRUIT, Type.Longer);

        private final String title;
        private final Item item;
        private final Type type;

        Mode(String title, Item item, Type type) {
            this.title = title;
            this.item = item;
            this.type = type;
        }

        @Override
        public String toString() {
            return title;
        }
    }
}
