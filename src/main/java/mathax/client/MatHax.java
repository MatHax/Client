package mathax.client;

import mathax.client.eventbus.EventBus;
import mathax.client.eventbus.EventHandler;
import mathax.client.eventbus.EventPriority;
import mathax.client.eventbus.IEventBus;
import mathax.client.events.game.OpenScreenEvent;
import mathax.client.events.mathax.KeyEvent;
import mathax.client.events.mathax.MouseButtonEvent;
import mathax.client.events.world.TickEvent;
import mathax.client.gui.GuiThemes;
import mathax.client.gui.WidgetScreen;
import mathax.client.gui.renderer.GuiRenderer;
import mathax.client.gui.tabs.Tabs;
import mathax.client.music.Music;
import mathax.client.renderer.GL;
import mathax.client.renderer.PostProcessRenderer;
import mathax.client.renderer.Renderer2D;
import mathax.client.renderer.Shaders;
import mathax.client.renderer.text.Fonts;
import mathax.client.systems.Systems;
import mathax.client.systems.config.Config;
import mathax.client.systems.hud.HUD;
import mathax.client.systems.modules.Categories;
import mathax.client.systems.modules.Modules;
import mathax.client.systems.modules.client.CapesModule;
import mathax.client.systems.modules.client.ClientSpoof;
import mathax.client.systems.modules.client.DiscordRPC;
import mathax.client.systems.modules.client.MiddleClickFriend;
import mathax.client.systems.modules.combat.*;
import mathax.client.utils.Utils;
import mathax.client.utils.Version;
import mathax.client.utils.misc.FakeClientPlayer;
import mathax.client.utils.misc.KeyBind;
import mathax.client.utils.misc.Names;
import mathax.client.utils.misc.WindowUtils;
import mathax.client.utils.misc.input.KeyAction;
import mathax.client.utils.misc.input.KeyBinds;
import mathax.client.utils.network.MatHaxExecutor;
import mathax.client.utils.player.DamageUtils;
import mathax.client.utils.player.EChestMemory;
import mathax.client.utils.player.Rotations;
import mathax.client.utils.render.EntityShaders;
import mathax.client.utils.render.color.Color;
import mathax.client.utils.render.color.RainbowColors;
import mathax.client.utils.world.BlockIterator;
import mathax.client.utils.world.BlockUtils;
import mathax.client.systems.modules.render.Background;
import mathax.client.systems.modules.render.Zoom;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.util.Formatting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;

/*/------------------------------------------------------------------/*/
/*/ THIS CLIENT IS A FORK OF METEOR CLIENT BY MINEGAME159 & SEASNAIL /*/
/*/ https://meteorclient.com                                         /*/
/*/ https://github.com/MeteorDevelopment/meteor-client               /*/
/*/------------------------------------------------------------------/*/
/*/ Music player used from Motor Tunez made by JFronny               /*/
/*/ https://github.com/JFronny/MotorTunez                            /*/
/*/------------------------------------------------------------------/*/

public class MatHax implements ClientModInitializer {
    public static MatHax INSTANCE;
    public static MinecraftClient mc;
    public static final IEventBus EVENT_BUS = new EventBus();

    public static final File GAME_FOLDER = new File(FabricLoader.getInstance().getGameDir().toString());
    public static final File FOLDER = new File(GAME_FOLDER, "MatHax");
    public static final File VERSION_FOLDER = new File(FOLDER + "/" + Version.getMinecraft());
    public static final File MUSIC_FOLDER = new File(FOLDER + "/Music");

    public final Color MATHAX_COLOR = new Color(230, 75, 100, 255);
    public final int MATHAX_COLOR_INT = Color.fromRGBA(230, 75, 100, 255);
    public final Color MATHAX_BACKGROUND_COLOR = new Color(30, 30, 45, 255);
    public final int MATHAX_BACKGROUND_COLOR_INT = Color.fromRGBA(30, 30, 45, 255);

    public static final Logger LOG = LogManager.getLogger();
    public static final String LOG_PREFIX = "[MatHax] ";

    public static final String URL = "https://mathaxclient.xyz/";
    public static final String API_URL = "https://api.mathaxclient.xyz/";

    public static List<String> getDeveloperUUIDs() {
        return Arrays.asList(

            // MATEJKO06
            "3e24ef27e66d45d2bf4b2c7ade68ff47",
            "7c73f84473c33a7d9978004ba0a6436e",

            //NobreHD
            "2905e61c51794d0d967c255a16287056",
            "4c3d2322a3df30fcb371837ba257ea37"

        );
    }

    public static List<String> getSplashes() {
        return Arrays.asList(

            // SPLASHES
            Formatting.RED + "MatHax on top!",
            Formatting.GRAY + "Matejko06" + Formatting.RED + " based god",
            Formatting.RED + "MatHaxClient.xyz",
            Formatting.RED + "MatHaxClient.xyz/Discord",
            Formatting.RED + Version.getStylized(),
            Formatting.RED + Version.getMinecraft(),

            // MEME SPLASHES
            Formatting.YELLOW + "cope",
            Formatting.YELLOW + "I am funny -HiIAmFunny",
            Formatting.YELLOW + "IntelliJ IDEa",
            Formatting.YELLOW + "I <3 nns",
            Formatting.YELLOW + "haha 69",
            Formatting.YELLOW + "420 XDDDDDD",
            Formatting.YELLOW + "ayy",
            Formatting.YELLOW + "too ez",
            Formatting.YELLOW + "owned",
            Formatting.YELLOW + "your mom :joy:",
            Formatting.YELLOW + "BOOM BOOM BOOM!",
            Formatting.YELLOW + "I <3 forks",
            Formatting.YELLOW + "based",
            Formatting.YELLOW + "Pog",
            Formatting.YELLOW + "Big Rat on top!",
            Formatting.YELLOW + "bigrat.monster",

            // PERSONALIZED
            Formatting.YELLOW + "You're cool, " + Formatting.GRAY + MinecraftClient.getInstance().getSession().getUsername(),
            Formatting.YELLOW + "Owning with " + Formatting.GRAY + MinecraftClient.getInstance().getSession().getUsername(),
            Formatting.YELLOW + "Who is " + Formatting.GRAY + MinecraftClient.getInstance().getSession().getUsername() + Formatting.YELLOW + "?"

        );
    }

    @Override
    public void onInitializeClient() {
        // Instance
        if (INSTANCE == null) {
            INSTANCE = this;
            return;
        }

        // Log
        LOG.info(LOG_PREFIX + "Initializing MatHax " + Version.getStylized() + " for Minecraft " + Version.getMinecraft() + "...");

        // Global Minecraft client accessor
        mc = MinecraftClient.getInstance();

        // Icon & Title
        WindowUtils.MatHax.setIcon();
        WindowUtils.MatHax.setTitleLoading();

        // Register event handlers
        EVENT_BUS.registerLambdaFactory("mathax.client", (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup()));

        // Pre-load
        Systems.addPreLoadTask(() -> {
            if (!FOLDER.exists()) {
                FOLDER.getParentFile().mkdirs();
                FOLDER.mkdir();

                // ACTIVATE
                Modules.get().get(CapesModule.class).forceToggle(true); // CAPES
                Modules.get().get(DiscordRPC.class).forceToggle(true); // DISCORD RPC
                Modules.get().get(Background.class).forceToggle(true); // BACKGROUND
                Modules.get().get(MiddleClickFriend.class).forceToggle(true); // MIDDLE CLICK FRIEND

                // VISIBILITY
                Modules.get().get(ClientSpoof.class).setVisible(false); // CLIENT SPOOF
                Modules.get().get(CapesModule.class).setVisible(false); // CAPES
                Modules.get().get(DiscordRPC.class).setVisible(false); // DISCORD RPC
                Modules.get().get(Background.class).setVisible(false); // BACKGROUND
                Modules.get().get(MiddleClickFriend.class).setVisible(false); // MIDDLE CLICK FRIEND
                Modules.get().get(Zoom.class).setVisible(false); // ZOOM

                // KEYBINDS
                Modules.get().get(Zoom.class).keybind.set(KeyBind.fromKey(GLFW.GLFW_KEY_C)); // ZOOM

                // KEYBIND OPTIONS
                Modules.get().get(Zoom.class).toggleOnBindRelease = true; // ZOOM

                // TOASTS
                Modules.get().get(AnchorAura.class).setToggleToast(true); // ANCHOR AURA
                Modules.get().get(BedAura.class).setToggleToast(true); // BED AURA
                Modules.get().get(CEVBreaker.class).setToggleToast(true); // CEV BREAKER
                Modules.get().get(CrystalAura.class).setToggleToast(true); // CRYSTAL AURA
                Modules.get().get(KillAura.class).setToggleToast(true); // KILL AURA

                // MESSAGES
                Modules.get().get(Zoom.class).setToggleMessage(false); // ZOOM
            }

            // RESET HUD LOCATIONS
            if (!Systems.get(HUD.class).getFile().exists()) Systems.get(HUD.class).reset.run(); // HUD
        });

        // Pre init
        Utils.init();
        Version.init();
        GL.init();
        Shaders.init();
        Renderer2D.init();
        EntityShaders.initOutlines();
        MatHaxExecutor.init();
        BlockIterator.init();
        EChestMemory.init();
        Rotations.init();
        Names.init();
        FakeClientPlayer.init();
        PostProcessRenderer.init();
        Tabs.init();
        GuiThemes.init();
        Fonts.init();
        DamageUtils.init();
        BlockUtils.init();
        Music.init();

        // Register module categories
        Categories.init();

        // Load systems
        Systems.init();

        // Event bus
        EVENT_BUS.subscribe(this);

        // Sorting modules
        Modules.get().sortModules();

        // Load saves
        Systems.load();

        // Post init
        Fonts.load();
        GuiRenderer.init();
        GuiThemes.postInit();
        RainbowColors.init();

        // Title
        WindowUtils.MatHax.setTitleLoaded();

        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            DiscordRPC.disable();
            Systems.save();
            GuiThemes.save();
        }));

        // Icon & Title
        ClientSpoof cs = Modules.get().get(ClientSpoof.class);
        if (cs.isActive() && cs.changeWindowIcon()) WindowUtils.Meteor.setIcon();
        else WindowUtils.MatHax.setIcon();
        if (cs.isActive() && cs.changeWindowTitle()) WindowUtils.Meteor.setTitle();
        else WindowUtils.MatHax.setTitle();

        // Log
        LOG.info(LOG_PREFIX + "MatHax " + Version.getStylized() + " initialized for Minecraft " + Version.getMinecraft() + "!");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.currentScreen == null && mc.getOverlay() == null && KeyBinds.OPEN_COMMANDS.wasPressed()) mc.setScreen(new ChatScreen(Config.get().prefix.get()));

        if (Music.player == null) return;
        if (Music.player.getVolume() != Config.get().musicVolume.get()) Music.player.setVolume(Config.get().musicVolume.get());
    }

    @EventHandler
    private void onKey(KeyEvent event) {
        if (mc.getOverlay() instanceof SplashOverlay) return;
        if (event.action == KeyAction.Press && KeyBinds.OPEN_CLICK_GUI.matchesKey(event.key, 0)) openClickGUI();
    }

    private void onMouseButton(MouseButtonEvent event) {
        if (mc.getOverlay() instanceof SplashOverlay) return;
        if (event.action == KeyAction.Press && KeyBinds.OPEN_CLICK_GUI.matchesMouse(event.button)) openClickGUI();
    }

    private void openClickGUI() {
        if (Utils.canOpenClickGUI()) Tabs.get().get(0).openScreen(GuiThemes.get());
    }

    private boolean wasWidgetScreen, wasHudHiddenRoot;

    @EventHandler(priority = EventPriority.LOWEST)
    private void onOpenScreen(OpenScreenEvent event) {
        boolean hideHud = GuiThemes.get().hideHUD();

        if (hideHud) {
            if (!wasWidgetScreen) wasHudHiddenRoot = mc.options.hudHidden;

            if (event.screen instanceof WidgetScreen) mc.options.hudHidden = true;
            else if (!wasHudHiddenRoot) mc.options.hudHidden = false;
        }

        wasWidgetScreen = event.screen instanceof WidgetScreen;
    }
}
