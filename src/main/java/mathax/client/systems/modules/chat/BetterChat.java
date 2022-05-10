package mathax.client.systems.modules.chat;

import it.unimi.dsi.fastutil.chars.Char2CharArrayMap;
import it.unimi.dsi.fastutil.chars.Char2CharMap;
import mathax.client.eventbus.EventHandler;
import mathax.client.events.game.ReceiveMessageEvent;
import mathax.client.events.game.SendMessageEvent;
import mathax.client.mixin.ChatHudAccessor;
import mathax.client.settings.*;
import mathax.client.systems.commands.Commands;
import mathax.client.systems.commands.commands.SayCommand;
import mathax.client.systems.modules.Categories;
import mathax.client.systems.modules.Module;
import mathax.client.systems.modules.Modules;
import mathax.client.utils.Utils;
import mathax.client.utils.misc.ChatUtils;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.item.Items;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class BetterChat extends Module {
    private final Char2CharMap FULL_WIDTH = new Char2CharArrayMap(); {
        String[] chars = "aábcčdďeéěfghchiíjklmnňoópqrřsštťuúůvwxyýzžAÁBCČDĎEÉĚFGHCHIÍJKLMNŇOÓPQRŘSŠTŤUÚŮVWXYÝZŽ0123456789|[]!?.,{}()\"'".split("");
        String[] fontchars = "ａáｂｃčｄďｅéěｆｇｈｃｈｉíｊｋｌｍｎňｏóｐｑｒřｓšｔťｕúůｖｗｘｙýｚžＡÁＢＣČＤĎＥÉĚＦＧＨＣＨＩÍＪＫＬＭＮŇＯÓＰＱＲŘＳŠＴŤＵÚŮＶＷＸＹÝＺŽ０１２３４５６７８９｜［］！？．，｛｝（）\"＇".split("");
        for (int i = 0; i < chars.length; i++) FULL_WIDTH.put(chars[i].charAt(0), fontchars[i].charAt(0));
    }

    private final Char2CharMap SMALL_CAPS = new Char2CharArrayMap(); {
        String[] chars = "abcdefghchijklmnopqrstuvwxyzABCDEFGHCHIJKLMNOPQRSTUVWXYZ0123456789|[]!?.,{}()\"'".split("");
        String[] fontchars = "ᴀʙᴄᴅᴇꜰɢʜᴄʜɪᴊᴋʟᴍɴᴏᴘqʀꜱᴛᴜᴠᴡxʏᴢᴀʙᴄᴅᴇꜰɢʜᴄʜɪᴊᴋʟᴍɴᴏᴩQʀꜱᴛᴜᴠᴡxYᴢ0123456789｜[]!?.,{}()\"'".split("");
        for (int i = 0; i < chars.length; i++) SMALL_CAPS.put(chars[i].charAt(0), fontchars[i].charAt(0));
    }

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
    private final SimpleDateFormat timeFormatSeconds = new SimpleDateFormat("HH:mm:ss");

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgFilter = settings.createGroup("Filter");
    private final SettingGroup sgLongerChat = settings.createGroup("Longer Chat");
    private final SettingGroup sgFancyChat = settings.createGroup("Fancy Chat");
    private final SettingGroup sgPrefix = settings.createGroup("Prefix");
    private final SettingGroup sgSuffix = settings.createGroup("Suffix");

    // General

    private final Setting<Boolean> timestamps = sgGeneral.add(new BoolSetting.Builder()
        .name("timestamps")
        .description("Adds client side time stamps to the beginning of chat messages.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> timestampsSeconds = sgGeneral.add(new BoolSetting.Builder()
        .name("seconds")
        .description("Adds seconds to the timestamps.")
        .defaultValue(true)
        .visible(timestamps::get)
        .build()
    );

    private final Setting<Boolean> playerHeads = sgGeneral.add(new BoolSetting.Builder()
        .name("player-heads")
        .description("Displays player heads next to their messages.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> coordsProtection = sgGeneral.add(new BoolSetting.Builder()
        .name("coords-protection")
        .description("Prevents you from sending messages in chat that may contain coordinates.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> emojiFix = sgGeneral.add(new BoolSetting.Builder()
        .name("emoji-fix")
        .description("Fixes Minecraft emojis.")
        .defaultValue(true)
        .build()
    );

    // Fancy Chat

    public final Setting<FancyType> fancy = sgFancyChat.add(new EnumSetting.Builder<FancyType>()
        .name("fancy")
        .description("Determines what font or style to use in the your messages.")
        .defaultValue(FancyType.None)
        .build()
    );

    public final Setting<Boolean> annoy = sgFancyChat.add(new BoolSetting.Builder()
        .name("annoy")
        .description("Makes your messages aNnOyInG.")
        .defaultValue(false)
        .build()
    );

    // Filter

    private final Setting<Boolean> antiSpam = sgFilter.add(new BoolSetting.Builder()
        .name("anti-spam")
        .description("Blocks duplicate messages from filling your chat.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> antiSpamDepth = sgFilter.add(new IntSetting.Builder()
        .name("depth")
        .description("How many messages to filter.")
        .defaultValue(20)
        .min(1)
        .sliderMin(1)
        .visible(antiSpam::get)
        .build()
    );

    private final Setting<Boolean> filterRegex = sgFilter.add(new BoolSetting.Builder()
        .name("filter-regex")
        .description("Filter out chat messages that match the regex filter.")
        .defaultValue(false)
        .build()
    );

    private final Setting<List<String>> regexFilters = sgFilter.add(new StringListSetting.Builder()
        .name("regex-filter")
        .description("Regex filter used for filtering chat messages.")
        .visible(filterRegex::get)
        .build()
    );


    // Longer chat

    private final Setting<Boolean> infiniteChatBox = sgLongerChat.add(new BoolSetting.Builder()
        .name("infinite-chat-box")
        .description("Lets you type infinitely long messages.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> longerChatHistory = sgLongerChat.add(new BoolSetting.Builder()
        .name("longer-chat-history")
        .description("Extends chat length.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> longerChatLines = sgLongerChat.add(new IntSetting.Builder()
        .name("extra-lines")
        .description("The amount of extra chat lines.")
        .defaultValue(1000)
        .min(100)
        .sliderRange(100, 1000)
        .visible(longerChatHistory::get)
        .build()
    );

    // Prefix

    public final Setting<Boolean> prefix = sgPrefix.add(new BoolSetting.Builder()
        .name("enabled")
        .description("Adds a prefix to your chat messages.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> prefixRandom = sgPrefix.add(new BoolSetting.Builder()
        .name("random")
        .description("Uses a random number as your prefix.")
        .defaultValue(false)
        .build()
    );

    public final Setting<String> prefixText = sgPrefix.add(new StringSetting.Builder()
        .name("text")
        .description("The text to add as your prefix.")
        .defaultValue("> ")
        .visible(() -> !prefixRandom.get())
        .build()
    );

    public final Setting<Fonts> prefixFont = sgPrefix.add(new EnumSetting.Builder<Fonts>()
        .name("font")
        .description("Determines what font to use in the prefix.")
        .defaultValue(Fonts.Full_Width)
        .build()
    );

    // Suffix

    public final Setting<Boolean> suffix = sgSuffix.add(new BoolSetting.Builder()
        .name("enabled")
        .description("Adds a suffix to your chat messages.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> suffixRandom = sgSuffix.add(new BoolSetting.Builder()
        .name("random")
        .description("Uses a random number as your suffix.")
        .defaultValue(false)
        .build()
    );

    public final Setting<String> suffixText = sgSuffix.add(new StringSetting.Builder()
        .name("text")
        .description("The text to add as your suffix.")
        .defaultValue(" | MatHax")
        .visible(() -> !suffixRandom.get())
        .build()
    );

    public final Setting<Fonts> suffixFont = sgSuffix.add(new EnumSetting.Builder<Fonts>()
        .name("font")
        .description("Determines what font to use in the suffix.")
        .defaultValue(Fonts.Full_Width)
        .build()
    );

    public BetterChat() {
        super(Categories.Chat, Items.DROPPER, "better-chat", "Improves your chat experience in various ways.");
    }

    @EventHandler
    public void onMessageReceive(ReceiveMessageEvent event) {
        ((ChatHudAccessor) mc.inGameHud.getChatHud()).getVisibleMessages().removeIf((message) -> message.getId() == event.id && event.id != 0);
        ((ChatHudAccessor) mc.inGameHud.getChatHud()).getMessages().removeIf((message) -> message.getId() == event.id && event.id != 0);

        Text message = event.getMessage();

        ChatEncryption chatEncryption = Modules.get().get(ChatEncryption.class);
        if (chatEncryption.isActive() && (chatEncryption.encryptAll.get() || message.getString().contains(chatEncryption.prefix.get()))) return;

        if (filterRegex.get()) {
            for (int i = 0; i < regexFilters.get().size(); i++) {
                Pattern p;
                try {
                    p = Pattern.compile(regexFilters.get().get(i));
                } catch (PatternSyntaxException e) {
                    String removed = regexFilters.get().remove(i);
                    error("Removing Invalid regex: %s", removed);
                    continue;
                }

                if (p.matcher(message.getString()).find()) {
                    event.cancel();
                    return;
                }
            }
        }

        if (timestamps.get()) {
            Matcher matcher = Pattern.compile("^(<[0-9]{2}:[0-9]{2}>\\s)").matcher(message.getString());
            if (matcher.matches()) message.getSiblings().subList(0, 8).clear();

            String time;
            if (timestampsSeconds.get()) time = timeFormatSeconds.format(new Date());
            else time = timeFormat.format(new Date());

            Text timestamp = new LiteralText("<" + time + "> ").formatted(Formatting.GRAY);

            message = new LiteralText("").append(timestamp).append(message);
        }

        if (playerHeads.get()) message = new LiteralText("  ").append(message);

        for (int i = 0; i < antiSpamDepth.get(); i++) {
            if (antiSpam.get()) {
                Text antiSpammed = appendAntiSpam(message, i);

                if (antiSpammed != null) {
                    message = antiSpammed;
                    ((ChatHudAccessor) mc.inGameHud.getChatHud()).getMessages().remove(i);
                    ((ChatHudAccessor) mc.inGameHud.getChatHud()).getVisibleMessages().remove(i);
                }
            }
        }

        event.setMessage(message);
    }

    private Text appendAntiSpam(Text text, int index) {
        List<ChatHudLine<OrderedText>> visibleMessages = ((ChatHudAccessor) mc.inGameHud.getChatHud()).getVisibleMessages();
        if (visibleMessages.isEmpty() || index < 0 || index > visibleMessages.size() - 1) return null;

        ChatHudLine<OrderedText> visibleMessage = visibleMessages.get(index);

        LiteralText parsed = new LiteralText("");

        visibleMessage.getText().accept((i, style, codePoint) -> {
            parsed.append(new LiteralText(new String(Character.toChars(codePoint))).setStyle(style));
            return true;
        });

        String oldMessage = parsed.getString();
        String newMessage = text.getString();

        if (oldMessage.equals(newMessage)) return parsed.append(new LiteralText(" (2)").formatted(Formatting.GRAY));
        else {
            Matcher matcher = Pattern.compile(".*(\\([0-9]+\\)$)").matcher(oldMessage);

            if (!matcher.matches()) return null;

            String group = matcher.group(matcher.groupCount());
            int number = Integer.parseInt(group.substring(1, group.length() - 1));

            String counter = " (" + number + ")";

            if (oldMessage.substring(0, oldMessage.length() - counter.length()).equals(newMessage)) {
                for (int i = 0; i < counter.length(); i++) parsed.getSiblings().remove(parsed.getSiblings().size() - 1);
                return parsed.append(new LiteralText( " (" + (number + 1) + ")").formatted(Formatting.GRAY));
            }
        }

        return null;
    }

    @EventHandler
    public void onMessageSend(SendMessageEvent event) {
        String message = event.message;

        if (coordsProtection.get() && containsCoordinates(message)) {
            BaseText warningMessage = new LiteralText(Formatting.GRAY + "It looks like there are coordinates in your message! ");

            BaseText sendButton = getSendButton(message);
            warningMessage.append(sendButton);

            ChatUtils.sendMsg(warningMessage);

            event.cancel();
            return;
        }

        ChatEncryption chatEncryption = Modules.get().get(ChatEncryption.class);
        if (chatEncryption.isActive() && (chatEncryption.encryptAll.get() || message.startsWith(chatEncryption.prefix.get()))) return;

        if (emojiFix.get()) {
            message = applyEmojiFix(message);
            event.message = message;
        }

        if (annoy.get()) message = applyAnnoy(message);

        switch (fancy.get()) {
            case Full_Width -> message = applyFull(message);
            case Small_CAPS -> message = applySmall(message);
            case UwU -> message = applyUwU(message);
            case Leet -> message = applyLeet(message);
        }

        if (prefix.get()) message = getAffix(prefixText.get(), prefixFont.get(), prefixRandom.get()) + message;

        if (suffix.get()) message = message + getAffix(suffixText.get(), suffixFont.get(), suffixRandom.get());

        event.message = message;
    }

    public String applyAnnoy(String message) {
        StringBuilder sb = new StringBuilder(message.length());
        boolean upperCase = true;

        for (int cp : message.codePoints().toArray()) {
            if (upperCase) sb.appendCodePoint(Character.toUpperCase(cp));
            else sb.appendCodePoint(Character.toLowerCase(cp));
            upperCase = !upperCase;
        }

        message = sb.toString();
        return message;
    }

    public String applyFull(String message) {
        StringBuilder sb = new StringBuilder();

        for (char ch : message.toCharArray()) {
            if (FULL_WIDTH.containsKey(ch)) sb.append(FULL_WIDTH.get(ch));
            else sb.append(ch);
        }

        return sb.toString();
    }

    public String applySmall(String message) {
        StringBuilder sb = new StringBuilder();

        for (char ch : message.toCharArray()) {
            if (SMALL_CAPS.containsKey(ch)) sb.append(SMALL_CAPS.get(ch));
            else sb.append(ch);
        }

        return sb.toString();
    }

    public String applyUwU(String message) {
        message = message.replace("ove", "uv");
        message = message.replace("the", "da");
        message = message.replace("is", "ish");
        message = message.replace("r", "w");
        message = message.replace("ve", "v");
        message = message.replace("OVE", "UV");
        message = message.replace("THE", "DA");
        message = message.replace("IS", "ISH");
        message = message.replace("R", "W");
        message = message.replace("L", "W");
        return message.replace("l", "w");
    }

    public String applyLeet(String message) {
        message = message.replace("a", "4");
        message = message.replace("e", "3");
        message = message.replace("g", "6");
        message = message.replace("l" , "1");
        message = message.replace("i" , "1");
        message = message.replace("o", "0");
        message = message.replace("s", "$");
        message = message.replace("A", "4");
        message = message.replace("E", "3");
        message = message.replace("G", "6");
        message = message.replace("L" , "1");
        message = message.replace("I" , "1");
        message = message.replace("O", "0");
        message = message.replace("S", "$");
        message = message.replace("T", "7");
        return message.replace("t", "7");
    }

    public String applyEmojiFix(String msg) {
        if (msg.contains("😄")) msg = msg.replace("😄", "☺");
        if (msg.contains(":sad:")) msg = msg.replace(":sad:", "☹");
        if (msg.contains("❤️")) msg = msg.replace("❤️", "❤");
        if (msg.contains("💀")) msg = msg.replace("💀", "☠");
        if (msg.contains("⭐")) msg = msg.replace("⭐", "★");
        if (msg.contains(":flower:")) msg = msg.replace(":flower:", "❀");
        if (msg.contains(":lightning:")) msg = msg.replace(":lightning:", "⚡");
        return msg;
    }

    public String getAffix(String text, Fonts font, boolean random) {
        if (random) return String.format("(%03d) ", Utils.random(0, 1000));

        switch (font) {
            case Full_Width -> {
                return applyFull(text);
            }
            case Small_CAPS -> {
                return applySmall(text);
            }
        }

        return text;
    }

    public boolean containsCoordinates(String message) {
        return message.matches(".*(?<x>-?\\d{3,}(?:\\.\\d*)?)(?:\\s+(?<y>\\d{1,3}(?:\\.\\d*)?))?\\s+(?<z>-?\\d{3,}(?:\\.\\d*)?).*");
    }

    public BaseText getSendButton(String message) {
        BaseText sendButton = new LiteralText("[SEND ANYWAY]");
        BaseText hintBaseText = new LiteralText("");

        BaseText hintMsg = new LiteralText("Send your message to the global chat even if there are coordinates:");
        hintMsg.setStyle(hintBaseText.getStyle().withFormatting(Formatting.GRAY));
        hintBaseText.append(hintMsg);
        hintBaseText.append(new LiteralText('\n' + message));

        sendButton.setStyle(sendButton.getStyle().withFormatting(Formatting.DARK_RED).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, Commands.get().get(SayCommand.class).toString(message))).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hintBaseText)));

        return sendButton;
    }

    public boolean isInfiniteChatBox() {
        return isActive() && infiniteChatBox.get();
    }

    public boolean isLongerChat() {
        return isActive() && longerChatHistory.get();
    }

    public boolean displayPlayerHeads() {
        return isActive() && playerHeads.get();
    }

    public int getChatLength() {
        return longerChatLines.get();
    }

    public enum FancyType {
        Full_Width("Full Width"),
        Small_CAPS("Small CAPS"),
        UwU("UwU"),
        Leet("Leet"),
        None("None");

        private final String title;

        FancyType(String title) {
            this.title = title;
        }


        @Override
        public String toString() {
            return title;
        }
    }

    public enum Fonts {
        Full_Width("Full Width"),
        Small_CAPS("Small CAPS"),
        None("None");

        private final String title;

        Fonts(String title) {
            this.title = title;
        }


        @Override
        public String toString() {
            return title;
        }
    }
}
