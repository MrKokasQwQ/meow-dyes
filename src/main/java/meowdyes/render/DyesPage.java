package meowdyes.render;

import meowdyes.AreaState;
import meowdyes.Dye;
import meowdyes.Fmt;
import meowdyes.Stats;

import java.util.ArrayList;
import java.util.List;

/** Layout and drawing of the /md dyes screen, independent of the game so it can be previewed offline. */
public final class DyesPage {
    private DyesPage() {}

    public static final int GAP = 8, TOP = 46, FOOTER = 30;
    private static final int SIDE = 12, MIN_CARD_W = 168, MAX_CARD_W = 250, MIN_CARD_H = 74, MAX_CARD_H = 100;
    private static final int[] TITLE_GRADIENT = {0xFF8AD8, 0xFFD27A, 0x8CE8FF};
    /** Three-quarter view used for every card; the hovered card spins. */
    private static final float ICON_YAW = 0.78f, ICON_PITCH = 0.48f;

    /**
     * @param scope which counts these are, e.g. "SkyBlock counts"
     * @return the total height of the scrollable card area (for clamping the scroll offset)
     */
    public static int render(Canvas c, Stats stats, String scope, AreaState area, int mouseX, int mouseY, float scroll, float time) {
        int w = c.width(), h = c.height();
        Dye[] dyes = Dye.values();

        // up to 4 cards per row, as big as the window allows
        int availW = w - 2 * SIDE;
        int cols = 4;
        while (cols > 1 && cols * MIN_CARD_W + (cols - 1) * GAP > availW) cols--;
        int cardW = Math.min(MAX_CARD_W, (availW - (cols - 1) * GAP) / cols);
        int rows = (dyes.length + cols - 1) / cols;
        int viewTop = TOP, viewBottom = h - FOOTER;
        int cardH = (viewBottom - viewTop - 6 - (rows - 1) * GAP) / rows;
        cardH = Math.max(MIN_CARD_H, Math.min(MAX_CARD_H, cardH));
        int gridW = cols * cardW + (cols - 1) * GAP;
        int contentH = rows * (cardH + GAP) - GAP;
        // centre the grid vertically when it fits
        float y0 = viewTop + Math.max(0, (viewBottom - viewTop - contentH) / 2f);

        Dye hovered = null;
        for (int i = 0; i < dyes.length; i++) {
            int row = i / cols, col = i % cols;
            int inRow = Math.min(cols, dyes.length - row * cols);
            float x = (w - gridW) / 2f + (cols - inRow) * (cardW + GAP) / 2f + col * (cardW + GAP);
            float y = y0 + row * (cardH + GAP) - scroll;
            if (y + cardH < viewTop - 2 || y > viewBottom + 2) continue; // off screen: skip entirely
            boolean hover = mouseX >= x && mouseX < x + cardW && mouseY >= y && mouseY < y + cardH
                    && mouseY >= viewTop && mouseY < viewBottom;
            if (hover) hovered = dyes[i];
            drawCard(c, dyes[i], stats.get(dyes[i]), x, y, cardW, cardH, hover, time);
        }

        boolean scrolls = contentH > viewBottom - viewTop;

        // header and footer bars are drawn over anything that scrolled under them
        if (scrolls) c.rect(0, 0, w, TOP - 3, 0xFF101014);
        String title = "Meow Dyes";
        float ts = 2f;
        float tw = c.textWidth(title, true) * ts;
        c.gradientText(title, (w - tw) / 2f, 8, ts, TITLE_GRADIENT, 1f, true, true);
        String sub = scope + "  ·  " + Fmt.num(stats.totalMined()) + " blocks counted  ·  " + Fmt.num(stats.totalFound()) + " dyes found";
        c.text(sub, (w - c.textWidth(sub, false)) / 2f, 29, 1f, 0xFFAAAAAA, false, true);

        c.rect(0, h - FOOTER, w, FOOTER, scrolls ? 0xFF101014 : 0xB0000000);
        String a = "Area: " + area.describe();
        c.text(a, (w - c.textWidth(a, false)) / 2f, h - FOOTER + 5, 1f, 0xFF9AD8FF, false, true);
        String help = (scrolls ? "Scroll for more  ·  " : "") + "Hover a card for details  ·  /md test <dye> plays a drop";
        c.text(help, (w - c.textWidth(help, false)) / 2f, h - FOOTER + 17, 1f, 0xFF777777, false, true);

        if (hovered != null) drawTooltip(c, hovered, stats.get(hovered), mouseX, mouseY);
        return contentH;
    }

    private static void drawCard(Canvas c, Dye d, Stats.Entry e, float x, float y, int cw, int ch, boolean hover, float time) {
        int hl = d.highlight();
        c.rect(x, y, cw, ch, hover ? 0xD8202028 : 0xC0101014);
        int border = Fmt.argb(hl, hover ? 0.95f : 0.55f);
        c.rect(x, y, cw, 1, border);
        c.rect(x, y + ch - 1, cw, 1, border);
        c.rect(x, y, 1, ch, border);
        c.rect(x + cw - 1, y, 1, ch, border);

        String l1 = "Found: " + Fmt.num(e.found), l2 = "Mined: " + Fmt.num(e.mined);
        String l3 = "1 in " + Fmt.num(d.oneIn), l4 = "Exp: " + Fmt.expected(e.mined, d.oneIn);
        int textW = Math.max(c.textWidth(d.fullName(), true), Math.max(Math.max(c.textWidth(l1, false), c.textWidth(l2, false)),
                Math.max(c.textWidth(l3, false), c.textWidth(l4, false))));
        float modelBox = ch * 0.92f;
        float left = x + Math.max(4f, (cw - modelBox - textW) / 2f); // centre model + text in the card

        // the model only moves while you hover its card
        float yaw = hover ? ICON_YAW + time * 1.6f : ICON_YAW;
        ModelRenderer.render(c, VoxelModel.ICON, d.palette, left + modelBox * 0.48f, y + ch / 2f + 1, ch * 0.056f, yaw, ICON_PITCH, 1f);

        float tx = left + modelBox;
        float ty = y + (ch - 53) / 2f;
        c.gradientText(d.fullName(), tx, ty, 1f, d.titleGradient, 1f, true, true);
        c.text(l1, tx, ty + 12, 1f, e.found > 0 ? 0xFF55FF55 : 0xFFAAAAAA, false, true);
        c.text(l2, tx, ty + 22, 1f, 0xFFDDDDDD, false, true);
        c.text(l3, tx, ty + 32, 1f, 0xFFAAAAAA, false, true);
        c.text(l4, tx, ty + 42, 1f, 0xFF888888, false, true);
    }

    private static void drawTooltip(Canvas c, Dye d, Stats.Entry e, int mx, int my) {
        List<String> lines = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        lines.add(d.fullName()); colors.add(0xFF000000 | d.highlight());
        for (String part : wrap("Counts: " + d.counts(), 200, c)) { lines.add(part); colors.add(0xFF9AD8FF); }
        lines.add("Mined: " + Fmt.num(e.mined) + " " + d.unit); colors.add(0xFFDDDDDD);
        lines.add("Since last dye: " + Fmt.num(e.since)); colors.add(0xFFDDDDDD);
        lines.add("Found: " + Fmt.num(e.found)); colors.add(e.found > 0 ? 0xFF55FF55 : 0xFFDDDDDD);
        lines.add("Expected by now: " + Fmt.expected(e.mined, d.oneIn)); colors.add(0xFFDDDDDD);
        lines.add("Chance of 1+ by now: " + Fmt.chanceByNow(e.mined, d.oneIn)); colors.add(0xFFDDDDDD);
        lines.add("Preview: /md test " + d.id); colors.add(0xFF777777);

        int tw = 0;
        for (String s : lines) tw = Math.max(tw, c.textWidth(s, false));
        int th = lines.size() * 10 + 4;
        float x = mx + 12, y = my - 12;
        if (x + tw + 8 > c.width()) x = mx - 16 - tw;
        if (y + th + 4 > c.height()) y = c.height() - th - 4;
        if (y < 2) y = 2;
        c.rect(x - 4, y - 4, tw + 8, th + 4, 0xF0100010);
        int b = Fmt.argb(d.highlight(), 0.8f);
        c.rect(x - 4, y - 4, tw + 8, 1, b);
        c.rect(x - 4, y + th - 1, tw + 8, 1, b);
        c.rect(x - 4, y - 4, 1, th + 4, b);
        c.rect(x + tw + 3, y - 4, 1, th + 4, b);
        for (int i = 0; i < lines.size(); i++) {
            c.text(lines.get(i), x, y + i * 10 + (i == 0 ? 0 : 2), 1f, colors.get(i), false, true);
        }
    }

    private static List<String> wrap(String s, int maxW, Canvas c) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : s.split(" ")) {
            String trial = line.isEmpty() ? word : line + " " + word;
            if (c.textWidth(trial, false) > maxW && !line.isEmpty()) {
                out.add(line.toString());
                line = new StringBuilder("  " + word);
            } else {
                line = new StringBuilder(trial);
            }
        }
        if (!line.isEmpty()) out.add(line.toString());
        return out;
    }
}
