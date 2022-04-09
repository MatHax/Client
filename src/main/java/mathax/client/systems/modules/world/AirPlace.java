package mathax.client.systems.modules.world;

import mathax.client.MatHax;
import mathax.client.eventbus.EventHandler;
import mathax.client.events.render.Render3DEvent;
import mathax.client.events.world.TickEvent;
import mathax.client.renderer.ShapeMode;
import mathax.client.settings.*;
import mathax.client.systems.modules.Categories;
import mathax.client.systems.modules.Module;
import mathax.client.utils.render.color.SettingColor;
import mathax.client.utils.world.BlockUtils;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;

public class AirPlace extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

    private final Setting<Boolean> render = sgGeneral.add(new BoolSetting.Builder()
        .name("render")
        .description("Renders a block overlay where the obsidian will be placed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("Determines how the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgGeneral.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The color of the sides of the blocks being rendered.")
        .defaultValue(new SettingColor(MatHax.INSTANCE.MATHAX_COLOR.r, MatHax.INSTANCE.MATHAX_COLOR.g, MatHax.INSTANCE.MATHAX_COLOR.b, 75))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The color of the lines of the blocks being rendered.")
        .defaultValue(new SettingColor(MatHax.INSTANCE.MATHAX_COLOR.r, MatHax.INSTANCE.MATHAX_COLOR.g, MatHax.INSTANCE.MATHAX_COLOR.b, 255))
        .build()
    );

    public AirPlace() {
        super(Categories.World, Items.BARRIER, "air-place", "Places a block where your crosshair is pointing at.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!(mc.crosshairTarget instanceof BlockHitResult) || !(mc.player.getMainHandStack().getItem() instanceof BlockItem)) return;
        if (mc.options.keyUse.isPressed()) BlockUtils.place(((BlockHitResult) mc.crosshairTarget).getBlockPos(), Hand.MAIN_HAND, mc.player.getInventory().selectedSlot, false, 0, true, true, false);
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (!(mc.crosshairTarget instanceof BlockHitResult) || !mc.world.getBlockState(((BlockHitResult) mc.crosshairTarget).getBlockPos()).getMaterial().isReplaceable() || !(mc.player.getMainHandStack().getItem() instanceof BlockItem) || !render.get()) return;
        event.renderer.box(((BlockHitResult) mc.crosshairTarget).getBlockPos(), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }
}
