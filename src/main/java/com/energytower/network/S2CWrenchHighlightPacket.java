package com.energytower.network;

import com.energytower.EnergyTower;
import com.energytower.client.ClientWrenchData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;
import java.util.Optional;

/**
 * 服务端 → 客户端：同步「扳手高亮」数据（已选能量塔 + 绑定到该塔的机器清单）。
 * 客户端仅缓存并渲染线框，不参与任何逻辑判定。
 */
public record S2CWrenchHighlightPacket(Optional<BlockPos> selectedTower, List<BlockPos> boundTargets) implements CustomPacketPayload {

    public static final Type<S2CWrenchHighlightPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EnergyTower.MODID, "wrench_highlight"));

    public static final StreamCodec<FriendlyByteBuf, S2CWrenchHighlightPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), S2CWrenchHighlightPacket::selectedTower,
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), S2CWrenchHighlightPacket::boundTargets,
            S2CWrenchHighlightPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CWrenchHighlightPacket data, IPayloadContext context) {
        context.enqueueWork(() ->
                ClientWrenchData.update(data.selectedTower().orElse(null), data.boundTargets()));
    }
}
