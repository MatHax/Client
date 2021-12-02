package mathax.legacy.client.systems.modules.combat;

import mathax.legacy.client.eventbus.EventHandler;
import mathax.legacy.client.events.packets.PacketEvent;
import mathax.legacy.client.events.world.TickEvent;
import mathax.legacy.client.settings.BoolSetting;
import mathax.legacy.client.settings.EnumSetting;
import mathax.legacy.client.settings.Setting;
import mathax.legacy.client.settings.SettingGroup;
import mathax.legacy.client.systems.modules.Categories;
import mathax.legacy.client.systems.modules.Module;
import mathax.legacy.client.systems.modules.Modules;
import mathax.legacy.client.systems.modules.render.WallHack;
import mathax.legacy.client.utils.player.FindItemResult;
import mathax.legacy.client.utils.player.InvUtils;
import mathax.legacy.client.utils.world.BlockUtils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.util.math.BlockPos;

public class SelfTrapPlus extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

    private final Setting<Mode> placement = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("Determines where to place blocks.")
        .defaultValue(Mode.Top)
        .build()
    );

    private final Setting<Boolean> selfToggle = sgGeneral.add(new BoolSetting.Builder()
        .name("self-toggle")
        .description("Turns off after placing all blocks.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> onlyHole = sgGeneral.add(new BoolSetting.Builder()
        .name("only-hole")
        .description("Toggles when not in a hole.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> tpToggle = sgGeneral.add(new BoolSetting.Builder()
        .name("tp-toggle")
        .description("Toggles after teleporting.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Sends rotation packets to the server when placing.")
        .defaultValue(false)
        .build()
    );

    public SelfTrapPlus(){
        super(Categories.Combat, Items.OBSIDIAN, "self-trap+", "Places obsidian above your head.");
    }

    @Override
    public void onActivate() {
        if (Modules.get().isActive(WallHack.class)) {
            error("(highlight)Self Trap(default) was enabled while enabling (highlight)Self Trap+(default), disabling (highlight)Self Trap(default)...");
            Modules.get().get(SelfTrap.class).toggle();
        }
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof TeleportConfirmC2SPacket && tpToggle.get()) toggle();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        FindItemResult obsidian = InvUtils.findInHotbar(Items.OBSIDIAN);

        if (!obsidian.found()) {
            error("No obsidian in hotbar, disabling...");
            toggle();
            return;
        }

        if (onlyHole.get() && !check(mc.player)) return;

        BlockPos pos = mc.player.getBlockPos();
        switch (placement.get()) {
            case Full -> {
                place(pos.add(0, 2, 0));
                place(pos.add(1, 1, 0));
                place(pos.add(-1, 1, 0));
                place(pos.add(0, 1, 1));
                place(pos.add(0, 1, -1));
            }
            case Full_Plus -> {
                place(pos.add(0, 2, 0));
                place(pos.add(0, 3, 0));
                place(pos.add(1, 1, 0));
                place(pos.add(-1, 1, 0));
                place(pos.add(0, 1, 1));
                place(pos.add(0, 1, -1));
            }
            case Full_Plus_Plus -> {
                place(pos.add(0, 3, 0));
                place(pos.add(1, 2, 0));
                place(pos.add(-1, 2, 0));
                place(pos.add(0, 2, 1));
                place(pos.add(0, 2, -1));
                place(pos.add(1, 1, 0));
                place(pos.add(-1, 1, 0));
                place(pos.add(0, 1, 1));
                place(pos.add(0, 1, -1));
            }
            case Top -> place(pos.add(0, 2, 0));
        }

        if (selfToggle.get()) toggle();
    }

    private boolean check(LivingEntity target) {
        assert mc.world != null;
        return !mc.world.getBlockState(target.getBlockPos().add(1, 0, 0)).isAir() && !mc.world.getBlockState(target.getBlockPos().add(-1, 0, 0)).isAir() && !mc.world.getBlockState(target.getBlockPos().add(0, 0, 1)).isAir() && !mc.world.getBlockState(target.getBlockPos().add(0, 0, -1)).isAir();
    }

    private void place(BlockPos pos){
        FindItemResult obsidian = InvUtils.findInHotbar(Items.OBSIDIAN);
        BlockUtils.place(pos, obsidian, rotate.get(), 50);
    }

    public enum Mode {
        Full,
        Full_Plus,
        Full_Plus_Plus,
        Top;

        @Override
        public String toString() {
            if (this == Full_Plus) return "Full+";
            else if (this == Full_Plus_Plus) return "Full++";
            return super.toString().replace("_", " ");
        }
    }
}