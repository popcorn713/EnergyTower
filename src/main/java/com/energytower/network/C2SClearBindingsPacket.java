package com.energytower.network;

import com.energytower.EnergyTower;
import com.energytower.blockentity.EnergyTowerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;
import java.util.Optional;

/**
 * 客户端 → 服务端：请求清空能量塔的全部无线绑定（GUI「清除全部绑定」按钮，带确认后发送）。
 * <p>
 * 服务端校验玩家在塔旁后才执行，执行后把最新（已清空）的高亮数据同步回该玩家客户端。
 */
public record C2SClearBindingsPacket(BlockPos pos) implements CustomPacketPayload {

    public static final Type<C2SClearBindingsPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EnergyTower.MODID, "clear_bindings"));

    public static final StreamCodec<FriendlyByteBuf, C2SClearBindingsPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, C2SClearBindingsPacket::pos,
            C2SClearBindingsPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SClearBindingsPacket data, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                Level level = serverPlayer.level();
                BlockEntity be = level.getBlockEntity(data.pos());
                if (be instanceof EnergyTowerBlockEntity tower) {
                    // 防作弊：玩家须在塔 8 格范围内
                    if (serverPlayer.distanceToSqr(data.pos().getCenter()) <= 8.0 * 8.0) {
                        tower.clearBindings();
                        serverPlayer.displayClientMessage(
                                net.minecraft.network.chat.Component.translatable("message.energy_tower.bindings_cleared"), true);
                        // 同步高亮（已清空，仅保留塔本身）
                        PacketDistributor.sendToPlayer(serverPlayer, new S2CWrenchHighlightPacket(
                                Optional.of(tower.getBlockPos()), List.of()));
                    }
                }
            }
        });
    }
}
