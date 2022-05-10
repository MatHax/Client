package mathax.client.utils;

import com.google.gson.JsonParser;
import mathax.client.MatHax;
import mathax.client.eventbus.EventHandler;
import mathax.client.events.game.GameJoinedEvent;
import mathax.client.events.game.GameLeftEvent;
import mathax.client.utils.network.HTTP;
import mathax.client.utils.render.prompts.YesNoPrompt;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.util.Util;

public class Version {
    private final String string;
    private final int[] numbers;

    public Version(String string) {
        this.string = string;
        this.numbers = new int[3];

        String[] split = string.split("\\.");
        if (split.length != 3) throw new IllegalArgumentException("[MatHax] Version string needs to have 3 numbers.");

        for (int i = 0; i < 3; i++) {
            try {
                numbers[i] = Integer.parseInt(split[i]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("[MatHax] Failed to parse version string.");
            }
        }
    }

    public static void init() {
        MatHax.EVENT_BUS.subscribe(Version.class);
    }

    public boolean isHigherThan(Version version) {
        for (int i = 0; i < 3; i++) {
            if (numbers[i] > version.numbers[i]) return true;
            if (numbers[i] < version.numbers[i]) return false;
        }

        return false;
    }

    @Override
    public String toString() {
        return string;
    }

    public static Version get() {
        return new Version(FabricLoader.getInstance().getModContainer("mathax").get().getMetadata().getVersion().getFriendlyString());
    }

    public static Integer getDev() {
        return 0;
    }

    public static String getDevString() {
        if (getDev() < 1) return "";
        else return "Dev-" + getDev();
    }

    public static String getStylized() {
        if (getDev() < 1) return "v" + get();
        else return "v" + get() + " " + getDevString();
    }

    public static String getMinecraft(){
        return SharedConstants.getGameVersion().getName();
    }

    public static Integer getMinecraftProtocol(){
        return SharedConstants.getGameVersion().getProtocolVersion();
    }

    public static class UpdateChecker {
        public static boolean checkForLatestTitle = true;
        public static boolean checkForLatest = true;

        @EventHandler
        private static void onGameJoined(GameJoinedEvent event) {
            Version.UpdateChecker.checkForLatest = true;
        }

        @EventHandler
        private static void onGameLeft(GameLeftEvent event) {
            Version.UpdateChecker.checkForLatest = true;
        }

        public static Version getLatest() {
            String api = HTTP.get(MatHax.API_URL + "Version/metadata.json").sendString();
            if (api == null || !api.contains(getMinecraft().replace(".", "-"))) return null;

            return new Version(new JsonParser().parse(api).getAsJsonObject().get(getMinecraft().replace(".", "-")).getAsString());
        }

        public static CheckResult checkLatest() {
            if (getDev() != 0) return CheckResult.Running_Dev;

            Version latestVersion = getLatest();
            if (latestVersion == null) return CheckResult.Cant_Check;
            else {
                if (latestVersion.isHigherThan(get())) return CheckResult.Newer_Found;
                else return CheckResult.Latest;
            }
        }

        public static void checkForUpdate() {
            MatHax.LOG.info(MatHax.LOG_PREFIX + "Checking for latest version of MatHax!");
            switch (checkLatest()) {
                case Cant_Check -> MatHax.LOG.info(MatHax.LOG_PREFIX + "Could not check for latest version!");
                case Newer_Found -> {
                    MatHax.LOG.info(MatHax.LOG_PREFIX + "There is a new version of MatHax, v" + getLatest() + "! You are using " + Version.getStylized() + "! You can download the newest version on " + MatHax.URL + "Download!");
                    YesNoPrompt.create()
                        .id("new-update")
                        .title("New Update")
                        .message("A new version of MatHax has been released!")
                        .message("\n")
                        .message("Your version: %s", Version.getStylized())
                        .message("Latest version: v%s", getLatest())
                        .message("\n")
                        .message("Do you want to update?")
                        .message("Using old versions of MatHax is not recommended")
                        .message("and could report in issues.")
                        .onYes(() -> Util.getOperatingSystem().open(MatHax.URL + "Download"))
                        .show();
                }
                case Latest -> MatHax.LOG.info(MatHax.LOG_PREFIX + "You are using the latest version of MatHax, " + Version.getStylized() + "!");
                case Running_Dev -> MatHax.LOG.info(MatHax.LOG_PREFIX + "Developer builds do not get update notifications about another developer build of the version they are a developer build of! You are running " + getStylized() + "!");
            }
        }

        public enum CheckResult {
            Cant_Check,
            Newer_Found,
            Latest,
            Running_Dev
        }
    }
}
