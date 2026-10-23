package meowdyes.mc;

import meowdyes.render.Canvas;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Canvas on top of the game's GUI renderer. A parallelogram is drawn as fill(0,0,1,1) under an affine
 * transform that maps the unit square onto it, so the 3D model needs nothing beyond the normal GUI fill.
 */
final class McCanvas implements Canvas {
    private static final Map<String, MutableComponent> PLAIN = new HashMap<>();
    private static final Map<String, MutableComponent> BOLD = new HashMap<>();

    private final GuiGraphicsExtractor g;
    private final Font font;
    private final int w, h;

    McCanvas(GuiGraphicsExtractor g, Font font, int w, int h) {
        this.g = g;
        this.font = font;
        this.w = w;
        this.h = h;
    }

    @Override public int width() { return w; }
    @Override public int height() { return h; }

    @Override
    public void quad(float x, float y, float ux, float uy, float vx, float vy, int argb) {
        if ((argb >>> 24) == 0) return;
        float det = ux * vy - uy * vx;
        if (Math.abs(det) < 1e-5f) return;
        if (det < 0) { // keep the same winding as a plain fill() so nothing gets back-face culled
            float tx = ux, ty = uy;
            ux = vx; uy = vy;
            vx = tx; vy = ty;
        }
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.mul(new Matrix3x2f(ux, uy, vx, vy, x, y));
        g.fill(0, 0, 1, 1, argb);
        pose.popMatrix();
    }

    @Override
    public void rect(float x, float y, float rw, float rh, int argb) {
        // whole-pixel rectangles go straight through fill()
        if (x == (int) x && y == (int) y && rw == (int) rw && rh == (int) rh) {
            if ((argb >>> 24) == 0 || rw <= 0 || rh <= 0) return;
            g.fill((int) x, (int) y, (int) (x + rw), (int) (y + rh), argb);
            return;
        }
        quad(x, y, rw, 0, 0, rh, argb);
    }

    @Override
    public int textWidth(String s, boolean bold) {
        return font.width(component(s, bold));
    }

    @Override
    public void text(String s, float x, float y, float scale, int argb, boolean bold, boolean shadow) {
        // the font treats alpha < 4 as "no alpha given" and draws fully opaque, so skip near-invisible text
        if ((argb >>> 24) < 5) return;
        MutableComponent c = component(s, bold);
        if (scale == 1f && x == (int) x && y == (int) y) {
            g.text(font, c, (int) x, (int) y, argb, shadow);
            return;
        }
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scale, scale);
        g.text(font, c, 0, 0, argb, shadow);
        pose.popMatrix();
    }

    /** Frees the text cache (called when nothing of ours is on screen any more). */
    static void clearCache() {
        PLAIN.clear();
        BOLD.clear();
    }

    private static MutableComponent component(String s, boolean bold) {
        Map<String, MutableComponent> cache = bold ? BOLD : PLAIN;
        MutableComponent c = cache.get(s);
        if (c == null) {
            if (cache.size() > 256) cache.clear();
            c = Component.literal(s);
            if (bold) c = c.withStyle(ChatFormatting.BOLD);
            cache.put(s, c);
        }
        return c;
    }
}
