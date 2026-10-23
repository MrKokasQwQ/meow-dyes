package meowdyes.mc;

import meowdyes.AreaState;
import meowdyes.MeowLog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.List;

/** Reads the sidebar scoreboard and the tab list into an {@link AreaState}. */
final class AreaTracker {
    private static AreaState current = AreaState.NONE;

    private AreaTracker() {}

    static AreaState current() {
        return current;
    }

    static void update(Minecraft mc) {
        if (mc.level == null) {
            current = AreaState.NONE;
            return;
        }
        String title = null;
        List<String> sidebar = new ArrayList<>();
        List<String> tab = new ArrayList<>();
        try {
            Scoreboard sb = mc.level.getScoreboard();
            Objective obj = sb.getDisplayObjective(DisplaySlot.SIDEBAR);
            if (obj != null) {
                title = obj.getDisplayName().getString();
                for (PlayerScoreEntry e : sb.listPlayerScores(obj)) {
                    PlayerTeam team = sb.getPlayersTeam(e.owner());
                    if (team != null) {
                        sidebar.add(team.getPlayerPrefix().getString() + team.getPlayerSuffix().getString());
                    } else {
                        sidebar.add(e.owner());
                    }
                }
            }
        } catch (Throwable t) {
            MeowLog.once("reading the scoreboard", t);
        }
        try {
            ClientPacketListener conn = mc.getConnection();
            if (conn != null) {
                // only the "Area: ..." line matters, so stop as soon as it's found
                for (PlayerInfo info : conn.getOnlinePlayers()) {
                    Component name = info.getTabListDisplayName();
                    if (name == null) continue;
                    String line = name.getString();
                    if (AreaState.isAreaLine(line)) {
                        tab.add(line);
                        break;
                    }
                }
            }
        } catch (Throwable t) {
            MeowLog.once("reading the tab list", t);
        }
        current = AreaState.parse(title, sidebar, tab);
    }
}
