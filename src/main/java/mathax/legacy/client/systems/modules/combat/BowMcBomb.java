package mathax.legacy.client.systems.modules.combat;

import mathax.legacy.client.eventbus.EventHandler;
import mathax.legacy.client.events.packets.PacketEvent;
import mathax.legacy.client.settings.BoolSetting;
import mathax.legacy.client.settings.IntSetting;
import mathax.legacy.client.settings.Setting;
import mathax.legacy.client.settings.SettingGroup;
import mathax.legacy.client.systems.modules.Categories;
import mathax.legacy.client.systems.modules.Module;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;

public class BowMcBomb extends Module {

    public BowMcBomb(){
        super(Categories.Combat, Items.BOW, "BowMcBomb", "Does the bow thingy");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> Ttimeout = sgGeneral.add(new IntSetting.Builder()
        .name("Timeout")
        .description("Timeout ticks amount")
        .range(0, 10000)
        .sliderRange(0, 40)
        .defaultValue(20)
        .build()
    );

    private final Setting<Integer> spoofs = sgGeneral.add(new IntSetting.Builder()
        .name("Spoofs")
        .description("How many spoofs to do")
        .range(0, 10)
        .sliderRange(0, 10)
        .defaultValue(5)
        .build()
    );

    private final Setting<Boolean> hypickle = sgGeneral.add(new BoolSetting.Builder()
        .name("bye-pass")
        .description("Hi pickle moment")
        .defaultValue(false)
        .build()
    );

    private long /*ike matejko's cock*/ lastShoteingTiem;

    @Override
    public void onActivate(){
        lastShoteingTiem = System.currentTimeMillis();
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event){
        if (event.packet instanceof PlayerActionC2SPacket){
            PlayerActionC2SPacket packet = (PlayerActionC2SPacket) event.packet;

            if (packet.getAction() == PlayerActionC2SPacket.Action.RELEASE_USE_ITEM) {
                ItemStack stack = mc.player.getMainHandStack();
                if (!stack.isEmpty() && stack.getItem() != null && stack.getItem() instanceof BowItem) {
                    doSpoofs();
                }
            } else if (event.packet instanceof PlayerInteractItemC2SPacket) {
                PlayerInteractItemC2SPacket packet2 = (PlayerInteractItemC2SPacket) event.packet;

                if (packet2.getHand() == Hand.MAIN_HAND) {
                    ItemStack stack = mc.player.getMainHandStack();

                    if (!stack.isEmpty() && stack.getItem() != null && stack.getItem() == Items.BOW) {
                        doSpoofs();
                    }
                }
            }
        }
    }


    private void doSpoofs() {
        if (System.currentTimeMillis() - lastShoteingTiem >= Ttimeout.get()) {
            lastShoteingTiem = System.currentTimeMillis();

            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));

            for (int i = 0; i < spoofs.get(); ++i) {
                if (hypickle.get()) {
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1e-10, mc.player.getZ(), false));
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() - 1e-10, mc.player.getZ(), true));
                } else {
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() - 1e-10, mc.player.getZ(), true));
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1e-10, mc.player.getZ(), false));
                }
            }

            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));

        }
    }
}
