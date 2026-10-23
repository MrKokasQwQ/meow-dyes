package meowdyes;

/**
 * Block id (e.g. "coal_ore") + area -> dye, or null when the block doesn't count there.
 *
 * Block lists cross-checked against SkyHanni 9.2.0 (OreBlock.kt) and Skyblocker 6.9.2 for 26.1.2:
 *  - Glacite Tunnels tungsten "cobblestone" is really infested_cobblestone; Mineshafts use cobblestone,
 *    cobblestone_slab and cobblestone_stairs.
 *  - Umber's red sandstone is smooth_red_sandstone.
 *  - In the Crystal Hollows, gray wool and cyan terracotta are Hard Stone, not mithril.
 */
public final class Classifier {
    private Classifier() {}

    public static Dye classify(String blockId, AreaState a) {
        if (blockId == null || a == null) return null;
        String id = blockId.startsWith("minecraft:") ? blockId.substring(10) : blockId;
        if (!a.inSkyBlock) return classifyNormalWorld(id);
        boolean glacite = a.isGlacite();
        boolean dwarven = a.isDwarvenMines();
        boolean hollows = a.isCrystalHollows();
        boolean mineshaft = a.isMineshaft();

        switch (id) {
            // ---- glacite-only ores
            case "packed_ice":
                return glacite ? Dye.GLACITE : null;
            case "terracotta":
            case "brown_terracotta":
            case "smooth_red_sandstone":
                return glacite ? Dye.UMBER : null;
            case "clay":
                return glacite ? Dye.TUNGSTEN : null;
            case "infested_cobblestone":
            case "cobblestone":
                return glacite ? Dye.TUNGSTEN : Dye.COBBLESTONE;
            case "cobblestone_slab":
            case "cobblestone_stairs":
                return glacite ? Dye.TUNGSTEN : null;

            // ---- mithril
            case "gray_wool":
            case "cyan_terracotta":
                // low tier mithril; in the Crystal Hollows these are Hard Stone
                return (dwarven || mineshaft) ? Dye.MITHRIL : null;
            case "prismarine":
            case "prismarine_bricks":
            case "dark_prismarine":
            case "light_blue_wool":
                return (dwarven || hollows || mineshaft) ? Dye.MITHRIL : null;

            // ---- anywhere
            case "coal_ore":
            case "deepslate_coal_ore":
            case "coal_block":
                return Dye.COAL;
            case "mycelium":
                return Dye.MYCELIUM;
            case "red_sand":
                return Dye.RED_SAND;
            case "end_stone":
                return Dye.END_STONE;
            case "netherrack":
                return Dye.NETHERRACK;
            case "gold_block":
                return Dye.GOLD;
            case "gravel":
                return Dye.FLINT;
            case "diamond_block":
                return Dye.DIAMOND;
            case "emerald_block":
                return Dye.EMERALD;
            default:
                return null;
        }
    }

    /**
     * Outside SkyBlock (singleplayer, other servers) there are no areas, so every listed block counts
     * everywhere. Lets you try the whole thing in a normal world.
     */
    static Dye classifyNormalWorld(String id) {
        return switch (id) {
            case "coal_ore", "deepslate_coal_ore", "coal_block" -> Dye.COAL;
            case "mycelium" -> Dye.MYCELIUM;
            case "red_sand" -> Dye.RED_SAND;
            case "cobblestone", "infested_cobblestone", "mossy_cobblestone" -> Dye.COBBLESTONE;
            case "gray_wool", "light_blue_wool", "cyan_terracotta", "prismarine", "prismarine_bricks", "dark_prismarine" -> Dye.MITHRIL;
            case "packed_ice" -> Dye.GLACITE;
            case "terracotta", "brown_terracotta", "smooth_red_sandstone" -> Dye.UMBER;
            case "clay" -> Dye.TUNGSTEN;
            case "end_stone" -> Dye.END_STONE;
            case "netherrack" -> Dye.NETHERRACK;
            case "gold_block" -> Dye.GOLD;
            case "gravel" -> Dye.FLINT;
            case "diamond_block" -> Dye.DIAMOND;
            case "emerald_block" -> Dye.EMERALD;
            default -> null;
        };
    }

    /** True for chat lines like "PRISTINE! You found ✎ Flawed Sapphire Gemstone x2!". */
    public static boolean isPristineMessage(String plain) {
        if (plain == null) return false;
        String s = AreaState.stripFormatting(plain).trim();
        return s.startsWith("PRISTINE! You found") && s.contains("Gemstone");
    }
}
