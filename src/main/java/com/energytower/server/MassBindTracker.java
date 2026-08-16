package com.energytower.server;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

/**
 * 批量绑定保护（服务端）。
 * <p>
 * 原因：Shift+Ctrl+右键 在客户端会同时发出「批量绑定 C2S 包」和「普通右键包」。
 * 服务端看不到 Ctrl 键，普通右键会把刚被批量绑定（尤其是点中的那台）又解绑。
 * 由于同一连接上的包按序到达，批量绑定包总在普通右键之前被处理，
 * 这里记录“刚被批量绑定的位置”，让随后到达的普通右键在窗口期内跳过。
 */
public final class MassBindTracker {

    /** 保护窗口（Tick） */
    private static final long WINDOW_TICKS = 5;

    /** 位置 → 标记时的游戏时间 */
    private static final Map<BlockPos, Long> RECENT = new HashMap<>();

    private MassBindTracker() {
    }

    /** 标记某个位置刚被批量绑定 */
    public static void mark(BlockPos pos, long gameTime) {
        RECENT.put(pos, gameTime);
        if (RECENT.size() > 256) {
            prune(gameTime);
        }
    }

    /** 该位置是否在窗口期内刚被批量绑定（普通右键应跳过，避免把刚绑定的机器又解掉） */
    public static boolean isRecent(BlockPos pos, long gameTime) {
        Long t = RECENT.get(pos);
        return t != null && gameTime - t < WINDOW_TICKS;
    }

    private static void prune(long gameTime) {
        RECENT.entrySet().removeIf(e -> gameTime - e.getValue() >= WINDOW_TICKS);
    }
}
