package team.jackdaw.npcsystem.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Display.TextDisplay;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;


public class TextBubbleEntity extends TextDisplay {
    private static final byte SEE_THROUGH_FLAG = 0x02;
    private static final Method SET_TEXT = findTextDisplayMethod("setText", Component.class);
    private static final Method SET_TEXT_OPACITY = findTextDisplayMethod("setTextOpacity", byte.class);
    private static final Method SET_BACKGROUND_COLOR = findTextDisplayMethod("setBackgroundColor", int.class);
    private static final Method SET_FLAGS = findTextDisplayMethod("setFlags", byte.class);

    private final NPCEntity speaker;
    private final double heightOffset = 0.55D;
    private long lastUpdateTime;        // In milliseconds.
    private long timeLastingPerChar;    // In milliseconds.
    private long bubbleLastingTime;     // In milliseconds.
    private TextBackgroundColor textBackgroundColor;

    TextBubbleEntity(@NotNull NPCEntity speaker) {
        super(EntityType.TEXT_DISPLAY, speaker.level());
        this.speaker = speaker;
        this.setPos(speaker.getX(), speaker.getY() + speaker.getBbHeight() + heightOffset, speaker.getZ());
        this.lastUpdateTime = System.currentTimeMillis();
        this.timeLastingPerChar = 500L;
        this.textBackgroundColor = TextBackgroundColor.DEFAULT;
        this.bubbleLastingTime = 0;
        speaker.level().addFreshEntity(this);
    }

    @Override
    public void tick() {
        super.tick();
        this.setPos(speaker.getX(), speaker.getY() + speaker.getBbHeight() + heightOffset, speaker.getZ());
        updateSeeThrough();
        if (this.speaker.isRemoved() || !this.speaker.level().equals(this.level()) || System.currentTimeMillis() - lastUpdateTime > bubbleLastingTime) {
            this.remove(RemovalReason.DISCARDED);
        }
    }

    private void updateSeeThrough() {
        invoke(SET_FLAGS, isSeeThroughBlock() ? SEE_THROUGH_FLAG : (byte) 0);
    }

    void setTimeLastingPerChar(long timeLastingPerChar) {
        this.timeLastingPerChar = timeLastingPerChar;
    }

    void setTextBackgroundColor(TextBackgroundColor textBackgroundColor) {
        this.textBackgroundColor = textBackgroundColor;
    }

    void update(String message) {
        updateAllNbt(message);
        bubbleLastingTime = bubbleLastingTime(message);
        lastUpdateTime = System.currentTimeMillis();
    }

    private void updateAllNbt(String message) {
        invoke(SET_TEXT_OPACITY, (byte) -1);
        invoke(SET_TEXT, textBuilder(message, textBackgroundColor));
        invoke(SET_BACKGROUND_COLOR, (int) textBackgroundColor.getBackgroundARGBAsLong());
        updateSeeThrough();
    }

    private Component textBuilder(String message, TextBackgroundColor textBackgroundColor) {
        MutableComponent replyText = Component.literal(message).copy();
        Style textStyle = Style.EMPTY.withColor(textBackgroundColor.getTextRGBAsInt());
        replyText.setStyle(textStyle);
        return replyText;
    }

    private long bubbleLastingTime(String message){
        return message.length() * this.timeLastingPerChar;
    }

    private boolean isSeeThroughBlock() {
        int checkingRadius = 1;
        Level world = this.level();
        BlockPos pos = this.blockPosition();
        for (int x = -checkingRadius; x <= checkingRadius; x++) {
            for (int y = 0; y <= checkingRadius; y++) {
                for (int z = -checkingRadius; z <= checkingRadius; z++) {
                    if(world.getBlockState(pos.offset(x, y, z)).canOcclude()) return true;
                }
            }
        }
        return false;
    }

    private static Method findTextDisplayMethod(String name, Class<?>... parameterTypes) {
        try {
            Method method = TextDisplay.class.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private void invoke(Method method, Object... args) {
        try {
            method.invoke(this, args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to update text display", e);
        }
    }

    public enum TextBackgroundColor{

        /**
         * RGB can be represented by Integer.
         * ARGB must represent by Long.
         */
        DEFAULT ("69C8FF", "FF160C0E"),
        DAY ("000000", "FFe0e0e0"),
        NIGHT("FFFFFF", "FF202020"),
        MATRIX("00d643", "FF0a0a0a"),
        FERN ("784884", "FF201f22"),
        ABBA ("091972", "FF0ABBA0"),
        SAKURANIGHT ("FEACAD", "FF1A153D"),
        SAKURADAY ("f9316d", "FFfed9d5"),
        MISTYBLUE ("001532", "FFa0afb7"),
        UCL ("FF9933", "FF000000"),
        TUB ("FFFFFF", "FFC61521"),
        KTH ("FFFFFF", "FF2258A5");
    
        private final String textRGB;
        private final String backgroundARGB;
    
        TextBackgroundColor(String textRGB, String backgroundARGB){
            this.textRGB = textRGB;
            this.backgroundARGB = backgroundARGB;
        }
        
        int getTextRGBAsInt(){
            return Integer.parseInt(textRGB, 16);
        }
    
        long getBackgroundARGBAsLong(){
            return Long.parseLong(backgroundARGB, 16);
        }
    
    }

}
