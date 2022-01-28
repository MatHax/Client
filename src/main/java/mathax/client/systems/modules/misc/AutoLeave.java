/*
 * Decompiled with CFR 0.151.
 */
package minegame159.meteorclient.systems.modules.bedtrap;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.game.GameLeftEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.settings.StringSetting;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.bedtrap.InvUtils;
import minegame159.meteorclient.utils.player.ChatUtils;
import net.minecraft.class_1802;
import net.minecraft.class_2561;
import net.minecraft.class_2585;
import net.minecraft.class_2661;

public class AutoLeave
extends Module {
    private final SettingGroup sgChat;
    private final Setting<Integer> totems;
    private final Setting<Boolean> disable;
    static final boolean $assertionsDisabled = !AutoLeave.class.desiredAssertionStatus();
    public int leavedelay;
    public int delay;
    String msg;
    private final Setting<String> leaveMessage;
    private final Setting<Mode> mode;
    String command;
    private final SettingGroup sgGeneral;
    private final Setting<String> leaveCommand;

    @EventHandler
    private void onGameLeft(GameLeftEvent gameLeftEvent) {
        if (!this.disable.get().booleanValue()) {
            return;
        }
        this.toggle();
    }

    @EventHandler
    private void onTick(TickEvent.Post post) {
        InvUtils.FindItemResult findItemResult = InvUtils.findItemWithCount(class_1802.field_8288);
        this.msg = this.leaveMessage.get();
        this.command = this.leaveCommand.get();
        if (!$assertionsDisabled && this.mc.field_1724 == null) {
            throw new AssertionError();
        }
        if (findItemResult.count <= this.totems.get()) {
            switch (1.$SwitchMap$minegame159$meteorclient$systems$modules$bedtrap$AutoLeave$Mode[this.mode.get().ordinal()]) {
                case 1: {
                    this.mc.field_1724.method_3142(this.msg);
                    if (this.delay < this.leavedelay) {
                        ++this.delay;
                        return;
                    }
                    this.mc.field_1724.field_3944.method_11083(new class_2661((class_2561)new class_2585(String.valueOf(new StringBuilder().append("[AutoLeave] Your totems <=  ").append(this.totems.get()).append(".")))));
                    break;
                }
                case 2: {
                    ChatUtils.info(this.msg, new Object[0]);
                    if (this.delay < this.leavedelay) {
                        ++this.delay;
                        return;
                    }
                    this.mc.field_1724.field_3944.method_11083(new class_2661((class_2561)new class_2585(String.valueOf(new StringBuilder().append("[AutoLeave] Your totems <=  ").append(this.totems.get()).append(".")))));
                    break;
                }
                case 3: {
                    this.mc.field_1724.method_3142(this.command);
                    this.toggle();
                    break;
                }
                case 4: {
                    this.mc.field_1724.field_3944.method_11083(new class_2661((class_2561)new class_2585(String.valueOf(new StringBuilder().append("[AutoLeave] Your totems <=  ").append(this.totems.get()).append(".")))));
                }
            }
        }
        this.delay = 0;
    }

    public AutoLeave() {
        super(Categories.BedTrap, "auto-leave", "Automatically disconnects you when certain requirements are met.");
        this.sgGeneral = this.settings.getDefaultGroup();
        this.sgChat = this.settings.createGroup("Chat");
        this.mode = this.sgGeneral.add(new EnumSetting.Builder().name("mode").description("Sends to the chat message.").defaultValue(Mode.NoneAndLeave).build());
        this.totems = this.sgGeneral.add(new IntSetting.Builder().name("totems").description("How many totems need to disconnect.").defaultValue(2).min(1).sliderMax(36).build());
        this.disable = this.sgGeneral.add(new BoolSetting.Builder().name("auto-disable").description("prevent from pushing out of block.").defaultValue(true).build());
        this.leaveMessage = this.sgChat.add(new StringSetting.Builder().name("message").description("Chat alert to send when you leave from the server.").defaultValue("LoLFunnyLoL mode activated.").build());
        this.leaveCommand = this.sgChat.add(new StringSetting.Builder().name("command").description("Very useful for SLABIY players.").defaultValue("/home").build());
        this.delay = 0;
        this.leavedelay = 1;
    }

    public static enum Mode {
        MessageAndLeave,
        ClientAndLeave,
        CommandAndLeave,
        NoneAndLeave;

    }
}

