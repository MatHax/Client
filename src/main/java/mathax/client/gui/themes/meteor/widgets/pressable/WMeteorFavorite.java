package mathax.client.gui.themes.meteor.widgets.pressable;

import mathax.client.gui.themes.meteor.MeteorWidget;
import mathax.client.gui.widgets.pressable.WFavorite;
import mathax.client.utils.render.color.Color;

public class WMeteorFavorite extends WFavorite implements MeteorWidget {
    public WMeteorFavorite(boolean checked) {
        super(checked);
    }

    @Override
    protected Color getColor() {
        return theme().favoriteColor.get();
    }
}
