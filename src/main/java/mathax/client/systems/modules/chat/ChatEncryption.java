package mathax.client.systems.modules.chat;

import mathax.client.MatHax;
import mathax.client.eventbus.EventHandler;
import mathax.client.events.game.ReceiveMessageEvent;
import mathax.client.events.game.SendMessageEvent;
import mathax.client.mixin.ChatHudAccessor;
import mathax.client.settings.BoolSetting;
import mathax.client.settings.Setting;
import mathax.client.settings.SettingGroup;
import mathax.client.settings.StringSetting;
import mathax.client.systems.modules.Categories;
import mathax.client.systems.modules.Module;
import mathax.client.systems.modules.Modules;
import mathax.client.utils.base91.Base91;
import net.minecraft.item.Items;
import net.minecraft.text.BaseText;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/*/-----------------/*/
/*/ Made by NobreHD /*/
/*/-----------------/*/

public class ChatEncryption extends Module {
    private static final String password = "MatHaxEncryption";
    public final String encryptedPrefix = "Ø";
    private String actualPassword = "";

    private static SecretKeySpec secretKey;

    private static byte[] key;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

    public final Setting<Boolean> encryptAll = sgGeneral.add(new BoolSetting.Builder()
        .name("encrypt-all")
        .description("Encrypts all sent messages.")
        .defaultValue(false)
        .build()
    );

    public final Setting<String> prefix = sgGeneral.add(new StringSetting.Builder()
        .name("prefix")
        .description("The prefix determining which messages will get encrypted.")
        .defaultValue(";")
        .visible(() -> !encryptAll.get())
        .build()
    );

    private final Setting<String> suffix = sgGeneral.add(new StringSetting.Builder()
        .name("suffix")
        .description("The prefix determining how will encrypted messages end.")
        .defaultValue("<3")
        .build()
    );

    private final Setting<Boolean> customKey = sgGeneral.add(new BoolSetting.Builder()
        .name("custom-key")
        .description("Allow you to use a custom key.")
        .defaultValue(true)
        .build()
    );

    private final Setting<String> groupKey = sgGeneral.add(new StringSetting.Builder()
        .name("key")
        .description("The key to use.")
        .defaultValue("MatHaxEncryption")
        .visible(customKey::get)
        .build()
    );

    public ChatEncryption(){
        super(Categories.Chat, Items.BARRIER, "chat-encryption", "Encrypts your chat messages.");
    }

    @EventHandler
    private void onMessageReceive(ReceiveMessageEvent event) {
        ((ChatHudAccessor) mc.inGameHud.getChatHud()).getVisibleMessages().removeIf((message) -> message.getId() == event.id && event.id != 0);
        ((ChatHudAccessor) mc.inGameHud.getChatHud()).getMessages().removeIf((message) -> message.getId() == event.id && event.id != 0);

        Text message = event.getMessage();

        if (message.getString().endsWith(suffix.get()) && !suffix.get().isEmpty()) {
            String[] msg = message.getString().split(encryptedPrefix);

            try {
                String chat = decrypt(msg[1], customKey.get() ? groupKey.get() : password);

                BaseText prefixOpenBorder = new LiteralText("[");
                prefixOpenBorder.setStyle(prefixOpenBorder.getStyle().withFormatting(Formatting.GRAY));

                BaseText prefix = new LiteralText("Encrypted Chat");
                prefix.setStyle(prefix.getStyle().withColor(MatHax.INSTANCE.MATHAX_COLOR.getPacked()));

                BaseText prefixCloseBorder = new LiteralText("] ");
                prefixCloseBorder.setStyle(prefixCloseBorder.getStyle().withFormatting(Formatting.GRAY));

                BaseText chatText = new LiteralText(msg[0] + chat);
                chatText.setStyle(chatText.getStyle().withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText(msg[1]))));

                BaseText chatMessage = new LiteralText("");
                if (Modules.get().get(BetterChat.class).displayPlayerHeads()) chatMessage.append("  ");
                chatMessage.append(prefixOpenBorder);
                chatMessage.append(prefix);
                chatMessage.append(prefixCloseBorder);
                chatMessage.append(chatText);

                message = chatMessage;
            } catch (Exception exception) {
                message = event.getMessage();
            }
        }

        event.setMessage(message);
    }

    @EventHandler
    private void onMessageSend(SendMessageEvent event) {
        String message = event.message;

        if (suffix.get().isEmpty()) {
            error("Suffix is empty, not sending!");
            event.message = null;
            event.setCancelled(true);
        } else if (encryptAll.get() || message.startsWith(prefix.get())) {
            if (!encryptAll.get()) message = message.substring(prefix.get().length());

            message = encryptedPrefix + encrypt(message, (customKey.get() ? groupKey.get() : password));

            if (message.length() > 256) {
                error("Message is too long, not sending!");
                event.message = null;
                event.setCancelled(true);
            }
        }

        event.message = message;
    }

    public void setKey(String myKey) {
        try {
            if (actualPassword.isEmpty()) actualPassword = password;
            else if (actualPassword.equals(myKey)) return;
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            key = Arrays.copyOf(sha.digest(myKey.getBytes(StandardCharsets.UTF_8)), 16);
            secretKey = new SecretKeySpec(key, "AES");
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public String encrypt(String strToEncrypt, String secret) {
        try {
            setKey(secret);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base91.encodeToString(cipher.doFinal(compress(strToEncrypt.getBytes(StandardCharsets.UTF_8)))) + suffix.get();
        } catch (Exception exception) {
            error("Error while encrypting: " + exception);
        }

        return null;
    }

    public String decrypt(String toDecrypt, String secret) throws Exception {
        setKey(secret);
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return new String(decompress(cipher.doFinal(Base91.decode(toDecrypt.substring(0, toDecrypt.length() - suffix.get().length())))), StandardCharsets.UTF_8);
    }

    public static byte[] compress(byte[] in) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DeflaterOutputStream deflater = new DeflaterOutputStream(out);
            deflater.write(in);
            deflater.flush();
            deflater.close();

            return out.toByteArray();
        } catch (Exception exception) {
            exception.printStackTrace();
            System.exit(150);
            return null;
        }
    }

    public static byte[] decompress(byte[] in) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InflaterOutputStream inflater = new InflaterOutputStream(out);
            inflater.write(in);
            inflater.flush();
            inflater.close();

            return out.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(150);
            return null;
        }
    }
}
