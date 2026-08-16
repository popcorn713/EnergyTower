package com.energytower.client;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 客户端缓存的「扳手高亮」数据（由服务端 S2C 包同步）。
 * <p>
 * 纯数据持有类，不引用任何客户端渲染类，因此可以安全地在通用端（服务端也会加载该类，
 * 只是不会写入/读取）加载，避免把渲染逻辑混进网络层。
 */
public final class ClientWrenchData {

    /** 当前已选的能量塔坐标（可为 null） */
    @Nullable
    private static BlockPos selectedTower;

    /** 绑定到已选能量塔的机器坐标清单 */
    private static List<BlockPos> boundTargets = List.of();

    private ClientWrenchData() {
    }

    public static void update(@Nullable BlockPos tower, @Nullable List<BlockPos> targets) {
        selectedTower = tower;
        boundTargets = targets == null ? List.of() : List.copyOf(targets);
    }

    @Nullable
    public static BlockPos getSelectedTower() {
        return selectedTower;
    }

    public static List<BlockPos> getBoundTargets() {
        return boundTargets;
    }

    /** 是否有需要渲染的高亮数据 */
    public static boolean hasData() {
        return selectedTower != null || !boundTargets.isEmpty();
    }
}
