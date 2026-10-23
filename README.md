# Meow Dyes

Fake Hypixel SkyBlock dyes for **Fabric, Minecraft 26.1.x**. Every eligible block you mine (and every
Pristine proc) rolls for a dye. On a hit you get a drop animation (recoloured dye jar spinning in toward the
screen, particles, a big `<NAME> DYE` title and `1 in X`, then a fade) and a chat line:

```
CRAZY RARE DROP! Coal Dye
 1 in 5,000,000  ·  dye #1  ·  after 629,721 coal
```

## Install
Put `meowdyes-1.0.0+26.1.2.jar` in your `mods` folder next to Fabric API (Fabric Loader 0.18.4+, Java 25).

## Commands
| Command | What it does |
|---|---|
| `/md dyes` | Stats screen: every dye, how many you've got, blocks mined, odds, expected (hover a card for more) |
| `/md test <dye>` | Plays the drop (chat + animation) without counting it; `/md test` alone picks a random dye |

Dye ids: `coal mycelium red_sand cobblestone mithril glacite umber tungsten end_stone netherrack gold flint diamond emerald pristine`

To reset counts, delete `config/meowdyes.properties` (SkyBlock) or `config/meowdyes-normal-worlds.properties`
(normal worlds) while the game is closed.

## What counts
Only the block you hit. The mod remembers each block you attack (and the block under your crosshair while
attack is held) and counts it when that block turns into anything else (air, bedrock, stone…). Blocks that
vanish around it (Mining Spread, Pickobulus) were never hit, so they don't count. Instamining (a block a
tick) counts every block. If several blocks you hit change in the same tick, only the most recent one counts.

**Normal worlds.** Outside SkyBlock (singleplayer, other servers) there are no areas, so every block in
the list counts anywhere: coal ore / deepslate coal ore / coal block, mycelium, red sand, cobblestone,
gray & light blue wool, cyan terracotta, prismarine (mithril), packed ice (glacite), terracotta, brown
terracotta, smooth red sandstone (umber), clay (tungsten), end stone, netherrack, gravel, and gold / diamond
/ emerald blocks. These counts are kept separately (`config/meowdyes-normal-worlds.properties`) so they never
mix with your SkyBlock counts. Pristine only happens on SkyBlock.

Area comes from the scoreboard (`⏣ Glacite Tunnels`) and the tab list (`Area: Dwarven Mines`).

| Dye | Odds | Blocks |
|---|---|---|
| Coal | 1/5m | coal ore, block of coal |
| Mycelium | 1/10m | mycelium |
| Red Sand | 1/10m | red sand |
| Cobblestone | 1/10m | cobblestone (outside Glacite areas) |
| Mithril | 1/5m | gray wool, cyan terracotta, light blue wool, prismarine ×3 (Dwarven Mines incl. tunnels, Mineshafts; in the Crystal Hollows only prismarine + light blue wool, since gray wool and cyan terracotta are Hard Stone there) |
| Glacite | 1/2m | packed ice (Glacite Tunnels, Great Glacite Lake, Mineshafts) |
| Umber | 1/2m | terracotta, brown terracotta, **smooth** red sandstone (Glacite) |
| Tungsten | 1/2m | cobblestone (in the tunnels it's really `infested_cobblestone`), cobblestone slab/stairs, clay (Glacite) |
| End Stone | 1/10m | end stone |
| Netherrack | 1/10m | netherrack |
| Gold | 1/7.5m | block of gold |
| Flint | 1/10m | gravel |
| Diamond | 1/7.5m | block of diamond |
| Emerald | 1/7.5m | block of emerald |
| Pristine | 1/5m | one roll per `PRISTINE! You found … Gemstone` message |

Block lists were cross-checked against SkyHanni 9.2.0 and Skyblocker 6.9.2 (both for 26.1.2).

SkyBlock counts are saved in `config/meowdyes.properties`.

## Client-only: no packets, no network, no updater
The mod never sends anything to the server it's connected to, never connects to the internet, never
downloads or updates anything, and loads no extra code. It only reads what your client already has (the
scoreboard, the tab list, chat, and the blocks in front of you). Its chat lines are added to your own chat
window, `/md` commands are handled on your side by Fabric, and the sound and particles play only for you.
It writes two files: `config/meowdyes.properties` and `config/meowdyes-normal-worlds.properties`.

## Performance
When no drop is playing and the dyes screen is closed, nothing is drawn. Per tick the mod only checks the
block you're hitting (well under a microsecond) and reads the area from the scoreboard and tab list once a
second. The drop animation exists only for the ~5 seconds after a drop, and the dyes screen only draws while
it's open (its models stay still unless you hover one). Counts are saved at most once a minute, only when
they changed.

## Building from source
Minecraft 26.1 is unobfuscated, so the project uses Loom without mappings. With Gradle 9.5+ and Java 25:

```
gradle build          # jar in build/libs
gradle logicTests     # plain-Java tests for area parsing, block rules, hit tracking, stats
```

Layout: `meowdyes/` is game-independent logic (area parsing, block rules, hit tracking, stats),
`meowdyes/render/` draws the model, animation and stats screen on an abstract canvas, and `meowdyes/mc/`
wires it to the game (events, HUD, commands, screen).
