package mathax.client.systems.modules.combat;

import mathax.client.eventbus.EventHandler;
import mathax.client.events.packets.PacketEvent;
import mathax.client.events.world.TickEvent;
import mathax.client.settings.*;
import mathax.client.systems.modules.Categories;
import mathax.client.systems.modules.Module;
import mathax.client.systems.modules.Modules;
import mathax.client.systems.modules.movement.NoFall;
import mathax.client.systems.modules.player.AntiHunger;
import mathax.client.utils.Utils;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class Sniper extends Module {
    private long lastShootTime;
    private boolean spoofed;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

    private final Setting<Double> base = sgGeneral.add(new DoubleSetting.Builder()
        .name("hide-base")
        .description("Base for the exponent number for hiding rubberband.")
        .defaultValue(10)
        .range(1.001, 2147483647)
        .sliderRange(1.001, 2147483647)
        .build()
    );

    private final Setting<Double> exponent = sgGeneral.add(new DoubleSetting.Builder()
        .name("hide-exponent")
        .description("Exponent for the base number for hiding rubberband.")
        .defaultValue(5)
        .range(0.001, 2147483647)
        .sliderRange(0.001, 2147483647)
        .build()
    );

    private final Setting<Boolean> antiHungerPause = sgGeneral.add(new BoolSetting.Builder()
        .name("anti-hunger-pause")
        .description("Pauses Anti Hunger when you are spoofing")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> noFallPause = sgGeneral.add(new BoolSetting.Builder()
        .name("no-fall-pause")
        .description("Pauses No Fall when you are spoofing")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> bows = sgGeneral.add(new BoolSetting.Builder()
        .name("bows")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> tridents = sgGeneral.add(new BoolSetting.Builder()
        .name("tridents")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> timeout = sgGeneral.add(new IntSetting.Builder()
        .name("timeout")
        .defaultValue(5000)
        .range(100, 20000)
        .sliderRange(100, 20000)
        .build()
    );

    private final Setting<Integer> spoofs = sgGeneral.add(new IntSetting.Builder()
        .name("spoofs")
        .defaultValue(10)
        .range(1, 300)
        .sliderRange(1, 300)
        .build()
    );

    private final Setting<Boolean> sprint = sgGeneral.add(new BoolSetting.Builder()
        .name("sprint")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> bypass = sgGeneral.add(new BoolSetting.Builder()
        .name("bypass")
        .defaultValue(false)
        .build()
    );

    public Sniper() {
        super(Categories.Combat, Items.BOW, "sniper", "Exploits the one shot kill exploit.");
    }

    @Override
    public void onActivate() {
        lastShootTime = Utils.getCurrentTimeMillis();
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        AntiHunger antiHunger = Modules.get().get(AntiHunger.class);
        NoFall noFall = Modules.get().get(NoFall.class);

        if ((isBow() && bows.get()) || (isTrident() && tridents.get())) {
            if (mc.player.getItemUseTime() > 0 && mc.options.keyUse.isPressed()) {
                if (antiHungerPause.get() && antiHunger.isActive()) antiHunger.toggle();
                if (noFallPause.get() && noFall.isActive()) noFall.toggle();
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        AntiHunger antiHunger = Modules.get().get(AntiHunger.class);
        NoFall noFall = Modules.get().get(NoFall.class);

        if (spoofed && !mc.options.keyUse.isPressed()) {
            if (antiHungerPause.get() && !antiHunger.isActive()) antiHunger.toggle();
            if (noFallPause.get() && !noFall.isActive()) noFall.toggle();
        }
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (event.packet == null) return;

        if (event.packet instanceof PlayerActionC2SPacket) {
            PlayerActionC2SPacket packet = (PlayerActionC2SPacket) event.packet;

            if (packet.getAction() == PlayerActionC2SPacket.Action.RELEASE_USE_ITEM) {
                if ((isBow() && bows.get()) || (isTrident() && tridents.get())) {
                    spoofed = false;
                    doSpoofs();
                }
            }
        }
    }

    private void doSpoofs() {
        if (System.currentTimeMillis() - lastShootTime >= timeout.get()) {
            float value = (float) Math.pow(base.get(), -exponent.get());
            lastShootTime = Utils.getCurrentTimeMillis();

            if (sprint.get()) mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));

            for (int index = 0; index < spoofs.get(); ++index) {
                if (bypass.get()) {
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + value, mc.player.getZ(), false));
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() - value, mc.player.getZ(), true));
                } else {
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() - value, mc.player.getZ(), true));
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + value, mc.player.getZ(), false));
                }
            }

            spoofed = true;
        }
    }

    private boolean isBow() {
        assert mc.player != null;
        return mc.player.getMainHandStack().getItem() == Items.BOW || mc.player.getOffHandStack().getItem() == Items.BOW;
    }

    private boolean isTrident() {
        assert mc.player != null;
        return mc.player.getMainHandStack().getItem() == Items.TRIDENT || mc.player.getOffHandStack().getItem() == Items.TRIDENT;
    }
}
