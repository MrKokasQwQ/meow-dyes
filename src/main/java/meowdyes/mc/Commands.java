package meowdyes.mc;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import meowdyes.Dye;
import meowdyes.Stats;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/** /md dyes and /md test [dye]. Both run only on your side; nothing is sent to the server. */
final class Commands {
    private Commands() {}

    private static final SuggestionProvider<FabricClientCommandSource> DYES = (ctx, builder) -> {
        String rem = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (Dye d : Dye.values()) if (d.id.startsWith(rem)) builder.suggest(d.id);
        return builder.buildFuture();
    };

    static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommands.literal("md")
                .executes(ctx -> {
                    ctx.getSource().sendFeedback(MeowDyesClient.prefix()
                            .append(Component.literal("/md dyes  ·  /md test <dye>").withColor(0xDDDDDD)));
                    return 1;
                })
                .then(ClientCommands.literal("dyes").executes(ctx -> {
                    MeowDyesClient.openScreen(new DyesScreen());
                    return 1;
                }))
                .then(ClientCommands.literal("test")
                        .executes(ctx -> test(Dye.values()[ThreadLocalRandom.current().nextInt(Dye.values().length)]))
                        .then(ClientCommands.argument("dye", StringArgumentType.word()).suggests(DYES)
                                .executes(Commands::testArg))));
    }

    private static int testArg(CommandContext<FabricClientCommandSource> ctx) {
        String arg = StringArgumentType.getString(ctx, "dye");
        Dye d = Dye.byId(arg);
        if (d == null) {
            StringBuilder ids = new StringBuilder();
            for (Dye x : Dye.values()) ids.append(ids.isEmpty() ? "" : ", ").append(x.id);
            ctx.getSource().sendError(Component.literal("Unknown dye '" + arg + "'. Try: " + ids));
            return 0;
        }
        return test(d);
    }

    private static int test(Dye d) {
        Stats.Entry e = MeowDyesClient.active().get(d);
        MeowDyesClient.announce(d, e.found + 1, e.since, true);
        return 1;
    }
}
