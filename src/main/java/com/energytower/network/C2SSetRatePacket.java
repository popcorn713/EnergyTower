package com.energytower.network;

import com.energytower.EnergyTower;
import com.energytower.blockentity.EnergyTowerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 客户端 → 服务端：设置无线单次传输速率（GUI 滑块拖动时发送）。
 * <p>
 * 服务端会再次校验：方块实体必须是能量塔、玩家必须在合理距离内，才真正生效。
 */
public record C2SSetRatePacket(BlockPos pos, int rate) implements CustomPacketPayload {

    public static final Type<C2SSetRatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EnergyTower.MODID, "set_rate"));

    public static final StreamCodec<FriendlyByteBuf, C2SSetRatePacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, C2SSetRatePacket::pos,
            ByteBufCodecs.VAR_INT, C2SSetRatePacket::rate,
            C2SSetRatePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final C2SSetRatePacket data, final IPayloadContext context) {
        // 网络线程 → 主线程执行，才能安全访问世界
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                Level level = serverPlayer.level();
                BlockEntity be = level.getBlockEntity(data.pos());
                if (be instanceof EnergyTowerBlockEntity tower) {
                    // 防作弊：玩家必须站在能量塔 8 格范围内
                    if (serverPlayer.distanceToSqr(data.pos().getCenter()) <= 8.0 * 8.0) {
                        tower.setTransferRate(data.rate());
                    }
                }
            }
        });
    }
}
