package meowdyes;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Where the player is, worked out from the sidebar scoreboard (sub-area line "⏣ Glacite Tunnels")
 * and the tab list ("Area: Dwarven Mines"). Pure Java so it can be tested without the game.
 */
public final class AreaState {
    public static final AreaState NONE = new AreaState(false, "", "");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    /** Sub-areas (scoreboard) that only exist on the Dwarven Mines island. */
    private static final Set<String> DWARVEN_SUB_AREAS = Set.of(
            "dwarven mines", "the forge", "forge basin", "lava springs", "palace bridge", "royal palace",
            "royal quarters", "aristocrat passage", "hanging court", "divan's gateway", "far reserve",
            "goblin burrows", "great ice wall", "miner's guild", "the mist", "rampart's quarry", "royal mines",
            "upper mines", "cliffside veins", "gates to the mines", "the lift", "dwarven village",
            "dwarven tavern", "grand library", "barracks of heroes", "c&c minecarts co.", "glacite tunnels",
            "great glacite lake", "dwarven base camp", "fossil research center");
    /** Sub-areas (scoreboard) that only exist in the Crystal Hollows. */
    private static final Set<String> HOLLOWS_SUB_AREAS = Set.of(
            "crystal hollows", "jungle", "jungle temple", "mithril deposits", "mines of divan",
            "precursor remnants", "lost precursor city", "goblin holdout", "goblin queen's den",
            "magma fields", "khazad-dûm", "khazad-dum", "fairy grotto", "dragon's lair", "crystal nucleus");
    /** Glacite areas inside the Dwarven Mines island (scoreboard). */
    private static final Set<String> GLACITE_SUB_AREAS = Set.of("glacite tunnels", "great glacite lake");

    public final boolean inSkyBlock;
    /** Island from the tab list, e.g. "Dwarven Mines", "Mineshaft", "Crystal Hollows"; "" when unknown. */
    public final String island;
    /** Sub-area from the scoreboard, e.g. "Glacite Tunnels"; "" when unknown. */
    public final String subArea;

    public AreaState(boolean inSkyBlock, String island, String subArea) {
        this.inSkyBlock = inSkyBlock;
        this.island = island == null ? "" : island;
        this.subArea = subArea == null ? "" : subArea;
    }

    private String islandKey() { return island.toLowerCase(Locale.ROOT); }
    private String subKey() { return subArea.toLowerCase(Locale.ROOT); }

    public boolean isMineshaft() {
        return islandKey().equals("mineshaft") || subKey().startsWith("glacite mineshaft");
    }

    public boolean isDwarvenMines() {
        if (!island.isEmpty()) return islandKey().equals("dwarven mines");
        return DWARVEN_SUB_AREAS.contains(subKey());
    }

    public boolean isCrystalHollows() {
        if (!island.isEmpty()) return islandKey().equals("crystal hollows");
        return HOLLOWS_SUB_AREAS.contains(subKey());
    }

    /** Glacite Tunnels, Great Glacite Lake or a Glacite Mineshaft. */
    public boolean isGlacite() {
        if (isMineshaft()) return true;
        if (!island.isEmpty() && !islandKey().equals("dwarven mines")) return false;
        return GLACITE_SUB_AREAS.contains(subKey());
    }

    public String describe() {
        if (!inSkyBlock) return "normal world (outside SkyBlock)";
        String i = island.isEmpty() ? "unknown island" : island;
        String s = subArea.isEmpty() ? "" : " / " + subArea;
        String tag = isGlacite() ? " (glacite rules)" : "";
        return i + s + tag;
    }

    // ---------------------------------------------------------------- parsing

    /**
     * @param sidebarTitle the sidebar objective's title text (may be null)
     * @param sidebarLines the sidebar lines as plain strings (any order)
     * @param tabLines     the tab-list display names as plain strings
     */
    public static AreaState parse(String sidebarTitle, List<String> sidebarLines, List<String> tabLines) {
        String island = "";
        boolean tabSaysSkyBlock = false;
        if (tabLines != null) {
            for (String raw : tabLines) {
                String line = clean(raw);
                String lower = line.toLowerCase(Locale.ROOT);
                if (lower.startsWith("area:") || lower.startsWith("dungeon:")) {
                    island = line.substring(line.indexOf(':') + 1).trim();
                    tabSaysSkyBlock = true;
                    break;
                }
            }
        }
        String sub = "";
        if (sidebarLines != null) {
            for (String raw : sidebarLines) {
                String line = stripFormatting(raw);
                int i = line.indexOf('⏣');
                if (i < 0) i = line.indexOf('ф'); // the Rift uses ф
                if (i >= 0) {
                    sub = clean(line.substring(i + 1));
                    break;
                }
            }
        }
        String title = sidebarTitle == null ? "" : stripFormatting(sidebarTitle).toUpperCase(Locale.ROOT);
        boolean titleSaysSkyBlock = title.contains("SKYBLOCK") || title.contains("SKIBLOCK");
        return new AreaState(titleSaysSkyBlock || tabSaysSkyBlock, island, sub);
    }

    /** The tab-list line that names the island ("Area: Dwarven Mines", or "Dungeon: ..." in dungeons). */
    public static boolean isAreaLine(String raw) {
        String s = stripFormatting(raw).trim();
        return s.regionMatches(true, 0, "Area:", 0, 5) || s.regionMatches(true, 0, "Dungeon:", 0, 8);
    }

    public static String stripFormatting(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '§' && i + 1 < s.length()) { i++; continue; }
            b.append(c);
        }
        return b.toString();
    }

    /** Removes formatting codes and decorative/junk characters, keeping letters, digits and simple punctuation. */
    public static String clean(String s) {
        String t = stripFormatting(s);
        StringBuilder b = new StringBuilder(t.length());
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (Character.isLetterOrDigit(c) || c == ' ' || c == '\'' || c == '-' || c == '.' || c == '&' || c == ':' || c == '(' || c == ')' || c == '/') {
                b.append(c);
            } else if (Character.isWhitespace(c)) {
                b.append(' ');
            }
        }
        return SPACES.matcher(b).replaceAll(" ").trim();
    }
}
