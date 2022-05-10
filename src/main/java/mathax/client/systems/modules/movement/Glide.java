package mathax.client.systems.modules.movement;

import mathax.client.eventbus.EventHandler;
import mathax.client.events.world.TickEvent;
import mathax.client.settings.BoolSetting;
import mathax.client.settings.DoubleSetting;
import mathax.client.settings.EnumSetting;
import mathax.client.settings.Setting;
import mathax.client.settings.SettingGroup;
import mathax.client.systems.modules.Categories;
import mathax.client.systems.modules.Module;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;

/*/------------------/*/
/*/ Made by Piotrek4 /*/
/*/------------------/*/

public class Glide extends Module {
	int x = 0;
	int z = 0;

	private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

	private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
	    .name("mode")
    	.description("Determines how Glide operates.")
    	.defaultValue(Mode.Normal)
    	.build()
	);

	private final Setting<Double> fallSpeed = sgGeneral.add(new DoubleSetting.Builder()
    	.name("fall-speed")
    	.description("Determines how fast to fall.")
    	.defaultValue(0.01)
    	.min(0.0000001)
    	.sliderRange(0.0000001, 0.6)
    	.build()
	);

	private final Setting<Boolean> defaultStrafe = sgGeneral.add(new BoolSetting.Builder()
    	.name("default-speed")
    	.description("Stops modifying your strafing speed.")
    	.defaultValue(true)
    	.build()
    );


	private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
    	.name("speed")
    	.description("Determines how fast to strafe.")
    	.defaultValue(0.2)
    	.min(0.001)
    	.sliderRange(0.050, 1.0)
        .visible(() -> !defaultStrafe.get())
    	.build()
	);

    public Glide() {
    	super(Categories.Movement, Items.FEATHER, "glide", "Makes you glide slowly while falling.");
    }

    @EventHandler
    private void onTick(final TickEvent.Post event) {
    	if (!defaultStrafe.get()) mc.player.airStrafingSpeed = speed.get().floatValue();

        if (mode.get() == Mode.Vertical) {
        	if (mc.player.isOnGround() || mc.player.isTouchingWater() || mc.player.isInLava() || mc.player.isClimbing()) return;

        	x = (int) mc.player.getVelocity().x;
        	z = (int) mc.player.getVelocity().z;

            mc.player.getAbilities().flying = false;

            mc.player.setVelocity(x, fallSpeed.get() * -1, z);
        } else if(mode.get() == Mode.Normal) {
        	if (mc.player.isOnGround() || mc.player.isTouchingWater() || mc.player.isInLava() || mc.player.isClimbing()) return;

        	Vec3d vel = mc.player.getVelocity();

        	mc.player.setVelocity(vel.x, Math.max(vel.y, -fallSpeed.get()), vel.z);
        }
    }

    public enum Mode {
        Normal("Normal"),
        Vertical("Vertical");

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
