package mathax.client.utils.render.prompts;

import com.mojang.blaze3d.systems.RenderSystem;
import mathax.client.gui.widgets.containers.WHorizontalList;
import mathax.client.gui.widgets.pressable.WCheckbox;
import mathax.client.gui.GuiTheme;
import mathax.client.gui.GuiThemes;
import mathax.client.gui.WindowScreen;
import mathax.client.gui.widgets.pressable.WButton;
import mathax.client.systems.config.Config;
import net.minecraft.client.gui.screen.Screen;

import java.util.ArrayList;
import java.util.List;

import static mathax.client.MatHax.mc;

public class OkPrompt {
    private final GuiTheme theme;
    private final Screen parent;

    private String title = "";
    private final List<String> messages = new ArrayList<>();
    private String id = null;

    private Runnable onOk = () -> {};

    private OkPrompt() {
        this(GuiThemes.get(), mc.currentScreen);
    }

    private OkPrompt(GuiTheme theme, Screen parent) {
        this.theme = theme;
        this.parent = parent;
    }

    public static OkPrompt create() {
        return new OkPrompt();
    }

    public static OkPrompt create(GuiTheme theme, Screen parent) {
        return new OkPrompt(theme, parent);
    }

    public OkPrompt title(String title) {
        this.title = title;
        return this;
    }

    public OkPrompt message(String message) {
        this.messages.add(message);
        return this;
    }

    public OkPrompt message(String message, Object... args) {
        this.messages.add(String.format(message, args));
        return this;
    }

    public OkPrompt id(String from) {
        this.id = from;
        return this;
    }

    public OkPrompt onOk(Runnable action) {
        this.onOk = action;
        return this;
    }

    public void show() {
        if (id == null) this.id(this.title);
        if (Config.get().dontShowAgainPrompts.contains(id)) return;

        if (!RenderSystem.isOnRenderThread()) RenderSystem.recordRenderCall(() -> mc.setScreen(new PromptScreen(theme)));
        else mc.setScreen(new PromptScreen(theme));
    }

    public class PromptScreen extends WindowScreen {
        public PromptScreen(GuiTheme theme) {
            super(theme, OkPrompt.this.title);

            this.parent = OkPrompt.this.parent;
        }

        @Override
        public void initWidgets() {
            for (String line : messages) add(theme.label(line)).expandX();
            add(theme.horizontalSeparator()).expandX();

            WHorizontalList checkboxContainer = add(theme.horizontalList()).expandX().widget();
            WCheckbox dontShowAgainCheckbox = checkboxContainer.add(theme.checkbox(false)).widget();
            checkboxContainer.add(theme.label("Don't show this again.")).expandX();

            WHorizontalList list = add(theme.horizontalList()).expandX().widget();
            WButton okButton = list.add(theme.button("Ok")).expandX().widget();
            okButton.action = () -> {
                if (dontShowAgainCheckbox.checked) Config.get().dontShowAgainPrompts.add(id);
                onOk.run();
                onClose();
            };
        }
    }
}
