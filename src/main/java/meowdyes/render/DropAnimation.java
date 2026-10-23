package meowdyes.render;

import meowdyes.Dye;
import meowdyes.Fmt;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The drop animation: the dye pot spins in toward the screen, particles burst out, "<NAME> DYE" and
 * "1 in X" appear, everything holds for a few seconds, then fades. Purely a function of time.
 */
public final class DropAnimation {
    public static final float DURATION = 4.9f;

    private static final float TAU = (float) (Math.PI * 2);

    public final Dye dye;
    private final String title;
    private final String subtitle;
    private final List<Particle> particles = new ArrayList<>();
    private final float yaw0;

    private static final class Particle {
        float start, life, x, y, vx, vy, gravity, size, spin, rot0, alpha;
        int rgb;
        int kind; // 0 square, 1 smoke, 2 sparkle
    }

    public DropAnimation(Dye dye, long seed) {
        this.dye = dye;
        this.title = dye.title();
        this.subtitle = "1 in " + Fmt.num(dye.oneIn);
        Random r = new Random(seed);
        this.yaw0 = r.nextFloat() * TAU;
        // burst of coloured bits when the pot pops in
        for (int i = 0; i < 70; i++) {
            Particle p = new Particle();
            p.kind = 0;
            p.start = 0.10f + r.nextFloat() * 0.10f;
            p.life = 0.7f + r.nextFloat() * 0.8f;
            float ang = r.nextFloat() * TAU, sp = 70 + r.nextFloat() * 190;
            p.x = (r.nextFloat() - 0.5f) * 20;
            p.y = (r.nextFloat() - 0.5f) * 20;
            p.vx = (float) Math.cos(ang) * sp;
            p.vy = (float) Math.sin(ang) * sp - 60;
            p.gravity = 230;
            p.size = 1.5f + r.nextFloat() * 3.2f;
            p.spin = (r.nextFloat() - 0.5f) * 12;
            p.rot0 = r.nextFloat() * TAU;
            p.alpha = 1f;
            p.rgb = dye.particleColors[r.nextInt(dye.particleColors.length)];
            particles.add(p);
        }
        // big soft smoke squares rising behind it
        for (int i = 0; i < 26; i++) {
            Particle p = new Particle();
            p.kind = 1;
            p.start = 0.02f + r.nextFloat() * 0.30f;
            p.life = 1.0f + r.nextFloat() * 0.9f;
            p.x = (r.nextFloat() - 0.5f) * 130;
            p.y = 20 + r.nextFloat() * 40;
            p.vx = (r.nextFloat() - 0.5f) * 80;
            p.vy = -(20 + r.nextFloat() * 50);
            p.gravity = -10;
            p.size = 10 + r.nextFloat() * 20;
            p.spin = (r.nextFloat() - 0.5f) * 1.5f;
            p.rot0 = r.nextFloat() * TAU;
            p.alpha = 0.55f;
            p.rgb = Fmt.shade(dye.particleColors[r.nextInt(Math.min(2, dye.particleColors.length))], 1.0f);
            particles.add(p);
        }
        // twinkles while it hovers
        for (int i = 0; i < 26; i++) {
            Particle p = new Particle();
            p.kind = 2;
            p.start = 0.5f + r.nextFloat() * 3.3f;
            p.life = 0.45f + r.nextFloat() * 0.35f;
            float ang = r.nextFloat() * TAU, rad = 45 + r.nextFloat() * 55;
            p.x = (float) Math.cos(ang) * rad;
            p.y = (float) Math.sin(ang) * rad * 0.8f - 6;
            p.vy = -8;
            p.size = 3 + r.nextFloat() * 3;
            p.alpha = 1f;
            p.rgb = dye.particleColors[dye.particleColors.length - 1 - r.nextInt(Math.min(2, dye.particleColors.length))];
            particles.add(p);
        }
        // a final puff as it leaves
        for (int i = 0; i < 30; i++) {
            Particle p = new Particle();
            p.kind = 0;
            p.start = 3.92f + r.nextFloat() * 0.08f;
            p.life = 0.5f + r.nextFloat() * 0.5f;
            float ang = r.nextFloat() * TAU, sp = 40 + r.nextFloat() * 110;
            p.vx = (float) Math.cos(ang) * sp;
            p.vy = (float) Math.sin(ang) * sp - 30;
            p.gravity = 140;
            p.size = 1.5f + r.nextFloat() * 2.5f;
            p.spin = (r.nextFloat() - 0.5f) * 10;
            p.alpha = 0.9f;
            p.rgb = dye.particleColors[r.nextInt(dye.particleColors.length)];
            particles.add(p);
        }
    }

    public boolean finished(float t) {
        return t >= DURATION;
    }

    public void render(Canvas c, float t) {
        int w = c.width(), h = c.height();
        float unit = Math.min(w, h) / 270f; // particle/ray distances are authored for a 270px-tall GUI
        float cx = w * 0.5f;
        float modelY = h * 0.40f;
        float voxel = h * 0.215f / VoxelModel.W;

        // ---------------------------------------------------------------- model transform over time
        float intro = clamp01(t / 0.55f);
        float scale = 0.08f + 0.92f * easeOutBack(intro, 2.4f);
        float rise = (1f - easeOutCubic(clamp01(t / 0.45f))) * h * 0.30f;
        float spinIn = 1f - cube(1f - clamp01(t / 1.15f));
        float yaw = yaw0 + spinIn * TAU * 1.5f + t * 0.9f;
        float pitch = 0.50f + 0.05f * (float) Math.sin(t * 2.2f);
        float bob = t > 0.5f ? (float) Math.sin((t - 0.5f) * 2.4f) * h * 0.010f : 0f;
        float modelAlpha = 1f;
        float out = clamp01((t - 3.9f) / 0.45f);
        if (out > 0) {
            scale *= 1f + 0.35f * out - 1.35f * out * out * out;
            yaw += out * out * 6f;
            modelAlpha = 1f - out * out;
        }
        float my = modelY + rise + bob;

        // ---------------------------------------------------------------- glow + rays behind the model
        float glowA = clamp01((t - 0.12f) / 0.25f) * (1f - clamp01((t - 3.7f) / 0.6f));
        if (glowA > 0) {
            int hl = dye.highlight();
            for (int i = 0; i < 5; i++) {
                float s = voxel * (13f + i * 3.2f) * Math.max(0.3f, scale);
                c.rotatedRect(cx, my, s, s, (float) Math.PI / 4 + t * 0.15f * (i % 2 == 0 ? 1 : -1), Fmt.argb(hl, 0.045f * glowA));
            }
        }
        float raysT = (t - 0.16f) / 0.95f;
        if (raysT > 0 && raysT < 1) {
            float grow = easeOutCubic(clamp01(raysT * 2.2f));
            float fade = 1f - clamp01((raysT - 0.35f) / 0.65f);
            int hl = dye.highlight();
            for (int i = 0; i < 16; i++) {
                float ang = i * TAU / 16 + t * 0.6f + (i % 2) * 0.12f;
                float r0 = voxel * 7.5f;
                float len = voxel * (5f + (i % 3) * 2.5f) * grow;
                float rx = cx + (float) Math.cos(ang) * (r0 + len * 0.5f);
                float ry = my + (float) Math.sin(ang) * (r0 + len * 0.5f);
                c.rotatedRect(rx, ry, len, Math.max(1f, voxel * 0.28f), ang, Fmt.argb(hl, 0.75f * fade));
            }
        }

        // smoke goes behind the model
        drawParticles(c, t, cx, my, unit, 1);

        // ---------------------------------------------------------------- the model
        ModelRenderer.render(c, VoxelModel.JAR, dye.palette, cx, my, voxel * scale, yaw, pitch, modelAlpha);

        // bits and twinkles in front
        drawParticles(c, t, cx, my, unit, 0);
        drawParticles(c, t, cx, my, unit, 2);

        // ---------------------------------------------------------------- text
        float titleScale = clamp(h / 140f, 1.5f, 6f);
        float titleIn = clamp01((t - 0.12f) / 0.28f);
        float titleOut = 1f - clamp01((t - 4.15f) / 0.7f);
        float titleA = titleIn * titleOut;
        if (titleA > 0.02f) {
            float pop = 1f + 0.35f * (1f - easeOutBack(titleIn, 1.8f));
            float s = titleScale * pop;
            float tw = c.textWidth(title, true) * s;
            float ty = h * 0.47f - 4.5f * s;
            c.gradientText(title, cx - tw / 2f, ty, s, dye.titleGradient, titleA, true, true);
        }
        float subA = clamp01((t - 0.32f) / 0.28f) * (1f - clamp01((t - 4.1f) / 0.7f));
        if (subA > 0.02f) {
            float s = titleScale * 0.55f;
            float sw = c.textWidth(subtitle, false) * s;
            float sy = h * 0.47f + 5.6f * titleScale;
            c.text(subtitle, cx - sw / 2f, sy, s, Fmt.argb(0xE6E6E6, subA), false, true);
        }
    }

    private void drawParticles(Canvas c, float t, float cx, float cy, float unit, int kind) {
        for (Particle p : particles) {
            if (p.kind != kind) continue;
            float age = t - p.start;
            if (age < 0 || age > p.life) continue;
            float f = age / p.life;
            float x = cx + (p.x + p.vx * age) * unit;
            float y = cy + (p.y + p.vy * age + 0.5f * p.gravity * age * age) * unit;
            switch (kind) {
                case 0 -> {
                    float a = p.alpha * (1f - f * f);
                    float s = p.size * unit * (1f - 0.4f * f);
                    c.rotatedRect(x, y, s, s, p.rot0 + p.spin * age, Fmt.argb(p.rgb, a));
                }
                case 1 -> {
                    float a = p.alpha * (f < 0.15f ? f / 0.15f : 1f - (f - 0.15f) / 0.85f);
                    float s = p.size * unit * (0.7f + 0.6f * f);
                    c.rotatedRect(x, y, s, s, p.rot0 + p.spin * age, Fmt.argb(p.rgb, a));
                }
                default -> {
                    float tw = (float) Math.sin(f * Math.PI);
                    float s = p.size * unit * tw;
                    int col = Fmt.argb(p.rgb, tw);
                    c.rect(x - s / 2f, y - s * 0.09f, s, s * 0.18f + 0.6f, col);
                    c.rect(x - s * 0.09f, y - s / 2f, s * 0.18f + 0.6f, s, col);
                }
            }
        }
    }

    // ---------------------------------------------------------------- easing
    static float clamp01(float v) { return v < 0 ? 0 : v > 1 ? 1 : v; }
    static float clamp(float v, float lo, float hi) { return v < lo ? lo : v > hi ? hi : v; }
    static float cube(float v) { return v * v * v; }
    static float easeOutCubic(float v) { return 1f - cube(1f - v); }

    static float easeOutBack(float v, float c1) {
        float c3 = c1 + 1f;
        float x = v - 1f;
        return 1f + c3 * x * x * x + c1 * x * x;
    }
}
