package meowdyes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

/** Counts per dye, stored in a small properties file in the config folder. */
public final class Stats {
    public static final class Entry {
        /** Eligible blocks broken (or pristine procs) since tracking started. */
        public long mined;
        /** Dyes found. */
        public long found;
        /** Eligible blocks since the last dye of this kind (dry streak). */
        public long since;
    }

    private final Map<Dye, Entry> entries = new EnumMap<>(Dye.class);
    private final Path file;
    private boolean dirty;

    public Stats(Path file) {
        this.file = file;
        for (Dye d : Dye.values()) entries.put(d, new Entry());
    }

    public Entry get(Dye d) { return entries.get(d); }

    /** Counts one eligible block; returns the new dry streak. */
    public long addMined(Dye d) {
        Entry e = entries.get(d);
        e.mined++;
        e.since++;
        dirty = true;
        return e.since;
    }

    /** Records a drop. Returns {dye number, blocks it took}. */
    public long[] addFound(Dye d) {
        Entry e = entries.get(d);
        e.found++;
        long after = e.since;
        e.since = 0;
        dirty = true;
        return new long[]{e.found, after};
    }

    public void set(Dye d, long mined, long found, long since) {
        Entry e = entries.get(d);
        e.mined = Math.max(0, mined);
        e.found = Math.max(0, found);
        e.since = Math.max(0, Math.min(since, e.mined));
        dirty = true;
    }

    public long totalMined() {
        long t = 0;
        for (Dye d : Dye.values()) if (d != Dye.PRISTINE) t += entries.get(d).mined;
        return t;
    }

    public long totalFound() {
        long t = 0;
        for (Entry e : entries.values()) t += e.found;
        return t;
    }

    public boolean isDirty() { return dirty; }

    // ------------------------------------------------------------------ io

    public void load() {
        if (file == null || !Files.isRegularFile(file)) return;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            p.load(in);
        } catch (IOException ex) {
            MeowLog.warn("Could not read " + file + ": " + ex);
            return;
        }
        for (Dye d : Dye.values()) {
            Entry e = entries.get(d);
            e.mined = num(p, d.id + ".mined");
            e.found = num(p, d.id + ".found");
            e.since = Math.min(num(p, d.id + ".since"), e.mined);
        }
        dirty = false;
    }

    public void save() {
        if (file == null) return;
        Properties p = new Properties();
        for (Dye d : Dye.values()) {
            Entry e = entries.get(d);
            p.setProperty(d.id + ".mined", Long.toString(e.mined));
            p.setProperty(d.id + ".found", Long.toString(e.found));
            p.setProperty(d.id + ".since", Long.toString(e.since));
        }
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            try (OutputStream out = Files.newOutputStream(tmp)) {
                p.store(out, "Meow Dyes - counts per dye (mined = eligible blocks broken, since = blocks since the last dye)");
            }
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicFailed) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
            dirty = false;
        } catch (IOException ex) {
            MeowLog.warn("Could not save " + file + ": " + ex);
        }
    }

    private static long num(Properties p, String key) {
        try {
            return Math.max(0, Long.parseLong(p.getProperty(key, "0").trim()));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
