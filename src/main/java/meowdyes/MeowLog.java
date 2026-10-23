package meowdyes;

import java.util.HashSet;
import java.util.Set;

/** Minimal logging to the game log (Minecraft captures stdout/stderr). */
public final class MeowLog {
    private static final Set<String> ONCE = new HashSet<>();

    private MeowLog() {}

    public static void info(String msg) {
        System.out.println("[Meow Dyes] " + msg);
    }

    public static void warn(String msg) {
        System.err.println("[Meow Dyes] " + msg);
    }

    /** Logs a problem only the first time it happens for a given key. */
    public static void once(String key, Throwable t) {
        synchronized (ONCE) {
            if (!ONCE.add(key)) return;
        }
        System.err.println("[Meow Dyes] " + key + " failed: " + t);
        t.printStackTrace();
    }
}
