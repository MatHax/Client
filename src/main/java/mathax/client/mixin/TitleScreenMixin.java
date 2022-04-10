package mathax.client.mixin;

import mathax.client.MatHax;
import mathax.client.systems.modules.Modules;
import mathax.client.systems.modules.misc.NameProtect;
import mathax.client.systems.proxies.Proxies;
import mathax.client.systems.proxies.Proxy;
import mathax.client.utils.Utils;
import mathax.client.utils.Version;
import mathax.client.utils.render.color.Color;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

@Mixin(TitleScreen.class)
public class TitleScreenMixin extends Screen {
    private final int WHITE = Color.fromRGBA(255, 255, 255, 255);
    private final int GRAY = Color.fromRGBA(175, 175, 175, 255);

    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/TitleScreen;drawStringWithShadow(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V", ordinal = 0))
    private void checkForUpdate(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo info) {
        if (Version.UpdateChecker.checkForLatestTitle) {
            Version.UpdateChecker.checkForLatestTitle = false;

            Version.UpdateChecker.checkForUpdate();
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo info) {
        float y = 2;
        float y2 = y + textRenderer.fontHeight + y;

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


        Proxy proxy = Proxies.get().getEnabled();
        String proxyUsing = proxy != null ? "Using proxy" + " " : "Not using a proxy";
        int proxyUsingLength = textRenderer.getWidth(proxyUsing);
        String proxyDetails = proxy != null ? "(" + proxy.name + ") " + proxy.address + ":" + proxy.port : null;

        String watermarkName = "MatHax";
        int watermarkNameLength = textRenderer.getWidth(watermarkName);
        String watermarkVersion = Version.getStylized();
        int watermarkVersionLength = textRenderer.getWidth(watermarkVersion);
        int watermarkFullLength = watermarkNameLength + spaceLength + watermarkVersionLength;

        String authorBy = "By";
        int authorByLength = textRenderer.getWidth(authorBy);
        String authorName = "Matejko06";
        int authorNameLength = textRenderer.getWidth(authorName);
        int authorFullLength = authorByLength + spaceLength + authorNameLength;

        drawStringWithShadow(matrices, textRenderer, loggedInAs, 2, (int) y, GRAY);
        drawStringWithShadow(matrices, textRenderer, space, loggedInAsLength + 2, (int) y, GRAY);
        drawStringWithShadow(matrices, textRenderer, loggedName, loggedInAsLength + spaceLength + 2, (int) y, WHITE);

        if (Modules.get() != null && !Modules.get().isActive(NameProtect.class) && Utils.isDeveloper(client.getSession().getUuid())) {
            drawStringWithShadow(matrices, textRenderer, space, loggedInAsLength + spaceLength + loggedNameLength + 2, (int) y, GRAY);
            drawStringWithShadow(matrices, textRenderer, loggedOpenDeveloper, loggedInAsLength + spaceLength + loggedNameLength + spaceLength + 2, (int) y, GRAY);
            drawStringWithShadow(matrices, textRenderer, loggedDeveloper, loggedInAsLength + spaceLength + loggedNameLength + spaceLength + loggedOpenDeveloperLength + 2, (int) y, MatHax.INSTANCE.MATHAX_COLOR_INT);
            drawStringWithShadow(matrices, textRenderer, loggedCloseDeveloper, loggedInAsLength + spaceLength + loggedNameLength + spaceLength + loggedOpenDeveloperLength + loggedDeveloperLength + 2, (int) y, GRAY);
        }

        int watermarkPreviousWidth = 0;
        drawStringWithShadow(matrices, textRenderer, watermarkName, width - watermarkFullLength - 2, (int) y, MatHax.INSTANCE.MATHAX_COLOR_INT);
        watermarkPreviousWidth += watermarkNameLength;
        drawStringWithShadow(matrices, textRenderer, space, width - watermarkFullLength + watermarkPreviousWidth - 2, (int) y, WHITE);
        watermarkPreviousWidth += spaceLength;
        drawStringWithShadow(matrices, textRenderer, watermarkVersion, width - watermarkFullLength + watermarkPreviousWidth - 2, (int) y, GRAY);

        int authorPreviousWidth = 0;
        drawStringWithShadow(matrices, textRenderer, authorBy, width - authorFullLength - 2, (int) y2, GRAY);
        authorPreviousWidth += authorByLength;
        drawStringWithShadow(matrices, textRenderer, space, width - authorFullLength + authorPreviousWidth - 2, (int) y2, GRAY);
        authorPreviousWidth += spaceLength;
        drawStringWithShadow(matrices, textRenderer, authorName, width - authorFullLength + authorPreviousWidth - 2, (int) y2, WHITE);

        drawStringWithShadow(matrices, textRenderer, proxyUsing, 2, (int) y2, GRAY);
        if (proxyDetails != null) drawStringWithShadow(matrices, textRenderer, proxyDetails, 2 + proxyUsingLength, (int) y2, WHITE);
    }
}
