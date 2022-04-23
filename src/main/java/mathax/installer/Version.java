package mathax.installer;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;
import java.util.Scanner;

public class Version {
    private final String string;
    private final int[] numbers;

    public Version(String string) {
        this.string = string;
        this.numbers = new int[3];

        String[] split = string.split("\\.");
        if (split.length != 3) throw new IllegalArgumentException("Version string needs to have 3 numbers.");

        for (int i = 0; i < 3; i++) {
            try {
                numbers[i] = Integer.parseInt(split[i]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Failed to parse version string.");
            }
        }
    }

    public boolean isHigherThan(Version version) {
        for (int i = 0; i < 3; i++) {
            if (numbers[i] > version.numbers[i]) return true;
            if (numbers[i] < version.numbers[i]) return false;
        }

        return false;
    }

    public static Version get() {
        Scanner scanner = new Scanner(Objects.requireNonNull(Version.class.getResourceAsStream("/metadata.json"))).useDelimiter("\\A");
        return new Version(new JSONObject(scanner.hasNext() ? scanner.next() : "").getString("version"));
    }

    public static Version getLatest() throws IOException {
        return new Version(JSONUtils.readJsonFromUrl(Installer.API_URL + "Version/Installer/metadata.json").getString("version"));
    }

    @Override
    public String toString() {
        return string;
    }
}
