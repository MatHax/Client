package mathax.client.utils.player;

import mathax.client.mixininterface.IClientPlayerInteractionManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

import java.util.ArrayList;
import java.util.function.Predicate;

import static mathax.client.MatHax.mc;

public class InvUtils {
    private static final Action ACTION = new Action();

    public static int previousSlot = -1;

    // Update Slot

    public static void updateSlot(int newSlot) {
        mc.player.getInventory().selectedSlot = newSlot;
    }

    // Finding items

    public static FindItemResult findEmpty() {
        return find(ItemStack::isEmpty);
    }

    public static int findItemInHotbar(final Item item) {
        int index = -1;
        for (int i = 0; i < 9; ++i) {
            if (mc.player.getInventory().getStack(i).getItem() == item) {
                index = i;
                break;
            }
        }

        return index;
    }

    public static int findBlockInHotbar(final Block block) {
        return findItemInHotbar(new ItemStack(block).getItem());
    }

    public static FindItemResult findInHotbar(Item... items) {
        return findInHotbar(itemStack -> {
            for (Item item : items) {
                if (itemStack.getItem() == item) return true;
            }

            return false;
        });
    }

    public static FindItemResult findInHotbar(Predicate<ItemStack> itemStackPredicate) {
        if (itemStackPredicate.test(mc.player.getOffHandStack())) return new FindItemResult(SlotUtils.OFFHAND, mc.player.getOffHandStack().getCount());

        if (itemStackPredicate.test(mc.player.getMainHandStack())) return new FindItemResult(mc.player.getInventory().selectedSlot, mc.player.getMainHandStack().getCount());

        return find(itemStackPredicate, 0, 8);
    }

    public static FindItemResult find(Item... items) {
        return find(itemStack -> {
            for (Item item : items) {
                if (itemStack.getItem() == item) return true;
            }

            return false;
        });
    }

    public static FindItemResult find(Predicate<ItemStack> itemStackPredicate) {
        return find(itemStackPredicate, 0, mc.player.getInventory().size());
    }

    public static FindItemResult find(Predicate<ItemStack> itemStackPredicate, int start, int end) {
        int slot = -1, count = 0;

        for (int i = start; i <= end; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);

            if (itemStackPredicate.test(stack)) {
                if (slot == -1) slot = i;
                count += stack.getCount();
            }
        }

        return new FindItemResult(slot, count);
    }

    public static FindItemResult findFastestTool(BlockState state) {
        float bestScore = -1;
        int slot = -1;

        for (int i = 0; i < 9; i++) {
            float score = mc.player.getInventory().getStack(i).getMiningSpeedMultiplier(state);
            if (score > bestScore) {
                bestScore = score;
                slot = i;
            }
        }

        return new FindItemResult(slot, 1);
    }

    public static Integer getEmptySlots() {
        int emptySlots = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);
            if (itemStack == null || itemStack.getItem() instanceof AirBlockItem) emptySlots++;
        }

        return emptySlots;
    }

    public static boolean isInventoryFull() {
        for (int i = 0; i < 36; i++) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);
            if (itemStack == null || itemStack.getItem() instanceof AirBlockItem) return false;
        }

        return true;
    }

    public static FindItemResult findEgap() {
        return InvUtils.findInHotbar(Items.ENCHANTED_GOLDEN_APPLE);
    }

    public static FindItemResult findSword() {
        return InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof SwordItem);
    }

    public static FindItemResult findPick() {
        return InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof PickaxeItem);
    }

    public static FindItemResult findObsidian() {
        return InvUtils.findInHotbar(Blocks.OBSIDIAN.asItem());
    }

    public static FindItemResult findCraftTable() {
        return InvUtils.findInHotbar(Blocks.CRAFTING_TABLE.asItem());
    }

    // Interactions

    public static Item getItemFromSlot(Integer slot) {
        if (slot == -1) return null;
        if (slot == 45) return mc.player.getOffHandStack().getItem();
        return mc.player.getInventory().getStack(slot).getItem();
    }

    public static void windowClickSwap(int slot, int swapWith) {
        clickSlot(0, slot, swapWith, SlotActionType.SWAP, mc.player);
    }

    public static void clickSlot(int syncId, int slotId, int clickData, SlotActionType actionType, PlayerEntity player) {}

    public static boolean swap(int slot) {
        return swap(slot, false);
    }

    public static boolean swap(int slot, boolean swapBack) {
        if (slot < 0 || slot > 8) return false;
        if (swapBack && previousSlot == -1) previousSlot = mc.player.getInventory().selectedSlot;

        mc.player.getInventory().selectedSlot = slot;
        ((IClientPlayerInteractionManager) mc.interactionManager).syncSelected();
        return true;
    }

    public static void swap2(int slot) {
        if (slot != mc.player.getInventory().selectedSlot && slot >= 0 && slot < 9) mc.player.getInventory().selectedSlot = slot;
    }

    public static boolean swapBack() {
        if (previousSlot == -1) return false;

        boolean return_ = swap(previousSlot, false);
        previousSlot = -1;
        return return_;
    }

    public static Action move() {
        ACTION.type = SlotActionType.PICKUP;
        ACTION.two = true;
        return ACTION;
    }

    public static Action click() {
        ACTION.type = SlotActionType.PICKUP;
        return ACTION;
    }

    public static Action quickMove() {
        ACTION.type = SlotActionType.QUICK_MOVE;
        return ACTION;
    }

    public static Action drop() {
        ACTION.type = SlotActionType.THROW;
        ACTION.data = 1;
        return ACTION;
    }

    public static void dropHand() {
        if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, ScreenHandler.EMPTY_SPACE_SLOT_INDEX, 0, SlotActionType.PICKUP, mc.player);
    }

    public static class Action {
        private SlotActionType type = null;
        private boolean two = false;
        private int from = -1;
        private int to = -1;
        private int data = 0;

        private boolean isRecursive = false;

        private Action() {}

        // From

        public Action fromId(int id) {
            from = id;
            return this;
        }

        public Action from(int index) {
            return fromId(SlotUtils.indexToId(index));
        }

        public Action fromHotbar(int i) {
            return from(SlotUtils.HOTBAR_START + i);
        }

        public Action fromOffhand() {
            return from(SlotUtils.OFFHAND);
        }

        public Action fromMain(int i) {
            return from(SlotUtils.MAIN_START + i);
        }

        public Action fromArmor(int i) {
            return from(SlotUtils.ARMOR_START + (3 - i));
        }

        // To

        public void toId(int id) {
            to = id;
            run();
        }

        public void to(int index) {
            toId(SlotUtils.indexToId(index));
        }

        public void toHotbar(int i) {
            to(SlotUtils.HOTBAR_START + i);
        }

        public void toOffhand() {
            to(SlotUtils.OFFHAND);
        }

        public void toMain(int i) {
            to(SlotUtils.MAIN_START + i);
        }

        public void toArmor(int i) {
            to(SlotUtils.ARMOR_START + (3 - i));
        }

        // Slot

        public void slotId(int id) {
            from = to = id;
            run();
        }

        public void slot(int index) {
            slotId(SlotUtils.indexToId(index));
        }

        public void slotHotbar(int i) {
            slot(SlotUtils.HOTBAR_START + i);
        }

        public void slotOffhand() {
            slot(SlotUtils.OFFHAND);
        }

        public void slotMain(int i) {
            slot(SlotUtils.MAIN_START + i);
        }

        public void slotArmor(int i) {
            slot(SlotUtils.ARMOR_START + (3 - i));
        }

        // Other

        private void run() {
            boolean hadEmptyCursor = mc.player.currentScreenHandler.getCursorStack().isEmpty();

            if (type != null && from != -1 && to != -1) {
                click(from);
                if (two) click(to);
            }

            SlotActionType preType = type;
            boolean preTwo = two;
            int preFrom = from;
            int preTo = to;

            type = null;
            two = false;
            from = -1;
            to = -1;
            data = 0;

            if (!isRecursive && hadEmptyCursor && preType == SlotActionType.PICKUP && preTwo && (preFrom != -1 && preTo != -1) && !mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                isRecursive = true;
                InvUtils.click().slotId(preFrom);
                isRecursive = false;
            }
        }

        private void click(int id) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, id, data, type, mc.player);
        }
    }

    // Auto Totem

    private static void clickSlot(int id, int button, SlotActionType action) {
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, id, button, action, mc.player);
    }

    public static void clickId(int id) {
        clickSlot(id, 0, SlotActionType.PICKUP);
    }

    public static void swap(int id, int button) {
        clickSlot(id, button, SlotActionType.SWAP);
    }

    public static int getFirstHotbarSlotId() {
        if (mc.player.currentScreenHandler instanceof PlayerScreenHandler) return 36;
        return mc.player.currentScreenHandler.slots.size() - 9;
    }

    // Lists

    public static ArrayList<Item> wools = new ArrayList<>() {{
        add(Items.WHITE_WOOL);
        add(Items.ORANGE_WOOL);
        add(Items.MAGENTA_WOOL);
        add(Items.LIGHT_BLUE_WOOL);
        add(Items.YELLOW_WOOL);
        add(Items.LIME_WOOL);
        add(Items.PINK_WOOL);
        add(Items.GRAY_WOOL);
        add(Items.LIGHT_GRAY_WOOL);
        add(Items.CYAN_WOOL);
        add(Items.PURPLE_WOOL);
        add(Items.BLUE_WOOL);
        add(Items.BROWN_WOOL);
        add(Items.GREEN_WOOL);
        add(Items.RED_WOOL);
        add(Items.BLACK_WOOL);
    }};

    public static ArrayList<Item> planks = new ArrayList<>() {{
        add(Items.OAK_PLANKS);
        add(Items.SPRUCE_PLANKS);
        add(Items.BIRCH_PLANKS);
        add(Items.JUNGLE_PLANKS);
        add(Items.ACACIA_PLANKS);
        add(Items.DARK_OAK_PLANKS);
    }};
}
