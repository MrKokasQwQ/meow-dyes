package meowdyes.mc;

import meowdyes.Classifier;
import meowdyes.Dye;
import meowdyes.Fmt;
import meowdyes.MeowLog;
import meowdyes.Stats;
import meowdyes.render.DropAnimation;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class MeowDyesClient implements ClientModInitializer {
    public static final String MOD_ID = "meowdyes";

    /** SkyBlock counts. */
    static Stats skyblock;
    /** Counts from normal worlds (singleplayer, other servers), kept apart so they don't mix with SkyBlock. */
    static Stats normalWorld;
    private static long ticks;
    private static Object areaLevel;
    private static Screen pendingScreen;

    // animation queue
    private static DropAnimation playing;
    private static long playingStart;
    private static final ArrayDeque<Dye> QUEUE = new ArrayDeque<>();

    // tick-scheduled effects (sounds, world particles)
    private record Scheduled(long at, Runnable action) {}
    private static final List<Scheduled> SCHEDULED = new ArrayList<>();

    private static boolean particlesBroken, soundsBroken;

    @Override
    public void onInitializeClient() {
        Path dir = null;
        try {
            dir = FabricLoader.getInstance().getConfigDir();
        } catch (Throwable t) {
            MeowLog.once("finding the config folder", t);
        }
        skyblock = new Stats(dir == null ? null : dir.resolve("meowdyes.properties"));
        skyblock.load();
        normalWorld = new Stats(dir == null ? null : dir.resolve("meowdyes-normal-worlds.properties"));
        normalWorld.load();

        ClientTickEvents.END_CLIENT_TICK.register(MeowDyesClient::onEndTick);
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            try {
                BreakTracker.onAttack(level, pos);
            } catch (Throwable t) {
                MeowLog.once("tracking the hit block", t);
            }
            return InteractionResult.PASS;
        });
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> onGameMessage(message, overlay));
        ClientReceiveMessageEvents.GAME_CANCELED.register((message, overlay) -> onGameMessage(message, overlay));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> Commands.register(dispatcher));
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "drop_animation"),
                (graphics, deltaTracker) -> renderHud(graphics));
        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> {
            if (skyblock.isDirty()) skyblock.save();
            if (normalWorld.isDirty()) normalWorld.save();
        });
    }

    // ------------------------------------------------------------------------------------------ events

    private static void onEndTick(Minecraft mc) {
        ticks++;
        try {
            // the area only changes when you move between areas, so once a second is plenty
            if (mc.level != areaLevel || ticks % 20 == 0) {
                areaLevel = mc.level;
                AreaTracker.update(mc);
            }
            if (mc.level != null) BreakTracker.tick(mc);
        } catch (Throwable t) {
            MeowLog.once("tick", t);
        }
        if (!SCHEDULED.isEmpty()) {
            List<Scheduled> due = new ArrayList<>();
            for (Iterator<Scheduled> it = SCHEDULED.iterator(); it.hasNext(); ) {
                Scheduled s = it.next();
                if (s.at() <= ticks) {
                    due.add(s);
                    it.remove();
                }
            }
            for (Scheduled s : due) {
                try {
                    s.action().run();
                } catch (Throwable t) {
                    MeowLog.once("effect", t);
                }
            }
        }
        if (pendingScreen != null) {
            // opened a tick after the command so the closing chat screen doesn't replace it
            Screen s = pendingScreen;
            pendingScreen = null;
            try {
                mc.setScreen(s);
            } catch (Throwable t) {
                MeowLog.once("opening the dyes screen", t);
            }
        }
        if (ticks % 1200 == 0) { // save at most once a minute, and only if something changed
            if (skyblock.isDirty()) skyblock.save();
            if (normalWorld.isDirty()) normalWorld.save();
        }
    }

    private static void onGameMessage(Component message, boolean overlay) {
        if (overlay || message == null) return;
        try {
            if (Classifier.isPristineMessage(message.getString())) roll(Dye.PRISTINE);
        } catch (Throwable t) {
            MeowLog.once("reading chat", t);
        }
    }

    // ------------------------------------------------------------------------------------------ drops

    static void onBlockMined(Dye dye) {
        roll(dye);
    }

    /** The counts that apply where you are: SkyBlock, or normal worlds. */
    static Stats active() {
        return AreaTracker.current().inSkyBlock ? skyblock : normalWorld;
    }

    static boolean inNormalWorld() {
        return !AreaTracker.current().inSkyBlock;
    }

    private static void roll(Dye dye) {
        Stats s = active();
        s.addMined(dye);
        if (ThreadLocalRandom.current().nextLong(dye.oneIn) == 0L) {
            long[] r = s.addFound(dye);
            s.save();
            announce(dye, r[0], r[1], false);
        }
    }

    /** Chat lines + animation. {@code test} adds "(test)" and changes nothing in the stats. */
    static void announce(Dye dye, long number, long after, boolean test) {
        MutableComponent line1 = Component.empty()
                .append(Component.literal("CRAZY RARE DROP! ").withColor(dye.highlight()).withStyle(ChatFormatting.BOLD))
                .append(gradient(dye.fullName(), dye.titleGradient, true));
        if (test) line1.append(Component.literal(" (test)").withColor(0x777777));
        else if (inNormalWorld()) line1.append(Component.literal(" (normal world)").withColor(0x777777));
        MutableComponent line2 = Component.empty()
                .append(Component.literal(" 1 in " + Fmt.num(dye.oneIn)).withColor(0xE6E6E6))
                .append(Component.literal("  ·  ").withColor(0x555555))
                .append(Component.literal("dye #" + Fmt.num(number)).withColor(0xBBBBBB))
                .append(Component.literal("  ·  ").withColor(0x555555))
                .append(Component.literal("after " + Fmt.num(after) + " " + dye.unit).withColor(0x999999));
        send(line1);
        send(line2);
        play(dye);
    }

    static void play(Dye dye) {
        if (playing == null) start(dye);
        else if (QUEUE.size() < 5) QUEUE.add(dye);
    }

    private static void start(Dye dye) {
        playing = new DropAnimation(dye, System.nanoTime());
        playingStart = System.nanoTime();
        schedule(0, () -> worldParticles(dye, 1f));
        schedule(3, () -> worldParticles(dye, 0.6f));
        schedule(0, () -> sound(0.75f));
        schedule(2, () -> sound(1.0f));
        schedule(4, () -> sound(1.26f));
        schedule(7, () -> sound(1.5f));
    }

    private static void renderHud(GuiGraphicsExtractor graphics) {
        DropAnimation anim = playing;
        if (anim == null) return;
        float t = (System.nanoTime() - playingStart) / 1_000_000_000f;
        if (anim.finished(t)) {
            playing = null;
            Dye next = QUEUE.poll();
            if (next != null) start(next);
            else McCanvas.clearCache(); // nothing left to draw: let go of everything
            return;
        }
        try {
            Minecraft mc = Minecraft.getInstance();
            anim.render(new McCanvas(graphics, mc.font, graphics.guiWidth(), graphics.guiHeight()), t);
        } catch (Throwable th) {
            MeowLog.once("drawing the animation", th);
            playing = null;
        }
    }

    // ------------------------------------------------------------------------------------------ effects

    private static void schedule(int delayTicks, Runnable r) {
        SCHEDULED.add(new Scheduled(ticks + delayTicks, r));
    }

    private static void sound(float pitch) {
        if (soundsBroken) return;
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return;
        try {
            SoundEvent pling = SoundEvents.NOTE_BLOCK_PLING.value();
            p.playSound(pling, 0.9f, pitch);
        } catch (Throwable t) {
            MeowLog.once("playing the drop sound", t);
            soundsBroken = true;
            try {
                p.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.6f, 1f);
            } catch (Throwable ignored) {
            }
        }
    }

    private static void worldParticles(Dye dye, float amount) {
        if (particlesBroken) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        try {
            Vec3 pos = mc.player.position();
            Vec3 look = mc.player.getLookAngle();
            double x = pos.x + look.x * 2.2, y = pos.y + 1.62 + look.y * 2.2, z = pos.z + look.z * 2.2;
            ThreadLocalRandom r = ThreadLocalRandom.current();
            for (String name : dye.worldParticles) {
                ParticleOptions type = particle(name);
                if (type == null) continue;
                int n = Math.round(14 * amount);
                for (int i = 0; i < n; i++) {
                    mc.level.addParticle(type,
                            x + r.nextGaussian() * 0.45, y + r.nextGaussian() * 0.35, z + r.nextGaussian() * 0.45,
                            r.nextGaussian() * 0.06, 0.04 + r.nextDouble() * 0.08, r.nextGaussian() * 0.06);
                }
            }
        } catch (Throwable t) {
            MeowLog.once("spawning particles", t);
            particlesBroken = true;
        }
    }

    private static ParticleOptions particle(String name) {
        try {
            return switch (name) {
                case "FLAME" -> ParticleTypes.FLAME;
                case "LAVA" -> ParticleTypes.LAVA;
                case "LARGE_SMOKE" -> ParticleTypes.LARGE_SMOKE;
                case "SMOKE" -> ParticleTypes.SMOKE;
                case "FIREWORK" -> ParticleTypes.FIREWORK;
                case "END_ROD" -> ParticleTypes.END_ROD;
                case "SNOWFLAKE" -> ParticleTypes.SNOWFLAKE;
                case "HAPPY_VILLAGER" -> ParticleTypes.HAPPY_VILLAGER;
                case "ELECTRIC_SPARK" -> ParticleTypes.ELECTRIC_SPARK;
                case "PORTAL" -> ParticleTypes.PORTAL;
                case "REVERSE_PORTAL" -> ParticleTypes.REVERSE_PORTAL;
                case "WAX_ON" -> ParticleTypes.WAX_ON;
                case "CRIT" -> ParticleTypes.CRIT;
                case "ENCHANT" -> ParticleTypes.ENCHANT;
                case "CLOUD" -> ParticleTypes.CLOUD;
                case "TOTEM_OF_UNDYING" -> ParticleTypes.TOTEM_OF_UNDYING;
                case "CRIMSON_SPORE" -> ParticleTypes.CRIMSON_SPORE;
                case "ASH" -> ParticleTypes.ASH;
                case "WITCH" -> ParticleTypes.WITCH;
                case "MYCELIUM" -> ParticleTypes.MYCELIUM;
                case "POOF" -> ParticleTypes.POOF;
                default -> null;
            };
        } catch (Throwable t) {
            MeowLog.once("particle " + name, t);
            return null;
        }
    }

    // ------------------------------------------------------------------------------------------ helpers

    static void openScreen(Screen s) {
        pendingScreen = s;
    }

    static MutableComponent prefix() {
        return Component.literal("[Meow Dyes] ").withColor(0xFF8AD8);
    }

    static MutableComponent gradient(String s, int[] stops, boolean bold) {
        MutableComponent out = Component.empty();
        int n = Math.max(1, s.length() - 1);
        for (int i = 0; i < s.length(); i++) {
            MutableComponent ch = Component.literal(s.substring(i, i + 1)).withColor(Fmt.gradient(stops, i / (float) n));
            if (bold) ch = ch.withStyle(ChatFormatting.BOLD);
            out.append(ch);
        }
        return out;
    }

    /** Adds a line to your own chat window. Client-only: nothing is sent to the server. */
    static void send(Component c) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui == null) return;
        try {
            mc.gui.getChat().addClientSystemMessage(c);
        } catch (Throwable t) {
            MeowLog.once("showing a chat line", t);
        }
    }
}
