package mathax.client.utils.misc.text;

import mathax.client.utils.render.color.Color;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.*;

public class TextUtils {
    public static List<ColoredText> toColoredTextList(Text text) {
        Stack<ColoredText> stack = new Stack<>();
        List<ColoredText> coloredTexts = new ArrayList<>();
        preOrderTraverse(text, stack, coloredTexts);
        coloredTexts.removeIf(e -> e.getText().equals(""));
        return coloredTexts;
    }

    public static Color getMostPopularColor(Text text) {
        Comparator<Integer> integerComparator = Comparator.naturalOrder();
        Optional<Map.Entry<Color, Integer>> optionalColor = getColoredCharacterCount(toColoredTextList(text))
                .entrySet().stream()
                .max((a, b) -> integerComparator.compare(a.getValue(), b.getValue()));

        return optionalColor.map(Map.Entry::getKey).orElse(new Color(255, 255, 255));
    }

    public static Map<Color, Integer> getColoredCharacterCount(List<ColoredText> coloredTexts) {
        Map<Color, Integer> colorCount = new HashMap<>();

        for (ColoredText coloredText : coloredTexts) {
            if (colorCount.containsKey(coloredText.getColor())) colorCount.put(coloredText.getColor(), colorCount.get(coloredText.getColor()) + coloredText.getText().length());
            else colorCount.put(coloredText.getColor(), coloredText.getText().length());
        }

        return colorCount;
    }

    private static void preOrderTraverse(Text text, Stack<ColoredText> stack, List<ColoredText> coloredTexts) {
        if (text == null) return;

        String textString = text.asString();

        TextColor mcTextColor = text.getStyle().getColor();

        Color textColor;
        if (mcTextColor == null) {
            if (stack.empty()) textColor = new Color(255, 255, 255);
            else textColor = stack.peek().getColor();
        } else textColor = new Color((text.getStyle().getColor().getRgb()) | 0xFF000000);

        ColoredText coloredText = new ColoredText(textString, textColor);
        coloredTexts.add(coloredText);
        stack.push(coloredText);

        for (Text child : text.getSiblings()) preOrderTraverse(child, stack, coloredTexts);

        stack.pop();
    }
}
