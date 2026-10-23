package meowdyes.mc;

import meowdyes.MeowLog;
import meowdyes.render.DyesPage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** /md dyes: a card per dye with its spinning model, counts and odds. */
final class DyesScreen extends Screen {
    private final long opened = System.nanoTime();
    private float scroll;
    private int contentHeight;
    private boolean broken;

    DyesScreen() {
        super(Component.literal("Meow Dyes"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        if (broken) return;
        try {
            clampScroll();
            float time = (System.nanoTime() - opened) / 1_000_000_000f;
            McCanvas canvas = new McCanvas(graphics, Minecraft.getInstance().font, this.width, this.height);
            String scope = MeowDyesClient.inNormalWorld() ? "Normal-world counts" : "SkyBlock counts";
            contentHeight = DyesPage.render(canvas, MeowDyesClient.active(), scope, AreaTracker.current(), mouseX, mouseY, scroll, time);
        } catch (Throwable t) {
            MeowLog.once("drawing the dyes screen", t);
            broken = true;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        scroll -= (float) vertical * 24f;
        clampScroll();
        return true;
    }

    private void clampScroll() {
        float max = Math.max(0, contentHeight - (this.height - DyesPage.TOP - DyesPage.FOOTER) + 4);
        if (scroll > max) scroll = max;
        if (scroll < 0) scroll = 0;
    }

    /** Called when the screen closes: drop the cached text so nothing of ours stays around. */
    @Override
    public void removed() {
        McCanvas.clearCache();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
