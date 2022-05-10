package mathax.client.mixin;

import mathax.client.gui.screens.servermanager.ServerCleanUpScreen;
import mathax.client.mixininterface.IMultiplayerScreen;
import mathax.client.systems.modules.Modules;
import mathax.client.systems.proxies.Proxies;
import mathax.client.systems.proxies.Proxy;
import mathax.client.utils.Utils;
import mathax.client.utils.Version;
import mathax.client.utils.misc.LastServerInfo;
import mathax.client.utils.render.color.Color;
import mathax.client.MatHax;
import mathax.client.gui.GuiThemes;
import mathax.client.systems.modules.misc.NameProtect;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/*/--------------------------------------------/*/
/*/ Server Clean Up by JFronny                 /*/
/*/ https://github.com/JFronny/MeteorAdditions /*/
/*/--------------------------------------------/*/

@Mixin(MultiplayerScreen.class)
public abstract class MultiplayerScreenMixin extends Screen implements IMultiplayerScreen {
    private final int WHITE = Color.fromRGBA(255, 255, 255, 255);
    private final int GRAY = Color.fromRGBA(175, 175, 175, 255);

    @Shadow
    protected MultiplayerServerListWidget serverListWidget;

    public MultiplayerScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo info) {
        Version.UpdateChecker.checkForLatest = true;

        addDrawableChild(new ButtonWidget(width - 77, 2, 75, 20, new LiteralText("Accounts"), button -> client.setScreen(GuiThemes.get().accountsScreen())));
        addDrawableChild(new ButtonWidget(width - 154, 2, 75, 20, new LiteralText("Proxies"), button -> client.setScreen(GuiThemes.get().proxiesScreen())));
        addDrawableChild(new ButtonWidget(width - 231, 2, 75, 20, new LiteralText("Clean Up"), button -> client.setScreen(new ServerCleanUpScreen(GuiThemes.get(), this))));

        if (LastServerInfo.getLastServer() != null) addDrawableChild(new ButtonWidget(width / 2 - 154, 10, 100, 20, new LiteralText("Last Server"), button -> LastServerInfo.reconnect(client.currentScreen)));
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo info) {
        float x = 2;
        float y = 2;

        String space = " ";
        int spaceLength = textRenderer.getWidth(space);

        String loggedInAs = "Logged in as";
        int loggedInAsLength = textRenderer.getWidth(loggedInAs);
        String loggedName = Modules.get().get(NameProtect.class).getName(client.getSession().getUsername());
        int loggedNameLength = textRenderer.getWidth(loggedName);
        String loggedOpenDeveloper = "[";
        int loggedOpenDeveloperLength = textRenderer.getWidth(loggedOpenDeveloper);
        String loggedDeveloper = "Developer";
        int loggedDeveloperLength = textRenderer.getWidth(loggedDeveloper);
        String loggedCloseDeveloper = "]";

        // Logged in as

        drawStringWithShadow(matrices, textRenderer, loggedInAs, 2, (int) y, GRAY);
        drawStringWithShadow(matrices, textRenderer, space, loggedInAsLength + 2, (int) y, GRAY);
        drawStringWithShadow(matrices, textRenderer, loggedName, loggedInAsLength + spaceLength + 2, (int) y, WHITE);
        if (Modules.get() != null && !Modules.get().isActive(NameProtect.class) && Utils.isDeveloper(client.getSession().getUuid())) {
            drawStringWithShadow(matrices, textRenderer, space, loggedInAsLength + spaceLength + loggedNameLength + 2, (int) y, GRAY);
            drawStringWithShadow(matrices, textRenderer, loggedOpenDeveloper, loggedInAsLength + spaceLength + loggedNameLength + spaceLength + 2, (int) y, GRAY);
            drawStringWithShadow(matrices, textRenderer, loggedDeveloper, loggedInAsLength + spaceLength + loggedNameLength + spaceLength + loggedOpenDeveloperLength + 2, (int) y, MatHax.INSTANCE.MATHAX_COLOR_INT);
            drawStringWithShadow(matrices, textRenderer, loggedCloseDeveloper, loggedInAsLength + spaceLength + loggedNameLength + spaceLength + loggedOpenDeveloperLength + loggedDeveloperLength + 2, (int) y, GRAY);
        }

        y += textRenderer.fontHeight + 2;

        // Proxy

        Proxy proxy = Proxies.get().getEnabled();

        String proxyLeft = proxy != null ? "Using proxy" + " " : "Not using a proxy";
        String proxyRight = proxy != null ? (proxy.name != null && !proxy.name.isEmpty() ? "(" + proxy.name + ") " : "") + proxy.address + ":" + proxy.port : null;

        drawStringWithShadow(matrices, textRenderer, proxyLeft, (int)x, (int) y, GRAY);
        if (proxyRight != null) drawStringWithShadow(matrices, textRenderer, proxyRight, (int)x + textRenderer.getWidth(proxyLeft), (int) y, WHITE);
    }

    @Override
    public MultiplayerServerListWidget getServerListWidget() {
        return serverListWidget;
    }

    @Inject(at = @At("HEAD"), method = "connect(Lnet/minecraft/client/network/ServerInfo;)V")
    private void onConnect(ServerInfo entry, CallbackInfo info) {
        LastServerInfo.setLastServer(entry);
    }
}
