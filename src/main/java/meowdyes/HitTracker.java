package meowdyes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Decides which broken blocks were "the block I was mining".
 *
 * Every block you hit (the attack event, plus the block under the crosshair while attack is held) is
 * remembered with what it was. When one of those blocks becomes anything else (air, bedrock, stone, ...)
 * it counts as mined. Blocks that disappear around it (Mining Spread, Pickobulus) were never hit, so they
 * are never remembered. If several remembered blocks change in the same tick, only the one hit most
 * recently counts. A remembered block is forgotten {@link #GRACE_TICKS} ticks after you stop hitting it.
 *
 * @param <P> block position type
 * @param <B> block type (compared by identity)
 */
public final class HitTracker<P, B> {
    /** How long after you stop aiming at a block its change still counts (covers ping). */
    public static final int GRACE_TICKS = 20;

    public interface Env<P, B> {
        /** The block at pos right now (null if unloaded). */
        B blockAt(P pos);

        long key(P pos);

        /** A copy of pos that is safe to keep. */
        P keep(P pos);

        String id(B block);

        /** Which dye this block counts for in the current area, or null. */
        Dye classify(String blockId);

        /** False when nothing should be tracked (no world). */
        boolean active();
    }

    public interface Sink {
        void mined(Dye dye, String blockId);
    }

    private final class Watch {
        final P pos;
        final B block;
        final String id;
        final Dye dye;
        long lastHit;

        Watch(P pos, B block, String id, Dye dye) {
            this.pos = pos;
            this.block = block;
            this.id = id;
            this.dye = dye;
            this.lastHit = tick;
        }
    }

    private final Env<P, B> env;
    private final Sink sink;
    private final Map<Long, Watch> watches = new HashMap<>();
    private final List<Watch> changed = new ArrayList<>();
    private long tick;

    public HitTracker(Env<P, B> env, Sink sink) {
        this.env = env;
        this.sink = sink;
    }

    public void clear() {
        watches.clear();
        changed.clear();
    }

    public int watching() {
        return watches.size();
    }

    /** You are hitting the block at pos (attack event, or crosshair while attack is held). */
    public void hit(P pos) {
        if (!env.active()) return;
        B block = env.blockAt(pos);
        if (block == null) return;
        long key = env.key(pos);
        Watch existing = watches.get(key);
        if (existing != null) {
            if (existing.block == block) {
                existing.lastHit = tick;
                return;
            }
            // it changed before the end-of-tick check saw it
            watches.remove(key);
            changed.add(existing);
        }
        String id = env.id(block);
        if (id.equals("air") || id.equals("cave_air") || id.equals("void_air")) return;
        Dye dye = env.classify(id);
        if (dye == null) return;
        watches.put(key, new Watch(env.keep(pos), block, id, dye));
    }

    /** Call once per client tick, after the world has been updated. */
    public void tick() {
        tick++;
        for (Iterator<Watch> it = watches.values().iterator(); it.hasNext(); ) {
            Watch w = it.next();
            B now = env.blockAt(w.pos);
            if (now != w.block) {
                changed.add(w);
                it.remove();
            } else if (tick - w.lastHit > GRACE_TICKS) {
                it.remove();
            }
        }
        if (changed.isEmpty()) return;
        Watch best = null;
        for (Watch w : changed) {
            if (tick - w.lastHit > GRACE_TICKS) continue;
            if (best == null || w.lastHit > best.lastHit) best = w;
        }
        changed.clear();
        if (best != null) sink.mined(best.dye, best.id);
    }
}
