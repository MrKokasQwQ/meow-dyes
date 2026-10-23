package meowdyes.render;

import java.util.ArrayList;
import java.util.List;

/**
 * The dye jar: a rounded 10×10×10 voxel cube with a mottled body, a black lid and a coloured ring on top.
 * Faces are pre-merged into rectangles so each one is a single parallelogram on screen.
 */
public final class VoxelModel {
    public static final int W = 10, H = 10, D = 10;

    // colour slots (index into the dye palette, see Dye.jar)
    static final int BODY0 = 0, BODY4 = 4, LID = 5, LID_DARK = 6, RING = 7, RING_LIGHT = 8, CENTER = 9, CENTER_GRAY = 10;

    /** Top of the lid, x across and z down. K lid, D lid speck, P ring, L ring light, C centre, G centre gray, . empty. */
    private static final String[] TOP = {
            "..KKKKKK..",
            ".KKDKKKKK.",
            "KKKPPPPKKK",
            "KKPLCCLPKD",
            "KKPCGGCPKK",
            "KKPCGGCPKK",
            "DKPLCCLPKK",
            "KKKPPPPKKK",
            ".KKKKKDKK.",
            "..KKKKKK..",
    };

    // built after TOP (static fields initialise in order)
    public static final VoxelModel JAR = buildJar(1);
    /** Same jar with coarser mottling (2×2 patches), so it needs far fewer shapes: used for the small icons. */
    public static final VoxelModel ICON = buildJar(2);

    /** One merged face: corner p0, corners p1 (along u) and p3 (along v), all centred model coords. */
    public static final class Face {
        public final int dir; // 0:+X 1:-X 2:+Y 3:-Y 4:+Z 5:-Z
        public final float[] p0, p1, p3;
        public final float cx, cy, cz;
        public final int slot;

        Face(int dir, float[] p0, float[] p1, float[] p3, int slot) {
            this.dir = dir;
            this.p0 = p0;
            this.p1 = p1;
            this.p3 = p3;
            this.slot = slot;
            this.cx = (p1[0] + p3[0]) * 0.5f;
            this.cy = (p1[1] + p3[1]) * 0.5f;
            this.cz = (p1[2] + p3[2]) * 0.5f;
        }

        public float nx() { return dir == 0 ? 1 : dir == 1 ? -1 : 0; }
        public float ny() { return dir == 2 ? 1 : dir == 3 ? -1 : 0; }
        public float nz() { return dir == 4 ? 1 : dir == 5 ? -1 : 0; }
        public boolean emissive() { return slot == RING || slot == RING_LIGHT; }
    }

    private final int[] cells = new int[W * H * D]; // slot + 1, 0 = empty
    public final List<Face> faces = new ArrayList<>();

    private int idx(int x, int y, int z) { return (y * D + z) * W + x; }

    int get(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= W || y >= H || z >= D) return 0;
        return cells[idx(x, y, z)];
    }

    private void set(int x, int y, int z, int slot) { cells[idx(x, y, z)] = slot + 1; }

    public int cellCount() {
        int n = 0;
        for (int c : cells) if (c != 0) n++;
        return n;
    }

    /** Stable per-voxel noise in [0, 1) so every dye shares the same texture. */
    private static float noise(int x, int y, int z) {
        int h = x * 73856093 ^ y * 19349663 ^ z * 83492791;
        h ^= h >>> 13;
        h *= 0x5bd1e995;
        h ^= h >>> 15;
        return (h & 0xFFFF) / 65536f;
    }

    private static boolean roundedCorner(int x, int z, int cut) {
        int dx = Math.min(x, W - 1 - x), dz = Math.min(z, D - 1 - z);
        return dx + dz < cut;
    }

    private static VoxelModel buildJar(int grain) {
        VoxelModel m = new VoxelModel();
        for (int x = 0; x < W; x++) {
            for (int z = 0; z < D; z++) {
                // body: rounded vertical edges, a slightly tucked-in bottom
                for (int y = 0; y <= 7; y++) {
                    if (roundedCorner(x, z, y == 0 ? 2 : 1)) continue;
                    if (y == 0 && (x == 0 || x == W - 1 || z == 0 || z == D - 1)) continue;
                    int gx = x / grain * grain, gy = y / grain * grain, gz = z / grain * grain;
                    float dark = 0.45f * (1f - gy / 7f) + 0.38f * ((gx + gz) / (float) (W + D - 2));
                    float n = noise(x / grain, y / grain, z / grain);
                    int shade = Math.round(dark * 4f + (n - 0.5f) * 1.9f);
                    if (n > 0.95f) shade = 0; // a few bright flecks
                    m.set(x, y, z, BODY0 + Math.max(0, Math.min(4, shade)));
                }
                // lid band
                if (!roundedCorner(x, z, 1)) m.set(x, 8, z, grain == 1 && noise(x, 8, z) > 0.8f ? LID_DARK : LID);
                // lid top with the ring
                char c = TOP[z].charAt(x);
                if (c != '.') {
                    int slot = switch (c) {
                        case 'D' -> LID_DARK;
                        case 'P' -> RING;
                        case 'L' -> RING_LIGHT;
                        case 'C' -> CENTER;
                        case 'G' -> CENTER_GRAY;
                        default -> LID;
                    };
                    m.set(x, 9, z, slot);
                }
            }
        }
        m.mesh();
        return m;
    }

    /** Greedy-merges exposed faces of equal colour into rectangles. */
    private void mesh() {
        int[] size = {W, H, D};
        for (int axis = 0; axis < 3; axis++) {
            int ua = axis == 0 ? 1 : 0;
            int va = axis == 2 ? 1 : 2;
            for (int sign = 1; sign >= -1; sign -= 2) {
                int dir = axis * 2 + (sign > 0 ? 0 : 1);
                int nu = size[ua], nv = size[va];
                for (int k = 0; k < size[axis]; k++) {
                    int[] mask = new int[nu * nv];
                    int[] p = new int[3];
                    for (int u = 0; u < nu; u++) {
                        for (int v = 0; v < nv; v++) {
                            p[axis] = k; p[ua] = u; p[va] = v;
                            int c = get(p[0], p[1], p[2]);
                            int n = 0;
                            if (c != 0) {
                                p[axis] = k + sign;
                                n = get(p[0], p[1], p[2]);
                            }
                            mask[u * nv + v] = (c != 0 && n == 0) ? c : 0;
                        }
                    }
                    float plane = sign > 0 ? k + 1 : k;
                    for (int u = 0; u < nu; u++) {
                        for (int v = 0; v < nv; ) {
                            int c = mask[u * nv + v];
                            if (c == 0) { v++; continue; }
                            int h = 1;
                            while (v + h < nv && mask[u * nv + v + h] == c) h++;
                            int w = 1;
                            outer:
                            while (u + w < nu) {
                                for (int j = 0; j < h; j++) if (mask[(u + w) * nv + v + j] != c) break outer;
                                w++;
                            }
                            for (int i = 0; i < w; i++) for (int j = 0; j < h; j++) mask[(u + i) * nv + v + j] = 0;
                            faces.add(new Face(dir,
                                    corner(axis, ua, va, plane, u, v),
                                    corner(axis, ua, va, plane, u + w, v),
                                    corner(axis, ua, va, plane, u, v + h),
                                    c - 1));
                            v += h;
                        }
                    }
                }
            }
        }
    }

    private static float[] corner(int axis, int ua, int va, float plane, float u, float v) {
        float[] p = new float[3];
        p[axis] = plane;
        p[ua] = u;
        p[va] = v;
        p[0] -= W / 2f;
        p[1] -= H / 2f;
        p[2] -= D / 2f;
        return p;
    }
}
