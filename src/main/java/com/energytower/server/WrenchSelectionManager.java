package com.energytower.server;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 服务端玩家「已选能量塔」状态管理。
 * <p>
 * 只保存在服务端内存中（临时交互状态，无需持久化），
 * 客户端无法读取或篡改，保证绑定操作全程由服务端权威判定，防止作弊。
 */
public final class WrenchSelectionManager {

    /** 一个选中状态：维度 + 坐标 */
    public record TowerSelection(ResourceKey<Level> dimension, BlockPos pos) {
    }

    /** 玩家 UUID → 已选能量塔 */
    private static final Map<UUID, TowerSelection> SELECTIONS = new HashMap<>();

    private WrenchSelectionManager() {
    }

    public static void select(net.minecraft.world.entity.player.Player player, ResourceKey<Level> dimension, BlockPos pos) {
        SELECTIONS.put(player.getUUID(), new TowerSelection(dimension, pos));
    }

    public static void clear(net.minecraft.world.entity.player.Player player) {
        SELECTIONS.remove(player.getUUID());
    }

    @Nullable
    public static TowerSelection get(net.minecraft.world.entity.player.Player player) {
        return SELECTIONS.get(player.getUUID());
    }

    /** 判断玩家当前选中的是否就是该塔 */
    public static boolean isSelected(net.minecraft.world.entity.player.Player player, ResourceKey<Level> dimension, BlockPos pos) {
        TowerSelection sel = SELECTIONS.get(player.getUUID());
        return sel != null && sel.dimension().equals(dimension) && sel.pos().equals(pos);
    }

}
