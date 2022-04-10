package mathax.client.systems.modules.misc;

import mathax.client.eventbus.EventHandler;
import mathax.client.events.packets.PacketEvent;
import mathax.client.events.world.TickEvent;
import mathax.client.settings.BoolSetting;
import mathax.client.settings.IntSetting;
import mathax.client.settings.Setting;
import mathax.client.settings.SettingGroup;
import mathax.client.systems.modules.Categories;
import mathax.client.systems.modules.Module;
import mathax.client.systems.modules.Modules;
import mathax.client.systems.modules.world.MountBypass;
import mathax.client.utils.player.InvUtils;
import mathax.client.utils.player.Rotations;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.screen.ingame.HorseScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.AbstractDonkeyEntity;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class AutoMountBypassDupe extends Module {
    private final List<Integer> slotsToMove = new ArrayList<>();
    private final List<Integer> slotsToThrow = new ArrayList<>();

    private AbstractDonkeyEntity entity;

    private boolean noCancel = false;
    private boolean sneak = false;

    private int timer;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

    private final Setting<Boolean> shulkersOnly = sgGeneral.add(new BoolSetting.Builder()
        .name("shulker-only")
        .description("Only moves shulker boxes into the inventory.")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> faceDown = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate-down")
        .description("Faces down when dropping items.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("The delay in ticks between actions.")
        .defaultValue(4)
        .min(0)
        .build()
    );

    public AutoMountBypassDupe() {
        super(Categories.Misc, Items.COMMAND_BLOCK, "auto-mount-bypass-dupe", "Automatically does the mount bypass dupe.");
    }

    @Override
    public void onActivate() {
        timer = 0;
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (noCancel) return;

        Modules.get().get(MountBypass.class).onSendPacket(event);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS) {
            toggle();
            mc.player.closeHandledScreen();
            return;
        }

        if (timer <= 0) timer = delay.get();
        else {
            timer--;
            return;
        }

        int slots = getInvSize(mc.player.getVehicle());

        for (Entity e : mc.world.getEntities()) {
            if (e.distanceTo(mc.player) < 5 && e instanceof AbstractDonkeyEntity && ((AbstractDonkeyEntity) e).isTame()) entity = (AbstractDonkeyEntity) e;
        }

        if (entity == null) return;

        if (sneak) {
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
            mc.player.setSneaking(false);
            sneak = false;
            return;
        }

        if (slots == -1) {
            if (entity.hasChest() || mc.player.getMainHandStack().getItem() == Items.CHEST) mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.interact(entity, mc.player.isSneaking(), Hand.MAIN_HAND));
            else {
                int slot = InvUtils.findInHotbar(Items.CHEST).slot();

                if (!InvUtils.swap(slot, true)) {
                    error("Cannot find chest in your hotbar, disabling...");
                    toggle();
                }
            }
        } else if (slots == 0) {
            if (isDupeTime()) {
                if (!slotsToThrow.isEmpty()) {
                    if (faceDown.get()) Rotations.rotate(mc.player.getYaw(), 90, 99, this::drop);
                    else drop();
                } else {
                    for (int i = 2; i < getDupeSize() + 1; i++) {
                        slotsToThrow.add(i);
                    }
                }
            } else {
                mc.player.closeHandledScreen();
                mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));

                mc.player.setSneaking(true);
                sneak = true;
            }
        } else if (!(mc.currentScreen instanceof HorseScreen)) mc.player.openRidingInventory();
        else if (slots > 0) {
            if (slotsToMove.isEmpty()) {
                boolean empty = true;
                for (int i = 2; i <= slots; i++) {
                    if (!(mc.player.currentScreenHandler.getStacks().get(i).isEmpty())) {
                        empty = false;
                        break;
                    }
                }

                if (empty) {
                    for (int i = slots + 2; i < mc.player.currentScreenHandler.getStacks().size(); i++) {
                        if (!(mc.player.currentScreenHandler.getStacks().get(i).isEmpty())) {
                            if (mc.player.currentScreenHandler.getSlot(i).getStack().getItem() == Items.CHEST) continue;
                            if (!(mc.player.currentScreenHandler.getSlot(i).getStack().getItem() instanceof BlockItem && ((BlockItem) mc.player.currentScreenHandler.getSlot(i).getStack().getItem()).getBlock() instanceof ShulkerBoxBlock) && shulkersOnly.get()) continue;
                            slotsToMove.add(i);

                            if (slotsToMove.size() >= slots) break;
                        }
                    }
                } else {
                    noCancel = true;
                    mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.interact(entity, mc.player.isSneaking(), Hand.MAIN_HAND));
                    noCancel = false;
                    return;
                }
            }

            if (!slotsToMove.isEmpty()) {
                for (int i : slotsToMove) {
                    InvUtils.quickMove().from(i).to(0);
                }

                slotsToMove.clear();
            }
        }
    }

    private int getInvSize(Entity e) {
        if (!(e instanceof AbstractDonkeyEntity)) return -1;

        if (!((AbstractDonkeyEntity) e).hasChest()) return 0;

        if (e instanceof LlamaEntity) return 3 * ((LlamaEntity) e).getStrength();

        return 15;
    }

    private boolean isDupeTime() {
        if (mc.player.getVehicle() != entity || entity.hasChest() || mc.player.currentScreenHandler.getStacks().size() == 46) return false;

        if (mc.player.currentScreenHandler.getStacks().size() > 38) {
            for (int i = 2; i < getDupeSize() + 1; i++) {
                if (mc.player.currentScreenHandler.getSlot(i).hasStack()) return true;
            }
        }

        return false;
    }

    private void drop() {
        for (int i : slotsToThrow) {
            InvUtils.drop().slot(i);
        }

        slotsToThrow.clear();
    }

    private int getDupeSize() {
        if (mc.player.getVehicle() != entity || entity.hasChest() || mc.player.currentScreenHandler.getStacks().size() == 46) return 0;

        return mc.player.currentScreenHandler.getStacks().size() - 38;
    }
}
