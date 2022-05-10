package mathax.client.systems;

import mathax.client.MatHax;
import mathax.client.eventbus.EventHandler;
import mathax.client.events.game.GameLeftEvent;
import mathax.client.systems.accounts.Accounts;
import mathax.client.systems.commands.Commands;
import mathax.client.systems.config.Config;
import mathax.client.systems.enemies.Enemies;
import mathax.client.systems.friends.Friends;
import mathax.client.systems.macros.Macros;
import mathax.client.systems.modules.Modules;
import mathax.client.systems.hud.HUD;
import mathax.client.systems.profiles.Profiles;
import mathax.client.systems.proxies.Proxies;
import mathax.client.systems.waypoints.Waypoints;
import mathax.client.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Systems {
    private static final Map<Class<? extends System>, System<?>> systems = new HashMap<>();
    private static final List<Runnable> preLoadTasks = new ArrayList<>(1);

    public static void addPreLoadTask(Runnable task) {
        preLoadTasks.add(task);
    }

    public static void init() {
        System<?> config = add(new Config());
        config.init();
        config.load();

        add(new Modules());
        add(new Commands());
        add(new Friends());
        add(new Enemies());
        add(new Macros());
        add(new Accounts());
        add(new Waypoints());
        add(new Profiles());
        add(new Proxies());
        add(new HUD());

        MatHax.EVENT_BUS.subscribe(Systems.class);
    }

    private static System<?> add(System<?> system) {
        systems.put(system.getClass(), system);
        MatHax.EVENT_BUS.subscribe(system);
        system.init();

        return system;
    }

    // Game leave

    @EventHandler
    private static void onGameLeft(GameLeftEvent event) {
        save();
    }

    // Save

    public static void save(File folder) {
        long start = Utils.getCurrentTimeMillis();
        MatHax.LOG.info("Systems are saving...");

        for (System<?> system : systems.values()) system.save(folder);

        MatHax.LOG.info("Systems saved in {} milliseconds.", Utils.getCurrentTimeMillis() - start);
    }

    public static void save() {
        save(null);
    }

    // Load

    public static void load(File folder) {
        long start = Utils.getCurrentTimeMillis();
        MatHax.LOG.info("Systems are loading...");

        for (Runnable task : preLoadTasks) task.run();

        for (System<?> system : systems.values()) system.load(folder);

        MatHax.LOG.info("Systems loaded in {} milliseconds.", Utils.getCurrentTimeMillis() - start);
    }

    public static void load() {
        load(null);
    }

    public static <T extends System<?>> T get(Class<T> klass) {
        return (T) systems.get(klass);
    }
}
