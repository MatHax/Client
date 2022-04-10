package mathax.client.systems.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import mathax.client.systems.System;
import mathax.client.systems.Systems;
import mathax.client.systems.commands.commands.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.command.CommandSource;

import java.util.*;

import static mathax.client.MatHax.mc;

public class Commands extends System<Commands> {
    private final CommandDispatcher<CommandSource> DISPATCHER = new CommandDispatcher<>();
    private final CommandSource COMMAND_SOURCE = new ChatCommandSource(mc);
    private final List<Command> commands = new ArrayList<>();
    private final Map<Class<? extends Command>, Command> commandInstances = new HashMap<>();

    public Commands() {
        super(null);
    }

    public static Commands get() {
        return Systems.get(Commands.class);
    }

    @Override
    public void init() {
        add(new BaritoneCommand());
        add(new BindsCommand());
        add(new BookDupeCommand());
        add(new VClipCommand());
        add(new HClipCommand());
        add(new HeadsCommand());
        add(new ClearChatCommand());
        add(new DismountCommand());
        add(new DamageCommand());
        add(new DropCommand());
        add(new EnchantCommand());
        add(new FakePlayerCommand());
        add(new FOVCommand());
        add(new FriendsCommand());
        add(new EnemiesCommand());
        add(new CommandsCommand());
        add(new InventoryCommand());
        add(new LocateCommand());
        add(new NbtCommand());
        add(new NotebotCommand());
        add(new PanicCommand());
        add(new PeekCommand());
        add(new PingCommand());
        add(new PluginsCommand());
        add(new PrefixCommand());
        add(new ProfilesCommand());
        add(new ReloadCommand());
        add(new ReloadSoundSystemCommand());
        add(new ResetCommand());
        add(new RotationCommand());
        add(new SayCommand());
        add(new ServerCommand());
        add(new SwarmCommand());
        add(new ToggleCommand());
        add(new TPSCommand());
        add(new SettingCommand());
        add(new SpectateCommand());
        add(new GamemodeCommand());
        add(new GhostCommand());
        add(new SaveCommand());
        add(new SaveMapCommand());
        add(new ModulesCommand());
        add(new MusicCommand());
        add(new GiveCommand());
        add(new NameHistoryCommand());
        add(new WaypointCommand());

        commands.sort(Comparator.comparing(Command::getName));
    }

    public void dispatch(String message) throws CommandSyntaxException {
        dispatch(message, new ChatCommandSource(mc));
    }

    public void dispatch(String message, CommandSource source) throws CommandSyntaxException {
        ParseResults<CommandSource> results = DISPATCHER.parse(message, source);
        DISPATCHER.execute(results);
    }

    public CommandDispatcher<CommandSource> getDispatcher() {
        return DISPATCHER;
    }

    public CommandSource getCommandSource() {
        return COMMAND_SOURCE;
    }

    private final static class ChatCommandSource extends ClientCommandSource {
        public ChatCommandSource(MinecraftClient client) {
            super(null, client);
        }
    }

    public void add(Command command) {
        commands.removeIf(command1 -> command1.getName().equals(command.getName()));
        commandInstances.values().removeIf(command1 -> command1.getName().equals(command.getName()));

        command.registerTo(DISPATCHER);
        commands.add(command);
        commandInstances.put(command.getClass(), command);
    }

    public int getCount() {
        return commands.size();
    }

    public List<Command> getAll() {
        return commands;
    }

    public <T extends Command> T get(Class<T> klass) {
        return (T) commandInstances.get(klass);
    }
}
