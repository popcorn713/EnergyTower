package com.energytower.network;

import com.energytower.EnergyTower;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 网络载荷注册（NeoForge 1.21.1 新网络 API：SimpleChannel → PayloadRegistrar）。
 * <p>
 * 所有客户端 → 服务端的数据包都只传递「操作意图」，服务端校验后才会真正修改数据，
 * 防止作弊。
 */
public final class ModNetwork {

    private ModNetwork() {
    }

    /** 在 RegisterPayloadHandlersEvent（GAME 总线）中注册全部数据包 */
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(EnergyTower.MODID).versioned("1").optional();
        // 客户端请求：调整无线传输速率
        registrar.playToServer(C2SSetRatePacket.TYPE, C2SSetRatePacket.STREAM_CODEC, C2SSetRatePacket::handle);
        // 客户端请求：切换传输模式
        registrar.playToServer(C2SSetModePacket.TYPE, C2SSetModePacket.STREAM_CODEC, C2SSetModePacket::handle);
        // 客户端请求：清空全部绑定（GUI 二次确认后发送）
        registrar.playToServer(C2SClearBindingsPacket.TYPE, C2SClearBindingsPacket.STREAM_CODEC, C2SClearBindingsPacket::handle);
        // 客户端请求：一键批量绑定相邻同种机器（Shift+Ctrl+右键）
        registrar.playToServer(C2SMassBindPacket.TYPE, C2SMassBindPacket.STREAM_CODEC, C2SMassBindPacket::handle);
        // 服务端 → 客户端：同步扳手高亮（已选塔 + 绑定机器）
        registrar.playToClient(S2CWrenchHighlightPacket.TYPE, S2CWrenchHighlightPacket.STREAM_CODEC, S2CWrenchHighlightPacket::handle);
    }
}
