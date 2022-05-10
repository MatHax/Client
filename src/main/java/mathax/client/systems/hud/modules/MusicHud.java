package mathax.client.systems.hud.modules;


import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import mathax.client.MatHax;
import mathax.client.music.Music;
import mathax.client.renderer.Mesh;
import mathax.client.renderer.Renderer2D;
import mathax.client.renderer.text.TextRenderer;
import mathax.client.settings.*;
import mathax.client.utils.render.color.SettingColor;
import mathax.client.systems.hud.HUD;
import mathax.client.systems.hud.HudElement;
import mathax.client.systems.hud.HudRenderer;

public class MusicHud extends HudElement {
    private static String time = "00:00:00";
    private static final String notPlaying = "Not playing";
    private static final String noAuthor = "No author";

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgColors = settings.createGroup("Colors");
    private final SettingGroup sgSizes = settings.createGroup("Sizes");

    // General

    private final Setting<Boolean> showTitle = sgGeneral.add(new BoolSetting.Builder()
        .name("show-title")
        .description("Shows the title of the current song.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showAuthor = sgGeneral.add(new BoolSetting.Builder()
        .name("show-author")
        .description("Shows the author of the current song.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showRemainingTime = sgGeneral.add(new BoolSetting.Builder()
        .name("show-remaining-time")
        .description("Shows the total amount of time left.")
        .defaultValue(true)
        .build()
    );

    // Colors

    private final Setting<SettingColor> backgroundColor = sgColors.add(new ColorSetting.Builder()
        .name("background-color")
        .description("Color of background.")
        .defaultValue(new SettingColor(MatHax.INSTANCE.MATHAX_BACKGROUND_COLOR.r, MatHax.INSTANCE.MATHAX_BACKGROUND_COLOR.g, MatHax.INSTANCE.MATHAX_BACKGROUND_COLOR.b, 75))
        .build()
    );

    private final Setting<SettingColor> progressColor = sgColors.add(new ColorSetting.Builder()
        .name("progress-color")
        .description("Color of the progress bar.")
        .defaultValue(new SettingColor(MatHax.INSTANCE.MATHAX_COLOR.r, MatHax.INSTANCE.MATHAX_COLOR.g, MatHax.INSTANCE.MATHAX_COLOR.b))
        .build()
    );

    private final Setting<SettingColor> progressBackgroundColor = sgColors.add(new ColorSetting.Builder()
        .name("progress-background-color")
        .description("Color of the progress bars background.")
        .defaultValue(new SettingColor(35, 35, 35, 100))
        .build()
    );

    private final Setting<SettingColor> textColor = sgColors.add(new ColorSetting.Builder()
        .name("text-color")
        .description("Color of text and controls.")
        .defaultValue(new SettingColor(255, 255, 255))
        .build()
    );

    private final Setting<SettingColor> iconColor = sgColors.add(new ColorSetting.Builder()
        .name("icon-color")
        .description("Color of status icon.")
        .defaultValue(new SettingColor(255, 255, 255))
        .build()
    );

    // Sizes

    private final Setting<Double> size = sgSizes.add(new DoubleSetting.Builder()
        .name("size")
        .description("The size of this element.")
        .defaultValue(100)
        .min(20)
        .sliderRange(20, 200)
        .build()
    );

    private final Setting<Double> recordPart = sgSizes.add(new DoubleSetting.Builder()
        .name("record-part")
        .description("The relative amount of space the progress bar should consume.")
        .defaultValue(0.8)
        .min(0.1)
        .sliderRange(0.1, 1)
        .build()
    );

    private final Setting<Double> progressWidth = sgSizes.add(new DoubleSetting.Builder()
        .name("progress-width")
        .description("The relative amount of pixels to use in the actual progress part of the bar.")
        .defaultValue(5)
        .min(1)
        .sliderRange(1, 10)
        .build()
    );

    private final Setting<Double> recordStatusPart = sgSizes.add(new DoubleSetting.Builder()
        .name("record-status-part")
        .description("The relative amount of space the status component of the progress bar should consume.")
        .defaultValue(0.5)
        .min(0.1)
        .sliderRange(0.1, 1)
        .build()
    );

    private final Setting<Double> textScale = sgSizes.add(new DoubleSetting.Builder()
        .name("text-scale")
        .description("The relative amount of space the status component of the progress bar should consume.")
        .defaultValue(3)
        .min(1)
        .sliderRange(1, 5)
        .build()
    );

    public MusicHud(HUD hud) {
        super(hud, "music", "Displays the currently playing music.", true);
    }

    @Override
    public void update(HudRenderer renderer) {
        if (Music.player.getPlayingTrack() == null) time = "00:00:00";
        else time = Music.getTime();
    }

    @Override
    public void render(HudRenderer renderer) {
        if (Music.player != null) {
            double width = size.get();
            AudioTrack current = Music.player.getPlayingTrack();
            TextRenderer textRenderer = TextRenderer.get();
            textRenderer.end();
            textRenderer.begin(0.45 * textScale.get(), false, true);
            if (showTitle.get()) width = Math.max(width, size.get() + renderer.roundAmount() + textRenderer.getWidth(current == null ? notPlaying : current.getInfo().title));
            if (showAuthor.get()) width = Math.max(width, size.get() + renderer.roundAmount() + textRenderer.getWidth(current == null ? noAuthor : current.getInfo().author));
            if (showRemainingTime.get()) width = Math.max(width, size.get() + renderer.roundAmount() + textRenderer.getWidth(time));
            textRenderer.end();
            textRenderer.begin();
            box.setSize(width, size.get());
        }

        renderer.addPostTask(() -> {
            double x = box.getX();
            double y = box.getY();
            double w = box.width;
            double h = box.height;

            // Background
            Renderer2D.COLOR.begin();
            Renderer2D.COLOR.quadRounded(x, y, w, h, backgroundColor.get(), renderer.roundAmount(), true);

            AudioTrack current = Music.player.getPlayingTrack();
            double progress = current == null ? 1 : current.getPosition() * 1d / current.getDuration();
            String title = current == null ? notPlaying : current.getInfo().title.replace("&amp;", "&");
            String author = current == null ? noAuthor : current.getInfo().author.replace("&amp;", "&");

            // Song view
            double r = h * recordPart.get() / 2;
            double start = h / 2;
            Renderer2D.COLOR.circlePart(x + start, y + start, r, 0, Math.PI * 2, progressBackgroundColor.get());
            Renderer2D.COLOR.circlePartOutline(x + start, y + start, r, 0, (Math.PI * 2) * progress, progressColor.get(), progressWidth.get());

            double controlSizeHalf = r * recordStatusPart.get();
            if (Music.player.isPaused()) {
                double pauseStart = h / 2 - controlSizeHalf;
                double quadWidth = controlSizeHalf * 2 / 3;

                Renderer2D.COLOR.quad(x + pauseStart, y + pauseStart, quadWidth, controlSizeHalf * 2, iconColor.get());
                Renderer2D.COLOR.quad(x + h / 2 + controlSizeHalf - quadWidth, y + pauseStart, quadWidth, controlSizeHalf * 2, iconColor.get());
            } else {
                controlSizeHalf /= 2;
                double startX = h / 2 - controlSizeHalf;
                double startY = h / 2 - controlSizeHalf * 2;

                Mesh mesh = Renderer2D.COLOR.triangles;
                mesh.triangle(
                    mesh.vec2(x + startX, y + startY).color(iconColor.get()).next(),
                    mesh.vec2(x + startX, y + startY + controlSizeHalf * 4).color(iconColor.get()).next(),
                    mesh.vec2(x + startX + controlSizeHalf * 2, y + startY + controlSizeHalf * 2).color(iconColor.get()).next()
                );
            }

            Renderer2D.COLOR.render(null);

            TextRenderer textRenderer = TextRenderer.get();
            textRenderer.begin(0.45 * textScale.get(), false, true);

            // Title
            double titleY = textRenderer.getHeight() / (showAuthor.get() ? 1 : 2);
            if (showTitle.get()) textRenderer.render(title, x + h, y + h / 2 - titleY, textColor.get(), true);

            // Author
            double authorY = showTitle.get() ? 0 : textRenderer.getHeight() / 2;
            if (showAuthor.get()) textRenderer.render(author, x + h, y + h / 2 - authorY, textColor.get(), true);

            textRenderer.end();

            // The total duration in Hours, Minutes and Seconds
            textRenderer.begin(0.4 * textScale.get(), false, true);
            if (showRemainingTime.get()) textRenderer.render(time, x + h, y + h - textRenderer.getHeight(), textColor.get());
            textRenderer.end();
        });
    }
}
