package mathax.client.systems.modules.movement;

import mathax.client.eventbus.EventHandler;
import mathax.client.events.world.CollisionShapeEvent;
import mathax.client.events.world.TickEvent;
import mathax.client.settings.DoubleSetting;
import mathax.client.settings.EnumSetting;
import mathax.client.settings.Setting;
import mathax.client.settings.SettingGroup;
import mathax.client.systems.modules.Categories;
import mathax.client.systems.modules.Module;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;

public class Phase extends Module {
    private double prevX = Double.NaN;
    private double prevZ = Double.NaN;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("The phase mode used.")
        .defaultValue(Mode.NRNB)
        .onChanged(mode -> setPos())
        .build()
    );

    private final Setting<Double> distance = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed")
        .description("The X and Z distance per clip.")
        .defaultValue(0.1)
        .min(0)
        .sliderRange(0, 10.0)
        .visible(() -> mode.get() != Mode.Collision_Shape && mode.get() != Mode.No_NCP)
        .build()
    );

    private final Setting<Double> clipDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed")
        .description("Determines the clip distance.")
        .defaultValue(0.1)
        .range(0.01, 10)
        .sliderRange(0.01, 10)
        .visible(() -> mode.get() == Mode.No_NCP)
        .build()
    );

    public Phase() {
        super(Categories.Movement, Items.ELYTRA, "phase", "Lets you clip through ground sometimes.");
    }

    @Override
    public void onActivate() {
        if (mc.player == null) return;
        setPos();
    }

    @Override
    public void onDeactivate() {
        prevX = Double.NaN;
        prevZ = Double.NaN;
    }

    @EventHandler
    private void onCollisionShape(CollisionShapeEvent event) {
        if (mc.world == null || mc.player == null) return;
        if (mode.get() != Mode.Collision_Shape) return;
        if (event == null || event.pos == null) return;
        if (event.type != CollisionShapeEvent.CollisionType.BLOCK) return;
        if (event.pos.getY() < mc.player.getY()) {
            if (mc.player.isSneaking()) event.shape = VoxelShapes.empty();
        } else event.shape = VoxelShapes.empty();
    }

    @EventHandler
    public void onTick(TickEvent.Pre event){
        if (mode.get() != Mode.No_NCP) return;

        double blocks = clipDistance.get();
        if (!mc.player.isOnGround()) return;

        if (mc.options.keyForward.isPressed()){
            Vec3d forward = Vec3d.fromPolar(0, mc.player.getYaw());
            mc.player.updatePosition(mc.player.getX() + forward.x * blocks, mc.player.getY(), mc.player.getZ() + forward.z * blocks);
        }

        if (mc.options.keyBack.isPressed()){
            Vec3d forward = Vec3d.fromPolar(0, mc.player.getYaw() - 180);
            mc.player.updatePosition(mc.player.getX() + forward.x * blocks, mc.player.getY(), mc.player.getZ() + forward.z * blocks);
        }

        if (mc.options.keyLeft.isPressed()){
            Vec3d forward = Vec3d.fromPolar(0, mc.player.getYaw() - 90);
            mc.player.updatePosition(mc.player.getX() + forward.x * blocks, mc.player.getY(), mc.player.getZ() + forward.z * blocks);
        }

        if (mc.options.keyRight.isPressed()) {
            Vec3d forward = Vec3d.fromPolar(0, mc.player.getYaw() - 270);
            mc.player.updatePosition(mc.player.getX() + forward.x * blocks, mc.player.getY(), mc.player.getZ() + forward.z * blocks);
        }

        if (mc.options.keyJump.isPressed()) mc.player.updatePosition(mc.player.getX(), mc.player.getY() + 0.05, mc.player.getZ());

        if (mc.options.keySneak.isPressed()) mc.player.updatePosition(mc.player.getX(), mc.player.getY() - 0.05, mc.player.getZ());
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mode.get() == Mode.Collision_Shape || mode.get() == Mode.No_NCP) return;
        if (mc.player == null) return;

        if (Double.isNaN(prevX) || Double.isNaN(prevZ)) setPos();

        Vec3d yawForward = Vec3d.fromPolar(0.0f, mc.player.getYaw());
        Vec3d yawBack = Vec3d.fromPolar(0.0f, mc.player.getYaw() - 180f);
        Vec3d yawLeft = Vec3d.fromPolar(0.0f, mc.player.getYaw() - 90f);
        Vec3d yawRight = Vec3d.fromPolar(0.0f, mc.player.getYaw() - 270f);

        if (mode.get() == Mode.Normal) {
            if (mc.options.keyForward.isPressed()) mc.player.setPos(mc.player.getX() + yawForward.x * distance.get(), mc.player.getY(), mc.player.getZ() + yawForward.z * distance.get());

            if (mc.options.keyBack.isPressed()) mc.player.setPos(mc.player.getX() + yawBack.x * distance.get(), mc.player.getY(), mc.player.getZ() + yawBack.z * distance.get());

            if (mc.options.keyLeft.isPressed()) mc.player.setPos(mc.player.getX() + yawLeft.x * distance.get(), mc.player.getY(), mc.player.getZ() + yawLeft.z * distance.get());

            if (mc.options.keyRight.isPressed()) mc.player.setPos(mc.player.getX() + yawRight.x * distance.get(), mc.player.getY(), mc.player.getZ() + yawRight.z * distance.get());
        } else if (mode.get() == Mode.NRNB) {
            if (mc.options.keyForward.isPressed()) {
                prevX += yawForward.x * distance.get();
                prevZ += yawForward.z * distance.get();
                mc.player.setPos(prevX, mc.player.getY(), prevZ);
            }

            if (mc.options.keyBack.isPressed()) {
                prevX += yawBack.x * distance.get();
                prevZ += yawBack.z * distance.get();
                mc.player.setPos(prevX, mc.player.getY(), prevZ);
            }

            if (mc.options.keyLeft.isPressed()) {
                prevX += yawLeft.x * distance.get();
                prevZ += yawLeft.z * distance.get();
                mc.player.setPos(prevX, mc.player.getY(), prevZ);
            }

            if (mc.options.keyRight.isPressed()) {
                prevX += yawRight.x * distance.get();
                prevZ += yawRight.z * distance.get();
                mc.player.setPos(prevX, mc.player.getY(), prevZ);
            }
        }
    }

    private void setPos() {
        if (mc.player == null) return;
        prevX = mc.player.getX();
        prevZ = mc.player.getZ();
    }

    public enum Mode {
        NRNB("NRNB"),
        Normal("Normal"),
        No_NCP("No NCP"),
        Collision_Shape("Collision Shape");

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
