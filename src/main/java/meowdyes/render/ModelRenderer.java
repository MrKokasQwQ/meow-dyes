package meowdyes.render;

import meowdyes.Fmt;

import java.util.Arrays;
import java.util.List;

/** Orthographic voxel renderer: rotate, light, back-face cull, sort far-to-near, draw parallelograms. */
public final class ModelRenderer {
    private ModelRenderer() {}

    // light comes from the upper left, slightly toward the viewer (view space)
    private static final float LX, LY, LZ;

    static {
        float x = -0.45f, y = 0.8f, z = 0.55f;
        float l = (float) Math.sqrt(x * x + y * y + z * z);
        LX = x / l;
        LY = y / l;
        LZ = z / l;
    }

    /**
     * @param palette    the dye palette (see Dye.jar)
     * @param pxPerVoxel size of one voxel on screen
     * @param yaw        rotation around the vertical axis (radians)
     * @param pitch      tilt toward the viewer so the top is visible (radians)
     */
    public static void render(Canvas c, VoxelModel model, int[] palette, float cx, float cy, float pxPerVoxel,
                              float yaw, float pitch, float alpha) {
        if (alpha <= 0.01f || pxPerVoxel <= 0.05f) return;
        float cyw = (float) Math.cos(yaw), syw = (float) Math.sin(yaw);
        float cp = (float) Math.cos(pitch), sp = (float) Math.sin(pitch);

        List<VoxelModel.Face> faces = model.faces;
        int n = faces.size();
        if (orderBuf.length < n) orderBuf = new long[n];
        long[] order = orderBuf;
        int count = 0;
        for (int i = 0; i < n; i++) {
            VoxelModel.Face f = faces.get(i);
            // rotate the normal; skip faces pointing away from the viewer
            float nx = f.nx(), ny = f.ny(), nz = f.nz();
            float nz1 = -nx * syw + nz * cyw;
            float nz2 = ny * sp + nz1 * cp;
            if (nz2 <= 0.002f) continue;
            float z1 = -f.cx * syw + f.cz * cyw;
            float depth = f.cy * sp + z1 * cp; // larger = nearer
            // sort key: depth (as sortable int bits) in the high half, face index in the low half
            int bits = Float.floatToIntBits(depth);
            bits ^= (bits >> 31) & 0x7FFFFFFF;
            order[count++] = ((long) bits << 32) | (i & 0xFFFFFFFFL);
        }
        Arrays.sort(order, 0, count);

        float[] a = A, b = B, d = D;
        for (int k = 0; k < count; k++) {
            VoxelModel.Face f = faces.get((int) (order[k] & 0xFFFFFFFFL));
            project(f.p0, cx, cy, pxPerVoxel, cyw, syw, cp, sp, a);
            project(f.p1, cx, cy, pxPerVoxel, cyw, syw, cp, sp, b);
            project(f.p3, cx, cy, pxPerVoxel, cyw, syw, cp, sp, d);
            float ux = b[0] - a[0], uy = b[1] - a[1];
            float vx = d[0] - a[0], vy = d[1] - a[1];
            // grow each face by a fraction of a pixel so neighbours overlap instead of leaving hairline gaps
            float lu = (float) Math.sqrt(ux * ux + uy * uy), lv = (float) Math.sqrt(vx * vx + vy * vy);
            if (lu < 1e-4f || lv < 1e-4f) continue;
            float e = 0.35f;
            float x0 = a[0] - (ux / lu + vx / lv) * e;
            float y0 = a[1] - (uy / lu + vy / lv) * e;
            float gu = (lu + 2 * e) / lu, gv = (lv + 2 * e) / lv;

            int rgb = colour(palette, f.slot);
            float nx = f.nx(), ny = f.ny(), nz = f.nz();
            float nx1 = nx * cyw + nz * syw;
            float nz1 = -nx * syw + nz * cyw;
            float ny2 = ny * cp - nz1 * sp;
            float nz2 = ny * sp + nz1 * cp;
            float lambert = Math.max(0f, nx1 * LX + ny2 * LY + nz2 * LZ);
            float shade = f.emissive() ? 0.85f + 0.22f * lambert : 0.52f + 0.55f * lambert;
            c.quad(x0, y0, ux * gu, uy * gu, vx * gv, vy * gv, Fmt.argb(Fmt.shade(rgb, shade), alpha));
        }
    }

    // scratch buffers, reused every call (drawing only happens on the render thread)
    private static long[] orderBuf = new long[0];
    private static final float[] A = new float[2], B = new float[2], D = new float[2];

    private static void project(float[] p, float cx, float cy, float s, float cyw, float syw, float cp, float sp, float[] out) {
        float x1 = p[0] * cyw + p[2] * syw;
        float z1 = -p[0] * syw + p[2] * cyw;
        float y2 = p[1] * cp - z1 * sp;
        out[0] = cx + x1 * s;
        out[1] = cy - y2 * s;
    }

    static int colour(int[] palette, int slot) {
        return palette[slot];
    }
}
