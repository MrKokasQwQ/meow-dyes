package meowdyes;

import java.util.Locale;

public final class Fmt {
    private Fmt() {}

    /** 5000000 -> "5,000,000" */
    public static String num(long n) {
        return String.format(Locale.US, "%,d", n);
    }

    /** Expected number of dyes after n tries at 1/oneIn, e.g. "0.1259". */
    public static String expected(long n, long oneIn) {
        double e = (double) n / (double) oneIn;
        if (e >= 100) return String.format(Locale.US, "%,.1f", e);
        return String.format(Locale.US, "%.4f", e);
    }

    /** Chance of having found at least one by now, as a percentage string. */
    public static String chanceByNow(long n, long oneIn) {
        double p = -Math.expm1(n * Math.log1p(-1.0 / oneIn));
        double pct = p * 100.0;
        if (pct < 0.01 && pct > 0) return String.format(Locale.US, "%.4f%%", pct);
        return String.format(Locale.US, "%.2f%%", pct);
    }

    public static int lerpColor(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        int r = Math.round(ar + (br - ar) * t);
        int g = Math.round(ag + (bg - ag) * t);
        int bl = Math.round(ab + (bb - ab) * t);
        return (r << 16) | (g << 8) | bl;
    }

    /** Colour at position t (0..1) along a multi-stop gradient (RGB). */
    public static int gradient(int[] stops, float t) {
        if (stops.length == 1) return stops[0];
        t = Math.max(0f, Math.min(1f, t));
        float f = t * (stops.length - 1);
        int i = Math.min((int) f, stops.length - 2);
        return lerpColor(stops[i], stops[i + 1], f - i);
    }

    public static int argb(int rgb, float alpha) {
        int a = Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f);
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    public static int shade(int rgb, float k) {
        int r = Math.min(255, Math.round(((rgb >> 16) & 255) * k));
        int g = Math.min(255, Math.round(((rgb >> 8) & 255) * k));
        int b = Math.min(255, Math.round((rgb & 255) * k));
        return (r << 16) | (g << 8) | b;
    }
}
