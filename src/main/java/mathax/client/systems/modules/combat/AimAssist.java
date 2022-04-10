package mathax.client.systems.modules.combat;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import mathax.client.eventbus.EventHandler;
import mathax.client.events.render.Render3DEvent;
import mathax.client.events.world.TickEvent;
import mathax.client.settings.*;
import mathax.client.systems.friends.Friends;
import mathax.client.systems.modules.Categories;
import mathax.client.systems.modules.Module;
import mathax.client.utils.Utils;
import mathax.client.utils.entity.EntityUtils;
import mathax.client.utils.entity.SortPriority;
import mathax.client.utils.entity.Target;
import mathax.client.utils.entity.TargetUtils;
import mathax.client.utils.misc.Vec3;
import mathax.client.utils.player.PlayerUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;

public class AimAssist extends Module {
    private final Vec3 vec3d1 = new Vec3();

    private Entity target;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSpeed = settings.createGroup("Aim Speed");

    // General

    private final Setting<Object2BooleanMap<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("Entities to aim at.")
        .defaultValue(Utils.asO2BMap(EntityType.PLAYER))
        .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("The range at which an entity can be targeted.")
        .defaultValue(5)
        .min(0)
        .sliderRange(0, 10)
        .build()
    );

    private final Setting<Boolean> ignoreWalls = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-walls")
        .description("Whether or not to ignore aiming through walls.")
        .defaultValue(false)
        .build()
    );

    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
        .name("priority")
        .description("How to select target from entities in range.")
        .defaultValue(SortPriority.Lowest_Health)
        .build()
    );

    private final Setting<Target> bodyTarget = sgGeneral.add(new EnumSetting.Builder<Target>()
        .name("aim-target")
        .description("Which part of the entities body to aim at.")
        .defaultValue(Target.Body)
        .build()
    );

    // Aim Speed

    private final Setting<Boolean> instant = sgSpeed.add(new BoolSetting.Builder()
        .name("instant-look")
        .description("Instantly looks at the entity.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> speed = sgSpeed.add(new DoubleSetting.Builder()
        .name("speed")
        .description("How fast to aim at the entity.")
        .defaultValue(5)
        .min(0)
        .sliderRange(0, 10)
        .visible(() -> !instant.get())
        .build()
    );

    public AimAssist() {
        super(Categories.Combat, Items.ARMOR_STAND, "aim-assist", "Automatically aims at entities.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        target = TargetUtils.get(entity -> {
            if (!entity.isAlive()) return false;
            if (mc.player.distanceTo(entity) >= range.get()) return false;
            if (!ignoreWalls.get() && !PlayerUtils.canSeeEntity(entity)) return false;
            if (entity == mc.player || !entities.get().getBoolean(entity.getType())) return false;
            if (entity instanceof PlayerEntity) return Friends.get().shouldAttack((PlayerEntity) entity);
            return true;
        }, priority.get());
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (target != null) aim(target, event.tickDelta, instant.get());
    }

    private void aim(Entity target, double delta, boolean instant) {
        vec3d1.set(target, delta);

        switch (bodyTarget.get()) {
            case Head -> vec3d1.add(0, target.getEyeHeight(target.getPose()), 0);
            case Body -> vec3d1.add(0, target.getEyeHeight(target.getPose()) / 2, 0);
        }

        double deltaX = vec3d1.x - mc.player.getX();
        double deltaZ = vec3d1.z - mc.player.getZ();
        double deltaY = vec3d1.y - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));

        double angle = Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90;
        double deltaAngle;
        double toRotate;

        if (instant) mc.player.setYaw((float) angle);
        else {
            deltaAngle = MathHelper.wrapDegrees(angle - mc.player.getYaw());
            toRotate = speed.get() * (deltaAngle >= 0 ? 1 : -1) * delta;
            if ((toRotate >= 0 && toRotate > deltaAngle) || (toRotate < 0 && toRotate < deltaAngle)) toRotate = deltaAngle;
            mc.player.setYaw(mc.player.getYaw() + (float) toRotate);
        }

        double idk = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        angle = -Math.toDegrees(Math.atan2(deltaY, idk));

        if (instant) mc.player.setPitch((float) angle);
        else {
            deltaAngle = MathHelper.wrapDegrees(angle - mc.player.getPitch());
            toRotate = speed.get() * (deltaAngle >= 0 ? 1 : -1) * delta;
            if ((toRotate >= 0 && toRotate > deltaAngle) || (toRotate < 0 && toRotate < deltaAngle)) toRotate = deltaAngle;
            mc.player.setPitch(mc.player.getPitch() + (float) toRotate);
        }
    }

    @Override
    public String getInfoString() {
        return EntityUtils.getName(target);
    }
}
