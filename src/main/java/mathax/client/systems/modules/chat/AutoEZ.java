package mathax.client.systems.modules.chat;

import mathax.client.eventbus.EventHandler;
import mathax.client.events.packets.PacketEvent;
import mathax.client.events.world.TickEvent;
import mathax.client.settings.*;
import mathax.client.systems.modules.combat.*;
import mathax.client.systems.friends.Friends;
import mathax.client.systems.modules.Categories;
import mathax.client.systems.modules.Module;
import mathax.client.systems.modules.Modules;
import mathax.client.utils.misc.Placeholders;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.*;

import java.util.*;

public class AutoEZ extends Module {
    private final Random random = new Random();

    private boolean canSendPop;

    private int ticks;

    private final SettingGroup sgKills = settings.createGroup("Kills");
    private final SettingGroup sgTotemPops = settings.createGroup("Totem Pops");

    // Kills

    private final Setting<Boolean> kills = sgKills.add(new BoolSetting.Builder()
        .name("enabled")
        .description("Enables the kill messages.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Mode> killMode = sgKills.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("Determines what messages to use.")
        .defaultValue(Mode.MatHax)
        .build()
    );

    private final Setting<MessageStyle> killMessageStyle = sgKills.add(new EnumSetting.Builder<MessageStyle>()
        .name("style")
        .description("Determines what message style to use.")
        .defaultValue(MessageStyle.EZ)
        .visible(() -> killMode.get() == Mode.MatHax)
        .build()
    );

    private final Setting<Boolean> killIgnoreFriends = sgKills.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .description("Ignores friends.")
        .defaultValue(true)
        .build()
    );

    private final Setting<List<String>> killMessages = sgKills.add(new StringListSetting.Builder()
        .name("messages")
        .description("Custom messages when you kill someone.")
        .defaultValue(Arrays.asList(
            "haha %player% is a noob! EZZz",
            "I just raped %player%!",
            "I just ended %player%!",
            "I just EZZz'd %player%!",
            "I just fucked %player%!",
            "Take the L nerd %player%! You just got ended!",
            "I just nae nae'd %player%!",
            "I am too good for %player%!"
        ))
        .visible(() -> killMode.get() == Mode.Custom)
        .build()
    );

    // Totem Pops

    private final Setting<Boolean> totems = sgTotemPops.add(new BoolSetting.Builder()
        .name("enabled")
        .description("Enables the totem pop messages.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> delay = sgTotemPops.add(new IntSetting.Builder()
        .name("delay")
        .description("Determines how often to send totem pop messages.")
        .defaultValue(600)
        .min(0)
        .sliderRange(0, 600)
        .build()
    );

    private final Setting<Mode> totemMode = sgTotemPops.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("Determines what messages to use.")
        .defaultValue(Mode.MatHax)
        .build()
    );

    private final Setting<Boolean> totemIgnoreFriends = sgTotemPops.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .description("Ignores friends.")
        .defaultValue(true)
        .build()
    );

    private final Setting<List<String>> totemMessages = sgTotemPops.add(new StringListSetting.Builder()
        .name("messages")
        .description("Custom messages when you pop someone.")
        .defaultValue(Arrays.asList(
            "%player% just lost 1 totem thanks to my skill!",
            "I just ended %player%'s totem!",
            "I just popped %player%!",
            "I just easily popped %player%!"
        ))
        .visible(() -> totemMode.get() == Mode.Custom)
        .build()
    );

    public AutoEZ() {
        super(Categories.Chat, Items.LIGHTNING_ROD, "auto-ez", "Announces EASY or GG when you kill or pop someone.");
    }

    @Override
    public void onActivate() {
        canSendPop = true;
        ticks = 0;
    }

    @EventHandler
    public void onPacketReadMessage(PacketEvent.Receive event) {
        if (!kills.get() || !(event.packet instanceof GameMessageS2CPacket) || mc.player == null || mc.world == null) return;
        if (killMode.get() == Mode.Custom && killMessages.get().isEmpty()) return;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || mc.player.isCreative() || mc.player.isSpectator() || player.getGameProfile().getName().equals(mc.player.getGameProfile().getName())) return;
            if (killIgnoreFriends.get() && Friends.get().isFriend(player)) return;

            String msg = Placeholders.apply(getMessageStyle()).replace("%player%", player.getName().getString());
            String message = ((GameMessageS2CPacket) event.packet).getMessage().getString();
            if (message.contains(player.getName().getString())) {
                if (message.contains("by " + mc.getSession().getUsername()) || message.contains("whilst fighting " + mc.getSession().getUsername()) || message.contains(mc.getSession().getUsername() + " sniped") || message.contains(mc.getSession().getUsername() + " annaly fucked") || message.contains(mc.getSession().getUsername() + " destroyed") || message.contains(mc.getSession().getUsername() + " killed") || message.contains(mc.getSession().getUsername() + " fucked") || message.contains(mc.getSession().getUsername() + " separated") || message.contains(mc.getSession().getUsername() + " punched") || message.contains(mc.getSession().getUsername() + " shoved")) {
                    if (message.contains("end crystal") || message.contains("end-crystal")) {
                        if (Modules.get().isActive(CrystalAura.class) && mc.player.distanceTo(player) < Modules.get().get(CrystalAura.class).targetRange.get()) mc.player.sendChatMessage(msg);
                        else if (Modules.get().isActive(PistonAura.class) && mc.player.distanceTo(player) < Modules.get().get(PistonAura.class).targetRange.get()) mc.player.sendChatMessage(msg);
                        else if (Modules.get().isActive(CEVBreaker.class) && mc.player.distanceTo(player) < Modules.get().get(CEVBreaker.class).targetRange.get()) mc.player.sendChatMessage(msg);
                        else if (mc.player.distanceTo(player) < 7) mc.player.sendChatMessage(msg);
                    } else {
                        if (Modules.get().isActive(KillAura.class) && mc.player.distanceTo(player) < Modules.get().get(KillAura.class).targetRange.get()) mc.player.sendChatMessage(msg);
                        else if (mc.player.distanceTo(player) < 8) mc.player.sendChatMessage(msg);
                    }
                } else {
                    if ((message.contains("bed") || message.contains("[Intentional Game Design]")) && Modules.get().isActive(BedAura.class) && mc.player.distanceTo(player) < Modules.get().get(BedAura.class).targetRange.get()) mc.player.sendChatMessage(msg);
                    else if ((message.contains("anchor") || message.contains("[Intentional Game Design]")) && Modules.get().isActive(AnchorAura.class) && mc.player.distanceTo(player) < Modules.get().get(AnchorAura.class).targetRange.get()) mc.player.sendChatMessage(msg);
                }
            }
        }
    }

    public String getMessageStyle() {
        return switch (killMode.get()) {
            case MatHax -> switch (killMessageStyle.get()) {
                case EZ -> getMessages().get(random.nextInt(getMessages().size()));
                case GG -> getGGMessages().get(random.nextInt(getGGMessages().size()));
            };
            case Custom -> killMessages.get().get(random.nextInt(killMessages.get().size()));
        };
    }

    private static List<String> getMessages() {
        return Arrays.asList(
            "%player% just got raped by MatHax!",
            "%player% just got ended by MatHax!",
            "haha %player% is a noob! MatHax on top!",
            "I just EZZz'd %player% using MatHax!",
            "I just fucked %player% using MatHax!",
            "Take the L nerd %player%! You just got ended by MatHax!",
            "I just nae nae'd %player% using MatHax!",
            "I am too good for %player%! MatHax on top!"
        );
    }

    private static List<String> getGGMessages() {
        return Arrays.asList(
            "GG %player%! MatHax is so op!",
            "Nice fight but MatHax is better, %player%! I really enjoyed it!",
            "Close fight %player%, but MatHax won!",
            "Good fight, %player%! MatHax on top!"
        );
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!totems.get() || !(event.packet instanceof EntityStatusS2CPacket packet) || mc.player == null || mc.world == null) return;
        if (totemMode.get() == Mode.Custom && totemMessages.get().isEmpty()) return;

        if (packet.getStatus() != 35 || !(packet.getEntity(mc.world) instanceof PlayerEntity player)) return;
        if (player == mc.player || mc.player.isCreative() || mc.player.isSpectator() || player.getGameProfile().getName().equals(mc.player.getGameProfile().getName())) return;
        if (totemIgnoreFriends.get() && Friends.get().isFriend(player)) return;
        if (mc.player.distanceTo(player) > 8) return;

        if (canSendPop) {
            mc.player.sendChatMessage(Placeholders.apply(getTotemMessageStyle()).replace("%player%", player.getName().getString()));
            canSendPop = false;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (ticks >= delay.get()) {
            canSendPop = true;
            ticks = 0;
        }

        if (!canSendPop) ticks++;
    }

    public String getTotemMessageStyle() {
        return switch (totemMode.get()) {
            case MatHax -> getTotemMessages().get(random.nextInt(getTotemMessages().size()));
            case Custom -> totemMessages.get().get(random.nextInt(totemMessages.get().size()));
        };
    }

    private static List<String> getTotemMessages() {
        return Arrays.asList(
            "%player% just got popped by MatHax!",
            "Keep popping %player%! MatHax owns you!",
            "%player%'s totem just got ended by MatHax!",
            "%player% just lost 1 totem thanks to MatHax!",
            "I just easily popped %player% using MatHax!"
        );
    }

    public enum Mode {
        MatHax("MatHax"),
        Custom("Custom");

        private final String title;

        Mode(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    public enum MessageStyle {
        EZ("EZ"),
        GG("GG");

        private final String title;

        MessageStyle(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }
}
