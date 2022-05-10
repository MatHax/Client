package mathax.client.systems.modules.client;

import mathax.client.MatHax;
import mathax.client.gui.screens.hud.HudElementScreen;
import mathax.client.gui.screens.proxies.*;
import mathax.client.gui.screens.accounts.*;
import mathax.client.gui.screens.clickgui.*;
import mathax.client.gui.screens.music.*;
import mathax.client.gui.screens.servermanager.*;
import mathax.client.gui.screens.settings.*;
import mathax.client.gui.tabs.builtin.*;
import mathax.client.mixin.MinecraftServerAccessor;
import mathax.client.settings.BoolSetting;
import mathax.client.settings.EnumSetting;
import mathax.client.settings.Setting;
import mathax.client.settings.SettingGroup;
import mathax.client.systems.modules.Categories;
import mathax.client.systems.modules.Module;
import mathax.client.systems.modules.Modules;
import mathax.client.systems.modules.render.search.SBlockDataScreen;
import mathax.client.utils.Utils;
import mathax.client.utils.Version;
import mathax.client.utils.misc.LastServerInfo;
import mathax.client.utils.render.PeekScreen;
import mathax.client.utils.render.prompts.OkPrompt;
import mathax.client.utils.render.prompts.YesNoPrompt;
import mathax.client.systems.modules.misc.NameProtect;
import net.arikia.dev.drpc.DiscordEventHandlers;
import net.arikia.dev.drpc.DiscordRichPresence;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.*;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.client.gui.screen.world.*;
import net.minecraft.client.realms.gui.screen.RealmsScreen;
import net.minecraft.item.Items;

import java.io.File;

public class DiscordRPC extends Module {
    private static final net.arikia.dev.drpc.DiscordRPC discord = new net.arikia.dev.drpc.DiscordRPC();
    private static final DiscordEventHandlers handlers = new DiscordEventHandlers();
    private static final DiscordRichPresence rpc = new DiscordRichPresence();

    private static final String APP_ID = "878967665501306920";
    private static final String STEAM_ID = "";

    private static int number = 1;
    private static int delay = 0;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

    public final Setting<Boolean> playerHealth = sgGeneral.add(new BoolSetting.Builder()
        .name("health")
        .description("Determines if your health will be visible on the rpc.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> serverVisibility = sgGeneral.add(new BoolSetting.Builder()
        .name("server")
        .description("Determines if the server ip will be visible on the rpc.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> worldVisibility = sgGeneral.add(new BoolSetting.Builder()
        .name("world")
        .description("Determines if the singleplayer world name will be visible on the rpc.")
        .defaultValue(true)
        .visible(serverVisibility::get)
        .build()
    );

    public final Setting<SmallImageMode> smallImageMode = sgGeneral.add(new EnumSetting.Builder<SmallImageMode>()
        .name("small-images")
        .description("Shows cats or dogs on MatHax rpc.")
        .defaultValue(SmallImageMode.Cats)
        .build()
    );

    public DiscordRPC() {
        super(Categories.Client, Items.COMMAND_BLOCK, "discord-rpc", "Shows MatHax as your Discord status.", true);
    }

    @Override
    public void onActivate() {
        MatHax.LOG.info(MatHax.LOG_PREFIX + "Enabling Discord Rich Presence...");

        discord.discordInitialize(APP_ID, handlers, true, STEAM_ID);
        rpc.startTimestamp = Utils.getCurrentTimeMillis() / 1000;
        rpc.details = Version.getStylized() + " | " + getUsername() + getHealth();
        rpc.state = getActivity();
        rpc.largeImageKey = "logo";
        rpc.largeImageText = "MatHax " + Version.getStylized() + " - " + mc.getVersionType() + " " + Version.getMinecraft();
        applySmallImage();
        rpc.smallImageText = getActivity();
        rpc.partyId = "ae488379-351d-4a4f-ad32-2b9b01c91657";
        rpc.joinSecret = "MTI4NzM0OjFpMmhuZToxMjMxMjM=";
        rpc.partySize = mc.getNetworkHandler() != null ? mc.getNetworkHandler().getPlayerList().size() : 1;
        rpc.partyMax = 1;
        discord.discordUpdatePresence(rpc);

        MatHax.LOG.info(MatHax.LOG_PREFIX + "Discord Rich Presence enabled!");

        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                discord.discordRunCallbacks();
                try {
                    rpc.details = Version.getStylized() + " | " + getUsername() + getHealth();
                    rpc.state = getActivity();
                    rpc.largeImageKey = "logo";
                    rpc.largeImageText = "MatHax " + Version.getStylized() + " - " + mc.getVersionType() + " " + Version.getMinecraft();
                    applySmallImage();
                    rpc.smallImageText = getActivity();
                    rpc.partySize = mc.getNetworkHandler() != null ? mc.getNetworkHandler().getPlayerList().size() : 1;
                    rpc.partyMax = 1;
                    discord.discordUpdatePresence(rpc);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }, "RPC-Callback-Handler").start();
    }

    @Override
    public void onDeactivate() {
        disable();
    }

    public static void disable() {
        MatHax.LOG.info(MatHax.LOG_PREFIX + "Disabling Discord Rich Presence...");
        discord.discordClearPresence();
        discord.discordShutdown();
        MatHax.LOG.info(MatHax.LOG_PREFIX + "Discord Rich Presence disabled!");
    }

    // TODO: Rewrite
    private String getActivity() {
        if (mc.getOverlay() instanceof SplashOverlay || mc.currentScreen == null) {
            if (mc.world != null && serverVisibility.get()) return "Loading something (" + getWorldActivity(true, true) + ")";
            else return "Loading something...";
        } else if (mc.currentScreen instanceof ProgressScreen) {
            if (mc.world != null && serverVisibility.get()) return "Loading something (" + getWorldActivity(true, true) + ")";
            else return "Loading something...";
        } else if (mc.currentScreen instanceof TitleScreen) return "In main menu";
        else if (mc.currentScreen instanceof MultiplayerScreen) return "In server selection";
        else if (mc.currentScreen instanceof RealmsScreen) return "Browsing Realms";
        else if (mc.currentScreen instanceof DirectConnectScreen) return "Using direct connect";
        else if (mc.currentScreen instanceof ServerCleanUpScreen) return "Using server list cleanup";
        else if (mc.currentScreen instanceof AddServerScreen addServerScreen) {
            if (addServerScreen.getTitle().getString().contains("Edit")) return "Editing a server";
            else return "Adding a server";
        } else if (mc.currentScreen instanceof SelectWorldScreen) return "In world selection";
        else if (mc.currentScreen instanceof EditWorldScreen) return "Editing a world";
        else if (mc.currentScreen instanceof CreateWorldScreen || mc.currentScreen instanceof EditGameRulesScreen) return "Creating a new world";
        else if (mc.currentScreen instanceof LevelLoadingScreen || mc.currentScreen instanceof SaveLevelScreen) return "Loading/Saving a world";
        else if (mc.currentScreen instanceof CreditsScreen) return "Reading credits";
        else if (mc.currentScreen instanceof AccountsScreen) return "In account manager";
        else if (mc.currentScreen instanceof AddCrackedAccountScreen) return "Adding cracked account";
        else if (mc.currentScreen instanceof AddAlteningAccountScreen) return "Adding The Altening account";
        else if (mc.currentScreen instanceof ProxiesScreen) return "Editing proxies";
        else if (mc.currentScreen instanceof ProxiesImportScreen) return "Importing proxies";
        else if (mc.currentScreen instanceof YesNoPrompt.PromptScreen) return "Viewing a prompt";
        else if (mc.currentScreen instanceof OkPrompt.PromptScreen) return "Viewing a prompt";
        else if (mc.currentScreen instanceof ConnectScreen) return "Connecting to " + getWorldActivity(true, false);
        else if (mc.currentScreen instanceof DisconnectedScreen) return "Got disconnected from " + getWorldActivity(true, false);
        else if (mc.currentScreen instanceof GameMenuScreen) return "Game paused on " + getWorldActivity(true, false);
        else if (mc.currentScreen instanceof PeekScreen) return "Using .peek on " + getWorldActivity(true, false);
        else if (mc.currentScreen.getClass().getName().contains("com.terraformersmc.modmenu.gui")) {
            if (mc.world != null && serverVisibility.get()) return "Viewing loaded mods (" + getWorldActivity(true, true) + ")";
            else return "Viewing loaded mods";
        } else if (mc.currentScreen instanceof BlockListSettingScreen) {
            if (mc.world != null && serverVisibility.get()) return "Selecting/Configuring blocks (" + getWorldActivity(true, true) + ")";
            else return  "Selecting/Configuring blocks";
        } else if (mc.currentScreen instanceof SBlockDataScreen) {
            if (mc.world != null && serverVisibility.get()) return "Configuring a block (" + getWorldActivity(true, true) + ")";
            else return  "Configuring a block";
        } else if (mc.currentScreen instanceof ItemSettingScreen) {
            if (mc.world != null && serverVisibility.get()) return "Selecting items (" + getWorldActivity(true, true) + ")";
            else return  "Selecting items";
        } else if (mc.currentScreen instanceof StatusEffectListSettingScreen) {
            if (mc.world != null && serverVisibility.get()) return "Selecting effects (" + getWorldActivity(true, true) + ")";
            else return  "Selecting effects";
        } else if (mc.currentScreen instanceof ParticleTypeListSettingScreen) {
            if (mc.world != null && serverVisibility.get()) return "Selecting particles (" + getWorldActivity(true, true) + ")";
            else return  "Selecting particles";
        } else if (mc.currentScreen instanceof SoundEventListSettingScreen) {
            if (mc.world != null && serverVisibility.get()) return "Selecting sounds (" + getWorldActivity(true, true) + ")";
            else return  "Selecting sounds";
        } else if (mc.currentScreen instanceof EnchantmentListSettingScreen) {
            if (mc.world != null && serverVisibility.get()) return "Selecting enchantments (" + getWorldActivity(true, true) + ")";
            else return  "Selecting enchantments";
        } else if (mc.currentScreen instanceof EntityTypeListSettingScreen) {
            if (mc.world != null && serverVisibility.get()) return "Selecting entities (" + getWorldActivity(true, true) + ")";
            else return  "Selecting entities";
        } else if (mc.currentScreen instanceof ModuleListSettingScreen) {
            if (mc.world != null && serverVisibility.get()) return "Selecting modules (" + getWorldActivity(true, true) + ")";
            else return  "Selecting modules";
        } else if (mc.currentScreen instanceof PacketBoolSettingScreen) {
            if (mc.world != null && serverVisibility.get()) return "Selecting packets (" + getWorldActivity(true, true) + ")";
            else return  "Selecting packets";
        } else if (mc.currentScreen instanceof ColorSettingScreen) {
            if (mc.world != null && serverVisibility.get()) return "Configuring a color (" + getWorldActivity(true, true) + ")";
            else return "Configuring a color";
        } else if (mc.currentScreen instanceof ModulesScreen) {
            if (mc.world != null && serverVisibility.get()) return "In Click GUI (" + getWorldActivity(true, true) + ")";
            else return "In Click GUI";
        } else if (mc.currentScreen instanceof ModuleScreen) {
            if (mc.world != null && serverVisibility.get()) return "Editing module " + ((ModuleScreen) mc.currentScreen).module.title + " (" + getWorldActivity(true, true) + ")";
            else return "Editing module " + ((ModuleScreen) mc.currentScreen).module.title;
        } else if (mc.currentScreen instanceof OpenToLanScreen) {
            if (mc.world != null && serverVisibility.get()) return "Opening to LAN (" + getWorldActivity(true, true) + ")";
            return "Opening to LAN";
        } else if (mc.currentScreen instanceof PackScreen) {
            if (mc.world != null && serverVisibility.get()) return "Changing resourcepack (" + getWorldActivity(true, true) + ")";
            return "Changing resourcepack";
        } else if (mc.currentScreen instanceof OptionsScreen) {
            if (mc.world != null && serverVisibility.get()) return "Changing settings (" + getWorldActivity(true, true) + ")";
            return "Changing settings";
        } else if (mc.currentScreen instanceof AccessibilityOptionsScreen) {
            if (mc.world != null && serverVisibility.get()) return "Changing accessibility settings (" + getWorldActivity(true, true) + ")";
            return "Changing accessibility settings";
        } else if (mc.currentScreen instanceof ChatOptionsScreen) {
            if (mc.world != null && serverVisibility.get()) return "Changing chat settings (" + getWorldActivity(true, true) + ")";
            return "Changing chat settings";
        } else if (mc.currentScreen instanceof SoundOptionsScreen) {
            if (mc.world != null && serverVisibility.get()) return "Changing sound settings (" + getWorldActivity(true, true) + ")";
            return "Changing sound settings";
        } else if (mc.currentScreen instanceof LanguageOptionsScreen) {
            if (mc.world != null && serverVisibility.get()) return "Changing language (" + getWorldActivity(true, true) + ")";
            return "Changing language";
        } else if (mc.currentScreen instanceof VideoOptionsScreen) {
            if (mc.world != null && serverVisibility.get()) return "Changing video settings (" + getWorldActivity(true, true) + ")";
            return "Changing video settings";
        } else if (mc.currentScreen instanceof SkinOptionsScreen) {
            if (mc.world != null && serverVisibility.get()) return "Changing skin settings (" + getWorldActivity(true, true) + ")";
            return "Changing Minecraft skin settings";
        } else if (mc.currentScreen instanceof ControlsOptionsScreen) {
            if (mc.world != null && serverVisibility.get()) return "Changing keybinds (" + getWorldActivity(true, true) + ")";
            return "Changing keybinds";
        } else if (mc.currentScreen instanceof StatsScreen) {
            if (mc.world != null && serverVisibility.get()) return "Viewing stats (" + getWorldActivity(true, true) + ")";
            else return "Viewing stats";
        } else if (mc.currentScreen instanceof BaritoneTab.BaritoneScreen) {
            if (mc.world != null && serverVisibility.get()) return "Configuring Baritone (" + getWorldActivity(true, true) + ")";
            else return "Configuring Baritone";
        } else if (mc.currentScreen instanceof ConfigTab.ConfigScreen) {
            if (mc.world != null && serverVisibility.get()) return "Editing config (" + getWorldActivity(true, true) + ")";
            else return "Editing config";
        } else if (mc.currentScreen instanceof EnemiesTab.EnemiesScreen) {
            if (mc.world != null && serverVisibility.get()) return "Editing enemies (" + getWorldActivity(true, true) + ")";
            else return "Editing enemies";
        } else if (mc.currentScreen instanceof FriendsTab.FriendsScreen) {
            if (mc.world != null && serverVisibility.get()) return "Editing friends (" + getWorldActivity(true, true) + ")";
            else return "Editing friends";
        } else if (mc.currentScreen instanceof GuiTab.GuiScreen) {
            if (mc.world != null && serverVisibility.get()) return "Editing GUI (" + getWorldActivity(true, true) + ")";
            else return "Editing GUI";
        } else if (mc.currentScreen instanceof HudTab.HudScreen || mc.currentScreen instanceof HudElementScreen) {
            if (mc.world != null && serverVisibility.get()) return "Editing HUD (" + getWorldActivity(true, true) + ")";
            else return "Editing HUD";
        } else if (mc.currentScreen instanceof MacrosTab.MacrosScreen) {
            if (mc.world != null && serverVisibility.get()) return "Configuring macros (" + getWorldActivity(true, true) + ")";
            else return "Configuring macros";
        } else if (mc.currentScreen instanceof MacrosTab.MacroEditorScreen) {
            if (mc.world != null && serverVisibility.get()) return "Configuring a macro (" + getWorldActivity(true, true) + ")";
            else return "Configuring a macro";
        } else if (mc.currentScreen instanceof ProfilesTab.ProfilesScreen) {
            if (mc.world != null && serverVisibility.get()) return "Changing profiles (" + getWorldActivity(true, true) + ")";
            else return "Changing profiles";
        } else if (mc.currentScreen instanceof MusicTab.MusicScreen) {
            if (mc.world != null && serverVisibility.get()) return "Configuring music (" + getWorldActivity(true, true) + ")";
            else return "Configuring music";
        } else if (mc.currentScreen instanceof PlaylistsScreen) {
            if (mc.world != null && serverVisibility.get()) return "Viewing playlists (" + getWorldActivity(true, true) + ")";
            else return "Viewing playlists";
        } else if (mc.currentScreen instanceof PlaylistViewScreen) {
            if (((PlaylistViewScreen) mc.currentScreen).getTitleString().contains("Search")) {
                if (mc.world != null && serverVisibility.get()) return "Searching for a song (" + getWorldActivity(true, true) + ")";
                else return "Searching for a song";
            } else {
                if (mc.world != null && serverVisibility.get()) return "Viewing a playlist (" + getWorldActivity(true, true) + ")";
                else return "Viewing a playlist";
            }
        } else if (mc.currentScreen.getClass().getName().contains("me.jellysquid.mods.sodium.client")) {
            if (mc.world != null && serverVisibility.get()) return "Changing Sodium video settings (" + getWorldActivity(true, true) + ")";
            else return "Changing Sodium video settings";
        } else if (mc.currentScreen.getClass().getName().contains("net.coderbot.iris.gui.screen")) {
            if (mc.world != null && serverVisibility.get()) return "Changing Iris shaderpack (" + getWorldActivity(true, true) + ")";
            else return "Changing Iris shaderpack";
        } else if (mc.currentScreen.getClass().getName().contains("com.viaversion.fabric.mc117.gui")) return "Changing Minecraft version";
        else if (mc.world != null) return getWorldActivity(false, false);
        return "Unknown Activity";
    }

    private String getUsername() {
        if (Modules.get().isActive(NameProtect.class)) return Modules.get().get(NameProtect.class).getName(mc.getSession().getUsername());
        else return mc.getSession().getUsername();
    }

    private String getHealth() {
        if (!Modules.get().get(DiscordRPC.class).playerHealth.get() || mc.world == null || mc.player == null) return "";
        else if (mc.player.isDead()) return " | Dead";
        else if (mc.player.isCreative()) return " | Creative Mode";
        else if (mc.player.isSpectator()) return " | Spectator Mode";
        return " | " + Utils.getPlayerHealth() + " HP";
    }

    // Retarded af cope
    private String getWorldActivity(boolean naked, boolean upperCase) {

        // Disconnected etc...
        if (naked && !mc.isInSingleplayer() && mc.getCurrentServerEntry() != null && LastServerInfo.getLastServer() != null) {
            if (Modules.get().get(DiscordRPC.class).serverVisibility.get()) return LastServerInfo.getLastServer().address;
            else return "a server";
        }

        // Multiplayer
        if (mc.getCurrentServerEntry() != null) {
            String name = mc.isConnectedToRealms() ? "realms" : mc.getCurrentServerEntry().address;

            if (naked) {
                if (name.equals("realms") && upperCase) return "Realms";
                return name;
            }

            if (Modules.get().get(DiscordRPC.class).serverVisibility.get()) return "Playing on " + name;
            else {
                if (naked) {
                    if (upperCase) return "A server";
                    return "a server";
                }

                return "Playing on a server";
            }
        }

        if ((mc.getServer()) == null) {
            if (naked) {
                if (upperCase) return "Unknown";
                return "unknown";
            }

            return "Could not get server/world";
        }

        if (((MinecraftServerAccessor) mc.getServer()).getSession() == null) {
            if (naked) {
                if (upperCase) return "Unknown";
                return "unknown";
            }

            return "Could not get server/world";
        }

        // Singleplayer
        if (mc.isInSingleplayer()) {
            File folder = ((MinecraftServerAccessor) mc.getServer()).getSession().getWorldDirectory(mc.world.getRegistryKey());
            if (folder.toPath().relativize(mc.runDirectory.toPath()).getNameCount() != 2) folder = folder.getParentFile();

            if (naked) {
                if (upperCase) {
                    if (worldVisibility.get()) return "Singleplayer (" + folder.getName() + ")";
                    return "Singleplayer";
                }

                if (worldVisibility.get()) return "singleplayer (" + folder.getName() + ")";
                return "singleplayer";
            }

            if (worldVisibility.get()) return "Playing singleplayer (" + folder.getName() + ")";
            else return "Playing singleplayer";
        }

        if (naked) {
            if (upperCase) return "Unknown";
            return "unknown";
        }

        return "Could not get server/world";
    }

    private static void applySmallImage() {
        if (delay == 5) {
            delay = 0;

            if (number == 16) number = 1;

            if (Modules.get().get(DiscordRPC.class).smallImageMode.get() == SmallImageMode.Dogs) rpc.smallImageKey = "dog-" + number;
            else rpc.smallImageKey = "cat-" + number;

            number++;
        } else delay++;
    }

    public enum SmallImageMode {
        Cats("Cats"),
        Dogs("Dogs");

        private final String title;

        SmallImageMode(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }
}
