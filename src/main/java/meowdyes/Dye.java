package meowdyes;

import java.util.Locale;

/**
 * Every fake dye: its odds, what counts toward it, and its colours.
 *
 * The model palette is built by {@link #jar}: body shades light to dark, then lid and ring colours.
 */
public enum Dye {
    //            id             name           odds        unit              body light, body dark, ring, ring light
    COAL("coal", "Coal", 5_000_000L, "coal",
            jar(0x5A5E64, 0x1C1E22, 0x8C8C8C, 0xCFCFCF),
            new int[]{0x2B2B2B, 0x8A8A8A, 0xC4C4C4},
            new int[]{0x151515, 0x2E2E2E, 0x4A4A4A, 0x7A7A7A, 0xB0B0B0},
            "LARGE_SMOKE", "SMOKE", "ASH"),
    MYCELIUM("mycelium", "Mycelium", 10_000_000L, "mycelium",
            jar(0xA48EB0, 0x45374C, 0xC9A3E8, 0xEFDDFF),
            new int[]{0x5B4A5B, 0x9C88A8, 0xD2B4F0},
            new int[]{0x4A3B4A, 0x8C7A8C, 0xA77BCA, 0xE2C6F5},
            "MYCELIUM", "PORTAL", "WITCH"),
    RED_SAND("red_sand", "Red Sand", 10_000_000L, "red sand",
            jar(0xF09A55, 0x86381A, 0xFFD08A, 0xFFF0D0),
            new int[]{0x8A4520, 0xD4772F, 0xFFC27A},
            new int[]{0x8A4520, 0xB4602A, 0xF0A055, 0xFFD9A0},
            "CLOUD", "CRIT", "POOF"),
    COBBLESTONE("cobblestone", "Cobblestone", 10_000_000L, "cobblestone",
            jar(0xB4B4B4, 0x484848, 0xE2E2E2, 0xFFFFFF),
            new int[]{0x4F4F4F, 0x9A9A9A, 0xEDEDED},
            new int[]{0x575757, 0x7A7A7A, 0xB5B5B5, 0xEDEDED},
            "POOF", "CLOUD", "CRIT"),
    MITHRIL("mithril", "Mithril", 5_000_000L, "mithril",
            jar(0x7AEAF0, 0x1A5470, 0x9BF0FF, 0xE6FFFF),
            new int[]{0x3C5A60, 0x2FB5C8, 0x9BF0FF},
            new int[]{0x2F6F73, 0x2AA5B8, 0x5FD4E6, 0xCFF7FF},
            "END_ROD", "ELECTRIC_SPARK", "ENCHANT"),
    GLACITE("glacite", "Glacite", 2_000_000L, "glacite",
            jar(0xE2F1FF, 0x5F80B8, 0xA8D4FF, 0xFFFFFF),
            new int[]{0x6C8FC4, 0xA8D0FF, 0xF2F9FF},
            new int[]{0x6C8FC4, 0xA8D0FF, 0xD4E9FF, 0xFFFFFF},
            "SNOWFLAKE", "END_ROD", "CLOUD"),
    UMBER("umber", "Umber", 2_000_000L, "umber",
            jar(0xBC8650, 0x46301C, 0xE8A86E, 0xFAD7AE),
            new int[]{0x4E341F, 0x9C6B42, 0xE0A06A},
            new int[]{0x3E2A1A, 0x6B4A2F, 0xC67F45, 0xE8B27A},
            "ASH", "SMOKE", "CRIT"),
    TUNGSTEN("tungsten", "Tungsten", 2_000_000L, "tungsten",
            jar(0xA6B2BC, 0x333B42, 0xDDE6EC, 0xFFFFFF),
            new int[]{0x434C53, 0x8C99A3, 0xDDE6EC},
            new int[]{0x2F363B, 0x5E6A73, 0xB4C0C8, 0xE4EDF2},
            "CRIT", "ELECTRIC_SPARK", "SMOKE"),
    END_STONE("end_stone", "End Stone", 10_000_000L, "end stone",
            jar(0xF4F4BE, 0x9C9C62, 0xB77CE0, 0xE6C4FF),
            new int[]{0xCFCF8F, 0xE8E8B8, 0xB77CE0},
            new int[]{0xD9D9A0, 0xEDEDBE, 0xA65FD0, 0xE0B8FF},
            "PORTAL", "REVERSE_PORTAL", "END_ROD"),
    NETHERRACK("netherrack", "Netherrack", 10_000_000L, "netherrack",
            jar(0xC24444, 0x461212, 0xFF7A3A, 0xFFC27A),
            new int[]{0x5A1A1A, 0xB02A2A, 0xFF7A3A},
            new int[]{0x3A1212, 0x6E2626, 0xF2552A, 0xFFB15A},
            "FLAME", "LAVA", "CRIMSON_SPORE"),
    GOLD("gold", "Gold", 7_500_000L, "gold blocks",
            jar(0xFFE878, 0xB0820E, 0xFFF3A8, 0xFFFFFF),
            new int[]{0xB88A12, 0xF5C518, 0xFFF3A8},
            new int[]{0xB88A12, 0xF5C518, 0xFFE066, 0xFFF7C2},
            "WAX_ON", "END_ROD", "FIREWORK"),
    FLINT("flint", "Flint", 10_000_000L, "gravel",
            jar(0x7A7A84, 0x1A1A1E, 0xB4B4BE, 0xE6E6EE),
            new int[]{0x2A2A2D, 0x6E6E76, 0xC4C4CC},
            new int[]{0x1A1A1C, 0x3A3A3D, 0x6E6E76, 0xA8A8B2},
            "SMOKE", "CRIT", "ASH"),
    DIAMOND("diamond", "Diamond", 7_500_000L, "diamond blocks",
            jar(0xA8FFF8, 0x1A8A86, 0xE6FFFE, 0xFFFFFF),
            new int[]{0x1E948F, 0x4AEDE5, 0xE6FFFE},
            new int[]{0x1E948F, 0x4AEDE5, 0x9AF7F2, 0xFFFFFF},
            "END_ROD", "ELECTRIC_SPARK", "FIREWORK"),
    EMERALD("emerald", "Emerald", 7_500_000L, "emerald blocks",
            jar(0x7CF59C, 0x0C6E2E, 0xC8FFD6, 0xFFFFFF),
            new int[]{0x0F7F35, 0x22D45A, 0xC8FFD6},
            new int[]{0x0F7F35, 0x22D45A, 0x6CF08F, 0xD8FFE2},
            "HAPPY_VILLAGER", "END_ROD", "FIREWORK"),
    PRISTINE("pristine", "Pristine", 5_000_000L, "pristine procs",
            jar(0x5CE8FF, 0x2440B8, 0xFF3DD2, 0xFF9CF0),
            new int[]{0xFF55FF, 0xC8A2FF, 0x55FFFF},
            new int[]{0xFF55FF, 0xFF9CF5, 0x55FFFF, 0xFFFFFF},
            "END_ROD", "ENCHANT", "TOTEM_OF_UNDYING");

    public final String id;
    public final String displayName;
    public final long oneIn;
    /** Word used in "after N <unit>". */
    public final String unit;
    public final int[] palette;
    public final int[] titleGradient;
    public final int[] particleColors;
    public final String[] worldParticles;

    Dye(String id, String displayName, long oneIn, String unit, int[] palette, int[] titleGradient,
        int[] particleColors, String... worldParticles) {
        this.id = id;
        this.displayName = displayName;
        this.oneIn = oneIn;
        this.unit = unit;
        this.palette = palette;
        this.titleGradient = titleGradient;
        this.particleColors = particleColors;
        this.worldParticles = worldParticles;
    }

    /**
     * Model colours for the dye jar: 5 body shades (light to dark), lid black, lid dark gray, ring, ring light,
     * centre dark, centre gray. Slot numbers are in VoxelModel.
     */
    static int[] jar(int bodyLight, int bodyDark, int ring, int ringLight) {
        int[] p = new int[11];
        for (int i = 0; i < 5; i++) p[i] = Fmt.lerpColor(bodyLight, bodyDark, i / 4f);
        p[5] = 0x131315;
        p[6] = 0x2C2C31;
        p[7] = ring;
        p[8] = ringLight;
        p[9] = 0x1B1B1F;
        p[10] = 0x55555C;
        return p;
    }

    /** Which blocks count toward this dye (shown in the stats screen). */
    public String counts() {
        return switch (this) {
            case COAL -> "Coal Ore, Block of Coal";
            case MYCELIUM -> "Mycelium";
            case RED_SAND -> "Red Sand";
            case COBBLESTONE -> "Cobblestone (outside Glacite areas)";
            case MITHRIL -> "Gray & Light Blue Wool, Cyan Terracotta, Prismarine (Dwarven Mines, Glacite; Hollows: Prismarine & Light Blue Wool)";
            case GLACITE -> "Packed Ice (Glacite Tunnels & Mineshafts)";
            case UMBER -> "Terracotta, Brown Terracotta, Smooth Red Sandstone (Glacite)";
            case TUNGSTEN -> "Cobblestone & Clay (Glacite Tunnels & Mineshafts)";
            case END_STONE -> "End Stone";
            case NETHERRACK -> "Netherrack";
            case GOLD -> "Block of Gold";
            case FLINT -> "Gravel";
            case DIAMOND -> "Block of Diamond";
            case EMERALD -> "Block of Emerald";
            case PRISTINE -> "Each PRISTINE! proc on gemstones";
        };
    }

    /** "Coal Dye" */
    public String fullName() {
        return displayName + " Dye";
    }

    /** "COAL DYE" */
    public String title() {
        return fullName().toUpperCase(Locale.ROOT);
    }

    /** Colour used for "CRAZY RARE DROP!" and other accents. */
    public int highlight() {
        return titleGradient[titleGradient.length - 1];
    }

    public static Dye byId(String id) {
        if (id == null) return null;
        String s = id.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (s.endsWith("_dye")) s = s.substring(0, s.length() - 4);
        for (Dye d : values()) if (d.id.equals(s)) return d;
        // a few friendly aliases
        return switch (s) {
            case "redsand" -> RED_SAND;
            case "endstone" -> END_STONE;
            case "cobble" -> COBBLESTONE;
            case "gravel" -> FLINT;
            case "gold_block", "gold_blocks" -> GOLD;
            case "diamond_block", "diamond_blocks" -> DIAMOND;
            case "emerald_block", "emerald_blocks" -> EMERALD;
            case "gem", "gemstone", "gemstones" -> PRISTINE;
            default -> null;
        };
    }
}
