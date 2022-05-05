package mathax.client.systems.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import joptsimple.internal.Strings;
import mathax.client.MatHax;
import mathax.client.eventbus.EventHandler;
import mathax.client.events.packets.PacketEvent;
import mathax.client.events.world.TickEvent;
import mathax.client.systems.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket;
import net.minecraft.network.packet.s2c.play.CommandSuggestionsS2CPacket;
import net.minecraft.util.Formatting;

import java.util.*;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class PluginsCommand extends Command {
    private static final List<String> ANTICHEAT_LIST = Arrays.asList("nocheatplus", "negativity", "warden", "horizon", "illegalstack", "coreprotect", "exploitsx", "vulcan", "abc", "spartan", "kauri", "anticheatreloaded", "witherac", "godseye", "matrix", "wraith");
    private List<String> plugins = new ArrayList<>();

    private static final String completionStarts = "/:abcdefghijklmnopqrstuvwxyz0123456789-";

    private int ticks = 0;

    public PluginsCommand() {
        super("plugins", "Prints server plugins.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            ticks = 0;
            plugins.clear();
            MatHax.EVENT_BUS.subscribe(this);
            info("Please wait around 5 seconds...");
            (new Thread(() -> {
                Random random = new Random();
                completionStarts.chars().forEach(i -> {
                    mc.player.networkHandler.sendPacket(new RequestCommandCompletionsC2SPacket(random.nextInt(200), Character.toString(i)));
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            })).start();
            return SINGLE_SUCCESS;
        });
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        ticks++;

        if (ticks >= 100) {
            Collections.sort(plugins);

            for (int i = 0; i < plugins.size(); i++) {
                plugins.set(i, formatName(plugins.get(i)));
            }

            if (!plugins.isEmpty()) info("Plugins (%d): %s ", plugins.size(), Strings.join(plugins.toArray(new String[0]), ", "));
            else error("No plugins found.");

            ticks = 0;
            plugins.clear();
            MatHax.EVENT_BUS.unsubscribe(this);
        }
    }

    @EventHandler
    private void onReadPacket(PacketEvent.Receive event) {
        try {
            if (event.packet instanceof CommandSuggestionsS2CPacket) {
                CommandSuggestionsS2CPacket packet = (CommandSuggestionsS2CPacket) event.packet;
                Suggestions matches = packet.getSuggestions();

                if (matches == null) {
                    error("Invalid Packet.");
                    return;
                }

                for (Suggestion suggestion : matches.getList()) {
                    String[] command = suggestion.getText().split(":");
                    if (command.length > 1) {
                        String pluginName = command[0].replace("/", "");

                        if (!plugins.contains(pluginName)) plugins.add(pluginName);
                    }
                }
            }
        } catch (Exception e) {
            error("An error occurred while trying to find plugins!");
        }
    }

    private String formatName(String name) {
        if (ANTICHEAT_LIST.contains(name)) return String.format("%s%s(default)", Formatting.RED, name);
        else if (name.contains("exploit") || name.contains("cheat") || name.contains("illegal")) return String.format("%s%s(default)", Formatting.RED, name);

        return String.format("(highlight)%s(default)", name);
    }
}
