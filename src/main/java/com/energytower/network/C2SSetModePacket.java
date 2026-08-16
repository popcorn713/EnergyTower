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
 * 客户端 → 服务端：切换无线传输模式（速率限制 / 无上限，GUI 按钮点击时发送）。
 * <p>
 * 服务端校验后才会生效，防止客户端伪造。
 */
public record C2SSetModePacket(BlockPos pos, boolean unlimited) implements CustomPacketPayload {

    public static final Type<C2SSetModePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EnergyTower.MODID, "set_mode"));

    public static final StreamCodec<FriendlyByteBuf, C2SSetModePacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, C2SSetModePacket::pos,
            ByteBufCodecs.BOOL, C2SSetModePacket::unlimited,
            C2SSetModePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final C2SSetModePacket data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                Level level = serverPlayer.level();
                BlockEntity be = level.getBlockEntity(data.pos());
                if (be instanceof EnergyTowerBlockEntity tower) {
                    if (serverPlayer.distanceToSqr(data.pos().getCenter()) <= 8.0 * 8.0) {
                        tower.setUnlimitedMode(data.unlimited());
                    }
                }
            }
        });
    }
}
