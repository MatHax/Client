package mathax.client.systems.hud.modules;

import com.mojang.blaze3d.systems.RenderSystem;
import mathax.client.MatHax;
import mathax.client.settings.*;
import mathax.client.utils.render.RenderUtils;
import mathax.client.utils.render.color.SettingColor;
import mathax.client.systems.hud.HUD;
import mathax.client.systems.hud.HudElement;
import mathax.client.systems.hud.HudRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class ArmorHud extends HudElement {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

    private final Setting<Boolean> flipOrder = sgGeneral.add(new BoolSetting.Builder()
        .name("flip-order")
        .description("Flips the order of armor items.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Orientation> orientation = sgGeneral.add(new EnumSetting.Builder<Orientation>()
        .name("orientation")
        .description("How to display armor.")
        .defaultValue(Orientation.Vertical)
        .build()
    );

    private final Setting<Durability> durability = sgGeneral.add(new EnumSetting.Builder<Durability>()
        .name("durability")
        .description("How to display armor durability.")
        .defaultValue(Durability.Percentage)
        .build()
    );

    private final Setting<SettingColor> textColor = sgGeneral.add(new ColorSetting.Builder()
        .name("text-color")
        .description("The color of durability")
        .defaultValue(new SettingColor(MatHax.INSTANCE.MATHAX_COLOR.r, MatHax.INSTANCE.MATHAX_COLOR.g, MatHax.INSTANCE.MATHAX_COLOR.b))
        .build()
    );

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("The scale.")
        .defaultValue(2.75)
        .min(1)
        .sliderRange(1, 5)
        .build()
    );

    public ArmorHud(HUD hud) {
        super(hud, "armor", "Displays information about your armor.", true);
    }

    @Override
    public void update(HudRenderer renderer) {
        switch (orientation.get()) {
            case Horizontal -> box.setSize(16 * scale.get() * 4 + 2 * 4, 16 * scale.get());
            case Vertical -> box.setSize(16 * scale.get(), 16 * scale.get() * 4 + 2 * 4);
        }
    }

    //TODO: IMPROVE
    @Override
    public void render(HudRenderer renderer) {
        double x = box.getX();
        double y = box.getY();
        double armorX = 0;
        double armorY = 0;

        MatrixStack matrices = RenderSystem.getModelViewStack();

        matrices.push();
        matrices.scale(scale.get().floatValue(), scale.get().floatValue(), 1);

        int slot = flipOrder.get() ? 3 : 0;
        for (int position = 0; position < 4; position++) {
            ItemStack itemStack = getItem(slot);

            switch (orientation.get()) {
                case Vertical -> {
                    armorX = x / scale.get();
                    armorY = y / scale.get() + position * 18;
                }
                case Horizontal -> {
                    armorX = x / scale.get() + position * 18;
                    armorY = y / scale.get();
                }
            }

            RenderUtils.drawItem(itemStack, (int) armorX, (int) armorY, (itemStack.isDamageable() && durability.get() == Durability.Bar));

            if (itemStack.isDamageable() && !isInEditor() && durability.get() != Durability.Bar && durability.get() != Durability.None) {
                String percentage = Integer.toString(Math.round(((itemStack.getMaxDamage() - itemStack.getDamage()) * 100f) / (float) itemStack.getMaxDamage()));
                String message = switch (durability.get()) {
                    case Total -> Integer.toString(itemStack.getMaxDamage() - itemStack.getDamage());
                    case Percentage -> percentage + "%";
                    default -> "err";
                };

                double messageWidth = renderer.textWidth(message);

                switch (orientation.get()) {
                    case Vertical -> {
                        armorX = x + 8 * scale.get() - messageWidth / 2.0;
                        armorY = y + (18 * position * scale.get()) + (18 * scale.get() - renderer.textHeight());
                    }
                    case Horizontal -> {
                        armorX = x + 18 * position * scale.get() + 8 * scale.get() - messageWidth / 2.0;
                        armorY = y + (box.height - renderer.textHeight());
                    }
                }

                renderer.text(message, armorX, armorY, textColor.get());
            }

            if (flipOrder.get()) slot--;
            else slot++;
        }

        matrices.pop();
    }

    private ItemStack getItem(int i) {
        if (isInEditor()) {
            return switch (i) {
                default -> Items.NETHERITE_BOOTS.getDefaultStack();
                case 1 -> Items.NETHERITE_LEGGINGS.getDefaultStack();
                case 2 -> Items.NETHERITE_CHESTPLATE.getDefaultStack();
                case 3 -> Items.NETHERITE_HELMET.getDefaultStack();
            };
        }

        return mc.player.getInventory().getArmorStack(i);
    }

    public enum Durability {
        Bar("Bar"),
        Total("Total"),
        Percentage("Percentage"),
        None("None");

        private final String title;

        Durability(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    public enum Orientation {
        Horizontal("Horizontal"),
        Vertical("Vertical");

        private final String title;

        Orientation(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }
}
