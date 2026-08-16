package com.energytower.network;

import com.energytower.EnergyTower;
import com.energytower.blockentity.EnergyTowerBlockEntity;
import com.energytower.energy.EnergyTransferUtil;
import com.energytower.server.MassBindTracker;
import com.energytower.server.WrenchSelectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 客户端 → 服务端：请求“一键绑定所有相邻的同种机器”（Shift+Ctrl+右键 机器时由客户端发送）。
 * <p>
 * 服务端从玩家点击的机器出发做 BFS 洪泛，把「与它相连的、方块类型相同的机器」全部绑定到
 * 玩家当前选中的能量塔上。数量有限制，防止滥用/性能问题。
 */
public record C2SMassBindPacket(BlockPos machinePos) implements CustomPacketPayload {

    /** 单次批量绑定的最大机器数（防止超大集群导致卡顿） */
    private static final int MAX_MASS_BIND = 128;

    public static final Type<C2SMassBindPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EnergyTower.MODID, "mass_bind"));

    public static final StreamCodec<FriendlyByteBuf, C2SMassBindPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, C2SMassBindPacket::machinePos,
            C2SMassBindPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SMassBindPacket data, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                Level level = serverPlayer.level();
                // 必须已选定能量塔，且与点击机器同维度
                WrenchSelectionManager.TowerSelection sel = WrenchSelectionManager.get(serverPlayer);
                if (sel == null || !sel.dimension().equals(level.dimension())) {
                    serverPlayer.displayClientMessage(Component.translatable("message.energy_tower.no_tower_selected"), true);
                    return;
                }
                if (!(level.getBlockEntity(sel.pos()) instanceof EnergyTowerBlockEntity tower)) {
                    WrenchSelectionManager.clear(serverPlayer);
                    serverPlayer.displayClientMessage(Component.translatable("message.energy_tower.tower_missing"), true);
                    return;
                }
                // 点击的机器必须可接收 FE
                if (!EnergyTransferUtil.supportsEnergyReceive(level, data.machinePos())) {
                    serverPlayer.displayClientMessage(Component.translatable("message.energy_tower.not_energy_receiver"), true);
                    return;
                }

                // BFS 洪泛：与点击机器相邻（6 向）且方块类型相同的机器全部绑定
                BlockState base = level.getBlockState(data.machinePos());
                Set<BlockPos> visited = new HashSet<>();
                Deque<BlockPos> queue = new ArrayDeque<>();
                queue.add(data.machinePos());
                int bound = 0;
                while (!queue.isEmpty() && visited.size() < MAX_MASS_BIND) {
                    BlockPos cur = queue.poll();
                    if (!visited.add(cur)) {
                        continue;
                    }
                    if (!level.isLoaded(cur)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(cur);
                    if (state.isAir() || state.getBlock() != base.getBlock()) {
                        continue; // 只处理同种机器
                    }
                    if (EnergyTransferUtil.supportsEnergyReceive(level, cur) && tower.addBinding(level, cur)) {
                        bound++;
                    }
                    for (Direction dir : Direction.values()) {
                        queue.add(cur.relative(dir));
                    }
                }
                // 标记刚被批量绑定的位置：让随后到达的普通右键跳过，避免把点中的机器又解绑
                for (BlockPos p : visited) {
                    MassBindTracker.mark(p, level.getGameTime());
                }

                if (bound > 0) {
                    serverPlayer.displayClientMessage(Component.translatable("message.energy_tower.mass_bind", bound), true);
                } else {
                    serverPlayer.displayClientMessage(Component.translatable("message.energy_tower.mass_bind_none"), true);
                }
                // 同步高亮：已选塔 + 全部绑定机器
                PacketDistributor.sendToPlayer(serverPlayer, new S2CWrenchHighlightPacket(
                        Optional.of(tower.getBlockPos()), List.copyOf(tower.getBoundTargets())));
            }
        });
    }
}
