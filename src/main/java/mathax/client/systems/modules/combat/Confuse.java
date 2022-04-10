package mathax.client.systems.modules.combat;

import mathax.client.MatHax;
import mathax.client.eventbus.EventHandler;
import mathax.client.events.render.Render3DEvent;
import mathax.client.events.world.TickEvent;
import mathax.client.settings.*;
import mathax.client.systems.modules.Categories;
import mathax.client.systems.modules.Module;
import mathax.client.utils.Utils;
import mathax.client.utils.entity.SortPriority;
import mathax.client.utils.entity.TargetUtils;
import mathax.client.utils.render.color.Color;
import mathax.client.utils.render.color.SettingColor;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.Random;

/*/------------------------------------------------------------------------------------------------------------/*/
/*/ Used from Meteor Rejects                                                                                   /*/
/*/ https://github.com/AntiCope/meteor-rejects/blob/master/src/main/java/anticope/rejects/modules/Confuse.java /*/
/*/------------------------------------------------------------------------------------------------------------/*/

public class Confuse extends Module {
    private final Random random = new Random();

    Entity target = null;

    double circleProgress = 0;
    double addition = 0.0;

    int delayWaited = 0;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .defaultValue(Mode.Random_TP)
        .description("Mode")
        .build()
    );

    private final Setting<Integer> delay  = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay")
        .defaultValue(3)
        .min(0)
        .sliderRange(0, 20)
        .build()
    );

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("radius")
        .description("Range to confuse opponents")
        .defaultValue(6)
        .min(0)
        .sliderRange(0, 10)
        .build()
    );

    private final Setting<SortPriority> targetPriority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
        .name("target-priority")
        .description("How to select the player to target.")
        .defaultValue(SortPriority.Lowest_Health)
        .build()
    );

    private final Setting<Integer> circleSpeed  = sgGeneral.add(new IntSetting.Builder()
        .name("circle-speed")
        .description("Circle mode speed.")
        .defaultValue(10)
        .min(1)
        .sliderRange(1, 180)
        .build()
    );

    private final Setting<Boolean> moveThroughBlocks = sgGeneral.add(new BoolSetting.Builder()
        .name("move-through-blocks")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> budgetGraphics = sgGeneral.add(new BoolSetting.Builder()
        .name("budget-graphics")
        .defaultValue(false)
        .build()
    );

    private final Setting<SettingColor> circleColor = sgGeneral.add(new ColorSetting.Builder()
        .name("circle-color")
        .description("Color for circle rendering")
        .defaultValue(new SettingColor(MatHax.INSTANCE.MATHAX_COLOR.r, MatHax.INSTANCE.MATHAX_COLOR.g, MatHax.INSTANCE.MATHAX_COLOR.b))
        .visible(budgetGraphics::get)
        .build()
    );

    public Confuse() {
        super(Categories.Combat, Items.COMPASS, "confuse", "Confuses your targets.");
    }

    @Override
    public void onActivate() {
        delayWaited = 0;
        circleProgress = 0;
        addition = 0.0;
        target = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        delayWaited++;
        if (delayWaited < delay.get()) return;
        delayWaited = 0;

        target = TargetUtils.getPlayerTarget(range.get(), targetPriority.get());

        if (target == null) return;

        Vec3d entityPos = target.getPos();
        Vec3d playerPos = mc.player.getPos();

        BlockHitResult hit;
        int halfRange = range.get() / 2;

        switch (mode.get()) {
            case Random_TP -> {
                double x = random.nextDouble() * range.get() - halfRange;
                double y = 0;
                double z = random.nextDouble() * range.get() - halfRange;
                Vec3d addend = new Vec3d(x, y, z);
                Vec3d goal = entityPos.add(addend);
                if (mc.world.getBlockState(new BlockPos(goal.x, goal.y, goal.z)).getBlock() != Blocks.AIR) goal = new Vec3d(x, playerPos.y, z);
                if (mc.world.getBlockState(new BlockPos(goal.x, goal.y, goal.z)).getBlock() == Blocks.AIR) {
                    hit = mc.world.raycast(new RaycastContext(mc.player.getPos(), goal, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, mc.player));
                    if (!moveThroughBlocks.get() && hit.isInsideBlock()) {
                        delayWaited = delay.get() - 1;
                        break;
                    }

                    mc.player.updatePosition(goal.x, goal.y, goal.z);
                } else delayWaited = delay.get() - 1;
            }
            case Switch -> {
                Vec3d diff = entityPos.subtract(playerPos);
                Vec3d diff1 = new Vec3d(Utils.clamp(diff.x, -halfRange, halfRange), Utils.clamp(diff.y, -halfRange, halfRange), Utils.clamp(diff.z, -halfRange, halfRange));
                Vec3d goal2 = entityPos.add(diff1);
                hit = mc.world.raycast(new RaycastContext(mc.player.getPos(), goal2, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, mc.player));
                if (!moveThroughBlocks.get() && hit.isInsideBlock()) {
                    delayWaited = delay.get() - 1;
                    break;
                }
                mc.player.updatePosition(goal2.x, goal2.y, goal2.z);
            }
            case Circle -> {
                delay.set(0);
                circleProgress += circleSpeed.get();
                if (circleProgress > 360) circleProgress -= 360;
                double rad = Math.toRadians(circleProgress);
                double sin = Math.sin(rad) * 3;
                double cos = Math.cos(rad) * 3;
                Vec3d current = new Vec3d(entityPos.x + sin, playerPos.y, entityPos.z + cos);
                hit = mc.world.raycast(new RaycastContext(mc.player.getPos(), current, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, mc.player));
                if (!moveThroughBlocks.get() && hit.isInsideBlock()) break;
                mc.player.updatePosition(current.x, current.y, current.z);
            }
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (target == null) return;

        boolean flag = budgetGraphics.get();
        Vec3d last = null;
        addition += flag ? 0 : 1.0;
        if (addition > 360) addition = 0;
        for (int i = 0; i < 360; i += flag ? 7 : 1) {
            Color color;
            if (flag) color = circleColor.get();
            else {
                double rot = (255.0 * 3) * (((((double) i) + addition) % 360) / 360.0);
                int seed = (int) Math.floor(rot / 255.0);
                double current = rot % 255;
                double red = seed == 0 ? current : (seed == 1 ? Math.abs(current - 255) : 0);
                double green = seed == 1 ? current : (seed == 2 ? Math.abs(current - 255) : 0);
                double blue = seed == 2 ? current : (seed == 0 ? Math.abs(current - 255) : 0);

                color = new Color((int) red, (int) green, (int) blue);
            }

            Vec3d tp = target.getPos();
            double rad = Math.toRadians(i);
            double sin = Math.sin(rad) * 3;
            double cos = Math.cos(rad) * 3;
            Vec3d vec3d = new Vec3d(tp.x + sin, tp.y + target.getHeight() / 2, tp.z + cos);
            if (last != null) event.renderer.line(last.x, last.y, last.z, vec3d.x, vec3d.y, vec3d.z, color);
            last = vec3d;
        }
    }

    public enum Mode {
        Random_TP("Random TP"),
        Switch("Switch"),
        Circle("Client");

        private final String title;

        Mode(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }
}
