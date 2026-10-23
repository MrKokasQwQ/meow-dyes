package meowdyes.mc;

import meowdyes.Classifier;
import meowdyes.Dye;
import meowdyes.HitTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.IdentityHashMap;
import java.util.Map;

/** Feeds the game's attack events, crosshair and world into {@link HitTracker}. */
final class BreakTracker {
    private static final Map<Block, String> IDS = new IdentityHashMap<>();
    private static Level lastLevel;

    private static final HitTracker<BlockPos, Block> TRACKER = new HitTracker<>(new HitTracker.Env<>() {
        @Override
        public Block blockAt(BlockPos pos) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return null;
            BlockState state = mc.level.getBlockState(pos);
            return state == null ? null : state.getBlock();
        }

        @Override public long key(BlockPos pos) { return pos.asLong(); }
        @Override public BlockPos keep(BlockPos pos) { return pos.immutable(); }
        @Override public String id(Block block) { return blockId(block); }
        @Override public Dye classify(String blockId) { return Classifier.classify(blockId, AreaTracker.current()); }
        @Override public boolean active() { return Minecraft.getInstance().level != null; }
    }, (dye, blockId) -> MeowDyesClient.onBlockMined(dye));

    private BreakTracker() {}

    /** Fabric AttackBlockCallback: the exact block you just started hitting. */
    static void onAttack(Level level, BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (level == null || level != mc.level || level != lastLevel || pos == null) return;
        TRACKER.hit(pos);
    }

    static void clear() {
        TRACKER.clear();
    }

    static void tick(Minecraft mc) {
        if (mc.level == null || mc.level != lastLevel) {
            // new world (warp, server switch): forget everything so old positions can't "change"
            TRACKER.clear();
            lastLevel = mc.level;
            if (mc.level == null) return;
        }
        TRACKER.tick();
        // keep the block under the crosshair remembered while the attack key is held
        if (mc.screen == null && mc.options != null && mc.options.keyAttack.isDown()) {
            HitResult hit = mc.hitResult;
            if (hit != null && hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult bhr) {
                TRACKER.hit(bhr.getBlockPos());
            }
        }
    }

    static String blockId(Block block) {
        String id = IDS.get(block);
        if (id == null) {
            Identifier key = BuiltInRegistries.BLOCK.getKey(block);
            id = key == null ? "unknown" : key.getPath();
            IDS.put(block, id);
        }
        return id;
    }
}
