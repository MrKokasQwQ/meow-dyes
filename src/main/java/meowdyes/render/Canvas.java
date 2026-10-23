package meowdyes.render;

/**
 * The few drawing operations the animation and the stats screen need. Implemented on top of the game's
 * GuiGraphicsExtractor in-game, and on top of Java2D for offline previews/tests.
 */
public interface Canvas {
    int width();

    int height();

    /** Filled parallelogram with corner (x, y) and edge vectors (ux, uy) and (vx, vy). */
    void quad(float x, float y, float ux, float uy, float vx, float vy, int argb);

    /** Width in unscaled GUI pixels. */
    int textWidth(String s, boolean bold);

    /** Draws text with its top-left corner at (x, y), scaled by {@code scale}. */
    void text(String s, float x, float y, float scale, int argb, boolean bold, boolean shadow);

    default void rect(float x, float y, float w, float h, int argb) {
        quad(x, y, w, 0, 0, h, argb);
    }

    /** w×h rectangle centred on (cx, cy), rotated by {@code ang} radians. */
    default void rotatedRect(float cx, float cy, float w, float h, float ang, int argb) {
        float c = (float) Math.cos(ang), s = (float) Math.sin(ang);
        float ux = c * w, uy = s * w;
        float vx = -s * h, vy = c * h;
        quad(cx - (ux + vx) * 0.5f, cy - (uy + vy) * 0.5f, ux, uy, vx, vy, argb);
    }

    /** Text with a left-to-right colour gradient across the whole string. */
    default float gradientText(String s, float x, float y, float scale, int[] stops, float alpha, boolean bold, boolean shadow) {
        float total = textWidth(s, bold) * scale;
        float cx = x;
        for (int i = 0; i < s.length(); i++) {
            String ch = s.substring(i, i + 1);
            float w = textWidth(ch, bold) * scale;
            float mid = total <= 0 ? 0 : ((cx - x) + w * 0.5f) / total;
            int rgb = meowdyes.Fmt.gradient(stops, mid);
            if (!ch.equals(" ")) text(ch, cx, y, scale, meowdyes.Fmt.argb(rgb, alpha), bold, shadow);
            cx += w;
        }
        return total;
    }
}
