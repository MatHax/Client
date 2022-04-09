package mathax.client.systems.modules.movement;

import mathax.client.eventbus.EventHandler;
import mathax.client.events.entity.BoatMoveEvent;
import mathax.client.mixininterface.IVec3d;
import mathax.client.settings.BoolSetting;
import mathax.client.settings.DoubleSetting;
import mathax.client.settings.Setting;
import mathax.client.settings.SettingGroup;
import mathax.client.systems.modules.Categories;
import mathax.client.systems.modules.Module;
import mathax.client.utils.player.PlayerUtils;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;

/*/--------------------------------------------------------------------------------------------------------------/*/
/*/ Used from Meteor Rejects                                                                                     /*/
/*/ https://github.com/AntiCope/meteor-rejects/blob/master/src/main/java/anticope/rejects/modules/BoatPhase.java /*/
/*/--------------------------------------------------------------------------------------------------------------/*/

public class BoatPhase extends Module {
    private BoatEntity boat = null;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSpeed = settings.createGroup("Speed");

    // General

    private final Setting<Boolean> lockYaw = sgGeneral.add(new BoolSetting.Builder()
        .name("lock-boat-yaw")
        .description("Locks boat yaw to the direction you're facing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> verticalControl = sgGeneral.add(new BoolSetting.Builder()
        .name("vertical-control")
        .description("Whether or not space/ctrl can be used to move vertically.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> adjustHorizontalSpeed = sgGeneral.add(new BoolSetting.Builder()
        .name("adjust-horizontal-speed")
        .description("Whether or not horizontal speed is modified.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> fall = sgGeneral.add(new BoolSetting.Builder()
        .name("fall")
        .description("Toggles vertical glide.")
        .defaultValue(false)
        .build()
    );

    // Speed

    private final Setting<Double> horizontalSpeed = sgSpeed.add(new DoubleSetting.Builder()
        .name("horizontal-speed")
        .description("Horizontal speed in blocks per second.")
        .defaultValue(10)
        .min(0)
        .sliderRange(0, 50)
        .build()
    );

    private final Setting<Double> verticalSpeed = sgSpeed.add(new DoubleSetting.Builder()
        .name("vertical-speed")
        .description("Vertical speed in blocks per second.")
        .defaultValue(5)
        .min(0)
        .sliderRange(0, 20)
        .build()
    );

    private final Setting<Double> fallSpeed = sgSpeed.add(new DoubleSetting.Builder()
        .name("fall-speed")
        .description("How fast you fall in blocks per second.")
        .defaultValue(0.625)
        .min(0)
        .sliderRange(0, 10)
        .build()
    );

    public BoatPhase() {
        super(Categories.Movement, Items.OAK_BOAT, "boat-phase", "Phase through blocks using a boat.");
    }

    @Override
    public void onActivate() {
        boat = null;
    }

    @Override
    public void onDeactivate() {
        if (boat != null) boat.noClip = false;
    }

    @EventHandler
    private void onBoatMove(BoatMoveEvent event) {
        if (mc.player.getVehicle() != null && mc.player.getVehicle().getType().equals(EntityType.BOAT)) {
            if (boat != mc.player.getVehicle()) {
                if (boat != null) boat.noClip = false;
                boat = (BoatEntity) mc.player.getVehicle();
            }
        } else boat = null;

        if (boat != null) {
            boat.noClip = true;
            //boat.pushSpeedReduction = 1;

            if (lockYaw.get()) boat.setYaw(mc.player.getYaw());

            Vec3d vel;

            if (adjustHorizontalSpeed.get()) vel = PlayerUtils.getHorizontalVelocity(horizontalSpeed.get());
            else vel = boat.getVelocity();

            double velX = vel.x;
            double velY = 0;
            double velZ = vel.z;

            if (verticalControl.get()) {
                if (mc.options.keyJump.isPressed()) velY += verticalSpeed.get() / 20;
                if (mc.options.keySprint.isPressed()) velY -= verticalSpeed.get() / 20;
                else if (fall.get()) velY -= fallSpeed.get() / 20;
            } else if (fall.get()) velY -= fallSpeed.get() / 20;

            ((IVec3d) boat.getVelocity()).set(velX,velY,velZ);
        }
    }
}
