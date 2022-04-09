package mathax.client.gui.screens.proxies;

import mathax.client.gui.GuiTheme;
import mathax.client.gui.WindowScreen;
import mathax.client.gui.widgets.containers.WVerticalList;
import mathax.client.gui.widgets.pressable.WButton;
import mathax.client.systems.proxies.Proxies;
import mathax.client.systems.proxies.Proxy;
import mathax.client.systems.proxies.ProxyType;
import mathax.client.utils.Utils;
import mathax.client.utils.render.color.Color;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.regex.Matcher;

public class ProxiesImportScreen extends WindowScreen {
    private final File file;

    public ProxiesImportScreen(GuiTheme theme, File file) {
        super(theme, "Import Proxies");
        this.file = file;
        this.onClosed(() -> {
            if (parent instanceof ProxiesScreen screen) screen.doReload = true;
        });
    }

    @Override
    public void initWidgets() {
        if (file.exists() && file.isFile()) {
            add(theme.label("Importing proxies from " + file.getName() + "...").color(Color.GREEN));
            WVerticalList list = add(theme.section("Log", false)).widget().add(theme.verticalList()).expandX().widget();
            Proxies proxies = Proxies.get();
            try {
                int one = 0;
                int two = 0;
                for (String line : Files.readAllLines(file.toPath())) {
                    Matcher matcher = Proxies.PROXY_PATTERN.matcher(line);
                    if (matcher.matches()) {
                        Proxy proxy = new Proxy();
                        proxy.address = matcher.group(2).replaceAll("\\b0+\\B", "");
                        proxy.port = Integer.parseInt(matcher.group(3));
                        proxy.name = matcher.group(1) != null ? matcher.group(1) : proxy.address + ":" + proxy.port;
                        proxy.type = matcher.group(4) != null ? ProxyType.parse(matcher.group(4)) : ProxyType.Socks4;
                        if (proxies.add(proxy)) {
                            list.add(theme.label("Imported proxy: " + proxy.name).color(Color.GREEN));
                            one++;
                        } else {
                            list.add(theme.label("Proxy already exists: " + proxy.name).color(Color.ORANGE));
                            two++;
                        }
                    } else {
                        list.add(theme.label("Invalid proxy: " + line).color(Color.RED));
                        two++;
                    }
                }
                add(theme
                    .label("Successfully imported " + one + "/" + (two + one) + " proxies.")
                    .color(Utils.lerp(Color.RED, Color.GREEN, (float) one / (one + two)))
                );
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else add(theme.label("Invalid File!"));
        add(theme.horizontalSeparator()).expandX();
        WButton btnBack = add(theme.button("Back")).expandX().widget();
        btnBack.action = this::onClose;
    }
}
