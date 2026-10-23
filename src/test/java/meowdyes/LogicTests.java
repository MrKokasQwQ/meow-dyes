package meowdyes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Plain-Java tests for everything that doesn't need the game. Run: java meowdyes.LogicTests */
public final class LogicTests {
    private static int passed, failed;

    static void check(boolean ok, String what) {
        if (ok) passed++;
        else {
            failed++;
            System.out.println("FAIL: " + what);
        }
    }

    static void eq(Object expected, Object actual, String what) {
        check(expected == null ? actual == null : expected.equals(actual), what + " (expected " + expected + ", got " + actual + ")");
    }

    // ------------------------------------------------------------------ a tiny fake world for HitTracker
    static final String AIR = "air", BEDROCK = "bedrock", STONE = "stone", COAL = "coal_ore", COBBLE = "cobblestone",
            GRAY_WOOL = "gray_wool", PRISMARINE = "prismarine", PACKED_ICE = "packed_ice";

    static final class Sim {
        final Map<Long, String> world = new HashMap<>();
        final List<String> mined = new ArrayList<>();
        AreaState area = new AreaState(true, "Dwarven Mines", "Cliffside Veins");
        boolean active = true;
        final HitTracker<Long, String> t = new HitTracker<>(new HitTracker.Env<>() {
            public String blockAt(Long pos) { return world.getOrDefault(pos, AIR); }
            public long key(Long pos) { return pos; }
            public Long keep(Long pos) { return pos; }
            public String id(String block) { return block; }
            public Dye classify(String id) { return Classifier.classify(id, area); }
            public boolean active() { return active; }
        }, new HitTracker.Sink() {
            public void mined(Dye dye, String id) { mined.add(dye.id + ":" + id); }
        });

        void set(long pos, String b) { world.put(pos, b); }
    }

    static void hitTrackerTests() {
        // 1) slow mining: hit for 30 ticks, then the server turns it into bedrock
        {
            Sim s = new Sim();
            s.set(1, COAL);
            for (int i = 0; i < 30; i++) { s.t.hit(1L); s.t.tick(); }
            s.set(1, BEDROCK);
            s.t.tick();
            eq(List.of("coal:coal_ore"), s.mined, "slow mining counts once");
        }
        // 2) Mining Spread: neighbours vanish in the same tick but were never hit
        {
            Sim s = new Sim();
            s.set(1, COAL); s.set(2, COAL); s.set(3, COAL);
            for (int i = 0; i < 10; i++) { s.t.hit(1L); s.t.tick(); }
            s.set(1, BEDROCK); s.set(2, BEDROCK); s.set(3, AIR);
            s.t.tick();
            s.t.tick();
            eq(1, s.mined.size(), "mining spread: only the hit block counts");
        }
        // 3) spread block that the crosshair brushed earlier, changing in the same tick -> still one
        {
            Sim s = new Sim();
            s.set(1, COAL); s.set(2, COAL);
            s.t.hit(2L); s.t.tick();
            for (int i = 0; i < 5; i++) { s.t.hit(1L); s.t.tick(); }
            s.set(1, BEDROCK); s.set(2, BEDROCK);
            s.t.tick();
            eq(1, s.mined.size(), "two hit blocks changing together count once");
        }
        // 4) instamine one block per tick with 3 ticks of latency
        {
            Sim s = new Sim();
            for (long p = 10; p < 20; p++) s.set(p, COAL);
            for (int tick = 0; tick < 20; tick++) {
                long target = 10 + tick;
                if (target < 20) s.t.hit(target);
                long breaking = 10 + tick - 3;
                if (breaking >= 10 && breaking < 20) s.set(breaking, BEDROCK);
                s.t.tick();
            }
            eq(10, s.mined.size(), "instamining 10 blocks with latency counts 10");
        }
        // 5) client-predicted instant break: hit and gone within the same tick
        {
            Sim s = new Sim();
            for (long p = 30; p < 35; p++) s.set(p, COAL);
            for (long p = 30; p < 35; p++) {
                s.t.hit(p);
                s.set(p, AIR); // client destroys it right after the attack event
                s.t.tick();
            }
            eq(5, s.mined.size(), "client-side instant breaks count");
        }
        // 6) looking but never hitting
        {
            Sim s = new Sim();
            s.set(1, COAL);
            s.t.tick();
            s.set(1, AIR);
            s.t.tick();
            eq(0, s.mined.size(), "blocks never hit don't count");
        }
        // 7) hit, look away for longer than the grace period, then someone else breaks it
        {
            Sim s = new Sim();
            s.set(1, COAL);
            s.t.hit(1L); s.t.tick();
            for (int i = 0; i < HitTracker.GRACE_TICKS + 5; i++) s.t.tick();
            s.set(1, AIR); s.t.tick();
            eq(0, s.mined.size(), "stale hits expire");
        }
        // 8) regenerating ore at the same spot counts each time
        {
            Sim s = new Sim();
            s.set(1, COAL);
            for (int i = 0; i < 3; i++) { s.t.hit(1L); s.t.tick(); }
            s.set(1, BEDROCK); s.t.hit(1L); s.t.tick();
            for (int i = 0; i < 3; i++) { s.t.hit(1L); s.t.tick(); } // hitting bedrock: not tracked
            s.set(1, COAL); s.t.tick();
            for (int i = 0; i < 3; i++) { s.t.hit(1L); s.t.tick(); }
            s.set(1, BEDROCK); s.t.tick();
            eq(2, s.mined.size(), "regenerated ore counts again");
        }
        // 9) area rules: cobblestone in the tunnels is tungsten, elsewhere cobblestone
        {
            Sim s = new Sim();
            s.area = new AreaState(true, "Dwarven Mines", "Glacite Tunnels");
            s.set(1, COBBLE); s.t.hit(1L); s.t.tick(); s.set(1, BEDROCK); s.t.tick();
            s.area = new AreaState(true, "Dwarven Mines", "Royal Mines");
            s.set(2, COBBLE); s.t.hit(2L); s.t.tick(); s.set(2, BEDROCK); s.t.tick();
            eq(List.of("tungsten:cobblestone", "cobblestone:cobblestone"), s.mined, "cobblestone depends on area");
        }
        // 10) blocks that don't count aren't tracked
        {
            Sim s = new Sim();
            s.set(1, STONE); s.t.hit(1L); s.t.tick(); s.set(1, AIR); s.t.tick();
            eq(0, s.mined.size(), "stone doesn't count");
            eq(0, s.t.watching(), "nothing left tracked");
        }
        // 10b) normal world (active) counts with normal-world rules
        {
            Sim s = new Sim();
            s.area = new AreaState(false, "", "");
            s.set(1, PACKED_ICE); s.t.hit(1L); s.t.tick(); s.set(1, AIR); s.t.tick();
            eq(List.of("glacite:packed_ice"), s.mined, "normal world counts");
        }
        // 11) inactive (no world) -> nothing is tracked
        {
            Sim s = new Sim();
            s.active = false;
            s.set(1, COAL); s.t.hit(1L); s.t.tick(); s.set(1, AIR); s.t.tick();
            eq(0, s.mined.size(), "inactive -> nothing");
        }
        // 12) block changed between the attack event of one tick and the next attack on the same spot
        {
            Sim s = new Sim();
            s.set(1, PACKED_ICE);
            s.area = new AreaState(true, "Mineshaft", "Glacite Mineshafts");
            s.t.hit(1L);
            s.set(1, BEDROCK);
            s.t.hit(1L); // same tick: notices the change
            s.t.tick();
            eq(List.of("glacite:packed_ice"), s.mined, "change noticed during a hit");
        }
    }

    static void areaTests() {
        AreaState a = AreaState.parse("§e§lSKYBLOCK",
                List.of("§710/23/26 §8m12AB", " §7⏣ §bGlacite Tunnels🎂", "§fPurse: §61,234"),
                List.of("§b§lArea: §7Dwarven Mines", "§e§lServer: §7mini12"));
        check(a.inSkyBlock, "in skyblock from title");
        eq("Dwarven Mines", a.island, "island from tab");
        eq("Glacite Tunnels", a.subArea, "sub area cleaned");
        check(a.isGlacite(), "tunnels are glacite");
        check(a.isDwarvenMines(), "tunnels are on the Dwarven Mines island");

        AreaState shaft = AreaState.parse("SKYBLOCK CO-OP", List.of(" ⏣ Glacite Mineshafts"), List.of("Area: Mineshaft"));
        check(shaft.isGlacite() && shaft.isMineshaft(), "mineshaft is glacite");

        AreaState hollows = AreaState.parse("SKYBLOCK", List.of(" ⏣ Mithril Deposits"), List.of("Area: Crystal Hollows"));
        check(hollows.isCrystalHollows() && !hollows.isGlacite() && !hollows.isDwarvenMines(), "hollows");

        AreaState noTab = AreaState.parse("SKYBLOCK", List.of(" ⏣ Cliffside Veins"), List.of());
        check(noTab.isDwarvenMines() && !noTab.isGlacite(), "dwarven from sub-area only");
        AreaState noTabLake = AreaState.parse("SKYBLOCK", List.of(" ⏣ Great Glacite Lake"), List.of());
        check(noTabLake.isGlacite(), "glacite lake from sub-area only");

        AreaState rift = AreaState.parse("SKYBLOCK", List.of(" ф Wizard Tower"), List.of("Area: The Rift"));
        eq("Wizard Tower", rift.subArea, "rift sub-area");

        AreaState lobby = AreaState.parse("§e§lHYPIXEL", List.of("Rank: MVP+"), List.of("[MVP+] someone"));
        check(!lobby.inSkyBlock, "lobby isn't skyblock");

        AreaState tabOnly = AreaState.parse(null, List.of(), List.of("Area: Hub"));
        check(tabOnly.inSkyBlock, "tab 'Area:' alone means skyblock");
    }

    static void classifierTests() {
        AreaState dm = new AreaState(true, "Dwarven Mines", "Royal Mines");
        AreaState tunnels = new AreaState(true, "Dwarven Mines", "Glacite Tunnels");
        AreaState shaft = new AreaState(true, "Mineshaft", "Glacite Mineshafts");
        AreaState ch = new AreaState(true, "Crystal Hollows", "Jungle");
        AreaState hub = new AreaState(true, "Hub", "Coal Mine");
        AreaState isle = new AreaState(true, "Crimson Isle", "Stronghold");
        AreaState end = new AreaState(true, "The End", "Dragon's Nest");

        eq(Dye.COAL, Classifier.classify("coal_ore", hub), "coal ore anywhere");
        eq(Dye.COAL, Classifier.classify("minecraft:coal_block", dm), "block of coal");
        eq(Dye.MITHRIL, Classifier.classify("gray_wool", dm), "gray wool = mithril in DM");
        eq(Dye.MITHRIL, Classifier.classify("light_blue_wool", dm), "light blue wool");
        eq(Dye.MITHRIL, Classifier.classify("cyan_terracotta", dm), "cyan terracotta");
        eq(Dye.MITHRIL, Classifier.classify("dark_prismarine", dm), "dark prismarine");
        eq(null, Classifier.classify("gray_wool", ch), "gray wool is hard stone in the Hollows");
        eq(null, Classifier.classify("cyan_terracotta", ch), "cyan terracotta is hard stone in the Hollows");
        eq(Dye.MITHRIL, Classifier.classify("prismarine_bricks", ch), "prismarine mithril in the Hollows");
        eq(Dye.MITHRIL, Classifier.classify("light_blue_wool", ch), "light blue wool in the Hollows");
        eq(null, Classifier.classify("gray_wool", hub), "wool elsewhere isn't mithril");
        eq(Dye.MITHRIL, Classifier.classify("gray_wool", tunnels), "mithril in the tunnels");
        eq(Dye.GLACITE, Classifier.classify("packed_ice", tunnels), "glacite");
        eq(Dye.GLACITE, Classifier.classify("packed_ice", shaft), "glacite in mineshaft");
        eq(null, Classifier.classify("packed_ice", dm), "packed ice outside glacite");
        eq(Dye.UMBER, Classifier.classify("terracotta", tunnels), "umber terracotta");
        eq(Dye.UMBER, Classifier.classify("brown_terracotta", shaft), "umber brown terracotta");
        eq(Dye.UMBER, Classifier.classify("smooth_red_sandstone", tunnels), "umber smooth red sandstone");
        eq(null, Classifier.classify("red_sandstone", tunnels), "plain red sandstone isn't umber");
        eq(Dye.TUNGSTEN, Classifier.classify("infested_cobblestone", tunnels), "tunnels tungsten is infested cobblestone");
        eq(Dye.TUNGSTEN, Classifier.classify("cobblestone", shaft), "mineshaft tungsten cobblestone");
        eq(Dye.TUNGSTEN, Classifier.classify("cobblestone_stairs", shaft), "mineshaft tungsten stairs");
        eq(Dye.TUNGSTEN, Classifier.classify("clay", tunnels), "tungsten clay");
        eq(null, Classifier.classify("clay", hub), "clay elsewhere");
        eq(Dye.COBBLESTONE, Classifier.classify("cobblestone", dm), "cobblestone in DM");
        eq(Dye.COBBLESTONE, Classifier.classify("cobblestone", hub), "cobblestone in hub");
        eq(Dye.GOLD, Classifier.classify("gold_block", ch), "gold block");
        eq(null, Classifier.classify("gold_ore", ch), "gold ore isn't a gold block");
        eq(Dye.DIAMOND, Classifier.classify("diamond_block", dm), "diamond block");
        eq(Dye.EMERALD, Classifier.classify("emerald_block", dm), "emerald block");
        eq(Dye.FLINT, Classifier.classify("gravel", hub), "gravel -> flint");
        eq(Dye.MYCELIUM, Classifier.classify("mycelium", isle), "mycelium");
        eq(Dye.RED_SAND, Classifier.classify("red_sand", isle), "red sand");
        eq(Dye.NETHERRACK, Classifier.classify("netherrack", isle), "netherrack");
        eq(Dye.END_STONE, Classifier.classify("end_stone", end), "end stone");
        eq(null, Classifier.classify("stone", dm), "stone");
        eq(null, Classifier.classify("bedrock", dm), "bedrock");

        AreaState normal = new AreaState(false, "", "");
        eq(Dye.COAL, Classifier.classify("deepslate_coal_ore", normal), "normal world: deepslate coal");
        eq(Dye.GLACITE, Classifier.classify("packed_ice", normal), "normal world: packed ice anywhere");
        eq(Dye.TUNGSTEN, Classifier.classify("clay", normal), "normal world: clay");
        eq(Dye.UMBER, Classifier.classify("terracotta", normal), "normal world: terracotta");
        eq(Dye.MITHRIL, Classifier.classify("prismarine", normal), "normal world: prismarine");
        eq(Dye.COBBLESTONE, Classifier.classify("cobblestone", normal), "normal world: cobblestone is cobblestone");
        eq(Dye.FLINT, Classifier.classify("gravel", normal), "normal world: gravel");
        eq(null, Classifier.classify("stone", normal), "normal world: stone");
        eq(null, Classifier.classify("dirt", normal), "normal world: dirt");

        check(Classifier.isPristineMessage("PRISTINE! You found ✎ Flawed Sapphire Gemstone x2!"), "pristine plain");
        check(Classifier.isPristineMessage("§d§lPRISTINE! §r§fYou found §r§a☘ Flawed Jade Gemstone §r§8x20§r§f!"), "pristine formatted");
        check(!Classifier.isPristineMessage("[MVP+] Someone: PRISTINE! You found ✎ Flawed Ruby Gemstone x2!"), "player chat isn't pristine");
        check(!Classifier.isPristineMessage("You found a Flawed Ruby Gemstone"), "other messages");
    }

    static void statsTests() throws Exception {
        Path dir = Files.createTempDirectory("meowdyes");
        Path f = dir.resolve("config").resolve("meowdyes.properties");
        Stats s = new Stats(f);
        for (int i = 0; i < 629_721; i++) s.addMined(Dye.COAL);
        long[] r = s.addFound(Dye.COAL);
        eq(1L, r[0], "dye #1");
        eq(629_721L, r[1], "after 629,721");
        s.addMined(Dye.COAL);
        s.save();
        Stats t = new Stats(f);
        t.load();
        eq(629_722L, t.get(Dye.COAL).mined, "mined persisted");
        eq(1L, t.get(Dye.COAL).found, "found persisted");
        eq(1L, t.get(Dye.COAL).since, "since persisted");
        eq("0.1259", Fmt.expected(629_721, 5_000_000), "expected like the clip");
        eq("5,000,000", Fmt.num(5_000_000), "number format");
        eq("7,500,000", Fmt.num(Dye.GOLD.oneIn), "gold odds");
        eq("11.83%", Fmt.chanceByNow(629_721, 5_000_000), "chance by now");
        eq(Dye.RED_SAND, Dye.byId("red sand"), "byId alias");
        eq(Dye.FLINT, Dye.byId("gravel"), "byId gravel");
        eq("COAL DYE", Dye.COAL.title(), "title");
    }

    public static void main(String[] args) throws Exception {
        hitTrackerTests();
        areaTests();
        classifierTests();
        statsTests();
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}
