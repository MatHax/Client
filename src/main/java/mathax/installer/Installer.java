package mathax.installer;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import mathax.installer.layouts.VerticalLayout;
import net.fabricmc.installer.Main;
import net.fabricmc.installer.util.MetaHandler;
import net.fabricmc.installer.util.Reference;
import net.fabricmc.installer.util.Utils;
import org.json.JSONException;

import javax.net.ssl.HttpsURLConnection;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

/*/-------------------------------------------------/*/
/*/ THANKS TO IRIS DEVELOPERS FOR MOST OF THIS CODE /*/
/*/ https://irisshaders.net                         /*/
/*/ https://github.com/IrisShaders/Iris-Installer/  /*/
/*/-------------------------------------------------/*/

public class Installer {
    public static String API_URL = "https://api.mathaxclient.xyz/";

    InstallerMeta INSTALLER_META;
    Version INSTALLER_VERSION;
    List<String> CLIENT_VERSIONS;
    List<String> GAME_VERSIONS;

    String selectedClientVersion;
    String selectedGameVersion;
    Path customInstallDir;

    JButton button;
    JComboBox<String> clientVersionDropdown;
    JComboBox<String> gameVersionDropdown;
    JButton installDirectoryPicker;
    JButton installHelpButton;
    JProgressBar progressBar;

    boolean finishedSuccessfulInstall = false;

    public Installer() {}

    public static void main(String[] args) {
        System.out.println("Launching installer...");
        new Installer().start();
    }

    public void start() {
        INSTALLER_VERSION = Version.get();

        boolean dark = DarkModeDetector.isDarkMode();
        System.setProperty("apple.awt.application.appearance", "system");
        if (dark) FlatDarkLaf.setup();
        else FlatLightLaf.setup();

        boolean newerFound;
        Version latest = Version.getLatest();
        if (latest == null) newerFound = false;
        else newerFound = latest.isHigherThan(INSTALLER_VERSION);
        if (newerFound) {
            System.out.println("There is a new version of MatHax Installer, v" + Version.getLatest() + "! You are using v" + INSTALLER_VERSION + "!");
            JOptionPane.showMessageDialog(null, "There is a new version of MatHax Installer, v" + Version.getLatest() + "! You are using v" + INSTALLER_VERSION + "!", "Newer MatHax Installer found!", JOptionPane.ERROR_MESSAGE);
            return;
        }

        INSTALLER_META = new InstallerMeta(API_URL + "Version/Installer/metadata.json");
        try {
            INSTALLER_META.load();
        } catch (IOException exception) {
            System.out.println("Failed to fetch installer metadata from the server!");
            exception.printStackTrace();
            JOptionPane.showMessageDialog(null, "The installer was unable to fetch metadata from the server, please check your internet connection and try again later.", "Please check your internet connection!", JOptionPane.ERROR_MESSAGE);
            return;
        } catch (JSONException exception) {
            System.out.println("Failed to fetch installer metadata from the server!");
            exception.printStackTrace();
            JOptionPane.showMessageDialog(null, "Installer metadata parsing failed, please contact the MatHax support team via Discord! \nError: " + exception, "Metadata parsing failed!", JOptionPane.ERROR_MESSAGE);
            return;
        }

        CLIENT_VERSIONS = INSTALLER_META.getClientVersions();
        GAME_VERSIONS = INSTALLER_META.getGameVersions();

        JFrame frame = new JFrame("MatHax Installer - v" + INSTALLER_VERSION);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setSize((int) Toolkit.getDefaultToolkit().getScreenSize().getWidth() / 5, (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight() / 4);
        frame.setLocationRelativeTo(null);
        frame.setIconImage(new ImageIcon(Objects.requireNonNull(Utils.class.getClassLoader().getResource("assets/mathax/textures/icons/icon.png"))).getImage());

        JPanel topPanel = new JPanel(new VerticalLayout());

        JPanel clientVersionPanel = new JPanel();

        JLabel clientVersionDropdownLabel = new JLabel("Select Client Version:");
        clientVersionDropdownLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        List<String> clientVersions = CLIENT_VERSIONS.subList(0, CLIENT_VERSIONS.size());
        Collections.reverse(clientVersions);
        String[] clientVersionList = clientVersions.toArray(new String[0]);
        selectedClientVersion = clientVersionList[0];

        clientVersionDropdown = new JComboBox<>(clientVersionList);
        clientVersionDropdown.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                selectedClientVersion = (String) e.getItem();

                readyAll();
            }
        });

        clientVersionPanel.add(clientVersionDropdownLabel);
        clientVersionPanel.add(clientVersionDropdown);

        JPanel gameVersionPanel = new JPanel();

        JLabel gameVersionDropdownLabel = new JLabel("Select Game Version:");
        gameVersionDropdownLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        List<String> gameVersions = GAME_VERSIONS.subList(0, GAME_VERSIONS.size());
        Collections.reverse(gameVersions);
        String[] gameVersionList = gameVersions.toArray(new String[0]);
        selectedGameVersion = gameVersionList[0];

        gameVersionDropdown = new JComboBox<>(gameVersionList);
        gameVersionDropdown.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                selectedGameVersion = (String) e.getItem();

                readyAll();
            }
        });

        gameVersionPanel.add(gameVersionDropdownLabel);
        gameVersionPanel.add(gameVersionDropdown);

        JPanel installDirectoryPanel = new JPanel();

        JLabel installDirectoryPickerLabel = new JLabel("Select Install Directory:");
        installDirectoryPickerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        installDirectoryPicker = new JButton(getDefaultInstallDir().toFile().getName());
        installDirectoryPicker.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setFileHidingEnabled(false);
            int option = fileChooser.showOpenDialog(frame);
            if (option == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                customInstallDir = file.toPath();
                installDirectoryPicker.setText(file.getName());

                readyAll();
            }
        });

        installDirectoryPanel.add(installDirectoryPickerLabel);
        installDirectoryPanel.add(installDirectoryPicker);

        JPanel installHelpPanel = new JPanel();

        JLabel installHelpLabel = new JLabel("Installation Help:");
        installHelpLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        installHelpButton = new JButton("Help");
        installHelpButton.addActionListener(event -> openUrl("https://mathaxclient.xyz/Installation/"));

        installHelpPanel.add(installHelpLabel);
        installHelpPanel.add(installHelpButton);

        topPanel.add(clientVersionPanel);
        topPanel.add(gameVersionPanel);
        topPanel.add(installDirectoryPanel);
        topPanel.add(installHelpPanel);

        JPanel bottomPanel = new JPanel();

        progressBar = new JProgressBar();
        progressBar.setValue(0);
        progressBar.setMaximum(100);
        progressBar.setStringPainted(true);

        button = new JButton("Install");
        button.addActionListener(action -> {
            File storageDir = getStorageDirectory().toFile();
            if (!storageDir.exists() || !storageDir.isDirectory()) storageDir.mkdir();

            button.setText("Downloading...");
            progressBar.setValue(0);
            setInteractionEnabled(false);

            String jarName = "MatHax-v" + selectedClientVersion + "-Fabric_" + selectedGameVersion + ".jar";
            String downloadURL = "https://api.mathaxclient.xyz/Download/" + selectedGameVersion.replace(".", "-") + "/" + jarName;

            File saveLocation = getStorageDirectory().resolve(jarName).toFile();

            final Downloader downloader = new Downloader(downloadURL, saveLocation);
            downloader.addPropertyChangeListener(event -> {
                if ("progress".equals(event.getPropertyName())) progressBar.setValue((Integer) event.getNewValue());
                else if (event.getNewValue() == SwingWorker.StateValue.DONE) {
                    try {
                        downloader.get();
                    } catch (InterruptedException | ExecutionException e) {
                        System.out.println("Failed to download jar!");
                        e.getCause().printStackTrace();

                        String msg = String.format("An error occurred while attempting to download the required files, please check your internet connection and try again! \nError: %s", e.getCause().toString());
                        JOptionPane.showMessageDialog(frame, msg, "Download Failed!", JOptionPane.ERROR_MESSAGE, null);
                        readyAll();

                        return;
                    }

                    button.setText("Download completed!");

                    boolean cancelled = false;

                    File installDir = getInstallDir().toFile();
                    if (!installDir.exists() || !installDir.isDirectory()) installDir.mkdir();

                    File modsFolder = getInstallDir().resolve("mods").toFile();
                    File[] modsFolderContents = modsFolder.listFiles();

                    if (modsFolderContents != null) {
                        boolean isEmpty = modsFolderContents.length == 0;

                        if (modsFolder.exists() && modsFolder.isDirectory() && !isEmpty) {
                            boolean failedToRemoveMatHax = false;
                            boolean shownMatHaxDialog = false;

                            for (File mod : modsFolderContents) {
                                String modName = mod.getName().toLowerCase();
                                if (!shownMatHaxDialog && !modName.contains("installer") && (modName.contains("mathax"))) {
                                    int result = JOptionPane.showOptionDialog(frame, "Another installation of MatHax was found in your mods folder. Do you want to remove it, or cancel the installation? \n\nFile Name: " + mod.getName(), "Installed MatHax Detected", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, new String[] {"Yes", "Cancel"}, "Yes");

                                    shownMatHaxDialog = true;
                                    if (result != JOptionPane.YES_OPTION) {
                                        cancelled = true;
                                        break;
                                    }

                                    if (!mod.delete()) failedToRemoveMatHax = true;
                                }
                            }

                            if (failedToRemoveMatHax) {
                                System.out.println("Failed to remove MatHax from mods folder to update them!");
                                JOptionPane.showMessageDialog(frame, "Failed to remove MatHax from your mods folder to update them, please make sure your game is closed and try again!", "Failed to prepare MatHax for update", JOptionPane.ERROR_MESSAGE);
                                cancelled = true;
                            }
                        }

                        if (!cancelled) {
                            boolean shownOptifineDialog = false;
                            boolean failedToRemoveOptifine = false;

                            for (File mod : modsFolderContents) {
                                String modName = mod.getName().toLowerCase();
                                if (modName.contains("optifine") || modName.contains("optifabric")) {
                                    if (!shownOptifineDialog) {
                                        int result = JOptionPane.showOptionDialog(frame,"Optifine was found in your mods folder, but Optifine is incompatible with MatHax. Do you want to remove it, or cancel the installation? \n\nFile Name: " + mod.getName(), "Installed Optifine Detected", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, new String[] {"Yes", "Cancel"}, "Yes");

                                        shownOptifineDialog = true;
                                        if (result != JOptionPane.YES_OPTION) {
                                            cancelled = true;
                                            break;
                                        }
                                    }

                                    if (!mod.delete()) failedToRemoveOptifine = true;
                                }
                            }

                            if (failedToRemoveOptifine) {
                                System.out.println("Failed to delete Optifine from mods folder");
                                JOptionPane.showMessageDialog(frame, "Failed to remove Optifine from your mods folder, please make sure your game is closed and try again!", "Failed to remove Optifine", JOptionPane.ERROR_MESSAGE);
                                cancelled = true;
                            }
                        }
                    }

                    if (cancelled) {
                        readyAll();
                        return;
                    }

                    if (!modsFolder.exists() || !modsFolder.isDirectory()) modsFolder.mkdir();

                    boolean installSuccess = installJar(saveLocation, jarName, modsFolder);
                    if (installSuccess) {
                        button.setText("Installation succeeded!");
                        finishedSuccessfulInstall = true;
                        clientVersionDropdown.setEnabled(true);
                        gameVersionDropdown.setEnabled(true);
                        installDirectoryPicker.setEnabled(true);
                        installHelpButton.setEnabled(true);
                    } else {
                        button.setText("Installation failed!");
                        System.out.println("Failed to install to mods folder!");
                        JOptionPane.showMessageDialog(frame, "Failed to install to mods folder, please make sure your game is closed and try again!", "Installation Failed!", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });

            downloader.execute();
        });

        bottomPanel.add(progressBar);
        bottomPanel.add(button);

        frame.getContentPane().add(topPanel, BorderLayout.NORTH);
        frame.getContentPane().add(bottomPanel, BorderLayout.SOUTH);

        frame.setVisible(true);

        System.out.println("Installer launched!");
    }

    class Downloader extends SwingWorker<Void, Void> {
        private final String url;
        private final File file;

        public Downloader(String url, File file) {
            this.url = url;
            this.file = file;
        }

        @Override
        protected Void doInBackground() throws Exception {
            URL url = new URL(this.url);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            long filesize = connection.getContentLengthLong();
            if (filesize == -1) throw new Exception("Content length must not be -1 (unknown)!");
            long totalDataRead = 0;
            try (java.io.BufferedInputStream in = new java.io.BufferedInputStream(connection.getInputStream())) {
                java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
                try (java.io.BufferedOutputStream bout = new BufferedOutputStream(fos, 1024)) {
                    byte[] data = new byte[1024];
                    int i;
                    while ((i = in.read(data, 0, 1024)) >= 0) {
                        totalDataRead = totalDataRead + i;
                        bout.write(data, 0, i);
                        int percent = (int) ((totalDataRead * 100) / filesize);
                        setProgress(percent);
                    }
                }
            }

            return null;
        }
    }

    public boolean installJar(File jar, String jarName, File mods) {
        try {
            Files.copy(jar.toPath(), new File(mods + "/" + jarName).toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Files.exists(mods.toPath().resolve(jarName));
    }

    public Path getStorageDirectory() {
        return getAppDataDirectory().resolve(getStorageDirectoryName());
    }

    public Path getInstallDir() {
        return customInstallDir != null ? customInstallDir : getDefaultInstallDir();
    }

    public Path getAppDataDirectory() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) return new File(System.getenv("APPDATA")).toPath();
        else if (os.contains("mac")) return new File(System.getProperty("user.home") + "/Library/Application Support").toPath();
        else if (os.contains("nux")) return new File(System.getProperty("user.home")).toPath();
        else return new File(System.getProperty("user.dir")).toPath();
    }

    public String getStorageDirectoryName() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) return "mathax-installer";
        else return ".mathax-installer";
    }

    public Path getDefaultInstallDir() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) return getAppDataDirectory().resolve("minecraft");
        else return getAppDataDirectory().resolve(".minecraft");
    }

    public void setInteractionEnabled(boolean enabled) {
        clientVersionDropdown.setEnabled(enabled);
        gameVersionDropdown.setEnabled(enabled);
        installDirectoryPicker.setEnabled(enabled);
        installHelpButton.setEnabled(enabled);
        button.setEnabled(enabled);
    }

    public void readyAll() {
        finishedSuccessfulInstall = false;
        button.setText("Install");
        progressBar.setValue(0);
        setInteractionEnabled(true);
    }

    public static void openUrl(String url) {
        String os = System.getProperty("os.name").toLowerCase();

        try {
            if (os.contains("win")) {
                if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) Desktop.getDesktop().browse(new URI(url));
            } else if (os.contains("mac")) Runtime.getRuntime().exec("open " + url);
            else if (os.contains("nix") || os.contains("nux")) Runtime.getRuntime().exec("xdg-open " + url);
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
    }
}
